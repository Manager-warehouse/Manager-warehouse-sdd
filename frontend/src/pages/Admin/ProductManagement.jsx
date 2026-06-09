import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { masterDataService } from '../../services/masterData.service';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import { Plus, Search, Edit, ToggleLeft, ToggleRight, AlertCircle, Filter, Loader2 } from 'lucide-react';

const ProductManagement = () => {
  const { hasRole } = useAuthStore();
  const { addToast } = useUiStore();

  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL'); // ALL, ACTIVE, INACTIVE
  const [serialFilter, setSerialFilter] = useState('ALL'); // ALL, YES, NO

  // Modal states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalType, setModalType] = useState('ADD'); // ADD or EDIT
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Form states
  const [formSku, setFormSku] = useState('');
  const [formName, setFormName] = useState('');
  const [formUnit, setFormUnit] = useState('Cái');
  const [formUnitPerPack, setFormUnitPerPack] = useState('1');
  const [formWeight, setFormWeight] = useState('0');
  const [formVolume, setFormVolume] = useState('0');
  const [formHasSerial, setFormHasSerial] = useState(false);
  const [formHasExpiry, setFormHasExpiry] = useState(false);
  const [formShelfLifeDays, setFormShelfLifeDays] = useState('0');
  const [formReorderPoint, setFormReorderPoint] = useState('10');
  const [formDescription, setFormDescription] = useState('');
  const [formErrors, setFormErrors] = useState({});

  useEffect(() => {
    fetchProducts();
  }, []);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      const data = await masterDataService.getProducts();
      setProducts(data);
    } catch (e) {
      addToast('Lỗi tải danh mục sản phẩm', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenAddModal = () => {
    setModalType('ADD');
    setSelectedProduct(null);
    setFormSku('');
    setFormName('');
    setFormUnit('Cái');
    setFormUnitPerPack('1');
    setFormWeight('0');
    setFormVolume('0');
    setFormHasSerial(false);
    setFormHasExpiry(false);
    setFormShelfLifeDays('0');
    setFormReorderPoint('10');
    setFormDescription('');
    setFormErrors({});
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (product) => {
    setModalType('EDIT');
    setSelectedProduct(product);
    setFormSku(product.sku);
    setFormName(product.name);
    setFormUnit(product.unit);
    setFormUnitPerPack(String(product.unit_per_pack || 1));
    setFormWeight(String(product.weight_kg || 0));
    setFormVolume(String(product.volume_m3 || 0));
    setFormHasSerial(!!product.has_serial);
    setFormHasExpiry(!!product.has_expiry);
    setFormShelfLifeDays(String(product.shelf_life_days || 0));
    setFormReorderPoint(String(product.reorder_point || 0));
    setFormDescription(product.description || '');
    setFormErrors({});
    setIsModalOpen(true);
  };

  const validateForm = () => {
    const errors = {};
    if (!formSku.trim()) errors.sku = 'SKU không được để trống';
    if (!formName.trim()) errors.name = 'Tên sản phẩm không được để trống';
    if (Number(formUnitPerPack) <= 0) errors.unit_per_pack = 'Số lượng đóng gói phải lớn hơn 0';
    if (Number(formWeight) < 0) errors.weight = 'Trọng lượng không được âm';
    if (Number(formVolume) < 0) errors.volume = 'Thể tích không được âm';
    if (Number(formReorderPoint) < 0) errors.reorder_point = 'Định mức tồn kho không được âm';
    if (formHasExpiry && (!formShelfLifeDays.trim() || Number(formShelfLifeDays) <= 0)) {
      errors.shelf_life_days = 'Số ngày sử dụng phải lớn hơn 0';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validateForm()) return;

    setSubmitting(true);
    const productData = {
      sku: formSku,
      name: formName,
      unit: formUnit,
      unit_per_pack: Number(formUnitPerPack),
      weight_kg: parseFloat(formWeight),
      volume_m3: parseFloat(formVolume),
      has_serial: formHasSerial,
      has_expiry: formHasExpiry,
      shelf_life_days: formHasExpiry ? Number(formShelfLifeDays) : null,
      reorder_point: parseFloat(formReorderPoint),
      description: formDescription,
    };

    try {
      if (modalType === 'ADD') {
        await masterDataService.createProduct(productData);
        addToast(`Tạo mới sản phẩm SKU ${formSku.toUpperCase()} thành công`, 'success');
      } else {
        await masterDataService.updateProduct(selectedProduct.id, productData);
        addToast(`Cập nhật sản phẩm SKU ${formSku.toUpperCase()} thành công`, 'success');
      }
      setIsModalOpen(false);
      fetchProducts();
    } catch (err) {
      if (err.message === 'DUPLICATE_SKU') {
        setFormErrors({ sku: 'Mã SKU này đã tồn tại trên hệ thống' });
        addToast('Lỗi: SKU trùng lặp', 'error');
      } else {
        addToast(modalType === 'ADD' ? 'Lỗi tạo sản phẩm' : 'Lỗi cập nhật sản phẩm', 'error');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleStatus = async (id, currentStatus) => {
    // Only Admin or CEO can disable active products
    if (currentStatus && !hasRole(ROLES.ADMIN) && !hasRole(ROLES.CEO)) {
      addToast('Chỉ Quản trị viên hoặc CEO mới có quyền khóa/vô hiệu hóa sản phẩm', 'warning');
      return;
    }

    try {
      const updated = await masterDataService.toggleProductStatus(id, !currentStatus);
      addToast(`${updated.is_active ? 'Kích hoạt' : 'Khóa'} sản phẩm ${updated.sku} thành công`, 'success');
      fetchProducts();
    } catch (e) {
      addToast('Lỗi thay đổi trạng thái sản phẩm', 'error');
    }
  };

  const filteredProducts = products.filter((prod) => {
    const matchesSearch =
      prod.sku.toLowerCase().includes(searchTerm.toLowerCase()) ||
      prod.name.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'ACTIVE' && prod.is_active) ||
      (statusFilter === 'INACTIVE' && !prod.is_active);

    const matchesSerial =
      serialFilter === 'ALL' ||
      (serialFilter === 'YES' && prod.has_serial) ||
      (serialFilter === 'NO' && !prod.has_serial);

    return matchesSearch && matchesStatus && matchesSerial;
  });

  return (
    <div className="flex flex-col gap-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Hệ thống / Admin
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Danh mục SKU & Sản phẩm
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản lý tập trung thông tin SKU sản phẩm, định mức tồn kho, trọng lượng, thể tích và các thuộc tính quản lý (Serial/QC).
          </p>
        </div>
        {hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN) ? (
          <Button
            variant="primary"
            icon={Plus}
            onClick={handleOpenAddModal}
          >
            Thêm sản phẩm mới
          </Button>
        ) : null}
      </div>

      {/* Search & Filters */}
      <div className="bg-white border border-hairline-light rounded-lg p-5 mb-6 shadow-sm flex flex-col md:flex-row gap-4 items-center">
        {/* Search */}
        <div className="relative flex-1 w-full">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
          <input
            type="text"
            placeholder="Tìm theo mã SKU hoặc tên sản phẩm..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-canvas-light text-ink text-sm pl-10 pr-4 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink min-h-[44px]"
          />
        </div>

        {/* Filter controls */}
        <div className="flex gap-4 w-full md:w-auto">
          <div className="w-1/2 md:w-44">
            <label className="block text-[10px] font-bold text-shade-50 uppercase mb-1 tracking-wider">Trạng thái</label>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="w-full bg-canvas-light text-ink text-sm px-3 py-2 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink min-h-[38px]"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="INACTIVE">Đang khóa</option>
            </select>
          </div>

          <div className="w-1/2 md:w-44">
            <label className="block text-[10px] font-bold text-shade-50 uppercase mb-1 tracking-wider">Theo dõi Serial</label>
            <select
              value={serialFilter}
              onChange={(e) => setSerialFilter(e.target.value)}
              className="w-full bg-canvas-light text-ink text-sm px-3 py-2 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink min-h-[38px]"
            >
              <option value="ALL">Tất cả</option>
              <option value="YES">Bắt buộc Serial</option>
              <option value="NO">Không dùng Serial</option>
            </select>
          </div>
        </div>
      </div>

      {/* Main Table */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : filteredProducts.length === 0 ? (
        <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm card-premium">
          <AlertCircle className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy sản phẩm</h3>
          <p className="text-sm text-shade-50">Thử thay đổi bộ lọc tìm kiếm hoặc thêm mới sản phẩm.</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-zinc-50 border-b border-hairline-light">
                  <th className="px-6 py-4 font-bold text-shade-60">SKU</th>
                  <th className="px-6 py-4 font-bold text-shade-60">Tên sản phẩm</th>
                  <th className="px-6 py-4 font-bold text-shade-60">Đơn vị</th>
                  <th className="px-6 py-4 font-bold text-shade-60 text-right">Quy đổi đóng gói</th>
                  <th className="px-6 py-4 font-bold text-shade-60 text-right">Trọng lượng (kg)</th>
                  <th className="px-6 py-4 font-bold text-shade-60 text-right">Thể tích (m³)</th>
                  <th className="px-6 py-4 font-bold text-shade-60 text-center">Theo dõi Serial</th>
                  <th className="px-6 py-4 font-bold text-shade-60 text-right">Mức tối thiểu</th>
                  <th className="px-6 py-4 font-bold text-shade-60 text-center">Trạng thái</th>
                  <th className="px-6 py-4 font-bold text-shade-60 text-right">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {filteredProducts.map((prod) => (
                  <tr key={prod.id} className={`hover:bg-zinc-50/50 transition-colors ${!prod.is_active ? 'opacity-60 bg-zinc-50/20' : ''}`}>
                    <td className="px-6 py-4">
                      <span className="font-mono font-bold text-ink bg-zinc-100 px-2 py-1 rounded text-xs border border-zinc-200">
                        {prod.sku}
                      </span>
                    </td>
                    <td className="px-6 py-4 max-w-xs font-semibold text-ink truncate" title={prod.name}>
                      {prod.name}
                    </td>
                    <td className="px-6 py-4 text-shade-60">{prod.unit}</td>
                    <td className="px-6 py-4 text-right font-medium">{prod.unit_per_pack}</td>
                    <td className="px-6 py-4 text-right font-mono text-shade-60">{prod.weight_kg?.toFixed(3)}</td>
                    <td className="px-6 py-4 text-right font-mono text-shade-60">{prod.volume_m3?.toFixed(5)}</td>
                    <td className="px-6 py-4 text-center">
                      {prod.has_serial ? (
                        <span className="text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 px-2 py-0.5 rounded-pill">Có (Unique)</span>
                      ) : (
                        <span className="text-[10px] text-shade-40">Không</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-right font-mono font-medium">{prod.reorder_point}</td>
                    <td className="px-6 py-4 text-center">
                      <Badge type={prod.is_active ? 'success' : 'neutral'}>
                        {prod.is_active ? 'Hoạt động' : 'Đang khóa'}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-right flex gap-3 justify-end items-center">
                      {hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN) ? (
                        <button
                          onClick={() => handleOpenEditModal(prod)}
                          className="p-1 hover:bg-zinc-100 rounded-full transition-colors"
                          title="Sửa thông tin"
                        >
                          <Edit className="w-4 h-4 text-shade-60 hover:text-ink" />
                        </button>
                      ) : null}
                      <button
                        onClick={() => handleToggleStatus(prod.id, prod.is_active)}
                        className="p-1 hover:bg-zinc-100 rounded-full transition-colors"
                        title={prod.is_active ? 'Khóa sản phẩm' : 'Kích hoạt sản phẩm'}
                      >
                        {prod.is_active ? (
                          <ToggleRight className="w-5 h-5 text-emerald-600" />
                        ) : (
                          <ToggleLeft className="w-5 h-5 text-shade-40" />
                        )}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Product Form Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title={modalType === 'ADD' ? 'Tạo mới sản phẩm SKU' : 'Cập nhật thông tin SKU'}
        maxWidth="max-w-lg"
      >
        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Mã SKU (Duy nhất)"
              value={formSku}
              onChange={(e) => setFormSku(e.target.value.toUpperCase())}
              disabled={modalType === 'EDIT'}
              error={formErrors.sku}
              placeholder="Ví dụ: SKU-PA-001"
              required
            />
            <Input
              label="Đơn vị tính"
              type="select"
              value={formUnit}
              onChange={(e) => setFormUnit(e.target.value)}
              options={[
                { value: 'Cái', label: 'Cái (Unit)' },
                { value: 'Hộp', label: 'Hộp (Box)' },
                { value: 'Thùng', label: 'Thùng (Carton)' },
                { value: 'Cuộn', label: 'Cuộn (Roll)' },
                { value: 'Gói', label: 'Gói (Pack)' },
              ]}
            />
          </div>

          <Input
            label="Tên sản phẩm"
            value={formName}
            onChange={(e) => setFormName(e.target.value)}
            error={formErrors.name}
            placeholder="Nhập tên sản phẩm đầy đủ..."
            required
          />

          <div className="grid grid-cols-3 gap-4">
            <Input
              label="Quy đổi đóng gói"
              type="number"
              value={formUnitPerPack}
              onChange={(e) => setFormUnitPerPack(e.target.value)}
              error={formErrors.unit_per_pack}
              min="1"
              step="1"
              required
            />
            <Input
              label="Nặng (kg)"
              type="number"
              value={formWeight}
              onChange={(e) => setFormWeight(e.target.value)}
              error={formErrors.weight}
              min="0"
              step="0.001"
              required
            />
            <Input
              label="Thể tích (m³)"
              type="number"
              value={formVolume}
              onChange={(e) => setFormVolume(e.target.value)}
              error={formErrors.volume}
              min="0"
              step="0.00001"
              required
            />
          </div>

          <div className="flex flex-col gap-4 bg-zinc-50 p-3.5 rounded border border-hairline-light">
            <div className="grid grid-cols-2 gap-4 items-center">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formHasSerial}
                  onChange={(e) => setFormHasSerial(e.target.checked)}
                  className="w-4 h-4 rounded border-hairline-light text-ink focus:ring-ink"
                />
                <div className="flex flex-col">
                  <span className="text-xs font-bold text-ink">Bắt buộc nhập Serial</span>
                  <span className="text-[10px] text-shade-50 leading-none">Khi nhập/xuất kho</span>
                </div>
              </label>

              <Input
                label="Mức Stock cảnh báo"
                type="number"
                value={formReorderPoint}
                onChange={(e) => setFormReorderPoint(e.target.value)}
                error={formErrors.reorder_point}
                min="0"
                step="1"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-4 items-center border-t border-zinc-200 pt-3">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={formHasExpiry}
                  onChange={(e) => setFormHasExpiry(e.target.checked)}
                  className="w-4 h-4 rounded border-hairline-light text-ink focus:ring-ink"
                />
                <div className="flex flex-col">
                  <span className="text-xs font-bold text-ink">Theo dõi Hạn sử dụng</span>
                  <span className="text-[10px] text-shade-50 leading-none">Quản lý lô & hạn dùng</span>
                </div>
              </label>

              {formHasExpiry ? (
                <Input
                  label="Số ngày sử dụng (Shelf Life)"
                  type="number"
                  value={formShelfLifeDays}
                  onChange={(e) => setFormShelfLifeDays(e.target.value)}
                  error={formErrors.shelf_life_days}
                  min="1"
                  step="1"
                  required
                />
              ) : (
                <div className="text-xs text-shade-40 italic pt-4">Không theo dõi hạn dùng.</div>
              )}
            </div>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">Mô tả sản phẩm</label>
            <textarea
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
              placeholder="Thông số kỹ thuật, hướng dẫn bảo quản..."
              className="w-full bg-canvas-light text-ink text-sm px-3 py-2 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink min-h-[80px]"
            />
          </div>

          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4 mt-2">
            <Button variant="outline-light" onClick={() => setIsModalOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" variant="primary" loading={submitting}>
              {modalType === 'ADD' ? 'Tạo mới' : 'Lưu thay đổi'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default ProductManagement;
