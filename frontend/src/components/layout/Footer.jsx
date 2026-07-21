import React from 'react';

const Footer = () => {
  return (
    <footer className="bg-canvas-cream border-t border-hairline-light py-4 px-6 text-center text-xs text-shade-50 flex items-center justify-between mt-6">
      <span>
        &copy; {new Date().getFullYear()} Hệ Thống Quản Lý Kho. Tất cả các quyền được bảo lưu.
      </span>
      <span className="font-mono text-[10px]">
        Phiên bản 1.0.0 (Sprint 1)
      </span>
    </footer>
  );
};

export default Footer;
