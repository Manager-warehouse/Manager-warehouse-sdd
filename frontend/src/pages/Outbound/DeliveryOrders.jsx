import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  PackageCheck, Plus, Search, Eye,
  Clock, Truck, AlertTriangle, X, Loader2
} from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import CreditCheckBanner from '../../components/warehouse/CreditCheckBanner';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { ROLES } from '../../utils/constants';

const DO_STATUS_MAP = {
  NEW:          { label: 'Mới',             color: 'bg-zinc-100 text-zinc-800 border-zinc-200' },
  PICKING:      { label: 'Đang soạn',       color: 'bg-blue-50 text-blue-700 border-blue-200' },
  READY_TO_SHIP:{ label: 'Chờ vận chuyển',  color: 'bg-amber-50 text-amber-700 border-amber-200' },
  IN_TRANSIT:   { label: 'Đang giao',       color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  DELIVERED:    { label: 'Đã giao',         color: 'bg-aloe-10 text-emerald-900 border-emerald-300' },
  RETURNED:     { label: 'Hoàn trả',        color: 'bg-orange-50 text-orange-700 border-orange-200' },
  CANCELLED:    { label: 'Đã hủy',          color: 'bg-red-50 text-red-700 border-red-200' },
};

const getStatusBadge = (status) => {
  const base = 'text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase tracking-wider whitespace-nowrap';
  const { label, color } = DO_STATUS_MAP[status] ?? { label: status, color: 'bg-zinc-100 text-zinc-800 border-zinc-200' };
  return <span className={`${base} ${color}`}>{label}</span>;
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
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [formData, setFormData] = useState({ dealer_id: '', expected_delivery_date: '', notes: '', items: [] });
  const [selectedDealerObj, setSelectedDealerObj] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [cancelModal, setCancelModal] = useState({ show: false, orderId: null, reason: '' });

  useEffect(() => {
    fetchOrders();
  }, [activeWarehouse?.id, statusFilter, search]);

  useEffect(() => {
    fetchMasterData();
  }, []);

  const fetchOrders = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getDeliveryOrders(activeWarehouse?.id, { status: statusFilter, search });
      setOrders(data);
    } catch {
      addToast('Lỗi khi tải danh sách đơn xuất', 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchMasterData = async () => {
    try {
      const [dealersData, productsData] = await Promise.all([
        masterDataService.getPartners('DEALER'),
        masterDataService.getProducts(),
      ]);
      setDealers(dealersData);
      setProducts(productsData.filter(p => p.is_active));
    } catch {
      // fail silently — master data errors don't block the list
    }
  };

  const handleCreateSubmit = async () => {
    if (formData.items.length === 0) { addToast('Vui lòng thêm ít nhất 1 sản phẩm', 'error'); return; }
    setSubmitting(true);
    try {
      await outboundService.createDeliveryOrder({
        ...formData,
        dealer_name: selectedDealerObj?.name,
        warehouse_id: activeWarehouse.id,
      });
      addToast('Tạo đơn xuất hàng thành công', 'success');
      setShowCreateModal(false);
      setFormData({ dealer_id: '', expected_delivery_date: '', notes: '', items: [] });
      setSelectedDealerObj(null);
      fetchOrders();
    } catch (error) {
      addToast(error.message === 'CREDIT_HOLD' ? 'Đại lý đang bị khóa công nợ!' : (error.message || 'Lỗi khi tạo đơn'), 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleApproveInline = async (orderId) => {
    try {
      await outboundService.approveWarehouseOutbound(orderId);
      addToast('Đã phê duyệt xuất kho thành công', 'success');
      fetchOrders();
    } catch {
      addToast('Lỗi khi phê duyệt', 'error');
    }
  };

  const handleCancelDO = async () => {
    if (!cancelModal.reason) { addToast('Vui lòng nhập lý do hủy', 'error'); return; }
    try {
      await outboundService.cancelDeliveryOrder(cancelModal.orderId, cancelModal.reason);
      addToast('Hủy đơn thành công', 'success');
      setCancelModal({ show: false, orderId: null, reason: '' });
      fetchOrders();
    } catch {
      addToast('Lỗi khi hủy đơn', 'error');
    }
  };

  const addItemRow = () => setFormData(prev => ({ ...prev, items: [...prev.items, { product_id: '', requested_qty: 1 }] }));

  const updateItemRow = (index, field, value) => {
    const newItems = [...formData.items];
    newItems[index][field] = value;
    if (field === 'product_id') {
      const prod = products.find(p => p.id === Number(value));
      if (prod) { newItems[index].product_name = prod.name; newItems[index].sku = prod.sku; }
    }
    setFormData({ ...formData, items: newItems });
  };

  const removeItemRow = (index) => {
    const newItems = [...formData.items];
    newItems.splice(index, 1);
    setFormData({ ...formData, items: newItems });
  };

  const totalDO = orders.length;
  const pickingDO = orders.filter(o => o.status === 'PICKING').length;
  const readyDO = orders.filter(o => o.status === 'READY_TO_SHIP').length;
  const inTransitDO = orders.filter(o => o.status === 'IN_TRANSIT').length;

  const creditStatus = selectedDealerObj?.id === 4 ? 'BLOCKED' : selectedDealerObj?.id === 2 ? 'WARNING' : selectedDealerObj ? 'OK' : null;
  const isSubmitDisabled = !formData.dealer_id || !formData.expected_delivery_date || formData.items.length === 0 || creditStatus === 'BLOCKED' || submitting;

  return (
    <div className="flex flex-col gap-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Vận hành / Xuất kho
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Đơn xuất hàng
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản lý lệnh xuất cho đại lý tại kho <span className="font-semibold text-ink">{activeWarehouse?.name} ({activeWarehouse?.code})</span>.
          </p>
        </div>
        {(hasRole(ROLES.PLANNER) || hasRole(ROLES.ADMIN)) && (
          <button onClick={() => setShowCreateModal(true)} className="btn-pill btn-pill-primary flex items-center gap-2">
            <Plus className="w-4 h-4" />
            <span>Lập đơn xuất mới</span>
          </button>
        )}
      </div>

      {/* KPI Pills */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {[
          { label: 'Tổng đơn', value: totalDO, icon: <PackageCheck className="w-5 h-5" />, accent: 'text-zinc-600 bg-zinc-100' },
          { label: 'Đang soạn', value: pickingDO, icon: <Clock className="w-5 h-5" />, accent: 'text-blue-600 bg-blue-50' },
          { label: 'Chờ vận chuyển', value: readyDO, icon: <PackageCheck className="w-5 h-5" />, accent: 'text-amber-600 bg-amber-50' },
          { label: 'Đang giao', value: inTransitDO, icon: <Truck className="w-5 h-5" />, accent: 'text-indigo-600 bg-indigo-50' },
        ].map(({ label, value, icon, accent }) => (
          <div key={label} className="bg-white rounded-lg border border-hairline-light p-4 shadow-sm flex items-center gap-3">
            <div className={`p-2.5 rounded-full ${accent}`}>{icon}</div>
            <div>
              <p className="text-xs text-shade-50 font-medium">{label}</p>
              <p className="text-2xl font-bold text-ink">{value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg border border-hairline-light p-4 shadow-sm flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
          <input
            type="text"
            placeholder="Tìm mã DO, tên đại lý..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full text-input pl-10"
          />
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold text-shade-50">Trạng thái:</span>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="text-input text-xs py-1.5">
            <option value="ALL">Tất cả</option>
            <option value="NEW">Mới</option>
            <option value="PICKING">Đang soạn</option>
            <option value="READY_TO_SHIP">Chờ vận chuyển</option>
            <option value="IN_TRANSIT">Đang giao</option>
            <option value="DELIVERED">Đã giao</option>
            <option value="RETURNED">Hoàn trả</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>
      </div>

      {/* Table */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : orders.length === 0 ? (
        <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
          <PackageCheck className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy đơn xuất hàng nào</h3>
          <p className="text-sm text-shade-50">Thay đổi bộ lọc hoặc tạo một đơn mới để bắt đầu.</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-zinc-50 border-b border-hairline-light">
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Mã DO</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Đại lý</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Ngày lập</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Ngày giao (DK)</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {orders.map(order => (
                  <tr key={order.id} className="hover:bg-zinc-50 transition-colors">
                    <td className="px-6 py-4 text-xs font-bold">{order.do_number}</td>
                    <td className="px-6 py-4 text-xs font-semibold">{order.dealer_name}</td>
                    <td className="px-6 py-4 text-xs text-shade-50">{new Date(order.document_date).toLocaleDateString('vi-VN')}</td>
                    <td className="px-6 py-4 text-xs text-shade-50">{new Date(order.expected_delivery_date).toLocaleDateString('vi-VN')}</td>
                    <td className="px-6 py-4">{getStatusBadge(order.status)}</td>
                    <td className="px-6 py-4 text-right whitespace-nowrap">
                      <div className="flex gap-2 justify-end items-center">
                        {(hasRole(ROLES.PLANNER) || hasRole(ROLES.ADMIN)) && order.status === 'NEW' && (
                          <button
                            onClick={() => setCancelModal({ show: true, orderId: order.id, reason: '' })}
                            className="inline-flex items-center justify-center rounded-full border border-red-300 text-red-600 hover:bg-red-50 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
                          >
                            Hủy đơn
                          </button>
                        )}
                        {(hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN)) && order.status === 'NEW' && (
                          <button
                            onClick={() => navigate(`/outbound/delivery-orders/${order.id}`)}
                            className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light text-ink hover:bg-zinc-100 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
                          >
                            Bắt đầu soạn
                          </button>
                        )}
                        {(hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN)) && order.status === 'PICKING' && (
                          <button
                            onClick={() => navigate(`/outbound/qc/${order.id}`)}
                            className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light text-ink hover:bg-zinc-100 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
                          >
                            Kiểm QC
                          </button>
                        )}
                        {(hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) && order.status === 'PICKING' && Boolean(order.qc_completed_at) && (
                          <button
                            onClick={() => handleApproveInline(order.id)}
                            className="inline-flex items-center justify-center rounded-full bg-aloe-10 text-emerald-950 border border-emerald-300 hover:bg-emerald-100 px-3 py-1 text-xs font-bold whitespace-nowrap transition-colors duration-150"
                          >
                            Duyệt xuất kho
                          </button>
                        )}
                        <button
                          onClick={() => navigate(`/outbound/delivery-orders/${order.id}`)}
                          className="p-1.5 hover:bg-zinc-200 rounded-full text-shade-50 hover:text-ink transition-colors flex items-center justify-center"
                          title="Xem chi tiết"
                        >
                          <Eye className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Create DO Modal */}
      <Modal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title="Lập đơn xuất hàng (DO)"
        maxWidth="max-w-3xl"
      >
        <div className="flex flex-col gap-5">
          <CreditCheckBanner
            status={creditStatus}
            remainingCredit={selectedDealerObj?.id === 2 ? 15000000 : 250000000}
          />

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Đại lý nhận hàng *"
              type="select"
              value={formData.dealer_id}
              onChange={(e) => {
                setFormData({ ...formData, dealer_id: e.target.value });
                setSelectedDealerObj(dealers.find(d => d.id === Number(e.target.value)));
              }}
              options={[
                { value: '', label: '-- Chọn đại lý --' },
                ...dealers.map(d => ({ value: d.id, label: d.name })),
                { value: 4, label: 'Đại lý Test Khóa Công Nợ' },
              ]}
            />
            <Input
              label="Ngày giao dự kiến *"
              type="date"
              value={formData.expected_delivery_date}
              onChange={(e) => setFormData({ ...formData, expected_delivery_date: e.target.value })}
            />
            <div className="col-span-2">
              <Input
                label="Ghi chú"
                value={formData.notes}
                onChange={(e) => setFormData({ ...formData, notes: e.target.value })}
                placeholder="Ghi chú về đơn hàng..."
              />
            </div>
          </div>

          <div>
            <div className="flex justify-between items-center mb-3">
              <span className="text-xs font-semibold uppercase tracking-wider text-shade-60">Danh sách sản phẩm</span>
              <button type="button" onClick={addItemRow} className="text-xs font-semibold text-ink hover:underline flex items-center gap-1">
                <Plus className="w-3.5 h-3.5" /> Thêm sản phẩm
              </button>
            </div>
            <div className="border border-hairline-light rounded-lg overflow-hidden bg-canvas-light">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-zinc-50 border-b border-hairline-light">
                    <th className="px-4 py-3 font-bold text-shade-60 uppercase tracking-wider">Sản phẩm</th>
                    <th className="px-4 py-3 font-bold text-shade-60 uppercase tracking-wider w-28">Số lượng</th>
                    <th className="px-4 py-3 w-10"></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {formData.items.length === 0 && (
                    <tr><td colSpan="3" className="px-4 py-8 text-center text-shade-40 italic text-xs">Chưa có sản phẩm nào — nhấn Thêm sản phẩm</td></tr>
                  )}
                  {formData.items.map((item, idx) => (
                    <tr key={idx} className="hover:bg-zinc-50/50">
                      <td className="px-4 py-2.5">
                        <select
                          className="w-full bg-canvas-light text-ink text-sm px-3 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all"
                          value={item.product_id}
                          onChange={(e) => updateItemRow(idx, 'product_id', e.target.value)}
                        >
                          <option value="">-- Chọn sản phẩm --</option>
                          {products.map(p => <option key={p.id} value={p.id}>[{p.sku}] {p.name}</option>)}
                        </select>
                      </td>
                      <td className="px-4 py-2.5">
                        <input
                          type="number" min="1"
                          className="w-full bg-canvas-light text-ink text-sm px-3 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all"
                          value={item.requested_qty}
                          onChange={(e) => updateItemRow(idx, 'requested_qty', Number(e.target.value))}
                        />
                      </td>
                      <td className="px-4 py-2.5 text-center">
                        <button type="button" onClick={() => removeItemRow(idx)} className="p-1 hover:bg-zinc-100 rounded-full text-shade-40 hover:text-red-600 transition-colors">
                          <X className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
            <Button variant="outline-light" onClick={() => setShowCreateModal(false)}>Đóng</Button>
            <Button
              variant="primary"
              loading={submitting}
              disabled={isSubmitDisabled}
              onClick={handleCreateSubmit}
            >
              Tạo đơn xuất
            </Button>
          </div>
        </div>
      </Modal>

      {/* Cancel Modal */}
      <Modal
        isOpen={cancelModal.show}
        onClose={() => setCancelModal({ show: false, orderId: null, reason: '' })}
        title="Hủy lệnh xuất hàng"
      >
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2 text-red-600 text-sm font-semibold">
            <AlertTriangle className="w-4 h-4 shrink-0" /> Hành động này không thể hoàn tác.
          </div>
          <Input
            label="Lý do hủy *"
            value={cancelModal.reason}
            onChange={(e) => setCancelModal({ ...cancelModal, reason: e.target.value })}
            placeholder="Nhập lý do hủy đơn này..."
          />
          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
            <Button variant="outline-light" onClick={() => setCancelModal({ show: false, orderId: null, reason: '' })}>Đóng</Button>
            <Button
              onClick={handleCancelDO}
              disabled={!cancelModal.reason}
              className="bg-red-600 hover:bg-red-700 text-white focus:ring-red-500"
            >
              Xác nhận hủy
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
