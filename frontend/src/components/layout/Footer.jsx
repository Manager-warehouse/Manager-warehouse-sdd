import React from 'react';

const Footer = () => {
  return (
    <footer className="bg-canvas-cream border-t border-hairline-light py-4 px-6 text-center text-xs text-shade-50 flex items-center justify-between mt-auto">
      <span>
        &copy; {new Date().getFullYear()} Công ty Cổ phần Máy tính Phúc Anh. Tất cả các quyền được bảo lưu.
      </span>
      <span className="font-mono text-[10px]">
        Phiên bản 1.0.0 (Sprint 1)
      </span>
    </footer>
  );
};

export default Footer;
