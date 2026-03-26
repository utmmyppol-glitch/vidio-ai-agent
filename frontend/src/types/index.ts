// ═══════════════════════════════════════════
// 현재 백엔드 정확 매칭 (2026-03-26 최신)
// ═══════════════════════════════════════════

export type Platform = 'YOUTUBE_SHORTS' | 'INSTAGRAM_REELS' | 'TIKTOK' | 'YOUTUBE_LONG';
export type Style = 'EMOTIONAL' | 'PROVOCATIVE' | 'INFORMATIVE' | 'HUMOROUS' | 'LUXURY' | 'MINIMAL';
export type ProjectStatus =
  | 'PENDING'
  | 'TREND_ANALYZING'
  | 'COPY_GENERATING'
  | 'SCRIPT_GENERATING'
  | 'VIDEO_GENERATING'
  | 'THUMBNAIL_GENERATING'
  | 'COMPLETED'
  | 'FAILED';

// AdGenerateRequest.java 매칭
export interface AdGenerateRequest {
  productName: string;
  productDescription?: string;
  targetAudience: string;
  platform: Platform;
  adStyle: Style;
  additionalRequest?: string;
}

// AdProjectResponse.java 매칭 (플랫 문자열)
export interface AdProjectResponse {
  id: number;
  productName: string;
  productDescription: string | null;
  targetAudience: string;
  platform: Platform;
  adStyle: Style;
  status: ProjectStatus;
  progressPercent: number | null;
  trendAnalysis: string | null;   // JSON string
  adCopy: string | null;          // JSON string
  script: string | null;
  hashtags: string | null;
  hookText: string | null;
  subtitles: string | null;       // JSON string
  videoUrl: string | null;
  thumbnailUrl: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

// ApiResponse.java 매칭
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// AdCopyResponse parsed types
export interface AdCopyParsed {
  title?: string;
  script?: string;
  hashtags?: string[];
  subtitles?: { startTime: number; endTime: number; text: string }[];
  thumbnailText?: string;
  description?: string;
  scenes?: Scene[];
}

export interface Scene {
  sceneNumber: number;
  purpose: 'hook' | 'problem' | 'change' | 'result' | 'cta';
  text: string;
  narration?: string;
  searchKeyword?: string;
  highlight?: string;
  mood?: 'warm' | 'cool' | 'dramatic' | 'bright' | 'dark' | 'neon';
  duration: number;
  // 하위 호환
  type?: string;
  visualDesc?: string;
  startTime?: number;
  endTime?: number;
}

export const SCENE_TYPE_LABELS: Record<string, string> = {
  hook: '🎣 HOOK',
  problem: '😤 문제',
  change: '✨ 변화',
  result: '🔥 결과',
  cta: '👉 CTA',
  // 하위 호환
  HOOK: '🎣 HOOK',
  EMPATHY: '😤 공감',
  SOLUTION: '✨ 해결',
  FEATURE: '💡 특장점',
  CTA: '👉 CTA',
};

export const MOOD_LABELS: Record<string, string> = {
  warm: '따뜻한',
  cool: '시원한',
  dramatic: '드라마틱',
  bright: '밝은',
  dark: '어두운',
  neon: '네온',
};

// UI Labels
export const PLATFORM_LABELS: Record<Platform, string> = {
  YOUTUBE_SHORTS: '유튜브 쇼츠',
  INSTAGRAM_REELS: '인스타 릴스',
  TIKTOK: '틱톡',
  YOUTUBE_LONG: '유튜브 일반',
};

export const STYLE_LABELS: Record<Style, string> = {
  EMOTIONAL: '감성적',
  PROVOCATIVE: '자극적',
  INFORMATIVE: '정보형',
  HUMOROUS: '유머',
  LUXURY: '럭셔리',
  MINIMAL: '미니멀',
};

export const STATUS_LABELS: Record<ProjectStatus, string> = {
  PENDING: '대기중',
  TREND_ANALYZING: '트렌드 분석중',
  COPY_GENERATING: '카피 생성중',
  SCRIPT_GENERATING: '스크립트 생성중',
  VIDEO_GENERATING: '영상 생성중',
  THUMBNAIL_GENERATING: '썸네일 생성중',
  COMPLETED: '완료',
  FAILED: '실패',
};
