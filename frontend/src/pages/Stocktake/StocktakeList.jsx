import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ClipboardList, Plus, Eye, Play, XCircle, CheckCircle, XOctagon } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { stocktakeService } from '../../services/stocktake.service';
import { ROLES } from '../../utils/constants';

const STATUS_LABELS = {
  DRAFT: 'Nháp',
  IN_PROGRESS: 'Đang kiểm',
  PENDING_APPROVAL: 'Chờ duyệt',
  APPROVED: 'Đã duyệt',
  REJECTED: 'Từ chối',
  CANCELLED: 'Đã hủy',
};

const STATUS_STYLES = {
  DRAFT: 'bg-zinc-100 text-zinc-600',
  IN_PROGRESS: 'bg-blue-100 text-blue-700',
  PENDING_APPROVAL: 'bg-amber-100 text-amber-700',
  APPROVED: 'bg-green-100 text-green-700',
  REJECTED: 'bg-red-100 text-red-700',
  CANCELLED: 'bg-zinc-100 text-zinc-400',
};

const APPROVAL_LABELS = {
  AUTO: 'Tự động',
  MANAGER: 'Trưởng kho',
  CEO: 'CEO',
};

const StocktakeList = () => {
  const navigate = useNavigate();
  const { user, hasRole, activeWarehouse } = useAuthStore();
  const { showToast } = useUiStore();

  const [stocktakes, setStocktakes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');
  const [confirmModal, setConfirmModal] = useState(null); // { action, id, label }

  const canCreate = hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN);
  const canApprove = hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN);

  const load = useCallback(async () => {
    if (!activeWarehouse?.id) return;
    setLoading(true);
    try {
      const data = await stocktakeService.getStockTakes(activeWarehouse.id, statusFilter || undefined);
      setStocktakes(data);
    } catch (err) {
      showToast?.('error', err.message || 'Không thể tải danh sách kiểm kê');
    } finally {
      setLoading(false);
    }
  }, [activeWarehouse?.id, statusFilter]);

  useEffect(() => { load(); }, [load]);

  const handleStart = async (id) => {
    try {
      await stocktakeService.startStockTake(id);
      showToast?.('success', 'Đã bắt đầu kiểm kê');
      load();
    } catch (err) {
      showToast?.('error', err.message);
    }
  };

  const handleCancel = async (id) => {
    try {
      await stocktakeService.cancelStockTake(id);
      showToast?.('success', 'Đã hủy phiếu kiểm kê');
      load();
    } catch (err) {
      showToast?.('error', err.message);
    } finally {
      setConfirmModal(null);
    }
  };

  const handleApprove = async (id) => {
    try {
      if (hasRole(ROLES.CEO)) {
        await stocktakeService.approveCeoStockTake(id);
      } else {
        await stocktakeService.approveStockTake(id);
      }
      showToast?.('success', 'Phiếu kiểm kê đã được phê duyệt');
      load();
    } catch (err) {
      showToast?.('error', err.message);
    } finally {
      setConfirmModal(null);
    }
  };

  if (!activeWarehouse) {
    return (
      <div className="p-8 text-center text-shade-50">
        Vui lòng chọn kho để xem danh sách kiểm kê.
      </div>
    );
  }

  return (
    <div className="p-6 w-full space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <ClipboardList className="w-6 h-6 text-aloe-40" />
          <div>
            <h1 className="text-xl font-bold text-canvas-night">Kiểm kê hàng hóa</h1>
            <p className="text-xs text-shade-50">{activeWarehouse.name}</p>
          </div>
        </div>
        {canCreate && (
          <button
            onClick={() => navigate('/stocktake/new')}
            className="flex items-center gap-2 px-4 py-2 rounded-pill bg-black text-white text-xs font-semibold hover:bg-zinc-800 transition-colors"
          >
            <Plus className="w-4 h-4" />
            Tạo phiếu kiểm kê
          </button>
        )}
      </div>

      {/* Filters */}
      <div className="flex items-center gap-3 flex-wrap">
        <span className="text-xs font-semibold text-shade-50 uppercase tracking-wider">Lọc:</span>
        {['', 'DRAFT', 'IN_PROGRESS', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED'].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 py-1.5 rounded-pill text-xs font-semibold transition-colors border ${
              statusFilter === s
                ? 'bg-black text-white border-black'
                : 'border-hairline text-shade-50 hover:border-black hover:text-black'
            }`}
          >
            {s ? STATUS_LABELS[s] : 'Tất cả'}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-white rounded-2xl border border-hairline shadow-xs overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-shade-50 text-sm">Đang tải...</div>
        ) : stocktakes.length === 0 ? (
          <div className="p-12 text-center text-shade-50 text-sm">Không có phiếu kiểm kê nào.</div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-canvas-cream border-b border-hairline">
                <th className="px-4 py-3 text-left text-xs font-bold text-shade-50 uppercase tracking-wider">Số phiếu</th>
                <th className="px-4 py-3 text-left text-xs font-bold text-shade-50 uppercase tracking-wider">Ngày kiểm</th>
                <th className="px-4 py-3 text-left text-xs font-bold text-shade-50 uppercase tracking-wider">Người kiểm</th>
                <th className="px-4 py-3 text-left text-xs font-bold text-shade-50 uppercase tracking-wider">Trạng thái</th>
                <th className="px-4 py-3 text-left text-xs font-bold text-shade-50 uppercase tracking-wider">Cấp duyệt</th>
                <th className="px-4 py-3 text-right text-xs font-bold text-shade-50 uppercase tracking-wider">Chênh lệch</th>
                <th className="px-4 py-3 text-center text-xs font-bold text-shade-50 uppercase tracking-wider">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-hairline">
              {stocktakes.map((st) => (
                <tr key={st.id} className="hover:bg-canvas-cream/50 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs text-aloe-50 font-semibold">
                    {st.stock_take_number}
                  </td>
                  <td className="px-4 py-3 text-xs text-shade-30">{st.stock_take_date}</td>
                  <td className="px-4 py-3 text-xs text-shade-30">{st.conducted_by_name}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${STATUS_STYLES[st.status] || 'bg-zinc-100 text-zinc-500'}`}>
                      {STATUS_LABELS[st.status] || st.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-shade-50">
                    {st.approval_level ? APPROVAL_LABELS[st.approval_level] || st.approval_level : '—'}
                  </td>
                  <td className="px-4 py-3 text-xs text-right font-mono">
                    {st.total_variance_value !== 0 && st.total_variance_value !== null ? (
                      <span className={st.total_variance_value < 0 ? 'text-red-600' : 'text-green-600'}>
                        {st.total_variance_value.toLocaleString('vi-VN')}₫
                      </span>
                    ) : '—'}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-center gap-1.5">
                      <button
                        onClick={() => navigate(`/stocktake/${st.id}`)}
                        className="p-1.5 rounded-lg text-shade-50 hover:text-aloe-50 hover:bg-aloe-10 transition-colors"
                        title="Xem chi tiết"
                      >
                        <Eye className="w-3.5 h-3.5" />
                      </button>
                      {st.status === 'DRAFT' && canCreate && (
                        <button
                          onClick={() => handleStart(st.id)}
                          className="p-1.5 rounded-lg text-shade-50 hover:text-blue-600 hover:bg-blue-50 transition-colors"
                          title="Bắt đầu kiểm kê"
                        >
                          <Play className="w-3.5 h-3.5" />
                        </button>
                      )}
                      {(st.status === 'DRAFT' || st.status === 'IN_PROGRESS') && canCreate && (
                        <button
                          onClick={() => setConfirmModal({ action: 'cancel', id: st.id, label: 'hủy phiếu kiểm kê' })}
                          className="p-1.5 rounded-lg text-shade-50 hover:text-red-500 hover:bg-red-50 transition-colors"
                          title="Hủy phiếu"
                        >
                          <XCircle className="w-3.5 h-3.5" />
                        </button>
                      )}
                      {st.status === 'PENDING_APPROVAL' && canApprove && (
                        <button
                          onClick={() => setConfirmModal({ action: 'approve', id: st.id, label: 'phê duyệt phiếu kiểm kê' })}
                          className="p-1.5 rounded-lg text-shade-50 hover:text-green-600 hover:bg-green-50 transition-colors"
                          title="Phê duyệt"
                        >
                          <CheckCircle className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Confirm Modal */}
      {confirmModal && (
        <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl shadow-xl p-6 max-w-sm w-full space-y-4">
            <h3 className="text-base font-bold text-canvas-night capitalize">Xác nhận</h3>
            <p className="text-sm text-shade-40">Bạn có chắc muốn <strong>{confirmModal.label}</strong>?</p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setConfirmModal(null)}
                className="px-4 py-2 rounded-pill text-xs font-semibold border border-hairline text-shade-50 hover:bg-canvas-cream transition-colors"
              >
                Hủy
              </button>
              <button
                onClick={() => confirmModal.action === 'cancel' ? handleCancel(confirmModal.id) : handleApprove(confirmModal.id)}
                className={`px-4 py-2 rounded-pill text-xs font-semibold text-white transition-colors ${
                  confirmModal.action === 'cancel' ? 'bg-red-500 hover:bg-red-600' : 'bg-aloe-40 hover:bg-aloe-50'
                }`}
              >
                Xác nhận
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default StocktakeList;
