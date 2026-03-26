import { motion } from 'framer-motion';
import type { ProjectStatus } from '../types';
import { STATUS_LABELS } from '../types';

const STEPS: { status: ProjectStatus; label: string; color: string }[] = [
  { status: 'TREND_ANALYZING', label: '트렌드 분석', color: '#E8A830' },
  { status: 'COPY_GENERATING', label: '카피 생성', color: '#8B6AE6' },
  { status: 'SCRIPT_GENERATING', label: '스크립트', color: '#2DB89A' },
  { status: 'VIDEO_GENERATING', label: '영상 생성', color: '#E8593C' },
  { status: 'THUMBNAIL_GENERATING', label: '썸네일', color: '#4A9EF2' },
  { status: 'COMPLETED', label: '완료', color: '#2DB89A' },
];

const ORDER: ProjectStatus[] = [
  'PENDING', 'TREND_ANALYZING', 'COPY_GENERATING', 'SCRIPT_GENERATING',
  'VIDEO_GENERATING', 'THUMBNAIL_GENERATING', 'COMPLETED',
];

interface Props { currentStatus: ProjectStatus; progress?: number | null; }

export default function PipelineProgress({ currentStatus, progress }: Props) {
  const currentIndex = ORDER.indexOf(currentStatus);
  const pct = progress ?? Math.round((currentIndex / (ORDER.length - 1)) * 100);
  const isFailed = currentStatus === 'FAILED';

  return (
    <div className="w-full">
      <div className="relative h-1.5 rounded-full overflow-hidden mb-6" style={{ background: 'var(--c-border)' }}>
        <motion.div className="absolute inset-y-0 left-0 rounded-full"
                    style={{ background: isFailed ? '#E84A4A' : '#E8593C' }}
                    initial={{ width: 0 }} animate={{ width: `${pct}%` }}
                    transition={{ duration: 0.6, ease: 'easeOut' }} />
      </div>

      <div className="flex justify-between gap-1">
        {STEPS.map((step, i) => {
          const stepIndex = ORDER.indexOf(step.status);
          const isActive = currentIndex >= stepIndex;
          const isCurrent = currentStatus === step.status;
          return (
            <div key={step.status} className="flex flex-col items-center gap-2 flex-1">
              <motion.div className="w-9 h-9 rounded-full flex items-center justify-center text-xs font-semibold"
                          style={{
                            background: isActive ? step.color : 'var(--c-card)',
                            color: isActive ? '#fff' : 'var(--c-text-dim)',
                            border: `1.5px solid ${isActive ? step.color : 'var(--c-border)'}`,
                          }}
                          animate={isCurrent ? { scale: [1, 1.12, 1] } : {}}
                          transition={{ repeat: Infinity, duration: 1.5 }}>
                {isActive && currentIndex > stepIndex ? '✓' : i + 1}
              </motion.div>
              <span className="text-[11px] text-center leading-tight"
                    style={{ color: isActive ? 'var(--c-text)' : 'var(--c-text-dim)' }}>
                {step.label}
              </span>
            </div>
          );
        })}
      </div>

      <div className="text-center mt-4">
        <span className="text-sm font-medium" style={{ color: isFailed ? '#E84A4A' : '#E8593C' }}>
          {STATUS_LABELS[currentStatus]}
        </span>
        <span className="text-sm ml-2" style={{ color: 'var(--c-text-dim)' }}>{pct}%</span>
      </div>
    </div>
  );
}
