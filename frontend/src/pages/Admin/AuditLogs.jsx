import React, { useState, useEffect } from 'react';
import { adminService } from '../../services/admin.service';
import { useUiStore } from '../../stores/ui.store';
import Table from '../../components/common/Table';
import Badge from '../../components/common/Badge';
import { formatDate } from '../../utils/format';
import { Search, RotateCcw, Filter } from 'lucide-react';

const AuditLogs = () => {
  const { addToast } = useUiStore();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);

  // Filters State
  const [search, setSearch] = useState('');
  const [selectedAction, setSelectedAction] = useState('ALL');
  const [selectedEntity, setSelectedEntity] = useState('ALL');

  // Load Audit Logs
  const loadLogs = async () => {
    setLoading(true);
    try {
      const response = await adminService.getAuditLogs();
      setLogs(response);
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
      log.actorName.toLowerCase().includes(search.toLowerCase()) ||
      log.details.toLowerCase().includes(search.toLowerCase());

    // Action filter
    const matchesAction = selectedAction === 'ALL' || log.action === selectedAction;

    // Entity filter
    const matchesEntity = selectedEntity === 'ALL' || log.entityType === selectedEntity;

    return matchesSearch && matchesAction && matchesEntity;
  });

  // Extract unique actions and entities for filter options
  const uniqueActions = ['ALL', ...new Set(logs.map((log) => log.action))];
  const uniqueEntities = ['ALL', ...new Set(logs.map((log) => log.entityType))];

  const getBadgeType = (action) => {
    if (action.includes('CREATED')) return 'success';
    if (action.includes('DEACTIVATED') || action.includes('DELETED')) return 'danger';
    if (action.includes('UPDATED')) return 'warning';
    return 'info';
  };

  return (
    <div className="flex-1 flex flex-col gap-6 pb-12">
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
      <Table
        headers={['Thời gian', 'Người thực hiện', 'Thao tác', 'Đối tượng', 'Nội dung']}
        data={filteredLogs}
        loading={loading}
        emptyMessage="Không tìm thấy nhật ký hoạt động phù hợp"
        renderRow={(log) => (
          <tr key={log.id} className="hover:bg-canvas-cream/50 transition-colors">
            <td className="px-6 py-4 text-xs font-mono text-shade-50">
              {formatDate(log.createdAt)}
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
              {log.details}
            </td>
          </tr>
        )}
      />
    </div>
  );
};

export default AuditLogs;
