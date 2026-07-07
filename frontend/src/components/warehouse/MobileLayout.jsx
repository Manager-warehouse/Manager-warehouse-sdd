import React from 'react';
import { ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const MobileLayout = ({ children, title, backTo }) => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col -m-4 sm:-m-6 min-h-[calc(100dvh-4rem)]">
      {title && (
        <div className="sticky top-0 z-10 bg-ink text-white px-4 py-3.5 flex items-center gap-3 shadow-md">
          {backTo && (
            <button
              onClick={() => navigate(backTo)}
              className="p-1 rounded-full hover:bg-white/10 transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
            </button>
          )}
          <h1 className="text-base font-semibold tracking-tight flex-1 truncate">{title}</h1>
        </div>
      )}
      <div className="flex-1 bg-canvas-cream overflow-y-auto p-4 md:p-6">
        {children}
      </div>
    </div>
  );
};

export default MobileLayout;
