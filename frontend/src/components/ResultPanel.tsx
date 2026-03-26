import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { FiCopy, FiCheck, FiDownload, FiPlay, FiRefreshCw } from 'react-icons/fi';
import type { AdProjectResponse } from '../types';
import { getVideoFileUrl, getThumbnailFileUrl, retryStep } from '../api/client';
import toast from 'react-hot-toast';

type TabId = 'trend' | 'copy' | 'script' | 'video';
const TABS: { id: TabId; label: string; color: string }[] = [
  { id: 'trend', label: '트렌드 분석', color: '#E8A830' },
  { id: 'copy', label: '씬 스크립트', color: '#8B6AE6' },
  { id: 'script', label: '자막 & 해시태그', color: '#2DB89A' },
  { id: 'video', label: '영상 & 썸네일', color: '#E8593C' },
];

const PURPOSE_LABELS: Record<string, { label: string; color: string }> = {
  hook: { label: 'HOOK', color: '#E8593C' },
  problem: { label: '문제제기', color: '#4A90E2' },
  change: { label: '변화', color: '#2DB89A' },
  result: { label: '결과', color: '#8B6AE6' },
  cta: { label: 'CTA', color: '#E8A830' },
};

interface Props {
  project: AdProjectResponse;
  onUpdate?: (p: AdProjectResponse) => void;
}

export default function ResultPanel({ project, onUpdate }: Props) {
  const [activeTab, setActiveTab] = useState<TabId>('trend');
  const [copiedField, setCopiedField] = useState<string | null>(null);
  const [retrying, setRetrying] = useState(false);

  const copy = (text: string | null | undefined, field: string) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedField(field);
    toast.success('복사 완료!');
    setTimeout(() => setCopiedField(null), 2000);
  };

  const handleRetry = async (step: string) => {
    setRetrying(true);
    try {
      const res = await retryStep(project.id, step);
      if (res.success && onUpdate) onUpdate(res.data);
      toast.success('재실행 시작!');
    } catch { toast.error('재실행 실패'); }
    setRetrying(false);
  };

  const CopyBtn = ({ text, field }: { text: string | null | undefined; field: string }) => (
    <button onClick={() => copy(text, field)}
            className="p-1.5 rounded-md hover:opacity-80" style={{ background: 'var(--c-border)' }}>
      {copiedField === field ? <FiCheck size={14} color="#2DB89A" /> : <FiCopy size={14} color="var(--c-text-muted)" />}
    </button>
  );

  const parsedTrend = safeJson(project.trendAnalysis);
  const parsedCopy = safeJson(project.adCopy);

  const renderContent = () => {
    switch (activeTab) {
      case 'trend':
        if (!project.trendAnalysis) return <Skeleton />;
        if (!parsedTrend) return <Card title="트렌드 분석"><Pre text={project.trendAnalysis} /></Card>;
        return (
          <div className="space-y-4">
            {parsedTrend.trendKeywords && (
              <Card title="트렌드 키워드">
                <div className="flex flex-wrap gap-2">
                  {parsedTrend.trendKeywords.map((kw: string, i: number) => (
                    <span key={i} className="px-3 py-1.5 rounded-lg text-xs font-medium"
                          style={{ background: 'rgba(232,168,48,0.1)', color: '#E8A830' }}>{kw}</span>
                  ))}
                </div>
              </Card>
            )}
            {parsedTrend.competitorAnalysis && (
              <Card title="경쟁 분석" right={<CopyBtn text={parsedTrend.competitorAnalysis} field="comp" />}>
                <Pre text={parsedTrend.competitorAnalysis} />
              </Card>
            )}
            {parsedTrend.viralPoints && (
              <Card title="바이럴 포인트">
                <ul className="space-y-2">
                  {parsedTrend.viralPoints.map((v: string, i: number) => (
                    <li key={i} className="flex gap-2 text-sm" style={{ color: 'var(--c-text)' }}>
                      <span style={{ color: '#E8593C' }}>●</span> {v}
                    </li>
                  ))}
                </ul>
              </Card>
            )}
            {parsedTrend.hookSuggestion && (
              <Card title="Hook 제안" right={<CopyBtn text={parsedTrend.hookSuggestion} field="hook" />}>
                <p className="text-lg font-bold" style={{ color: '#E8593C' }}>"{parsedTrend.hookSuggestion}"</p>
              </Card>
            )}
          </div>
        );

      case 'copy':
        if (!parsedCopy?.scenes && !project.adCopy) return <Skeleton />;
        if (parsedCopy?.scenes) {
          return (
            <div className="space-y-3">
              {/* Hook */}
              {parsedCopy.hook && (
                <Card title="Hook 텍스트" right={<CopyBtn text={parsedCopy.hook} field="hookMain" />}>
                  <p className="text-xl font-bold" style={{ color: '#E8593C' }}>"{parsedCopy.hook}"</p>
                </Card>
              )}

              {/* 씬 카드들 */}
              {parsedCopy.scenes.map((scene: any, i: number) => {
                const purpose = PURPOSE_LABELS[scene.purpose] || { label: 'SCENE', color: '#888' };
                return (
                  <motion.div key={i} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
                              transition={{ delay: i * 0.05 }}
                              className="rounded-xl overflow-hidden"
                              style={{ background: 'var(--c-card)', border: '1px solid var(--c-border)' }}>
                    <div className="px-4 py-3 flex items-center justify-between"
                         style={{ borderBottom: '1px solid var(--c-border)' }}>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold px-2 py-0.5 rounded"
                              style={{ background: `${purpose.color}20`, color: purpose.color }}>
                          {purpose.label}
                        </span>
                        <span className="text-xs" style={{ color: 'var(--c-text-dim)' }}>
                          Scene {scene.sceneNumber} · {scene.duration}s
                        </span>
                      </div>
                      <CopyBtn text={scene.text} field={`scene-${i}`} />
                    </div>
                    <div className="px-4 py-4">
                      <p className="text-base font-semibold" style={{ color: 'var(--c-text)' }}>{scene.text}</p>
                      {scene.videoSearchKeyword && (
                        <p className="text-xs mt-2" style={{ color: 'var(--c-text-dim)' }}>
                          🔍 영상 키워드: <span style={{ color: 'var(--c-text-muted)' }}>{scene.videoSearchKeyword}</span>
                        </p>
                      )}
                    </div>
                  </motion.div>
                );
              })}
            </div>
          );
        }
        return <Card title="광고 카피"><Pre text={project.adCopy} /></Card>;

      case 'script':
        return (
          <div className="space-y-4">
            {project.hookText && (
              <Card title="초반 3초 Hook" right={<CopyBtn text={project.hookText} field="hookT" />}>
                <p className="text-lg font-bold" style={{ color: '#E8593C' }}>"{project.hookText}"</p>
              </Card>
            )}
            {project.script && (
              <Card title="전체 스크립트" right={<CopyBtn text={project.script} field="script" />}>
                <Pre text={project.script} />
              </Card>
            )}
            {project.hashtags && (
              <Card title="해시태그" right={<CopyBtn text={project.hashtags} field="hash" />}>
                <div className="flex flex-wrap gap-1.5">
                  {project.hashtags.split(' ').filter(Boolean).map((h, i) => (
                    <span key={i} className="text-xs px-2.5 py-1 rounded-lg font-medium"
                          style={{ background: 'rgba(139,106,230,0.1)', color: '#8B6AE6' }}>{h}</span>
                  ))}
                </div>
              </Card>
            )}
            {parsedCopy?.title && (
              <Card title="영상 제목" right={<CopyBtn text={parsedCopy.title} field="title" />}>
                <p className="text-base font-semibold" style={{ color: 'var(--c-text)' }}>{parsedCopy.title}</p>
              </Card>
            )}
            {parsedCopy?.description && (
              <Card title="업로드 설명글" right={<CopyBtn text={parsedCopy.description} field="desc" />}>
                <Pre text={parsedCopy.description} />
              </Card>
            )}
          </div>
        );

      case 'video':
        return (
          <div className="space-y-4">
            {project.videoUrl ? (
              <Card title="생성된 영상">
                <div className="rounded-lg overflow-hidden bg-black">
                  <video src={getVideoFileUrl(project.videoUrl)} controls
                         className="w-full max-h-[500px]" style={{ objectFit: 'contain' }} />
                </div>
                <div className="mt-3 flex gap-2">
                  <a href={getVideoFileUrl(project.videoUrl)} download
                     className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium"
                     style={{ background: '#E8593C', color: '#fff' }}>
                    <FiDownload size={14} /> 다운로드
                  </a>
                  <button onClick={() => handleRetry('VIDEO_GENERATING')}
                          className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm"
                          style={{ background: 'var(--c-border)', color: 'var(--c-text-muted)' }}>
                    <FiRefreshCw size={14} className={retrying ? 'animate-spin' : ''} /> 재생성
                  </button>
                </div>
              </Card>
            ) : (
              <Card title="영상">
                <div className="text-center py-12">
                  <FiPlay size={40} className="mx-auto mb-3" style={{ color: 'var(--c-text-dim)' }} />
                  <p style={{ color: 'var(--c-text-dim)' }}>
                    {project.status === 'COMPLETED' ? 'FFmpeg 영상 생성 실패' : '영상 생성 대기중...'}
                  </p>
                </div>
              </Card>
            )}

            {project.thumbnailUrl && (
              <Card title="썸네일">
                <div className="rounded-lg overflow-hidden">
                  <img src={getThumbnailFileUrl(project.thumbnailUrl)} alt="thumbnail"
                       className="w-full max-h-[400px] object-contain bg-black" />
                </div>
                <div className="mt-3">
                  <a href={getThumbnailFileUrl(project.thumbnailUrl)} download
                     className="flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium w-fit"
                     style={{ background: '#8B6AE6', color: '#fff' }}>
                    <FiDownload size={14} /> 썸네일 다운로드
                  </a>
                </div>
              </Card>
            )}
          </div>
        );
    }
  };

  return (
    <div>
      <div className="flex gap-1 p-1 rounded-xl mb-4" style={{ background: 'var(--c-surface)' }}>
        {TABS.map(tab => (
          <button key={tab.id} onClick={() => setActiveTab(tab.id)}
                  className="flex-1 py-2.5 px-3 rounded-lg text-sm font-medium transition-all"
                  style={{
                    background: activeTab === tab.id ? 'var(--c-card)' : 'transparent',
                    color: activeTab === tab.id ? tab.color : 'var(--c-text-dim)',
                    border: activeTab === tab.id ? '1px solid var(--c-border)' : '1px solid transparent',
                  }}>
            {tab.label}
          </button>
        ))}
      </div>
      <AnimatePresence mode="wait">
        <motion.div key={activeTab} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -8 }} transition={{ duration: 0.2 }}>
          {renderContent()}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}

function safeJson(s: string | null) {
  try { return s ? JSON.parse(s) : null; } catch { return null; }
}

function Card({ title, right, children }: { title: string; right?: React.ReactNode; children: React.ReactNode }) {
  return (
    <div className="rounded-xl" style={{ background: 'var(--c-card)', border: '1px solid var(--c-border)' }}>
      <div className="flex items-center justify-between px-4 py-3" style={{ borderBottom: '1px solid var(--c-border)' }}>
        <h3 className="text-sm font-semibold" style={{ color: 'var(--c-text)' }}>{title}</h3>
        {right}
      </div>
      <div className="p-4">{children}</div>
    </div>
  );
}

function Pre({ text }: { text: string | null }) {
  if (!text) return null;
  return <p className="text-sm leading-relaxed whitespace-pre-wrap" style={{ color: 'var(--c-text)', maxHeight: 400, overflowY: 'auto' }}>{text}</p>;
}

function Skeleton() {
  return (
    <div className="space-y-3">
      {[1, 2, 3].map(i => (
        <div key={i} className="rounded-xl p-5 animate-shimmer" style={{ background: 'var(--c-card)', border: '1px solid var(--c-border)' }}>
          <div className="h-4 rounded w-1/3 mb-3" style={{ background: 'var(--c-border)' }} />
          <div className="space-y-2"><div className="h-3 rounded w-full" style={{ background: 'var(--c-border)' }} /><div className="h-3 rounded w-4/5" style={{ background: 'var(--c-border)' }} /></div>
        </div>
      ))}
    </div>
  );
}
