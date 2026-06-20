import apiClient, { useMock } from './api.client';

const KEYS = {
  DELIVERY_ORDERS: 'wms_db_delivery_orders',
  DO_ITEMS: 'wms_db_do_items',
  TRIPS: 'wms_db_trips',
  TRIP_DOS: 'wms_db_trip_dos',
};

const INITIAL_DELIVERY_ORDERS = [
  {
    id: 1,
    do_number: 'DO-20260612-001',
    dealer_id: 1,
    dealer_name: 'Đại lý Hoàng Phát',
    warehouse_id: 1,
    status: 'NEW',
    raw_status: 'NEW',
    expected_delivery_date: '2026-06-15',
    document_date: '2026-06-12',
    notes: 'Giao gấp trước 10h sáng',
    created_at: '2026-06-12T07:00:00.000Z',
    updated_at: '2026-06-12T07:00:00.000Z',
  },
  {
    id: 2,
    do_number: 'DO-20260612-002',
    dealer_id: 2,
    dealer_name: 'Đại lý Trần Gia',
    warehouse_id: 1,
    status: 'PICKING',
    raw_status: 'WAITING_PICKING',
    expected_delivery_date: '2026-06-14',
    document_date: '2026-06-12',
    qc_completed_at: null,
    notes: 'Gọi điện trước khi giao',
    created_at: '2026-06-12T08:00:00.000Z',
    updated_at: '2026-06-12T08:00:00.000Z',
  },
  {
    id: 3,
    do_number: 'DO-20260612-003',
    dealer_id: 3,
    dealer_name: 'Đại lý Minh Trí',
    warehouse_id: 1,
    status: 'READY_TO_SHIP',
    raw_status: 'WAREHOUSE_APPROVED',
    expected_delivery_date: '2026-06-13',
    document_date: '2026-06-11',
    qc_completed_at: '2026-06-11T15:00:00.000Z',
    notes: '',
    created_at: '2026-06-11T09:00:00.000Z',
    updated_at: '2026-06-11T15:00:00.000Z',
  },
];

const INITIAL_DO_ITEMS = [
  {
    id: 1,
    do_id: 1,
    product_id: 1,
    product_name: 'Nồi inox Sunhouse 24cm',
    sku: 'NOI-SH-24',
    requested_qty: 5,
    reserved_qty: 5,
    planned_qty: 5,
    picked_qty: 0,
    issued_qty: 0,
    qc_pass_qty: 0,
    qc_fail_qty: 0,
    unit_price: 450000,
    serial_number: '',
    allocations: [],
  },
  {
    id: 2,
    do_id: 2,
    product_id: 2,
    product_name: 'Chảo chống dính HappyCook 28cm',
    sku: 'CHAO-HC-28',
    requested_qty: 10,
    reserved_qty: 10,
    planned_qty: 10,
    picked_qty: 0,
    issued_qty: 0,
    qc_pass_qty: 0,
    qc_fail_qty: 0,
    unit_price: 320000,
    serial_number: '',
    allocations: [
      {
        allocation_id: 21,
        inventory_id: 21,
        batch_id: 3,
        location_id: 1,
        zone_id: 1,
        planned_qty: 10,
        picked_qty: 0,
      },
    ],
  },
  {
    id: 3,
    do_id: 3,
    product_id: 1,
    product_name: 'Nồi inox Sunhouse 24cm',
    sku: 'NOI-SH-24',
    requested_qty: 2,
    reserved_qty: 2,
    planned_qty: 2,
    picked_qty: 2,
    issued_qty: 2,
    qc_pass_qty: 2,
    qc_fail_qty: 0,
    unit_price: 450000,
    serial_number: '',
    qc_result: 'PASSED',
    qc_failure_reason: null,
    allocations: [
      {
        allocation_id: 31,
        inventory_id: 31,
        batch_id: 1,
        location_id: 2,
        zone_id: 1,
        planned_qty: 2,
        picked_qty: 2,
      },
    ],
  },
];

const INITIAL_TRIPS = [
  {
    id: 1,
    trip_number: 'TRP-20260612-001',
    warehouse_id: 1,
    vehicle_id: 1,
    vehicle_plate: '15C-123.45',
    driver_id: 13,
    driver_name: 'Nguyễn Văn Tài Xế',
    planned_date: '2026-06-13',
    status: 'PLANNED',
    total_weight_kg: 150,
    created_at: '2026-06-12T08:00:00.000Z',
  },
];

const INITIAL_TRIP_DOS = [
  {
    id: 1,
    trip_id: 1,
    do_id: 3,
    do_number: 'DO-20260612-003',
    stop_order: 1,
    dealer_name: 'Đại lý Minh Trí',
    dealer_address: '123 Lê Lợi, Hải Phòng',
    delivery_status: 'READY_TO_SHIP',
    raw_status: 'WAREHOUSE_APPROVED',
    current_attempt: null,
    failure_reason: null,
  },
];

export const getDb = (key, initial) => {
  const data = localStorage.getItem(key);
  if (!data) {
    localStorage.setItem(key, JSON.stringify(initial));
    return initial;
  }
  try {
    return JSON.parse(data);
  } catch {
    localStorage.setItem(key, JSON.stringify(initial));
    return initial;
  }
};

export const saveDb = (key, data) => {
  localStorage.setItem(key, JSON.stringify(data));
};

export const addAuditLog = (action, entityType, entityId, details) => {
  const logs = JSON.parse(localStorage.getItem('wms_audit_logs')) || [];
  const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { fullName: 'System' };
  const newLog = {
    id: logs.length + 1,
    actorName: currentUser.fullName,
    action,
    entityType,
    entityId,
    details,
    createdAt: new Date().toISOString(),
  };
  localStorage.setItem('wms_audit_logs', JSON.stringify([newLog, ...logs]));
};

const SEED_VERSION = 'v004-ui-api-r1';
if (typeof window !== 'undefined' && window.localStorage) {
  if (localStorage.getItem('wms_outbound_seed') !== SEED_VERSION) {
    localStorage.setItem(KEYS.DELIVERY_ORDERS, JSON.stringify(INITIAL_DELIVERY_ORDERS));
    localStorage.setItem(KEYS.DO_ITEMS, JSON.stringify(INITIAL_DO_ITEMS));
    localStorage.setItem(KEYS.TRIPS, JSON.stringify(INITIAL_TRIPS));
    localStorage.setItem(KEYS.TRIP_DOS, JSON.stringify(INITIAL_TRIP_DOS));
    localStorage.setItem('wms_outbound_seed', SEED_VERSION);
  }
}

const asArray = (data) => {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  return [];
};

const value = (object, camel, snake, fallback = null) => {
  if (object?.[camel] !== undefined && object?.[camel] !== null) return object[camel];
  if (object?.[snake] !== undefined && object?.[snake] !== null) return object[snake];
  return fallback;
};

const normalizeDoStatus = (status) => {
  const map = {
    NEW: 'NEW',
    WAITING_PICKING: 'PICKING',
    PICKING: 'PICKING',
    QC_PENDING_APPROVAL: 'PICKING',
    QC_COMPLETED: 'PICKING',
    WAREHOUSE_APPROVED: 'READY_TO_SHIP',
    READY_TO_SHIP: 'READY_TO_SHIP',
    IN_TRANSIT: 'IN_TRANSIT',
    COMPLETED: 'DELIVERED',
    DELIVERED: 'DELIVERED',
    RETURNED: 'RETURNED',
    REJECTED: 'PICKING',
    CANCELLED: 'CANCELLED',
  };
  return map[status] || status;
};

const normalizeDeliveryStatus = (status, attempt) => {
  if (attempt?.status === 'DELIVERED') return 'DELIVERED';
  if (attempt?.status === 'FAILED') return 'FAILED';
  return normalizeDoStatus(status);
};

const normalizeAllocation = (allocation = {}) => ({
  allocation_id: value(allocation, 'allocationId', 'allocation_id'),
  inventory_id: value(allocation, 'inventoryId', 'inventory_id'),
  batch_id: value(allocation, 'batchId', 'batch_id'),
  location_id: value(allocation, 'locationId', 'location_id'),
  zone_id: value(allocation, 'zoneId', 'zone_id'),
  planned_qty: Number(value(allocation, 'plannedQty', 'planned_qty', 0)),
  picked_qty: Number(value(allocation, 'pickedQty', 'picked_qty', 0)),
});

const normalizeDoItem = (item = {}) => ({
  id: value(item, 'id', 'id'),
  do_id: value(item, 'deliveryOrderId', 'do_id'),
  product_id: value(item, 'productId', 'product_id'),
  product_name: value(item, 'productName', 'product_name', `Sản phẩm #${value(item, 'productId', 'product_id', '')}`),
  sku: value(item, 'sku', 'sku', ''),
  requested_qty: Number(value(item, 'requestedQty', 'requested_qty', 0)),
  reserved_qty: Number(value(item, 'reservedQty', 'reserved_qty', 0)),
  planned_qty: Number(value(item, 'plannedQty', 'planned_qty', value(item, 'requestedQty', 'requested_qty', 0))),
  picked_qty: Number(value(item, 'pickedQty', 'picked_qty', 0)),
  issued_qty: Number(value(item, 'issuedQty', 'issued_qty', value(item, 'pickedQty', 'picked_qty', 0))),
  qc_pass_qty: Number(value(item, 'qcPassQty', 'qc_pass_qty', 0)),
  qc_fail_qty: Number(value(item, 'qcFailQty', 'qc_fail_qty', 0)),
  unit_price: Number(value(item, 'unitPrice', 'unit_price', 0)),
  qc_result: value(item, 'qcResult', 'qc_result'),
  qc_failure_reason: value(item, 'qcFailReason', 'qc_failure_reason'),
  allocations: asArray(value(item, 'allocations', 'allocations', [])).map(normalizeAllocation),
});

const normalizeDeliveryOrder = (order = {}) => {
  const rawStatus = value(order, 'status', 'status');
  const items = asArray(value(order, 'items', 'items', [])).map((item) => ({
    ...normalizeDoItem(item),
    do_id: value(order, 'id', 'id'),
  }));
  return {
    id: value(order, 'id', 'id'),
    do_number: value(order, 'doNumber', 'do_number'),
    dealer_id: value(order, 'dealerId', 'dealer_id'),
    dealer_name: value(order, 'dealerName', 'dealer_name', `Đại lý #${value(order, 'dealerId', 'dealer_id', '')}`),
    warehouse_id: value(order, 'warehouseId', 'warehouse_id'),
    raw_status: rawStatus,
    status: normalizeDoStatus(rawStatus),
    expected_delivery_date: value(order, 'expectedDeliveryDate', 'expected_delivery_date'),
    document_date: value(order, 'documentDate', 'document_date'),
    cancel_reason: value(order, 'cancelReason', 'cancel_reason') || value(order, 'rejectionReason', 'rejection_reason'),
    qc_completed_at: ['QC_PENDING_APPROVAL', 'QC_COMPLETED', 'WAREHOUSE_APPROVED'].includes(rawStatus)
      ? value(order, 'updatedAt', 'updated_at')
      : value(order, 'qcCompletedAt', 'qc_completed_at'),
    notes: value(order, 'notes', 'notes', ''),
    items,
    created_at: value(order, 'createdAt', 'created_at'),
    updated_at: value(order, 'updatedAt', 'updated_at'),
  };
};

const normalizeTripStop = (stop = {}) => {
  const attempt = value(stop, 'currentAttempt', 'current_attempt');
  const rawStatus = value(stop, 'status', 'delivery_status', value(stop, 'rawStatus', 'raw_status'));
  return {
    id: value(stop, 'id', 'id', value(stop, 'doId', 'do_id')),
    do_id: value(stop, 'doId', 'do_id'),
    do_number: value(stop, 'doNumber', 'do_number'),
    stop_order: value(stop, 'stopOrder', 'stop_order'),
    dealer_name: value(stop, 'dealerName', 'dealer_name', `Điểm giao #${value(stop, 'stopOrder', 'stop_order', '')}`),
    dealer_address: value(stop, 'dealerAddress', 'dealer_address', ''),
    raw_status: rawStatus,
    delivery_status: normalizeDeliveryStatus(rawStatus, attempt),
    current_attempt: attempt ? {
      delivery_id: value(attempt, 'deliveryId', 'delivery_id'),
      status: value(attempt, 'status', 'status'),
      pod_image_url: value(attempt, 'podImageUrl', 'pod_image_url'),
      pod_signature_url: value(attempt, 'podSignatureUrl', 'pod_signature_url'),
      failure_reason: value(attempt, 'failureReason', 'failure_reason'),
      delivered_at: value(attempt, 'deliveredAt', 'delivered_at'),
    } : null,
    failure_reason: value(attempt, 'failureReason', 'failure_reason') || value(stop, 'failureReason', 'failure_reason'),
  };
};

const normalizeTrip = (trip = {}) => ({
  id: value(trip, 'tripId', 'id', value(trip, 'id', 'id')),
  trip_number: value(trip, 'tripNumber', 'trip_number'),
  warehouse_id: value(trip, 'warehouseId', 'warehouse_id'),
  vehicle_id: value(trip, 'vehicleId', 'vehicle_id'),
  vehicle_plate: value(trip, 'vehiclePlate', 'vehicle_plate', value(trip, 'plateNumber', 'plate_number', '')),
  driver_id: value(trip, 'driverId', 'driver_id'),
  driver_name: value(trip, 'driverName', 'driver_name', ''),
  planned_date: value(trip, 'plannedDate', 'planned_date', value(trip, 'createdAt', 'created_at')),
  status: value(trip, 'status', 'status'),
  total_weight_kg: Number(value(trip, 'totalWeightKg', 'total_weight_kg', 0)),
  delivery_orders: asArray(value(trip, 'deliveryOrders', 'delivery_orders', [])).map(normalizeTripStop),
  created_at: value(trip, 'createdAt', 'created_at'),
});

const toDeliveryOrderCreatePayload = (data) => ({
  dealerId: Number(data.dealer_id),
  warehouseId: Number(data.warehouse_id),
  type: data.type || 'SALE',
  expectedDeliveryDate: data.expected_delivery_date || null,
  documentDate: data.document_date || new Date().toISOString().slice(0, 10),
  notes: data.notes || '',
  items: data.items.map((item) => ({
    productId: Number(item.product_id),
    requestedQty: Number(item.requested_qty),
    unitPrice: Number(item.unit_price || 0),
  })),
});

const toTripCreatePayload = (data) => ({
  warehouseId: Number(data.warehouse_id),
  vehicleId: Number(data.vehicle_id),
  driverId: Number(data.driver_id),
  plannedDate: data.planned_date,
  notes: data.notes || '',
  deliveryOrders: data.delivery_orders.map((order, index) => ({
    doId: Number(order.id || order.do_id),
    stopOrder: index + 1,
  })),
});

const buildPickingPlanPayload = (items) => {
  const allocations = items.flatMap((item) =>
    (item.allocations || []).map((allocation) => ({
      doItemId: Number(item.id),
      inventoryId: Number(allocation.inventory_id),
      batchId: Number(allocation.batch_id),
      locationId: Number(allocation.location_id),
      zoneId: Number(allocation.zone_id),
      plannedQty: Number(allocation.planned_qty || item.requested_qty),
    })),
  );
  if (!allocations.length) {
    throw new Error('Thiếu allocation để lưu kế hoạch lấy hàng. Vui lòng lập picking plan từ dữ liệu tồn kho trước.');
  }
  return { allocations, returnToBinRecords: [] };
};

const buildPickQcPayload = (items, qcRows = null) => {
  const rowsByItem = new Map((qcRows || []).map((row) => [Number(row.id), row]));
  const results = items.flatMap((item) => {
    const uiRow = rowsByItem.get(Number(item.id));
    const failReason = uiRow?.reason || item.qc_failure_reason || '';
    const isFailed = uiRow?.result === 'FAILED';
    return (item.allocations || []).map((allocation) => {
      const pickedQty = Number(uiRow?.picked_qty ?? uiRow?.issued_qty ?? item.issued_qty ?? allocation.planned_qty);
      return {
        doItemId: Number(item.id),
        allocationId: Number(allocation.allocation_id),
        batchId: Number(allocation.batch_id),
        locationId: Number(allocation.location_id),
        zoneId: Number(allocation.zone_id),
        pickedQty,
        qcPassQty: isFailed ? 0 : pickedQty,
        qcFailQty: isFailed ? pickedQty : 0,
        qcFailReason: isFailed ? failReason : null,
        notes: uiRow?.notes || '',
      };
    });
  });
  if (!results.length) {
    throw new Error('Thiếu allocation để gửi kết quả pick/QC.');
  }
  return {
    idempotencyKey: `pick-qc-${Date.now()}`,
    results,
  };
};

const mockDelay = (ms = 250) => new Promise((resolve) => setTimeout(resolve, ms));

export const outboundService = {
  getDeliveryOrders: async (warehouseId, filters = {}) => {
    if (useMock) {
      await mockDelay();
      let orders = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const wid = Number(warehouseId);
      if (warehouseId && !Number.isNaN(wid)) orders = orders.filter((order) => order.warehouse_id === wid);
      if (filters.status && filters.status !== 'ALL') orders = orders.filter((order) => order.status === filters.status);
      if (filters.search) {
        const query = filters.search.toLowerCase();
        orders = orders.filter((order) =>
          order.do_number.toLowerCase().includes(query) || order.dealer_name.toLowerCase().includes(query),
        );
      }
      return orders.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
    }

    const params = { warehouseId };
    if (filters.status && filters.status !== 'ALL') params.status = filters.status;
    if (filters.search) params.search = filters.search;
    const response = await apiClient.get('/delivery-orders', { params });
    return asArray(response.data).map(normalizeDeliveryOrder);
  },

  getDeliveryOrderById: async (id) => {
    if (useMock) {
      await mockDelay(150);
      const orders = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const order = orders.find((item) => item.id === Number(id));
      if (!order) throw new Error('Không tìm thấy đơn xuất hàng');
      const items = getDb(KEYS.DO_ITEMS, INITIAL_DO_ITEMS).filter((item) => item.do_id === Number(id));
      return { ...order, items };
    }
    const response = await apiClient.get(`/delivery-orders/${id}`);
    return normalizeDeliveryOrder(response.data);
  },

  createDeliveryOrder: async (data) => {
    if (useMock) {
      await mockDelay(350);
      const orders = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const items = getDb(KEYS.DO_ITEMS, INITIAL_DO_ITEMS);
      const today = new Date().toISOString().slice(0, 10);
      const id = orders.length > 0 ? Math.max(...orders.map((order) => order.id)) + 1 : 1;
      const doNumber = `DO-${today.replace(/-/g, '')}-${String(id).padStart(3, '0')}`;
      const newOrder = {
        id,
        do_number: doNumber,
        dealer_id: Number(data.dealer_id),
        dealer_name: data.dealer_name || `Đại lý #${data.dealer_id}`,
        warehouse_id: Number(data.warehouse_id),
        status: 'NEW',
        raw_status: 'NEW',
        expected_delivery_date: data.expected_delivery_date,
        document_date: today,
        notes: data.notes || '',
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      };
      const newItems = data.items.map((item, index) => ({
        id: items.length + index + 1,
        do_id: id,
        product_id: Number(item.product_id),
        product_name: item.product_name || `Sản phẩm #${item.product_id}`,
        sku: item.sku || '',
        requested_qty: Number(item.requested_qty),
        reserved_qty: Number(item.requested_qty),
        planned_qty: Number(item.requested_qty),
        picked_qty: 0,
        issued_qty: 0,
        unit_price: Number(item.unit_price || 0),
        allocations: [],
      }));
      saveDb(KEYS.DELIVERY_ORDERS, [...orders, newOrder]);
      saveDb(KEYS.DO_ITEMS, [...items, ...newItems]);
      addAuditLog('DO_CREATED', 'DeliveryOrder', id, `Tạo đơn xuất hàng: ${doNumber}`);
      return { ...newOrder, items: newItems };
    }
    const response = await apiClient.post('/delivery-orders', toDeliveryOrderCreatePayload(data));
    return normalizeDeliveryOrder(response.data);
  },

  cancelDeliveryOrder: async (id, reason) => {
    if (useMock) {
      await mockDelay();
      const orders = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = orders.findIndex((order) => order.id === Number(id));
      if (idx === -1) throw new Error('Không tìm thấy đơn xuất hàng');
      orders[idx] = { ...orders[idx], status: 'CANCELLED', raw_status: 'CANCELLED', cancel_reason: reason };
      saveDb(KEYS.DELIVERY_ORDERS, orders);
      return orders[idx];
    }
    const response = await apiClient.put(`/delivery-orders/${id}/cancel`, { reason });
    return normalizeDeliveryOrder(response.data);
  },

  startPicking: async (id) => {
    const order = await outboundService.getDeliveryOrderById(id);
    if (useMock) {
      const orders = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = orders.findIndex((item) => item.id === Number(id));
      if (idx !== -1) {
        orders[idx] = { ...orders[idx], status: 'PICKING', raw_status: 'WAITING_PICKING' };
        saveDb(KEYS.DELIVERY_ORDERS, orders);
      }
      return { ...order, status: 'PICKING', raw_status: 'WAITING_PICKING' };
    }
    const response = await apiClient.put(`/delivery-orders/${id}/picking-plan`, buildPickingPlanPayload(order.items));
    return normalizeDeliveryOrder(response.data);
  },

  completePicking: async (id, pickedItems) => {
    const order = await outboundService.getDeliveryOrderById(id);
    if (useMock) {
      const items = getDb(KEYS.DO_ITEMS, INITIAL_DO_ITEMS);
      pickedItems.forEach((picked) => {
        const idx = items.findIndex((item) => item.id === Number(picked.id));
        if (idx !== -1) {
          items[idx].issued_qty = Number(picked.issued_qty);
          items[idx].picked_qty = Number(picked.issued_qty);
          items[idx].serial_number = picked.serial_number || '';
        }
      });
      saveDb(KEYS.DO_ITEMS, items);
      return order;
    }
    const mergedItems = order.items.map((item) => ({
      ...item,
      ...(pickedItems.find((picked) => Number(picked.id) === Number(item.id)) || {}),
    }));
    const response = await apiClient.put(`/delivery-orders/${id}/pick-qc-result`, buildPickQcPayload(mergedItems));
    return normalizeDeliveryOrder(response.data);
  },

  confirmQCOutbound: async (id, qcData) => {
    const order = await outboundService.getDeliveryOrderById(id);
    if (useMock) {
      const items = getDb(KEYS.DO_ITEMS, INITIAL_DO_ITEMS);
      qcData.items.forEach((qcItem) => {
        const idx = items.findIndex((item) => item.id === Number(qcItem.id));
        if (idx !== -1) {
          items[idx].qc_result = qcItem.result;
          items[idx].qc_failure_reason = qcItem.reason || null;
        }
      });
      const orders = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const orderIdx = orders.findIndex((item) => item.id === Number(id));
      if (orderIdx !== -1) orders[orderIdx].qc_completed_at = new Date().toISOString();
      saveDb(KEYS.DO_ITEMS, items);
      saveDb(KEYS.DELIVERY_ORDERS, orders);
      return orders[orderIdx] || order;
    }
    const response = await apiClient.put(`/delivery-orders/${id}/pick-qc-result`, buildPickQcPayload(order.items, qcData.items));
    return normalizeDeliveryOrder(response.data);
  },

  approveQualityOutbound: async (id, notes = '') => {
    if (useMock) {
      await mockDelay();
      const orders = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = orders.findIndex((order) => order.id === Number(id));
      if (idx === -1) throw new Error('Không tìm thấy đơn xuất hàng');
      orders[idx] = { ...orders[idx], status: 'PICKING', raw_status: 'QC_COMPLETED', qc_completed_at: new Date().toISOString() };
      saveDb(KEYS.DELIVERY_ORDERS, orders);
      return orders[idx];
    }
    const response = await apiClient.put(`/delivery-orders/${id}/quality-approval`, { notes });
    return normalizeDeliveryOrder(response.data);
  },

  approveWarehouseOutbound: async (id, notes = '') => {
    if (useMock) {
      await mockDelay();
      const orders = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = orders.findIndex((order) => order.id === Number(id));
      if (idx === -1) throw new Error('Không tìm thấy đơn xuất hàng');
      orders[idx] = { ...orders[idx], status: 'READY_TO_SHIP', raw_status: 'WAREHOUSE_APPROVED' };
      saveDb(KEYS.DELIVERY_ORDERS, orders);
      return orders[idx];
    }
    const response = await apiClient.put(`/delivery-orders/${id}/warehouse-approval`, { notes });
    return normalizeDeliveryOrder(response.data);
  },

  rejectWarehouseOutbound: async (id, reason) => {
    if (useMock) {
      await mockDelay();
      const orders = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = orders.findIndex((order) => order.id === Number(id));
      if (idx === -1) throw new Error('Không tìm thấy đơn xuất hàng');
      orders[idx] = { ...orders[idx], status: 'PICKING', raw_status: 'REJECTED', cancel_reason: reason, qc_completed_at: null };
      saveDb(KEYS.DELIVERY_ORDERS, orders);
      return orders[idx];
    }
    const response = await apiClient.put(`/delivery-orders/${id}/warehouse-reject`, { reason, returnToBinRecords: [] });
    return normalizeDeliveryOrder(response.data);
  },

  getTrips: async (warehouseId, filters = {}) => {
    if (useMock) {
      await mockDelay();
      let trips = getDb(KEYS.TRIPS, INITIAL_TRIPS);
      const wid = Number(warehouseId);
      if (warehouseId && !Number.isNaN(wid)) trips = trips.filter((trip) => trip.warehouse_id === wid);
      if (filters.status && filters.status !== 'ALL') trips = trips.filter((trip) => trip.status === filters.status);
      return trips.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
    }
    try {
      const response = await apiClient.get('/trips', { params: { warehouseId, status: filters.status } });
      return asArray(response.data).map(normalizeTrip);
    } catch (error) {
      if (error.response?.status === 404 || error.response?.status === 405) return [];
      throw error;
    }
  },

  getTripById: async (id) => {
    if (useMock) {
      await mockDelay(150);
      const trip = getDb(KEYS.TRIPS, INITIAL_TRIPS).find((item) => item.id === Number(id));
      if (!trip) throw new Error('Không tìm thấy chuyến xe');
      const stops = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS)
        .filter((item) => item.trip_id === Number(id))
        .sort((a, b) => a.stop_order - b.stop_order);
      return { ...trip, delivery_orders: stops };
    }
    const response = await apiClient.get(`/trips/${id}`);
    return normalizeTrip(response.data);
  },

  createTrip: async (data) => {
    if (useMock) {
      await mockDelay(350);
      const trips = getDb(KEYS.TRIPS, INITIAL_TRIPS);
      const tripDos = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS);
      const today = new Date().toISOString().slice(0, 10).replace(/-/g, '');
      const id = trips.length > 0 ? Math.max(...trips.map((trip) => trip.id)) + 1 : 1;
      const newTrip = {
        id,
        trip_number: `TRP-${today}-${String(id).padStart(3, '0')}`,
        warehouse_id: Number(data.warehouse_id),
        vehicle_id: Number(data.vehicle_id),
        vehicle_plate: data.vehicle_plate,
        driver_id: Number(data.driver_id),
        driver_name: data.driver_name,
        planned_date: data.planned_date,
        status: 'PLANNED',
        total_weight_kg: data.total_weight_kg || 0,
        created_at: new Date().toISOString(),
      };
      const stops = data.delivery_orders.map((order, index) => ({
        id: tripDos.length + index + 1,
        trip_id: id,
        do_id: order.id,
        do_number: order.do_number,
        stop_order: index + 1,
        dealer_name: order.dealer_name,
        dealer_address: order.dealer_address || 'Địa chỉ đại lý',
        delivery_status: 'READY_TO_SHIP',
        raw_status: 'WAREHOUSE_APPROVED',
      }));
      saveDb(KEYS.TRIPS, [...trips, newTrip]);
      saveDb(KEYS.TRIP_DOS, [...tripDos, ...stops]);
      return newTrip;
    }
    const response = await apiClient.post('/trips', toTripCreatePayload(data));
    return normalizeTrip(response.data);
  },

  departTrip: async (id) => {
    if (useMock) {
      await mockDelay();
      const trips = getDb(KEYS.TRIPS, INITIAL_TRIPS);
      const idx = trips.findIndex((trip) => trip.id === Number(id));
      if (idx === -1) throw new Error('Không tìm thấy chuyến xe');
      trips[idx] = { ...trips[idx], status: 'IN_TRANSIT' };
      saveDb(KEYS.TRIPS, trips);
      return trips[idx];
    }
    const response = await apiClient.put(`/trips/${id}/depart`, { confirmedAt: new Date().toISOString() });
    return normalizeTrip(response.data);
  },

  uploadPodEvidence: async (tripId, doId, { goodsImage, signDocumentImage, notes = '' }) => {
    if (useMock) return { success: true };
    const formData = new FormData();
    formData.append('goodsImage', goodsImage);
    formData.append('signDocumentImage', signDocumentImage);
    if (notes) formData.append('notes', notes);
    const response = await apiClient.post(`/trips/${tripId}/delivery-orders/${doId}/pod-evidence`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  },

  requestOTP: async (tripId, doId, resend = false) => {
    if (useMock) {
      await mockDelay();
      return { status: 'ACTIVE', expiresAt: new Date(Date.now() + 5 * 60000).toISOString() };
    }
    const response = await apiClient.post(`/trips/${tripId}/delivery-orders/${doId}/delivery-otp`, { resend });
    return response.data;
  },

  verifyOTPAndDeliver: async (tripId, doId, otp, notes = '') => {
    if (useMock) {
      await mockDelay(350);
      if (otp !== '123456') throw new Error('Mã OTP không chính xác');
      const stops = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS);
      const idx = stops.findIndex((stop) => stop.trip_id === Number(tripId) && stop.do_id === Number(doId));
      if (idx !== -1) stops[idx] = { ...stops[idx], delivery_status: 'DELIVERED', raw_status: 'COMPLETED' };
      saveDb(KEYS.TRIP_DOS, stops);
      return { success: true };
    }
    const response = await apiClient.put(`/trips/${tripId}/delivery-orders/${doId}/confirm-delivery`, { otp, notes });
    return response.data;
  },

  reportDeliveryFailure: async (tripId, doId, failureReason, notes = '') => {
    if (useMock) {
      await mockDelay(300);
      const stops = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS);
      const idx = stops.findIndex((stop) => stop.trip_id === Number(tripId) && stop.do_id === Number(doId));
      if (idx !== -1) stops[idx] = { ...stops[idx], delivery_status: 'FAILED', failure_reason: failureReason };
      saveDb(KEYS.TRIP_DOS, stops);
      return { success: true };
    }
    const response = await apiClient.put(`/trips/${tripId}/delivery-orders/${doId}/fail-delivery`, {
      failureReason,
      notes,
    });
    return response.data;
  },
};
