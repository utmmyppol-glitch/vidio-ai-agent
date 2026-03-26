import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiArrowLeft } from 'react-icons/fi';
import { useProjectPolling } from '../hooks/useProjectPolling';
import { useAppStore } from '../store/useAppStore';
import PipelineProgress from '../components/PipelineProgress';
import ResultPanel from '../components/ResultPanel';
import { PLATFORM_LABELS, STYLE_LABELS } from '../types';

export default function ProjectPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { currentProject, setCurrentProject } = useAppStore();
  const projectId = id ? parseInt(id) : null;

  useProjectPolling(projectId);

  if (!currentProject) {
    return (
      <div className="min-h-screen flex items-center justify-center" style={{ background: 'var(--c-bg)' }}>
        <div className="text-center">
          <div className="w-10 h-10 border-2 border-t-transparent rounded-full animate-spin mx-auto mb-3"
               style={{ borderColor: '#E8593C', borderTopColor: 'transparent' }} />
          <p className="text-sm" style={{ color: 'var(--c-text-muted)' }}>프로젝트 로딩중...</p>
        </div>
      </div>
    );
  }

  const p = currentProject;
  const isProcessing = p.status !== 'COMPLETED' && p.status !== 'FAILED';

  return (
    <div className="min-h-screen" style={{ background: 'var(--c-bg)' }}>
      <header className="px-6 py-4 flex items-center gap-3 sticky top-0 z-10"
              style={{ background: 'var(--c-bg)', borderBottom: '1px solid var(--c-border)' }}>
        <button onClick={() => navigate('/')} className="p-2 rounded-lg" style={{ background: 'var(--c-card)' }}>
          <FiArrowLeft size={16} color="var(--c-text-muted)" />
        </button>
        <div className="flex-1 min-w-0">
          <h1 className="text-sm font-semibold truncate">{p.productName}</h1>
          <div className="flex gap-2 text-xs mt-0.5" style={{ color: 'var(--c-text-dim)' }}>
            <span>{PLATFORM_LABELS[p.platform]}</span>
            <span>·</span>
            <span>{STYLE_LABELS[p.adStyle]}</span>
            <span>·</span>
            <span>{p.targetAudience}</span>
          </div>
        </div>
        {isProcessing && (
          <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium"
               style={{ background: 'rgba(232,89,60,0.1)', color: '#E8593C' }}>
            <div className="w-1.5 h-1.5 rounded-full animate-pulse" style={{ background: '#E8593C' }} /> 진행중
          </div>
        )}
      </header>

      <main className="max-w-2xl mx-auto px-6 py-6 space-y-6">
        <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                    className="rounded-xl p-5"
                    style={{ background: 'var(--c-surface)', border: '1px solid var(--c-border)' }}>
          <PipelineProgress currentStatus={p.status} />
        </motion.div>

        {p.status === 'FAILED' && p.errorMessage && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}
                      className="rounded-xl p-4 text-sm"
                      style={{ background: 'rgba(232,74,74,0.08)', border: '1px solid rgba(232,74,74,0.2)', color: '#E84A4A' }}>
            <strong>오류:</strong> {p.errorMessage}
          </motion.div>
        )}

        {(p.trendAnalysis || p.adCopy || p.script || p.videoUrl) && (
          <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.1 }}>
            <ResultPanel project={p} onUpdate={setCurrentProject} />
          </motion.div>
        )}

        {isProcessing && !p.trendAnalysis && (
          <div className="space-y-3">
            {[1, 2, 3].map(i => (
              <div key={i} className="rounded-xl p-5 animate-shimmer"
                   style={{ background: 'var(--c-card)', border: '1px solid var(--c-border)' }}>
                <div className="h-4 rounded w-1/4 mb-3" style={{ background: 'var(--c-border)' }} />
                <div className="space-y-2">
                  <div className="h-3 rounded w-full" style={{ background: 'var(--c-border)' }} />
                  <div className="h-3 rounded w-3/4" style={{ background: 'var(--c-border)' }} />
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
