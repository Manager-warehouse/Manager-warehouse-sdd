import React, { useEffect, useState } from 'react';
import { Camera } from 'lucide-react';

const readImageAsDataUrl = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader();
  reader.onload = () => resolve(reader.result);
  reader.onerror = reject;
  reader.readAsDataURL(file);
});

const PhotoCaptureInput = ({
  label,
  value,
  fileName,
  onChange,
  output = 'dataUrl',
  required = false,
  disabled = false,
}) => {
  const [localPreview, setLocalPreview] = useState('');
  const [localName, setLocalName] = useState('');

  useEffect(() => () => {
    if (localPreview.startsWith('blob:')) {
      URL.revokeObjectURL(localPreview);
    }
  }, [localPreview]);

  const selectedName = fileName || localName;
  const preview = output === 'dataUrl' ? value : localPreview;

  const handleFile = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file || !file.type.startsWith('image/')) return;

    setLocalName(file.name || 'Ảnh đã chọn');

    if (output === 'file') {
      if (localPreview.startsWith('blob:')) {
        URL.revokeObjectURL(localPreview);
      }
      setLocalPreview(URL.createObjectURL(file));
      onChange?.(file, file);
      return;
    }

    const dataUrl = await readImageAsDataUrl(file);
    onChange?.(dataUrl, file);
  };

  return (
    <div className="flex flex-col gap-2">
      {label && (
        <div className="text-xs font-semibold uppercase tracking-wider text-shade-60">
          {label}{required ? ' *' : ''}
        </div>
      )}
      <div className="flex flex-col sm:flex-row gap-2">
        <label className={`rounded-pill font-medium transition-all duration-150 inline-flex items-center justify-center gap-2 text-sm leading-none box-border h-10 px-6 bg-canvas-light text-ink border border-ink hover:bg-shade-30 ${disabled ? 'opacity-50 pointer-events-none' : 'cursor-pointer'}`}>
          <Camera className="w-4 h-4" />
          Chọn hoặc chụp ảnh
          <input
            type="file"
            accept="image/*"
            className="sr-only"
            disabled={disabled}
            onChange={handleFile}
          />
        </label>
        <div className="min-h-10 flex flex-1 items-center rounded-md border border-hairline-light bg-canvas-light px-3 text-xs text-shade-60">
          {selectedName || 'Chưa chọn ảnh'}
        </div>
      </div>
      {preview && (
        <img
          src={preview}
          alt={label || 'Ảnh đã chọn'}
          className="h-28 w-28 rounded-md border border-hairline-light object-cover"
        />
      )}
    </div>
  );
};

export default PhotoCaptureInput;
