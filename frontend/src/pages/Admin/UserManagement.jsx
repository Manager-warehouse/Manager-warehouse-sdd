import React, { useEffect, useState } from 'react';
import { Eye, History, Plus, Search, Users } from 'lucide-react';
import { adminService } from '../../services/admin.service';
import { useUiStore } from '../../stores/ui.store';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Pagination from '../../components/common/Pagination';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Table from '../../components/common/Table';
import UserTable from './UserTable';
import UserFormModal from './UserFormModal';
import { WAREHOUSES } from '../../utils/constants';
import { formatDate } from '../../utils/format';

const emptyFilters = { from: '', to: '', warehouseId: '' };

const UserManagement = () => {
  const { addToast } = useUiStore();
  const [activeTab, setActiveTab] = useState('users');

  // Users Data States
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');

  // Pagination States for Users
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Form Modal States
  const [modalOpen, setModalOpen] = useState(false);
  const [editingUser, setEditingUser] = useState(null);
  const [formLoading, setFormLoading] = useState(false);

  // Audit Logs States
  const [auditLogs, setAuditLogs] = useState([]);
  const [auditPage, setAuditPage] = useState(1);
  const [auditPageMeta, setAuditPageMeta] = useState({
    hasNext: false,
    hasPrevious: false,
    requiresFilterForOlder: false
  });
  const [auditFilters, setAuditFilters] = useState(emptyFilters);
  const [auditDetailOpen, setAuditDetailOpen] = useState(false);
  const [auditDetail, setAuditDetail] = useState(null);
  const [auditDetailLoading, setAuditDetailLoading] = useState(false);

  // Warehouse options for filters
  const warehouseOptions = WAREHOUSES.map((w) => ({
    value: w.id,
    label: `${w.name} (${w.code})`
  }));

  // Fetch audit logs
  const loadAuditLogs = async (page = auditPage, filters = auditFilters) => {
    try {
      const response = await adminService.getAuditLogs({
        page,
        pageSize: 30,
        from: filters.from || undefined,
        to: filters.to || undefined,
        warehouseId: filters.warehouseId || undefined
      });
      setAuditLogs(response.data || response || []);
      setAuditPage(response.page || page);
      setAuditPageMeta({
        hasNext: Boolean(response.hasNext),
        hasPrevious: Boolean(response.hasPrevious),
        requiresFilterForOlder: Boolean(response.requiresFilterForOlder)
      });
    } catch (err) {
      console.error('Không thể tải nhật ký hoạt động:', err);
    }
  };

  // Load All Data
  const loadData = async () => {
    setLoading(true);
    try {
      const uList = await adminService.getUsers();
      setUsers(uList || []);
      await loadAuditLogs(1, auditFilters);
    } catch (err) {
      console.error('Error loading admin user data:', err);
      addToast('Không thể tải dữ liệu quản trị', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Filter users
  const filteredUsers = users.filter((u) => {
    const searchLower = search.toLowerCase();
    return (
      u.fullName.toLowerCase().includes(searchLower) ||
      (u.email && u.email.toLowerCase().includes(searchLower)) ||
      (u.code && u.code.toLowerCase().includes(searchLower))
    );
  });

  // Reset page to 1 when search changes
  useEffect(() => {
    setCurrentPage(1);
  }, [search]);

  // Paginated Users
  const totalItems = filteredUsers.length;
  const totalPages = Math.ceil(totalItems / pageSize) || 1;
  const paginatedUsers = filteredUsers.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  const handleOpenCreateModal = () => {
    setEditingUser(null);
    setModalOpen(true);
  };

  const handleOpenEditModal = (user) => {
    setEditingUser(user);
    setModalOpen(true);
  };

  const handleSaveUser = async (payload) => {
    setFormLoading(true);
    try {
      if (editingUser) {
        // Edit flow
        await adminService.updateUser(editingUser.id, {
          code: payload.code,
          fullName: payload.fullName,
          email: payload.email,
          phone: payload.phone,
          role: payload.role,
          jobTitle: payload.jobTitle,
          shift: payload.shift,
          region: payload.region,
          warehouses: payload.warehouses
        });
        addToast('Cập nhật tài khoản thành công', 'success');
      } else {
        // Create flow
        await adminService.createUser(payload);
        addToast('Tạo tài khoản mới thành công', 'success');
      }
      setModalOpen(false);
      await loadData();
    } catch (err) {
      throw err; // Re-throw so UserFormModal can handle validation message
    } finally {
      setFormLoading(false);
    }
  };

  const handleToggleUserStatus = async (user) => {
    const nextStatus = !user.isActive;
    try {
      await adminService.toggleUserStatus(user.id, nextStatus);
      addToast(
        `${nextStatus ? 'Kích hoạt' : 'Khóa'} tài khoản ${user.fullName} thành công`,
        'success'
      );
      await loadData();
    } catch {
      addToast('Không thể cập nhật trạng thái tài khoản', 'error');
    }
  };

  const getAuditBadgeType = (action = '') => {
    if (['CREATE', 'LOGIN', 'ASSIGN', 'STATUS_CHANGE', 'PRODUCT_CREATED', 'WAREHOUSE_CREATED', 'BIN_CREATED', 'DEALER_CREATED', 'SUPPLIER_CREATED', 'VEHICLE_CREATED', 'DRIVER_CREATED', 'RECEIPT_DRAFTED', 'RECEIPT_RECEIVED', 'RECEIPT_APPROVED', 'RECEIPT_PUTAWAY_COMPLETED'].includes(action)) return 'success';
    if (['SOFT_DELETE', 'CANCEL', 'REJECT', 'RECEIPT_REJECTED', 'QUARANTINE_RTV'].includes(action)) return 'danger';
    if (['UPDATE', 'SYSTEM_CONFIG_UPDATED', 'PRODUCT_UPDATED', 'WAREHOUSE_UPDATED', 'BIN_UPDATED', 'DEALER_UPDATED', 'SUPPLIER_UPDATED', 'VEHICLE_UPDATED', 'DRIVER_UPDATED'].includes(action)) return 'info';
    return 'neutral';
  };

  const applyAuditFilters = async () => {
    setLoading(true);
    try {
      await loadAuditLogs(1, auditFilters);
    } catch {
      addToast('Không thể tải nhật ký hoạt động', 'error');
    } finally {
      setLoading(false);
    }
  };

  const changeAuditPage = async (nextPage) => {
    setLoading(true);
    try {
      await loadAuditLogs(nextPage, auditFilters);
    } catch {
      addToast('Không thể tải trang nhật ký', 'error');
    } finally {
      setLoading(false);
    }
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
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Hệ thống / Admin
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Quản trị tài khoản & Phân quyền
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản lý tài khoản nhân viên, vai trò RBAC và kho được phép truy cập.
          </p>
        </div>
        <Button onClick={handleOpenCreateModal} variant="primary" icon={Plus}>
          Tạo tài khoản
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-hairline-light">
        <button
          onClick={() => setActiveTab('users')}
          className={`flex items-center gap-2 px-5 py-3 border-b-2 text-xs font-semibold uppercase tracking-wider ${
            activeTab === 'users'
              ? 'border-ink text-ink'
              : 'border-transparent text-shade-50 hover:text-ink'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Danh sách tài khoản</span>
        </button>
        <button
          onClick={() => setActiveTab('auditLogs')}
          className={`flex items-center gap-2 px-5 py-3 border-b-2 text-xs font-semibold uppercase tracking-wider ${
            activeTab === 'auditLogs'
              ? 'border-ink text-ink'
              : 'border-transparent text-shade-50 hover:text-ink'
          }`}
        >
          <History className="w-4 h-4" />
          <span>Nhật ký hoạt động</span>
        </button>
      </div>

      {activeTab === 'users' ? (
        <div className="flex flex-col gap-4">
          {/* Search bar */}
          <div className="relative w-full max-w-md">
            <Search className="absolute left-3 top-3.5 w-4 h-4 text-shade-50" />
            <input
              type="text"
              placeholder="Tìm theo tài khoản, tên hoặc email..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full bg-canvas-light text-sm pl-9 pr-4 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all min-h-[44px]"
            />
          </div>

          {/* User Table Component */}
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden flex flex-col">
            <UserTable
              users={paginatedUsers}
              loading={loading}
              onEdit={handleOpenEditModal}
              onToggleStatus={handleToggleUserStatus}
            />
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              totalItems={totalItems}
              pageSize={pageSize}
              onPageChange={setCurrentPage}
              onPageSizeChange={setPageSize}
            />
          </div>
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          {/* Audit Filters */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3 bg-canvas-light p-4 rounded-lg border border-hairline-light">
            <Input
              label="Từ ngày"
              type="date"
              value={auditFilters.from}
              onChange={(e) => setAuditFilters((current) => ({ ...current, from: e.target.value }))}
            />
            <Input
              label="Đến ngày"
              type="date"
              value={auditFilters.to}
              onChange={(e) => setAuditFilters((current) => ({ ...current, to: e.target.value }))}
            />
            <Input
              label="Kho làm việc"
              type="select"
              value={auditFilters.warehouseId}
              onChange={(e) => setAuditFilters((current) => ({ ...current, warehouseId: e.target.value }))}
              options={[{ value: '', label: 'Tất cả kho' }, ...warehouseOptions]}
            />
            <div className="flex items-end">
              <Button onClick={applyAuditFilters} className="w-full">
                Lọc nhật ký
              </Button>
            </div>
          </div>

          {/* Audit Table */}
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden flex flex-col">
            <Table
              headers={['Thời gian', 'Người thực hiện', 'Thao tác', 'Đối tượng', 'Nội dung', '']}
              data={auditLogs}
              loading={loading}
              renderRow={(log) => (
                <tr key={log.id} className="hover:bg-canvas-cream/50 transition-colors">
                  <td className="px-6 py-4 text-xs font-mono text-shade-50">
                    {formatDate(log.timestamp || log.createdAt)}
                  </td>
                  <td className="px-6 py-4 text-xs font-semibold text-ink">
                    {log.actorName}
                  </td>
                  <td className="px-6 py-4">
                    <Badge type={getAuditBadgeType(log.action)}>
                      {log.action}
                    </Badge>
                  </td>
                  <td className="px-6 py-4 text-xs text-shade-60">
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
            {/* Pagination for Audit Logs */}
            <div className="flex items-center justify-between text-xs text-shade-60 p-4 border-t border-hairline-light bg-canvas-light">
              <span>
                Trang {auditPage} / 50
                {auditPageMeta.requiresFilterForOlder
                  ? ' - dùng bộ lọc để tìm kiếm nhật ký cũ hơn'
                  : ''}
              </span>
              <div className="flex gap-2">
                <Button
                  variant="outline-light"
                  disabled={!auditPageMeta.hasPrevious || loading}
                  onClick={() => changeAuditPage(Math.max(1, auditPage - 1))}
                >
                  Trước
                </Button>
                <Button
                  variant="outline-light"
                  disabled={loading || (!auditPageMeta.hasNext && auditPage >= 50)}
                  onClick={() => changeAuditPage(auditPage + 1)}
                >
                  Sau
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* User Form Modal Component */}
      <UserFormModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSave={handleSaveUser}
        user={editingUser}
        loading={formLoading}
      />

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

export default UserManagement;
