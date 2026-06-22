import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Calendar, Clock, Eye, Loader2, Package, Plus, Search, Truck, User } from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import TripCapacityBar from '../../components/warehouse/TripCapacityBar';
import { ROLES } from '../../utils/constants';

const TRIP_STATUS_MAP = {
  PLANNED: { label: 'Lên kế hoạch', color: 'bg-zinc-100 text-zinc-800 border-zinc-200' },
  IN_TRANSIT: { label: 'Đang giao', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: 'Hoàn thành', color: 'bg-emerald-50 text-emerald-900 border-emerald-300' },
  CANCELLED: { label: 'Đã hủy', color: 'bg-red-50 text-red-700 border-red-200' },
};

const emptyForm = { vehicle_id: '', driver_id: '', planned_date: '', planned_start_at: '', planned_end_at: '', notes: '', delivery_orders: [] };

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
  const [vehicles, setVehicles] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [availableDOs, setAvailableDOs] = useState([]);
  const [formData, setFormData] = useState(emptyForm);
  const [selectedVehicleObj, setSelectedVehicleObj] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [detailTrip, setDetailTrip] = useState(null);

  useEffect(() => {
    fetchTrips();
  }, [activeWarehouse?.id, statusFilter]);

  useEffect(() => {
    fetchFleet();
  }, []);

  useEffect(() => {
    if (!routeId || !trips.length) return;
    const trip = trips.find((item) => Number(item.id) === Number(routeId));
    if (trip) {
      setDetailTrip(trip);
    }
  }, [routeId, trips]);

  const fetchTrips = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getTrips(activeWarehouse?.id, { status: statusFilter });
      setTrips(data);
    } catch (error) {
      addToast(error.message || 'Lỗi khi tải danh sách chuyến xe', 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchFleet = async () => {
    try {
      const [vehicleData, driverData] = await Promise.all([
        masterDataService.getVehicles(),
        masterDataService.getDrivers(),
      ]);
      setVehicles(vehicleData.filter((vehicle) => vehicle.is_active !== false));
      setDrivers(driverData.filter((driver) => driver.is_active !== false));
    } catch {
      addToast('Không thể tải danh sách xe hoặc tài xế', 'warning');
    }
  };

  const openCreateModal = async () => {
    try {
      const orders = await outboundService.getDeliveryOrders(activeWarehouse?.id, { status: 'WAREHOUSE_APPROVED' });
      setAvailableDOs(orders.map((order) => ({
        ...order,
        weight: Number(order.total_weight_kg || order.items?.reduce((sum, item) => sum + Number(item.requested_qty || 0) * 5, 0) || 50),
      })));
      setShowCreateModal(true);
    } catch (error) {
      addToast(error.message || 'Không thể tải đơn chờ vận chuyển', 'error');
    }
  };

  const toggleDOSelection = (order) => {
    setFormData((prev) => {
      const isSelected = prev.delivery_orders.some((item) => item.id === order.id);
      return {
        ...prev,
        delivery_orders: isSelected
          ? prev.delivery_orders.filter((item) => item.id !== order.id)
          : [...prev.delivery_orders, order],
      };
    });
  };

  const moveStopUp = (index) => {
    if (index === 0) return;
    const orders = [...formData.delivery_orders];
    [orders[index - 1], orders[index]] = [orders[index], orders[index - 1]];
    setFormData((prev) => ({ ...prev, delivery_orders: orders }));
  };

  const handleCreateSubmit = async () => {
    if (!formData.delivery_orders.length) {
      addToast('Vui lòng chọn ít nhất 1 đơn xuất hàng', 'error');
      return;
    }
    const totalWeight = formData.delivery_orders.reduce((sum, order) => sum + Number(order.weight || 0), 0);
    if (selectedVehicleObj && totalWeight > Number(selectedVehicleObj.max_weight_kg || selectedVehicleObj.maxWeightKg || 0)) {
      addToast('Tổng khối lượng vượt quá tải trọng của xe', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const driver = drivers.find((item) => Number(item.id) === Number(formData.driver_id));
      await outboundService.createTrip({
        ...formData,
        vehicle_plate: selectedVehicleObj?.plate_number || selectedVehicleObj?.plate || selectedVehicleObj?.license_plate,
        driver_name: driver?.full_name || driver?.name,
        warehouse_id: activeWarehouse?.id,
        total_weight_kg: totalWeight,
      });
      addToast('Tạo chuyến xe thành công', 'success');
      setShowCreateModal(false);
      setFormData(emptyForm);
      setSelectedVehicleObj(null);
      fetchTrips();
    } catch (error) {
      addToast(error.message || 'Lỗi khi tạo chuyến xe', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const openDetailModal = (trip) => {
    setDetailTrip(trip);
    if (routeId !== String(trip.id)) {
      navigate(`/outbound/trips/${trip.id}`);
    }
  };

  const closeDetailModal = () => {
    setDetailTrip(null);
    if (routeId) navigate('/outbound/trips');
  };

  const filteredTrips = useMemo(() => {
    return trips.filter((trip) => {
      const query = search.toLowerCase();
      return !search
        || trip.trip_number?.toLowerCase().includes(query)
        || trip.vehicle_plate?.toLowerCase().includes(query)
        || trip.driver_name?.toLowerCase().includes(query);
    });
  }, [search, trips]);

  const currentWeight = formData.delivery_orders.reduce((sum, order) => sum + Number(order.weight || 0), 0);
  const maxWeight = Number(selectedVehicleObj?.max_weight_kg || selectedVehicleObj?.maxWeightKg || 0);
  const isOverweight = selectedVehicleObj && currentWeight > maxWeight;
  const isSubmitDisabled = !formData.vehicle_id || !formData.driver_id || !formData.planned_date || !formData.delivery_orders.length || isOverweight || submitting;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Giao hàng</span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">Quản lý chuyến xe</h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Lập chuyến và điều phối giao hàng từ kho <span className="font-semibold text-ink">{activeWarehouse?.name} ({activeWarehouse?.code})</span>.
          </p>
        </div>
        {hasRole(ROLES.DISPATCHER) && (
          <button onClick={openCreateModal} className="btn-pill btn-pill-primary flex items-center gap-2">
            <Plus className="w-4 h-4" />
            <span>Lập chuyến mới</span>
          </button>
        )}
      </div>

      <div className="bg-white rounded-lg border border-hairline-light p-4 shadow-sm flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
          <input
            type="text"
            placeholder="Tìm mã chuyến, xe, tài xế..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            className="w-full text-input pl-10"
          />
        </div>
        <div className="flex items-center gap-2 w-full md:w-auto justify-end">
          <span className="text-xs font-semibold text-shade-50">Trạng thái:</span>
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)} className="text-input text-xs py-1.5">
            <option value="ALL">Tất cả</option>
            <option value="PLANNED">Lên kế hoạch</option>
            <option value="IN_TRANSIT">Đang giao</option>
            <option value="COMPLETED">Hoàn thành</option>
            <option value="CANCELLED">Đã hủy</option>
          </select>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : filteredTrips.length === 0 ? (
        <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
          <Truck className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy chuyến xe nào</h3>
          <p className="text-sm text-shade-50">Thử đổi bộ lọc hoặc tạo chuyến mới để bắt đầu.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredTrips.map((trip) => (
            <div key={trip.id} className="bg-white rounded-lg border border-hairline-light shadow-sm hover:shadow-md transition-shadow card-premium overflow-hidden">
              <div className="p-4 border-b border-hairline-light bg-zinc-50 flex justify-between items-center">
                <span className="text-xs font-bold text-ink">{trip.trip_number}</span>
                {getTripStatusBadge(trip.status)}
              </div>
              <div className="p-4 space-y-2">
                <p className="flex items-center gap-2 text-xs"><Truck className="w-3.5 h-3.5 text-shade-40" /><span className="text-shade-50">Xe:</span><span className="font-semibold text-ink">{trip.vehicle_plate || '-'}</span></p>
                <p className="flex items-center gap-2 text-xs"><User className="w-3.5 h-3.5 text-shade-40" /><span className="text-shade-50">Tài xế:</span><span className="font-semibold text-ink">{trip.driver_name || trip.driver_id}</span></p>
                <p className="flex items-center gap-2 text-xs"><Calendar className="w-3.5 h-3.5 text-shade-40" /><span className="text-shade-50">Ngày giao:</span><span className="font-semibold text-ink">{trip.planned_date ? new Date(trip.planned_date).toLocaleDateString('vi-VN') : '-'}</span></p>
                {(trip.planned_start_at || trip.planned_end_at) && (
                  <p className="flex items-center gap-2 text-xs"><Clock className="w-3.5 h-3.5 text-shade-40" /><span className="text-shade-50">Khung giờ:</span><span className="font-semibold text-ink">{trip.planned_start_at ? new Date(trip.planned_start_at).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '?'} – {trip.planned_end_at ? new Date(trip.planned_end_at).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '?'}</span></p>
                )}
                <p className="text-xs"><span className="text-shade-50">Tổng KL:</span> <span className="font-semibold text-ink">{trip.total_weight_kg} kg</span></p>
              </div>
              <div className="p-4 border-t border-hairline-light flex gap-2">
                <button onClick={() => openDetailModal(trip)} className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-full border border-hairline-light bg-white text-ink hover:bg-zinc-50 px-3 py-1.5 text-xs font-semibold transition-colors">
                  <Eye className="w-3.5 h-3.5" /> Chi tiết
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <Modal isOpen={!!detailTrip} onClose={closeDetailModal} title={detailTrip?.trip_number ?? 'Chi tiết chuyến xe'} maxWidth="max-w-2xl">
        {detailTrip && (
          <div className="flex flex-col gap-6">
            <div className="grid grid-cols-2 gap-3">
              {[
                { label: 'Biển số xe', value: detailTrip.vehicle_plate || '-', icon: <Truck className="w-3.5 h-3.5" /> },
                { label: 'Tài xế', value: detailTrip.driver_name || detailTrip.driver_id, icon: <User className="w-3.5 h-3.5" /> },
                { label: 'Ngày giao', value: detailTrip.planned_date ? new Date(detailTrip.planned_date).toLocaleDateString('vi-VN') : '-', icon: <Calendar className="w-3.5 h-3.5" /> },
                { label: 'Khung giờ', value: (detailTrip.planned_start_at || detailTrip.planned_end_at) ? `${detailTrip.planned_start_at ? new Date(detailTrip.planned_start_at).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '?'} – ${detailTrip.planned_end_at ? new Date(detailTrip.planned_end_at).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }) : '?'}` : 'Chưa thiết lập', icon: <Clock className="w-3.5 h-3.5" /> },
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
                  {detailTrip.delivery_orders.map((stop, index) => (
                    <div key={`${stop.do_id}-${index}`} className="rounded-lg border p-4 flex gap-3 bg-canvas-cream border-hairline-light">
                      <div className="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 bg-ink text-white">
                        {index + 1}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-bold text-ink">{stop.dealer_name || stop.do_number}</p>
                        <p className="text-xs text-shade-40 mt-0.5 font-mono">{stop.do_number}</p>
                      </div>
                      <div className="shrink-0 text-xs font-semibold text-shade-50">{stop.raw_status || stop.status || '-'}</div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="rounded-lg border border-amber-200 bg-amber-50 p-4 text-xs text-amber-800">
              Frontend này đã đồng bộ với backend hiện tại: dispatcher chỉ lập/xem trip, không xuất bến. Driver sẽ xác nhận depart trong màn hình driver.
            </div>

            <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
              <Button variant="outline-light" onClick={closeDetailModal}>Đóng</Button>
            </div>
          </div>
        )}
      </Modal>

      <Modal isOpen={showCreateModal} onClose={() => setShowCreateModal(false)} title="Lập chuyến xe giao hàng" maxWidth="max-w-4xl">
        <div className="flex flex-col md:flex-row gap-6">
          <div className="flex-1 flex flex-col gap-5">
            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Phương tiện *"
                type="select"
                value={formData.vehicle_id}
                onChange={(event) => {
                  setFormData((prev) => ({ ...prev, vehicle_id: event.target.value }));
                  setSelectedVehicleObj(vehicles.find((vehicle) => Number(vehicle.id) === Number(event.target.value)));
                }}
                options={[
                  { value: '', label: '-- Chọn xe --' },
                  ...vehicles.map((vehicle) => ({
                    value: vehicle.id,
                    label: `${vehicle.plate_number || vehicle.plate || vehicle.license_plate} (Tải: ${vehicle.max_weight_kg || vehicle.maxWeightKg || 0}kg)`,
                  })),
                ]}
              />
              <Input
                label="Tài xế *"
                type="select"
                value={formData.driver_id}
                onChange={(event) => setFormData((prev) => ({ ...prev, driver_id: event.target.value }))}
                options={[
                  { value: '', label: '-- Chọn tài xế --' },
                  ...drivers.map((driver) => ({ value: driver.id, label: driver.full_name || driver.name })),
                ]}
              />
              <div className="col-span-2">
                <Input
                  label="Ngày thực hiện *"
                  type="date"
                  value={formData.planned_date}
                  onChange={(event) => setFormData((prev) => ({ ...prev, planned_date: event.target.value }))}
                />
              </div>
              <Input
                label="Giờ xuất phát dự kiến"
                type="datetime-local"
                value={formData.planned_start_at}
                onChange={(event) => setFormData((prev) => ({ ...prev, planned_start_at: event.target.value }))}
              />
              <Input
                label="Giờ kết thúc dự kiến"
                type="datetime-local"
                value={formData.planned_end_at}
                onChange={(event) => setFormData((prev) => ({ ...prev, planned_end_at: event.target.value }))}
              />
            </div>

            <div>
              <span className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-2">Chọn đơn xuất hàng chờ vận chuyển</span>
              <div className="border border-hairline-light rounded-lg overflow-hidden bg-canvas-light max-h-[260px] overflow-y-auto">
                {!availableDOs.length ? (
                  <p className="p-6 text-center text-shade-40 text-xs italic">Không có đơn hàng nào chờ vận chuyển</p>
                ) : (
                  availableDOs.map((order) => {
                    const isSelected = formData.delivery_orders.some((item) => item.id === order.id);
                    return (
                      <button
                        type="button"
                        key={order.id}
                        className={`w-full text-left px-4 py-3 border-b border-hairline-light flex items-center justify-between transition-colors ${
                          isSelected ? 'bg-emerald-50 border-l-2 border-l-emerald-500' : 'hover:bg-zinc-50'
                        }`}
                        onClick={() => toggleDOSelection(order)}
                      >
                        <div>
                          <p className="text-xs font-bold text-ink">{order.do_number}</p>
                          <p className="text-[11px] text-shade-40">{order.dealer_name}</p>
                        </div>
                        <div className="text-[11px] font-medium text-shade-50">{order.weight}kg</div>
                      </button>
                    );
                  })
                )}
              </div>
            </div>
          </div>

          <div className="w-full md:w-[300px] bg-canvas-cream rounded-lg border border-hairline-light p-4 flex flex-col gap-4">
            <span className="text-xs font-bold uppercase tracking-widest text-shade-40">Lộ trình & tải trọng</span>
            {selectedVehicleObj ? (
              <TripCapacityBar currentWeight={currentWeight} maxWeight={maxWeight} />
            ) : (
              <p className="text-xs text-shade-40 italic">Chọn xe để xem tải trọng.</p>
            )}
            <div className="flex-1">
              <span className="text-xs font-semibold text-shade-50 block mb-2">Thứ tự giao hàng</span>
              {!formData.delivery_orders.length ? (
                <div className="p-4 border-2 border-dashed border-shade-30 rounded-lg text-center text-shade-40 text-xs">Chưa chọn đơn hàng nào</div>
              ) : (
                <div className="space-y-2">
                  {formData.delivery_orders.map((order, index) => (
                    <div key={order.id} className="bg-canvas-light rounded-lg border border-hairline-light p-2.5 flex items-center gap-2">
                      <div className="w-6 h-6 rounded-full bg-ink text-white flex items-center justify-center text-[10px] font-bold shrink-0">{index + 1}</div>
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-bold text-ink truncate">{order.do_number}</p>
                        <p className="text-[11px] text-shade-40 truncate">{order.dealer_name}</p>
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
          <Button variant="primary" loading={submitting} disabled={isSubmitDisabled} onClick={handleCreateSubmit}>Tạo chuyến xe</Button>
        </div>
      </Modal>
    </div>
  );
}
