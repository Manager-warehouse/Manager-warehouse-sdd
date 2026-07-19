import React, { useEffect, useRef, useState } from 'react';
import { Camera } from 'lucide-react';

const readImageAsDataUrl = (file) => new Promise((resolve, reject) => {
  const reader = new FileReader();
  reader.onload = () => resolve(reader.result);
  reader.onerror = reject;
  reader.readAsDataURL(file);
});

const compressImageFile = async (file, maxDimension = 1600, quality = 0.78) => {
  const dataUrl = await readImageAsDataUrl(file);
  const image = new Image();
  await new Promise((resolve, reject) => {
    image.onload = resolve;
    image.onerror = reject;
    image.src = dataUrl;
  });

  const ratio = Math.min(1, maxDimension / Math.max(image.width, image.height));
  const canvas = document.createElement('canvas');
  canvas.width = Math.max(1, Math.round(image.width * ratio));
  canvas.height = Math.max(1, Math.round(image.height * ratio));
  const context = canvas.getContext('2d');
  context.drawImage(image, 0, 0, canvas.width, canvas.height);

  const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', quality));
  if (!blob || blob.size >= file.size) {
    return file;
  }

  const baseName = (file.name || 'photo').replace(/\.[^.]+$/, '');
  return new File([blob], `${baseName}.jpg`, { type: 'image/jpeg', lastModified: Date.now() });
};

const PhotoCaptureInput = ({
  label,
  value,
  fileName,
  onChange,
  output = 'dataUrl',
  required = false,
  disabled = false,
}) => {
  const cameraInputRef = useRef(null);
  const galleryInputRef = useRef(null);
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

    let selectedFile = file;
    try {
      selectedFile = await compressImageFile(file);
    } catch {
      selectedFile = file;
    }

    setLocalName(selectedFile.name || 'Ảnh đã chọn');

    if (output === 'file') {
      if (localPreview.startsWith('blob:')) {
        URL.revokeObjectURL(localPreview);
      }
      setLocalPreview(URL.createObjectURL(selectedFile));
      onChange?.(selectedFile, selectedFile);
      return;
    }

    const dataUrl = await readImageAsDataUrl(selectedFile);
    onChange?.(dataUrl, selectedFile);
  };

  const openCamera = () => {
    if (disabled) return;
    cameraInputRef.current?.click();
  };

  const openGallery = () => {
    if (disabled) return;
    galleryInputRef.current?.click();
  };

  return (
    <div className="flex flex-col gap-2">
      {label && (
        <div className="text-xs font-semibold uppercase tracking-wider text-shade-60">
          {label}{required ? ' *' : ''}
        </div>
      )}
      <div className="flex flex-col sm:flex-row gap-2">
        <button
          type="button"
          onClick={openCamera}
          disabled={disabled}
          className={`rounded-pill font-medium transition-all duration-150 inline-flex items-center justify-center gap-2 text-sm leading-none box-border h-10 px-6 bg-canvas-light text-ink border border-ink hover:bg-shade-30 disabled:opacity-50 disabled:cursor-not-allowed`}
        >
          <Camera className="w-4 h-4" />
          Chụp ảnh
        </button>
        <button
          type="button"
          onClick={openGallery}
          disabled={disabled}
          className={`rounded-pill font-medium transition-all duration-150 inline-flex items-center justify-center gap-2 text-sm leading-none box-border h-10 px-6 bg-canvas-light text-ink border border-hairline-light hover:bg-shade-30 disabled:opacity-50 disabled:cursor-not-allowed`}
        >
          Chọn ảnh
        </button>
        <input
          ref={cameraInputRef}
          type="file"
          accept="image/*"
          capture="environment"
          className="hidden"
          disabled={disabled}
          onChange={handleFile}
        />
        <input
          ref={galleryInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          disabled={disabled}
          onChange={handleFile}
        />
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
