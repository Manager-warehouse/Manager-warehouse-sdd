import React, { useMemo, useState } from 'react';
import { AlertTriangle, Camera, CheckCircle2, ChevronDown, ChevronUp, Eye, ImageOff, RotateCcw, ShieldCheck, Truck } from 'lucide-react';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';

const FLOW_STYLES = {
  // Điều chuyển nội bộ tách ảnh theo chặng để truy vết trách nhiệm khi có thiếu/thừa hoặc hàng quay đầu.
  source: {
    icon: ShieldCheck,
    label: 'Xuất kho nguồn',
    className: 'border-info-200 bg-info-50 text-info-700',
  },
  handover: {
    icon: Truck,
    label: 'Vận chuyển / bàn giao',
    className: 'border-aloe-200 bg-aloe-50 text-aloe-700',
  },
  receive: {
    icon: CheckCircle2,
    label: 'QC kho nhận',
    className: 'border-success-200 bg-success-50 text-success-700',
  },
  return: {
    icon: RotateCcw,
    label: 'Quay đầu xe',
    className: 'border-warning-200 bg-warning-50 text-warning-700',
  },
};

const formatDateTime = (value) => {
  if (!value) return 'Chưa ghi nhận';
  return new Intl.DateTimeFormat('vi-VN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value));
};

const normalizeRef = (ref) => {
  // Backend có thể trả URL tuyệt đối hoặc path tương đối; UI chuẩn hóa để preview ảnh bằng chứng thống nhất.
  if (!ref) return '';
  if (/^(https?:|data:|blob:)/i.test(ref)) return ref;
  if (ref.startsWith('/')) return ref;
  return `/${ref}`;
};

const EvidenceCard = ({ item, onPreview }) => {
  const style = FLOW_STYLES[item.flow];
  const Icon = style.icon;
  const hasImage = Boolean(item.photoRef);

  return (
    <div className={`rounded-lg border p-3 text-xs ${item.highlight ? 'border-danger-200 bg-danger-50/50' : 'border-hairline-light bg-canvas-light'}`}>
      <div className="flex items-start justify-between gap-3">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <span className={`inline-flex items-center gap-1 rounded-full border px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider ${style.className}`}>
              <Icon className="h-3 w-3" />
              {style.label}
            </span>
            <span className={hasImage ? 'text-success-700 font-semibold' : 'text-shade-40 font-semibold'}>
              {hasImage ? 'Đã có ảnh' : 'Chưa có ảnh'}
            </span>
          </div>
          <div className="mt-2 font-semibold text-ink">{item.title}</div>
          <div className="mt-1 text-shade-50">{item.description}</div>
          <div className="mt-2 grid grid-cols-1 sm:grid-cols-2 gap-1 text-[11px] text-shade-60">
            <span>Người ghi nhận: <strong className="text-ink">{item.actorName || 'Chưa ghi nhận'}</strong></span>
            <span>Thời gian: <strong className="text-ink">{formatDateTime(item.timestamp)}</strong></span>
          </div>
        </div>
        <div className="h-16 w-20 shrink-0 overflow-hidden rounded-md border border-hairline-light bg-canvas-cream flex items-center justify-center">
          {hasImage ? (
            <img src={normalizeRef(item.photoRef)} alt={item.title} className="h-full w-full object-cover" />
          ) : (
            <ImageOff className="h-5 w-5 text-shade-30" />
          )}
        </div>
      </div>
      <div className="mt-3 flex justify-end">
        <Button
          variant="outline-light"
          size="sm"
          icon={hasImage ? Eye : Camera}
          onClick={() => hasImage && onPreview(item)}
          disabled={!hasImage}
        >
          Xem ảnh
        </Button>
      </div>
    </div>
  );
};

const TransferEvidencePanel = ({ transfer }) => {
  const [preview, setPreview] = useState(null);
  const [expanded, setExpanded] = useState(false);

  const evidenceGroups = useMemo(() => {
    if (!transfer) return [];
    // Khi có discrepancy/return, panel highlight các ảnh cần đối chiếu trước: QC nguồn, bàn giao xe, QC nhận.
    const hasDiscrepancy = transfer.status === 'COMPLETED_WITH_DISCREPANCY'
      || Boolean(transfer.discrepancyReason)
      || transfer.items?.some((item) => Number(item.receivedQty ?? item.sentQty ?? 0) !== Number(item.sentQty ?? item.receivedQty ?? 0));
    const hasReturn = Boolean(transfer.isReturned || transfer.returnDepartedAt || transfer.returnArrivedAt || transfer.returnPhotoRef);

    const sourceItems = [
      {
        flow: 'source',
        title: 'Ảnh QC xuất kho nguồn',
        description: 'Bằng chứng Thủ kho nguồn kiểm hàng trước khi cho xuất khỏi kho.',
        photoRef: transfer.outboundQcPhotoRef,
        actorName: transfer.outboundQcByName,
        timestamp: transfer.outboundQcAt,
        highlight: hasDiscrepancy,
      },
      {
        flow: 'source',
        title: 'Ảnh bàn giao hàng lên xe',
        description: 'Bằng chứng hàng đã được bàn giao cho tài xế tại kho nguồn.',
        photoRef: transfer.loadHandoverPhotoRef,
        actorName: transfer.loadHandoverByName,
        timestamp: transfer.loadHandoverAt,
        highlight: hasDiscrepancy,
      },
    ];

    const destinationItems = [
      {
        flow: 'handover',
        title: 'Ảnh bàn giao tại kho đích',
        description: 'Bằng chứng xe/hàng đã đến kho nhận và được bàn giao vật lý.',
        photoRef: transfer.arrivalHandoverPhotoRef,
        actorName: transfer.arrivalHandoverByName,
        timestamp: transfer.arrivalHandoverAt,
        highlight: hasDiscrepancy,
      },
      {
        flow: 'receive',
        title: 'Ảnh QC nhận hàng kho đích',
        description: 'Bằng chứng kiểm hàng khi nhận, dùng để đối chiếu hỏng, thiếu hoặc sai tình trạng.',
        photoRef: transfer.receiveQcPhotoRef,
        actorName: transfer.arrivalHandoverByName,
        timestamp: transfer.arrivalHandoverAt,
        highlight: hasDiscrepancy,
      },
    ];

    const returnItems = [
      {
        flow: 'return',
        title: 'Ảnh bàn giao hàng quay đầu',
        description: 'Bằng chứng hàng/xe quay về kho nguồn khi quá hạn hoặc return-to-source.',
        photoRef: transfer.returnPhotoRef,
        actorName: transfer.returnArrivalHandoverByName,
        timestamp: transfer.returnArrivalHandoverAt || transfer.returnArrivedAt,
        highlight: hasReturn,
      },
    ];

    return [
      { key: 'source', title: 'Luồng xuất kho nguồn', items: sourceItems },
      { key: 'destination', title: 'Luồng vận chuyển và QC kho nhận', items: destinationItems },
      { key: 'return', title: 'Luồng quay đầu xe', items: returnItems },
    ];
  }, [transfer]);

  if (!transfer) return null;

  const hasAnyPhoto = evidenceGroups.some((group) => group.items.some((item) => item.photoRef));
  const hasDiscrepancy = transfer.status === 'COMPLETED_WITH_DISCREPANCY' || Boolean(transfer.discrepancyReason);
  const photoCount = evidenceGroups.reduce(
    (total, group) => total + group.items.filter((item) => item.photoRef).length,
    0,
  );

  return (
    <div className="border border-hairline-light rounded-lg bg-canvas-light p-4">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3">
        <div className="flex flex-col gap-1">
          <div className="text-xs font-bold uppercase tracking-wider text-shade-60">Bằng chứng ảnh</div>
          <div className="text-xs text-shade-50">
            {photoCount > 0
              ? `${photoCount} ảnh đã ghi nhận cho phiếu ${transfer.transferNumber}.`
              : `Chưa có ảnh nào được ghi nhận cho phiếu ${transfer.transferNumber}.`}
          </div>
        </div>
        <Button
          variant="outline-light"
          size="sm"
          icon={expanded ? ChevronUp : ChevronDown}
          onClick={() => setExpanded((value) => !value)}
        >
          {expanded ? 'Ẩn bằng chứng ảnh' : 'Xem bằng chứng ảnh'}
        </Button>
      </div>

      {expanded && hasDiscrepancy && (
        <div className="mt-3 mb-3 rounded-md border border-danger-200 bg-danger-50 px-3 py-2 text-xs text-danger-700 flex gap-2">
          <AlertTriangle className="h-4 w-4 shrink-0" />
          <span>Phiếu có chênh lệch: ưu tiên đối chiếu ảnh QC xuất kho, bàn giao lên xe, bàn giao kho đích và QC nhận hàng.</span>
        </div>
      )}

      {expanded && !hasAnyPhoto && (
        <div className="mt-3 mb-3 rounded-md border border-hairline-light bg-canvas-cream px-3 py-2 text-xs text-shade-50">
          Chưa có ảnh nào được ghi nhận cho phiếu này.
        </div>
      )}

      {expanded && (
        <div className="mt-3 flex flex-col gap-3">
          {evidenceGroups.map((group) => (
            <div key={group.key}>
              <div className="mb-2 text-[11px] font-bold uppercase tracking-wider text-shade-60">{group.title}</div>
              <div className="grid grid-cols-1 gap-2">
                {group.items.map((item) => (
                  <EvidenceCard key={item.title} item={item} onPreview={setPreview} />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal
        isOpen={Boolean(preview)}
        onClose={() => setPreview(null)}
        title={preview?.title || 'Xem ảnh'}
        maxWidth="max-w-4xl"
      >
        {preview && (
          <div className="flex flex-col gap-3">
            <div className="rounded-lg border border-hairline-light bg-canvas-night/5 overflow-hidden">
              <img src={normalizeRef(preview.photoRef)} alt={preview.title} className="w-full max-h-[70vh] object-contain bg-black" />
            </div>
            <div className="text-xs text-shade-60">
              {preview.actorName || 'Chưa ghi nhận người upload'} · {formatDateTime(preview.timestamp)}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default TransferEvidencePanel;
