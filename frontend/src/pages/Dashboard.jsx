import React, { useState, useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../stores/auth.store';
import Badge from '../components/common/Badge';
import Input from '../components/common/Input';
import Modal from '../components/common/Modal';
import Button from '../components/common/Button';
import { Package, TrendingUp, AlertCircle, RefreshCw, Search, ArrowRightLeft, Send } from 'lucide-react';
import { ROLES } from '../utils/constants';
import { masterDataService } from '../services/masterData.service';
import { interWarehouseTransferService } from '../services/inter-warehouse-transfer.service';
import { useUiStore } from '../stores/ui.store';
import { useDebounce } from '../hooks/useDebounce';

const Dashboard = () => {
  const { user, activeWarehouse } = useAuthStore();
  const { addToast } = useUiStore();

  const [productsStock, setProductsStock] = useState([]);
  const [physicalWarehouses, setPhysicalWarehouses] = useState([]);
  const [loadingStock, setLoadingStock] = useState(false);
  const [loadingOverview, setLoadingOverview] = useState(false);
  const [stockOverview, setStockOverview] = useState({
    availableQty: 0,
    todayReceiptCount: 0,
    todayDeliveryOrderCount: 0,
    activeLowStockCount: 0,
  });
  const [searchQuery, setSearchQuery] = useState('');

  // Transfer Modal state
  const [showModal, setShowModal] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [selectedSourceWhId, setSelectedSourceWhId] = useState('');
  const [requestedQty, setRequestedQty] = useState(1);
  const [neededByDate, setNeededByDate] = useState('');
  const [businessReason, setBusinessReason] = useState('');
  const [notes, setNotes] = useState('Yêu cầu điều chuyển nhanh từ Dashboard');
  const [submitting, setSubmitting] = useState(false);
  const [mobileStockLimit, setMobileStockLimit] = useState(3);

  const formatQuantity = (value) => Number(value || 0).toLocaleString('vi-VN', {
    maximumFractionDigits: 2,
  });

  const overviewKpis = [
    {
      title: 'Tồn kho khả dụng',
      value: loadingOverview ? 'Đang tải...' : `${formatQuantity(stockOverview.availableQty)} sản phẩm`,
      desc: activeWarehouse?.name ? `Dữ liệu từ ${activeWarehouse.name}` : 'Chưa chọn kho',
      icon: Package,
      type: 'highlight'
    },
    {
      title: 'Giao dịch trong ngày',
      value: loadingOverview
        ? 'Đang tải...'
        : `${stockOverview.todayReceiptCount + stockOverview.todayDeliveryOrderCount} phiếu`,
      desc: `${stockOverview.todayReceiptCount} nhập kho & ${stockOverview.todayDeliveryOrderCount} xuất kho`,
      icon: TrendingUp,
      type: 'premium'
    },
    {
      title: 'Cảnh báo tồn kho thấp',
      value: loadingOverview ? 'Đang tải...' : `${stockOverview.activeLowStockCount} sản phẩm`,
      desc: 'SKU đang dưới hạn mức tối thiểu',
      icon: AlertCircle,
      type: 'danger'
    }
  ];

  const debouncedSearchQuery = useDebounce(searchQuery);
  const welcomeName = user?.fullName
    ? user.fullName.split(' ').filter(Boolean).slice(0, 2).join(' ')
    : 'Người dùng';

  const loadStockOverview = async () => {
    if (!activeWarehouse?.id) return;
    setLoadingOverview(true);
    try {
      const data = await interWarehouseTransferService.getStockOverview(activeWarehouse.id);
      setStockOverview({
        availableQty: Number(data.availableQty ?? data.available_qty ?? 0),
        todayReceiptCount: Number(data.todayReceiptCount ?? data.today_receipt_count ?? 0),
        todayDeliveryOrderCount: Number(data.todayDeliveryOrderCount ?? data.today_delivery_order_count ?? 0),
        activeLowStockCount: Number(data.activeLowStockCount ?? data.active_low_stock_count ?? 0),
      });
    } catch (error) {
      addToast('Không thể tải chỉ số tổng quan từ database', 'error');
    } finally {
      setLoadingOverview(false);
    }
  };

  const loadCrossWarehouseStock = async () => {
    setLoadingStock(true);
    try {
      // 1. Load warehouses
      const warehouses = await masterDataService.getWarehouses();
      const physicals = warehouses.filter(w => w.type !== 'IN_TRANSIT' && w.is_active !== false);
      setPhysicalWarehouses(physicals);

      // 2. Load products list
      const products = await masterDataService.getProducts({ search: debouncedSearchQuery });
      const list = products.slice(0, 10); // Limit to 10 for performance

      // 3. For each product, lookup stock in all warehouses
      const results = await Promise.all(
        list.map(async (p) => {
          try {
            const stockDetails = await interWarehouseTransferService.stockLookup(p.id);
            return {
              ...p,
              stockMap: Object.fromEntries(
                stockDetails.map(s => [s.warehouseId, s.availableQty])
              )
            };
          } catch (err) {
            return {
              ...p,
              stockMap: {}
            };
          }
        })
      );
      setProductsStock(results);
    } catch (error) {
      addToast('Không thể tải thông tin tồn kho toàn hệ thống', 'error');
    } finally {
      setLoadingStock(false);
    }
  };

  useEffect(() => {
    document.documentElement.scrollLeft = 0;
    document.body.scrollLeft = 0;
    document.querySelectorAll('.app-main, .app-main-inner, .app-content').forEach((node) => {
      node.scrollLeft = 0;
    });
  }, []);

  // Accountants have no warehouse assignment and no need for warehouse-stock KPIs;
  // the backend rejects these calls for their role, which otherwise surfaces as a
  // misleading "cannot load from database" toast instead of sending them to a page
  // they can actually use.
  const isAccountingRole = user?.role === ROLES.ACCOUNTANT || user?.role === ROLES.ACCOUNTANT_MANAGER;

  useEffect(() => {
    if (user?.role === ROLES.DRIVER || isAccountingRole) return;
    setMobileStockLimit(3);
    loadCrossWarehouseStock();
  }, [debouncedSearchQuery, user?.role]);

  useEffect(() => {
    if (user?.role === ROLES.DRIVER || isAccountingRole) return;
    loadStockOverview();
  }, [activeWarehouse?.id, user?.role]);

  if (user?.role === ROLES.DRIVER) {
    return <Navigate to="/outbound/driver/trips" replace />;
  }

  if (isAccountingRole) {
    return <Navigate to="/finance/invoices" replace />;
  }

  const handleOpenTransferModal = (product) => {
    setSelectedProduct(product);
    // Find first other warehouse that has stock > 0 to select as default source
    const otherWhs = physicalWarehouses.filter(w => Number(w.id) !== Number(activeWarehouse?.id));
    const defaultSrc = otherWhs.find(w => (product.stockMap?.[w.id] || 0) > 0);
    
    setSelectedSourceWhId(defaultSrc ? String(defaultSrc.id) : '');
    setRequestedQty(1);
    const defaultNeededBy = new Date();
    defaultNeededBy.setDate(defaultNeededBy.getDate() + 2);
    setNeededByDate(defaultNeededBy.toISOString().slice(0, 10));
    setBusinessReason(`Bổ sung tồn khả dụng cho ${product.sku}`);
    setNotes(`Yêu cầu điều chuyển nhanh sản phẩm ${product.sku} từ Dashboard`);
    setShowModal(true);
  };

  const handleCreateTransferRequest = async (e) => {
    e.preventDefault();
    if (!selectedSourceWhId) {
      addToast('Vui lòng chọn kho nguồn gửi hàng', 'warning');
      return;
    }
    if (requestedQty <= 0) {
      addToast('Số lượng yêu cầu phải lớn hơn 0', 'warning');
      return;
    }
    if (!businessReason.trim()) {
      addToast('Vui lòng nhập lý do nghiệp vụ', 'warning');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        sourceWarehouseId: Number(selectedSourceWhId),
        destinationWarehouseId: Number(activeWarehouse.id),
        neededByDate: neededByDate || null,
        businessReason: businessReason.trim(),
        notes: notes,
        items: [
          {
            productId: Number(selectedProduct.id),
            requestedQty: Number(requestedQty)
          }
        ]
      };

      await interWarehouseTransferService.createTransferRequest(payload);
      addToast('Đã tạo thành công yêu cầu điều chuyển nháp (DRAFT)!', 'success');
      setShowModal(false);
      loadCrossWarehouseStock(); // Refresh stock
    } catch (error) {
      addToast(error.message || 'Lỗi khi tạo yêu cầu điều chuyển', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="mobile-page w-full max-w-full overflow-x-hidden">
      {/* Welcome Banner */}
      <div className="dashboard-hero flex w-full min-w-0 max-w-full flex-col items-start justify-between gap-3 md:flex-row md:items-center">
        <div className="min-w-0 max-w-full">
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            WMS Phúc Anh
          </span>
          <h1 className="dashboard-title text-xl md:text-3xl font-display font-semibold tracking-tight">
            Xin chào, {welcomeName}
          </h1>
          <p className="dashboard-subtitle text-xs text-shade-50 font-light mt-1">
            Hôm nay bạn đang làm việc tại <span className="font-semibold text-ink">{activeWarehouse?.name || 'Chưa chọn kho'}</span>.
          </p>
        </div>

        <div className="min-w-0 max-w-full">
          <Badge type="highlight" className="text-xs py-1 px-3">
            {activeWarehouse?.code || 'HP-01'}
          </Badge>
        </div>
      </div>

      {/* KPI Cards section */}
      <div className="grid w-full min-w-0 grid-cols-1 gap-3 md:grid-cols-3 md:gap-6">
        {overviewKpis.map((kpi, idx) => {
          const isHighlight = kpi.type === 'highlight';
          const isPremium = kpi.type === 'premium';
          const isDanger = kpi.type === 'danger';

          let cardStyle = 'card-premium';
          let titleStyle = 'text-shade-60';
          let valStyle = 'text-ink';
          let descStyle = 'text-shade-50';
          let iconStyle = 'text-shade-70 bg-canvas-cream';

          if (isHighlight) {
            cardStyle = 'card-featured-mint';
            titleStyle = 'text-ink/75';
            valStyle = 'text-ink';
            descStyle = 'text-ink/60';
            iconStyle = 'text-ink bg-canvas-light/50';
          } else if (isPremium) {
            cardStyle = 'bg-canvas-night text-onPrimary rounded-lg p-6 shadow-level-3 transition-all duration-200 border border-hairline-dark';
            titleStyle = 'text-shade-40';
            valStyle = 'text-onPrimary';
            descStyle = 'text-shade-40';
            iconStyle = 'text-onPrimary bg-canvas-nightElevated';
          } else if (isDanger) {
            cardStyle = 'bg-danger-50/50 rounded-lg p-6 border border-danger-200 shadow-level-3 hover:shadow-lg transition-all duration-200';
            titleStyle = 'text-danger-700/80';
            valStyle = 'text-danger-700';
            descStyle = 'text-danger-600/60';
            iconStyle = 'text-danger-700 bg-danger-100/50';
          }

          return (
            <div key={idx} className={`${cardStyle} mobile-kpi-card flex h-40 min-w-0 max-w-full flex-col justify-between overflow-hidden`}>
              <div className="flex min-w-0 items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <span className="text-[10px] font-bold uppercase tracking-wider block">
                    {kpi.title}
                  </span>
                  <span className="mobile-kpi-value mt-2 block max-w-full font-display text-xl font-semibold leading-tight md:text-2xl">
                    {kpi.value}
                  </span>
                </div>
                <div className={`mobile-kpi-icon shrink-0 rounded-full p-2.5 ${iconStyle}`}>
                  <kpi.icon className="w-5 h-5" />
                </div>
              </div>
              <span className={`mobile-kpi-desc text-[11px] font-light ${descStyle}`}>
                {kpi.desc}
              </span>
            </div>
          );
        })}
      </div>

      {/* Cross-Warehouse Stock Management Section */}
      <div className="flex min-w-0 max-w-full flex-col overflow-hidden rounded-lg border border-hairline-light bg-canvas-light shadow-level-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-hairline-light px-4 sm:px-6 py-4 gap-3">
          <div className="min-w-0">
            <h3 className="text-sm font-bold text-shade-70 uppercase tracking-wider">
              Tồn kho hệ thống & Xin điều chuyển nhanh
            </h3>
            <p className="text-[11px] text-shade-50 font-light mt-0.5">
              So sánh lượng tồn kho khả dụng giữa các chi nhánh vật lý và lập đề xuất nhanh.
            </p>
          </div>
          
          <div className="mobile-filter-bar sm:flex sm:items-center sm:gap-3">
            {/* Search Input */}
            <div className="sm:w-60">
              <Input
                type="text"
                leftIcon={Search}
                placeholder="Tìm SKU hoặc tên..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
            
            <button
              onClick={() => {
                loadStockOverview();
                loadCrossWarehouseStock();
              }}
              disabled={loadingStock || loadingOverview}
              className="text-xs text-shade-60 hover:text-ink flex items-center gap-1 font-semibold transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingStock || loadingOverview ? 'animate-spin' : ''}`} />
              <span>Tải lại</span>
            </button>
          </div>
        </div>

        {loadingStock && productsStock.length === 0 ? (
          <div className="px-6 py-12 text-center text-shade-50 text-xs font-light">
            Đang truy vấn tồn kho hệ thống...
          </div>
        ) : productsStock.length === 0 ? (
          <div className="px-6 py-12 text-center text-shade-50 text-xs font-light">
            Không tìm thấy sản phẩm nào phù hợp.
          </div>
        ) : (
          <>
            {/* Desktop/tablet: table view */}
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-canvas-cream border-b border-hairline-light">
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã SKU</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Tên Sản Phẩm</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Đơn vị</th>
                    {physicalWarehouses.map((wh) => (
                      <th key={wh.id} className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center">
                        {wh.name}
                      </th>
                    ))}
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {productsStock.map((prod) => {
                    // Check if any other warehouse has stock > 0
                    const hasStockElsewhere = physicalWarehouses.some(
                      wh => Number(wh.id) !== Number(activeWarehouse?.id) && (prod.stockMap?.[wh.id] || 0) > 0
                    );

                    return (
                      <tr
                        key={prod.id}
                        className="hover:bg-canvas-cream/50 transition-colors text-xs font-light text-ink"
                      >
                        <td className="px-6 py-4 font-mono font-semibold text-shade-70">
                          {prod.sku}
                        </td>
                        <td className="px-6 py-4 font-normal">
                          {prod.name}
                        </td>
                        <td className="px-6 py-4 text-shade-50">
                          {prod.unit}
                        </td>
                        {physicalWarehouses.map((wh) => {
                          const qty = prod.stockMap?.[wh.id] || 0;
                          const isActiveWh = Number(wh.id) === Number(activeWarehouse?.id);
                          return (
                            <td
                              key={wh.id}
                              className={`px-6 py-4 text-center font-semibold ${
                                isActiveWh
                                  ? 'bg-canvas-cream/40 border-x border-hairline-light text-ink'
                                  : qty > 0 ? 'text-[#127a3c]' : 'text-shade-40'
                              }`}
                            >
                              {qty} {prod.unit.toLowerCase()}
                            </td>
                          );
                        })}
                        <td className="px-6 py-4 text-right">
                          {hasStockElsewhere ? (
                            <button
                              onClick={() => handleOpenTransferModal(prod)}
                              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill btn-pill-aloe text-xs font-semibold transition-colors"
                            >
                              <ArrowRightLeft className="w-3.5 h-3.5" />
                              Xin điều chuyển
                            </button>
                          ) : (
                            <span className="text-shade-50 text-[10px] font-medium">Không có sẵn</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Mobile: stacked card view */}
            <div className="flex flex-col gap-3 p-3 md:hidden">
              {productsStock.slice(0, mobileStockLimit).map((prod) => {
                const hasStockElsewhere = physicalWarehouses.some(
                  wh => Number(wh.id) !== Number(activeWarehouse?.id) && (prod.stockMap?.[wh.id] || 0) > 0
                );
                const compactWarehouses = physicalWarehouses.filter((wh) => {
                  const qty = prod.stockMap?.[wh.id] || 0;
                  return Number(wh.id) === Number(activeWarehouse?.id) || qty > 0;
                });

                return (
                  <div key={prod.id} className="mobile-stock-card rounded-lg border border-hairline-light bg-canvas-cream/30 overflow-hidden text-xs font-light text-ink">
                    <div className="border-b border-hairline-light bg-canvas-cream p-3 flex justify-between items-center gap-2">
                      <span className="font-mono font-semibold text-shade-70">{prod.sku}</span>
                      <span className="text-shade-50">{prod.unit}</span>
                    </div>
                    <div className="p-3 flex flex-col gap-2">
                      <div className="font-normal leading-snug">{prod.name}</div>
                      <div className="grid grid-cols-1 gap-2 mt-1">
                        {compactWarehouses.map((wh) => {
                          const qty = prod.stockMap?.[wh.id] || 0;
                          const isActiveWh = Number(wh.id) === Number(activeWarehouse?.id);
                          return (
                            <div
                              key={wh.id}
                              className={`flex items-center justify-between gap-2 rounded px-2.5 py-2 font-semibold ${
                                isActiveWh
                                  ? 'bg-canvas-cream border border-hairline-light text-ink'
                                  : qty > 0 ? 'text-[#127a3c] bg-canvas-light' : 'text-shade-40 bg-canvas-light'
                              }`}
                            >
                              <div className="min-w-0 truncate text-[9px] uppercase tracking-wide font-bold">{wh.name}</div>
                              <div className="shrink-0">{qty} {prod.unit.toLowerCase()}</div>
                            </div>
                          );
                        })}
                      </div>
                    </div>
                    <div className="border-t border-hairline-light p-3 flex justify-end">
                      {hasStockElsewhere ? (
                        <button
                          onClick={() => handleOpenTransferModal(prod)}
                          className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill btn-pill-aloe text-xs font-semibold transition-colors"
                        >
                          <ArrowRightLeft className="w-3.5 h-3.5" />
                          Xin điều chuyển
                        </button>
                      ) : (
                        <span className="text-shade-50 text-[10px] font-medium">Không có sẵn</span>
                      )}
                    </div>
                  </div>
                );
              })}
              {productsStock.length > mobileStockLimit && (
                <button
                  type="button"
                  onClick={() => setMobileStockLimit((limit) => Math.min(limit + 3, productsStock.length))}
                  className="rounded-pill border border-hairline-light bg-canvas-light px-4 py-2 text-xs font-semibold text-shade-70 shadow-level-1"
                >
                  Xem thêm {Math.min(3, productsStock.length - mobileStockLimit)} sản phẩm
                </button>
              )}
            </div>
          </>
        )}
      </div>

      {/* Quick Transfer Request Modal */}
      <Modal isOpen={showModal && !!selectedProduct} onClose={() => setShowModal(false)} title="Xin điều chuyển nhanh" maxWidth="max-w-md">
        {selectedProduct && (
          <form onSubmit={handleCreateTransferRequest} className="flex flex-col gap-4">
            <div>
              <span className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-1.5">
                Sản phẩm yêu cầu
              </span>
              <div className="bg-canvas-cream p-3 rounded-md border border-hairline-light flex flex-col gap-1">
                <span className="font-mono font-bold text-xs text-shade-70">{selectedProduct.sku}</span>
                <span className="text-xs text-ink font-semibold">{selectedProduct.name}</span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Kho nhận (Kho đích)"
                value={activeWarehouse?.name || 'Kho hiện tại'}
                disabled
              />

              <Input
                label="Kho gửi (Kho nguồn) *"
                type="select"
                value={selectedSourceWhId}
                onChange={(e) => setSelectedSourceWhId(e.target.value)}
                required
                options={[
                  { value: '', label: '-- Chọn kho gửi --' },
                  ...physicalWarehouses
                    .filter(w => Number(w.id) !== Number(activeWarehouse?.id))
                    .map((w) => {
                      const qty = selectedProduct.stockMap?.[w.id] || 0;
                      return {
                        value: w.id,
                        label: `${w.name} (Sẵn có: ${qty} ${selectedProduct.unit.toLowerCase()})`,
                        disabled: qty <= 0,
                      };
                    }),
                ]}
              />
            </div>

            <Input
              label={`Số lượng yêu cầu (Tối đa: ${selectedSourceWhId ? selectedProduct.stockMap?.[selectedSourceWhId] || 0 : 0})`}
              type="number"
              min="1"
              max={selectedSourceWhId ? selectedProduct.stockMap?.[selectedSourceWhId] || 9999 : 1}
              value={requestedQty}
              onChange={(e) => setRequestedQty(Math.max(1, Number(e.target.value)))}
              required
            />

            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Ngày cần hàng"
                type="date"
                value={neededByDate}
                onChange={(e) => setNeededByDate(e.target.value)}
              />

              <Input
                label="Lý do nghiệp vụ *"
                type="text"
                value={businessReason}
                onChange={(e) => setBusinessReason(e.target.value)}
                required
                placeholder="VD: Bổ sung tồn bán"
              />
            </div>

            <div>
              <label className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-1.5">
                Ghi chú yêu cầu
              </label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows="2"
                className="text-input resize-none"
                placeholder="Lý do xin điều chuyển..."
              />
            </div>

            <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
              <Button type="button" variant="outline-light" onClick={() => setShowModal(false)}>
                Hủy
              </Button>
              <Button type="submit" variant="primary" icon={Send} loading={submitting}>
                {submitting ? 'Đang gửi...' : 'Gửi yêu cầu nháp'}
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
};

export default Dashboard;
