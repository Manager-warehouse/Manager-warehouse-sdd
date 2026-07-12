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
    success: 'bg-success-50 text-success-700 border-success-200',
    danger: 'bg-danger-50 text-danger-700 border-danger-200',
    warning: 'bg-warning-50 text-warning-700 border-warning-200',
    info: 'bg-info-50 text-info-700 border-info-200',
    neutral: 'bg-shade-30 text-ink border-hairline-light',
    premium: 'bg-ink text-onPrimary border-ink',
    highlight: 'bg-aloe-10 text-ink border-success-200',
  };

  const selectedType = colorClassName || types[type] || types.neutral;

  return (
    <span className={`${baseStyle} ${selectedType} ${className}`}>
      {children}
    </span>
  );
};

export default Badge;
