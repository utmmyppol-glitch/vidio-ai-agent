import { create } from 'zustand';
import type { AdProjectResponse } from '../types';

interface AppStore {
  currentProject: AdProjectResponse | null;
  setCurrentProject: (p: AdProjectResponse | null) => void;
}

export const useAppStore = create<AppStore>((set) => ({
  currentProject: null,
  setCurrentProject: (p) => set({ currentProject: p }),
}));
