import axios from 'axios';
import type { AdGenerateRequest, AdProjectResponse, ApiResponse } from '../types';

const api = axios.create({
  baseURL: '/api',
  timeout: 120_000,
});

// POST /api/projects → 프로젝트 생성
export const createProject = (data: AdGenerateRequest) =>
  api.post<ApiResponse<AdProjectResponse>>('/projects', data).then(r => r.data);

// GET /api/projects/{id} → 상태 조회 (폴링)
export const getProject = (id: number) =>
  api.get<ApiResponse<AdProjectResponse>>(`/projects/${id}`).then(r => r.data);

// GET /api/projects → 전체 목록
export const getAllProjects = () =>
  api.get<ApiResponse<AdProjectResponse[]>>('/projects').then(r => r.data);

// POST /api/projects/{id}/retry?step=xxx
export const retryStep = (id: number, step: string) =>
  api.post<ApiResponse<AdProjectResponse>>(`/projects/${id}/retry?step=${step}`).then(r => r.data);

// 파일 URL
export const getVideoFileUrl = (fileName: string) => `/api/files/video/${fileName}`;
export const getThumbnailFileUrl = (fileName: string) => `/api/files/thumbnail/${fileName}`;

export default api;
