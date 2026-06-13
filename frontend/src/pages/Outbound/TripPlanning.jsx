import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Truck, Plus, Play, CheckCircle2, Eye, X, Loader2, Calendar, User, MapPin, Package, Search
} from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import TripCapacityBar from '../../components/warehouse/TripCapacityBar';
import Modal from '../../components/common/Modal';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { ROLES } from '../../utils/constants';

const TRIP_STATUS_MAP = {
  PLANNED:    { label: 'Lên kế hoạch', color: 'bg-zinc-100 text-zinc-800 border-zinc-200' },
  IN_TRANSIT: { label: 'Đang giao',   color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED:  { label: 'Hoàn thành',  color: 'bg-aloe-10 text-emerald-900 border-emerald-300' },
  CANCELLED:  { label: 'Đã hủy',      color: 'bg-red-50 text-red-700 border-red-200' },
};

const getTripStatusBadge = (status) => {
  const base = 'text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase tracking-wider whitespace-nowrap';
  const { label, color } = TRIP_STATUS_MAP[status] ?? { label: status, color: 'bg-zinc-100 text-zinc-800 border-zinc-200' };
  return <span className={`${base} ${color}`}>{label}</span>;
};

export default function TripPlanning() {
  const navigate = useNavigate();
  const { id: routeId } = useParams();
  const { addToast } = useUiStore();
  const { hasRole, activeWarehouse } = useAuthStore();

  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [search, setSearch] = useState('');

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [vehicles] = useState([
    { id: 1, plate: '15C-123.45', max_weight_kg: 500 },
    { id: 2, plate: '29C-987.65', max_weight_kg: 1000 },
    { id: 3, plate: '51C-555.55', max_weight_kg: 2000 },
  ]);
  const [drivers] = useState([
    { id: 6, name: 'Nguyễn Văn Tài Xế' },
    { id: 7, name: 'Trần Văn Lái Xe' },
  ]);
  const [availableDOs, setAvailableDOs] = useState([]);
  const [formData, setFormData] = useState({ vehicle_id: '', driver_id: '', planned_date: '', delivery_orders: [] });
  const [selectedVehicleObj, setSelectedVehicleObj] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Detail modal state
  const [detailTrip, setDetailTrip] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  useEffect(() => {
    fetchTrips();
  }, [activeWarehouse?.id, statusFilter]);

  // When route has an id param, open the detail modal automatically
  useEffect(() => {
    if (routeId) {
      openDetailModal(Number(routeId));
    }
  }, [routeId]);

  const fetchTrips = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getTrips(activeWarehouse?.id, { status: statusFilter });
      setTrips(data);
    } catch {
      addToast('Lỗi khi tải danh sách chuyến xe', 'error');
    } finally {
      setLoading(false);
    }
  };

  const openCreateModal = async () => {
    try {
      const dos = await outboundService.getDeliveryOrders(activeWarehouse.id, { status: 'READY_TO_SHIP' });
      setAvailableDOs(dos.map(d => ({ ...d, weight: 50 })));
    } catch {
      // fail silently
    }
    setShowCreateModal(true);
  };

  const toggleDOSelection = (doItem) => {
    setFormData(prev => {
      const isSelected = prev.delivery_orders.some(d => d.id === doItem.id);
      return {
        ...prev,
        delivery_orders: isSelected
          ? prev.delivery_orders.filter(d => d.id !== doItem.id)
          : [...prev.delivery_orders, doItem],
      };
    });
  };

  const moveStopUp = (index) => {
    if (index === 0) return;
    const newDOs = [...formData.delivery_orders];
    [newDOs[index - 1], newDOs[index]] = [newDOs[index], newDOs[index - 1]];
    setFormData({ ...formData, delivery_orders: newDOs });
  };

  const handleCreateSubmit = async () => {
    if (formData.delivery_orders.length === 0) { addToast('Vui lòng chọn ít nhất 1 đơn xuất hàng', 'error'); return; }
    const totalWeight = formData.delivery_orders.reduce((sum, d) => sum + d.weight, 0);
    if (selectedVehicleObj && totalWeight > selectedVehicleObj.max_weight_kg) {
      addToast('Tổng khối lượng vượt quá tải trọng của xe!', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await outboundService.createTrip({
        ...formData,
        vehicle_plate: selectedVehicleObj?.plate,
        driver_name: drivers.find(d => d.id === Number(formData.driver_id))?.name,
        warehouse_id: activeWarehouse.id,
        total_weight_kg: totalWeight,
      });
      addToast('Tạo chuyến xe thành công', 'success');
      setShowCreateModal(false);
      setFormData({ vehicle_id: '', driver_id: '', planned_date: '', delivery_orders: [] });
      setSelectedVehicleObj(null);
      fetchTrips();
    } catch (error) {
      addToast(error.message || 'Lỗi khi tạo chuyến', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDepart = async (tripId) => {
    try {
      await outboundService.departTrip(tripId);
      addToast('Chuyến xe đã xuất phát', 'success');
      fetchTrips();
    } catch {
      addToast('Lỗi khi xuất phát', 'error');
    }
  };

  const openDetailModal = async (tripId) => {
    setDetailLoading(true);
    setDetailTrip(null);
    try {
      const data = await outboundService.getTripById(tripId);
      setDetailTrip(data);
    } catch {
      addToast('Không thể tải chi tiết chuyến xe', 'error');
      navigate('/outbound/trips');
    } finally {
      setDetailLoading(false);
    }
  };

  const closeDetailModal = () => {
    setDetailTrip(null);
    if (routeId) navigate('/outbound/trips');
  };

  const currentWeight = formData.delivery_orders.reduce((sum, d) => sum + d.weight, 0);
  const isOverweight = selectedVehicleObj && currentWeight > selectedVehicleObj.max_weight_kg;
  const isSubmitDisabled = !formData.vehicle_id || !formData.driver_id || !formData.planned_date || formData.delivery_orders.length === 0 || isOverweight || submitting;

  const filteredTrips = trips.filter(t => {
    if (!search) return true;
    const q = search.toLowerCase();
    return t.trip_number?.toLowerCase().includes(q) || t.vehicle_plate?.toLowerCase().includes(q) || t.driver_name?.toLowerCase().includes(q);
  });

  return (
    <div className="flex flex-col gap-6">
      {/* Page Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Vận hành / Giao hàng
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Quản lý chuyến xe
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Lập chuyến và điều phối giao hàng từ kho <span className="font-semibold text-ink">{activeWarehouse?.name} ({activeWarehouse?.code})</span>.
          </p>
        </div>
        {(hasRole(ROLES.DISPATCHER) || hasRole(ROLES.ADMIN)) && (
          <button onClick={openCreateModal} className="btn-pill btn-pill-primary flex items-center gap-2">
            <Plus className="w-4 h-4" />
            <span>Lập chuyến mới</span>
          </button>
        )}
      </div>

      {/* Filters */}
      <div className="bg-white rounded-lg border border-hairline-light p-4 shadow-sm flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
          <input
            type="text"
            placeholder="Tìm mã chuyến, xe, tài xế..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full text-input pl-10"
          />
        </div>
        <div className="flex items-center gap-2 w-full md:w-auto justify-end">
          <span className="text-xs font-semibold text-shade-50">Trạng thái:</span>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="text-input text-xs py-1.5">
            <option value="ALL">Tất cả</option>
            <option value="PLANNED">Lên kế hoạch</option>
            <option value="IN_TRANSIT">Đang giao</option>
            <option value="COMPLETED">Hoàn thành</option>
          </select>
        </div>
      </div>

      {/* Trip Cards */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : filteredTrips.length === 0 ? (
        <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
          <Truck className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy chuyến xe nào</h3>
          <p className="text-sm text-shade-50">Thay đổi bộ lọc hoặc tạo chuyến mới để bắt đầu.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredTrips.map(trip => (
            <div key={trip.id} className="bg-white rounded-lg border border-hairline-light shadow-sm hover:shadow-md transition-shadow card-premium overflow-hidden">
              <div className="p-4 border-b border-hairline-light bg-zinc-50 flex justify-between items-center">
                <span className="text-xs font-bold text-ink">{trip.trip_number}</span>
                {getTripStatusBadge(trip.status)}
              </div>
              <div className="p-4 space-y-2">
                <div className="flex items-center gap-2 text-xs">
                  <Truck className="w-3.5 h-3.5 text-shade-40 shrink-0" />
                  <span className="text-shade-50">Xe:</span>
                  <span className="font-semibold text-ink">{trip.vehicle_plate}</span>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <User className="w-3.5 h-3.5 text-shade-40 shrink-0" />
                  <span className="text-shade-50">Tài xế:</span>
                  <span className="font-semibold text-ink">{trip.driver_name}</span>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <Calendar className="w-3.5 h-3.5 text-shade-40 shrink-0" />
                  <span className="text-shade-50">Ngày giao:</span>
                  <span className="font-semibold text-ink">{new Date(trip.planned_date).toLocaleDateString('vi-VN')}</span>
                </div>
                <div className="flex items-center gap-2 text-xs">
                  <span className="text-shade-50">Tổng KL:</span>
                  <span className="font-semibold text-ink">{trip.total_weight_kg} kg</span>
                </div>
              </div>
              <div className="p-4 border-t border-hairline-light flex gap-2">
                <button
                  onClick={() => openDetailModal(trip.id)}
                  className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-full border border-hairline-light bg-white text-ink hover:bg-zinc-50 px-3 py-1.5 text-xs font-semibold transition-colors"
                >
                  <Eye className="w-3.5 h-3.5" /> Chi tiết
                </button>
                {trip.status === 'PLANNED' && (hasRole(ROLES.DISPATCHER) || hasRole(ROLES.ADMIN)) && (
                  <button
                    onClick={() => handleDepart(trip.id)}
                    className="flex-1 inline-flex items-center justify-center gap-1.5 btn-pill btn-pill-primary px-3 py-1.5 text-xs"
                  >
                    <Play className="w-3.5 h-3.5" /> Xuất bến
                  </button>
                )}
                {trip.status === 'IN_TRANSIT' && (hasRole(ROLES.DRIVER) || hasRole(ROLES.ADMIN)) && (
                  <button
                    onClick={() => navigate(`/outbound/driver/trips/${trip.id}`)}
                    className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-full bg-indigo-50 border border-indigo-200 text-indigo-700 hover:bg-indigo-100 px-3 py-1.5 text-xs font-semibold transition-colors"
                  >
                    <Truck className="w-3.5 h-3.5" /> Xem lộ trình
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Trip Detail Modal */}
      <Modal
        isOpen={!!(detailLoading || detailTrip)}
        onClose={closeDetailModal}
        title={detailTrip?.trip_number ?? 'Chi tiết chuyến xe'}
        maxWidth="max-w-2xl"
      >
        {detailLoading ? (
          <div className="flex items-center justify-center p-16">
            <Loader2 className="w-7 h-7 animate-spin text-shade-50" />
          </div>
        ) : detailTrip && (
          <div className="flex flex-col gap-6">
            <div className="grid grid-cols-2 gap-3">
              {[
                { label: 'Biển số xe', value: detailTrip.vehicle_plate, icon: <Truck className="w-3.5 h-3.5" /> },
                { label: 'Tài xế', value: detailTrip.driver_name, icon: <User className="w-3.5 h-3.5" /> },
                { label: 'Ngày giao', value: new Date(detailTrip.planned_date).toLocaleDateString('vi-VN'), icon: <Calendar className="w-3.5 h-3.5" /> },
                { label: 'Tổng khối lượng', value: `${detailTrip.total_weight_kg} kg`, icon: <Package className="w-3.5 h-3.5" /> },
              ].map(({ label, value, icon }) => (
                <div key={label} className="bg-canvas-cream rounded-lg border border-hairline-light p-3.5">
                  <p className="text-[10px] font-bold uppercase tracking-wider text-shade-40 mb-1 flex items-center gap-1">{icon}{label}</p>
                  <p className="text-sm font-semibold text-ink">{value}</p>
                </div>
              ))}
            </div>

            <div>
              <h4 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3">
                Lộ trình giao hàng ({detailTrip.delivery_orders?.length ?? 0} điểm)
              </h4>
              {!detailTrip.delivery_orders?.length ? (
                <p className="text-xs text-shade-40 italic text-center py-4">Chưa có điểm giao nào</p>
              ) : (
                <div className="space-y-3">
                  {detailTrip.delivery_orders.map((stop, idx) => {
                    const isDelivered = stop.delivery_status === 'DELIVERED';
                    const isFailed = stop.delivery_status === 'FAILED';
                    return (
                      <div key={stop.id} className={`rounded-lg border p-4 flex gap-3 ${isDelivered ? 'bg-aloe-10 border-emerald-300' : isFailed ? 'bg-red-50 border-red-200' : 'bg-canvas-cream border-hairline-light'}`}>
                        <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${isDelivered ? 'bg-emerald-600 text-white' : isFailed ? 'bg-red-500 text-white' : 'bg-ink text-white'}`}>
                          {idx + 1}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-bold text-ink">{stop.dealer_name}</p>
                          {stop.dealer_address && (
                            <p className="text-xs text-shade-40 mt-0.5 flex items-center gap-1">
                              <MapPin className="w-3 h-3 shrink-0" /> {stop.dealer_address}
                            </p>
                          )}
                          <p className="text-xs text-shade-50 mt-1 font-mono">{stop.do_number}</p>
                        </div>
                        <div className="shrink-0">
                          {getTripStatusBadge(isDelivered ? 'COMPLETED' : isFailed ? 'CANCELLED' : 'PLANNED')}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
              <Button variant="outline-light" onClick={closeDetailModal}>Đóng</Button>
              {detailTrip.status === 'PLANNED' && (hasRole(ROLES.DISPATCHER) || hasRole(ROLES.ADMIN)) && (
                <Button
                  variant="primary"
                  icon={Play}
                  onClick={async () => { await handleDepart(detailTrip.id); closeDetailModal(); }}
                >
                  Xuất bến
                </Button>
              )}
            </div>
          </div>
        )}
      </Modal>

      {/* Create Trip Modal */}
      <Modal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title="Lập chuyến xe giao hàng"
        maxWidth="max-w-4xl"
      >
        <div className="flex flex-col md:flex-row gap-6">
          {/* Left: Form + DO selection */}
          <div className="flex-1 flex flex-col gap-5">
            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Phương tiện *"
                type="select"
                value={formData.vehicle_id}
                onChange={(e) => {
                  setFormData({ ...formData, vehicle_id: e.target.value });
                  setSelectedVehicleObj(vehicles.find(v => v.id === Number(e.target.value)));
                }}
                options={[
                  { value: '', label: '-- Chọn xe --' },
                  ...vehicles.map(v => ({ value: v.id, label: `${v.plate} (Tải: ${v.max_weight_kg}kg)` })),
                ]}
              />
              <Input
                label="Tài xế *"
                type="select"
                value={formData.driver_id}
                onChange={(e) => setFormData({ ...formData, driver_id: e.target.value })}
                options={[
                  { value: '', label: '-- Chọn tài xế --' },
                  ...drivers.map(d => ({ value: d.id, label: d.name })),
                ]}
              />
              <div className="col-span-2">
                <Input
                  label="Ngày thực hiện *"
                  type="date"
                  value={formData.planned_date}
                  onChange={(e) => setFormData({ ...formData, planned_date: e.target.value })}
                />
              </div>
            </div>

            <div>
              <span className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-2">Chọn đơn xuất hàng (Chờ vận chuyển)</span>
              <div className="border border-hairline-light rounded-lg overflow-hidden bg-canvas-light max-h-[260px] overflow-y-auto">
                {availableDOs.length === 0 ? (
                  <p className="p-6 text-center text-shade-40 text-xs italic">Không có đơn hàng nào chờ vận chuyển</p>
                ) : (
                  availableDOs.map(doItem => {
                    const isSelected = formData.delivery_orders.some(d => d.id === doItem.id);
                    return (
                      <div
                        key={doItem.id}
                        className={`px-4 py-3 border-b border-hairline-light flex items-center justify-between cursor-pointer transition-colors ${isSelected ? 'bg-aloe-10 border-l-2 border-l-emerald-500' : 'hover:bg-zinc-50'}`}
                        onClick={() => toggleDOSelection(doItem)}
                      >
                        <div>
                          <p className="text-xs font-bold text-ink">{doItem.do_number}</p>
                          <p className="text-[11px] text-shade-40">{doItem.dealer_name}</p>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="text-[11px] font-medium text-shade-50">{doItem.weight}kg</span>
                          <div className={`w-4 h-4 rounded border-2 flex items-center justify-center ${isSelected ? 'bg-emerald-600 border-emerald-600' : 'border-shade-30'}`}>
                            {isSelected && <CheckCircle2 className="w-3 h-3 text-white" />}
                          </div>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>

          {/* Right: Capacity + Stop Order */}
          <div className="w-full md:w-[300px] bg-canvas-cream rounded-lg border border-hairline-light p-4 flex flex-col gap-4">
            <span className="text-xs font-bold uppercase tracking-widest text-shade-40">Lộ trình & Tải trọng</span>
            {selectedVehicleObj ? (
              <TripCapacityBar currentWeight={currentWeight} maxWeight={selectedVehicleObj.max_weight_kg} />
            ) : (
              <p className="text-xs text-shade-40 italic">Chọn xe để xem tải trọng.</p>
            )}
            <div className="flex-1">
              <span className="text-xs font-semibold text-shade-50 block mb-2">Thứ tự giao hàng</span>
              {formData.delivery_orders.length === 0 ? (
                <div className="p-4 border-2 border-dashed border-shade-30 rounded-lg text-center text-shade-40 text-xs">
                  Chưa chọn đơn hàng nào
                </div>
              ) : (
                <div className="space-y-2">
                  {formData.delivery_orders.map((d, index) => (
                    <div key={d.id} className="bg-canvas-light rounded-lg border border-hairline-light p-2.5 flex items-center gap-2">
                      <div className="w-6 h-6 rounded-full bg-ink text-white flex items-center justify-center text-[10px] font-bold shrink-0">{index + 1}</div>
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-bold text-ink truncate">{d.do_number}</p>
                        <p className="text-[11px] text-shade-40 truncate">{d.dealer_name}</p>
                      </div>
                      <button onClick={() => moveStopUp(index)} disabled={index === 0} className="p-1 hover:bg-shade-30 rounded text-shade-40 disabled:opacity-30 font-bold text-sm" title="Chuyển lên">↑</button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="flex justify-end gap-3 border-t border-hairline-light pt-4 mt-4">
          <Button variant="outline-light" onClick={() => setShowCreateModal(false)}>Đóng</Button>
          <Button variant="primary" loading={submitting} disabled={isSubmitDisabled} onClick={handleCreateSubmit}>
            Tạo chuyến xe
          </Button>
        </div>
      </Modal>
    </div>
  );
}
