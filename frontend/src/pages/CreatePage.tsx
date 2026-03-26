import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiZap, FiArrowRight } from 'react-icons/fi';
import { createProject } from '../api/client';
import type { AdGenerateRequest, Platform, Style } from '../types';
import toast from 'react-hot-toast';

const PLATFORMS: { value: Platform; label: string; emoji: string }[] = [
  { value: 'YOUTUBE_SHORTS', label: '유튜브 쇼츠', emoji: '📱' },
  { value: 'INSTAGRAM_REELS', label: '인스타 릴스', emoji: '📸' },
  { value: 'TIKTOK', label: '틱톡', emoji: '🎵' },
  { value: 'YOUTUBE_LONG', label: '유튜브 일반', emoji: '🎬' },
];

const STYLES: { value: Style; label: string; desc: string }[] = [
  { value: 'EMOTIONAL', label: '감성적', desc: '따뜻한 스토리텔링' },
  { value: 'PROVOCATIVE', label: '자극적', desc: '강렬한 임팩트' },
  { value: 'INFORMATIVE', label: '정보형', desc: '팩트 기반 전달' },
  { value: 'HUMOROUS', label: '유머', desc: '바이럴 유머' },
  { value: 'LUXURY', label: '럭셔리', desc: '프리미엄 무드' },
  { value: 'MINIMAL', label: '미니멀', desc: '깔끔한 세련미' },
];

export default function CreatePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState<AdGenerateRequest>({
    productName: '',
    productDescription: '',
    targetAudience: '',
    platform: 'YOUTUBE_SHORTS',
    adStyle: 'EMOTIONAL',
    additionalRequest: '',
  });

  const update = (key: keyof AdGenerateRequest, value: string) =>
    setForm(prev => ({ ...prev, [key]: value }));

  const handleSubmit = async () => {
    if (!form.productName.trim()) return toast.error('상품명을 입력해주세요');
    if (!form.targetAudience.trim()) return toast.error('타겟 고객을 입력해주세요');

    setLoading(true);
    try {
      const res = await createProject(form);
      if (res.success && res.data) {
        toast.success('파이프라인 시작!');
        navigate(`/project/${res.data.id}`);
      } else {
        toast.error(res.message || '생성 실패');
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || '서버 연결 실패');
    }
    setLoading(false);
  };

  const inputCls = "w-full px-4 py-3 rounded-xl text-sm outline-none transition-colors";
  const inputStyle = { background: 'var(--c-card)', border: '1px solid var(--c-border)', color: 'var(--c-text)' };

  return (
    <div className="min-h-screen flex flex-col" style={{ background: 'var(--c-bg)' }}>
      <header className="px-6 py-4 flex items-center justify-between"
              style={{ borderBottom: '1px solid var(--c-border)' }}>
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg flex items-center justify-center"
               style={{ background: 'var(--c-accent-soft)' }}>
            <FiZap size={16} color="#E8593C" />
          </div>
          <span className="font-semibold text-sm tracking-wide">VIDIO AI</span>
        </div>
        <button onClick={() => navigate('/history')}
                className="text-sm px-3 py-1.5 rounded-lg"
                style={{ color: 'var(--c-text-muted)', background: 'var(--c-card)' }}>
          히스토리
        </button>
      </header>

      <main className="flex-1 flex items-center justify-center p-6">
        <motion.div className="w-full max-w-lg"
                    initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}>
          <div className="text-center mb-8">
            <h1 className="text-3xl font-light mb-2" style={{ letterSpacing: '-0.02em' }}>
              광고 영상, <span className="font-semibold" style={{ color: '#E8593C' }}>AI가 만들어요</span>
            </h1>
            <p className="text-sm" style={{ color: 'var(--c-text-muted)' }}>
              상품 정보만 입력하면 트렌드 분석부터 영상까지 자동 생성
            </p>
          </div>

          <div className="space-y-4">
            <Field label="상품/서비스명 *">
              <input type="text" value={form.productName}
                     onChange={e => update('productName', e.target.value)}
                     placeholder="예: 닥터자르트 시카페어 크림"
                     className={inputCls} style={inputStyle} />
            </Field>

            <Field label="상품 설명">
              <textarea value={form.productDescription}
                        onChange={e => update('productDescription', e.target.value)}
                        placeholder="상품의 핵심 특징을 간단히 적어주세요"
                        rows={2} className={inputCls + " resize-none"} style={inputStyle} />
            </Field>

            <Field label="타겟 고객 *">
              <input type="text" value={form.targetAudience}
                     onChange={e => update('targetAudience', e.target.value)}
                     placeholder="예: 피부 고민 있는 20대 여성"
                     className={inputCls} style={inputStyle} />
            </Field>

            <Field label="플랫폼">
              <div className="grid grid-cols-2 gap-2">
                {PLATFORMS.map(p => (
                  <button key={p.value} onClick={() => update('platform', p.value)}
                          className="px-3 py-2.5 rounded-xl text-sm text-left transition-all"
                          style={{
                            background: form.platform === p.value ? 'var(--c-accent-soft)' : 'var(--c-card)',
                            border: `1.5px solid ${form.platform === p.value ? '#E8593C' : 'var(--c-border)'}`,
                            color: form.platform === p.value ? '#E8593C' : 'var(--c-text-muted)',
                          }}>
                    {p.emoji} {p.label}
                  </button>
                ))}
              </div>
            </Field>

            <Field label="광고 스타일">
              <div className="grid grid-cols-3 gap-2">
                {STYLES.map(s => (
                  <button key={s.value} onClick={() => update('adStyle', s.value)}
                          className="px-3 py-2 rounded-xl text-center transition-all"
                          style={{
                            background: form.adStyle === s.value ? 'var(--c-purple-soft)' : 'var(--c-card)',
                            border: `1.5px solid ${form.adStyle === s.value ? '#8B6AE6' : 'var(--c-border)'}`,
                            color: form.adStyle === s.value ? '#8B6AE6' : 'var(--c-text-muted)',
                          }}>
                    <div className="text-sm font-medium">{s.label}</div>
                    <div className="text-[10px] mt-0.5 opacity-60">{s.desc}</div>
                  </button>
                ))}
              </div>
            </Field>

            <Field label="추가 요청사항">
              <input type="text" value={form.additionalRequest}
                     onChange={e => update('additionalRequest', e.target.value)}
                     placeholder="예: 초반에 충격적인 사실로 시작해주세요"
                     className={inputCls} style={inputStyle} />
            </Field>

            <motion.button onClick={handleSubmit} disabled={loading}
                           className="w-full py-3.5 rounded-xl text-sm font-semibold flex items-center justify-center gap-2"
                           style={{
                             background: loading ? 'var(--c-card)' : '#E8593C',
                             color: loading ? 'var(--c-text-dim)' : '#fff',
                             cursor: loading ? 'not-allowed' : 'pointer',
                           }}
                           whileHover={!loading ? { scale: 1.01 } : {}}
                           whileTap={!loading ? { scale: 0.98 } : {}}>
              {loading ? (
                <><div className="w-4 h-4 border-2 border-current border-t-transparent rounded-full animate-spin" /> 생성 중...</>
              ) : (
                <>AI 파이프라인 시작 <FiArrowRight size={16} /></>
              )}
            </motion.button>
          </div>
        </motion.div>
      </main>
    </div>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <label className="block text-xs font-medium mb-1.5" style={{ color: 'var(--c-text-muted)' }}>{label}</label>
      {children}
    </div>
  );
}
