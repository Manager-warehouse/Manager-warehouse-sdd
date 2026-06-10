import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/admin.service';
import { useUiStore } from '../../stores/ui.store';
import Table from '../../components/common/Table';
import Pagination from '../../components/common/Pagination';
import Badge from '../../components/common/Badge';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import { formatDate } from '../../utils/format';
import { Search, RotateCcw, Filter, Eye } from 'lucide-react';

const AuditLogs = () => {
  const { addToast } = useUiStore();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);

  // Pagination States
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);

  // Filters State
  const [search, setSearch] = useState('');
  const [selectedAction, setSelectedAction] = useState('ALL');
  const [selectedEntity, setSelectedEntity] = useState('ALL');

  // Detail Modal States
  const [auditDetailOpen, setAuditDetailOpen] = useState(false);
  const [auditDetail, setAuditDetail] = useState(null);
  const [auditDetailLoading, setAuditDetailLoading] = useState(false);

  // Load Audit Logs
  const loadLogs = async () => {
    setLoading(true);
    try {
      const response = await adminService.getAuditLogs();
      setLogs(response.data || response || []);
    } catch (err) {
      console.error(err);
      addToast('Không thể tải nhật ký hoạt động', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLogs();
  }, []);

  const handleResetFilters = () => {
    setSearch('');
    setSelectedAction('ALL');
    setSelectedEntity('ALL');
  };

  // Filter logs locally
  const filteredLogs = logs.filter((log) => {
    // Search filter
    const matchesSearch =
      search === '' ||
      (log.actorName || '').toLowerCase().includes(search.toLowerCase()) ||
      (log.details || log.description || '').toLowerCase().includes(search.toLowerCase());

    // Action filter
    const matchesAction = selectedAction === 'ALL' || log.action === selectedAction;

    // Entity filter
    const matchesEntity = selectedEntity === 'ALL' || log.entityType === selectedEntity;

    return matchesSearch && matchesAction && matchesEntity;
  });

  // Reset page to 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [search, selectedAction, selectedEntity]);

  // Paginated Logs
  const totalItems = filteredLogs.length;
  const totalPages = Math.ceil(totalItems / pageSize) || 1;
  const paginatedLogs = filteredLogs.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  // Extract unique actions and entities for filter options
  const uniqueActions = ['ALL', ...new Set(logs.map((log) => log.action))];
  const uniqueEntities = ['ALL', ...new Set(logs.map((log) => log.entityType))];

  const getBadgeType = (action) => {
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
          <td colSpan={3} className="px-3 py-4 text-center text-xs text-shade-50 italic">
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
          <td className="px-3 py-2 text-xs font-semibold text-ink font-mono">{field}</td>
          <td className="px-3 py-2 text-xs text-shade-60 break-all max-w-[200px]">
            {oldValStr !== undefined && oldValStr !== null ? String(oldValStr) : '-'}
          </td>
          <td className="px-3 py-2 text-xs text-shade-60 break-all max-w-[200px]">
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
          {/* Search Input */}
          <div className="relative w-full md:w-72">
            <Search className="absolute left-3 top-3 w-4 h-4 text-shade-50" />
            <input
              type="text"
              placeholder="Tìm theo người dùng, nội dung..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-canvas-light text-xs pl-9 pr-4 py-2 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all min-h-[38px]"
            />
          </div>

          {/* Action Filter */}
          <div className="flex flex-col gap-1 w-full md:w-48">
            <select
              value={selectedAction}
              onChange={(e) => setSelectedAction(e.target.value)}
              className="w-full bg-canvas-light text-xs px-3 py-2 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all min-h-[38px]"
            >
              <option value="ALL">Tất cả thao tác</option>
              {uniqueActions.filter(a => a !== 'ALL').map((act) => (
                <option key={act} value={act}>
                  {act}
                </option>
              ))}
            </select>
          </div>

          {/* Entity Type Filter */}
          <div className="flex flex-col gap-1 w-full md:w-48">
            <select
              value={selectedEntity}
              onChange={(e) => setSelectedEntity(e.target.value)}
              className="w-full bg-canvas-light text-xs px-3 py-2 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all min-h-[38px]"
            >
              <option value="ALL">Tất cả đối tượng</option>
              {uniqueEntities.filter(e => e !== 'ALL').map((ent) => (
                <option key={ent} value={ent}>
                  {ent}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Reset button */}
        <button
          onClick={handleResetFilters}
          className="text-xs font-semibold flex items-center gap-1.5 px-4 py-2 hover:bg-canvas-cream rounded-pill border border-hairline-light transition-colors text-shade-70 w-full md:w-auto justify-center"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Đặt lại bộ lọc</span>
        </button>
      </div>

      {/* Logs Table */}
      <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden flex flex-col">
        <Table
          headers={['Thời gian', 'Người thực hiện', 'Thao tác', 'Đối tượng', 'Nội dung', '']}
          data={paginatedLogs}
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
                {log.entityType} (ID: {log.entityId})
              </td>
              <td className="px-6 py-4 text-xs font-medium text-shade-70">
                {log.description || log.details}
              </td>
              <td className="px-6 py-4 text-right">
                <button
                  onClick={() => openAuditDetail(log)}
                  className="text-shade-60 hover:text-ink transition-colors"
                  title="Xem chi tiết thay đổi"
                >
                  <Eye className="w-4 h-4" />
                </button>
              </td>
            </tr>
          )}
        />
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          totalItems={totalItems}
          pageSize={pageSize}
          onPageChange={setCurrentPage}
          onPageSizeChange={setPageSize}
          pageSizeOptions={[25, 50, 100]}
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
                {auditDetail?.entityType} #{auditDetail?.entityId}
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
                    <th className="px-3 py-2 text-xs font-semibold text-shade-70">Trường dữ liệu</th>
                    <th className="px-3 py-2 text-xs font-semibold text-shade-70">Giá trị cũ</th>
                    <th className="px-3 py-2 text-xs font-semibold text-shade-70">Giá trị mới</th>
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
