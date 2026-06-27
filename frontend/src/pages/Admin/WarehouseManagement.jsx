import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { masterDataService } from '../../services/masterData.service';
import { adminService } from '../../services/admin.service';
import { ROLES, MOCK_USERS } from '../../utils/constants';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import { Plus, Edit, ToggleLeft, ToggleRight, AlertTriangle, ShieldAlert, Loader2, Home, Layers } from 'lucide-react';

const WarehouseManagement = () => {
  const { hasRole } = useAuthStore();
  const { addToast } = useUiStore();

  const [warehouses, setWarehouses] = useState([]);
  const [selectedWh, setSelectedWh] = useState(null);
  const [bins, setBins] = useState([]);
  const [loadingWh, setLoadingWh] = useState(true);
  const [loadingBins, setLoadingBins] = useState(false);
  
  // Manager Users List
  const [managerUsers, setManagerUsers] = useState([]);
  const [loadingManagers, setLoadingManagers] = useState(false);

  // Warehouse Modal States
  const [isWhModalOpen, setIsWhModalOpen] = useState(false);
  const [whModalType, setWhModalType] = useState('ADD'); // ADD or EDIT
  const [whFormErrors, setWhFormErrors] = useState({});
  const [whSubmitting, setWhSubmitting] = useState(false);
  const [whCode, setWhCode] = useState('');
  const [whName, setWhName] = useState('');
  const [whAddress, setWhAddress] = useState('');
  const [whPhone, setWhPhone] = useState('');
  const [whManagerId, setWhManagerId] = useState('');
  const [whType, setWhType] = useState('PHYSICAL');

  // Bin Modal States
  const [isBinModalOpen, setIsBinModalOpen] = useState(false);
  const [binModalType, setBinModalType] = useState('ADD'); // ADD or EDIT
  const [selectedBin, setSelectedBin] = useState(null);
  const [binFormErrors, setBinFormErrors] = useState({});
  const [binSubmitting, setBinSubmitting] = useState(false);
  // Bin Fields
  const [binZone, setBinZone] = useState('');
  const [binRack, setBinRack] = useState('');
  const [binShelf, setBinShelf] = useState('');
  const [binName, setBinName] = useState('');
  const [binCapacityM3, setBinCapacityM3] = useState('10');
  const [binCapacityKg, setBinCapacityKg] = useState('1000');
  const [binIsQuarantine, setBinIsQuarantine] = useState(false);

  useEffect(() => {
    fetchWarehouses();
    fetchManagerUsers();
  }, []);

  useEffect(() => {
    if (selectedWh) {
      fetchBins(selectedWh.id);
    } else {
      setBins([]);
    }
  }, [selectedWh]);

  const fetchManagerUsers = async () => {
    setLoadingManagers(true);
    try {
      // Get users with WAREHOUSE_MANAGER or STOREKEEPER roles
      const response = await adminService.getUsers();
      const filteredUsers = (response || []).filter(
        u => u.role === ROLES.WAREHOUSE_MANAGER || u.role === ROLES.STOREKEEPER
      );
      setManagerUsers(filteredUsers);
    } catch (e) {
      console.error('Error fetching manager users:', e);
      // Fallback to MOCK_USERS if API fails
      setManagerUsers(MOCK_USERS.filter(u => u.role === ROLES.WAREHOUSE_MANAGER || u.role === ROLES.STOREKEEPER));
    } finally {
      setLoadingManagers(false);
    }
  };

  const fetchWarehouses = async () => {
    setLoadingWh(true);
    try {
      const data = await masterDataService.getWarehouses();
      setWarehouses(data);
      if (data.length > 0 && !selectedWh) {
        setSelectedWh(data[0]);
      } else if (selectedWh) {
        // Refresh selected wh reference
        const currentSelected = data.find(w => w.id === selectedWh.id);
        if (currentSelected) setSelectedWh(currentSelected);
      }
    } catch (e) {
      addToast('Lỗi tải danh sách kho', 'error');
    } finally {
      setLoadingWh(false);
    }
  };

  const fetchBins = async (whId) => {
    setLoadingBins(true);
    try {
      const data = await masterDataService.getBinLocations(whId);
      setBins((data || []).filter(b => b.type === 'BIN'));
    } catch (e) {
      addToast('Lỗi tải danh sách vị trí ô kệ', 'error');
    } finally {
      setLoadingBins(false);
    }
  };

  // --- WAREHOUSE ACTIONS ---
  const handleOpenAddWh = () => {
    setWhModalType('ADD');
    setWhCode('');
    setWhName('');
    setWhAddress('');
    setWhPhone('');
    setWhManagerId('');
    setWhType('PHYSICAL');
    setWhFormErrors({});
    setIsWhModalOpen(true);
  };

  const handleOpenEditWh = (wh) => {
    setWhModalType('EDIT');
    setWhCode(wh.code);
    setWhName(wh.name);
    setWhAddress(wh.address || '');
    setWhPhone(wh.phone || '');
    setWhManagerId(wh.manager_id ? String(wh.manager_id) : '');
    setWhType(wh.type || 'PHYSICAL');
    setWhFormErrors({});
    setIsWhModalOpen(true);
  };

  const validateWhForm = () => {
    const errors = {};
    if (!whCode.trim()) errors.code = 'Mã kho không được để trống';
    if (!whName.trim()) errors.name = 'Tên kho không được để trống';
    setWhFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleWhSubmit = async (e) => {
    e.preventDefault();
    if (!validateWhForm()) return;

    setWhSubmitting(true);
    const whData = {
      code: whCode.trim(),
      name: whName.trim(),
      address: whAddress.trim(),
      phone: whPhone.trim(),
      manager_id: whManagerId ? Number(whManagerId) : null,
      type: whType,
    };

    try {
      if (whModalType === 'ADD') {
        const newWh = await masterDataService.createWarehouse(whData);
        addToast(`Tạo mới kho ${newWh.name} thành công`, 'success');
        setSelectedWh(newWh);
      } else {
        await masterDataService.updateWarehouse(selectedWh.id, whData);
        addToast(`Cập nhật kho ${whName} thành công`, 'success');
      }
      setIsWhModalOpen(false);
      fetchWarehouses();
    } catch (err) {
      if (err.message === 'DUPLICATE_WAREHOUSE_CODE') {
        setWhFormErrors({ code: 'Mã kho này đã tồn tại trên hệ thống' });
      } else {
        addToast('Lỗi lưu trữ thông tin kho', 'error');
      }
    } finally {
      setWhSubmitting(false);
    }
  };

  const handleToggleWhStatus = async (wh) => {
    if (wh.is_active && !hasRole(ROLES.ADMIN) && !hasRole(ROLES.CEO)) {
      addToast('Chỉ Quản trị viên hoặc CEO mới có quyền tắt kích hoạt kho', 'warning');
      return;
    }
    try {
      const updated = await masterDataService.toggleWarehouseStatus(wh.id, !wh.is_active);
      addToast(`${updated.is_active ? 'Kích hoạt' : 'Khóa'} kho ${updated.name} thành công`, 'success');
      fetchWarehouses();
    } catch (e) {
      addToast('Lỗi thay đổi trạng thái kho', 'error');
    }
  };

  // --- BIN ACTIONS ---
  const handleOpenAddBin = () => {
    setBinModalType('ADD');
    setSelectedBin(null);
    setBinZone('');
    setBinRack('');
    setBinShelf('');
    setBinName('');
    setBinCapacityM3('10');
    setBinCapacityKg('1000');
    setBinIsQuarantine(false);
    setBinFormErrors({});
    setIsBinModalOpen(true);
  };

  const handleOpenEditBin = (bin) => {
    setBinModalType('EDIT');
    setSelectedBin(bin);
    // Split the generated code back if possible to edit or just show read-only fields
    setBinCapacityM3(String(bin.capacity_m3));
    setBinCapacityKg(String(bin.capacity_kg));
    setBinIsQuarantine(!!bin.is_quarantine);
    setBinFormErrors({});
    setIsBinModalOpen(true);
  };

  const validateBinForm = () => {
    const errors = {};
    if (binModalType === 'ADD') {
      if (!binZone.trim()) errors.zone = 'Zone bắt buộc (ví dụ: Z1)';
      if (!binRack.trim()) errors.rack = 'Rack bắt buộc (ví dụ: R1)';
      if (!binShelf.trim()) errors.shelf = 'Shelf bắt buộc (ví dụ: S1)';
      if (!binName.trim()) errors.bin = 'Bin bắt buộc (ví dụ: B01)';
    }
    if (Number(binCapacityM3) <= 0) errors.capacity_m3 = 'Thể tích phải lớn hơn 0';
    if (Number(binCapacityKg) <= 0) errors.capacity_kg = 'Khối lượng phải lớn hơn 0';

    setBinFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleBinSubmit = async (e) => {
    e.preventDefault();
    if (!validateBinForm()) return;

    setBinSubmitting(true);
    try {
      if (binModalType === 'ADD') {
        const binData = {
          warehouse_id: selectedWh.id,
          zone: binZone,
          rack: binRack,
          shelf: binShelf,
          bin: binName,
          capacity_m3: parseFloat(binCapacityM3),
          capacity_kg: parseFloat(binCapacityKg),
          is_quarantine: binIsQuarantine,
        };
        await masterDataService.createBinLocation(binData);
        addToast('Tạo vị trí ô kệ thành công', 'success');
      } else {
        const binData = {
          capacity_m3: parseFloat(binCapacityM3),
          capacity_kg: parseFloat(binCapacityKg),
          is_quarantine: binIsQuarantine,
        };
        await masterDataService.updateBinLocation(selectedBin.id, binData);
        addToast(`Cập nhật ô kệ ${selectedBin.code} thành công`, 'success');
      }
      setIsBinModalOpen(false);
      fetchBins(selectedWh.id);
    } catch (err) {
      if (err.message === 'DUPLICATE_BIN_CODE') {
        addToast('Lỗi: Vị trí ô kệ này đã tồn tại trong kho này', 'error');
        setBinFormErrors({ bin: 'Mã vị trí trùng lặp' });
      } else {
        addToast('Lỗi lưu trữ vị trí ô kệ', 'error');
      }
    } finally {
      setBinSubmitting(false);
    }
  };

  const handleToggleBinStatus = async (bin) => {
    try {
      const updated = await masterDataService.toggleBinStatus(bin.id, !bin.is_active);
      addToast(`${updated.is_active ? 'Kích hoạt' : 'Khóa'} vị trí ${updated.code} thành công`, 'success');
      fetchBins(selectedWh.id);
    } catch (e) {
      addToast('Lỗi đổi trạng thái vị trí ô kệ', 'error');
    }
  };

  const getManagerName = (managerId) => {
    if (!managerId) return 'Chưa gán';
    const user = managerUsers.find(u => u.id === managerId);
    return user ? user.fullName || user.full_name : 'Chưa gán';
  };

  // Helper render progress bar
  const renderCapacityBar = (current, capacity, unit) => {
    if (capacity === null || capacity === undefined) {
      return <span className="text-shade-40">-</span>;
    }
    const capNum = Number(capacity);
    if (isNaN(capNum) || capNum <= 0) {
      return <span className="text-shade-40">-</span>;
    }
    const curNum = Number(current) || 0;
    const pct = (curNum / capNum) * 100;
    let barColor = 'bg-emerald-500'; // Under 80%
    if (pct >= 100) {
      barColor = 'bg-red-500';
    } else if (pct >= 80) {
      barColor = 'bg-amber-500';
    }

    return (
      <div className="flex flex-col gap-1 w-full max-w-[150px]">
        <div className="flex justify-between text-[10px] text-shade-50 font-semibold font-mono">
          <span>{curNum.toFixed(2)} / {capNum.toFixed(0)} {unit}</span>
          <span>{pct.toFixed(0)}%</span>
        </div>
        <div className="w-full bg-zinc-100 rounded-full h-1.5 overflow-hidden border border-zinc-200/50">
          <div 
            className={`h-full rounded-full transition-all duration-300 ${barColor}`} 
            style={{ width: `${Math.min(pct, 100)}%` }}
          />
        </div>
      </div>
    );
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Hệ thống / Admin
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Quản lý kho & Vị trí ô kệ
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Cấu hình kho vật lý, phân khu vực (Zones), các hàng kệ (Racks), tầng kệ (Shelves) và quản lý chi tiết sức chứa tối đa của từng Bin.
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 items-start">
        {/* Left Column: Warehouse List */}
        <div className="lg:col-span-1 bg-white border border-hairline-light rounded-lg shadow-sm card-premium p-5 flex flex-col gap-4">
          <div className="flex justify-between items-center pb-3 border-b border-hairline-light">
            <h3 className="font-bold text-sm flex items-center gap-1.5">
              <Home className="w-4 h-4 text-shade-60" />
              Danh sách Kho
            </h3>
            {hasRole(ROLES.ADMIN) || hasRole(ROLES.CEO) ? (
              <button 
                onClick={handleOpenAddWh}
                className="text-xs font-bold text-ink hover:underline flex items-center gap-0.5"
              >
                <Plus className="w-3.5 h-3.5" /> Thêm kho
              </button>
            ) : null}
          </div>

          {loadingWh ? (
            <div className="flex items-center justify-center p-8">
              <Loader2 className="w-6 h-6 animate-spin text-shade-40" />
            </div>
          ) : (
            <div className="flex flex-col gap-2">
              {warehouses.map((wh) => (
                <div
                  key={wh.id}
                  onClick={() => setSelectedWh(wh)}
                  className={`p-4 rounded-lg border text-left cursor-pointer transition-all ${
                    selectedWh?.id === wh.id
                      ? 'bg-canvas-cream border-ink shadow-sm'
                      : 'bg-white border-hairline-light hover:bg-zinc-50/50'
                  } ${!wh.is_active ? 'opacity-50' : ''}`}
                >
                  <div className="flex justify-between items-start mb-1">
                    <span className="font-mono font-bold text-xs bg-zinc-900 text-white px-2 py-0.5 rounded">
                      {wh.code}
                    </span>
                    <Badge type={wh.is_active ? 'success' : 'neutral'} className="text-[9px] py-0">
                      {wh.is_active ? 'Hoạt động' : 'Khóa'}
                    </Badge>
                  </div>
                  <h4 className="font-bold text-sm text-ink mb-1">{wh.name}</h4>
                  <p className="text-[11px] text-shade-50 mb-2 truncate" title={wh.address}>
                    {wh.address || 'Không có địa chỉ'}
                  </p>
                  <div className="flex justify-between items-center text-[10px] text-shade-60 border-t border-zinc-100 pt-2">
                    <span>Quản lý: <strong>{getManagerName(wh.manager_id)}</strong></span>
                    {(hasRole(ROLES.ADMIN) || hasRole(ROLES.CEO)) ? (
                      <div className="flex gap-1" onClick={(e) => e.stopPropagation()}>
                        <button 
                          onClick={() => handleOpenEditWh(wh)} 
                          className="p-1 hover:bg-zinc-200 rounded-full transition-colors shrink-0"
                          title="Sửa kho"
                        >
                          <Edit className="w-3.5 h-3.5 text-shade-60 hover:text-ink" />
                        </button>
                        <button 
                          onClick={() => handleToggleWhStatus(wh)} 
                          className="p-1 hover:bg-zinc-200 rounded-full transition-colors shrink-0"
                          title={wh.is_active ? 'Khóa kho' : 'Kích hoạt kho'}
                        >
                          {wh.is_active ? (
                            <ToggleRight className="w-4 h-4 text-emerald-600" />
                          ) : (
                            <ToggleLeft className="w-4 h-4 text-shade-40" />
                          )}
                        </button>
                      </div>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Right Column: Bin Locations for Selected Warehouse */}
        <div className="lg:col-span-2 bg-white border border-hairline-light rounded-lg shadow-sm card-premium p-5 flex flex-col gap-4">
          <div className="flex justify-between items-center pb-3 border-b border-hairline-light">
            <div>
              <h3 className="font-bold text-sm flex items-center gap-1.5">
                <Layers className="w-4 h-4 text-shade-60" />
                Vị trí ô kệ (Bin Locations) - {selectedWh?.name}
              </h3>
              <p className="text-[10px] text-shade-40">Các ô kệ nằm trong kho phục vụ quy trình Putaway & Picking.</p>
            </div>
            {selectedWh?.is_active && (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) ? (
              <Button
                variant="outline-light"
                icon={Plus}
                onClick={handleOpenAddBin}
                className="py-1 px-3.5 text-xs font-bold"
              >
                Thêm ô kệ mới
              </Button>
            ) : null}
          </div>

          {loadingBins ? (
            <div className="flex items-center justify-center p-20">
              <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
            </div>
          ) : bins.length === 0 ? (
            <div className="bg-canvas-cream rounded-lg border border-hairline-light p-12 text-center">
              <AlertTriangle className="w-10 h-10 text-shade-30 mx-auto mb-3" />
              <h4 className="font-bold text-sm text-ink mb-1">Chưa có vị trí ô kệ nào được định nghĩa</h4>
              <p className="text-xs text-shade-50">Vui lòng nhấp vào nút để thêm cấu hình ô kệ đầu tiên.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-zinc-50 border-b border-hairline-light">
                    <th className="px-4 py-3 font-bold text-shade-60">Mã ô kệ</th>
                    <th className="px-4 py-3 font-bold text-shade-60 text-center">Loại Khu vực</th>
                    <th className="px-4 py-3 font-bold text-shade-60">Sức chứa Thể tích (m³)</th>
                    <th className="px-4 py-3 font-bold text-shade-60">Sức chứa Khối lượng (kg)</th>
                    <th className="px-4 py-3 font-bold text-shade-60 text-center">Trạng thái</th>
                    <th className="px-4 py-3 font-bold text-shade-60 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {bins.map((bin) => (
                    <tr 
                      key={bin.id} 
                      className={`hover:bg-zinc-50/50 transition-colors ${bin.is_quarantine ? 'bg-amber-50/20' : ''} ${!bin.is_active ? 'opacity-50' : ''}`}
                    >
                      <td className="px-4 py-3">
                        <span className="font-mono font-bold text-xs text-ink">{bin.code}</span>
                      </td>
                      <td className="px-4 py-3 text-center">
                        {bin.is_quarantine ? (
                          <span className="text-[9px] font-bold bg-amber-50 text-amber-700 border border-amber-200 px-2 py-0.5 rounded-pill whitespace-nowrap inline-flex items-center gap-1">
                            <ShieldAlert className="w-2.5 h-2.5" /> Quarantine
                          </span>
                        ) : (
                          <span className="text-[9px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 px-2 py-0.5 rounded-pill whitespace-nowrap inline-flex items-center gap-1">
                            Storage Bin
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        {renderCapacityBar(bin.current_volume_m3 || 0, bin.capacity_m3, 'm³')}
                      </td>
                      <td className="px-4 py-3">
                        {renderCapacityBar(bin.current_weight_kg || 0, bin.capacity_kg, 'kg')}
                      </td>
                      <td className="px-4 py-3 text-center">
                        <Badge type={bin.is_active ? 'success' : 'neutral'} className="text-[9px]">
                          {bin.is_active ? 'Hoạt động' : 'Khóa'}
                        </Badge>
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex gap-2 justify-end items-center whitespace-nowrap">
                          {hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN) ? (
                            <button
                              onClick={() => handleOpenEditBin(bin)}
                              className="p-1 hover:bg-zinc-100 rounded-full transition-colors shrink-0"
                              title="Sửa ô kệ"
                            >
                              <Edit className="w-4 h-4 text-shade-60 hover:text-ink" />
                            </button>
                          ) : null}
                          <button
                            onClick={() => handleToggleBinStatus(bin)}
                            className="p-1 hover:bg-zinc-100 rounded-full transition-colors shrink-0"
                            title={bin.is_active ? 'Khóa ô kệ' : 'Kích hoạt ô kệ'}
                          >
                            {bin.is_active ? (
                              <ToggleRight className="w-5 h-5 text-emerald-600" />
                            ) : (
                              <ToggleLeft className="w-5 h-5 text-shade-40" />
                            )}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {/* Warehouse Modal */}
      <Modal
        isOpen={isWhModalOpen}
        onClose={() => setIsWhModalOpen(false)}
        title={whModalType === 'ADD' ? 'Thêm mới kho vật lý / trung chuyển' : 'Cập nhật cấu hình kho'}
        maxWidth="max-w-md"
      >
        <form onSubmit={handleWhSubmit} className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Mã kho (Unique)"
              value={whCode}
              onChange={(e) => setWhCode(e.target.value.toUpperCase())}
              disabled={whModalType === 'EDIT'}
              error={whFormErrors.code}
              placeholder="VD: HP-01"
              required
            />
            <Input
              label="Loại kho"
              type="select"
              value={whType}
              onChange={(e) => setWhType(e.target.value)}
              options={[
                { value: 'PHYSICAL', label: 'Vật lý (Physical)' },
                { value: 'IN_TRANSIT', label: 'Đang đi đường (In-Transit)' },
              ]}
            />
          </div>
          <Input
            label="Tên kho"
            value={whName}
            onChange={(e) => setWhName(e.target.value)}
            error={whFormErrors.name}
            placeholder="VD: Kho Hải Phòng - Phúc Anh"
            required
          />
          <Input
            label="Số điện thoại liên hệ"
            value={whPhone}
            onChange={(e) => setWhPhone(e.target.value)}
            placeholder="VD: 0225 3888 999"
          />
          <Input
            label="Địa chỉ kho"
            value={whAddress}
            onChange={(e) => setWhAddress(e.target.value)}
            placeholder="Nhập địa chỉ chi tiết..."
          />
          <Input
            label="Người quản lý kho"
            type="select"
            value={whManagerId}
            onChange={(e) => setWhManagerId(e.target.value)}
            disabled={loadingManagers}
            options={[
              { value: '', label: loadingManagers ? 'Đang tải...' : 'Chưa gán quản lý' },
              ...managerUsers.map(u => ({
                value: String(u.id),
                label: `${u.fullName || u.full_name} (${u.jobTitle || u.job_title || u.role})`
              }))
            ]}
          />

          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4 mt-2">
            <Button variant="outline-light" onClick={() => setIsWhModalOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" variant="primary" loading={whSubmitting}>
              {whModalType === 'ADD' ? 'Tạo mới' : 'Lưu thay đổi'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Bin Location Modal */}
      <Modal
        isOpen={isBinModalOpen}
        onClose={() => setIsBinModalOpen(false)}
        title={binModalType === 'ADD' ? `Thêm ô kệ mới tại kho ${selectedWh?.code}` : `Sửa ô kệ ${selectedBin?.code}`}
        maxWidth="max-w-md"
      >
        <form onSubmit={handleBinSubmit} className="flex flex-col gap-4">
          {binModalType === 'ADD' ? (
            <div className="flex flex-col gap-3">
              <span className="text-[10px] text-shade-40 block uppercase font-bold tracking-wider leading-none">Cấu trúc phân cấp ô kệ:</span>
              <div className="grid grid-cols-4 gap-2">
                <Input
                  label="Zone"
                  value={binZone}
                  onChange={(e) => setBinZone(e.target.value)}
                  error={binFormErrors.zone}
                  placeholder="VD: Z1"
                  required
                />
                <Input
                  label="Rack"
                  value={binRack}
                  onChange={(e) => setBinRack(e.target.value)}
                  error={binFormErrors.rack}
                  placeholder="VD: R1"
                  required
                />
                <Input
                  label="Shelf"
                  value={binShelf}
                  onChange={(e) => setBinShelf(e.target.value)}
                  error={binFormErrors.shelf}
                  placeholder="VD: S1"
                  required
                />
                <Input
                  label="Bin"
                  value={binName}
                  onChange={(e) => setBinName(e.target.value)}
                  error={binFormErrors.bin}
                  placeholder="VD: B01"
                  required
                />
              </div>
              <div className="bg-zinc-50 border border-zinc-200/50 p-2.5 rounded font-mono text-[10px] text-center text-shade-60">
                Mã định danh tự động: <strong className="text-ink">{selectedWh?.code}.{binZone.toUpperCase() || '?'}.{binRack.toUpperCase() || '?'}.{binShelf.toUpperCase() || '?'}.{binName.toUpperCase() || '?'}</strong>
              </div>
            </div>
          ) : (
            <div className="bg-zinc-100 p-3 rounded font-mono text-xs text-ink font-bold text-center border border-zinc-200">
              Đang chỉnh sửa ô kệ: {selectedBin?.code}
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Thể tích chứa tối đa (m³)"
              type="number"
              value={binCapacityM3}
              onChange={(e) => setBinCapacityM3(e.target.value)}
              error={binFormErrors.capacity_m3}
              min="0"
              step="0.1"
              required
            />
            <Input
              label="Tải trọng tối đa (kg)"
              type="number"
              value={binCapacityKg}
              onChange={(e) => setBinCapacityKg(e.target.value)}
              error={binFormErrors.capacity_kg}
              min="0"
              step="1"
              required
            />
          </div>

          <label className="flex items-center gap-2 cursor-pointer bg-zinc-50 p-3.5 rounded border border-hairline-light mt-1">
            <input
              type="checkbox"
              checked={binIsQuarantine}
              onChange={(e) => setBinIsQuarantine(e.target.checked)}
              className="w-4 h-4 rounded border-hairline-light text-ink focus:ring-ink"
            />
            <div className="flex flex-col">
              <span className="text-xs font-bold text-ink">Khu vực kiểm định cách ly (Quarantine Zone)</span>
              <span className="text-[10px] text-shade-50 leading-none">Chỉ dùng để chứa hàng hóa chưa qua QC hoặc QC thất bại</span>
            </div>
          </label>

          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4 mt-2">
            <Button variant="outline-light" onClick={() => setIsBinModalOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" variant="primary" loading={binSubmitting}>
              {binModalType === 'ADD' ? 'Tạo mới' : 'Lưu thay đổi'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default WarehouseManagement;
