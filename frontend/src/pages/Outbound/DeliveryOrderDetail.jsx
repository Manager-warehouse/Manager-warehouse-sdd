import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Clock,
  FileText,
  Loader2,
  MapPin,
  PackageSearch,
  X,
} from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import PickingListTable from '../../components/warehouse/PickingListTable';
import DeliveryOrderPickingPlanEditor from '../../components/warehouse/DeliveryOrderPickingPlanEditor';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import { ROLES } from '../../utils/constants';

const DO_STATUS_MAP = {
  NEW: { label: 'Mới', color: 'bg-canvas-cream text-shade-70 border-hairline-light' },
  WAITING_PICKING: { label: 'Chờ lấy hàng & kiểm định', color: 'bg-info-50 text-info-700 border-info-200' },
  QC_PENDING_APPROVAL: { label: 'Chờ duyệt kiểm định', color: 'bg-violet-50 text-violet-700 border-violet-200' },
  QC_COMPLETED: { label: 'Hoàn tất kiểm định', color: 'bg-success-50 text-success-700 border-success-200' },
  WAREHOUSE_APPROVED: { label: 'Chờ vận chuyển', color: 'bg-warning-50 text-warning-700 border-warning-200' },
  IN_TRANSIT: { label: 'Đang giao', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: 'Đã giao', color: 'bg-success-50 text-success-900 border-success-300' },
  RETURNED: { label: 'Hoàn trả', color: 'bg-orange-50 text-orange-700 border-orange-200' },
  REJECTED: { label: 'Bị từ chối', color: 'bg-rose-50 text-rose-700 border-rose-200' },
  CANCELLED: { label: 'Đã hủy', color: 'bg-danger-50 text-danger-700 border-danger-200' },
};

const getStatusBadge = (status) => {
  const { label, color } = DO_STATUS_MAP[status] ?? { label: status, color: 'bg-canvas-cream text-shade-70 border-hairline-light' };
  return <Badge size="sm" colorClassName={color}>{label}</Badge>;
};

export default function DeliveryOrderDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();
  const { hasRole } = useAuthStore();

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [loadingCandidates, setLoadingCandidates] = useState(false);
  const [rejectModal, setRejectModal] = useState({ show: false, reason: '' });
  const [draftItems, setDraftItems] = useState([]);
  const [pickingCandidates, setPickingCandidates] = useState({});

  useEffect(() => {
    fetchOrder();
  }, [id]);

  const fetchOrder = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getDeliveryOrderById(id);
      setOrder(data);
      setDraftItems(outboundService.createPickingPlanDraft(data.items || []));
      if (hasRole(ROLES.STOREKEEPER) && ['NEW', 'WAITING_PICKING'].includes(data.status || data.raw_status)) {
        setLoadingCandidates(true);
        try {
          const candidates = await outboundService.getPickingCandidates(id);
          setPickingCandidates(candidates);
        } finally {
          setLoadingCandidates(false);
        }
      } else {
        setPickingCandidates({});
      }
    } catch (error) {
      addToast(error.message || 'Không tìm thấy đơn xuất hàng', 'error');
      navigate('/outbound/delivery-orders');
    } finally {
      setLoading(false);
    }
  };

  const handleStartPicking = async () => {
    const hasInvalidItem = draftItems.some((item) => {
      const plannedQty = (item.allocations || []).reduce(
        (sum, allocation) => sum + Number(allocation.planned_qty || 0),
        0,
      );
      const hasIncompleteAllocation = (item.allocations || []).some(
        (allocation) => Number(allocation.planned_qty || 0) > 0 && !allocation.inventory_id,
      );
      return hasIncompleteAllocation || plannedQty !== Number(item.requested_qty || 0);
    });

    if (hasInvalidItem) {
      addToast('Mỗi dòng hàng phải chọn đủ inventory và số lượng trước khi lưu.', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await outboundService.savePickingPlan(id, draftItems);
      addToast('Đã lưu kế hoạch lấy hàng', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Không thể bắt đầu soạn hàng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleApproveQuality = async () => {
    setSubmitting(true);
    try {
      await outboundService.approveQualityOutbound(id);
      addToast('Đã xác nhận chất lượng sau QC', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Lỗi khi xác nhận chất lượng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleApproveWarehouse = async () => {
    setSubmitting(true);
    try {
      await outboundService.approveWarehouseOutbound(id);
      addToast('Đã phê duyệt xuất kho', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Lỗi khi phê duyệt xuất kho', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!rejectModal.reason.trim()) {
      addToast('Vui lòng nhập lý do từ chối', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await outboundService.rejectWarehouseOutbound(id, rejectModal.reason.trim());
      addToast('Đã từ chối đơn xuất hàng', 'success');
      setRejectModal({ show: false, reason: '' });
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Lỗi khi từ chối đơn xuất hàng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-20">
        <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
      </div>
    );
  }

  if (!order) return null;

  const currentStatus = order.status || order.raw_status;
  const canEditPickingPlan = ['NEW', 'WAITING_PICKING'].includes(currentStatus) && hasRole(ROLES.STOREKEEPER);
  const canOpenQc = currentStatus === 'WAITING_PICKING' && hasRole(ROLES.WAREHOUSE_STAFF);
  const canApproveQuality = currentStatus === 'QC_PENDING_APPROVAL' && hasRole(ROLES.STOREKEEPER);
  const canApproveWarehouse = currentStatus === 'QC_COMPLETED' && hasRole(ROLES.WAREHOUSE_MANAGER);
  const displayedItems = canEditPickingPlan ? draftItems : order.items;

  const handleAddAllocation = (itemId) => {
    setDraftItems((previous) => previous.map((item) => (
      Number(item.id) === Number(itemId)
        ? {
            ...item,
            allocations: [...(item.allocations || []), outboundService.createEmptyAllocationDraft()],
          }
        : item
    )));
  };

  const handleRemoveAllocation = (itemId, allocationIndex) => {
    setDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;
      const nextAllocations = (item.allocations || []).filter((_, index) => index !== allocationIndex);
      return {
        ...item,
        allocations: nextAllocations.length ? nextAllocations : [outboundService.createEmptyAllocationDraft()],
      };
    }));
  };

  const handleAllocationChange = (itemId, allocationIndex, field, value) => {
    setDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;
      return {
        ...item,
        allocations: (item.allocations || []).map((allocation, index) => (
          index === allocationIndex
            ? {
                ...allocation,
                [field]: field === 'planned_qty' ? Number(value || 0) : value,
              }
            : allocation
        )),
      };
    }));
  };

  const handleCandidateSelect = (itemId, allocationIndex, inventoryId) => {
    const candidate = (pickingCandidates[itemId] || []).find(
      (row) => Number(row.inventory_id) === Number(inventoryId),
    );
    if (!candidate) {
      handleAllocationChange(itemId, allocationIndex, 'inventory_id', '');
      return;
    }

    setDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;

      const totalOtherQty = (item.allocations || []).reduce((sum, allocation, index) => (
        index === allocationIndex ? sum : sum + Number(allocation.planned_qty || 0)
      ), 0);
      const remainingQty = Math.max(0, Number(item.requested_qty || 0) - totalOtherQty);

      return {
        ...item,
        allocations: (item.allocations || []).map((allocation, index) => (
          index === allocationIndex
            ? outboundService.applyPickingCandidate(
                candidate,
                allocation.planned_qty ? Number(allocation.planned_qty) : remainingQty,
              )
            : allocation
        )),
      };
    }));
  };

  const hasInvalidDraft = draftItems.some((item) => {
    const plannedQty = (item.allocations || []).reduce(
      (sum, allocation) => sum + Number(allocation.planned_qty || 0),
      0,
    );
    const hasIncompleteAllocation = (item.allocations || []).some(
      (allocation) => Number(allocation.planned_qty || 0) > 0 && !allocation.inventory_id,
    );
    return hasIncompleteAllocation || plannedQty !== Number(item.requested_qty || 0);
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
        <div className="flex items-start gap-4">
          <button
            onClick={() => navigate('/outbound/delivery-orders')}
            className="mt-1 p-1.5 hover:bg-canvas-cream rounded-full transition-colors text-shade-50 hover:text-ink shrink-0"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Xuất kho</span>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">{order.do_number}</h1>
              {getStatusBadge(currentStatus)}
            </div>
            <p className="text-xs text-shade-50 font-light mt-1">
              Lập ngày: {order.document_date ? new Date(order.document_date).toLocaleDateString('vi-VN') : '-'}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3 ml-10 md:ml-0">
          {canOpenQc && (
            <button onClick={() => navigate(`/outbound/qc/${id}`)} className="btn-pill btn-pill-primary flex items-center gap-2">
              <PackageSearch className="w-4 h-4" /> Nhập kết quả lấy hàng & kiểm định
            </button>
          )}

          {canApproveQuality && (
            <button disabled={submitting} onClick={handleApproveQuality} className="btn-pill btn-pill-aloe flex items-center gap-2 disabled:opacity-50">
              <CheckCircle2 className="w-4 h-4" /> Duyệt kết quả QC
            </button>
          )}

          {canApproveWarehouse && (
            <>
              <button
                disabled={submitting}
                onClick={() => setRejectModal({ show: true, reason: '' })}
                className="btn-pill border border-danger-300 text-danger-600 hover:bg-danger-50 flex items-center gap-2 disabled:opacity-50"
              >
                <X className="w-4 h-4" /> Từ chối
              </button>
              <button disabled={submitting} onClick={handleApproveWarehouse} className="btn-pill btn-pill-aloe flex items-center gap-2 disabled:opacity-50">
                <CheckCircle2 className="w-4 h-4" /> Phê duyệt xuất kho
              </button>
            </>
          )}
        </div>
      </div>

      {currentStatus === 'WAITING_PICKING' && (
        <div className="bg-info-50 border border-info-200 rounded-lg p-4 flex items-center gap-3">
          <Clock className="w-4 h-4 text-info-700 shrink-0" />
          <p className="text-xs font-semibold text-info-900">
            Kế hoạch lấy hàng đã được lưu. Nhân viên kho nhập số lượng đã lấy và kết quả QC theo từng dòng phân bổ.
          </p>
        </div>
      )}

      {currentStatus === 'QC_PENDING_APPROVAL' && (
        <div className="bg-violet-50 border border-violet-200 rounded-lg p-4 flex items-center gap-3">
          <CheckCircle2 className="w-4 h-4 text-violet-700 shrink-0" />
          <p className="text-xs font-semibold text-violet-900">
            Nhân viên kho đã gửi kết quả lấy hàng & kiểm định. Thủ kho rà soát và duyệt chất lượng trước khi chuyển bước tiếp.
          </p>
        </div>
      )}

      {order.cancel_reason && (
        <div className="bg-danger-50 border border-danger-200 rounded-lg p-4 flex items-start gap-3">
          <AlertTriangle className="w-4 h-4 text-danger-600 shrink-0 mt-0.5" />
          <div>
            <p className="text-xs font-bold text-danger-700">Ghi chú trạng thái:</p>
            <p className="text-xs text-danger-600 mt-0.5">{order.cancel_reason}</p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-canvas-light rounded-lg border border-hairline-light p-5 shadow-level-3">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3 flex items-center gap-2">
            <MapPin className="w-3.5 h-3.5" /> Thông tin đại lý
          </h3>
          <div className="flex flex-col gap-1.5 text-xs">
            <p><span className="text-shade-50">Tên đại lý:</span> <span className="font-semibold text-ink">{order.dealer_name}</span></p>
            <p><span className="text-shade-50">Mã đại lý:</span> <span className="font-mono text-ink">{order.dealer_id}</span></p>
          </div>
        </div>

        <div className="bg-canvas-light rounded-lg border border-hairline-light p-5 shadow-level-3">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3 flex items-center gap-2">
            <Clock className="w-3.5 h-3.5" /> Tiến độ giao hàng
          </h3>
          <div className="flex flex-col gap-1.5 text-xs">
            <p>
              <span className="text-shade-50">Ngày giao dự kiến:</span>{' '}
              <span className="font-semibold text-ink">{order.expected_delivery_date ? new Date(order.expected_delivery_date).toLocaleDateString('vi-VN') : '-'}</span>
            </p>
            <p><span className="text-shade-50">Trạng thái backend:</span> <span className="font-semibold text-ink">{currentStatus}</span></p>
          </div>
        </div>

        <div className="bg-canvas-light rounded-lg border border-hairline-light p-5 shadow-level-3">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3 flex items-center gap-2">
            <FileText className="w-3.5 h-3.5" /> Ghi chú
          </h3>
          <p className="text-xs text-shade-60 italic bg-canvas-cream p-3 rounded border border-hairline-light min-h-[48px]">
            {order.notes || 'Không có ghi chú'}
          </p>
        </div>
      </div>

      {canEditPickingPlan && (
        <DeliveryOrderPickingPlanEditor
          items={draftItems}
          candidatesByItemId={pickingCandidates}
          submitting={submitting || loadingCandidates}
          disableSave={hasInvalidDraft}
          onAddAllocation={handleAddAllocation}
          onAllocationChange={handleAllocationChange}
          onCandidateSelect={handleCandidateSelect}
          onRemoveAllocation={handleRemoveAllocation}
          onSave={handleStartPicking}
        />
      )}

      {canEditPickingPlan && hasInvalidDraft && (
        <div className="bg-warning-50 border border-warning-200 rounded-lg p-4 flex items-center gap-3">
          <AlertTriangle className="w-4 h-4 text-warning-700 shrink-0" />
          <p className="text-xs font-semibold text-warning-900">
            Mỗi dòng hàng phải được phân bổ đủ số lượng yêu cầu và mọi allocation có số lượng lớn hơn 0 phải chọn inventory cụ thể trước khi lưu.
          </p>
        </div>
      )}

      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xs font-bold uppercase tracking-widest text-shade-40 flex items-center gap-2">
            <PackageSearch className="w-3.5 h-3.5" /> Chi tiết hàng hóa ({order.items.length} mặt hàng)
          </h2>
        </div>
        <PickingListTable items={displayedItems} />
      </div>

      <Modal isOpen={rejectModal.show} onClose={() => setRejectModal({ show: false, reason: '' })} title="Từ chối đơn xuất hàng" maxWidth="max-w-md">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2 text-danger-600 text-sm font-semibold">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            Hành động này trả đơn về trạng thái cần xử lý lại.
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-shade-60 mb-1.5">Lý do từ chối *</label>
            <textarea
              rows={4}
              placeholder="Nhập lý do từ chối đơn này..."
              value={rejectModal.reason}
              onChange={(event) => setRejectModal((prev) => ({ ...prev, reason: event.target.value }))}
              className="w-full bg-canvas-light text-ink text-sm px-3 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all resize-none min-h-[44px]"
            />
          </div>
          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
            <Button variant="outline-light" onClick={() => setRejectModal({ show: false, reason: '' })}>Hủy</Button>
            <button
              disabled={!rejectModal.reason.trim() || submitting}
              onClick={handleReject}
              className="rounded-pill font-medium transition-all duration-150 inline-flex items-center justify-center gap-2 text-sm focus:outline-none focus:ring-2 focus:ring-offset-2 px-6 py-2.5 bg-danger-600 text-white hover:bg-danger-700 focus:ring-danger-500 disabled:opacity-50 disabled:pointer-events-none"
            >
              Xác nhận từ chối
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
