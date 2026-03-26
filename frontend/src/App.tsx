import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import CreatePage from './pages/CreatePage';
import ProjectPage from './pages/ProjectPage';
import HistoryPage from './pages/HistoryPage';

function App() {
  return (
    <BrowserRouter>
      <Toaster
        position="top-center"
        toastOptions={{
          style: {
            background: '#1C1C22',
            color: '#F0EDE6',
            border: '1px solid rgba(255,255,255,0.06)',
            fontSize: '14px',
          },
        }}
      />
      <Routes>
        <Route path="/" element={<CreatePage />} />
        <Route path="/project/:id" element={<ProjectPage />} />
        <Route path="/history" element={<HistoryPage />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
