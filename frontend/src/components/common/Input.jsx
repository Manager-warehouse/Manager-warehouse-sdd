import React from 'react';

const Input = React.forwardRef(({
  label,
  type = 'text',
  error,
  className = '',
  id,
  options = [], // for type="select" or multiselect
  ...props
}, ref) => {
  const inputId = id || `input-${Math.random().toString(36).substring(2, 9)}`;
  
  const baseInputStyle = 'w-full bg-canvas-light text-ink text-sm px-3 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all min-h-[44px]';
  const errorInputStyle = 'border-red-500 focus:ring-red-500 focus:border-red-500';
  
  return (
    <div className={`flex flex-col gap-1.5 w-full ${className}`}>
      {label && (
        <label htmlFor={inputId} className="text-xs font-semibold uppercase tracking-wider text-shade-60">
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
        <input
          id={inputId}
          type={type}
          ref={ref}
          className={`${baseInputStyle} ${error ? errorInputStyle : ''}`}
          {...props}
        />
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
