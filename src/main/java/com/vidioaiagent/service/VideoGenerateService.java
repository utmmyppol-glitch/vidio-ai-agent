package com.vidioaiagent.service;

import com.vidioaiagent.dto.request.AdGenerateRequest;
import com.vidioaiagent.dto.response.AdCopyResponse;
import com.vidioaiagent.dto.response.VideoGenerateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 숏폼 영상 조립 엔진.
 *
 * 파이프라인:
 * 1) 씬별 Pexels 스톡 영상 다운 (videoSearchKeyword)
 * 2) FFmpeg 트림 + 리사이즈 + 크롭 + 페이드
 * 3) 크로스페이드 합성
 * 4) ASS 자막 번인 (hook/cta=중앙 강조, 나머지=하단)
 * 5) TTS 내레이션 합성
 * 6) BGM 믹스
 *
 * 성공 기준: 키워드 적합 + 5씬 구조 + 자막 보임 + 오디오 있음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerateService {

    private final StockImageService stockService;

    private static final String FFMPEG = "/opt/homebrew/bin/ffmpeg";
    private static final String FFPROBE = "/opt/homebrew/bin/ffprobe";
    private static final String SAY = "/usr/bin/say";

    @Value("${app.content.video-dir:./generated-content/videos}")
    private String videoDir;
    @Value("${app.content.thumbnail-dir:./generated-content/thumbnails}")
    private String thumbnailDir;

    private boolean ffmpegOk = false;
    private boolean ttsOk = false;

    @PostConstruct
    public void init() {
        mkdirs(videoDir); mkdirs(thumbnailDir);
        ffmpegOk = cmdOk(FFMPEG, "-version");
        ttsOk = cmdOk(SAY, "--version");
        log.info("=== FFmpeg:{} TTS:{} Pexels:{} ===", ffmpegOk, ttsOk, stockService.hasPexelsKey());
    }

    public VideoGenerateResponse generateVideo(AdCopyResponse adCopy, AdGenerateRequest request) {
        log.info("=== 영상 조립 시작: {} (씬 {}개) ===",
                request.getProductName(), adCopy.getScenes() != null ? adCopy.getScenes().size() : 0);

        String video = null, thumb = null;
        try { thumb = generateThumbnail(adCopy, request); } catch (Exception e) { log.error("썸네일: {}", e.getMessage()); }
        if (ffmpegOk && adCopy.getScenes() != null && !adCopy.getScenes().isEmpty()) {
            try { video = buildVideo(adCopy, request); } catch (Exception e) { log.error("영상: {}", e.getMessage(), e); }
        }

        return VideoGenerateResponse.builder()
                .videoUrl(video).thumbnailUrl(thumb)
                .uploadText(buildUploadText(adCopy))
                .status(video != null ? "VIDEO_READY" : "SCRIPT_READY").build();
    }

    // ═══════════════════════════════════════════════════════════
    //  메인 파이프라인
    // ═══════════════════════════════════════════════════════════

    private String buildVideo(AdCopyResponse adCopy, AdGenerateRequest request) throws Exception {
        stockService.resetUsedVideos(); // 프로젝트별 영상 재사용 방지 초기화
        List<AdCopyResponse.SceneEntry> scenes = adCopy.getScenes();
        boolean vert = !request.getPlatform().name().contains("LONG");
        int w = vert ? 1080 : 1920, h = vert ? 1920 : 1080;
        String orient = vert ? "portrait" : "landscape";
        String uid = uuid8();
        List<Path> cleanup = new ArrayList<>();

        // ── STEP 1: 씬별 클립 생성 (영상 + 자막 PNG overlay) ──
        List<Path> clips = new ArrayList<>();
        for (int i = 0; i < scenes.size(); i++) {
            AdCopyResponse.SceneEntry sc = scenes.get(i);
            double dur = Math.max(sc.getDuration(), 2.0);
            log.info("  씬{}/{}: [{}] '{}' ({}s) kw='{}'",
                    i + 1, scenes.size(), sc.getPurpose(), sc.getText(), dur, sc.getVideoSearchKeyword());

            // 1a. 클린 클립 생성 (자막 없음)
            Path cleanClip = makeClip(sc, dur, w, h, orient, uid, i);
            if (cleanClip == null || !Files.exists(cleanClip) || Files.size(cleanClip) < 1000) {
                log.warn("    → 클립 실패, 스킵");
                continue;
            }

            // 1b. 자막 PNG 생성 (투명 배경 + 텍스트)
            Path subPng = renderSubtitlePng(w, h, sc.getText(), sc.getPurpose(), uid, i);
            cleanup.add(subPng);

            // 1c. 자막 PNG를 클립 위에 overlay
            Path subtitledClip = Path.of(videoDir, uid + "_subcl" + i + ".mp4");
            try {
                runFF("-y",
                        "-i", cleanClip.toString(),
                        "-i", subPng.toString(),
                        "-filter_complex", "[0:v][1:v]overlay=0:0:format=auto",
                        "-c:v", "libx264", "-preset", "fast", "-pix_fmt", "yuv420p",
                        subtitledClip.toString());
                safeDelete(cleanClip);
                if (Files.exists(subtitledClip) && Files.size(subtitledClip) > 1000) {
                    clips.add(subtitledClip);
                    cleanup.add(subtitledClip);
                    log.info("    → OK + 자막 ({}KB)", Files.size(subtitledClip) / 1024);
                } else {
                    // overlay 실패 시 자막 없는 클립 사용
                    clips.add(cleanClip);
                    cleanup.add(cleanClip);
                    log.info("    → OK (자막 없이) ({}KB)", Files.size(cleanClip) / 1024);
                }
            } catch (Exception e) {
                log.warn("    자막 overlay 실패, 자막 없이: {}", e.getMessage());
                clips.add(cleanClip);
                cleanup.add(cleanClip);
            }
        }
        if (clips.isEmpty()) throw new RuntimeException("클립 없음");

        // ── STEP 2: 크로스페이드 합성 ──
        Path merged;
        if (clips.size() == 1) {
            merged = clips.get(0);
        } else {
            merged = Path.of(videoDir, uid + "_merged.mp4");
            crossfadeMerge(clips, merged);
            cleanup.add(merged);
        }

        Path videoBase = merged;

        // ── STEP 4: TTS 내레이션 ──
        Path ttsFile = null;
        if (ttsOk) {
            ttsFile = generateTTS(scenes, uid);
            if (ttsFile != null) cleanup.add(ttsFile);
        }

        // ── STEP 5: BGM 생성 ──
        Path bgmFile = generateBGM(uid, totalDuration(scenes));
        if (bgmFile != null) cleanup.add(bgmFile);

        // ── STEP 6: 오디오 믹스 + 최종 출력 ──
        String outName = UUID.randomUUID() + ".mp4";
        Path finalOut = Path.of(videoDir, outName);

        boolean hasTTS = ttsFile != null && Files.exists(ttsFile) && Files.size(ttsFile) > 100;
        boolean hasBGM = bgmFile != null && Files.exists(bgmFile) && Files.size(bgmFile) > 100;

        if (hasTTS && hasBGM) {
            // TTS + BGM 믹스 → 영상에 합성
            Path mixedAudio = Path.of(videoDir, uid + "_mix.aac");
            cleanup.add(mixedAudio);
            runFF("-y", "-i", ttsFile.toString(), "-i", bgmFile.toString(),
                    "-filter_complex", "[0:a]volume=1.0[tts];[1:a]volume=0.15[bgm];[tts][bgm]amix=inputs=2:duration=longest[out]",
                    "-map", "[out]", "-c:a", "aac", "-b:a", "128k", mixedAudio.toString());
            runFF("-y", "-i", videoBase.toString(), "-i", mixedAudio.toString(),
                    "-c:v", "copy", "-c:a", "aac", "-shortest", finalOut.toString());
        } else if (hasTTS) {
            runFF("-y", "-i", videoBase.toString(), "-i", ttsFile.toString(),
                    "-c:v", "copy", "-c:a", "aac", "-b:a", "128k", "-shortest", finalOut.toString());
        } else if (hasBGM) {
            runFF("-y", "-i", videoBase.toString(), "-i", bgmFile.toString(),
                    "-c:v", "copy", "-c:a", "aac", "-b:a", "64k", "-shortest", finalOut.toString());
        } else {
            Files.copy(videoBase, finalOut, StandardCopyOption.REPLACE_EXISTING);
        }

        // 정리
        for (Path p : cleanup) safeDelete(p);

        long sz = Files.exists(finalOut) ? Files.size(finalOut) : 0;
        log.info("=== 최종 영상: {} ({}KB) 오디오:{} ===", outName, sz / 1024,
                hasTTS ? "TTS" + (hasBGM ? "+BGM" : "") : (hasBGM ? "BGM" : "없음"));
        return sz > 1000 ? outName : null;
    }

    // ═══════════════════════════════════════════════════════════
    //  씬 클립: Pexels 영상 → 트림+리사이즈 (자막 없이)
    // ═══════════════════════════════════════════════════════════

    private Path makeClip(AdCopyResponse.SceneEntry sc, double dur, int w, int h,
                           String orient, String uid, int idx) {
        String keyword = sc.getVideoSearchKeyword();

        // 1) Pexels 영상 다운로드
        Path raw = Path.of(videoDir, uid + "_raw" + idx + ".mp4");
        boolean got = false;
        if (keyword != null && !keyword.isBlank()) {
            got = stockService.downloadVideo(keyword, orient, raw);
        }
        if (!got) {
            got = stockService.downloadVideo(getFallbackKeyword(sc.getPurpose()), orient, raw);
        }

        // 2) 영상 정규화
        if (got && Files.exists(raw)) {
            try {
                if (Files.size(raw) > 5000) {
                    Path clip = Path.of(videoDir, uid + "_clip" + idx + ".mp4");
                    runFF("-y", "-i", raw.toString(),
                            "-t", fmt(dur),
                            "-vf", String.format("scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d,setsar=1,fps=30", w, h, w, h),
                            "-c:v", "libx264", "-preset", "fast", "-pix_fmt", "yuv420p", "-an",
                            clip.toString());
                    safeDelete(raw);
                    if (Files.exists(clip) && Files.size(clip) > 1000) return clip;
                }
            } catch (Exception e) {
                log.warn("    영상 정규화 실패: {}", e.getMessage());
            }
        }
        safeDelete(raw);

        // 3) 폴백: 이미지 + Ken Burns
        return makeImageClip(sc, dur, w, h, uid, idx);
    }

    private Path makeImageClip(AdCopyResponse.SceneEntry sc, double dur, int w, int h, String uid, int idx) {
        try {
            Path img = Path.of(videoDir, uid + "_img" + idx + ".jpg");
            String kw = sc.getVideoSearchKeyword() != null ? sc.getVideoSearchKeyword() : "aesthetic";
            if (!stockService.downloadImage(kw, w, h, img) || !Files.exists(img)) {
                renderGradient(img, w, h, idx);
            }
            Path clip = Path.of(videoDir, uid + "_clipfb" + idx + ".mp4");
            int frames = (int)(dur * 30);
            runFF("-y", "-loop", "1", "-i", img.toString(),
                    "-vf", String.format("scale=%d:%d:force_original_aspect_ratio=increase,crop=%d:%d," +
                                    "zoompan=z='min(zoom+0.0015,1.2)':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=%d:s=%dx%d:fps=30",
                            w * 2, h * 2, w * 2, h * 2, frames, w, h),
                    "-t", fmt(dur), "-c:v", "libx264", "-preset", "fast", "-pix_fmt", "yuv420p", clip.toString());
            safeDelete(img);
            return (Files.exists(clip) && Files.size(clip) > 1000) ? clip : null;
        } catch (Exception e) {
            log.warn("    이미지 클립 실패: {}", e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  크로스페이드 합성 (씬 간 부드러운 전환)
    // ═══════════════════════════════════════════════════════════

    private void crossfadeMerge(List<Path> clips, Path output) throws Exception {
        double fade = 0.3;
        StringBuilder filter = new StringBuilder();
        String prev = "[0:v]";
        double offset = 0;

        for (int i = 1; i < clips.size(); i++) {
            double d = videoDur(clips.get(i - 1));
            offset = (i == 1) ? d - fade : offset + d - fade;
            String label = (i == clips.size() - 1) ? "[vout]" : "[v" + i + "]";
            filter.append(String.format("%s[%d:v]xfade=transition=fade:duration=%.1f:offset=%.1f%s;",
                    prev, i, fade, offset, label));
            prev = label;
        }
        String f = filter.toString();
        if (f.endsWith(";")) f = f.substring(0, f.length() - 1);

        List<String> cmd = new ArrayList<>();
        cmd.add("-y");
        for (Path c : clips) { cmd.add("-i"); cmd.add(c.toString()); }
        cmd.addAll(List.of("-filter_complex", f, "-map", "[vout]",
                "-c:v", "libx264", "-preset", "fast", "-pix_fmt", "yuv420p", output.toString()));
        runFF(cmd.toArray(new String[0]));
    }

    // ═══════════════════════════════════════════════════════════
    //  자막 PNG 렌더링 (투명 배경 → overlay 필터용)
    //  hook/cta = 중앙 큰 텍스트 + 강조 색상
    //  나머지 = 하단 자막 스타일
    // ═══════════════════════════════════════════════════════════

    private Path renderSubtitlePng(int w, int h, String text, String purpose, String uid, int idx) {
        try {
            Path out = Path.of(videoDir, uid + "_sub" + idx + ".png");
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); // 투명 배경!
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (text == null || text.isBlank()) { g.dispose(); ImageIO.write(img, "png", out.toFile()); return out; }

            boolean isHookOrCta = "hook".equals(purpose) || "cta".equals(purpose);
            boolean isVert = h > w;

            if (isHookOrCta) {
                // ── 중앙 큰 텍스트 + 반투명 배경 ──
                int fontSize = isVert ? 72 : 60;
                g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                FontMetrics fm = g.getFontMetrics();

                List<String> lines = wrapText(text, fm, w - 160);
                int lineH = fm.getHeight() + 10;
                int totalH = lines.size() * lineH;
                int startY = h / 2 - totalH / 2;

                // 반투명 박스
                int pad = 40;
                int boxW = 0;
                for (String line : lines) boxW = Math.max(boxW, fm.stringWidth(line));
                g.setColor(new Color(0, 0, 0, 160));
                g.fill(new RoundRectangle2D.Double(
                        (w - boxW) / 2.0 - pad, startY - pad / 2.0,
                        boxW + pad * 2, totalH + pad, 24, 24));

                // 텍스트
                Color textColor = "hook".equals(purpose) ? new Color(255, 200, 60) : new Color(60, 220, 200);
                g.setColor(textColor);
                int y = startY + fm.getAscent();
                for (String line : lines) {
                    // 그림자
                    g.setColor(new Color(0, 0, 0, 200));
                    g.drawString(line, (w - fm.stringWidth(line)) / 2 + 3, y + 3);
                    // 메인 텍스트
                    g.setColor(textColor);
                    g.drawString(line, (w - fm.stringWidth(line)) / 2, y);
                    y += lineH;
                }
            } else {
                // ── 하단 자막 스타일 ──
                int fontSize = isVert ? 44 : 36;
                g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
                FontMetrics fm = g.getFontMetrics();

                List<String> lines = wrapText(text, fm, w - 120);
                int lineH = fm.getHeight() + 6;
                int totalH = lines.size() * lineH;
                int bottomY = h - (isVert ? 350 : 180);

                // 반투명 바 (하단 전체)
                g.setColor(new Color(0, 0, 0, 140));
                g.fillRect(0, bottomY - 20, w, totalH + 40);

                // 텍스트 (흰색)
                g.setColor(Color.WHITE);
                int y = bottomY + fm.getAscent();
                for (String line : lines) {
                    // 그림자
                    g.setColor(new Color(0, 0, 0, 180));
                    g.drawString(line, (w - fm.stringWidth(line)) / 2 + 2, y + 2);
                    // 메인
                    g.setColor(Color.WHITE);
                    g.drawString(line, (w - fm.stringWidth(line)) / 2, y);
                    y += lineH;
                }
            }

            g.dispose();
            ImageIO.write(img, "png", out.toFile());
            return out;
        } catch (Exception e) {
            log.warn("  자막 PNG 렌더링 실패: {}", e.getMessage());
            // 빈 투명 PNG 반환
            try {
                Path out = Path.of(videoDir, uid + "_sub" + idx + ".png");
                BufferedImage empty = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                ImageIO.write(empty, "png", out.toFile());
                return out;
            } catch (Exception ex) { return Path.of(videoDir, "nonexistent.png"); }
        }
    }

    private List<String> wrapText(String text, FontMetrics fm, int maxW) {
        List<String> lines = new ArrayList<>();
        if (fm.stringWidth(text) <= maxW) { lines.add(text); return lines; }
        StringBuilder cur = new StringBuilder();
        for (char c : text.toCharArray()) {
            cur.append(c);
            if (fm.stringWidth(cur.toString()) > maxW) {
                lines.add(cur.substring(0, cur.length() - 1));
                cur = new StringBuilder().append(c);
            }
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines;
    }

    // ═══════════════════════════════════════════════════════════
    //  TTS 내레이션 (macOS say → AIFF → 합치기)
    // ═══════════════════════════════════════════════════════════

    private Path generateTTS(List<AdCopyResponse.SceneEntry> scenes, String uid) {
        try {
            List<Path> parts = new ArrayList<>();
            for (int i = 0; i < scenes.size(); i++) {
                var sc = scenes.get(i);
                String txt = sc.getText();
                if (txt == null || txt.isBlank()) continue;

                Path p = Path.of(videoDir, uid + "_tts" + i + ".aiff");
                int rate = "hook".equals(sc.getPurpose()) ? 210 : "cta".equals(sc.getPurpose()) ? 170 : 190;
                runCmd(10, SAY, "-v", "Yuna", "-r", String.valueOf(rate), "-o", p.toString(), txt);
                if (Files.exists(p) && Files.size(p) > 100) parts.add(p);
            }
            if (parts.isEmpty()) return null;

            // 씬 간 0.3초 무음
            Path sil = Path.of(videoDir, uid + "_sil.aiff");
            runFF("-y", "-f", "lavfi", "-i", "anullsrc=r=22050:cl=mono",
                    "-t", "0.3", "-c:a", "pcm_s16le", sil.toString());

            Path list = Path.of(videoDir, uid + "_ttslist.txt");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                sb.append("file '").append(parts.get(i).toAbsolutePath()).append("'\n");
                if (i < parts.size() - 1) sb.append("file '").append(sil.toAbsolutePath()).append("'\n");
            }
            Files.writeString(list, sb.toString(), StandardCharsets.UTF_8);

            Path merged = Path.of(videoDir, uid + "_voice.aiff");
            runFF("-y", "-f", "concat", "-safe", "0", "-i", list.toString(), "-c", "copy", merged.toString());

            for (Path p : parts) safeDelete(p);
            safeDelete(sil); safeDelete(list);

            if (Files.exists(merged) && Files.size(merged) > 100) {
                log.info("  TTS 생성 OK ({}개 씬)", parts.size());
                return merged;
            }
            return null;
        } catch (Exception e) {
            log.warn("  TTS 실패: {}", e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  BGM 생성 (FFmpeg lavfi → 앰비언트 톤)
    // ═══════════════════════════════════════════════════════════

    private Path generateBGM(String uid, double duration) {
        try {
            Path bgm = Path.of(videoDir, uid + "_bgm.aac");
            // 부드러운 앰비언트 사인파 + 약간의 하모닉스 (광고 배경음 느낌)
            String filter = String.format(
                    "sine=f=220:d=%.1f[s1];sine=f=330:d=%.1f[s2];sine=f=440:d=%.1f[s3];" +
                    "[s1][s2][s3]amix=inputs=3:duration=longest,volume=0.3,afade=t=in:st=0:d=1,afade=t=out:st=%.1f:d=1",
                    duration, duration, duration, duration - 1
            );
            runFF("-y", "-f", "lavfi", "-i", filter, "-c:a", "aac", "-b:a", "64k", bgm.toString());

            if (Files.exists(bgm) && Files.size(bgm) > 100) {
                log.info("  BGM 생성 OK ({}s)", fmt(duration));
                return bgm;
            }
            return null;
        } catch (Exception e) {
            log.warn("  BGM 실패: {}", e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  썸네일
    // ═══════════════════════════════════════════════════════════

    private String generateThumbnail(AdCopyResponse adCopy, AdGenerateRequest req) throws IOException {
        String name = UUID.randomUUID() + "_thumb.png";
        Path out = Path.of(thumbnailDir, name);
        boolean vert = !req.getPlatform().name().contains("LONG");
        int w = vert ? 1080 : 1920, h = vert ? 1920 : 1080;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        String kw = adCopy.getScenes() != null && !adCopy.getScenes().isEmpty()
                ? adCopy.getScenes().get(0).getVideoSearchKeyword() : null;
        Path bg = Path.of(thumbnailDir, UUID.randomUUID() + "_bg.jpg");
        boolean hasBg = kw != null && stockService.downloadImage(kw, w, h, bg);
        if (hasBg) {
            try { g.drawImage(ImageIO.read(bg.toFile()), 0, 0, w, h, null); g.setColor(new Color(0, 0, 0, 130)); g.fillRect(0, 0, w, h); }
            catch (Exception e) { drawGradBg(g, w, h); }
        } else { drawGradBg(g, w, h); }
        safeDelete(bg);

        g.setColor(new Color(232, 89, 60, 25));
        g.fillOval(w / 2 - 250, h / 3 - 250, 500, 500);

        String text = adCopy.getThumbnailText() != null ? adCopy.getThumbnailText()
                : (adCopy.getHook() != null ? adCopy.getHook() : req.getProductName());
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, vert ? 76 : 64));
        drawWrapped(g, text, w, h / 2 - 40, g.getFontMetrics(), w - 120);
        g.setColor(new Color(220, 220, 220, 200));
        g.setFont(new Font("SansSerif", Font.PLAIN, vert ? 30 : 26));
        drawCentered(g, req.getProductName(), w, h / 2 + 80);

        g.setColor(new Color(232, 89, 60));
        g.fill(new RoundRectangle2D.Double(w / 2.0 - 140, h - 210, 280, 60, 30, 30));
        g.setColor(Color.WHITE); g.setFont(new Font("SansSerif", Font.BOLD, 20));
        drawCentered(g, "자세히 보기", w, h - 175);

        g.dispose();
        ImageIO.write(img, "png", out.toFile());
        return name;
    }

    // ═══════════════════════════════════════════════════════════
    //  유틸
    // ═══════════════════════════════════════════════════════════

    private String getFallbackKeyword(String purpose) {
        return switch (purpose != null ? purpose : "") {
            case "hook" -> "dramatic close up slow motion";
            case "problem" -> "person frustrated thinking";
            case "change" -> "unboxing product reveal";
            case "result" -> "happy surprised reaction";
            case "cta" -> "phone shopping online";
            default -> "aesthetic minimal";
        };
    }

    private double totalDuration(List<AdCopyResponse.SceneEntry> scenes) {
        return scenes.stream().mapToDouble(s -> Math.max(s.getDuration(), 2.0)).sum();
    }

    private void renderGradient(Path out, int w, int h, int idx) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Color[][] p = {{new Color(35,10,10),new Color(80,20,15)},{new Color(10,15,35),new Color(15,25,55)},
                {new Color(10,30,25),new Color(15,50,40)},{new Color(25,10,35),new Color(40,20,60)},{new Color(40,15,5),new Color(80,30,10)}};
        int pi = idx % p.length;
        g.setPaint(new GradientPaint(0, 0, p[pi][0], 0, h, p[pi][1]));
        g.fillRect(0, 0, w, h); g.dispose();
        ImageIO.write(img, "jpg", out.toFile());
    }

    private void drawGradBg(Graphics2D g, int w, int h) {
        g.setPaint(new GradientPaint(0, 0, new Color(20, 10, 10), w, h, new Color(10, 10, 30)));
        g.fillRect(0, 0, w, h);
    }

private void runFF(String... args) throws Exception {
        List<String> cmd = new ArrayList<>(); cmd.add(FFMPEG); cmd.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(cmd); pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String l; while ((l = br.readLine()) != null) out.append(l).append("\n");
        }
        if (!p.waitFor(180, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new RuntimeException("타임아웃"); }
        if (p.exitValue() != 0) {
            String tail = out.length() > 800 ? out.substring(out.length() - 800) : out.toString();
            log.error("FFmpeg 실패(exit={}):\n{}", p.exitValue(), tail);
            throw new RuntimeException("FFmpeg exit=" + p.exitValue());
        }
    }

    private void runCmd(int sec, String... c) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(c); pb.redirectErrorStream(true);
        Process p = pb.start(); p.getInputStream().readAllBytes(); p.waitFor(sec, TimeUnit.SECONDS);
    }

    private boolean cmdOk(String... c) {
        try { Process p = new ProcessBuilder(c).redirectErrorStream(true).start();
            p.getInputStream().readAllBytes(); p.waitFor(5,TimeUnit.SECONDS);
            return p.exitValue()==0; } catch(Exception e){return false;}
    }

    private double videoDur(Path v) {
        try { ProcessBuilder pb = new ProcessBuilder(FFPROBE,"-v","quiet","-show_entries","format=duration","-of","csv=p=0",v.toString());
            pb.redirectErrorStream(true); Process p=pb.start();
            String s=new String(p.getInputStream().readAllBytes()).trim(); p.waitFor(5,TimeUnit.SECONDS);
            return Double.parseDouble(s); } catch(Exception e){return 3.0;}
    }

    private String uuid8(){return UUID.randomUUID().toString().substring(0,8);}
    private String fmt(double d){return String.format("%.2f",d);}
    private void safeDelete(Path p){try{Files.deleteIfExists(p);}catch(Exception ignored){}}
    private void mkdirs(String d){try{Files.createDirectories(Path.of(d));}catch(Exception ignored){}}

    private void drawCentered(Graphics2D g, String text, int cw, int y) {
        if(text==null)return; FontMetrics fm=g.getFontMetrics(); g.drawString(text,(cw-fm.stringWidth(text))/2,y);
    }
    private void drawWrapped(Graphics2D g, String text, int cw, int centerY, FontMetrics fm, int maxW) {
        if(text==null)return;
        List<String> lines=new ArrayList<>(); StringBuilder cur=new StringBuilder();
        for(char c:text.toCharArray()){cur.append(c);if(fm.stringWidth(cur.toString())>maxW){lines.add(cur.substring(0,cur.length()-1));cur=new StringBuilder().append(c);}}
        if(!cur.isEmpty())lines.add(cur.toString());
        int totalH=lines.size()*fm.getHeight(); int y=centerY-totalH/2+fm.getAscent();
        for(String line:lines){g.drawString(line,(cw-fm.stringWidth(line))/2,y);y+=fm.getHeight();}
    }
    private String buildUploadText(AdCopyResponse a) {
        StringBuilder sb=new StringBuilder();
        if(a.getTitle()!=null)sb.append(a.getTitle()).append("\n\n");
        if(a.getDescription()!=null)sb.append(a.getDescription()).append("\n\n");
        if(a.getHashtags()!=null)sb.append(String.join(" ",a.getHashtags()));
        return sb.toString();
    }
}
