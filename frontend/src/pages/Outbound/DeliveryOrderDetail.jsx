import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertTriangle, ArrowLeft, CheckCircle2, CheckSquare, Clock, FileText, Loader2, MapPin, PackageSearch, Play, X } from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import PickingListTable from '../../components/warehouse/PickingListTable';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import { ROLES } from '../../utils/constants';

const DO_STATUS_MAP = {
  NEW: { label: 'Mới', color: 'bg-zinc-100 text-zinc-800 border-zinc-200' },
  PICKING: { label: 'Đang soạn', color: 'bg-blue-50 text-blue-700 border-blue-200' },
  READY_TO_SHIP: { label: 'Chờ vận chuyển', color: 'bg-amber-50 text-amber-700 border-amber-200' },
  IN_TRANSIT: { label: 'Đang giao', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  DELIVERED: { label: 'Đã giao', color: 'bg-aloe-10 text-emerald-900 border-emerald-300' },
  RETURNED: { label: 'Hoàn trả', color: 'bg-orange-50 text-orange-700 border-orange-200' },
  CANCELLED: { label: 'Đã hủy', color: 'bg-red-50 text-red-700 border-red-200' },
};

const getStatusBadge = (status) => {
  const base = 'text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase tracking-wider whitespace-nowrap';
  const { label, color } = DO_STATUS_MAP[status] ?? { label: status, color: 'bg-zinc-100 text-zinc-800 border-zinc-200' };
  return <span className={`${base} ${color}`}>{label}</span>;
};

export default function DeliveryOrderDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();
  const { hasRole } = useAuthStore();

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [pickedItems, setPickedItems] = useState([]);
  const [rejectModal, setRejectModal] = useState({ show: false, reason: '' });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchOrder();
  }, [id]);

  const fetchOrder = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getDeliveryOrderById(id);
      setOrder(data);
      setPickedItems((data.items || []).map((item) => ({
        id: item.id,
        issued_qty: item.issued_qty || item.picked_qty || 0,
        serial_number: item.serial_number || '',
      })));
    } catch (error) {
      addToast(error.message || 'Không tìm thấy đơn xuất hàng', 'error');
      navigate('/outbound/delivery-orders');
    } finally {
      setLoading(false);
    }
  };

  const handleStartPicking = async () => {
    setSubmitting(true);
    try {
      await outboundService.startPicking(id);
      addToast('Đã lưu kế hoạch lấy hàng', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Không thể bắt đầu soạn hàng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePickItem = (itemId, qty, serial = '') => {
    setPickedItems((prev) => prev.map((item) => (
      item.id === itemId ? { ...item, issued_qty: qty, serial_number: serial } : item
    )));
  };

  const handleCompletePicking = async () => {
    const invalid = pickedItems.some((picked) => {
      const original = order.items.find((item) => item.id === picked.id);
      return !original || Number(picked.issued_qty) < 0 || Number(picked.issued_qty) > Number(original.requested_qty);
    });
    if (invalid) {
      addToast('Số lượng lấy không hợp lệ', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await outboundService.completePicking(id, pickedItems);
      addToast('Hoàn tất soạn hàng thành công', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Lỗi khi hoàn tất soạn hàng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleApproveQC = async () => {
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

  const qcDone = Boolean(order.qc_completed_at) || ['QC_PENDING_APPROVAL', 'QC_COMPLETED', 'WAREHOUSE_APPROVED'].includes(order.raw_status);
  const needsQualityApproval = order.raw_status === 'QC_PENDING_APPROVAL';
  const needsWarehouseApproval = ['QC_COMPLETED', 'PICKING'].includes(order.raw_status) || (qcDone && !needsQualityApproval);
  const allItemsPicked = pickedItems.length > 0 && pickedItems.every((picked) => {
    const original = order.items.find((item) => item.id === picked.id);
    return original && Number(picked.issued_qty) === Number(original.requested_qty);
  });
  const canPick = order.status === 'PICKING' && (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN));

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
        <div className="flex items-start gap-4">
          <button
            onClick={() => navigate('/outbound/delivery-orders')}
            className="mt-1 p-1.5 hover:bg-zinc-200 rounded-full transition-colors text-shade-50 hover:text-ink shrink-0"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Xuất kho</span>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">{order.do_number}</h1>
              {getStatusBadge(order.status)}
            </div>
            <p className="text-xs text-shade-50 font-light mt-1">
              Lập ngày: {order.document_date ? new Date(order.document_date).toLocaleDateString('vi-VN') : '-'}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3 ml-10 md:ml-0">
          {order.status === 'NEW' && (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN)) && (
            <button disabled={submitting} onClick={handleStartPicking} className="btn-pill btn-pill-primary flex items-center gap-2 disabled:opacity-50">
              <Play className="w-4 h-4" /> Bắt đầu soạn hàng
            </button>
          )}

          {canPick && !qcDone && (
            <button
              disabled={!allItemsPicked || submitting}
              onClick={handleCompletePicking}
              title={!allItemsPicked ? 'Nhập đầy đủ số lượng thực lấy trong bảng bên dưới' : ''}
              className="btn-pill btn-pill-aloe flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <CheckSquare className="w-4 h-4" /> Xác nhận hoàn tất
            </button>
          )}

          {order.status === 'PICKING' && (hasRole(ROLES.WAREHOUSE_STAFF) || hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN)) && allItemsPicked && !qcDone && (
            <button onClick={() => navigate(`/outbound/qc/${id}`)} className="btn-pill btn-pill-primary flex items-center gap-2">
              <PackageSearch className="w-4 h-4" /> Kiểm tra QC
            </button>
          )}

          {order.status === 'PICKING' && (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN)) && needsQualityApproval && (
            <button disabled={submitting} onClick={handleApproveQuality} className="btn-pill btn-pill-aloe flex items-center gap-2 disabled:opacity-50">
              <CheckCircle2 className="w-4 h-4" /> Xác nhận chất lượng
            </button>
          )}

          {order.status === 'PICKING' && (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) && qcDone && needsWarehouseApproval && (
            <>
              <button
                disabled={submitting}
                onClick={() => setRejectModal({ show: true, reason: '' })}
                className="btn-pill border border-red-300 text-red-600 hover:bg-red-50 flex items-center gap-2 disabled:opacity-50"
              >
                <X className="w-4 h-4" /> Từ chối
              </button>
              <button disabled={submitting} onClick={handleApproveQC} className="btn-pill btn-pill-aloe flex items-center gap-2 disabled:opacity-50">
                <CheckCircle2 className="w-4 h-4" /> Phê duyệt xuất kho
              </button>
            </>
          )}
        </div>
      </div>

      {qcDone && order.status === 'PICKING' && (
        <div className="bg-aloe-10 border border-emerald-300 rounded-lg p-4 flex items-center gap-3">
          <CheckCircle2 className="w-4 h-4 text-emerald-700 shrink-0" />
          <p className="text-xs font-semibold text-emerald-900">
            QC Outbound đã hoàn tất. Đơn đang chờ Trưởng kho phê duyệt xuất.
          </p>
        </div>
      )}

      {order.cancel_reason && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-start gap-3">
          <AlertTriangle className="w-4 h-4 text-red-600 shrink-0 mt-0.5" />
          <div>
            <p className="text-xs font-bold text-red-700">Ghi chú trạng thái:</p>
            <p className="text-xs text-red-600 mt-0.5">{order.cancel_reason}</p>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-white rounded-lg border border-hairline-light p-5 shadow-sm">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3 flex items-center gap-2">
            <MapPin className="w-3.5 h-3.5" /> Thông tin đại lý
          </h3>
          <div className="space-y-1.5 text-xs">
            <p><span className="text-shade-50">Tên đại lý:</span> <span className="font-semibold text-ink">{order.dealer_name}</span></p>
            <p><span className="text-shade-50">Mã đại lý:</span> <span className="font-mono text-ink">{order.dealer_id}</span></p>
          </div>
        </div>
        <div className="bg-white rounded-lg border border-hairline-light p-5 shadow-sm">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3 flex items-center gap-2">
            <Clock className="w-3.5 h-3.5" /> Tiến độ giao hàng
          </h3>
          <div className="space-y-1.5 text-xs">
            <p>
              <span className="text-shade-50">Ngày giao dự kiến:</span>{' '}
              <span className="font-semibold text-ink">{order.expected_delivery_date ? new Date(order.expected_delivery_date).toLocaleDateString('vi-VN') : '-'}</span>
            </p>
            <p><span className="text-shade-50">Trạng thái gốc:</span> <span className="font-semibold text-ink">{order.raw_status}</span></p>
          </div>
        </div>
        <div className="bg-white rounded-lg border border-hairline-light p-5 shadow-sm">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3 flex items-center gap-2">
            <FileText className="w-3.5 h-3.5" /> Ghi chú
          </h3>
          <p className="text-xs text-shade-60 italic bg-zinc-50 p-3 rounded border border-hairline-light min-h-[48px]">
            {order.notes || 'Không có ghi chú'}
          </p>
        </div>
      </div>

      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xs font-bold uppercase tracking-widest text-shade-40 flex items-center gap-2">
            <PackageSearch className="w-3.5 h-3.5" /> Chi tiết hàng hóa ({order.items.length} mặt hàng)
          </h2>
          {canPick && !qcDone && (
            <span className="text-[10px] font-semibold uppercase tracking-wider text-blue-700 bg-blue-50 px-3 py-1 rounded-pill border border-blue-200">
              Đang soạn. Nhập số lượng thực lấy rồi bấm Xác nhận.
            </span>
          )}
        </div>
        <PickingListTable
          items={order.items}
          isPicking={canPick && !qcDone}
          pickedItems={pickedItems}
          onPickItem={handlePickItem}
        />
      </div>

      <Modal isOpen={rejectModal.show} onClose={() => setRejectModal({ show: false, reason: '' })} title="Từ chối đơn xuất hàng" maxWidth="max-w-md">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2 text-red-600 text-sm font-semibold">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            Hành động này trả đơn về trạng thái cần xử lý lại.
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-shade-60 mb-1.5">Lý do từ chối *</label>
            <textarea
              rows={4}
              placeholder="Nhập lý do từ chối đơn này..."
              value={rejectModal.reason}
              onChange={(event) => setRejectModal({ ...rejectModal, reason: event.target.value })}
              className="w-full bg-canvas-light text-ink text-sm px-3 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all resize-none min-h-[44px]"
            />
          </div>
          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
            <Button variant="outline-light" onClick={() => setRejectModal({ show: false, reason: '' })}>Hủy</Button>
            <button
              disabled={!rejectModal.reason.trim() || submitting}
              onClick={handleReject}
              className="rounded-pill font-medium transition-all duration-150 inline-flex items-center justify-center gap-2 text-sm focus:outline-none focus:ring-2 focus:ring-offset-2 px-6 py-2.5 bg-red-600 text-white hover:bg-red-700 focus:ring-red-500 disabled:opacity-50 disabled:pointer-events-none"
            >
              Xác nhận từ chối
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
