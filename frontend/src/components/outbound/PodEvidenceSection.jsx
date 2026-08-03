import React, { useCallback, useEffect, useRef, useState } from 'react';
import { AlertTriangle, Image, Loader2, RefreshCw } from 'lucide-react';
import Modal from '../common/Modal';
import { outboundService } from '../../services/outbound.service';

const EVIDENCE = [
  { type: 'GOODS', key: 'goods', label: 'Ảnh hàng hóa khi giao' },
  { type: 'SIGNED_DOCUMENT', key: 'signedDocument', label: 'Ảnh chứng từ đã ký' },
];

const PodEvidenceSection = ({ deliveryOrderId, status }) => {
  const [state, setState] = useState({ loading: false, error: '', images: {} });
  const [preview, setPreview] = useState(null);
  const objectUrls = useRef([]);

  const releaseObjectUrls = useCallback(() => {
    objectUrls.current.forEach((url) => URL.revokeObjectURL(url));
    objectUrls.current = [];
  }, []);

  const loadEvidence = useCallback(async () => {
    releaseObjectUrls();
    setPreview(null);
    setState({ loading: true, error: '', images: {} });
    try {
      const blobs = await Promise.all(EVIDENCE.map(({ type }) => (
        outboundService.getPodEvidenceImage(deliveryOrderId, type)
      )));
      const urls = blobs.map((blob) => URL.createObjectURL(blob));
      objectUrls.current = urls;
      setState({
        loading: false,
        error: '',
        images: Object.fromEntries(EVIDENCE.map(({ key }, index) => [key, urls[index]])),
      });
    } catch (error) {
      releaseObjectUrls();
      setState({
        loading: false,
        error: error?.response?.data?.message || error?.message || 'Không thể tải ảnh POD.',
        images: {},
      });
    }
  }, [deliveryOrderId, releaseObjectUrls]);

  useEffect(() => {
    if (!['COMPLETED', 'CLOSED'].includes(status)) return undefined;
    loadEvidence();
    return releaseObjectUrls;
  }, [loadEvidence, releaseObjectUrls, status]);

  if (!['COMPLETED', 'CLOSED'].includes(status)) return null;

  return (
    <section aria-labelledby="pod-evidence-title" className="border-t border-hairline-light pt-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 id="pod-evidence-title" className="flex items-center gap-2 text-xs font-bold uppercase tracking-widest text-shade-40">
          <Image className="h-3.5 w-3.5" /> Bằng chứng giao hàng
        </h2>
        {!state.loading && state.error && (
          <button
            type="button"
            onClick={loadEvidence}
            className="inline-flex min-h-9 items-center gap-2 rounded-md border border-hairline-light px-3 text-xs font-semibold text-ink hover:bg-canvas-cream"
          >
            <RefreshCw className="h-3.5 w-3.5" /> Thử lại
          </button>
        )}
      </div>

      {state.loading && (
        <div role="status" className="flex min-h-40 items-center justify-center text-sm text-shade-50">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" /> Đang tải ảnh...
        </div>
      )}

      {!state.loading && state.error && (
        <div role="alert" className="flex min-h-28 items-center gap-3 rounded-md border border-danger-200 bg-danger-50 p-4 text-sm text-danger-700">
          <AlertTriangle className="h-5 w-5 shrink-0" /> {state.error}
        </div>
      )}

      {!state.loading && !state.error && (
        <div className="grid gap-4 sm:grid-cols-2">
          {EVIDENCE.map(({ key, label }) => (
            <figure key={key} className="overflow-hidden rounded-md border border-hairline-light bg-canvas-light">
              <button
                type="button"
                title={`Xem ${label.toLowerCase()}`}
                onClick={() => setPreview({ src: state.images[key], label })}
                className="block aspect-[4/3] w-full overflow-hidden bg-canvas-cream"
              >
                <img src={state.images[key]} alt={label} className="h-full w-full object-contain" />
              </button>
              <figcaption className="border-t border-hairline-light px-3 py-2 text-xs font-semibold text-ink">
                {label}
              </figcaption>
            </figure>
          ))}
        </div>
      )}

      <Modal
        isOpen={Boolean(preview)}
        onClose={() => setPreview(null)}
        title={preview?.label || 'Ảnh POD'}
        maxWidth="max-w-4xl"
      >
        {preview && <img src={preview.src} alt={preview.label} className="max-h-[72vh] w-full object-contain" />}
      </Modal>
    </section>
  );
};

export default PodEvidenceSection;
