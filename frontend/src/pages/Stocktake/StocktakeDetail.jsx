import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, ClipboardList, Play, CheckCircle, XCircle, Save } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { stocktakeService } from '../../services/stocktake.service';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';

const STATUS_LABELS = {
  DRAFT: 'Nháp',
  IN_PROGRESS: 'Đang kiểm',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
};

const STATUS_STYLES = {
  DRAFT: 'bg-canvas-cream text-shade-60 border-hairline-light',
  IN_PROGRESS: 'bg-blue-50 text-blue-700 border-blue-200',
  PENDING_APPROVAL: 'bg-amber-50 text-amber-700 border-amber-200',
  APPROVED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  REJECTED: 'bg-red-50 text-red-700 border-red-200',
  CANCELLED: 'bg-canvas-cream text-shade-50 border-hairline-light',
};

const StocktakeDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { hasRole } = useAuthStore();
  const { showToast } = useUiStore();

  const [stocktake, setStocktake] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  // count edits: itemId -> { actual_qty, is_employee_fault, notes }
  const [countEdits, setCountEdits] = useState({});

  // reject modal
  const [rejectModal, setRejectModal] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');

  // STOREKEEPER: tạo phiếu, bắt đầu đếm, nhập số liệu, hoàn tất & trình duyệt
  const canCount = hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN);
  // WAREHOUSE_MANAGER + CEO: chỉ duyệt/từ chối — không thao tác kiểm đếm
  // Approval is gated by both role AND the approval_level on the stocktake
  const canManagerApprove = (st) => st?.approval_level === 'MANAGER' && (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN));
  const canCeoApprove = (st) => st?.approval_level === 'CEO' && (hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN));
  const canAutoApprove = (st) => st?.approval_level === 'AUTO';
  const canApprove = (st) => canManagerApprove(st) || canCeoApprove(st) || canAutoApprove(st);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await stocktakeService.getStockTakeById(id);
      setStocktake(data);
      // Pre-fill edits with existing actual_qty
      const edits = {};
      (data.items || []).forEach((item) => {
        edits[item.id] = {
          actual_qty: item.actual_qty ?? '',
          is_employee_fault: item.is_employee_fault || false,
          notes: item.notes || '',
        };
      });
      setCountEdits(edits);
    } catch (err) {
      showToast?.('error', err.message || 'Không thể tải phiếu kiểm kê');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  const updateEdit = (itemId, field, value) => {
    setCountEdits((prev) => ({
      ...prev,
      [itemId]: { ...prev[itemId], [field]: value },
    }));
  };

  const handleSaveCount = async () => {
    // Validate: notes required when actual_qty differs from system_qty
    const itemsWithVarianceNoReason = (stocktake.items || []).filter((item) => {
      const edit = countEdits[item.id] || {};
      if (edit.actual_qty === '' || edit.actual_qty === undefined) return false;
      const actualNum = Number(edit.actual_qty);
      const hasVariance = actualNum !== item.system_qty;
      const noNotes = !edit.notes || !edit.notes.trim();
      return hasVariance && noNotes;
    });
    if (itemsWithVarianceNoReason.length > 0) {
      showToast?.('error', `Vui lòng nhập lý do chênh lệch cho ${itemsWithVarianceNoReason.length} dòng hàng có số lượng khác hệ thống`);
      return;
    }

    setSaving(true);
    try {
      const items = Object.entries(countEdits)
        .filter(([, v]) => v.actual_qty !== '')
        .map(([itemId, v]) => ({
          item_id: Number(itemId),
          actual_qty: Number(v.actual_qty),
          is_employee_fault: v.is_employee_fault,
          notes: v.notes || null,
        }));
      await stocktakeService.recordCount(id, items);
      showToast?.('success', 'Đã lưu số liệu kiểm kê');
      load();
    } catch (err) {
      showToast?.('error', err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleStart = async () => {
    try {
      await stocktakeService.startStockTake(id);
      showToast?.('success', 'Đã bắt đầu kiểm kê');
      load();
    } catch (err) {
      showToast?.('error', err.message);
    }
  };

  const handleComplete = async () => {
    try {
      const result = await stocktakeService.completeStockTake(id);
      showToast?.('success', result.status === 'APPROVED' ? 'Phiếu đã được tự động phê duyệt (chênh lệch nhỏ)' : 'Đã gửi phiếu chờ phê duyệt');
      load();
    } catch (err) {
      showToast?.('error', err.message);
    }
  };

  const handleApprove = async () => {
    try {
      if (stocktake.approval_level === 'CEO') {
        await stocktakeService.approveCeoStockTake(id);
      } else {
        await stocktakeService.approveStockTake(id);
      }
      showToast?.('success', 'Phiếu kiểm kê đã được phê duyệt');
      load();
    } catch (err) {
      showToast?.('error', err.message);
    }
  };

  const handleReject = async () => {
    if (!rejectionReason.trim()) {
      showToast?.('error', 'Vui lòng nhập lý do từ chối');
      return;
    }
    try {
      if (stocktake.approval_level === 'CEO') {
        await stocktakeService.rejectCeoStockTake(id, rejectionReason);
      } else {
        await stocktakeService.rejectStockTake(id, rejectionReason);
      }
      showToast?.('success', 'Đã từ chối phiếu kiểm kê');
      setRejectModal(false);
      setRejectionReason('');
      load();
    } catch (err) {
      showToast?.('error', err.message);
    }
  };

  if (loading) {
    return <div className="p-12 text-center text-shade-50">Đang tải...</div>;
  }

  if (!stocktake) {
    return <div className="p-12 text-center text-red-500">Không tìm thấy phiếu kiểm kê.</div>;
  }

  const isInProgress = stocktake.status === 'IN_PROGRESS';
  const isPendingApproval = stocktake.status === 'PENDING_APPROVAL';
  const isDraft = stocktake.status === 'DRAFT';

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-start justify-between flex-wrap gap-3">
        <div className="flex items-start gap-3">
          <button
            onClick={() => navigate('/stocktake')}
            className="p-2 rounded-pill text-shade-50 hover:bg-canvas-cream transition-colors mt-1 shrink-0"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Kiểm kê</span>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight font-mono">{stocktake.stock_take_number}</h1>
              <Badge size="sm" colorClassName={STATUS_STYLES[stocktake.status]}>
                {STATUS_LABELS[stocktake.status]}
              </Badge>
            </div>
            <p className="text-xs text-shade-50 font-light mt-1">{stocktake.warehouse_name}</p>
          </div>
        </div>

        {/* Action buttons */}
        <div className="flex items-center gap-2 flex-wrap">
          {isDraft && canCount && (
            <button
              onClick={handleStart}
              className="inline-flex items-center gap-2 px-4 py-2 rounded-pill bg-blue-600 text-onPrimary text-xs font-semibold hover:bg-blue-700 transition-colors disabled:opacity-50"
            >
              <Play className="w-3.5 h-3.5" />
              Bắt đầu kiểm kê
            </button>
          )}
          {isInProgress && canCount && (
            <>
              <Button
                variant="outline-light"
                onClick={handleSaveCount}
                disabled={saving}
                loading={saving}
              >
                <Save className="w-3.5 h-3.5" />
                {saving ? 'Đang lưu...' : 'Lưu số liệu'}
              </Button>
              <button
                onClick={handleComplete}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-pill bg-amber-500 text-onPrimary text-xs font-semibold hover:bg-amber-600 transition-colors disabled:opacity-50"
              >
                <CheckCircle className="w-3.5 h-3.5" />
                Hoàn tất & trình duyệt
              </button>
            </>
          )}
          {isPendingApproval && canApprove(stocktake) && (
            <>
              <Button
                variant="aloe"
                onClick={handleApprove}
              >
                <CheckCircle className="w-3.5 h-3.5" />
                Phê duyệt
              </Button>
              <button
                onClick={() => setRejectModal(true)}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-pill bg-red-600 text-onPrimary text-xs font-semibold hover:bg-red-700 transition-colors disabled:opacity-50"
              >
                <XCircle className="w-3.5 h-3.5" />
                Từ chối
              </button>
            </>
          )}
          {isPendingApproval && !canApprove(stocktake) && stocktake.approval_level === 'CEO' && (hasRole(ROLES.WAREHOUSE_MANAGER)) && (
            <span className="text-xs font-semibold text-amber-700 bg-amber-50 border border-amber-200 px-3 py-1.5 rounded-pill">
              Phiếu này yêu cầu CEO phê duyệt
            </span>
          )}
        </div>
      </div>

      {/* Info card */}
      <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 p-5">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <div>
            <p className="text-[10px] font-bold text-shade-50 uppercase tracking-wider mb-1">Ngày kiểm kê</p>
            <p className="font-semibold text-canvas-night">{stocktake.stock_take_date}</p>
          </div>
          <div>
            <p className="text-[10px] font-bold text-shade-50 uppercase tracking-wider mb-1">Người thực hiện</p>
            <p className="font-semibold text-canvas-night">{stocktake.conducted_by_name}</p>
          </div>
          <div>
            <p className="text-[10px] font-bold text-shade-50 uppercase tracking-wider mb-1">Kỳ kế toán</p>
            <p className="font-semibold text-canvas-night">{stocktake.accounting_period_name}</p>
          </div>
          <div>
            <p className="text-[10px] font-bold text-shade-50 uppercase tracking-wider mb-1">Cấp duyệt</p>
            <p className="font-semibold text-canvas-night">
              {stocktake.approval_level ? { AUTO: 'Tự động', MANAGER: 'Trưởng kho', CEO: 'CEO' }[stocktake.approval_level] : '—'}
            </p>
          </div>
          {stocktake.total_variance_value !== 0 && stocktake.total_variance_value !== null && (
            <div>
              <p className="text-[10px] font-bold text-shade-50 uppercase tracking-wider mb-1">Tổng chênh lệch</p>
              <p className={`font-bold text-base ${stocktake.total_variance_value < 0 ? 'text-red-600' : 'text-green-600'}`}>
                {stocktake.total_variance_value.toLocaleString('vi-VN')}₫
              </p>
            </div>
          )}
          {stocktake.rejection_reason && (
            <div className="col-span-2">
              <p className="text-[10px] font-bold text-shade-50 uppercase tracking-wider mb-1">Lý do từ chối</p>
              <p className="text-red-600 font-semibold">{stocktake.rejection_reason}</p>
            </div>
          )}
          {stocktake.approved_by_name && (
            <div>
              <p className="text-[10px] font-bold text-shade-50 uppercase tracking-wider mb-1">Người duyệt</p>
              <p className="font-semibold text-canvas-night">{stocktake.approved_by_name}</p>
            </div>
          )}
        </div>
      </div>

      {/* Items table */}
      {stocktake.items && stocktake.items.length > 0 && (
        <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
          <div className="px-5 py-3 border-b border-hairline-light bg-canvas-cream">
            <h2 className="text-xs font-bold text-shade-50 uppercase tracking-wider">Dòng hàng kiểm kê</h2>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-hairline-light">
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-shade-60">SKU / Tên</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-shade-60">Lô</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-shade-60">Vị trí</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-shade-60">Hệ thống</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-shade-60">Thực tế</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold uppercase tracking-wider text-shade-60">Chênh lệch</th>
                  <th className="px-4 py-3 text-center text-xs font-semibold uppercase tracking-wider text-shade-60">Lỗi NV</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold uppercase tracking-wider text-shade-60">
                    Lý do chênh lệch {isInProgress && <span className="text-red-500">*</span>}
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {stocktake.items.map((item) => {
                  const edit = countEdits[item.id] || {};
                  const actualNum = edit.actual_qty !== '' && edit.actual_qty !== undefined ? Number(edit.actual_qty) : null;
                  const variance = actualNum !== null ? actualNum - item.system_qty : item.variance_qty;

                  return (
                    <tr key={item.id} className="hover:bg-canvas-cream/40">
                      <td className="px-4 py-3">
                        <p className="text-xs font-mono text-shade-50">{item.product_sku}</p>
                        <p className="text-sm font-semibold text-canvas-night">{item.product_name}</p>
                      </td>
                      <td className="px-4 py-3 text-xs font-mono text-shade-50">{item.batch_number}</td>
                      <td className="px-4 py-3 text-xs font-mono text-shade-50">{item.location_code}</td>
                      <td className="px-4 py-3 text-right text-sm font-semibold text-canvas-night">
                        {item.system_qty.toLocaleString('vi-VN')}
                      </td>
                      <td className="px-4 py-3 text-right">
                        {isInProgress ? (
                          <input
                            type="number"
                            min="0"
                            step="1"
                            value={edit.actual_qty ?? ''}
                            onChange={(e) => updateEdit(item.id, 'actual_qty', e.target.value)}
                            placeholder="0"
                            className="w-20 px-2 py-1 rounded-lg border border-hairline-light focus:border-aloe-40 text-sm text-right outline-none"
                          />
                        ) : (
                          <span className={`text-sm font-semibold ${item.actual_qty === null ? 'text-shade-50 italic' : 'text-canvas-night'}`}>
                            {item.actual_qty !== null && item.actual_qty !== undefined ? item.actual_qty.toLocaleString('vi-VN') : '—'}
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-right">
                        {variance !== null && variance !== undefined && variance !== 0 ? (
                          <span className={`text-sm font-bold ${variance < 0 ? 'text-red-600' : 'text-green-600'}`}>
                            {variance > 0 ? '+' : ''}{variance.toLocaleString('vi-VN')}
                          </span>
                        ) : (
                          <span className="text-sm text-shade-50">—</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-center">
                        {isInProgress ? (
                          <input
                            type="checkbox"
                            checked={edit.is_employee_fault || false}
                            onChange={(e) => updateEdit(item.id, 'is_employee_fault', e.target.checked)}
                            className="w-4 h-4 accent-red-500"
                          />
                        ) : (
                          item.is_employee_fault ? (
                            <span className="text-red-600 font-bold text-xs">✓</span>
                          ) : (
                            <span className="text-shade-30 text-xs">—</span>
                          )
                        )}
                      </td>
                      <td className="px-4 py-3">
                        {isInProgress ? (
                          <input
                            type="text"
                            value={edit.notes || ''}
                            onChange={(e) => updateEdit(item.id, 'notes', e.target.value)}
                            placeholder={variance !== null && variance !== 0 ? 'Bắt buộc nhập lý do...' : 'Ghi chú (không bắt buộc)'}
                            className={`w-full px-2 py-1 rounded-lg border text-xs outline-none ${
                              variance !== null && variance !== 0 && !edit.notes?.trim()
                                ? 'border-red-300 bg-red-50 focus:border-red-400'
                                : 'border-hairline-light focus:border-aloe-40'
                            }`}
                          />
                        ) : (
                          <span className={`text-xs ${item.notes ? 'text-canvas-night' : 'text-shade-30 italic'}`}>
                            {item.notes || '—'}
                          </span>
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

      {/* Variance Report Panel — shown to approver when PENDING_APPROVAL */}
      {isPendingApproval && (() => {
        const variantItems = (stocktake.items || []).filter(
          (it) => it.variance_qty !== null && it.variance_qty !== 0
        );
        if (variantItems.length === 0) return null;
        return (
          <div className="bg-amber-50 border border-amber-200 rounded-lg overflow-hidden">
            <div className="px-5 py-3 border-b border-amber-200 bg-amber-100 flex items-center justify-between">
              <h2 className="text-xs font-bold text-amber-800 uppercase tracking-wider">
                Báo cáo chênh lệch — {variantItems.length} dòng hàng cần phê duyệt
              </h2>
              <span className={`text-xs font-bold px-2 py-0.5 rounded-full ${
                stocktake.approval_level === 'CEO'
                  ? 'bg-red-100 text-red-700'
                  : 'bg-amber-200 text-amber-800'
              }`}>
                Cấp duyệt: {stocktake.approval_level === 'CEO' ? 'CEO' : 'Trưởng kho'}
              </span>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-amber-200 bg-amber-50">
                    <th className="px-4 py-2 text-left text-xs font-bold text-amber-700 uppercase">SKU / Tên</th>
                    <th className="px-4 py-2 text-right text-xs font-bold text-amber-700 uppercase">Hệ thống</th>
                    <th className="px-4 py-2 text-right text-xs font-bold text-amber-700 uppercase">Thực tế</th>
                    <th className="px-4 py-2 text-right text-xs font-bold text-amber-700 uppercase">Chênh lệch</th>
                    <th className="px-4 py-2 text-center text-xs font-bold text-amber-700 uppercase">Lỗi NV</th>
                    <th className="px-4 py-2 text-left text-xs font-bold text-amber-700 uppercase">Lý do</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-amber-100">
                  {variantItems.map((it) => (
                    <tr key={it.id} className={it.is_employee_fault ? 'bg-red-50' : ''}>
                      <td className="px-4 py-2">
                        <p className="text-xs font-mono text-shade-50">{it.product_sku}</p>
                        <p className="text-sm font-semibold text-canvas-night">{it.product_name}</p>
                      </td>
                      <td className="px-4 py-2 text-right text-sm font-semibold">{it.system_qty?.toLocaleString('vi-VN')}</td>
                      <td className="px-4 py-2 text-right text-sm font-semibold">{it.actual_qty?.toLocaleString('vi-VN')}</td>
                      <td className="px-4 py-2 text-right">
                        <span className={`text-sm font-bold ${it.variance_qty < 0 ? 'text-red-600' : 'text-green-600'}`}>
                          {it.variance_qty > 0 ? '+' : ''}{it.variance_qty?.toLocaleString('vi-VN')}
                        </span>
                      </td>
                      <td className="px-4 py-2 text-center">
                        {it.is_employee_fault
                          ? <span className="text-red-600 font-bold text-xs bg-red-100 px-1.5 py-0.5 rounded">Có</span>
                          : <span className="text-shade-40 text-xs">Không</span>}
                      </td>
                      <td className="px-4 py-2 text-xs text-canvas-night">
                        {it.notes || <span className="text-shade-30 italic">Chưa có lý do</span>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {stocktake.is_employee_fault && (
              <div className="px-5 py-3 border-t border-amber-200 bg-red-50 text-xs text-red-700 font-semibold">
                ⚠ Phiếu này được đánh dấu có lỗi nhân viên — yêu cầu CEO phê duyệt
              </div>
            )}
          </div>
        );
      })()}

      {/* Reject Modal */}
      {rejectModal && (
        <div className="fixed inset-0 bg-canvas-night/40 z-50 flex items-center justify-center p-4">
          <div className="bg-canvas-light rounded-lg shadow-level-3 p-6 max-w-sm w-full flex flex-col gap-4">
            <h3 className="text-base font-bold text-canvas-night">Từ chối phiếu kiểm kê</h3>
            <div className="space-y-1.5">
              <label className="block text-xs font-semibold text-shade-40 uppercase tracking-wider">
                Lý do từ chối <span className="text-red-500">*</span>
              </label>
              <textarea
                rows={3}
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                placeholder="Nhập lý do từ chối..."
                className="w-full px-3 py-2 rounded-md border border-hairline-light focus:border-red-400 text-sm outline-none resize-none"
              />
            </div>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => { setRejectModal(false); setRejectionReason(''); }}
                className="px-4 py-2 rounded-pill text-xs font-semibold border border-hairline-light text-shade-50 hover:bg-canvas-cream transition-colors"
              >
                Hủy
              </button>
              <button
                onClick={handleReject}
                className="px-4 py-2 rounded-pill text-xs font-semibold bg-red-500 text-white hover:bg-red-600 transition-colors"
              >
                Xác nhận từ chối
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default StocktakeDetail;
