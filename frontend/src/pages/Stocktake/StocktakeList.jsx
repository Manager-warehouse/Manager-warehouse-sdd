import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { ClipboardList, Plus, Eye, Play, XCircle, CheckCircle, XOctagon } from 'lucide-react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { stocktakeService } from '../../services/stocktake.service';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import Pagination from '../../components/common/Pagination';

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
  IN_PROGRESS: 'bg-info-50 text-info-700 border-info-200',
  PENDING_APPROVAL: 'bg-warning-50 text-warning-700 border-warning-200',
  APPROVED: 'bg-success-50 text-success-700 border-success-200',
  REJECTED: 'bg-danger-50 text-danger-700 border-danger-200',
  CANCELLED: 'bg-canvas-cream text-shade-50 border-hairline-light',
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
  const [confirmModal, setConfirmModal] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [totalItems, setTotalItems] = useState(0);

  const canCreate = hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN);
  const canApprove = hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN);

  const load = useCallback(async () => {
    if (!activeWarehouse?.id) return;
    setLoading(true);
    try {
      const data = await stocktakeService.getStockTakes(
        activeWarehouse.id,
        statusFilter || undefined,
        currentPage - 1,
        pageSize
      );
      setStocktakes(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalItems(data.totalElements || 0);
    } catch (err) {
      showToast?.('error', err.message || 'Không thể tải danh sách kiểm kê');
    } finally {
      setLoading(false);
    }
  }, [activeWarehouse?.id, statusFilter, currentPage, pageSize]);

  useEffect(() => { load(); }, [load]);

  const handlePageChange = (page) => {
    setCurrentPage(page);
  };

  const handlePageSizeChange = (size) => {
    setPageSize(size);
    setCurrentPage(1);
  };

  useEffect(() => {
    setCurrentPage(1);
  }, [statusFilter]);

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
    <div className="mobile-page">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Kiểm kê</span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">Kiểm kê hàng hóa</h1>
          <p className="text-xs text-shade-50 font-light mt-1">{activeWarehouse.name}</p>
        </div>
        {canCreate && (
          <Button variant="primary" icon={Plus} onClick={() => navigate('/stocktake/new')}>
            Tạo phiếu kiểm kê
          </Button>
        )}
      </div>

      {/* Filters */}
      <div className="flex border-b border-hairline-light overflow-x-auto whitespace-nowrap scrollbar-none mb-1 md:mb-2">
        {['', 'DRAFT', 'IN_PROGRESS', 'PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'CANCELLED'].map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`px-3 md:px-4 py-2.5 font-semibold text-xs transition-colors border-b-2 uppercase tracking-wide ${
              statusFilter === s
                ? 'border-ink text-ink'
                : 'border-transparent text-shade-50 hover:text-ink'
            }`}
          >
            {s ? STATUS_LABELS[s] : 'Tất cả'}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
        {loading ? (
          <div className="p-12 text-center text-shade-50 text-sm">Đang tải...</div>
        ) : stocktakes.length === 0 ? (
          <div className="p-12 text-center text-shade-50 text-sm">Không có phiếu kiểm kê nào.</div>
        ) : (
          <>
            {/* Desktop/tablet: table view */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-canvas-cream border-b border-hairline-light">
                    <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider text-shade-60">Số phiếu</th>
                    <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider text-shade-60">Ngày kiểm</th>
                    <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider text-shade-60">Người kiểm</th>
                    <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider text-shade-60">Trạng thái</th>
                    <th className="px-6 py-4 text-left text-xs font-semibold uppercase tracking-wider text-shade-60">Cấp duyệt</th>
                    <th className="px-6 py-4 text-right text-xs font-semibold uppercase tracking-wider text-shade-60">Chênh lệch</th>
                    <th className="px-6 py-4 text-right text-xs font-semibold uppercase tracking-wider text-shade-60">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {stocktakes.map((st) => (
                    <tr key={st.id} className="hover:bg-canvas-cream/50 transition-colors">
                      <td className="px-6 py-4 font-mono text-xs text-success-700 font-semibold">
                        {st.stock_take_number}
                      </td>
                      <td className="px-6 py-4 text-xs text-shade-50">{st.stock_take_date}</td>
                      <td className="px-6 py-4 text-xs text-shade-50">{st.conducted_by_name}</td>
                      <td className="px-6 py-4">
                        <Badge size="sm" colorClassName={STATUS_STYLES[st.status] || 'bg-canvas-cream text-shade-50 border-hairline-light'}>
                          {STATUS_LABELS[st.status] || st.status}
                        </Badge>
                      </td>
                      <td className="px-6 py-4 text-xs text-shade-50">
                        {st.approval_level ? APPROVAL_LABELS[st.approval_level] || st.approval_level : '—'}
                      </td>
                      <td className="px-6 py-4 text-xs text-right font-mono">
                        {st.total_variance_value !== 0 && st.total_variance_value !== null ? (
                          <span className={st.total_variance_value < 0 ? 'text-danger-600' : 'text-success-600'}>
                            {st.total_variance_value.toLocaleString('vi-VN')}₫
                          </span>
                        ) : '—'}
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex items-center justify-end gap-1.5">
                          <button
                            onClick={() => navigate(`/stocktake/${st.id}`)}
                            className="p-1 rounded-full text-shade-50 hover:text-success-700 hover:bg-aloe-10 transition-colors"
                            title="Xem chi tiết"
                          >
                            <Eye className="w-3.5 h-3.5" />
                          </button>
                          {st.status === 'DRAFT' && canCreate && (
                            <button
                              onClick={() => handleStart(st.id)}
                              className="p-1 rounded-full text-shade-50 hover:text-info-600 hover:bg-info-50 transition-colors"
                              title="Bắt đầu kiểm kê"
                            >
                              <Play className="w-3.5 h-3.5" />
                            </button>
                          )}
                          {(st.status === 'DRAFT' || st.status === 'IN_PROGRESS') && canCreate && (
                            <button
                              onClick={() => setConfirmModal({ action: 'cancel', id: st.id, label: 'hủy phiếu kiểm kê' })}
                              className="p-1 rounded-full text-shade-50 hover:text-danger-500 hover:bg-danger-50 transition-colors"
                              title="Hủy phiếu"
                            >
                              <XCircle className="w-3.5 h-3.5" />
                            </button>
                          )}
                          {st.status === 'PENDING_APPROVAL' && canApprove && (
                            <button
                              onClick={() => setConfirmModal({ action: 'approve', id: st.id, label: 'phê duyệt phiếu kiểm kê' })}
                              className="p-1 rounded-full text-shade-50 hover:text-success-600 hover:bg-success-50 transition-colors"
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
            </div>

            {/* Mobile: stacked card view */}
            <div className="flex flex-col gap-3 p-4 md:hidden">
              {stocktakes.map((st) => (
                <div key={st.id} className="rounded-lg border border-hairline-light bg-canvas-cream/30 overflow-hidden">
                  <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                    <span className="font-mono text-xs text-success-700 font-semibold">{st.stock_take_number}</span>
                    <Badge size="sm" colorClassName={STATUS_STYLES[st.status] || 'bg-canvas-cream text-shade-50 border-hairline-light'}>
                      {STATUS_LABELS[st.status] || st.status}
                    </Badge>
                  </div>
                  <div className="p-4 flex flex-col gap-2 text-xs">
                    <p className="text-shade-50">Ngày kiểm: <span className="font-medium text-ink">{st.stock_take_date}</span></p>
                    <p className="text-shade-50">Người kiểm: <span className="font-medium text-ink">{st.conducted_by_name}</span></p>
                    <p className="text-shade-50">Cấp duyệt: <span className="font-medium text-ink">{st.approval_level ? APPROVAL_LABELS[st.approval_level] || st.approval_level : '—'}</span></p>
                    <p className="text-shade-50">Chênh lệch: <span className={`font-mono font-medium ${st.total_variance_value < 0 ? 'text-danger-600' : st.total_variance_value > 0 ? 'text-success-600' : 'text-ink'}`}>
                      {st.total_variance_value !== 0 && st.total_variance_value !== null ? `${st.total_variance_value.toLocaleString('vi-VN')}₫` : '—'}
                    </span></p>
                  </div>
                  <div className="p-4 border-t border-hairline-light flex items-center justify-end gap-1.5">
                    <button
                      onClick={() => navigate(`/stocktake/${st.id}`)}
                      className="p-1.5 rounded-full text-shade-50 hover:text-success-700 hover:bg-aloe-10 transition-colors"
                      title="Xem chi tiết"
                    >
                      <Eye className="w-3.5 h-3.5" />
                    </button>
                    {st.status === 'DRAFT' && canCreate && (
                      <button
                        onClick={() => handleStart(st.id)}
                        className="p-1.5 rounded-full text-shade-50 hover:text-info-600 hover:bg-info-50 transition-colors"
                        title="Bắt đầu kiểm kê"
                      >
                        <Play className="w-3.5 h-3.5" />
                      </button>
                    )}
                    {(st.status === 'DRAFT' || st.status === 'IN_PROGRESS') && canCreate && (
                      <button
                        onClick={() => setConfirmModal({ action: 'cancel', id: st.id, label: 'hủy phiếu kiểm kê' })}
                        className="p-1.5 rounded-full text-shade-50 hover:text-danger-500 hover:bg-danger-50 transition-colors"
                        title="Hủy phiếu"
                      >
                        <XCircle className="w-3.5 h-3.5" />
                      </button>
                    )}
                    {st.status === 'PENDING_APPROVAL' && canApprove && (
                      <button
                        onClick={() => setConfirmModal({ action: 'approve', id: st.id, label: 'phê duyệt phiếu kiểm kê' })}
                        className="p-1.5 rounded-full text-shade-50 hover:text-success-600 hover:bg-success-50 transition-colors"
                        title="Phê duyệt"
                      >
                        <CheckCircle className="w-3.5 h-3.5" />
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>

            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              totalItems={totalItems}
              pageSize={pageSize}
              onPageChange={handlePageChange}
              onPageSizeChange={handlePageSizeChange}
              pageSizeOptions={[10, 25, 50]}
            />
          </>
        )}
      </div>

      {/* Confirm Modal */}
      {confirmModal && (
        <div className="fixed inset-0 bg-canvas-night/40 z-50 flex items-center justify-center p-4">
          <div className="bg-canvas-light rounded-lg shadow-level-3 p-6 max-w-sm w-full flex flex-col gap-4">
            <h3 className="text-base font-bold text-canvas-night capitalize">Xác nhận</h3>
            <p className="text-sm text-shade-40">Bạn có chắc muốn <strong>{confirmModal.label}</strong>?</p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setConfirmModal(null)}
                className="px-4 py-2 rounded-pill text-xs font-semibold border border-hairline-light text-shade-50 hover:bg-canvas-cream transition-colors"
              >
                Hủy
              </button>
              <button
                onClick={() => confirmModal.action === 'cancel' ? handleCancel(confirmModal.id) : handleApprove(confirmModal.id)}
                className={`px-4 py-2 rounded-pill text-xs font-semibold transition-colors ${
                  confirmModal.action === 'cancel' ? 'bg-danger-500 hover:bg-danger-600 text-white' : 'bg-aloe-10 hover:opacity-90 text-ink'
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
