import { useEffect, useRef, useCallback } from 'react';
import { getProject } from '../api/client';
import { useAppStore } from '../store/useAppStore';

export function useProjectPolling(projectId: number | null) {
  const { setCurrentProject } = useAppStore();
  const intervalRef = useRef<number | null>(null);
  const stoppedRef = useRef(false);

  const poll = useCallback(async () => {
    if (!projectId || stoppedRef.current) return;
    try {
      const res = await getProject(projectId);
      if (res.success && res.data) {
        setCurrentProject(res.data);
        if (res.data.status === 'COMPLETED' || res.data.status === 'FAILED') {
          stoppedRef.current = true;
          if (intervalRef.current) clearInterval(intervalRef.current);
        }
      }
    } catch (err) {
      console.error('폴링 에러:', err);
    }
  }, [projectId, setCurrentProject]);

  useEffect(() => {
    if (!projectId) return;
    stoppedRef.current = false;
    poll();
    intervalRef.current = window.setInterval(poll, 2000);
    return () => { if (intervalRef.current) clearInterval(intervalRef.current); };
  }, [projectId, poll]);
}
