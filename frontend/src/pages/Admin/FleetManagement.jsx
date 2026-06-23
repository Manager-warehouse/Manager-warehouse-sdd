import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { masterDataService } from '../../services/masterData.service';
import { ROLES, WAREHOUSES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import { Plus, Search, Edit, ToggleLeft, ToggleRight, AlertCircle, Loader2, Truck, UserCheck, Calendar, ShieldAlert } from 'lucide-react';

const FleetManagement = () => {
  const { user, activeWarehouse, hasRole } = useAuthStore();
  const { addToast } = useUiStore();

  const [activeTab, setActiveTab] = useState('VEHICLES'); // VEHICLES or DRIVERS
  const [vehicles, setVehicles] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [driverUsers, setDriverUsers] = useState([]);
  const [driverUserLoadFailed, setDriverUserLoadFailed] = useState(false);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  // Vehicle Modal States
  const [isVhModalOpen, setIsVhModalOpen] = useState(false);
  const [vhModalType, setVhModalType] = useState('ADD'); // ADD or EDIT
  const [selectedVehicle, setSelectedVehicle] = useState(null);
  const [vhSubmitting, setVhSubmitting] = useState(false);
  const [vhFormErrors, setVhFormErrors] = useState({});

  // Vehicle Fields
  const [vhPlateNumber, setVhPlateNumber] = useState('');
  const [vhType, setVhType] = useState('');
  const [vhMaxWeight, setVhMaxWeight] = useState('1500');
  const [vhMaxVolume, setVhMaxVolume] = useState('10');
  const [vhWarehouseId, setVhWarehouseId] = useState('1');
  const [vhStatus, setVhStatus] = useState('AVAILABLE');

  // Driver Modal States
  const [isDrModalOpen, setIsDrModalOpen] = useState(false);
  const [drModalType, setDrModalType] = useState('ADD'); // ADD or EDIT
  const [selectedDriver, setSelectedDriver] = useState(null);
  const [drSubmitting, setDrSubmitting] = useState(false);
  const [drFormErrors, setDrFormErrors] = useState({});

  // Driver Fields
  const [drUserId, setDrUserId] = useState('');
  const [drFullName, setDrFullName] = useState('');
  const [drPhone, setDrPhone] = useState('');
  const [drLicenseNumber, setDrLicenseNumber] = useState('');
  const [drLicenseExpiry, setDrLicenseExpiry] = useState('');
  const [drStatus, setDrStatus] = useState('AVAILABLE');

  const getUserId = (user) => user?.id;
  const getUserCode = (user) => user?.code;
  const getUserFullName = (user) => user?.full_name || user?.fullName || '';
  const getUserPhone = (user) => user?.phone || '';
  const getUserWarehouses = (user) => user?.warehouse_ids || user?.warehouseIds || user?.warehouses || [];
  const isUserActive = (user) => user?.is_active !== false && user?.isActive !== false;
  const hasGlobalFleetScope = hasRole(ROLES.ADMIN) || hasRole(ROLES.CEO);
  const fleetWarehouseIds = hasGlobalFleetScope
    ? []
    : (
      hasRole(ROLES.DISPATCHER) && activeWarehouse?.id
        ? [Number(activeWarehouse.id)]
        : (user?.warehouses || []).map(Number)
    );
  const isInFleetWarehouseScope = (warehouseIds = []) => (
    hasGlobalFleetScope
    || fleetWarehouseIds.some((id) => (warehouseIds || []).map(Number).includes(id))
  );
  const fleetWarehouses = hasGlobalFleetScope
    ? WAREHOUSES
    : WAREHOUSES.filter((warehouse) => fleetWarehouseIds.includes(warehouse.id));

  useEffect(() => {
    fetchData();
  }, [activeTab]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'VEHICLES') {
        const data = await masterDataService.getVehicles();
        setVehicles(data);
      } else {
        const [driverResult, userResult] = await Promise.allSettled([
          masterDataService.getDrivers(),
          masterDataService.getDriverUserCandidates(),
        ]);
        if (driverResult.status === 'rejected') {
          throw driverResult.reason;
        }
        const driverData = driverResult.value || [];
        const userData = userResult.status === 'fulfilled' ? userResult.value : [];
        setDrivers(driverData);
        setDriverUsers(Array.isArray(userData) ? userData.filter((user) => user.role === ROLES.DRIVER) : []);
        setDriverUserLoadFailed(userResult.status === 'rejected');
      }
    } catch (e) {
      addToast('Lỗi tải dữ liệu đội xe', 'error');
    } finally {
      setLoading(false);
    }
  };

  // --- VEHICLE ACTIONS ---
  const handleOpenAddVehicle = () => {
    setVhModalType('ADD');
    setSelectedVehicle(null);
    setVhPlateNumber('');
    setVhType('');
    setVhMaxWeight('1500');
    setVhMaxVolume('10');
    setVhWarehouseId(String(fleetWarehouses[0]?.id || ''));
    setVhStatus('AVAILABLE');
    setVhFormErrors({});
    setIsVhModalOpen(true);
  };

  const handleOpenEditVehicle = (vehicle) => {
    setVhModalType('EDIT');
    setSelectedVehicle(vehicle);
    setVhPlateNumber(vehicle.plate_number);
    setVhType(vehicle.vehicle_type);
    setVhMaxWeight(String(vehicle.max_weight_kg));
    setVhMaxVolume(String(vehicle.max_volume_m3));
    setVhWarehouseId(String(vehicle.warehouse_id || vehicle.warehouseId || 1));
    setVhStatus(vehicle.status || 'AVAILABLE');
    setVhFormErrors({});
    setIsVhModalOpen(true);
  };

  const validateVhForm = () => {
    const errors = {};
    if (!vhPlateNumber.trim()) errors.plate_number = 'Biển số xe bắt buộc';
    if (!vhType.trim()) errors.vehicle_type = 'Loại xe tải bắt buộc';
    if (!vhWarehouseId) errors.warehouse_id = 'Kho phụ trách bắt buộc';
    if (Number(vhMaxWeight) <= 0) errors.max_weight = 'Tải trọng tối đa phải lớn hơn 0';
    if (Number(vhMaxVolume) <= 0) errors.max_volume = 'Thể tích tối đa phải lớn hơn 0';
    setVhFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleVhSubmit = async (e) => {
    e.preventDefault();
    if (!validateVhForm()) return;

    setVhSubmitting(true);
    const vhData = {
      plate_number: vhPlateNumber.trim().toUpperCase(),
      vehicle_type: vhType.trim(),
      max_weight_kg: parseFloat(vhMaxWeight),
      max_volume_m3: parseFloat(vhMaxVolume),
      warehouse_id: Number(vhWarehouseId),
      status: vhStatus,
    };

    try {
      if (vhModalType === 'ADD') {
        await masterDataService.createVehicle(vhData);
        addToast(`Đăng ký xe tải ${vhPlateNumber.toUpperCase()} thành công`, 'success');
      } else {
        await masterDataService.updateVehicle(selectedVehicle.id, vhData);
        addToast(`Cập nhật xe tải ${vhPlateNumber.toUpperCase()} thành công`, 'success');
      }
      setIsVhModalOpen(false);
      fetchData();
    } catch (err) {
      if (err.message === 'DUPLICATE_PLATE_NUMBER') {
        setVhFormErrors({ plate_number: 'Biển số xe này đã được đăng ký' });
      } else {
        addToast('Lỗi lưu trữ xe tải', 'error');
      }
    } finally {
      setVhSubmitting(false);
    }
  };

  const handleToggleVhStatus = async (vehicle) => {
    if (vehicle.is_active && !hasRole(ROLES.ADMIN) && !hasRole(ROLES.CEO)) {
      addToast('Chỉ Quản trị viên hoặc CEO mới có quyền tắt kích hoạt phương tiện', 'warning');
      return;
    }
    try {
      const updated = await masterDataService.toggleVehicleStatus(vehicle.id, !vehicle.is_active);
      addToast(`${updated.is_active ? 'Kích hoạt' : 'Khóa'} xe tải ${updated.plate_number} thành công`, 'success');
      fetchData();
    } catch (e) {
      addToast('Lỗi thay đổi trạng thái xe', 'error');
    }
  };

  // --- DRIVER ACTIONS ---
  const handleOpenAddDriver = (user = null) => {
    setDrModalType('ADD');
    setSelectedDriver(null);
    setDrUserId(user ? String(getUserId(user)) : '');
    setDrFullName(getUserFullName(user));
    setDrPhone(getUserPhone(user));
    setDrLicenseNumber('');
    setDrLicenseExpiry('');
    setDrStatus('AVAILABLE');
    setDrFormErrors({});
    setIsDrModalOpen(true);
  };

  const handleOpenEditDriver = (driver) => {
    setDrModalType('EDIT');
    setSelectedDriver(driver);
    setDrUserId(String(driver.user_id));
    setDrFullName(driver.full_name);
    setDrPhone(driver.phone || '');
    setDrLicenseNumber(driver.license_number);
    setDrLicenseExpiry(driver.license_expiry);
    setDrStatus(driver.status || 'AVAILABLE');
    setDrFormErrors({});
    setIsDrModalOpen(true);
  };

  const validateDrForm = () => {
    const errors = {};
    if (drModalType === 'ADD') {
      if (!drUserId) errors.user_id = 'Tài khoản tài xế bắt buộc';
    }
    if (!drFullName.trim()) errors.full_name = 'Họ tên tài xế bắt buộc';
    if (!drLicenseNumber.trim()) errors.license_number = 'Số bằng lái bắt buộc';
    if (!drLicenseExpiry) errors.license_expiry = 'Ngày hết hạn bằng lái bắt buộc';

    setDrFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleDrSubmit = async (e) => {
    e.preventDefault();
    if (!validateDrForm()) return;

    setDrSubmitting(true);
    const driverData = {
      user_id: Number(drUserId),
      warehouse_ids: getUserWarehouses(driverUsers.find((user) => Number(getUserId(user)) === Number(drUserId))),
      full_name: drFullName.trim(),
      phone: drPhone.trim(),
      license_number: drLicenseNumber.trim(),
      license_expiry: drLicenseExpiry,
      status: drStatus,
    };

    try {
      if (drModalType === 'ADD') {
        await masterDataService.createDriver(driverData);
        addToast(`Thêm tài xế ${drFullName} thành công`, 'success');
      } else {
        await masterDataService.updateDriver(selectedDriver.id, driverData);
        addToast(`Cập nhật tài xế ${drFullName} thành công`, 'success');
      }
      setIsDrModalOpen(false);
      fetchData();
    } catch (err) {
      addToast(err.message || 'Lỗi lưu trữ thông tin tài xế', 'error');
    } finally {
      setDrSubmitting(false);
    }
  };

  const handleToggleDrStatus = async (driver) => {
    if (driver.is_active && !hasRole(ROLES.ADMIN) && !hasRole(ROLES.CEO)) {
      addToast('Chỉ Quản trị viên hoặc CEO mới có quyền tắt kích hoạt tài xế', 'warning');
      return;
    }
    try {
      const updated = await masterDataService.toggleDriverStatus(driver.id, !driver.is_active);
      addToast(`${updated.is_active ? 'Kích hoạt' : 'Khóa'} tài xế ${updated.full_name} thành công`, 'success');
      fetchData();
    } catch (e) {
      addToast('Lỗi đổi trạng thái tài xế', 'error');
    }
  };

  // --- EXPIRY UTILS ---
  const getExpiryBadge = (dateStr) => {
    if (!dateStr) return <span className="text-shade-40">N/A</span>;
    const expiry = new Date(dateStr);
    const today = new Date();
    today.setHours(0,0,0,0);
    const diffTime = expiry - today;
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays < 0) {
      return (
        <span className="text-[10px] font-bold bg-red-50 text-red-700 border border-red-200 px-2 py-0.5 rounded-pill whitespace-nowrap inline-flex items-center gap-1">
          <ShieldAlert className="w-3 h-3" /> ĐÃ HẾT HẠN
        </span>
      );
    }
    if (diffDays <= 30) {
      return (
        <span className="text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 px-2 py-0.5 rounded-pill whitespace-nowrap inline-flex items-center gap-1" title={`Bằng lái sẽ hết hạn vào ngày ${dateStr}`}>
          <Calendar className="w-3 h-3" /> Hạn còn {diffDays} ngày
        </span>
      );
    }
    return <span className="font-mono text-shade-60">{dateStr}</span>;
  };

  const getVehicleStatusBadge = (status) => {
    const styles = {
      AVAILABLE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      ON_TRIP: 'bg-blue-50 text-blue-700 border-blue-200',
      MAINTENANCE: 'bg-amber-50 text-amber-700 border-amber-200',
    };
    const labels = {
      AVAILABLE: 'Sẵn sàng',
      ON_TRIP: 'Đang đi giao',
      MAINTENANCE: 'Bảo dưỡng',
    };
    return (
      <span className={`text-[10px] font-bold border px-2 py-0.5 rounded-pill whitespace-nowrap ${styles[status] || styles.AVAILABLE}`}>
        {labels[status] || status}
      </span>
    );
  };

  const getDriverStatusBadge = (status) => {
    const styles = {
      AVAILABLE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
      ON_TRIP: 'bg-blue-50 text-blue-700 border-blue-200',
      UNAVAILABLE: 'bg-amber-50 text-amber-700 border-amber-200',
    };
    const labels = {
      AVAILABLE: 'Sẵn sàng',
      ON_TRIP: 'Đang chạy chuyến',
      UNAVAILABLE: 'Không khả dụng',
    };
    return (
      <span className={`text-[10px] font-bold border px-2 py-0.5 rounded-pill whitespace-nowrap ${styles[status] || styles.AVAILABLE}`}>
        {labels[status] || status}
      </span>
    );
  };

  const getWarehouseLabels = (warehouseIds = []) => {
    if (!Array.isArray(warehouseIds) || warehouseIds.length === 0) return '-';
    return warehouseIds
      .map((id) => WAREHOUSES.find((warehouse) => warehouse.id === Number(id))?.code)
      .filter(Boolean)
      .join(', ') || '-';
  };
  const searchable = (value) => String(value ?? '').toLowerCase();

  // --- FILTERS ---
  const filteredVehicles = vehicles.filter((v) => {
    const warehouseId = Number(v.warehouse_id || v.warehouseId);
    const matchesWarehouse = hasGlobalFleetScope || fleetWarehouseIds.includes(warehouseId);
    const matchesSearch = searchable(v.plate_number).includes(searchable(searchTerm))
      || searchable(v.vehicle_type).includes(searchable(searchTerm))
      || searchable(v.warehouse_code).includes(searchable(searchTerm));
    return matchesWarehouse && matchesSearch;
  });

  const driverDirectory = [
    ...drivers.map((profile) => {
      const user = driverUsers.find((item) => Number(getUserId(item)) === Number(profile.user_id || profile.userId));
      return {
        ...profile,
        rowType: 'PROFILE',
        accountCode: getUserCode(user),
        accountWarehouses: getUserWarehouses(user).length ? getUserWarehouses(user) : profile.warehouse_ids || profile.warehouseIds || [],
      };
    }),
    ...driverUsers.filter((user) => (
      !drivers.some((driver) => Number(driver.user_id || driver.userId) === Number(getUserId(user)))
    )).map((user) => ({
      id: `account-${getUserId(user)}`,
      rowType: 'ACCOUNT_ONLY',
      user_id: getUserId(user),
      accountCode: getUserCode(user),
      full_name: getUserFullName(user),
      phone: getUserPhone(user),
      license_number: '',
      license_expiry: '',
      status: 'NO_PROFILE',
      is_active: isUserActive(user),
      accountWarehouses: getUserWarehouses(user),
      user,
    })),
  ].filter((driver) => isInFleetWarehouseScope(
    driver.warehouse_ids || driver.warehouseIds || driver.accountWarehouses
  ));
  const linkedDriverUserIds = new Set(drivers.map((driver) => Number(driver.user_id || driver.userId)));
  const selectableDriverUsers = driverUsers.filter((user) => (
    isInFleetWarehouseScope(getUserWarehouses(user))
    && (
      drModalType === 'EDIT'
        || !linkedDriverUserIds.has(Number(getUserId(user)))
        || Number(getUserId(user)) === Number(drUserId)
    )
  ));

  const filteredDrivers = driverDirectory.filter((d) =>
    searchable(d.full_name || d.fullName).includes(searchable(searchTerm)) ||
    searchable(d.license_number || d.licenseNumber).includes(searchable(searchTerm)) ||
    searchable(d.accountCode).includes(searchable(searchTerm)) ||
    searchable(getWarehouseLabels(d.warehouse_ids || d.warehouseIds || d.accountWarehouses)).includes(searchable(searchTerm))
  );

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Hệ thống / Admin
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Đội xe & Tài xế nội bộ
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản lý đội ngũ phương tiện vận tải nội bộ Phúc Anh và thông tin giấy phép lái xe, trạng thái làm việc của tài xế.
          </p>
        </div>
        <div>
          {activeTab === 'VEHICLES' ? (
            hasRole(ROLES.DISPATCHER) || hasRole(ROLES.ADMIN) || hasRole(ROLES.CEO) ? (
              <Button variant="primary" icon={Plus} onClick={handleOpenAddVehicle}>
                Đăng ký xe tải
              </Button>
            ) : null
          ) : (
            hasRole(ROLES.DISPATCHER) || hasRole(ROLES.ADMIN) || hasRole(ROLES.CEO) ? (
              <Button variant="primary" icon={Plus} onClick={handleOpenAddDriver}>
                Thêm tài xế mới
              </Button>
            ) : null
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-hairline-light mb-6">
        <button
          onClick={() => { setActiveTab('VEHICLES'); setSearchTerm(''); }}
          className={`px-5 py-3 font-semibold text-sm transition-all border-b-2 flex items-center gap-2 ${
            activeTab === 'VEHICLES'
              ? 'border-ink text-ink bg-white/50 rounded-t-lg'
              : 'border-transparent text-shade-50 hover:text-ink'
          }`}
        >
          <Truck className="w-4 h-4" />
          Phương tiện vận tải (Vehicles)
        </button>
        <button
          onClick={() => { setActiveTab('DRIVERS'); setSearchTerm(''); }}
          className={`px-5 py-3 font-semibold text-sm transition-all border-b-2 flex items-center gap-2 ${
            activeTab === 'DRIVERS'
              ? 'border-ink text-ink bg-white/50 rounded-t-lg'
              : 'border-transparent text-shade-50 hover:text-ink'
          }`}
        >
          <UserCheck className="w-4 h-4" />
          Đội ngũ Tài xế (Drivers)
        </button>
      </div>

      {/* Search Bar */}
      <div className="bg-white border border-hairline-light rounded-lg p-4 mb-6 shadow-sm">
        <div className="relative flex-1 w-full max-w-md">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
          <input
            type="text"
            placeholder={activeTab === 'VEHICLES' ? 'Tìm theo biển số hoặc loại xe...' : 'Tìm theo tên hoặc số bằng lái tài xế...'}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-canvas-light text-ink text-sm pl-10 pr-4 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink min-h-[44px]"
          />
        </div>
      </div>

      {/* Main Tables */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : activeTab === 'VEHICLES' ? (
        filteredVehicles.length === 0 ? (
          <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
            <AlertCircle className="w-12 h-12 text-shade-30 mx-auto mb-4" />
            <h3 className="text-lg font-bold mb-1">Không tìm thấy xe tải</h3>
            <p className="text-sm text-shade-50">Thử thay đổi bộ lọc tìm kiếm hoặc đăng ký xe mới.</p>
          </div>
        ) : (
          <div className="bg-white rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-zinc-50 border-b border-hairline-light">
                    <th className="px-6 py-4 font-bold text-shade-60">Biển kiểm soát</th>
                    <th className="px-6 py-4 font-bold text-shade-60">Dòng xe / Model</th>
                    <th className="px-6 py-4 font-bold text-shade-60">Kho phụ trách</th>
                    <th className="px-6 py-4 font-bold text-shade-60 text-right">Tải trọng tối đa (kg)</th>
                    <th className="px-6 py-4 font-bold text-shade-60 text-right">Thể tích tối đa (m³)</th>
                    <th className="px-6 py-4 font-bold text-shade-60 text-center">Trạng thái vận chuyển</th>
                    <th className="px-6 py-4 font-bold text-shade-60 text-center">Hoạt động</th>
                    <th className="px-6 py-4 font-bold text-shade-60 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {filteredVehicles.map((vh) => (
                    <tr key={vh.id} className={`hover:bg-zinc-50/50 transition-colors ${!vh.is_active ? 'opacity-50' : ''}`}>
                      <td className="px-6 py-4">
                        <span className="font-mono font-bold text-ink bg-zinc-100 border border-zinc-200 px-2 py-1 rounded">
                          {vh.plate_number}
                        </span>
                      </td>
                      <td className="px-6 py-4 font-semibold text-ink">{vh.vehicle_type}</td>
                      <td className="px-6 py-4 text-shade-60 font-semibold">
                        {vh.warehouse_code || WAREHOUSES.find((warehouse) => warehouse.id === Number(vh.warehouse_id || vh.warehouseId))?.code || '-'}
                      </td>
                      <td className="px-6 py-4 text-right font-mono text-shade-70 font-semibold">
                        {vh.max_weight_kg?.toLocaleString('vi-VN')} kg
                      </td>
                      <td className="px-6 py-4 text-right font-mono text-shade-60">
                        {vh.max_volume_m3?.toFixed(2)} m³
                      </td>
                      <td className="px-6 py-4 text-center">
                        {getVehicleStatusBadge(vh.status)}
                      </td>
                      <td className="px-6 py-4 text-center">
                        <Badge type={vh.is_active ? 'success' : 'neutral'} className="text-[9px]">
                          {vh.is_active ? 'Hoạt động' : 'Khóa'}
                        </Badge>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex gap-3.5 justify-end items-center font-bold">
                          {hasRole(ROLES.DISPATCHER) || hasRole(ROLES.ADMIN) || hasRole(ROLES.CEO) ? (
                            <button
                              onClick={() => handleOpenEditVehicle(vh)}
                              className="p-1 hover:bg-zinc-100 rounded-full transition-colors shrink-0 text-shade-60 hover:text-ink"
                              title="Sửa xe tải"
                            >
                              <Edit className="w-4 h-4" />
                            </button>
                          ) : null}
                          <button
                            onClick={() => handleToggleVhStatus(vh)}
                            className="p-1 hover:bg-zinc-100 rounded-full transition-colors shrink-0"
                            title={vh.is_active ? 'Khóa xe tải' : 'Kích hoạt xe tải'}
                          >
                            {vh.is_active ? (
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
          </div>
        )
      ) : (
        // DRIVERS TAB
        filteredDrivers.length === 0 ? (
          <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
            <AlertCircle className="w-12 h-12 text-shade-30 mx-auto mb-4" />
            <h3 className="text-lg font-bold mb-1">Không tìm thấy tài xế</h3>
            <p className="text-sm text-shade-50">Thử thay đổi bộ lọc tìm kiếm hoặc thêm mới tài xế.</p>
          </div>
        ) : (
          <div className="bg-white rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-zinc-50 border-b border-hairline-light">
                    <th className="px-6 py-4 font-bold text-shade-60">Họ và tên</th>
                    <th className="px-6 py-4 font-bold text-shade-60">ID nhân viên</th>
                    <th className="px-6 py-4 font-bold text-shade-60">Kho phụ trách</th>
                    <th className="px-6 py-4 font-bold text-shade-60">Số điện thoại</th>
                    <th className="px-6 py-4 font-bold text-shade-60">Số giấy phép lái xe</th>
                    <th className="px-6 py-4 font-bold text-shade-60">Hạn bằng lái</th>
                    <th className="px-6 py-4 font-bold text-shade-60 text-center">Trạng thái làm việc</th>
                    <th className="px-6 py-4 font-bold text-shade-60 text-center">Hoạt động</th>
                    <th className="px-6 py-4 font-bold text-shade-60 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {filteredDrivers.map((dr) => (
                    <tr key={dr.id} className={`hover:bg-zinc-50/50 transition-colors ${!dr.is_active ? 'opacity-50' : ''}`}>
                      <td className="px-6 py-4 font-semibold text-ink">{dr.full_name}</td>
                      <td className="px-6 py-4 font-mono text-shade-50">{dr.accountCode || `NV-${String(dr.user_id).padStart(3, '0')}`}</td>
                      <td className="px-6 py-4 text-shade-60 font-semibold">
                        {getWarehouseLabels(dr.warehouse_ids || dr.warehouseIds || dr.accountWarehouses)}
                      </td>
                      <td className="px-6 py-4 text-shade-60 font-mono">{dr.phone || 'N/A'}</td>
                      <td className="px-6 py-4 text-shade-60 font-mono font-bold">{dr.license_number || '-'}</td>
                      <td className="px-6 py-4">
                        {getExpiryBadge(dr.license_expiry)}
                      </td>
                      <td className="px-6 py-4 text-center">
                        {dr.rowType === 'ACCOUNT_ONLY' ? (
                          <span className="text-[10px] font-bold border px-2 py-0.5 rounded-pill whitespace-nowrap bg-amber-50 text-amber-700 border-amber-200">
                            Chưa có hồ sơ
                          </span>
                        ) : getDriverStatusBadge(dr.status)}
                      </td>
                      <td className="px-6 py-4 text-center">
                        <Badge type={dr.is_active ? 'success' : 'neutral'} className="text-[9px]">
                          {dr.is_active ? 'Hoạt động' : 'Khóa'}
                        </Badge>
                      </td>
                      <td className="px-6 py-4">
                        <div className="flex gap-3.5 justify-end items-center font-bold">
                          {hasRole(ROLES.DISPATCHER) || hasRole(ROLES.ADMIN) || hasRole(ROLES.CEO) ? (
                            <button
                              onClick={() => (dr.rowType === 'ACCOUNT_ONLY' ? handleOpenAddDriver(dr.user) : handleOpenEditDriver(dr))}
                              className="p-1 hover:bg-zinc-100 rounded-full transition-colors shrink-0 text-shade-60 hover:text-ink"
                              title={dr.rowType === 'ACCOUNT_ONLY' ? 'Tạo hồ sơ tài xế' : 'Sửa hồ sơ tài xế'}
                            >
                              <Edit className="w-4 h-4" />
                            </button>
                          ) : null}
                          {dr.rowType === 'PROFILE' && (
                            <button
                              onClick={() => handleToggleDrStatus(dr)}
                              className="p-1 hover:bg-zinc-100 rounded-full transition-colors shrink-0"
                              title={dr.is_active ? 'Khóa tài xế' : 'Kích hoạt tài xế'}
                            >
                              {dr.is_active ? (
                                <ToggleRight className="w-5 h-5 text-emerald-600" />
                              ) : (
                                <ToggleLeft className="w-5 h-5 text-shade-40" />
                              )}
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )
      )}

      {/* Vehicle Modal */}
      <Modal
        isOpen={isVhModalOpen}
        onClose={() => setIsVhModalOpen(false)}
        title={vhModalType === 'ADD' ? 'Đăng ký Phương tiện Vận tải mới' : 'Sửa thông tin Xe tải'}
        maxWidth="max-w-md"
      >
        <form onSubmit={handleVhSubmit} className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Biển số xe (Unique)"
              value={vhPlateNumber}
              onChange={(e) => setVhPlateNumber(e.target.value.toUpperCase())}
              disabled={vhModalType === 'EDIT'}
              error={vhFormErrors.plate_number}
              placeholder="VD: 15C-234.56"
              required
            />
            <Input
              label="Trạng thái xe"
              type="select"
              value={vhStatus}
              onChange={(e) => setVhStatus(e.target.value)}
              options={[
                { value: 'AVAILABLE', label: 'Sẵn sàng hoạt động' },
                { value: 'ON_TRIP', label: 'Đang đi giao hàng' },
                { value: 'MAINTENANCE', label: 'Đang sửa chữa bảo dưỡng' },
              ]}
            />
          </div>

          <Input
            label="Dòng xe / Tải trọng / Model"
            value={vhType}
            onChange={(e) => setVhType(e.target.value)}
            error={vhFormErrors.vehicle_type}
            placeholder="VD: Xe tải Hyundai H150 1.5 Tấn"
            required
          />

          <Input
            label="Kho phụ trách"
            type="select"
            value={vhWarehouseId}
            onChange={(e) => setVhWarehouseId(e.target.value)}
            error={vhFormErrors.warehouse_id}
            options={fleetWarehouses.map((warehouse) => ({
              value: String(warehouse.id),
              label: `${warehouse.code} - ${warehouse.name}`,
            }))}
            required
          />

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Tải trọng tối đa (kg)"
              type="number"
              value={vhMaxWeight}
              onChange={(e) => setVhMaxWeight(e.target.value)}
              error={vhFormErrors.max_weight}
              min="1"
              required
            />
            <Input
              label="Thể tích chứa tối đa (m³)"
              type="number"
              value={vhMaxVolume}
              onChange={(e) => setVhMaxVolume(e.target.value)}
              error={vhFormErrors.max_volume}
              min="0.1"
              step="0.01"
              required
            />
          </div>

          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4 mt-2">
            <Button variant="outline-light" onClick={() => setIsVhModalOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" variant="primary" loading={vhSubmitting}>
              {vhModalType === 'ADD' ? 'Tạo mới' : 'Lưu thay đổi'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Driver Modal */}
      <Modal
        isOpen={isDrModalOpen}
        onClose={() => setIsDrModalOpen(false)}
        title={drModalType === 'ADD' ? 'Thêm mới Hồ sơ Tài xế' : 'Sửa hồ sơ Tài xế'}
        maxWidth="max-w-md"
      >
        <form onSubmit={handleDrSubmit} className="flex flex-col gap-4">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Liên kết tài khoản"
              type="select"
              value={drUserId}
              onChange={(e) => {
                setDrUserId(e.target.value);
                // Pre-fill full name and phone if matching user
                const user = driverUsers.find(u => Number(getUserId(u)) === Number(e.target.value));
                if (user) {
                  setDrFullName(getUserFullName(user));
                  setDrPhone(getUserPhone(user));
                }
              }}
              disabled={drModalType === 'EDIT' || (drModalType === 'ADD' && selectableDriverUsers.length === 0)}
              error={drFormErrors.user_id}
              options={[
                {
                  value: '',
                  label: driverUserLoadFailed
                    ? 'Không tải được danh sách tài khoản DRIVER'
                    : selectableDriverUsers.length
                      ? 'Chọn tài khoản tài xế'
                      : 'Không còn tài khoản DRIVER chưa có hồ sơ',
                },
                ...selectableDriverUsers.map(u => ({
                  value: String(getUserId(u)),
                  label: `${getUserFullName(u)} (${getUserCode(u) || `NV-${String(getUserId(u)).padStart(3, '0')}`}) - ${getWarehouseLabels(getUserWarehouses(u))}`
                }))
              ]}
              required
            />
            <Input
              label="Trạng thái điều phối"
              type="select"
              value={drStatus}
              onChange={(e) => setDrStatus(e.target.value)}
              options={[
                { value: 'AVAILABLE', label: 'Sẵn sàng chạy chuyến' },
                ...(drStatus === 'ON_TRIP' ? [{ value: 'ON_TRIP', label: 'Đang chạy chuyến', disabled: true }] : []),
                { value: 'UNAVAILABLE', label: 'Không khả dụng / Nghỉ phép' },
              ]}
            />
          </div>
          {drModalType === 'ADD' && (driverUserLoadFailed || selectableDriverUsers.length === 0) && (
            <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
              {driverUserLoadFailed
                ? 'Không tải được danh sách tài khoản DRIVER để liên kết. Cần kiểm tra quyền/API quản lý tài khoản trước khi tạo hồ sơ tài xế.'
                : 'Tất cả tài khoản DRIVER hiện có đã có hồ sơ tài xế. Muốn thêm tài xế mới, hãy tạo tài khoản role DRIVER trước rồi quay lại tạo hồ sơ.'}
            </div>
          )}
          <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
            Trạng thái điều phối gồm: Sẵn sàng, Đang chạy chuyến, Không khả dụng. Tài xế không tự đổi trạng thái này; dispatcher/admin quản lý lịch rảnh, còn hệ thống chuyển sang Đang chạy chuyến theo luồng vận chuyển.
          </div>

          <Input
            label="Họ tên tài xế"
            value={drFullName}
            onChange={(e) => setDrFullName(e.target.value)}
            error={drFormErrors.full_name}
            placeholder="VD: Nguyễn Văn A"
            required
          />

          <Input
            label="Số điện thoại tài xế"
            value={drPhone}
            onChange={(e) => setDrPhone(e.target.value)}
            placeholder="VD: 0904 445 556"
          />

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Số giấy phép lái xe (GPLX)"
              value={drLicenseNumber}
              onChange={(e) => setDrLicenseNumber(e.target.value.toUpperCase())}
              error={drFormErrors.license_number}
              placeholder="VD: HP-12345678"
              required
            />
            <Input
              label="Hạn GPLX (Expiry)"
              type="date"
              value={drLicenseExpiry}
              onChange={(e) => setDrLicenseExpiry(e.target.value)}
              error={drFormErrors.license_expiry}
              required
            />
          </div>

          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4 mt-2">
            <Button variant="outline-light" onClick={() => setIsDrModalOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" variant="primary" loading={drSubmitting} disabled={drModalType === 'ADD' && selectableDriverUsers.length === 0}>
              {drModalType === 'ADD' ? 'Tạo mới' : 'Lưu thay đổi'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default FleetManagement;
