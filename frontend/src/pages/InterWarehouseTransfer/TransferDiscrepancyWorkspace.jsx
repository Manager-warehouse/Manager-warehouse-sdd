import React, { useEffect, useMemo, useState } from 'react';
import { AlertTriangle, CheckCircle2, Clock3, FileSearch, Loader2, RefreshCw, Save } from 'lucide-react';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import { interWarehouseTransferService } from '../../services/inter-warehouse-transfer.service';
import { useUiStore } from '../../stores/ui.store';

const STATUS_OPTIONS = [
  { value: '', label: 'Tất cả hồ sơ' },
  { value: 'OPEN', label: 'Đang mở' },
  { value: 'RESOLVED_ACCEPTED', label: 'Đã chấp nhận hao hụt' },
  { value: 'RESOLVED_SOURCE_FAULT', label: 'Lỗi kho nguồn' },
  { value: 'RESOLVED_CARRIER_FAULT', label: 'Lỗi vận chuyển' },
  { value: 'RESOLVED_DESTINATION_COUNT_ERROR', label: 'Đếm sai kho đích' },
];

// Điều chuyển nội bộ: final receive chỉ tạo hồ sơ và giữ phần lệch; CEO chốt trách nhiệm ở màn này.
// Với nhận thừa, quyết định của CEO mới quyết định có cộng phần tạm giữ vào kho đích và trừ thêm kho nguồn hay không.
const RESOLUTION_OPTIONS = [
  { value: 'RESOLVED_ACCEPTED', label: 'Chấp nhận hao hụt' },
  { value: 'RESOLVED_SOURCE_FAULT', label: 'Lỗi kho nguồn' },
  { value: 'RESOLVED_CARRIER_FAULT', label: 'Lỗi vận chuyển / tài xế' },
  { value: 'RESOLVED_DESTINATION_COUNT_ERROR', label: 'Đếm sai kho đích' },
];

const OVER_RECEIPT_RESOLUTION_OPTIONS = [
  { value: 'RESOLVED_SOURCE_FAULT', label: 'Lỗi kho nguồn - nhập phần thừa vào kho đích' },
  { value: 'RESOLVED_DESTINATION_COUNT_ERROR', label: 'Lỗi đếm kho đích - không nhập phần thừa' },
];

const statusMeta = {
  OPEN: {
    label: 'Đang mở',
    badge: 'bg-warning-50 text-warning-700 border-warning-200',
    icon: Clock3,
  },
  RESOLVED_ACCEPTED: {
    label: 'Chấp nhận hao hụt',
    badge: 'bg-success-50 text-success-700 border-success-200',
    icon: CheckCircle2,
  },
  RESOLVED_SOURCE_FAULT: {
    label: 'Lỗi kho nguồn',
    badge: 'bg-danger-50 text-danger-700 border-danger-200',
    icon: AlertTriangle,
  },
  RESOLVED_CARRIER_FAULT: {
    label: 'Lỗi vận chuyển',
    badge: 'bg-danger-50 text-danger-700 border-danger-200',
    icon: AlertTriangle,
  },
  RESOLVED_DESTINATION_COUNT_ERROR: {
    label: 'Đếm sai kho đích',
    badge: 'bg-info-50 text-info-700 border-info-200',
    icon: FileSearch,
  },
};

const incidentTypeLabel = {
  SHORTAGE: 'Thiếu hàng',
  OVER_RECEIPT: 'Thừa hàng',
};

const formatQty = (value) => Number(value || 0).toLocaleString('vi-VN', {
  minimumFractionDigits: 0,
  maximumFractionDigits: 2,
});

const formatDateTime = (value) => {
  if (!value) return '-';
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value));
};

const getStatusBadge = (status) => {
  const meta = statusMeta[status] || { label: status, badge: 'bg-shade-30 text-ink border-hairline-light' };
  return <Badge size="sm" colorClassName={meta.badge}>{meta.label}</Badge>;
};

const TransferDiscrepancyWorkspace = () => {
  const addToast = useUiStore((state) => state.addToast);
  const [incidents, setIncidents] = useState([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [loadError, setLoadError] = useState('');
  const [selectedIncident, setSelectedIncident] = useState(null);
  const [resolutionStatus, setResolutionStatus] = useState(RESOLUTION_OPTIONS[0].value);
  const [resolutionNote, setResolutionNote] = useState('');

  const fetchIncidents = async () => {
    setLoading(true);
    setLoadError('');
    try {
      // Màn này là màn CEO hậu kiểm, nên luôn tải toàn bộ hồ sơ để hồ sơ đã chốt vẫn tra cứu được.
      // Bộ lọc trạng thái chỉ áp dụng ở client, tránh làm các thẻ thống kê bị đếm sai theo filter hiện tại.
      const data = await interWarehouseTransferService.getDiscrepancyIncidents();
      setIncidents(data || []);
    } catch (error) {
      setLoadError(error.response?.data?.message || error.message || 'Không tải được hồ sơ chênh lệch.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchIncidents();
  }, []);

  const visibleIncidents = useMemo(() => {
    // Lọc client-side để người dùng tra nhanh theo TRF/SKU/kho; không thay đổi trạng thái hồ sơ.
    const keyword = search.trim().toLowerCase();
    const filteredByStatus = statusFilter
      ? incidents.filter((incident) => incident.status === statusFilter)
      : incidents;
    if (!keyword) return filteredByStatus;
    return filteredByStatus.filter((incident) => [
      incident.transferNumber,
      incident.sourceWarehouseCode,
      incident.destinationWarehouseCode,
      incident.productSku,
      incident.productName,
      incident.resolutionNote,
    ].some((value) => String(value || '').toLowerCase().includes(keyword)));
  }, [incidents, search, statusFilter]);

  const stats = useMemo(() => ({
    total: incidents.length,
    open: incidents.filter((incident) => incident.status === 'OPEN').length,
    resolved: incidents.filter((incident) => incident.status !== 'OPEN').length,
  }), [incidents]);

  const openResolveModal = (incident) => {
    setSelectedIncident(incident);
    setResolutionStatus(
      incident.incidentType === 'OVER_RECEIPT'
        ? OVER_RECEIPT_RESOLUTION_OPTIONS[0].value
        : RESOLUTION_OPTIONS[0].value
    );
    setResolutionNote('');
  };

  const closeResolveModal = () => {
    if (submitting) return;
    setSelectedIncident(null);
    setResolutionNote('');
  };

  const handleResolve = async (event) => {
    event.preventDefault();
    if (!selectedIncident) return;
    const note = resolutionNote.trim();
    // Resolve bắt buộc có ghi chú để audit có căn cứ kết luận vì sao thiếu/thừa được chấp nhận hoặc quy trách nhiệm.
    if (!note) {
      addToast('Vui lòng nhập ghi chú xử lý hồ sơ chênh lệch.', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await interWarehouseTransferService.resolveDiscrepancyIncident(selectedIncident.id, {
        status: resolutionStatus,
        resolutionNote: note,
      });
      addToast('Đã cập nhật hướng xử lý hồ sơ chênh lệch.', 'success');
      setSelectedIncident(null);
      await fetchIncidents();
    } catch (error) {
      addToast(error.response?.data?.message || error.message || 'Không cập nhật được hồ sơ.', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mobile-page">
      <div className="flex flex-col lg:flex-row lg:items-end justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Điều chuyển / Hồ sơ chênh lệch
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Hồ sơ thiếu thừa sau nhận hàng
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1 max-w-3xl">
            Chỉ CEO thấy màn hình này. Phần lệch được giữ ngoài tồn chính thức cho tới khi CEO chốt trách nhiệm và lý do xử lý.
          </p>
        </div>
        <Button variant="outline-light" icon={RefreshCw} onClick={fetchIncidents} disabled={loading}>
          Tải lại
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
        <div className="rounded-lg border border-hairline-light bg-canvas-light p-4 shadow-level-3">
          <div className="text-[10px] font-bold uppercase tracking-widest text-shade-50">Tổng hồ sơ</div>
          <div className="mt-2 text-2xl font-display font-semibold text-ink">{stats.total}</div>
        </div>
        <div className="rounded-lg border border-warning-200 bg-warning-50/60 p-4 shadow-level-3">
          <div className="text-[10px] font-bold uppercase tracking-widest text-warning-700">Đang mở</div>
          <div className="mt-2 text-2xl font-display font-semibold text-warning-700">{stats.open}</div>
        </div>
        <div className="rounded-lg border border-success-200 bg-success-50/60 p-4 shadow-level-3">
          <div className="text-[10px] font-bold uppercase tracking-widest text-success-700">Đã chốt</div>
          <div className="mt-2 text-2xl font-display font-semibold text-success-700">{stats.resolved}</div>
        </div>
      </div>

      <div className="flex flex-col md:flex-row gap-3 items-stretch md:items-end">
        <Input
          label="Tìm kiếm"
          placeholder="Mã TRF, SKU, kho, ghi chú..."
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          leftIcon={FileSearch}
        />
        <Input
          type="select"
          label="Trạng thái hồ sơ"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          options={STATUS_OPTIONS}
          className="md:max-w-xs"
        />
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-16">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : loadError ? (
        <div className="rounded-lg border border-danger-200 bg-danger-50/70 p-6 shadow-level-3">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-danger-600 mt-0.5 shrink-0" />
              <div>
                <h3 className="text-sm font-bold text-danger-700">Chưa tải được hồ sơ chênh lệch</h3>
                <p className="text-xs text-danger-700/80 mt-1">{loadError}</p>
              </div>
            </div>
            <Button variant="outline-light" icon={RefreshCw} onClick={fetchIncidents}>
              Tải lại
            </Button>
          </div>
        </div>
      ) : visibleIncidents.length === 0 ? (
        <div className="rounded-lg border border-hairline-light bg-canvas-light p-10 text-center shadow-level-3">
          <FileSearch className="w-10 h-10 text-shade-40 mx-auto mb-3" />
          <h3 className="text-base font-bold text-ink">Không có hồ sơ phù hợp</h3>
          <p className="text-xs text-shade-50 mt-1">Khi final receive phát sinh thiếu/thừa, hồ sơ sẽ chờ CEO kết luận ở đây.</p>
        </div>
      ) : (
        <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[960px]">
              <thead className="bg-canvas-cream border-b border-hairline-light">
                <tr>
                  <th className="px-4 py-3 text-left text-[10px] font-bold text-shade-60 uppercase tracking-widest">Phiếu / Kho</th>
                  <th className="px-4 py-3 text-left text-[10px] font-bold text-shade-60 uppercase tracking-widest">Sản phẩm</th>
                  <th className="px-4 py-3 text-right text-[10px] font-bold text-shade-60 uppercase tracking-widest">Số lượng</th>
                  <th className="px-4 py-3 text-left text-[10px] font-bold text-shade-60 uppercase tracking-widest">Trạng thái</th>
                  <th className="px-4 py-3 text-left text-[10px] font-bold text-shade-60 uppercase tracking-widest">Ghi chú</th>
                  <th className="px-4 py-3 text-right text-[10px] font-bold text-shade-60 uppercase tracking-widest">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {visibleIncidents.map((incident) => {
                  const StatusIcon = statusMeta[incident.status]?.icon || FileSearch;
                  return (
                    <tr key={incident.id} className="hover:bg-canvas-cream/50 transition-colors">
                      <td className="px-4 py-4 align-top">
                        <div className="font-mono text-xs font-bold text-ink">{incident.transferNumber}</div>
                        <div className="text-xs text-shade-50 mt-1">
                          {incident.sourceWarehouseCode} → {incident.destinationWarehouseCode}
                        </div>
                        <div className="text-[11px] text-shade-40 mt-1">{formatDateTime(incident.createdAt)}</div>
                      </td>
                      <td className="px-4 py-4 align-top">
                        <div className="text-xs font-bold text-ink">{incident.productSku}</div>
                        <div className="text-xs text-shade-60 mt-1 max-w-xs whitespace-normal">{incident.productName}</div>
                        <div className="mt-2">
                          <Badge size="sm" type={incident.incidentType === 'SHORTAGE' ? 'warning' : 'info'}>
                            {incidentTypeLabel[incident.incidentType] || incident.incidentType}
                          </Badge>
                        </div>
                      </td>
                      <td className="px-4 py-4 align-top text-right">
                        <span className="font-mono text-sm font-bold text-ink">{formatQty(incident.quantity)}</span>
                      </td>
                      <td className="px-4 py-4 align-top">
                        <div className="flex items-center gap-2">
                          <StatusIcon className="w-4 h-4 text-shade-50" />
                          {getStatusBadge(incident.status)}
                        </div>
                        {incident.resolvedAt && (
                          <div className="text-[11px] text-shade-50 mt-2">
                            {incident.resolvedByName || 'Đã chốt'} · {formatDateTime(incident.resolvedAt)}
                          </div>
                        )}
                      </td>
                      <td className="px-4 py-4 align-top">
                        <p className="text-xs text-shade-60 max-w-sm whitespace-normal">
                          {incident.resolutionNote || '-'}
                        </p>
                      </td>
                      <td className="px-4 py-4 align-top text-right">
                        {incident.status === 'OPEN' ? (
                          <Button variant="primary" icon={Save} onClick={() => openResolveModal(incident)}>
                            Chốt xử lý
                          </Button>
                        ) : (
                          <span className="text-xs text-shade-40">Đã khóa hồ sơ</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal
        isOpen={Boolean(selectedIncident)}
        onClose={closeResolveModal}
        title="Chốt hồ sơ chênh lệch"
        maxWidth="max-w-2xl"
      >
        {selectedIncident && (
          <form onSubmit={handleResolve} className="space-y-5">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
              <div className="rounded-lg border border-hairline-light bg-canvas-cream/50 p-3">
                <div className="text-shade-50 font-semibold uppercase tracking-wider text-[10px]">Phiếu điều chuyển</div>
                <div className="font-mono font-bold text-ink mt-1">{selectedIncident.transferNumber}</div>
                <div className="text-shade-60 mt-1">{selectedIncident.sourceWarehouseCode} → {selectedIncident.destinationWarehouseCode}</div>
              </div>
              <div className="rounded-lg border border-hairline-light bg-canvas-cream/50 p-3">
                <div className="text-shade-50 font-semibold uppercase tracking-wider text-[10px]">Sản phẩm / SL lệch</div>
                <div className="font-bold text-ink mt-1">{selectedIncident.productSku}</div>
                <div className="text-shade-60 mt-1">{formatQty(selectedIncident.quantity)} · {incidentTypeLabel[selectedIncident.incidentType]}</div>
              </div>
            </div>

            <Input
              type="select"
              label="Hướng xử lý"
              value={resolutionStatus}
              onChange={(event) => setResolutionStatus(event.target.value)}
              options={selectedIncident.incidentType === 'OVER_RECEIPT'
                ? OVER_RECEIPT_RESOLUTION_OPTIONS
                : RESOLUTION_OPTIONS}
            />

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">
                Ghi chú quyết định
              </label>
              <textarea
                value={resolutionNote}
                onChange={(event) => setResolutionNote(event.target.value)}
                rows={5}
                className="w-full bg-canvas-light text-ink text-sm px-3 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all resize-none"
                placeholder="VD: Đối chiếu ảnh bàn giao, seal nguyên vẹn; chấp nhận hao hụt theo biên bản..."
              />
              <p className="text-[11px] text-shade-50">
                Nếu thừa hàng do lỗi kho nguồn, hệ thống trừ thêm kho nguồn và nhập phần tạm giữ vào kho đích; nếu do đếm sai kho đích thì chỉ đóng hồ sơ.
              </p>
            </div>

            <div className="flex flex-col-reverse md:flex-row justify-end gap-3 pt-2">
              <Button type="button" variant="outline-light" onClick={closeResolveModal} disabled={submitting}>
                Hủy
              </Button>
              <Button type="submit" variant="primary" icon={Save} loading={submitting}>
                Lưu quyết định
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};

export default TransferDiscrepancyWorkspace;
