import React from 'react';

const Badge = ({
  children,
  type = 'neutral', // success, danger, warning, info, neutral, premium, highlight
  size = 'md', // md, sm
  colorClassName, // overrides the `type` color lookup, e.g. a per-domain status color map
  className = ''
}) => {
  const sizeStyle = size === 'sm' ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-0.5 text-xs';
  const baseStyle = `inline-flex items-center rounded-pill font-semibold uppercase tracking-wider border whitespace-nowrap ${sizeStyle}`;

  const types = {
    success: 'bg-[#e2fbeb] text-[#127a3c] border-[#a2f7c0]',
    danger: 'bg-red-50 text-red-700 border-red-200',
    warning: 'bg-amber-50 text-amber-700 border-amber-200',
    info: 'bg-blue-50 text-blue-700 border-blue-200',
    neutral: 'bg-shade-30 text-ink border-hairline-light',
    premium: 'bg-ink text-onPrimary border-ink',
    highlight: 'bg-aloe-10 text-ink border-[#a2f7c0]',
  };

  const selectedType = colorClassName || types[type] || types.neutral;

  return (
    <span className={`${baseStyle} ${selectedType} ${className}`}>
      {children}
    </span>
  );
};

export default Badge;
