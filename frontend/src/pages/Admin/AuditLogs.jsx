import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/admin.service';
import { masterDataService } from '../../services/masterData.service';
import { useUiStore } from '../../stores/ui.store';
import Table from '../../components/common/Table';
import Pagination from '../../components/common/Pagination';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { formatDate } from '../../utils/format';
import { RotateCcw, Eye } from 'lucide-react';

const AuditLogs = () => {
  const { addToast } = useUiStore();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);

  // Pagination States
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 30;
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  // Filters State
  const [selectedWarehouseId, setSelectedWarehouseId] = useState('');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [warehouses, setWarehouses] = useState([]);

  // Detail Modal States
  const [auditDetailOpen, setAuditDetailOpen] = useState(false);
  const [auditDetail, setAuditDetail] = useState(null);
  const [auditDetailLoading, setAuditDetailLoading] = useState(false);

  // Load Audit Logs
  const loadLogs = async () => {
    setLoading(true);
    try {
      const response = await adminService.getAuditLogs({
        page: currentPage,
        pageSize,
        warehouse_id: selectedWarehouseId || undefined,
        from: fromDate || undefined,
        to: toDate || undefined,
      });
      const pageData = response.data || response || [];
      const rows = Array.isArray(pageData) ? pageData : pageData.data || [];
      setLogs(rows);
      setTotalItems(pageData.totalItems ?? rows.length);
      const backendTotalPages = pageData.totalPages ?? (pageData.hasNext ? currentPage + 1 : currentPage);
      const hasTimeFilter = Boolean(fromDate || toDate);
      setTotalPages(hasTimeFilter ? backendTotalPages : Math.min(backendTotalPages, 50));
    } catch (err) {
      addToast('Không thể tải nhật ký hoạt động', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, [currentPage, pageSize, selectedWarehouseId, fromDate, toDate]);

  useEffect(() => {
    const loadWarehouses = async () => {
      try {
        const data = await masterDataService.getWarehouses();
        const rows = Array.isArray(data) ? data : [];
        setWarehouses(rows.filter((warehouse) => warehouse.is_active !== false));
      } catch {
        addToast('Không thấy tất cả danh sách kho', 'error');
      }
    };

    loadWarehouses();
  }, [addToast]);

  const handleResetFilters = () => {
    setSelectedWarehouseId('');
    setFromDate('');
    setToDate('');
    setCurrentPage(1);
  };

  const updateWarehouseFilter = (value) => {
    setSelectedWarehouseId(value);
    setCurrentPage(1);
  };

  const updateFromDateFilter = (value) => {
    setFromDate(value);
    setCurrentPage(1);
  };

  const updateToDateFilter = (value) => {
    setToDate(value);
    setCurrentPage(1);
  };

  const getBadgeType = (action) => {
    if (!action) return 'info';
    if (action.includes('CREATED')) return 'success';
    if (action.includes('DEACTIVATED') || action.includes('DELETED')) return 'danger';
    if (action.includes('UPDATED')) return 'warning';
    return 'info';
  };

  const openAuditDetail = async (log) => {
    setAuditDetail(log);
    setAuditDetailOpen(true);
    setAuditDetailLoading(true);
    try {
      const detail = await adminService.getAuditLogById(log.id);
      setAuditDetail(detail);
    } catch {
      addToast('Không thể tải chi tiết nhật ký', 'error');
    } finally {
      setAuditDetailLoading(false);
    }
  };

  const renderChangedFields = () => {
    const oldValue = auditDetail?.oldValue || {};
    const newValue = auditDetail?.newValue || {};
    const fields = Array.from(new Set([...Object.keys(oldValue), ...Object.keys(newValue)]));
    
    if (fields.length === 0) {
      return (
        <tr>
          <td colSpan={3} className="px-6 py-3 text-center text-xs text-shade-50 italic">
            Không có thông tin chi tiết thay đổi hoặc đối tượng mới được khởi tạo
          </td>
        </tr>
      );
    }
    
    return fields.map((field) => {
      let oldValStr = oldValue[field];
      let newValStr = newValue[field];
      
      if (typeof oldValStr === 'object' && oldValStr !== null) {
        oldValStr = JSON.stringify(oldValStr);
      }
      if (typeof newValStr === 'object' && newValStr !== null) {
        newValStr = JSON.stringify(newValStr);
      }
      
      return (
        <tr key={field} className="border-t border-hairline-light">
          <td className="px-6 py-3 text-xs font-semibold text-ink font-mono">{field}</td>
          <td className="px-6 py-3 text-xs text-shade-60 break-all max-w-[200px]">
            {oldValStr !== undefined && oldValStr !== null ? String(oldValStr) : '-'}
          </td>
          <td className="px-6 py-3 text-xs text-shade-60 break-all max-w-[200px]">
            {newValStr !== undefined && newValStr !== null ? String(newValStr) : '-'}
          </td>
        </tr>
      );
    });
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Hệ thống / Admin
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Nhật ký Hoạt động (Audit Trail)
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Truy vết toàn bộ lịch sử thao tác, cấu hình hệ thống và thay đổi phân quyền tài khoản.
        </p>
      </div>

      {/* Filter Bar */}
      <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 p-4 flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="flex flex-col md:flex-row gap-4 items-center w-full md:w-auto">
          <div className="flex flex-col gap-1 w-full md:w-56">
            <Input
              type="select"
              value={selectedWarehouseId}
              onChange={(e) => updateWarehouseFilter(e.target.value)}
              options={[
                { value: '', label: 'Tất cả kho' },
                ...warehouses.map((warehouse) => ({
                  value: String(warehouse.id),
                  label: `${warehouse.code} - ${warehouse.name}`
                }))
              ]}
            />
          </div>

          <div className="flex flex-col gap-1 w-full md:w-52">
            <Input
              type="datetime-local"
              value={fromDate}
              onChange={(e) => updateFromDateFilter(e.target.value)}
            />
          </div>

          <div className="flex flex-col gap-1 w-full md:w-52">
            <Input
              type="datetime-local"
              value={toDate}
              onChange={(e) => updateToDateFilter(e.target.value)}
            />
          </div>
        </div>

        {/* Reset button */}
        <Button
          variant="outline-light"
          onClick={handleResetFilters}
          icon={RotateCcw}
        >
          Đặt lại bộ lọc
        </Button>
      </div>

      {/* Logs Table */}
      <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden flex flex-col">
        <Table
          headers={['Thời gian', 'Người thực hiện', 'Thao tác', 'Đối tượng', 'Nội dung', 'Hành động']}
          data={logs}
          loading={loading}
          emptyMessage="Không tìm thấy nhật ký hoạt động phù hợp"
          renderRow={(log) => (
            <tr key={log.id} className="hover:bg-canvas-cream/50 transition-colors">
              <td className="px-6 py-4 text-xs font-mono text-shade-50">
                {formatDate(log.timestamp || log.createdAt)}
              </td>
              <td className="px-6 py-4 text-xs font-semibold text-ink">
                {log.actorName}
              </td>
              <td className="px-6 py-4">
                <Badge type={getBadgeType(log.action)}>
                  {log.action}
                </Badge>
              </td>
              <td className="px-6 py-4 text-xs text-shade-60 font-medium">
                {log.entityType || '-'} (ID: {log.entityId ?? '-'})
              </td>
              <td className="px-6 py-4 text-xs font-medium text-shade-70">
                {log.description || log.details}
              </td>
              <td className="px-6 py-4 text-right">
                <button
                  onClick={() => openAuditDetail(log)}
                  className="p-1 hover:bg-canvas-cream rounded-full transition-colors text-shade-60 hover:text-ink"
                  title="Xem chi tiết thay đổi"
                >
                  <Eye className="w-4 h-4" />
                </button>
              </td>
            </tr>
          )}
          renderCard={(log) => (
            <div key={log.id} className="rounded-lg border border-hairline-light bg-canvas-cream/30 overflow-hidden">
              <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                <span className="font-mono text-[11px] text-shade-50">{formatDate(log.timestamp || log.createdAt)}</span>
                <Badge type={getBadgeType(log.action)}>
                  {log.action}
                </Badge>
              </div>
              <div className="p-4 flex flex-col gap-2 text-xs">
                <div className="font-semibold text-ink">{log.actorName}</div>
                <p className="text-shade-50">Đối tượng: <span className="font-medium text-shade-70">{log.entityType || '-'} (ID: {log.entityId ?? '-'})</span></p>
                <p className="text-shade-50">{log.description || log.details}</p>
              </div>
              <div className="p-4 border-t border-hairline-light flex justify-end">
                <button
                  onClick={() => openAuditDetail(log)}
                  className="p-1 hover:bg-canvas-cream rounded-full transition-colors text-shade-60 hover:text-ink"
                  title="Xem chi tiết thay đổi"
                >
                  <Eye className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        />
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={pageSize}
          onPageChange={setCurrentPage}
        />
      </div>

      {/* Audit Detail Modal */}
      <Modal
        isOpen={auditDetailOpen}
        onClose={() => setAuditDetailOpen(false)}
        title="Chi tiết nhật ký hoạt động"
        maxWidth="max-w-xl"
      >
        {auditDetailLoading ? (
          <div className="py-8 text-center text-sm text-shade-50">Đang tải chi tiết...</div>
        ) : (
          <div className="flex flex-col gap-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs bg-canvas-cream/30 p-3 rounded border border-hairline-light">
              <div>
                <span className="font-semibold text-shade-60">Thời gian:</span>{' '}
                {formatDate(auditDetail?.timestamp || auditDetail?.createdAt)}
              </div>
              <div>
                <span className="font-semibold text-shade-60">Người thực hiện:</span>{' '}
                {auditDetail?.actorName}
              </div>
              <div>
                <span className="font-semibold text-shade-60">Vai trò:</span>{' '}
                {auditDetail?.actorRole}
              </div>
              <div>
                <span className="font-semibold text-shade-60">IP:</span>{' '}
                {auditDetail?.ipAddress || '-'}
              </div>
              <div>
                <span className="font-semibold text-shade-60">Đối tượng:</span>{' '}
                {auditDetail?.entityType || '-'} #{auditDetail?.entityId ?? '-'}
              </div>
              <div>
                <span className="font-semibold text-shade-60">Kho:</span>{' '}
                {auditDetail?.warehouseCode || '-'}
              </div>
            </div>
            <div className="text-sm font-semibold text-ink px-1">
              {auditDetail?.description || auditDetail?.details}
            </div>
            <div className="overflow-x-auto border border-hairline-light rounded-md">
              <table className="w-full text-left">
                <thead className="bg-canvas-cream">
                  <tr>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Trường dữ liệu</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Giá trị cũ</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Giá trị mới</th>
                  </tr>
                </thead>
                <tbody>{renderChangedFields()}</tbody>
              </table>
            </div>
            <div className="flex justify-end gap-3 mt-2">
              <Button onClick={() => setAuditDetailOpen(false)} variant="primary">
                Đóng
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default AuditLogs;
