import React from 'react';

const Button = ({
  children,
  type = 'button',
  variant = 'primary', // primary, outline-light, outline-dark, aloe
  loading = false,
  disabled = false,
  onClick,
  className = '',
  icon: Icon,
  ...props
}) => {
  const baseStyle = 'rounded-pill font-medium transition-all duration-150 inline-flex items-center justify-center gap-2 text-sm leading-none box-border h-10 focus:outline-none focus:ring-2 focus:ring-offset-2 active:scale-98 disabled:opacity-50 disabled:pointer-events-none disabled:active:scale-100';
  
  const sizeStyle = 'px-6 h-10';
  
  const variants = {
    primary: 'bg-ink text-onPrimary hover:bg-shade-70 focus:ring-shade-70',
    'outline-light': 'bg-canvas-light text-ink border border-ink hover:bg-shade-30 focus:ring-shade-40',
    'outline-dark': 'bg-transparent text-onPrimary border-2 border-onPrimary hover:bg-onPrimary hover:text-canvas-night focus:ring-onPrimary',
    aloe: 'bg-aloe-10 text-ink hover:opacity-90 focus:ring-aloe-10',
  };

  const selectedVariant = variants[variant] || variants.primary;

  return (
    <button
      type={type}
      className={`${baseStyle} ${sizeStyle} ${selectedVariant} ${className}`}
      disabled={disabled || loading}
      onClick={onClick}
      {...props}
    >
      {loading ? (
        <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-current" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
      ) : Icon ? (
        <Icon className="w-4 h-4" />
      ) : null}
      {children}
    </button>
  );
};

export default Button;
