import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiArrowLeft, FiPlus, FiClock } from 'react-icons/fi';
import { getAllProjects } from '../api/client';
import type { AdProjectResponse } from '../types';
import { PLATFORM_LABELS, STYLE_LABELS, STATUS_LABELS } from '../types';

export default function HistoryPage() {
  const navigate = useNavigate();
  const [projects, setProjects] = useState<AdProjectResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAllProjects()
      .then(res => { if (res.success) setProjects(res.data); })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  const statusColor = (s: string) =>
    s === 'COMPLETED' ? '#2DB89A' : s === 'FAILED' ? '#E84A4A' : '#E8A830';

  return (
    <div className="min-h-screen" style={{ background: 'var(--c-bg)' }}>
      <header className="px-6 py-4 flex items-center gap-3" style={{ borderBottom: '1px solid var(--c-border)' }}>
        <button onClick={() => navigate('/')} className="p-2 rounded-lg" style={{ background: 'var(--c-card)' }}>
          <FiArrowLeft size={16} color="var(--c-text-muted)" />
        </button>
        <h1 className="text-sm font-semibold flex-1">프로젝트 히스토리</h1>
        <button onClick={() => navigate('/')}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium"
                style={{ background: '#E8593C', color: '#fff' }}>
          <FiPlus size={14} /> 새 프로젝트
        </button>
      </header>

      <main className="max-w-2xl mx-auto px-6 py-6">
        {loading ? (
          <div className="space-y-3">
            {[1, 2, 3].map(i => (
              <div key={i} className="rounded-xl p-4 animate-shimmer"
                   style={{ background: 'var(--c-card)', border: '1px solid var(--c-border)' }}>
                <div className="h-4 rounded w-1/3 mb-2" style={{ background: 'var(--c-border)' }} />
                <div className="h-3 rounded w-2/3" style={{ background: 'var(--c-border)' }} />
              </div>
            ))}
          </div>
        ) : projects.length === 0 ? (
          <div className="text-center py-20">
            <FiClock size={40} className="mx-auto mb-3" style={{ color: 'var(--c-text-dim)' }} />
            <p style={{ color: 'var(--c-text-dim)' }}>아직 프로젝트가 없어요</p>
          </div>
        ) : (
          <div className="space-y-3">
            {projects.map((p, i) => (
              <motion.button key={p.id} onClick={() => navigate(`/project/${p.id}`)}
                             className="w-full text-left rounded-xl p-4"
                             style={{ background: 'var(--c-card)', border: '1px solid var(--c-border)' }}
                             initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }}
                             transition={{ delay: i * 0.05 }}>
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h3 className="text-sm font-semibold truncate">{p.productName}</h3>
                    <div className="flex gap-2 text-xs mt-1" style={{ color: 'var(--c-text-dim)' }}>
                      <span>{PLATFORM_LABELS[p.platform]}</span>·<span>{STYLE_LABELS[p.adStyle]}</span>·<span>{p.targetAudience}</span>
                    </div>
                  </div>
                  <span className="text-xs font-medium px-2 py-1 rounded-md shrink-0"
                        style={{ background: `${statusColor(p.status)}15`, color: statusColor(p.status) }}>
                    {STATUS_LABELS[p.status]}
                  </span>
                </div>
                {p.progressPercent != null && p.status !== 'COMPLETED' && p.status !== 'FAILED' && (
                  <div className="mt-3 h-1 rounded-full overflow-hidden" style={{ background: 'var(--c-border)' }}>
                    <div className="h-full rounded-full" style={{ width: `${p.progressPercent}%`, background: '#E8593C' }} />
                  </div>
                )}
                {p.createdAt && (
                  <div className="text-[11px] mt-2" style={{ color: 'var(--c-text-dim)' }}>
                    {new Date(p.createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                  </div>
                )}
              </motion.button>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
