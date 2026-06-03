import React, { useEffect, useState } from 'react';
import { Edit2, Eye, History, Plus, Search, ToggleLeft, ToggleRight, Users } from 'lucide-react';
import { adminService } from '../../services/admin.service';
import { useUiStore } from '../../stores/ui.store';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Table from '../../components/common/Table';
import { ROLES, ROLE_LABELS, WAREHOUSES } from '../../utils/constants';
import { formatDate } from '../../utils/format';

const emptyFilters = { from: '', to: '', warehouseId: '' };

const UserManagement = () => {
  const { addToast } = useUiStore();
  const [activeTab, setActiveTab] = useState('users');
  const [users, setUsers] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [modalType, setModalType] = useState('create');
  const [editingUserId, setEditingUserId] = useState(null);
  const [formError, setFormError] = useState('');
  const [formLoading, setFormLoading] = useState(false);
  const [auditPage, setAuditPage] = useState(1);
  const [auditPageMeta, setAuditPageMeta] = useState({ hasNext: false, hasPrevious: false, requiresFilterForOlder: false });
  const [auditFilters, setAuditFilters] = useState(emptyFilters);
  const [auditDetailOpen, setAuditDetailOpen] = useState(false);
  const [auditDetail, setAuditDetail] = useState(null);
  const [auditDetailLoading, setAuditDetailLoading] = useState(false);
  const [form, setForm] = useState({
    code: '',
    fullName: '',
    email: '',
    phone: '',
    password: '',
    role: ROLES.WAREHOUSE_STAFF,
    jobTitle: '',
    shift: '',
    region: '',
    warehouses: []
  });

  const roleOptions = Object.keys(ROLES).map((key) => ({
    value: ROLES[key],
    label: ROLE_LABELS[ROLES[key]]
  }));

  const warehouseOptions = WAREHOUSES.map((w) => ({
    value: w.id,
    label: `${w.name} (${w.code})`
  }));

  const loadAuditLogs = async (page = auditPage, filters = auditFilters) => {
    const response = await adminService.getAuditLogs({
      page,
      pageSize: 30,
      from: filters.from || undefined,
      to: filters.to || undefined,
      warehouseId: filters.warehouseId || undefined
    });
    setAuditLogs(response.data || response);
    setAuditPage(response.page || page);
    setAuditPageMeta({
      hasNext: Boolean(response.hasNext),
      hasPrevious: Boolean(response.hasPrevious),
      requiresFilterForOlder: Boolean(response.requiresFilterForOlder)
    });
  };

  const loadData = async () => {
    setLoading(true);
    try {
      const userList = await adminService.getUsers();
      setUsers(userList);
      await loadAuditLogs(1);
    } catch {
      addToast('Khong the tai du lieu quan tri', 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const updateForm = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const openCreateModal = () => {
    setModalType('create');
    setEditingUserId(null);
    setForm({
      code: '',
      fullName: '',
      email: '',
      phone: '',
      password: '',
      role: ROLES.WAREHOUSE_STAFF,
      jobTitle: '',
      shift: '',
      region: '',
      warehouses: []
    });
    setFormError('');
    setModalOpen(true);
  };

  const openEditModal = (user) => {
    setModalType('edit');
    setEditingUserId(user.id);
    setForm({
      code: user.code || '',
      fullName: user.fullName || '',
      email: user.email || '',
      phone: user.phone || '',
      password: '',
      role: user.role || ROLES.WAREHOUSE_STAFF,
      jobTitle: user.jobTitle || '',
      shift: user.shift || '',
      region: user.region || '',
      warehouses: user.warehouses || []
    });
    setFormError('');
    setModalOpen(true);
  };

  const validateForm = () => {
    if (!form.code || !form.fullName || !form.email) return 'Nhap ma nhan vien, ho ten va email';
    if (!form.role) return 'Chon vai tro';
    if (modalType === 'create' && !form.password) return 'Nhap mat khau khoi tao';
    if (modalType === 'create' && form.password.length < 8) return 'Mat khau toi thieu 8 ky tu';
    if (modalType === 'create' && (!/[a-zA-Z]/.test(form.password) || !/[0-9]/.test(form.password))) {
      return 'Mat khau phai co chu va so';
    }
    if (form.role !== ROLES.ADMIN && form.role !== ROLES.CEO && form.warehouses.length === 0) {
      return 'Nhan vien nghiep vu phai duoc gan it nhat mot kho';
    }
    return '';
  };

  const saveUser = async (event) => {
    event.preventDefault();
    const error = validateForm();
    setFormError(error);
    if (error) return;

    setFormLoading(true);
    try {
      const payload = { ...form };
      if (modalType === 'create') {
        await adminService.createUser(payload);
        addToast('Tao tai khoan thanh cong', 'success');
      } else {
        await adminService.updateUser(editingUserId, payload);
        addToast('Cap nhat tai khoan thanh cong', 'success');
      }
      setModalOpen(false);
      await loadData();
    } catch (err) {
      if (err.message === 'EMAIL_TAKEN') setFormError('Email da ton tai');
      else if (err.message === 'WEAK_PASSWORD') setFormError('Mat khau khong dat yeu cau');
      else setFormError('Khong the luu tai khoan');
    } finally {
      setFormLoading(false);
    }
  };

  const toggleUserStatus = async (user) => {
    try {
      await adminService.toggleUserStatus(user.id, !user.isActive);
      addToast('Cap nhat trang thai tai khoan thanh cong', 'success');
      await loadData();
    } catch {
      addToast('Khong the cap nhat trang thai tai khoan', 'error');
    }
  };

  const filteredUsers = users.filter((user) => {
    const q = search.toLowerCase();
    return user.fullName?.toLowerCase().includes(q) ||
      user.email?.toLowerCase().includes(q) ||
      user.code?.toLowerCase().includes(q);
  });

  const getAuditBadgeType = (action = '') => {
    if (['CREATE', 'LOGIN', 'ASSIGN'].includes(action)) return 'success';
    if (['SOFT_DELETE', 'CANCEL', 'REJECT'].includes(action)) return 'danger';
    if (['APPROVE', 'STATUS_CHANGE'].includes(action)) return 'warning';
    return 'info';
  };

  const applyAuditFilters = async () => {
    setLoading(true);
    try {
      await loadAuditLogs(1, auditFilters);
    } catch {
      addToast('Khong the tai nhat ky hoat dong', 'error');
    } finally {
      setLoading(false);
    }
  };

  const changeAuditPage = async (nextPage) => {
    setLoading(true);
    try {
      await loadAuditLogs(nextPage, auditFilters);
    } catch {
      addToast('Khong the tai trang nhat ky', 'error');
    } finally {
      setLoading(false);
    }
  };

  const openAuditDetail = async (log) => {
    setAuditDetail(log);
    setAuditDetailOpen(true);
    setAuditDetailLoading(true);
    try {
      setAuditDetail(await adminService.getAuditLogById(log.id));
    } catch {
      addToast('Khong the tai chi tiet nhat ky', 'error');
    } finally {
      setAuditDetailLoading(false);
    }
  };

  const renderChangedFields = () => {
    const oldValue = auditDetail?.oldValue || {};
    const newValue = auditDetail?.newValue || {};
    const fields = Array.from(new Set([...Object.keys(oldValue), ...Object.keys(newValue)]));
    if (fields.length === 0) return <p className="text-sm text-shade-50">Khong co field thay doi.</p>;
    return fields.map((field) => (
      <tr key={field} className="border-t border-hairline-light">
        <td className="px-3 py-2 text-xs font-semibold text-ink">{field}</td>
        <td className="px-3 py-2 text-xs text-shade-60">{String(oldValue[field] ?? '')}</td>
        <td className="px-3 py-2 text-xs text-shade-60">{String(newValue[field] ?? '')}</td>
      </tr>
    ));
  };

  return (
    <div className="flex-1 flex flex-col gap-6 pb-12">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">He thong / Admin</span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">Quan tri tai khoan & Phan quyen</h1>
          <p className="text-xs text-shade-50 font-light mt-1">Quan ly tai khoan nhan vien, vai tro RBAC va kho duoc phep truy cap.</p>
        </div>
        <Button onClick={openCreateModal} variant="primary" icon={Plus}>Tao tai khoan</Button>
      </div>

      <div className="flex border-b border-hairline-light">
        <button onClick={() => setActiveTab('users')} className={`flex items-center gap-2 px-5 py-3 border-b-2 text-xs font-semibold uppercase tracking-wider ${activeTab === 'users' ? 'border-ink text-ink' : 'border-transparent text-shade-50 hover:text-ink'}`}>
          <Users className="w-4 h-4" /><span>Danh sach tai khoan</span>
        </button>
        <button onClick={() => setActiveTab('auditLogs')} className={`flex items-center gap-2 px-5 py-3 border-b-2 text-xs font-semibold uppercase tracking-wider ${activeTab === 'auditLogs' ? 'border-ink text-ink' : 'border-transparent text-shade-50 hover:text-ink'}`}>
          <History className="w-4 h-4" /><span>Nhat ky hoat dong</span>
        </button>
      </div>

      {activeTab === 'users' ? (
        <div className="flex flex-col gap-4">
          <div className="relative w-full max-w-md">
            <Search className="absolute left-3 top-3.5 w-4 h-4 text-shade-50" />
            <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Tim theo ma, ten hoac email..." className="w-full bg-canvas-light text-sm pl-9 pr-4 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink min-h-[44px]" />
          </div>
          <Table
            headers={['Ma NV / Ho ten', 'Tai khoan / SDT', 'Vai tro', 'Kho phu trach', 'Trang thai', 'Thao tac']}
            data={filteredUsers}
            loading={loading}
            renderRow={(user) => {
              const assignedWarehouses = WAREHOUSES.filter((w) => user.warehouses?.includes(w.id)).map((w) => w.code).join(', ');
              return (
                <tr key={user.id} className="hover:bg-canvas-cream/50 transition-colors">
                  <td className="px-6 py-4"><div className="text-xs font-mono font-bold text-shade-60">{user.code || 'NV-000'}</div><div className="text-sm font-semibold text-ink">{user.fullName}</div></td>
                  <td className="px-6 py-4 text-xs text-shade-60"><div>{user.email || 'Khong co email'}</div><div>{user.phone || 'Chua co SDT'}</div></td>
                  <td className="px-6 py-4"><span className="text-[10px] font-bold bg-canvas-cream text-ink border border-hairline-light px-2.5 py-1 rounded-pill uppercase tracking-wider">{ROLE_LABELS[user.role] || user.role}</span></td>
                  <td className="px-6 py-4 text-xs font-medium text-shade-70 font-mono">{user.role === ROLES.ADMIN || user.role === ROLES.CEO ? 'Tat ca kho' : assignedWarehouses || 'Chua gan kho'}</td>
                  <td className="px-6 py-4"><Badge type={user.isActive ? 'success' : 'neutral'}>{user.isActive ? 'Dang hoat dong' : 'Tam khoa'}</Badge></td>
                  <td className="px-6 py-4"><div className="flex items-center gap-3"><button onClick={() => openEditModal(user)} className="text-shade-60 hover:text-ink"><Edit2 className="w-4 h-4" /></button><button onClick={() => toggleUserStatus(user)} className="text-shade-60 hover:text-ink">{user.isActive ? <ToggleRight className="w-6 h-6" /> : <ToggleLeft className="w-6 h-6" />}</button></div></td>
                </tr>
              );
            }}
          />
        </div>
      ) : (
        <div className="flex flex-col gap-4">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
            <Input label="Tu ngay" type="date" value={auditFilters.from} onChange={(e) => setAuditFilters((current) => ({ ...current, from: e.target.value }))} />
            <Input label="Den ngay" type="date" value={auditFilters.to} onChange={(e) => setAuditFilters((current) => ({ ...current, to: e.target.value }))} />
            <Input label="Kho" type="select" value={auditFilters.warehouseId} onChange={(e) => setAuditFilters((current) => ({ ...current, warehouseId: e.target.value }))} options={[{ value: '', label: 'Tat ca kho' }, ...warehouseOptions]} />
            <div className="flex items-end"><Button onClick={applyAuditFilters} className="w-full">Loc log</Button></div>
          </div>
          <Table
            headers={['Thoi gian', 'Nguoi thuc hien', 'Thao tac', 'Doi tuong', 'Noi dung', '']}
            data={auditLogs}
            loading={loading}
            renderRow={(log) => (
              <tr key={log.id} className="hover:bg-canvas-cream/50 transition-colors">
                <td className="px-6 py-4 text-xs font-mono text-shade-50">{formatDate(log.timestamp || log.createdAt)}</td>
                <td className="px-6 py-4 text-xs font-semibold text-ink">{log.actorName}</td>
                <td className="px-6 py-4"><Badge type={getAuditBadgeType(log.action)}>{log.action}</Badge></td>
                <td className="px-6 py-4 text-xs text-shade-60">{log.entityType} (ID: {log.entityId})</td>
                <td className="px-6 py-4 text-xs font-medium text-shade-70">{log.description || log.details}</td>
                <td className="px-6 py-4 text-right"><button onClick={() => openAuditDetail(log)} className="text-shade-60 hover:text-ink"><Eye className="w-4 h-4" /></button></td>
              </tr>
            )}
          />
          <div className="flex items-center justify-between text-xs text-shade-60">
            <span>Trang {auditPage} / 50{auditPageMeta.requiresFilterForOlder ? ' - dung filter de tim log cu hon' : ''}</span>
            <div className="flex gap-2">
              <Button variant="outline-light" disabled={!auditPageMeta.hasPrevious || loading} onClick={() => changeAuditPage(Math.max(1, auditPage - 1))}>Truoc</Button>
              <Button variant="outline-light" disabled={loading || (!auditPageMeta.hasNext && auditPage >= 50)} onClick={() => changeAuditPage(auditPage + 1)}>Sau</Button>
            </div>
          </div>
        </div>
      )}

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)} title={modalType === 'create' ? 'Tao tai khoan moi' : 'Chinh sua tai khoan'}>
        <form onSubmit={saveUser} className="flex flex-col gap-4">
          {formError && <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-xs font-medium">{formError}</div>}
          <Input label="Ma nhan vien" value={form.code} onChange={(e) => updateForm('code', e.target.value)} disabled={modalType === 'edit'} required />
          <Input label="Ho va ten" value={form.fullName} onChange={(e) => updateForm('fullName', e.target.value)} required />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input label="Email" type="email" value={form.email} onChange={(e) => updateForm('email', e.target.value)} required />
            <Input label="So dien thoai" value={form.phone} onChange={(e) => updateForm('phone', e.target.value)} />
          </div>
          {modalType === 'create' && <Input label="Mat khau khoi tao" type="password" value={form.password} onChange={(e) => updateForm('password', e.target.value)} required />}
          <Input label="Vai tro" type="select" options={roleOptions} value={form.role} onChange={(e) => updateForm('role', e.target.value)} />
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Input label="Chuc danh" value={form.jobTitle} onChange={(e) => updateForm('jobTitle', e.target.value)} />
            <Input label="Ca lam viec" value={form.shift} onChange={(e) => updateForm('shift', e.target.value)} />
            <Input label="Khu vuc phu trach" value={form.region} onChange={(e) => updateForm('region', e.target.value)} />
          </div>
          {form.role !== ROLES.ADMIN && form.role !== ROLES.CEO && <Input label="Phan cong kho" type="checkbox-group" options={warehouseOptions} value={form.warehouses} onChange={(value) => updateForm('warehouses', value)} />}
          <div className="flex justify-end gap-3 mt-4 pt-4 border-t border-hairline-light">
            <Button onClick={() => setModalOpen(false)} variant="outline-light">Huy</Button>
            <Button type="submit" variant="primary" loading={formLoading}>Luu lai</Button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={auditDetailOpen} onClose={() => setAuditDetailOpen(false)} title="Chi tiet audit log" maxWidth="max-w-xl">
        {auditDetailLoading ? (
          <div className="py-8 text-center text-sm text-shade-50">Dang tai chi tiet...</div>
        ) : (
          <div className="flex flex-col gap-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
              <div><span className="font-semibold text-shade-60">Thoi gian:</span> {formatDate(auditDetail?.timestamp)}</div>
              <div><span className="font-semibold text-shade-60">Nguoi thuc hien:</span> {auditDetail?.actorName}</div>
              <div><span className="font-semibold text-shade-60">Vai tro:</span> {auditDetail?.actorRole}</div>
              <div><span className="font-semibold text-shade-60">IP:</span> {auditDetail?.ipAddress || '-'}</div>
              <div><span className="font-semibold text-shade-60">Doi tuong:</span> {auditDetail?.entityType} #{auditDetail?.entityId}</div>
              <div><span className="font-semibold text-shade-60">Kho:</span> {auditDetail?.warehouseCode || '-'}</div>
            </div>
            <div className="text-sm font-semibold text-ink">{auditDetail?.description}</div>
            <div className="overflow-x-auto border border-hairline-light rounded-md">
              <table className="w-full text-left">
                <thead className="bg-canvas-cream">
                  <tr><th className="px-3 py-2 text-xs">Field</th><th className="px-3 py-2 text-xs">Before</th><th className="px-3 py-2 text-xs">After</th></tr>
                </thead>
                <tbody>{renderChangedFields()}</tbody>
              </table>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default UserManagement;
