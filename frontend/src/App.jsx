import React from 'react';
import { BrowserRouter } from 'react-router-dom';
import AppRoutes from './routes/AppRoutes';
import { ToastContainer } from './components/common/Toast';
import './styles/globals.css';

function App() {
  return (
    <BrowserRouter>
      {/* Dynamic App Routing */}
      <AppRoutes />
      
      {/* Toast Alert floating layout */}
      <ToastContainer />
    </BrowserRouter>
  );
}

export default App;
