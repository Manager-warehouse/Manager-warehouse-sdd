import React, { useState } from 'react';
import { Eye, EyeOff } from 'lucide-react';

const Input = React.forwardRef(({
  label,
  type = 'text',
  error,
  className = '',
  id,
  options = [], // for type="select" or multiselect
  ...props
}, ref) => {
  const [showPassword, setShowPassword] = useState(false);
  const inputId = id || `input-${Math.random().toString(36).substring(2, 9)}`;
  
  const baseInputStyle = 'w-full bg-canvas-light text-ink text-sm px-3 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all min-h-[44px] disabled:bg-canvas-cream/60 disabled:text-shade-50 disabled:cursor-not-allowed disabled:opacity-70';
  const errorInputStyle = 'border-red-500 focus:ring-red-500 focus:border-red-500';
  
  return (
    <div className={`flex flex-col gap-1.5 w-full ${className}`}>
      {label && (
        <label
          htmlFor={inputId}
          className="text-xs font-semibold uppercase tracking-wider text-shade-60 whitespace-nowrap overflow-hidden text-ellipsis"
          title={label}
        >
          {label}
        </label>
      )}

      {type === 'select' ? (
        <select
          id={inputId}
          ref={ref}
          className={`${baseInputStyle} ${error ? errorInputStyle : ''}`}
          {...props}
        >
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      ) : type === 'checkbox-group' ? (
        <div className="flex flex-col gap-2 mt-1">
          {options.map((opt) => (
            <label key={opt.value} className="inline-flex items-center gap-2 text-sm text-shade-70 cursor-pointer">
              <input
                type="checkbox"
                value={opt.value}
                checked={props.value?.includes(opt.value)}
                onChange={(e) => {
                  const val = opt.value;
                  const isChecked = e.target.checked;
                  const currentValues = props.value || [];
                  const newValues = isChecked
                    ? [...currentValues, val]
                    : currentValues.filter((v) => v !== val);
                  props.onChange(newValues);
                }}
                className="w-4 h-4 rounded border-hairline-light text-ink focus:ring-ink"
              />
              <span>{opt.label}</span>
            </label>
          ))}
        </div>
      ) : (
        <div className="relative w-full">
          <input
            id={inputId}
            type={type === 'password' ? (showPassword ? 'text' : 'password') : type}
            ref={ref}
            className={`${baseInputStyle} ${error ? errorInputStyle : ''} ${type === 'password' ? 'pr-10' : ''}`}
            {...props}
          />
          {type === 'password' && (
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-shade-50 hover:text-ink focus:outline-none transition-colors"
              title={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            >
              {showPassword ? (
                <EyeOff className="w-4 h-4" />
              ) : (
                <Eye className="w-4 h-4" />
              )}
            </button>
          )}
        </div>
      )}

      {error && (
        <span className="text-xs text-red-500 font-medium mt-0.5">
          {error}
        </span>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;
