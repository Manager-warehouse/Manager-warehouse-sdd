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
import pricingService from '../../services/pricing.service';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { useDebounce } from '../../hooks/useDebounce';
import CreditCheckBanner from '../../components/warehouse/CreditCheckBanner';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import { ROLES } from '../../utils/constants';

const DO_STATUS_MAP = {
  NEW: { label: 'Mới', color: 'bg-canvas-cream text-shade-70 border-hairline-light' },
  WAITING_PICKING: { label: 'Chờ lấy hàng/QC', color: 'bg-info-50 text-info-700 border-info-200' },
  QC_PENDING_APPROVAL: { label: 'Chờ duyệt QC', color: 'bg-violet-50 text-violet-700 border-violet-200' },
  QC_COMPLETED: { label: 'QC xong', color: 'bg-success-50 text-success-700 border-success-200' },
  WAREHOUSE_APPROVED: { label: 'Chờ vận chuyển', color: 'bg-warning-50 text-warning-700 border-warning-200' },
  IN_TRANSIT: { label: 'Đang giao', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: 'Đã giao', color: 'bg-success-50 text-success-900 border-success-300' },
  RETURNED: { label: 'Hoàn trả', color: 'bg-orange-50 text-orange-700 border-orange-200' },
  REJECTED: { label: 'Bị từ chối', color: 'bg-rose-50 text-rose-700 border-rose-200' },
  CANCELLED: { label: 'Đã hủy', color: 'bg-danger-50 text-danger-700 border-danger-200' },
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

const createEmptyItemRow = () => ({ product_id: '', requested_qty: 1, unit_price: 0, price_status: 'idle' });

const createEmptyForm = () => ({
  dealer_id: '',
  expected_delivery_date: '',
  notes: '',
  items: [createEmptyItemRow()],
});

const formatVND = (value) => Number(value || 0).toLocaleString('vi-VN');

const toMoney = (value) => {
  const amount = Number(value || 0);
  return Number.isFinite(amount) ? amount : 0;
};

const calculateOrderValue = (items) => items.reduce((total, item) => (
  total + (toMoney(item.requested_qty) * toMoney(item.unit_price))
), 0);

const getCreditCheck = (dealer, orderValue) => {
  if (!dealer) {
    return { status: null, remainingCredit: 0 };
  }

  const creditLimit = toMoney(dealer.credit_limit);
  const currentBalance = toMoney(dealer.current_balance);
  const remainingCredit = creditLimit - currentBalance - orderValue;

  if (dealer.credit_status === 'CREDIT_HOLD' || remainingCredit < 0) {
    return { status: 'BLOCKED', remainingCredit };
  }

  if (creditLimit > 0 && remainingCredit <= creditLimit * 0.1) {
    return { status: 'WARNING', remainingCredit };
  }

  return { status: 'OK', remainingCredit };
};

const getStatusBadge = (status) => {
  const { label, color } = DO_STATUS_MAP[status] ?? {
    label: status,
    color: 'bg-canvas-cream text-shade-70 border-hairline-light',
  };

  return <Badge size="sm" colorClassName={color}>{label}</Badge>;
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
  const [formData, setFormData] = useState(createEmptyForm);
  const [selectedDealerObj, setSelectedDealerObj] = useState(null);
  const [dealerSearch, setDealerSearch] = useState('');
  const [productSearch, setProductSearch] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [cancelModal, setCancelModal] = useState({ show: false, orderId: null, reason: '' });

  const debouncedSearch = useDebounce(search);
  const debouncedProductSearch = useDebounce(productSearch);

  useEffect(() => {
    fetchOrders();
  }, [activeWarehouse?.id, statusFilter, debouncedSearch]);

  useEffect(() => {
    if (!showCreateModal) {
      return;
    }

    fetchMasterData();
  }, [showCreateModal]);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getDeliveryOrders(activeWarehouse?.id, {
        status: statusFilter,
        search: debouncedSearch,
      });
      setOrders(data);
    } catch (error) {
      addToast(error.message || 'Lỗi khi tải danh sách đơn xuất hàng', 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchMasterData = async () => {
    setMasterDataLoading(true);
    try {
      const [dealersData, productsData] = await Promise.all([
        masterDataService.getDealers(),
        masterDataService.getProducts({ size: 200 }),
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
    setFormData(createEmptyForm());
    setSelectedDealerObj(null);
    setDealerSearch('');
    setProductSearch('');
    setShowCreateModal(true);
  };

  const handleCloseCreateModal = () => {
    setShowCreateModal(false);
    setFormData(createEmptyForm());
    setSelectedDealerObj(null);
    setDealerSearch('');
    setProductSearch('');
  };

  const addItemRow = () => {
    setFormData((prev) => ({
      ...prev,
      items: [...prev.items, createEmptyItemRow()],
    }));
  };

  const lookupItemPrice = async (index, productId) => {
    if (!activeWarehouse?.id || !productId) {
      return;
    }

    const documentDate = formData.document_date || new Date().toISOString().slice(0, 10);
    try {
      const price = await pricingService.lookupApproved({
        product_id: productId,
        warehouse_id: activeWarehouse.id,
        date: documentDate,
      });
      setFormData((prev) => {
        const items = [...prev.items];
        if (Number(items[index]?.product_id) !== Number(productId)) {
          return prev;
        }
        items[index] = {
          ...items[index],
          unit_price: Number(price.selling_price ?? price.sellingPrice ?? 0),
          price_status: 'ready',
        };
        return { ...prev, items };
      });
    } catch (error) {
      setFormData((prev) => {
        const items = [...prev.items];
        if (Number(items[index]?.product_id) !== Number(productId)) {
          return prev;
        }
        items[index] = { ...items[index], unit_price: 0, price_status: 'missing' };
        return { ...prev, items };
      });
      if (error?.response?.status !== 404) {
        addToast(error.message || 'Không thể tra báo giá sản phẩm', 'warning');
      }
    }
  };

  const updateItemRow = (index, field, value) => {
    const items = [...formData.items];
    items[index][field] = value;

    if (field === 'product_id') {
      const product = products.find((item) => Number(item.id) === Number(value));
      if (product) {
        items[index].product_name = product.name;
        items[index].sku = product.sku;
        items[index].unit_price = 0;
        items[index].price_status = 'loading';
        lookupItemPrice(index, value);
      } else {
        items[index].unit_price = 0;
        items[index].price_status = 'idle';
      }
    }

    setFormData((prev) => ({ ...prev, items }));
  };

  const removeItemRow = (index) => {
    const items = [...formData.items];
    items.splice(index, 1);
    setFormData((prev) => ({ ...prev, items: items.length ? items : [createEmptyItemRow()] }));
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
  const orderValue = calculateOrderValue(formData.items);
  const creditCheck = getCreditCheck(selectedDealerObj, orderValue);
  const creditStatus = creditCheck.status;
  const hasInvalidPrice = formData.items.some((item) => item.product_id && (item.price_status !== 'ready' || Number(item.unit_price) <= 0));
  const isSubmitDisabled = !formData.dealer_id || !formData.expected_delivery_date || !formData.items.length || hasInvalidPrice || creditStatus === 'BLOCKED' || submitting;
  const filteredDealers = dealers.filter((dealer) => {
    const keyword = dealerSearch.trim().toLowerCase();
    if (!keyword) {
      return true;
    }

    return `${dealer.code || ''} ${dealer.name || dealer.company_name || ''}`.toLowerCase().includes(keyword);
  });
  const filteredProducts = products.filter((product) => {
    const keyword = productSearch.trim().toLowerCase();
    if (!keyword) {
      return true;
    }

    return `${product.sku || ''} ${product.name || ''}`.toLowerCase().includes(keyword);
  });

  const productOptionsFor = (selectedProductId) => {
    if (!selectedProductId) {
      return filteredProducts;
    }

    const selectedProduct = products.find((product) => Number(product.id) === Number(selectedProductId));
    if (!selectedProduct || filteredProducts.some((product) => Number(product.id) === Number(selectedProductId))) {
      return filteredProducts;
    }

    return [selectedProduct, ...filteredProducts];
  };

  const renderDoActions = (order) => {
    const canCancel = hasRole(ROLES.WAREHOUSE_MANAGER)
      && ['NEW', 'WAITING_PICKING', 'QC_PENDING_APPROVAL', 'QC_COMPLETED'].includes(order.status);
    const canOpenPicking = hasRole(ROLES.STOREKEEPER)
      && ['NEW', 'WAITING_PICKING', 'QC_PENDING_APPROVAL', 'QC_COMPLETED'].includes(order.status);
    const canOpenQcEntry = hasRole(ROLES.WAREHOUSE_STAFF)
      && order.status === 'WAITING_PICKING';

    return (
      <>
        {canCancel && (
          <button
            onClick={() => setCancelModal({ show: true, orderId: order.id, reason: '' })}
            className="inline-flex items-center gap-1.5 text-xs font-semibold px-3 py-1 rounded-pill border border-danger-200 text-danger-600 hover:bg-danger-50 transition-colors"
          >
            Hủy đơn
          </button>
        )}
        {canOpenPicking && (
          <button
            onClick={() => navigate(`/outbound/delivery-orders/${order.id}`)}
            className="inline-flex items-center justify-center rounded-pill border border-ink bg-canvas-light px-3 py-1 text-xs font-semibold text-ink transition-colors hover:bg-canvas-cream"
          >
            {order.status === 'NEW' ? 'Lập kế hoạch lấy hàng' : 'Duyệt xử lý kho'}
          </button>
        )}
        {canOpenQcEntry && (
          <button
            onClick={() => navigate(`/outbound/delivery-orders/${order.id}`)}
            className="inline-flex items-center justify-center rounded-pill border border-info-300 bg-info-50 px-3 py-1 text-xs font-semibold text-info-700 transition-colors hover:bg-info-100"
          >
            Nhập kết quả lấy hàng/QC
          </button>
        )}
        <button
          onClick={() => navigate(`/outbound/delivery-orders/${order.id}`)}
          className="flex items-center justify-center rounded-pill p-1.5 text-shade-50 transition-colors hover:bg-canvas-cream hover:text-ink"
          title="Xem chi tiết"
        >
          <Eye className="h-4 w-4" />
        </button>
      </>
    );
  };

  return (
    <div className="mobile-page">
      <div className="flex flex-col items-start justify-between gap-4 md:flex-row md:items-center">
        <div>
          <span className="mb-1 block text-[10px] font-bold uppercase tracking-widest text-shade-60">Vận hành / Xuất kho</span>
          <h1 className="text-2xl font-display font-semibold tracking-tight md:text-3xl">Đơn xuất hàng</h1>
          <p className="mt-1 text-xs font-light text-shade-50">
            Quản lý lệnh xuất tại kho <span className="font-semibold text-ink">{activeWarehouse?.name} ({activeWarehouse?.code})</span>.
          </p>
        </div>
        {hasRole(ROLES.PLANNER) && (
          <Button onClick={handleOpenCreateModal} variant="primary" icon={Plus}>
            Lập đơn xuất mới
          </Button>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3 md:gap-4 md:grid-cols-4">
        {[
          { label: 'Tổng đơn', value: totalDO, icon: <PackageCheck className="h-5 w-5" />, accent: 'text-shade-60 bg-canvas-cream' },
          { label: 'Chờ lấy hàng/QC', value: waitingPickingDO, icon: <Clock className="h-5 w-5" />, accent: 'text-info-600 bg-info-50' },
          { label: 'Chờ duyệt QC', value: qcPendingDO, icon: <PackageCheck className="h-5 w-5" />, accent: 'text-violet-600 bg-violet-50' },
          { label: 'Chờ vận chuyển', value: approvedDO, icon: <Truck className="h-5 w-5" />, accent: 'text-warning-600 bg-warning-50' },
        ].map(({ label, value, icon, accent }) => (
          <div key={label} className="flex items-center gap-2.5 md:gap-3 rounded-lg border border-hairline-light bg-canvas-light p-3 md:p-4 shadow-level-3">
            <div className={`rounded-full p-2.5 ${accent}`}>{icon}</div>
            <div>
              <p className="text-xs font-medium text-shade-50">{label}</p>
              <p className="text-xl md:text-2xl font-bold text-ink">{value}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="mobile-filter-bar rounded-lg border border-hairline-light bg-canvas-light p-3 md:p-4 shadow-level-3 md:flex md:flex-row md:items-center md:justify-between md:gap-4">
        <div className="w-full md:w-80">
          <Input
            type="text"
            leftIcon={Search}
            placeholder="Tìm mã DO, tên đại lý..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <Input
          type="select"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          options={STATUS_OPTIONS}
        />
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="h-8 w-8 animate-spin text-shade-50" />
        </div>
      ) : orders.length === 0 ? (
        <div className="rounded-lg border border-hairline-light bg-canvas-light p-12 text-center shadow-level-3">
          <PackageCheck className="mx-auto mb-4 h-12 w-12 text-shade-30" />
          <h3 className="mb-1 text-lg font-bold">Không tìm thấy đơn xuất hàng nào</h3>
          <p className="text-sm text-shade-50">Thử đổi bộ lọc hoặc tạo một đơn mới để bắt đầu.</p>
        </div>
      ) : (
        <>
          {/* Desktop/tablet: table view */}
          <div className="hidden md:block overflow-hidden rounded-lg border border-hairline-light bg-canvas-light shadow-level-3">
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-left">
                <thead>
                  <tr className="border-b border-hairline-light bg-canvas-cream">
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã DO</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Đại lý</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Ngày lập</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Ngày giao dự kiến</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Trạng thái</th>
                    <th className="px-6 py-4 text-right text-xs font-semibold uppercase tracking-wider text-shade-60">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {orders.map((order) => (
                    <tr key={order.id} className="hover:bg-canvas-cream/50 transition-colors">
                      <td className="px-6 py-4 text-xs font-bold">{order.do_number}</td>
                      <td className="px-6 py-4">
                        <p className="text-xs font-semibold">{order.dealer_name}</p>
                        <p className="mt-1 text-[11px] text-shade-50">{getRoleHint(order, hasRole)}</p>
                      </td>
                      <td className="px-6 py-4 text-xs text-shade-50">{order.document_date ? new Date(order.document_date).toLocaleDateString('vi-VN') : '-'}</td>
                      <td className="px-6 py-4 text-xs text-shade-50">{order.expected_delivery_date ? new Date(order.expected_delivery_date).toLocaleDateString('vi-VN') : '-'}</td>
                      <td className="px-6 py-4">{getStatusBadge(order.status)}</td>
                      <td className="whitespace-nowrap px-6 py-4 text-right">
                        <div className="flex items-center justify-end gap-2">
                          {renderDoActions(order)}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Mobile: stacked card view */}
          <div className="flex flex-col gap-3 md:hidden">
            {orders.map((order) => (
              <div key={order.id} className="rounded-lg border border-hairline-light bg-canvas-light shadow-level-3 overflow-hidden">
                <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                  <span className="text-xs font-bold text-ink">{order.do_number}</span>
                  {getStatusBadge(order.status)}
                </div>
                <div className="p-4 flex flex-col gap-2 text-xs">
                  <p className="font-semibold">{order.dealer_name}</p>
                  <p className="text-shade-50">{getRoleHint(order, hasRole)}</p>
                  <p className="text-shade-50">Ngày lập: <span className="font-semibold text-ink">{order.document_date ? new Date(order.document_date).toLocaleDateString('vi-VN') : '-'}</span></p>
                  <p className="text-shade-50">Ngày giao dự kiến: <span className="font-semibold text-ink">{order.expected_delivery_date ? new Date(order.expected_delivery_date).toLocaleDateString('vi-VN') : '-'}</span></p>
                </div>
                <div className="p-4 border-t border-hairline-light flex flex-wrap gap-2">
                  {renderDoActions(order)}
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      <Modal isOpen={showCreateModal} onClose={handleCloseCreateModal} title="Lập đơn xuất hàng" maxWidth="max-w-4xl">
        <div className="flex flex-col gap-5">
          <CreditCheckBanner status={creditStatus} remainingCredit={creditCheck.remainingCredit} />

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
                label="Tìm sản phẩm trong danh sách"
                value={productSearch}
                onChange={(event) => setProductSearch(event.target.value)}
                placeholder="Nhập SKU hoặc tên sản phẩm"
              />
              <div className="flex items-center gap-2 text-xs text-shade-50">
                {masterDataLoading && <Loader2 className="h-4 w-4 animate-spin" />}
                <span>{masterDataLoading ? 'Đang tải danh mục sản phẩm...' : `Đang hiển thị ${filteredProducts.length}/${products.length} sản phẩm`}</span>
              </div>
            </div>

            <div className="overflow-hidden rounded-lg border border-hairline-light bg-canvas-light">
              <table className="w-full border-collapse text-left text-xs">
                <thead>
                  <tr className="border-b border-hairline-light bg-canvas-cream">
                    <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Sản phẩm</th>
                    <th className="w-28 px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Số lượng</th>
                    <th className="w-36 px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Báo giá</th>
                    <th className="w-10 px-4 py-4" />
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
                    <tr key={`${item.product_id || 'new'}-${index}`} className="hover:bg-canvas-cream/50 transition-colors">
                      <td className="px-4 py-3">
                        <Input
                          type="select"
                          disabled={masterDataLoading}
                          value={item.product_id}
                          onChange={(event) => updateItemRow(index, 'product_id', event.target.value)}
                          options={[
                            { value: '', label: masterDataLoading ? '-- Đang tải sản phẩm --' : '-- Chọn sản phẩm --' },
                            ...productOptionsFor(item.product_id).map((product) => ({ value: product.id, label: `[${product.sku}] ${product.name}` })),
                          ]}
                        />
                      </td>
                      <td className="px-4 py-3">
                        <Input
                          type="number"
                          min="1"
                          value={item.requested_qty}
                          onChange={(event) => updateItemRow(index, 'requested_qty', Number(event.target.value))}
                        />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex flex-col gap-1">
                          <div className="relative">
                            <Input
                              type="text"
                              value={item.price_status === 'ready' ? `${formatVND(item.unit_price)} VND` : ''}
                              disabled
                              placeholder={item.price_status === 'loading' ? 'Đang tra giá...' : 'Chọn sản phẩm'}
                            />
                            {item.price_status === 'loading' && (
                              <Loader2 className="absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 animate-spin text-shade-40" />
                            )}
                          </div>
                          {item.price_status === 'missing' && (
                            <span className="text-[11px] font-medium text-danger-600">
                              Chưa có báo giá được duyệt
                            </span>
                          )}
                        </div>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <button type="button" onClick={() => removeItemRow(index)} className="rounded-full p-1 text-shade-40 transition-colors hover:bg-canvas-cream hover:text-danger-600">
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
          <div className="flex items-center gap-2 text-sm font-semibold text-danger-600">
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
            <Button onClick={handleCancelDO} disabled={!cancelModal.reason.trim()} className="bg-danger-600 text-white hover:bg-danger-700 focus:ring-danger-500">
              Xác nhận hủy
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
