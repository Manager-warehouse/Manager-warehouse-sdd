import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Calendar, Eye, Loader2, MapPin, Package, Plus, Search, Trash2, Truck, User } from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import TripCapacityBar from '../../components/warehouse/TripCapacityBar';
import { ROLES } from '../../utils/constants';

const TRIP_STATUS_MAP = {
  PLANNED: { label: 'Lên kế hoạch', color: 'bg-canvas-cream text-shade-70 border-hairline-light' },
  IN_TRANSIT: { label: 'Đang giao', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: 'Hoàn thành', color: 'bg-success-50 text-success-900 border-success-300' },
  CANCELLED: { label: 'Đã hủy', color: 'bg-danger-50 text-danger-700 border-danger-200' },
};

const DELIVERY_ORDER_STATUS_LABELS = {
  WAREHOUSE_APPROVED: 'Đã duyệt xuất kho',
  IN_TRANSIT: 'Đang giao',
  COMPLETED: 'Hoàn thành',
  RETURNED: 'Chờ hoàn về kho',
  DELIVERY_FAILED: 'Giao hàng thất bại',
};

const emptyForm = { vehicle_id: '', driver_id: '', planned_start_at: '', planned_end_at: '', notes: '', delivery_orders: [] };

const todayDateValue = () => new Date().toISOString().slice(0, 10);

const nowDateTimeValue = () => {
  const now = new Date();
  const offsetDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return offsetDate.toISOString().slice(0, 16);
};

export const getSplitAllocationItems = (order) => (order?.items || []).flatMap((item) => {
  const byBatch = new Map();
  (item.allocations || []).forEach((allocation) => {
    const batchId = allocation.batch_id || allocation.batchId;
    const quantity = Number(allocation.qc_pass_qty || allocation.qcPassQty || 0);
    if (!batchId || quantity <= 0) return;
    const key = `${item.id}:${batchId}`;
    const current = byBatch.get(key);
    byBatch.set(key, {
      key,
      do_item_id: item.id,
      product_id: item.product_id,
      product_name: item.product_name,
      sku: item.sku,
      batch_id: batchId,
      batch_code: allocation.batch_code || allocation.batchCode || `#${batchId}`,
      quantity: Number(((current?.quantity || 0) + quantity).toFixed(2)),
    });
  });
  if (byBatch.size) return [...byBatch.values()];

  const quantity = Number(item.qc_pass_qty || item.requested_qty || 0);
  if (!item.batch_id || quantity <= 0) return [];
  return [{
    key: `${item.id}:${item.batch_id}`,
    do_item_id: item.id,
    product_id: item.product_id,
    product_name: item.product_name,
    sku: item.sku,
    batch_id: item.batch_id,
    batch_code: item.batch_code || `#${item.batch_id}`,
    quantity,
  }];
});

const buildDefaultSplitRows = (order, vehicles, drivers) => {
  const allocationItems = getSplitAllocationItems(order);
  const selectedVehicles = vehicles.slice(0, 2);
  const selectedDrivers = drivers.slice(0, 2);
  return selectedVehicles.map((vehicle, legIndex) => ({
    vehicle_id: vehicle?.id || '',
    driver_id: selectedDrivers[legIndex]?.id || '',
    item_quantities: allocationItems.reduce((map, item) => {
      const firstQuantity = Number((item.quantity / 2).toFixed(2));
      map[item.key] = legIndex === 0 ? firstQuantity : Number((item.quantity - firstQuantity).toFixed(2));
      return map;
    }, {}),
  }));
};

export const buildSplitPlanPayload = ({ order, rows, plannedStartAt, plannedEndAt }) => {
  const items = getSplitAllocationItems(order);
  return {
    do_id: order.id,
    lead_driver_id: rows[0].driver_id,
    planned_start_at: plannedStartAt,
    planned_end_at: plannedEndAt,
    legs: rows.map((row) => ({
      vehicle_id: row.vehicle_id,
      driver_id: row.driver_id,
      items: items.map((item) => ({
        do_item_id: item.do_item_id,
        product_id: item.product_id,
        batch_id: item.batch_id,
        quantity: Number(row.item_quantities[item.key] || 0),
      })).filter((item) => item.quantity > 0),
    })),
  };
};

const getTripStatusBadge = (status) => {
  const { label, color } = TRIP_STATUS_MAP[status] ?? { label: status, color: 'bg-canvas-cream text-shade-70 border-hairline-light' };
  return <Badge size="sm" colorClassName={color}>{label}</Badge>;
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
  const [splitRows, setSplitRows] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [detailTrip, setDetailTrip] = useState(null);

  const [isCancelling, setIsCancelling] = useState(false);
  const [cancelReason, setCancelReason] = useState('');
  const [cancellingSubmit, setCancellingSubmit] = useState(false);

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

  useEffect(() => {
    if (formData.delivery_orders.length !== 1 || splitRows.length || vehicles.length < 2 || drivers.length < 2) return;
    setSplitRows(buildDefaultSplitRows(formData.delivery_orders[0], vehicles, drivers));
  }, [drivers, formData.delivery_orders, splitRows.length, vehicles]);

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

  const updateSplitRow = (index, field, value) => {
    setSplitRows((prev) => prev.map((row, rowIndex) => (
      rowIndex === index ? { ...row, [field]: value } : row
    )));
  };

  const updateSplitQuantity = (rowIndex, itemId, value) => {
    setSplitRows((prev) => prev.map((row, index) => (
      index === rowIndex
        ? { ...row, item_quantities: { ...row.item_quantities, [itemId]: Number(value || 0) } }
        : row
    )));
  };

  const addSplitRow = () => {
    const order = formData.delivery_orders[0];
    setSplitRows((prev) => [
      ...prev,
      {
        vehicle_id: '',
        driver_id: '',
        item_quantities: getSplitAllocationItems(order)
          .reduce((map, item) => ({ ...map, [item.key]: 0 }), {}),
      },
    ]);
  };

  const removeSplitRow = (index) => {
    setSplitRows((prev) => prev.filter((_, rowIndex) => rowIndex !== index));
  };

  const handleCreateSubmit = async () => {
    if (!formData.delivery_orders.length) {
      addToast('Vui lòng chọn ít nhất 1 đơn xuất hàng', 'error');
      return;
    }
    if (formData.planned_start_at < nowDateTimeValue()) {
      addToast('Thời gian bắt đầu chuyến không được ở quá khứ', 'error');
      return;
    }
    if (formData.planned_end_at <= formData.planned_start_at) {
      addToast('Thời gian kết thúc dự kiến phải sau thời gian bắt đầu', 'error');
      return;
    }
    const expiredOrder = formData.delivery_orders.find((order) => order.expected_delivery_date && order.expected_delivery_date < todayDateValue());
    if (expiredOrder) {
      addToast(`Đơn ${expiredOrder.do_number} đã quá ngày giao dự kiến, không được lập chuyến`, 'error');
      return;
    }
    const afterDeadlineOrder = formData.delivery_orders.find((order) => order.expected_delivery_date
      && formData.planned_start_at.slice(0, 10) > order.expected_delivery_date);
    if (afterDeadlineOrder) {
      addToast(`Chuyến của đơn ${afterDeadlineOrder.do_number} phải bắt đầu không muộn hơn ngày giao dự kiến`, 'error');
      return;
    }
    const totalWeight = formData.delivery_orders.reduce((sum, order) => sum + Number(order.weight || 0), 0);
    if (selectedVehicleObj && totalWeight > Number(selectedVehicleObj.max_weight_kg || selectedVehicleObj.maxWeightKg || 0)) {
      addToast('Đơn vượt tải trọng 1 xe, hãy tạo kế hoạch nhiều xe', 'error');
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

  const handleCreateSplitSubmit = async () => {
    const order = formData.delivery_orders[0];
    const items = getSplitAllocationItems(order);
    if (!order || splitRows.length < 2) {
      addToast('Cần ít nhất 2 xe để chia một đơn xuất hàng', 'error');
      return;
    }
    if (splitRows.some((row) => !row.vehicle_id || !row.driver_id)) {
      addToast('Vui lòng chọn đủ phương tiện và tài xế cho từng xe', 'error');
      return;
    }
    const vehicleIds = new Set(splitRows.map((row) => String(row.vehicle_id)));
    const driverIds = new Set(splitRows.map((row) => String(row.driver_id)));
    if (vehicleIds.size !== splitRows.length || driverIds.size !== splitRows.length) {
      addToast('Phương tiện và tài xế trong kế hoạch không được trùng nhau', 'error');
      return;
    }
    const incompleteItem = items.find((item) => {
      const assigned = splitRows.reduce((sum, row) => sum + Number(row.item_quantities[item.key] || 0), 0);
      return Math.abs(assigned - item.quantity) > 0.001;
    });
    if (incompleteItem) {
      addToast('Tổng số lượng phân bổ phải bằng số lượng cần giao của từng sản phẩm và lô', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await outboundService.createSplitDeliveryPlan(buildSplitPlanPayload({
        order,
        rows: splitRows,
        plannedStartAt: formData.planned_start_at,
        plannedEndAt: formData.planned_end_at,
      }));
      addToast('Đã tạo kế hoạch giao hàng bằng nhiều xe', 'success');
      setShowCreateModal(false);
      setFormData(emptyForm);
      setSelectedVehicleObj(null);
      setSplitRows([]);
      fetchTrips();
    } catch (error) {
      addToast(error.message || 'Lỗi khi tạo kế hoạch giao hàng bằng nhiều xe', 'error');
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
    setIsCancelling(false);
    setCancelReason('');
    if (routeId) navigate('/outbound/trips');
  };

  const handleCancelTrip = async () => {
    if (!cancelReason.trim()) {
      addToast('Vui lòng nhập lý do hủy chuyến', 'error');
      return;
    }
    setCancellingSubmit(true);
    try {
      await outboundService.cancelTrip(detailTrip.id, cancelReason.trim());
      addToast('Đã hủy chuyến xe thành công', 'success');
      closeDetailModal();
      fetchTrips();
    } catch (error) {
      addToast(error.message || 'Lỗi khi hủy chuyến xe', 'error');
    } finally {
      setCancellingSubmit(false);
    }
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
  const splitOrder = formData.delivery_orders.length === 1 ? formData.delivery_orders[0] : null;
  const canCreateSplitPlan = Boolean(isOverweight && splitOrder);
  const isSubmitDisabled = !formData.vehicle_id || !formData.driver_id || !formData.planned_start_at || !formData.planned_end_at || !formData.delivery_orders.length || isOverweight || submitting;

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
          <Button onClick={openCreateModal} variant="primary" icon={Plus}>
            Lập chuyến mới
          </Button>
        )}
      </div>

      <div className="bg-canvas-light rounded-lg border border-hairline-light p-4 shadow-level-3 flex flex-col md:flex-row gap-4 items-center justify-between">
        <div className="w-full md:w-80">
          <Input
            type="text"
            leftIcon={Search}
            placeholder="Tìm mã chuyến, xe, tài xế..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />
        </div>
        <Input
          type="select"
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          options={[
            { value: 'ALL', label: 'Tất cả' },
            { value: 'PLANNED', label: 'Lên kế hoạch' },
            { value: 'IN_TRANSIT', label: 'Đang giao' },
            { value: 'COMPLETED', label: 'Hoàn thành' },
            { value: 'CANCELLED', label: 'Đã hủy' },
          ]}
        />
      </div>

      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : filteredTrips.length === 0 ? (
        <div className="bg-canvas-light rounded-lg border border-hairline-light p-12 text-center shadow-level-3">
          <Truck className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy chuyến xe nào</h3>
          <p className="text-sm text-shade-50">Thử đổi bộ lọc hoặc tạo chuyến mới để bắt đầu.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredTrips.map((trip) => (
            <div key={trip.id} className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 hover:shadow-md transition-shadow overflow-hidden flex flex-col h-full">
              <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center">
                <span className="text-xs font-bold text-ink">{trip.trip_number}</span>
                {getTripStatusBadge(trip.status)}
              </div>
              <div className="p-4 flex flex-col gap-2 flex-1">
                <p className="flex items-center gap-2 text-xs"><Truck className="w-3.5 h-3.5 text-shade-40" /><span className="text-shade-50">Xe:</span><span className="font-semibold text-ink">{trip.vehicle_plate || '-'}</span></p>
                <p className="flex items-center gap-2 text-xs"><User className="w-3.5 h-3.5 text-shade-40" /><span className="text-shade-50">Tài xế:</span><span className="font-semibold text-ink">{trip.driver_name || trip.driver_id}</span></p>
                <p className="flex items-center gap-2 text-xs"><Calendar className="w-3.5 h-3.5 text-shade-40" /><span className="text-shade-50">TG dự kiến:</span><span className="font-semibold text-ink">{trip.planned_start_at ? new Date(trip.planned_start_at).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' }) : '-'} - {trip.planned_end_at ? new Date(trip.planned_end_at).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' }) : '-'}</span></p>
                <p className="text-xs"><span className="text-shade-50">Tổng KL:</span> <span className="font-semibold text-ink">{trip.total_weight_kg} kg</span></p>
              </div>
              <div className="p-4 border-t border-hairline-light flex gap-2">
                <button onClick={() => openDetailModal(trip)} className="flex-1 inline-flex items-center justify-center gap-1.5 rounded-full border border-hairline-light bg-canvas-light text-ink hover:bg-canvas-cream px-3 py-1.5 text-xs font-semibold transition-colors">
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
            <div className="flex items-center justify-between">
              <span className="text-xs font-bold uppercase tracking-widest text-shade-40">Trạng thái chuyến xe</span>
              {getTripStatusBadge(detailTrip.status)}
            </div>
            <div className="grid grid-cols-2 gap-3">
              {[
                { label: 'Biển số xe', value: detailTrip.vehicle_plate || '-', icon: <Truck className="w-3.5 h-3.5" /> },
                { label: 'Loại xe', value: detailTrip.vehicle_type || '-', icon: <Truck className="w-3.5 h-3.5" /> },
                { label: 'Tài xế', value: detailTrip.driver_name || detailTrip.driver_id, icon: <User className="w-3.5 h-3.5" /> },
                { label: 'SĐT tài xế', value: detailTrip.driver_phone || '-', icon: <User className="w-3.5 h-3.5" /> },
                { label: 'GPLX', value: detailTrip.driver_license_number || '-', icon: <User className="w-3.5 h-3.5" /> },
                { label: 'TG Dự kiến', value: detailTrip.planned_start_at ? `${new Date(detailTrip.planned_start_at).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })} - ${new Date(detailTrip.planned_end_at).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit' })}` : '-', icon: <Calendar className="w-3.5 h-3.5" /> },
                { label: 'Tổng khối lượng', value: `${detailTrip.total_weight_kg} kg`, icon: <Package className="w-3.5 h-3.5" /> },
                { label: 'Tải trọng xe', value: detailTrip.vehicle_max_weight_kg ? `${detailTrip.vehicle_max_weight_kg} kg` : '-', icon: <Package className="w-3.5 h-3.5" /> },
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
                <div className="flex flex-col gap-3">
                  {detailTrip.delivery_orders.map((stop, index) => (
                    <div key={`${stop.do_id}-${index}`} className="rounded-lg border p-4 flex gap-3 bg-canvas-cream border-hairline-light">
                      <div className="w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 bg-ink text-white">
                        {index + 1}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-bold text-ink">{stop.dealer_name || stop.do_number}</p>
                        <p className="text-xs text-shade-40 mt-0.5 font-mono">{stop.do_number}</p>
                        {stop.dealer_address && (
                          <p className="text-xs text-shade-50 mt-1 flex items-start gap-1">
                            <MapPin className="w-3.5 h-3.5 shrink-0 mt-0.5" />
                            <span>{stop.dealer_address}</span>
                          </p>
                        )}
                      </div>
                      <div className="shrink-0 text-xs font-semibold text-shade-50">
                        {DELIVERY_ORDER_STATUS_LABELS[stop.raw_status || stop.status] || stop.raw_status || stop.status || '-'}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="rounded-lg border border-warning-200 bg-warning-50 p-4 text-xs text-warning-800">
              Frontend này đã đồng bộ với backend hiện tại: dispatcher chỉ lập/xem trip, không xuất bến. Driver sẽ xác nhận depart trong màn hình driver.
            </div>

            {isCancelling && (
              <div className="bg-danger-50 border border-danger-200 rounded-lg p-4 flex flex-col gap-3">
                <span className="text-xs font-bold text-danger-700 uppercase tracking-wider block">Xác nhận hủy chuyến xe</span>
                <textarea
                  className="text-input text-xs h-20 resize-none w-full border border-hairline-light rounded p-2"
                  placeholder="Nhập lý do hủy chuyến (bắt buộc)..."
                  value={cancelReason}
                  onChange={(e) => setCancelReason(e.target.value)}
                  required
                />
                <div className="flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => { setIsCancelling(false); setCancelReason(''); }}
                    className="px-4 py-2 text-xs font-semibold rounded-pill border border-hairline-light bg-canvas-light text-ink hover:bg-canvas-cream transition-all duration-150 active:scale-95"
                  >
                    Hủy bỏ
                  </button>
                  <button
                    type="button"
                    onClick={handleCancelTrip}
                    disabled={!cancelReason.trim() || cancellingSubmit}
                    className="px-4 py-2 text-xs font-semibold rounded-pill bg-danger-600 text-white hover:bg-danger-700 active:scale-95 transition-all duration-150 disabled:opacity-50"
                  >
                    {cancellingSubmit ? 'Đang hủy...' : 'Xác nhận hủy'}
                  </button>
                </div>
              </div>
            )}

            <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
              {hasRole(ROLES.DISPATCHER) && detailTrip.status === 'PLANNED' && !isCancelling && (
                <button
                  type="button"
                  onClick={() => setIsCancelling(true)}
                  className="mr-auto px-4 py-2 rounded-pill bg-danger-50 text-danger-700 border border-danger-200 hover:bg-danger-100 active:scale-95 transition-all text-xs font-semibold"
                >
                  Hủy chuyến xe
                </button>
              )}
              <Button variant="outline-light" onClick={closeDetailModal}>Đóng</Button>
            </div>
          </div>
        )}
      </Modal>

      <Modal isOpen={showCreateModal} onClose={() => setShowCreateModal(false)} title="Lập chuyến xe giao hàng" maxWidth="max-w-6xl">
        <div className="flex flex-col lg:flex-row gap-6">
          <div className="flex-1 flex flex-col gap-5">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {!canCreateSplitPlan && (
                <>
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
                </>
              )}
              {canCreateSplitPlan && (
                <div className="sm:col-span-2 flex items-center justify-between gap-3 border border-info-200 bg-info-50 px-4 py-3 rounded-lg">
                  <div className="flex items-center gap-2 min-w-0">
                    <Truck className="w-4 h-4 text-info-700 shrink-0" />
                    <span className="text-xs font-semibold text-info-800">Kế hoạch giao hàng bằng nhiều xe</span>
                  </div>
                  <button
                    type="button"
                    className="text-xs font-semibold text-info-800 underline underline-offset-2 shrink-0"
                    onClick={() => {
                      setSelectedVehicleObj(null);
                      setSplitRows([]);
                      setFormData((prev) => ({ ...prev, vehicle_id: '', driver_id: '' }));
                    }}
                  >
                    Chọn lại chuyến một xe
                  </button>
                </div>
              )}
              <div className="sm:col-span-2 grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Bắt đầu dự kiến *"
                  type="datetime-local"
                  min={nowDateTimeValue()}
                  value={formData.planned_start_at}
                  onChange={(event) => setFormData((prev) => ({ ...prev, planned_start_at: event.target.value }))}
                />
                <Input
                  label="Kết thúc dự kiến *"
                  type="datetime-local"
                  min={formData.planned_start_at || nowDateTimeValue()}
                  value={formData.planned_end_at}
                  onChange={(event) => setFormData((prev) => ({ ...prev, planned_end_at: event.target.value }))}
                />
              </div>
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
                          isSelected ? 'bg-success-50 border-l-2 border-l-success-500' : 'hover:bg-canvas-cream'
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

          <div className="w-full lg:w-[430px] lg:shrink-0 border-t lg:border-t-0 lg:border-l border-hairline-light pt-5 lg:pt-0 lg:pl-6 flex flex-col gap-4">
            <span className="text-xs font-bold uppercase tracking-widest text-shade-40">Lộ trình & tải trọng</span>
            {selectedVehicleObj ? (
              <TripCapacityBar currentWeight={currentWeight} maxWeight={maxWeight} />
            ) : (
              <p className="text-xs text-shade-40 italic">Chọn xe để xem tải trọng.</p>
            )}
            {canCreateSplitPlan && (
              <div className="flex flex-col gap-4">
                <div className="border-l-4 border-warning-500 bg-warning-50 px-4 py-3">
                  <p className="text-sm font-bold text-warning-900">Đơn xuất hàng vượt tải trọng một xe</p>
                  <p className="text-xs text-warning-800 mt-1">Phân bổ toàn bộ hàng trong một kế hoạch; các xe xuất phát cùng nhau.</p>
                </div>
                <div className="flex flex-col gap-3">
                  {splitRows.map((row, rowIndex) => (
                    <div key={rowIndex} className="rounded-lg border border-hairline-light bg-canvas-light p-3 flex flex-col gap-3 shadow-level-1">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-bold text-ink">Xe {rowIndex + 1}</span>
                          {rowIndex === 0 && (
                            <span className="text-[10px] font-semibold text-info-800 bg-info-50 border border-info-200 px-2 py-1 rounded-pill">
                              Xe trưởng đoàn
                            </span>
                          )}
                        </div>
                        {splitRows.length > 2 && (
                          <button
                            type="button"
                            className="w-8 h-8 inline-flex items-center justify-center text-danger-600 hover:bg-danger-50 rounded focus:outline-none focus:ring-2 focus:ring-danger-200"
                            onClick={() => removeSplitRow(rowIndex)}
                            title={`Xóa xe ${rowIndex + 1}`}
                            aria-label={`Xóa xe ${rowIndex + 1}`}
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                      <label className="flex flex-col gap-1.5">
                        <span className="text-[11px] font-semibold text-shade-60">Phương tiện</span>
                        <select
                          className="text-input text-xs border border-hairline-light rounded p-2 bg-canvas-light"
                          value={row.vehicle_id}
                          onChange={(event) => updateSplitRow(rowIndex, 'vehicle_id', event.target.value)}
                        >
                          <option value="">-- Chọn xe --</option>
                          {vehicles.map((vehicle) => (
                            <option key={vehicle.id} value={vehicle.id}>
                              {vehicle.plate_number || vehicle.plate || vehicle.license_plate} ({vehicle.max_weight_kg || vehicle.maxWeightKg || 0}kg)
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className="flex flex-col gap-1.5">
                        <span className="text-[11px] font-semibold text-shade-60">
                          {rowIndex === 0 ? 'Tài xế trưởng' : 'Tài xế'}
                        </span>
                        <select
                          className="text-input text-xs border border-hairline-light rounded p-2 bg-canvas-light"
                          value={row.driver_id}
                          onChange={(event) => updateSplitRow(rowIndex, 'driver_id', event.target.value)}
                        >
                          <option value="">-- Chọn tài xế --</option>
                          {drivers.map((driver) => (
                            <option key={driver.id} value={driver.id}>{driver.full_name || driver.name}</option>
                          ))}
                        </select>
                      </label>
                      <span className="text-[11px] font-semibold text-shade-60">Số lượng giao theo sản phẩm và lô</span>
                      {getSplitAllocationItems(splitOrder).map((item) => (
                        <label key={item.key} className="text-[11px] text-shade-60 flex items-center gap-2">
                          <span className="flex-1 truncate">
                            {item.product_name || item.sku || `Item ${item.do_item_id}`} (Lô {item.batch_code})
                          </span>
                          <input
                            type="number"
                            min="0"
                            max={item.quantity}
                            step="0.01"
                            className="w-20 text-input text-xs border border-hairline-light rounded p-1 text-right"
                            value={row.item_quantities[item.key] ?? 0}
                            onChange={(event) => updateSplitQuantity(rowIndex, item.key, event.target.value)}
                          />
                        </label>
                      ))}
                    </div>
                  ))}
                </div>
                <Button variant="outline-light" icon={Plus} className="self-start" onClick={addSplitRow}>
                  Thêm xe
                </Button>
              </div>
            )}
            <div className="flex-1">
              <span className="text-xs font-semibold text-shade-50 block mb-2">Thứ tự giao hàng</span>
              {!formData.delivery_orders.length ? (
                <div className="p-4 border-2 border-dashed border-shade-30 rounded-lg text-center text-shade-40 text-xs">Chưa chọn đơn hàng nào</div>
              ) : (
                <div className="flex flex-col gap-2">
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
          {canCreateSplitPlan && (
            <Button variant="primary" loading={submitting} disabled={!formData.planned_start_at || !formData.planned_end_at || submitting} onClick={handleCreateSplitSubmit}>
              Tạo kế hoạch nhiều xe
            </Button>
          )}
          {!canCreateSplitPlan && (
            <Button variant="primary" loading={submitting} disabled={isSubmitDisabled} onClick={handleCreateSubmit}>
              Tạo chuyến xe
            </Button>
          )}
        </div>
      </Modal>
    </div>
  );
}
