import React, { useState, useEffect } from 'react';
import { useAuthStore } from '../stores/auth.store';
import Badge from '../components/common/Badge';
import { Package, TrendingUp, AlertCircle, RefreshCw, Search, ArrowRightLeft, X, Send } from 'lucide-react';
import { masterDataService } from '../services/masterData.service';
import { interWarehouseTransferService } from '../services/inter-warehouse-transfer.service';
import { useUiStore } from '../stores/ui.store';

const Dashboard = () => {
  const { user, activeWarehouse } = useAuthStore();
  const { addToast } = useUiStore();

  const [productsStock, setProductsStock] = useState([]);
  const [physicalWarehouses, setPhysicalWarehouses] = useState([]);
  const [loadingStock, setLoadingStock] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  // Transfer Modal state
  const [showModal, setShowModal] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState(null);
  const [selectedSourceWhId, setSelectedSourceWhId] = useState('');
  const [requestedQty, setRequestedQty] = useState(1);
  const [notes, setNotes] = useState('Yêu cầu điều chuyển nhanh từ Dashboard');
  const [submitting, setSubmitting] = useState(false);

  const mockKpis = [
    {
      title: 'Tồn kho khả dụng',
      value: '4,520 sản phẩm',
      desc: 'Kho đang vận hành ổn định',
      icon: Package,
      type: 'highlight' // aloe mint green card
    },
    {
      title: 'Giao dịch trong ngày',
      value: '38 phiếu',
      desc: '12 nhập kho & 26 xuất kho',
      icon: TrendingUp,
      type: 'premium' // black card
    },
    {
      title: 'Cảnh báo tồn kho thấp',
      value: '2 sản phẩm',
      desc: 'SKU đang dưới hạn mức tối thiểu',
      icon: AlertCircle,
      type: 'danger'
    }
  ];

  const loadCrossWarehouseStock = async () => {
    setLoadingStock(true);
    try {
      // 1. Load warehouses
      const warehouses = await masterDataService.getWarehouses();
      const physicals = warehouses.filter(w => w.type !== 'IN_TRANSIT' && w.is_active !== false);
      setPhysicalWarehouses(physicals);

      // 2. Load products list
      const products = await masterDataService.getProducts({ search: searchQuery });
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
      console.error('Error loading cross stock:', error);
      addToast('Không thể tải thông tin tồn kho toàn hệ thống', 'error');
    } finally {
      setLoadingStock(false);
    }
  };

  useEffect(() => {
    loadCrossWarehouseStock();
  }, [searchQuery]);

  const handleOpenTransferModal = (product) => {
    setSelectedProduct(product);
    // Find first other warehouse that has stock > 0 to select as default source
    const otherWhs = physicalWarehouses.filter(w => Number(w.id) !== Number(activeWarehouse?.id));
    const defaultSrc = otherWhs.find(w => (product.stockMap?.[w.id] || 0) > 0);
    
    setSelectedSourceWhId(defaultSrc ? String(defaultSrc.id) : '');
    setRequestedQty(1);
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

    setSubmitting(true);
    try {
      const payload = {
        sourceWarehouseId: Number(selectedSourceWhId),
        destinationWarehouseId: Number(activeWarehouse.id),
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
      console.error('Error creating transfer request:', error);
      addToast(error.message || 'Lỗi khi tạo yêu cầu điều chuyển', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Welcome Banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            WMS Phúc Anh
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Xin chào, {user?.fullName || 'Người dùng'}
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Hôm nay bạn đang làm việc tại <span className="font-semibold text-ink">{activeWarehouse?.name || 'Chưa chọn kho'}</span>.
          </p>
        </div>

        <div>
          <Badge type="highlight" className="text-xs py-1 px-3">
            {activeWarehouse?.code || 'HP-01'}
          </Badge>
        </div>
      </div>

      {/* KPI Cards section */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {mockKpis.map((kpi, idx) => {
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
            cardStyle = 'bg-red-50/50 rounded-lg p-6 border border-red-200 shadow-level-3 hover:shadow-lg transition-all duration-200';
            titleStyle = 'text-red-700/80';
            valStyle = 'text-red-700';
            descStyle = 'text-red-600/60';
            iconStyle = 'text-red-700 bg-red-100/50';
          }

          return (
            <div key={idx} className={`${cardStyle} flex flex-col justify-between h-40`}>
              <div className="flex justify-between items-start">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider block">
                    {kpi.title}
                  </span>
                  <span className="text-xl md:text-2xl font-display font-semibold block mt-2">
                    {kpi.value}
                  </span>
                </div>
                <div className={`p-2.5 rounded-full ${iconStyle}`}>
                  <kpi.icon className="w-5 h-5" />
                </div>
              </div>
              <span className={`text-[11px] font-light ${descStyle}`}>
                {kpi.desc}
              </span>
            </div>
          );
        })}
      </div>

      {/* Cross-Warehouse Stock Management Section */}
      <div className="card-premium flex flex-col gap-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between border-b border-hairline-light pb-3 gap-3">
          <div>
            <h3 className="text-sm font-bold text-shade-70 uppercase tracking-wider">
              Tồn kho hệ thống & Xin điều chuyển nhanh
            </h3>
            <p className="text-[11px] text-shade-50 font-light mt-0.5">
              So sánh lượng tồn kho khả dụng giữa các chi nhánh vật lý và lập đề xuất nhanh.
            </p>
          </div>
          
          <div className="flex items-center gap-3">
            {/* Search Input */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-shade-40 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Tìm SKU hoặc tên..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-8 pr-3 py-1.5 bg-canvas-cream border border-hairline-light rounded-md text-xs focus:outline-none focus:border-shade-40 w-48 sm:w-60 font-light"
              />
            </div>
            
            <button
              onClick={loadCrossWarehouseStock}
              disabled={loadingStock}
              className="text-xs text-shade-60 hover:text-ink flex items-center gap-1 font-semibold transition-colors disabled:opacity-50"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingStock ? 'animate-spin' : ''}`} />
              <span>Tải lại</span>
            </button>
          </div>
        </div>

        {loadingStock && productsStock.length === 0 ? (
          <div className="py-12 text-center text-shade-50 text-xs font-light">
            Đang truy vấn tồn kho hệ thống...
          </div>
        ) : productsStock.length === 0 ? (
          <div className="py-12 text-center text-shade-50 text-xs font-light">
            Không tìm thấy sản phẩm nào phù hợp.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-hairline-light bg-canvas-cream/50 text-[10px] uppercase font-bold text-shade-60 tracking-wider">
                  <th className="py-3 px-4">Mã SKU</th>
                  <th className="py-3 px-4">Tên Sản Phẩm</th>
                  <th className="py-3 px-4">Đơn vị</th>
                  {physicalWarehouses.map((wh) => (
                    <th key={wh.id} className="py-3 px-4 text-center">
                      {wh.name}
                    </th>
                  ))}
                  <th className="py-3 px-4 text-right">Hành động</th>
                </tr>
              </thead>
              <tbody>
                {productsStock.map((prod) => {
                  // Check if any other warehouse has stock > 0
                  const hasStockElsewhere = physicalWarehouses.some(
                    wh => Number(wh.id) !== Number(activeWarehouse?.id) && (prod.stockMap?.[wh.id] || 0) > 0
                  );

                  return (
                    <tr
                      key={prod.id}
                      className="border-b border-hairline-light hover:bg-canvas-cream/30 transition-colors text-xs font-light text-ink"
                    >
                      <td className="py-3 px-4 font-mono font-semibold text-shade-70">
                        {prod.sku}
                      </td>
                      <td className="py-3 px-4 font-normal">
                        {prod.name}
                      </td>
                      <td className="py-3 px-4 text-shade-50">
                        {prod.unit}
                      </td>
                      {physicalWarehouses.map((wh) => {
                        const qty = prod.stockMap?.[wh.id] || 0;
                        const isActiveWh = Number(wh.id) === Number(activeWarehouse?.id);
                        return (
                          <td
                            key={wh.id}
                            className={`py-3 px-4 text-center font-semibold ${
                              isActiveWh 
                                ? 'bg-canvas-cream/40 border-x border-hairline-light text-ink'
                                : qty > 0 ? 'text-[#127a3c]' : 'text-shade-40'
                            }`}
                          >
                            {qty} {prod.unit.toLowerCase()}
                          </td>
                        );
                      })}
                      <td className="py-3 px-4 text-right">
                        {hasStockElsewhere ? (
                          <button
                            onClick={() => handleOpenTransferModal(prod)}
                            className="bg-aloe-10 hover:opacity-90 text-ink border border-aloe-10 px-3 py-1 rounded-pill text-[10px] font-bold uppercase tracking-wider transition-colors inline-flex items-center gap-1.5"
                          >
                            <ArrowRightLeft className="w-3 h-3" />
                            <span>Xin điều chuyển</span>
                          </button>
                        ) : (
                          <span className="text-[10px] text-shade-40 uppercase font-semibold">Không có sẵn</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Quick Transfer Request Modal */}
      {showModal && selectedProduct && (
        <div className="fixed inset-0 bg-canvas-night/60 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <div className="bg-canvas-cream border border-hairline-light rounded-lg shadow-level-3 max-w-md w-full overflow-hidden flex flex-col">
            {/* Modal Header */}
            <div className="bg-canvas-night text-onPrimary p-4 flex justify-between items-center border-b border-hairline-dark">
              <div className="flex items-center gap-2">
                <ArrowRightLeft className="w-4 h-4 text-[#127a3c]" />
                <h4 className="text-sm font-bold uppercase tracking-wider">
                  Xin điều chuyển nhanh
                </h4>
              </div>
              <button
                onClick={() => setShowModal(false)}
                className="text-shade-40 hover:text-onPrimary transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Modal Body */}
            <form onSubmit={handleCreateTransferRequest} className="p-5 flex flex-col gap-4">
              <div>
                <span className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-1">
                  Sản phẩm yêu cầu
                </span>
                <div className="bg-canvas-light p-3 rounded-md border border-hairline-light flex flex-col gap-1">
                  <span className="font-mono font-bold text-xs text-shade-70">{selectedProduct.sku}</span>
                  <span className="text-xs text-ink font-semibold">{selectedProduct.name}</span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-1">
                    Kho nhận (Kho đích)
                  </span>
                  <div className="bg-canvas-cream/50 p-2.5 rounded-md border border-hairline-light text-xs font-semibold text-shade-70">
                    {activeWarehouse?.name || 'Kho hiện tại'}
                  </div>
                </div>

                <div>
                  <label className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-1">
                    Kho gửi (Kho nguồn)
                  </label>
                  <select
                    value={selectedSourceWhId}
                    onChange={(e) => setSelectedSourceWhId(e.target.value)}
                    required
                    className="w-full bg-canvas-light border border-hairline-light rounded-md p-2 text-xs focus:outline-none focus:border-ink font-semibold"
                  >
                    <option value="">-- Chọn kho gửi --</option>
                    {physicalWarehouses
                      .filter(w => Number(w.id) !== Number(activeWarehouse?.id))
                      .map((w) => {
                        const qty = selectedProduct.stockMap?.[w.id] || 0;
                        return (
                          <option key={w.id} value={w.id} disabled={qty <= 0}>
                            {w.name} (Sẵn có: {qty} {selectedProduct.unit.toLowerCase()})
                          </option>
                        );
                      })}
                  </select>
                </div>
              </div>

              <div>
                <label className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-1">
                  Số lượng yêu cầu (Tối đa: {selectedSourceWhId ? selectedProduct.stockMap?.[selectedSourceWhId] || 0 : 0})
                </label>
                <input
                  type="number"
                  min="1"
                  max={selectedSourceWhId ? selectedProduct.stockMap?.[selectedSourceWhId] || 9999 : 1}
                  value={requestedQty}
                  onChange={(e) => setRequestedQty(Math.max(1, Number(e.target.value)))}
                  required
                  className="w-full bg-canvas-light border border-hairline-light rounded-md p-2 text-xs focus:outline-none focus:border-ink font-semibold"
                />
              </div>

              <div>
                <label className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-1">
                  Ghi chú yêu cầu
                </label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows="2"
                  className="w-full bg-canvas-light border border-hairline-light rounded-md p-2 text-xs focus:outline-none focus:border-ink font-light"
                  placeholder="Lý do xin điều chuyển..."
                />
              </div>

              {/* Modal Footer */}
              <div className="flex justify-end gap-3 border-t border-hairline-light pt-4 mt-2">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="bg-canvas-light border border-hairline-light hover:bg-canvas-cream text-ink px-4 py-2 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="bg-canvas-night hover:bg-canvas-nightElevated text-onPrimary px-4 py-2 rounded-pill text-xs font-semibold uppercase tracking-wider transition-colors inline-flex items-center gap-1.5 disabled:opacity-50"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>{submitting ? 'Đang gửi...' : 'Gửi yêu cầu nháp'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Dashboard;
