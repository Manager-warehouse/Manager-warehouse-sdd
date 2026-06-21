import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  AlertTriangle,
  Clock,
  Eye,
  Loader2,
  PackageCheck,
  Plus,
  Search,
  Truck,
  X,
} from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import CreditCheckBanner from '../../components/warehouse/CreditCheckBanner';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import { ROLES } from '../../utils/constants';

const DO_STATUS_MAP = {
  NEW: { label: 'Mới', color: 'bg-zinc-100 text-zinc-800 border-zinc-200' },
  WAITING_PICKING: { label: 'Chờ lấy hàng/QC', color: 'bg-blue-50 text-blue-700 border-blue-200' },
  QC_PENDING_APPROVAL: { label: 'Chờ duyệt QC', color: 'bg-violet-50 text-violet-700 border-violet-200' },
  QC_COMPLETED: { label: 'QC xong', color: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  WAREHOUSE_APPROVED: { label: 'Chờ vận chuyển', color: 'bg-amber-50 text-amber-700 border-amber-200' },
  IN_TRANSIT: { label: 'Đang giao', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: 'Đã giao', color: 'bg-emerald-50 text-emerald-900 border-emerald-300' },
  RETURNED: { label: 'Hoàn trả', color: 'bg-orange-50 text-orange-700 border-orange-200' },
  REJECTED: { label: 'Bị từ chối', color: 'bg-rose-50 text-rose-700 border-rose-200' },
  CANCELLED: { label: 'Đã hủy', color: 'bg-red-50 text-red-700 border-red-200' },
};

const STATUS_OPTIONS = [
  { value: 'ALL', label: 'Tất cả' },
  { value: 'NEW', label: 'Mới' },
  { value: 'WAITING_PICKING', label: 'Chờ lấy hàng/QC' },
  { value: 'QC_PENDING_APPROVAL', label: 'Chờ duyệt QC' },
  { value: 'QC_COMPLETED', label: 'QC xong' },
  { value: 'WAREHOUSE_APPROVED', label: 'Chờ vận chuyển' },
  { value: 'IN_TRANSIT', label: 'Đang giao' },
  { value: 'COMPLETED', label: 'Đã giao' },
  { value: 'RETURNED', label: 'Hoàn trả' },
  { value: 'REJECTED', label: 'Bị từ chối' },
  { value: 'CANCELLED', label: 'Đã hủy' },
];

const emptyForm = { dealer_id: '', expected_delivery_date: '', notes: '', items: [] };

const getStatusBadge = (status) => {
  const base = 'text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase tracking-wider whitespace-nowrap';
  const { label, color } = DO_STATUS_MAP[status] ?? {
    label: status,
    color: 'bg-zinc-100 text-zinc-800 border-zinc-200',
  };

  return <span className={`${base} ${color}`}>{label}</span>;
};

const getRoleHint = (order, hasRole) => {
  if (hasRole(ROLES.STOREKEEPER) && order.status === 'NEW') {
    return 'Thủ kho lập kế hoạch lấy hàng';
  }
  if (hasRole(ROLES.WAREHOUSE_STAFF) && order.status === 'WAITING_PICKING') {
    return 'Nhân viên kho nhập kết quả lấy hàng/QC';
  }
  if (hasRole(ROLES.STOREKEEPER) && order.status === 'QC_PENDING_APPROVAL') {
    return 'Thủ kho duyệt kết quả QC';
  }
  if (hasRole(ROLES.WAREHOUSE_MANAGER) && order.status === 'QC_COMPLETED') {
    return 'Quản lý kho phê duyệt xuất kho';
  }
  return 'Theo dõi chi tiết đơn xuất';
};

export default function DeliveryOrders() {
  const navigate = useNavigate();
  const { addToast } = useUiStore();
  const { hasRole, activeWarehouse } = useAuthStore();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [dealers, setDealers] = useState([]);
  const [products, setProducts] = useState([]);
  const [masterDataLoading, setMasterDataLoading] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [formData, setFormData] = useState(emptyForm);
  const [selectedDealerObj, setSelectedDealerObj] = useState(null);
  const [dealerSearch, setDealerSearch] = useState('');
  const [productSearch, setProductSearch] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [cancelModal, setCancelModal] = useState({ show: false, orderId: null, reason: '' });

  useEffect(() => {
    fetchOrders();
  }, [activeWarehouse?.id, statusFilter, search]);

  useEffect(() => {
    if (!showCreateModal) {
      return;
    }

    fetchMasterData(productSearch);
  }, [showCreateModal, productSearch]);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getDeliveryOrders(activeWarehouse?.id, {
        status: statusFilter,
        search,
      });
      setOrders(data);
    } catch (error) {
      addToast(error.message || 'Lỗi khi tải danh sách đơn xuất hàng', 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchMasterData = async (searchTerm = '') => {
    setMasterDataLoading(true);
    try {
      const [dealersData, productsData] = await Promise.all([
        masterDataService.getDealers(),
        masterDataService.getProducts({ search: searchTerm, size: 100 }),
      ]);
      setDealers(dealersData.filter((dealer) => dealer.is_active !== false));
      setProducts(productsData.filter((product) => product.is_active !== false));
    } catch {
      addToast('Không thể tải dữ liệu đại lý/sản phẩm', 'warning');
    } finally {
      setMasterDataLoading(false);
    }
  };

  const handleOpenCreateModal = () => {
    setFormData(emptyForm);
    setSelectedDealerObj(null);
    setDealerSearch('');
    setProductSearch('');
    setShowCreateModal(true);
  };

  const handleCloseCreateModal = () => {
    setShowCreateModal(false);
    setFormData(emptyForm);
    setSelectedDealerObj(null);
    setDealerSearch('');
    setProductSearch('');
  };

  const addItemRow = () => {
    setFormData((prev) => ({
      ...prev,
      items: [...prev.items, { product_id: '', requested_qty: 1, unit_price: 0 }],
    }));
  };

  const updateItemRow = (index, field, value) => {
    const items = [...formData.items];
    items[index][field] = value;

    if (field === 'product_id') {
      const product = products.find((item) => Number(item.id) === Number(value));
      if (product) {
        items[index].product_name = product.name;
        items[index].sku = product.sku;
        items[index].unit_price = Number(product.selling_price || product.unit_price || items[index].unit_price || 0);
      }
    }

    setFormData((prev) => ({ ...prev, items }));
  };

  const removeItemRow = (index) => {
    const items = [...formData.items];
    items.splice(index, 1);
    setFormData((prev) => ({ ...prev, items }));
  };

  const handleCreateSubmit = async () => {
    if (!activeWarehouse?.id) {
      addToast('Vui lòng chọn kho trước khi lập đơn xuất', 'error');
      return;
    }
    if (!formData.items.length) {
      addToast('Vui lòng thêm ít nhất 1 sản phẩm', 'error');
      return;
    }
    if (formData.items.some((item) => !item.product_id || Number(item.requested_qty) <= 0)) {
      addToast('Sản phẩm và số lượng phải hợp lệ', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await outboundService.createDeliveryOrder({
        ...formData,
        dealer_name: selectedDealerObj?.name || selectedDealerObj?.company_name,
        warehouse_id: activeWarehouse.id,
      });
      addToast('Tạo đơn xuất hàng thành công', 'success');
      handleCloseCreateModal();
      fetchOrders();
    } catch (error) {
      addToast(error.message || 'Lỗi khi tạo đơn xuất hàng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancelDO = async () => {
    if (!cancelModal.reason.trim()) {
      addToast('Vui lòng nhập lý do hủy', 'error');
      return;
    }
    try {
      await outboundService.cancelDeliveryOrder(cancelModal.orderId, cancelModal.reason.trim());
      addToast('Hủy đơn thành công', 'success');
      setCancelModal({ show: false, orderId: null, reason: '' });
      fetchOrders();
    } catch (error) {
      addToast(error.message || 'Lỗi khi hủy đơn', 'error');
    }
  };

  const totalDO = orders.length;
  const waitingPickingDO = orders.filter((order) => order.status === 'WAITING_PICKING').length;
  const qcPendingDO = orders.filter((order) => order.status === 'QC_PENDING_APPROVAL').length;
  const approvedDO = orders.filter((order) => order.status === 'WAREHOUSE_APPROVED').length;
  const creditStatus = selectedDealerObj?.id === 4 ? 'BLOCKED' : selectedDealerObj?.id === 2 ? 'WARNING' : selectedDealerObj ? 'OK' : null;
  const isSubmitDisabled = !formData.dealer_id || !formData.expected_delivery_date || !formData.items.length || creditStatus === 'BLOCKED' || submitting;
  const filteredDealers = dealers.filter((dealer) => {
    const keyword = dealerSearch.trim().toLowerCase();
    if (!keyword) {
      return true;
    }

    return `${dealer.code || ''} ${dealer.name || dealer.company_name || ''}`.toLowerCase().includes(keyword);
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-center">
        <div>
          <span className="mb-1 block text-[10px] font-bold uppercase tracking-widest text-shade-60">Vận hành / Xuất kho</span>
          <h1 className="text-2xl font-display font-semibold tracking-tight md:text-3xl">Đơn xuất hàng</h1>
          <p className="mt-1 text-xs font-light text-shade-50">
            Quản lý lệnh xuất tại kho <span className="font-semibold text-ink">{activeWarehouse?.name} ({activeWarehouse?.code})</span>.
          </p>
        </div>
        {hasRole(ROLES.PLANNER) && (
          <button onClick={handleOpenCreateModal} className="btn-pill btn-pill-primary flex items-center gap-2">
            <Plus className="h-4 w-4" />
            <span>Lập đơn xuất mới</span>
          </button>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
        {[
          { label: 'Tổng đơn', value: totalDO, icon: <PackageCheck className="h-5 w-5" />, accent: 'text-zinc-600 bg-zinc-100' },
          { label: 'Chờ lấy hàng/QC', value: waitingPickingDO, icon: <Clock className="h-5 w-5" />, accent: 'text-blue-600 bg-blue-50' },
          { label: 'Chờ duyệt QC', value: qcPendingDO, icon: <PackageCheck className="h-5 w-5" />, accent: 'text-violet-600 bg-violet-50' },
          { label: 'Chờ vận chuyển', value: approvedDO, icon: <Truck className="h-5 w-5" />, accent: 'text-amber-600 bg-amber-50' },
        ].map(({ label, value, icon, accent }) => (
          <div key={label} className="flex items-center gap-3 rounded-lg border border-hairline-light bg-white p-4 shadow-sm">
            <div className={`rounded-full p-2.5 ${accent}`}>{icon}</div>
            <div>
              <p className="text-xs font-medium text-shade-50">{label}</p>
              <p className="text-2xl font-bold text-ink">{value}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="flex flex-col items-center justify-between gap-4 rounded-lg border border-hairline-light bg-white p-4 shadow-sm md:flex-row">
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-shade-40" />
          <input
            type="text"
            placeholder="Tìm mã DO, tên đại lý..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            className="w-full text-input pl-10"
          />
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-shade-50">Trạng thái:</span>
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} className="text-input py-1.5 text-xs">
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>{option.label}</option>
            ))}
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="h-8 w-8 animate-spin text-shade-50" />
        </div>
      ) : orders.length === 0 ? (
        <div className="rounded-lg border border-hairline-light bg-white p-12 text-center shadow-sm">
          <PackageCheck className="mx-auto mb-4 h-12 w-12 text-shade-30" />
          <h3 className="mb-1 text-lg font-bold">Không tìm thấy đơn xuất hàng nào</h3>
          <p className="text-sm text-shade-50">Thử đổi bộ lọc hoặc tạo một đơn mới để bắt đầu.</p>
        </div>
      ) : (
        <div className="card-premium overflow-hidden rounded-lg border border-hairline-light bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left">
              <thead>
                <tr className="border-b border-hairline-light bg-zinc-50">
                  <th className="px-6 py-3.5 text-xs font-bold uppercase tracking-wider text-shade-60">Mã DO</th>
                  <th className="px-6 py-3.5 text-xs font-bold uppercase tracking-wider text-shade-60">Đại lý</th>
                  <th className="px-6 py-3.5 text-xs font-bold uppercase tracking-wider text-shade-60">Ngày lập</th>
                  <th className="px-6 py-3.5 text-xs font-bold uppercase tracking-wider text-shade-60">Ngày giao dự kiến</th>
                  <th className="px-6 py-3.5 text-xs font-bold uppercase tracking-wider text-shade-60">Trạng thái</th>
                  <th className="px-6 py-3.5 text-right text-xs font-bold uppercase tracking-wider text-shade-60">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {orders.map((order) => {
                  const canCancel = hasRole(ROLES.WAREHOUSE_MANAGER)
                    && ['NEW', 'WAITING_PICKING', 'QC_PENDING_APPROVAL', 'QC_COMPLETED'].includes(order.status);
                  const canOpenPicking = hasRole(ROLES.STOREKEEPER)
                    && ['NEW', 'WAITING_PICKING', 'QC_PENDING_APPROVAL', 'QC_COMPLETED'].includes(order.status);
                  const canOpenQcEntry = hasRole(ROLES.WAREHOUSE_STAFF)
                    && order.status === 'WAITING_PICKING';

                  return (
                    <tr key={order.id} className="hover:bg-zinc-50 transition-colors">
                      <td className="px-6 py-4 text-xs font-bold">{order.do_number}</td>
                      <td className="px-6 py-4">
                        <p className="text-xs font-semibold">{order.dealer_name}</p>
                        <p className="mt-1 text-[11px] text-shade-40">{getRoleHint(order, hasRole)}</p>
                      </td>
                      <td className="px-6 py-4 text-xs text-shade-50">{order.document_date ? new Date(order.document_date).toLocaleDateString('vi-VN') : '-'}</td>
                      <td className="px-6 py-4 text-xs text-shade-50">{order.expected_delivery_date ? new Date(order.expected_delivery_date).toLocaleDateString('vi-VN') : '-'}</td>
                      <td className="px-6 py-4">{getStatusBadge(order.status)}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          {canCancel && (
                            <button
                              onClick={() => setCancelModal({ show: true, orderId: order.id, reason: '' })}
                              className="inline-flex items-center justify-center rounded-full border border-red-300 px-3 py-1 text-xs font-semibold text-red-600 transition-colors hover:bg-red-50"
                            >
                              Hủy đơn
                            </button>
                          )}
                          {canOpenPicking && (
                            <button
                              onClick={() => navigate(`/outbound/delivery-orders/${order.id}`)}
                              className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light px-3 py-1 text-xs font-semibold text-ink transition-colors hover:bg-zinc-100"
                            >
                              {order.status === 'NEW' ? 'Lập kế hoạch lấy hàng' : 'Duyệt xử lý kho'}
                            </button>
                          )}
                          {canOpenQcEntry && (
                            <button
                              onClick={() => navigate(`/outbound/delivery-orders/${order.id}`)}
                              className="inline-flex items-center justify-center rounded-full border border-blue-300 bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700 transition-colors hover:bg-blue-100"
                            >
                              Nhập kết quả lấy hàng/QC
                            </button>
                          )}
                          <button
                            onClick={() => navigate(`/outbound/delivery-orders/${order.id}`)}
                            className="flex items-center justify-center rounded-full p-1.5 text-shade-50 transition-colors hover:bg-zinc-200 hover:text-ink"
                            title="Xem chi tiết"
                          >
                            <Eye className="h-4 w-4" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal isOpen={showCreateModal} onClose={handleCloseCreateModal} title="Lập đơn xuất hàng" maxWidth="max-w-4xl">
        <div className="flex flex-col gap-5">
          <CreditCheckBanner status={creditStatus} remainingCredit={selectedDealerObj?.id === 2 ? 15000000 : 250000000} />

          <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
            <div className="flex flex-col gap-3">
              <Input
                label="Tìm đại lý"
                value={dealerSearch}
                onChange={(event) => setDealerSearch(event.target.value)}
                placeholder="Nhập mã hoặc tên đại lý"
              />
              <Input
                label="Đại lý nhận hàng *"
                type="select"
                disabled={masterDataLoading}
                value={formData.dealer_id}
                onChange={(event) => {
                  setFormData((prev) => ({ ...prev, dealer_id: event.target.value }));
                  setSelectedDealerObj(dealers.find((dealer) => Number(dealer.id) === Number(event.target.value)));
                }}
                options={[
                  { value: '', label: masterDataLoading ? '-- Đang tải đại lý --' : '-- Chọn đại lý --' },
                  ...filteredDealers.map((dealer) => ({
                    value: dealer.id,
                    label: `${dealer.code ? `[${dealer.code}] ` : ''}${dealer.name || dealer.company_name}`,
                  })),
                ]}
              />
            </div>

            <Input
              label="Ngày giao dự kiến *"
              type="date"
              value={formData.expected_delivery_date}
              onChange={(event) => setFormData((prev) => ({ ...prev, expected_delivery_date: event.target.value }))}
            />

            <div className="md:col-span-2">
              <Input
                label="Ghi chú"
                value={formData.notes}
                onChange={(event) => setFormData((prev) => ({ ...prev, notes: event.target.value }))}
                placeholder="Ghi chú về đơn hàng..."
              />
            </div>
          </div>

          <div>
            <div className="mb-3 flex items-center justify-between">
              <span className="text-xs font-semibold uppercase tracking-wider text-shade-60">Danh sách sản phẩm</span>
              <button type="button" onClick={addItemRow} className="flex items-center gap-1 text-xs font-semibold text-ink hover:underline">
                <Plus className="h-3.5 w-3.5" /> Thêm sản phẩm
              </button>
            </div>

            <div className="mb-3 grid grid-cols-1 gap-3 md:grid-cols-[minmax(0,1fr)_auto] md:items-end">
              <Input
                label="Tìm sản phẩm từ API"
                value={productSearch}
                onChange={(event) => setProductSearch(event.target.value)}
                placeholder="Nhập SKU hoặc tên sản phẩm"
              />
              <div className="flex items-center gap-2 text-xs text-shade-50">
                {masterDataLoading && <Loader2 className="h-4 w-4 animate-spin" />}
                <span>{masterDataLoading ? 'Đang tải danh mục sản phẩm...' : `Đang hiển thị ${products.length} sản phẩm`}</span>
              </div>
            </div>

            <div className="overflow-hidden rounded-lg border border-hairline-light bg-canvas-light">
              <table className="w-full border-collapse text-left text-xs">
                <thead>
                  <tr className="border-b border-hairline-light bg-zinc-50">
                    <th className="px-4 py-3 font-bold uppercase tracking-wider text-shade-60">Sản phẩm</th>
                    <th className="w-28 px-4 py-3 font-bold uppercase tracking-wider text-shade-60">Số lượng</th>
                    <th className="w-36 px-4 py-3 font-bold uppercase tracking-wider text-shade-60">Đơn giá</th>
                    <th className="w-10 px-4 py-3" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {!formData.items.length && (
                    <tr>
                      <td colSpan="4" className="px-4 py-8 text-center text-xs italic text-shade-40">
                        Chưa có sản phẩm nào. Nhấn Thêm sản phẩm để bắt đầu.
                      </td>
                    </tr>
                  )}

                  {formData.items.map((item, index) => (
                    <tr key={`${item.product_id || 'new'}-${index}`} className="hover:bg-zinc-50/50">
                      <td className="px-4 py-2.5">
                        <select
                          disabled={masterDataLoading}
                          className="w-full rounded-md border border-hairline-light bg-canvas-light px-3 py-2.5 text-sm text-ink transition-all focus:border-ink focus:outline-none focus:ring-1 focus:ring-ink"
                          value={item.product_id}
                          onChange={(event) => updateItemRow(index, 'product_id', event.target.value)}
                        >
                          <option value="">{masterDataLoading ? '-- Đang tải sản phẩm --' : '-- Chọn sản phẩm --'}</option>
                          {products.map((product) => (
                            <option key={product.id} value={product.id}>[{product.sku}] {product.name}</option>
                          ))}
                        </select>
                      </td>
                      <td className="px-4 py-2.5">
                        <input
                          type="number"
                          min="1"
                          className="w-full rounded-md border border-hairline-light bg-canvas-light px-3 py-2.5 text-sm text-ink transition-all focus:border-ink focus:outline-none focus:ring-1 focus:ring-ink"
                          value={item.requested_qty}
                          onChange={(event) => updateItemRow(index, 'requested_qty', Number(event.target.value))}
                        />
                      </td>
                      <td className="px-4 py-2.5">
                        <input
                          type="number"
                          min="0"
                          className="w-full rounded-md border border-hairline-light bg-canvas-light px-3 py-2.5 text-sm text-ink transition-all focus:border-ink focus:outline-none focus:ring-1 focus:ring-ink"
                          value={item.unit_price}
                          onChange={(event) => updateItemRow(index, 'unit_price', Number(event.target.value))}
                        />
                      </td>
                      <td className="px-4 py-2.5 text-center">
                        <button type="button" onClick={() => removeItemRow(index)} className="rounded-full p-1 text-shade-40 transition-colors hover:bg-zinc-100 hover:text-red-600">
                          <X className="h-4 w-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
            <Button variant="outline-light" onClick={handleCloseCreateModal}>Đóng</Button>
            <Button variant="primary" loading={submitting} disabled={isSubmitDisabled} onClick={handleCreateSubmit}>Tạo đơn xuất</Button>
          </div>
        </div>
      </Modal>

      <Modal isOpen={cancelModal.show} onClose={() => setCancelModal({ show: false, orderId: null, reason: '' })} title="Hủy lệnh xuất hàng">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2 text-sm font-semibold text-red-600">
            <AlertTriangle className="h-4 w-4 shrink-0" /> Hành động này không thể hoàn tác.
          </div>
          <Input
            label="Lý do hủy *"
            value={cancelModal.reason}
            onChange={(event) => setCancelModal((prev) => ({ ...prev, reason: event.target.value }))}
            placeholder="Nhập lý do hủy đơn này..."
          />
          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
            <Button variant="outline-light" onClick={() => setCancelModal({ show: false, orderId: null, reason: '' })}>Đóng</Button>
            <Button onClick={handleCancelDO} disabled={!cancelModal.reason.trim()} className="bg-red-600 text-white hover:bg-red-700 focus:ring-red-500">
              Xác nhận hủy
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
