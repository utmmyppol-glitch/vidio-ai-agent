import { useEffect, useRef, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAppStore } from '../store/useAppStore';
import type { ProjectStatus } from '../types';

interface ProgressMessage {
  projectId: number;
  status: ProjectStatus;
  progressPercent: number;
  message: string;
}

export function useWebSocket(projectId: number | null) {
  const { currentProject, setCurrentProject } = useAppStore();
  const clientRef = useRef<Client | null>(null);

  const handleMessage = useCallback(
    (msg: ProgressMessage) => {
      if (!currentProject || currentProject.id !== msg.projectId) return;
      setCurrentProject({
        ...currentProject,
        status: msg.status,
        progressPercent: msg.progressPercent,
      });
    },
    [currentProject, setCurrentProject],
  );

  useEffect(() => {
    if (!projectId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe(`/topic/progress/${projectId}`, (frame) => {
          try {
            const data: ProgressMessage = JSON.parse(frame.body);
            handleMessage(data);
          } catch {
            // ignore parse errors
          }
        });
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
      clientRef.current = null;
    };
  }, [projectId]); // intentionally exclude handleMessage to avoid reconnect loops
}
