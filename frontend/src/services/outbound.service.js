import apiClient, { useMock } from './api.client';
import { masterDataService } from './masterData.service';

const KEYS = {
  DELIVERY_ORDERS: 'wms_db_delivery_orders',
  DO_ITEMS: 'wms_db_do_items',
  TRIPS: 'wms_db_trips',
  TRIP_DOS: 'wms_db_trip_dos',
  DELIVERIES: 'wms_db_deliveries',
  BILLING_NOTIFICATIONS: 'wms_db_billing_notifications'
};

const INITIAL_DELIVERY_ORDERS = [
  // Kho Hải Phòng (warehouse_id: 1)
  {
    id: 1,
    do_number: "DO-20260612-001",
    dealer_id: 1,
    dealer_name: "Đại lý Hoàng Phát",
    warehouse_id: 1,
    status: "NEW",
    expected_delivery_date: "2026-06-15",
    document_date: "2026-06-12",
    created_by: 7,
    cancel_reason: null,
    notes: "Giao gấp trước 10h sáng",
    created_at: "2026-06-12T07:00:00.000Z",
    updated_at: "2026-06-12T07:00:00.000Z"
  },
  {
    id: 2,
    do_number: "DO-20260612-002",
    dealer_id: 2,
    dealer_name: "Đại lý Trần Gia",
    warehouse_id: 1,
    status: "PICKING",
    expected_delivery_date: "2026-06-14",
    document_date: "2026-06-12",
    created_by: 7,
    cancel_reason: null,
    qc_completed_at: null,
    notes: "Gọi điện trước khi giao",
    created_at: "2026-06-12T08:00:00.000Z",
    updated_at: "2026-06-12T08:00:00.000Z"
  },
  {
    id: 3,
    do_number: "DO-20260612-003",
    dealer_id: 3,
    dealer_name: "Đại lý Minh Trí",
    warehouse_id: 1,
    status: "READY_TO_SHIP",
    expected_delivery_date: "2026-06-13",
    document_date: "2026-06-11",
    created_by: 7,
    cancel_reason: null,
    qc_completed_at: "2026-06-11T15:00:00.000Z",
    notes: "",
    created_at: "2026-06-11T09:00:00.000Z",
    updated_at: "2026-06-11T15:00:00.000Z"
  },
  // Kho Hà Nội (warehouse_id: 2)
  {
    id: 4,
    do_number: "DO-20260612-004",
    dealer_id: 1,
    dealer_name: "Đại lý Hoàng Phát (HN)",
    warehouse_id: 2,
    status: "NEW",
    expected_delivery_date: "2026-06-16",
    document_date: "2026-06-12",
    created_by: 7,
    cancel_reason: null,
    notes: "",
    created_at: "2026-06-12T09:00:00.000Z",
    updated_at: "2026-06-12T09:00:00.000Z"
  },
  {
    id: 5,
    do_number: "DO-20260612-005",
    dealer_id: 2,
    dealer_name: "Đại lý Trần Gia (HN)",
    warehouse_id: 2,
    status: "PICKING",
    expected_delivery_date: "2026-06-15",
    document_date: "2026-06-12",
    created_by: 7,
    cancel_reason: null,
    qc_completed_at: null,
    notes: "Ưu tiên giao trước",
    created_at: "2026-06-12T10:00:00.000Z",
    updated_at: "2026-06-12T10:00:00.000Z"
  },
  // Kho Hồ Chí Minh (warehouse_id: 3)
  {
    id: 6,
    do_number: "DO-20260612-006",
    dealer_id: 3,
    dealer_name: "Đại lý Minh Trí (HCM)",
    warehouse_id: 3,
    status: "NEW",
    expected_delivery_date: "2026-06-17",
    document_date: "2026-06-12",
    created_by: 7,
    cancel_reason: null,
    notes: "",
    created_at: "2026-06-12T11:00:00.000Z",
    updated_at: "2026-06-12T11:00:00.000Z"
  },
  {
    id: 7,
    do_number: "DO-20260610-001",
    dealer_id: 1,
    dealer_name: "Đại lý Hoàng Phát (HCM)",
    warehouse_id: 3,
    status: "DELIVERED",
    expected_delivery_date: "2026-06-11",
    document_date: "2026-06-10",
    created_by: 7,
    cancel_reason: null,
    qc_completed_at: "2026-06-10T14:00:00.000Z",
    notes: "",
    created_at: "2026-06-10T08:00:00.000Z",
    updated_at: "2026-06-11T10:00:00.000Z"
  },
];

const INITIAL_DO_ITEMS = [
  {
    id: 1,
    do_id: 1,
    product_id: 1,
    product_name: "ASUS Monitor ProArt",
    sku: "SKU-PA-001",
    batch_id: null,
    batch_number: null,
    location_id: null,
    bin_code: null,
    requested_qty: 5,
    reserved_qty: 5,
    issued_qty: 0,
    unit_price: 8500000,
    serial_number: null,
    qc_result: null,
    qc_failure_reason: null
  },
  {
    id: 2,
    do_id: 2,
    product_id: 2,
    product_name: "Logitech MX Master 3S",
    sku: "SKU-LOGI-MX3",
    batch_id: 3,
    batch_number: "BT-RC-20260603-0004-SKU-LOGI-MX3-A",
    location_id: 1,
    bin_code: "BIN-A1",
    requested_qty: 10,
    reserved_qty: 10,
    issued_qty: 0,
    unit_price: 2100000,
    serial_number: null,
    qc_result: null,
    qc_failure_reason: null
  },
  {
    id: 3,
    do_id: 3,
    product_id: 1,
    product_name: "ASUS Monitor ProArt",
    sku: "SKU-PA-001",
    batch_id: 1,
    batch_number: "BT-RC-20260601-0001-SKU-PA-001-A",
    location_id: 2,
    bin_code: "BIN-A2",
    requested_qty: 2,
    reserved_qty: 2,
    issued_qty: 2,
    unit_price: 8500000,
    serial_number: "SR-PA278-001,SR-PA278-002",
    qc_result: "PASSED",
    qc_failure_reason: null
  },
  // HN warehouse items (do_id: 4, 5)
  {
    id: 4,
    do_id: 4,
    product_id: 2,
    product_name: "Logitech MX Master 3S",
    sku: "SKU-LOGI-MX3",
    batch_id: null,
    batch_number: null,
    location_id: null,
    bin_code: null,
    requested_qty: 3,
    reserved_qty: 3,
    issued_qty: 0,
    unit_price: 2100000,
    serial_number: null,
    qc_result: null,
    qc_failure_reason: null
  },
  {
    id: 5,
    do_id: 5,
    product_id: 1,
    product_name: "ASUS Monitor ProArt",
    sku: "SKU-PA-001",
    batch_id: null,
    batch_number: null,
    location_id: null,
    bin_code: "BIN-B1",
    requested_qty: 4,
    reserved_qty: 4,
    issued_qty: 0,
    unit_price: 8500000,
    serial_number: null,
    qc_result: null,
    qc_failure_reason: null
  },
  // HCM warehouse items (do_id: 6, 7)
  {
    id: 6,
    do_id: 6,
    product_id: 2,
    product_name: "Logitech MX Master 3S",
    sku: "SKU-LOGI-MX3",
    batch_id: null,
    batch_number: null,
    location_id: null,
    bin_code: null,
    requested_qty: 8,
    reserved_qty: 8,
    issued_qty: 0,
    unit_price: 2100000,
    serial_number: null,
    qc_result: null,
    qc_failure_reason: null
  },
  {
    id: 7,
    do_id: 7,
    product_id: 1,
    product_name: "ASUS Monitor ProArt",
    sku: "SKU-PA-001",
    batch_id: 1,
    batch_number: "BT-RC-20260601-0001-SKU-PA-001-A",
    location_id: null,
    bin_code: "BIN-C3",
    requested_qty: 1,
    reserved_qty: 1,
    issued_qty: 1,
    unit_price: 8500000,
    serial_number: "SR-PA278-010",
    qc_result: "PASSED",
    qc_failure_reason: null
  },
];

const INITIAL_TRIPS = [
  {
    id: 1,
    trip_number: "TRP-20260612-001",
    warehouse_id: 1,
    vehicle_id: 1,
    vehicle_plate: "15C-123.45",
    driver_id: 13,
    driver_name: "Nguyễn Văn Tài Xế 1",
    planned_date: "2026-06-13",
    status: "PLANNED",
    total_weight_kg: 150,
    created_at: "2026-06-12T08:00:00.000Z"
  },
  {
    id: 2,
    trip_number: "TRP-20260611-001",
    warehouse_id: 2,
    vehicle_id: 2,
    vehicle_plate: "29C-987.65",
    driver_id: 14,
    driver_name: "Trần Văn Giao Hàng",
    planned_date: "2026-06-12",
    status: "IN_TRANSIT",
    total_weight_kg: 200,
    created_at: "2026-06-11T09:00:00.000Z"
  },
  {
    id: 3,
    trip_number: "TRP-20260610-001",
    warehouse_id: 3,
    vehicle_id: 3,
    vehicle_plate: "51C-555.55",
    driver_id: 13,
    driver_name: "Nguyễn Văn Tài Xế 1",
    planned_date: "2026-06-11",
    status: "COMPLETED",
    total_weight_kg: 80,
    created_at: "2026-06-10T07:00:00.000Z"
  }
];

const INITIAL_TRIP_DOS = [
  {
    id: 1,
    trip_id: 1,
    do_id: 3,
    do_number: "DO-20260612-003",
    stop_order: 1,
    dealer_name: "Đại lý Minh Trí",
    dealer_address: "123 Lê Lợi, Hải Phòng",
    delivery_status: "READY_TO_SHIP",
    otp_status: "NONE",
    otp_expires_at: null,
    otp_attempt_count: 0,
    failure_reason: null
  }
];

const INITIAL_BILLING_NOTIFICATIONS = [];

export const getDb = (key, initial) => {
  const data = localStorage.getItem(key);
  if (!data) {
    localStorage.setItem(key, JSON.stringify(initial));
    return initial;
  }
  try {
    return JSON.parse(data);
  } catch (e) {
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
    createdAt: new Date().toISOString()
  };
  localStorage.setItem('wms_audit_logs', JSON.stringify([newLog, ...logs]));
};

// Auto-seed with version check — bumping SEED_VERSION forces a fresh reseed
const SEED_VERSION = 'v004-r3';
if (typeof window !== 'undefined' && window.localStorage) {
  if (localStorage.getItem('wms_outbound_seed') !== SEED_VERSION) {
    localStorage.setItem(KEYS.DELIVERY_ORDERS, JSON.stringify(INITIAL_DELIVERY_ORDERS));
    localStorage.setItem(KEYS.DO_ITEMS, JSON.stringify(INITIAL_DO_ITEMS));
    localStorage.setItem(KEYS.TRIPS, JSON.stringify(INITIAL_TRIPS));
    localStorage.setItem(KEYS.TRIP_DOS, JSON.stringify(INITIAL_TRIP_DOS));
    localStorage.setItem(KEYS.BILLING_NOTIFICATIONS, JSON.stringify(INITIAL_BILLING_NOTIFICATIONS));
    localStorage.setItem('wms_outbound_seed', SEED_VERSION);
  }
}

export const outboundService = {
  // --- DELIVERY ORDERS ---
  getDeliveryOrders: async (warehouseId, filters = {}) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      let dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);

      // Filter by warehouse only when a valid numeric id is provided
      const wid = Number(warehouseId);
      if (warehouseId && !isNaN(wid)) {
        dos = dos.filter(d => d.warehouse_id === wid);
      }
      
      if (filters.status && filters.status !== 'ALL') {
        dos = dos.filter(d => d.status === filters.status);
      }
      
      if (filters.search) {
        const query = filters.search.toLowerCase();
        dos = dos.filter(d => 
          d.do_number.toLowerCase().includes(query) || 
          d.dealer_name.toLowerCase().includes(query)
        );
      }
      
      return dos.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
    }
    
    // API logic when backend is ready
    let url = `/delivery-orders?warehouseId=${warehouseId}`;
    if (filters.status && filters.status !== 'ALL') url += `&status=${filters.status}`;
    if (filters.search) url += `&search=${filters.search}`;
    const response = await apiClient.get(url);
    return response.data;
  },

  getDeliveryOrderById: async (id) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 200));
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const doItem = dos.find(d => d.id === Number(id));
      if (!doItem) throw new Error('DO_NOT_FOUND');

      const items = getDb(KEYS.DO_ITEMS, INITIAL_DO_ITEMS);
      const doItems = items.filter(item => item.do_id === Number(id));

      return {
        ...doItem,
        items: doItems
      };
    }
    const response = await apiClient.get(`/delivery-orders/${id}`);
    return response.data;
  },

  createDeliveryOrder: async (doData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const doItems = getDb(KEYS.DO_ITEMS, INITIAL_DO_ITEMS);
      const currentUser = JSON.parse(localStorage.getItem('wms_user')) || { id: 3, fullName: 'Planner' };

      // Simulate credit check block
      if (doData.dealer_id === 4) { // Let's pretend dealer 4 is blocked
        throw new Error('CREDIT_HOLD');
      }

      // Generate DO number
      const today = new Date();
      const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '');
      const countToday = dos.filter(d => d.do_number.startsWith(`DO-${dateStr}`)).length + 1;
      const doNumber = `DO-${dateStr}-${String(countToday).padStart(3, '0')}`;

      const newDo = {
        id: dos.length > 0 ? Math.max(...dos.map(d => d.id)) + 1 : 1,
        do_number: doNumber,
        dealer_id: Number(doData.dealer_id),
        dealer_name: doData.dealer_name || "Đại lý Test",
        warehouse_id: Number(doData.warehouse_id),
        status: "NEW",
        expected_delivery_date: doData.expected_delivery_date,
        document_date: today.toISOString().slice(0, 10),
        created_by: currentUser.id,
        cancel_reason: null,
        notes: doData.notes || "",
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      };

      const newItems = doData.items.map((item, index) => ({
        id: doItems.length > 0 ? Math.max(...doItems.map(di => di.id)) + index + 1 : index + 1,
        do_id: newDo.id,
        product_id: Number(item.product_id),
        product_name: item.product_name,
        sku: item.sku,
        batch_id: null,
        batch_number: null,
        location_id: null,
        bin_code: null,
        requested_qty: Number(item.requested_qty),
        reserved_qty: Number(item.requested_qty), // Mock automatic reserve
        issued_qty: 0,
        unit_price: Number(item.unit_price) || 0,
        serial_number: null,
        qc_result: null,
        qc_failure_reason: null
      }));

      dos.push(newDo);
      doItems.push(...newItems);

      saveDb(KEYS.DELIVERY_ORDERS, dos);
      saveDb(KEYS.DO_ITEMS, doItems);

      addAuditLog('DO_CREATED', 'DeliveryOrder', newDo.id, `Tạo đơn xuất hàng: ${newDo.do_number}`);
      return newDo;
    }
    const response = await apiClient.post('/delivery-orders', doData);
    return response.data;
  },

  cancelDeliveryOrder: async (id, reason) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = dos.findIndex(d => d.id === Number(id));
      if (idx === -1) throw new Error('DO_NOT_FOUND');
      
      if (dos[idx].status !== 'NEW') {
        throw new Error('CANNOT_CANCEL_PROCESSED_DO');
      }

      dos[idx].status = 'CANCELLED';
      dos[idx].cancel_reason = reason;
      dos[idx].updated_at = new Date().toISOString();

      saveDb(KEYS.DELIVERY_ORDERS, dos);
      
      addAuditLog('DO_CANCELLED', 'DeliveryOrder', id, `Hủy đơn xuất hàng: ${dos[idx].do_number} - Lý do: ${reason}`);
      return dos[idx];
    }
    const response = await apiClient.put(`/delivery-orders/${id}/cancel`, { reason });
    return response.data;
  },
  
  startPicking: async (id) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = dos.findIndex(d => d.id === Number(id));
      if (idx === -1) throw new Error('DO_NOT_FOUND');
      
      if (dos[idx].status !== 'NEW') {
        throw new Error('DO_NOT_IN_NEW_STATUS');
      }

      dos[idx].status = 'PICKING';
      dos[idx].updated_at = new Date().toISOString();

      saveDb(KEYS.DELIVERY_ORDERS, dos);
      
      addAuditLog('DO_PICKING_STARTED', 'DeliveryOrder', id, `Bắt đầu lấy hàng cho đơn: ${dos[idx].do_number}`);
      return dos[idx];
    }
    const response = await apiClient.put(`/delivery-orders/${id}/start-picking`);
    return response.data;
  },

  completePicking: async (id, pickedItems) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = dos.findIndex(d => d.id === Number(id));
      if (idx === -1) throw new Error('DO_NOT_FOUND');
      
      if (dos[idx].status !== 'PICKING') {
        throw new Error('DO_NOT_IN_PICKING_STATUS');
      }

      // Update item quantities
      const items = getDb(KEYS.DO_ITEMS, INITIAL_DO_ITEMS);
      pickedItems.forEach(picked => {
        const itemIdx = items.findIndex(i => i.id === picked.id);
        if (itemIdx !== -1) {
          items[itemIdx].issued_qty = picked.issued_qty;
          if (picked.serial_number) {
            items[itemIdx].serial_number = picked.serial_number;
          }
        }
      });

      // Status remains PICKING until QC and Warehouse Manager Approval
      dos[idx].updated_at = new Date().toISOString();

      saveDb(KEYS.DELIVERY_ORDERS, dos);
      saveDb(KEYS.DO_ITEMS, items);
      
      addAuditLog('DO_PICKING_COMPLETED', 'DeliveryOrder', id, `Hoàn tất lấy hàng cho đơn: ${dos[idx].do_number}`);
      return dos[idx];
    }
    const response = await apiClient.put(`/delivery-orders/${id}/complete-picking`, { items: pickedItems });
    return response.data;
  },

  confirmQCOutbound: async (id, qcData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = dos.findIndex(d => d.id === Number(id));
      if (idx === -1) throw new Error('DO_NOT_FOUND');
      
      const items = getDb(KEYS.DO_ITEMS, INITIAL_DO_ITEMS);
      qcData.items.forEach(qcItem => {
        const itemIdx = items.findIndex(i => i.id === qcItem.id);
        if (itemIdx !== -1) {
          items[itemIdx].qc_result = qcItem.result;
          items[itemIdx].qc_failure_reason = qcItem.reason || null;
        }
      });

      dos[idx].qc_completed_at = new Date().toISOString();
      dos[idx].updated_at = new Date().toISOString();

      saveDb(KEYS.DELIVERY_ORDERS, dos);
      saveDb(KEYS.DO_ITEMS, items);

      addAuditLog('QC_COMPLETED', 'DeliveryOrder', id, `Hoàn tất QC xuất kho. Đạt: ${qcData.items.filter(i=>i.result==='PASSED').length}`);
      return dos[idx];
    }
    const response = await apiClient.post(`/delivery-orders/${id}/qc`, qcData);
    return response.data;
  },

  approveWarehouseOutbound: async (id) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = dos.findIndex(d => d.id === Number(id));
      if (idx === -1) throw new Error('DO_NOT_FOUND');
      
      dos[idx].status = 'READY_TO_SHIP';
      dos[idx].updated_at = new Date().toISOString();
      saveDb(KEYS.DELIVERY_ORDERS, dos);

      addAuditLog('DO_APPROVED', 'DeliveryOrder', id, `Quản lý kho phê duyệt đơn xuất hàng`);
      return dos[idx];
    }
    const response = await apiClient.post(`/delivery-orders/${id}/approve`);
    return response.data;
  },

  rejectWarehouseOutbound: async (id, reason) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const idx = dos.findIndex(d => d.id === Number(id));
      if (idx === -1) throw new Error('DO_NOT_FOUND');
      
      // Rejection sends DO back to PICKING so storekeeper can re-pick
      dos[idx].status = 'PICKING';
      dos[idx].cancel_reason = `Quản lý kho từ chối: ${reason}`;
      dos[idx].qc_completed_at = null;
      dos[idx].updated_at = new Date().toISOString();
      saveDb(KEYS.DELIVERY_ORDERS, dos);

      addAuditLog('DO_REJECTED', 'DeliveryOrder', id, `Quản lý kho từ chối đơn xuất hàng: ${reason}`);
      return dos[idx];
    }
    const response = await apiClient.post(`/delivery-orders/${id}/reject`, { reason });
    return response.data;
  },

  // --- TRIPS ---
  getTrips: async (warehouseId, filters = {}) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 300));
      let trips = getDb(KEYS.TRIPS, INITIAL_TRIPS);
      const wid = Number(warehouseId);
      if (warehouseId && !isNaN(wid)) {
        trips = trips.filter(t => t.warehouse_id === wid);
      }
      if (filters.status && filters.status !== 'ALL') {
        trips = trips.filter(t => t.status === filters.status);
      }
      return trips.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
    }
    const response = await apiClient.get(`/trips?warehouseId=${warehouseId}`);
    return response.data;
  },

  getTripById: async (id) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 200));
      const trips = getDb(KEYS.TRIPS, INITIAL_TRIPS);
      const trip = trips.find(t => t.id === Number(id));
      if (!trip) throw new Error('TRIP_NOT_FOUND');

      const tripDos = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS);
      const dos = tripDos.filter(td => td.trip_id === Number(id)).sort((a, b) => a.stop_order - b.stop_order);

      return { ...trip, delivery_orders: dos };
    }
    const response = await apiClient.get(`/trips/${id}`);
    return response.data;
  },

  createTrip: async (tripData) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const trips = getDb(KEYS.TRIPS, INITIAL_TRIPS);
      const tripDos = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS);
      
      const today = new Date();
      const dateStr = today.toISOString().slice(0, 10).replace(/-/g, '');
      const countToday = trips.filter(t => t.trip_number.startsWith(`TRP-${dateStr}`)).length + 1;
      const tripNumber = `TRP-${dateStr}-${String(countToday).padStart(3, '0')}`;

      const newTrip = {
        id: trips.length > 0 ? Math.max(...trips.map(t => t.id)) + 1 : 1,
        trip_number: tripNumber,
        warehouse_id: Number(tripData.warehouse_id),
        vehicle_id: Number(tripData.vehicle_id),
        vehicle_plate: tripData.vehicle_plate,
        driver_id: Number(tripData.driver_id),
        driver_name: tripData.driver_name,
        planned_date: tripData.planned_date,
        status: "PLANNED",
        total_weight_kg: tripData.total_weight_kg || 0,
        created_at: new Date().toISOString()
      };

      const newTripDos = tripData.delivery_orders.map((doItem, index) => ({
        id: tripDos.length > 0 ? Math.max(...tripDos.map(td => td.id)) + index + 1 : index + 1,
        trip_id: newTrip.id,
        do_id: doItem.id,
        do_number: doItem.do_number,
        stop_order: index + 1,
        dealer_name: doItem.dealer_name,
        dealer_address: doItem.dealer_address || "Địa chỉ đại lý (Mock)",
        delivery_status: "READY_TO_SHIP",
        otp_status: "NONE",
        otp_expires_at: null,
        otp_attempt_count: 0,
        failure_reason: null
      }));

      trips.push(newTrip);
      tripDos.push(...newTripDos);

      saveDb(KEYS.TRIPS, trips);
      saveDb(KEYS.TRIP_DOS, tripDos);
      
      addAuditLog('TRIP_CREATED', 'Trip', newTrip.id, `Tạo chuyến xe: ${newTrip.trip_number}`);
      return newTrip;
    }
    const response = await apiClient.post('/trips', tripData);
    return response.data;
  },

  departTrip: async (id) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const trips = getDb(KEYS.TRIPS, INITIAL_TRIPS);
      const idx = trips.findIndex(t => t.id === Number(id));
      if (idx === -1) throw new Error('TRIP_NOT_FOUND');
      
      trips[idx].status = 'IN_TRANSIT';
      saveDb(KEYS.TRIPS, trips);

      // Update associated DOs to IN_TRANSIT
      const tripDos = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS);
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      
      const relatedTripDos = tripDos.filter(td => td.trip_id === Number(id));
      relatedTripDos.forEach(td => {
        const doIdx = dos.findIndex(d => d.id === td.do_id);
        if (doIdx !== -1 && dos[doIdx].status === 'READY_TO_SHIP') {
          dos[doIdx].status = 'IN_TRANSIT';
          dos[doIdx].updated_at = new Date().toISOString();
        }
      });
      saveDb(KEYS.DELIVERY_ORDERS, dos);

      addAuditLog('TRIP_DEPARTED', 'Trip', id, `Chuyến xe xuất phát: ${trips[idx].trip_number}`);
      return trips[idx];
    }
    const response = await apiClient.put(`/trips/${id}/depart`);
    return response.data;
  },

  // --- DRIVER & POD ---
  requestOTP: async (tripId, doId) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 400));
      const tripDos = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS);
      const idx = tripDos.findIndex(td => td.trip_id === Number(tripId) && td.do_id === Number(doId));
      if (idx === -1) throw new Error('TRIP_DO_NOT_FOUND');

      // Mock OTP as '123456'
      tripDos[idx].otp_status = 'SENT';
      // Mock expiry 5 mins
      tripDos[idx].otp_expires_at = new Date(Date.now() + 5 * 60000).toISOString();
      
      saveDb(KEYS.TRIP_DOS, tripDos);
      addAuditLog('OTP_REQUESTED', 'TripDO', tripDos[idx].id, `Yêu cầu gửi OTP cho đơn ${tripDos[idx].do_number}`);
      return { success: true, message: 'Đã gửi mã OTP đến số điện thoại đại lý' };
    }
    const response = await apiClient.post(`/trips/${tripId}/delivery-orders/${doId}/request-otp`);
    return response.data;
  },

  verifyOTPAndDeliver: async (tripId, doId, otpCode, photoUrls = []) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 600));
      const tripDos = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS);
      const idx = tripDos.findIndex(td => td.trip_id === Number(tripId) && td.do_id === Number(doId));
      if (idx === -1) throw new Error('TRIP_DO_NOT_FOUND');

      if (otpCode !== '123456') {
        tripDos[idx].otp_attempt_count += 1;
        saveDb(KEYS.TRIP_DOS, tripDos);
        throw new Error('Mã OTP không chính xác');
      }

      // Mark delivered
      tripDos[idx].delivery_status = 'DELIVERED';
      tripDos[idx].otp_status = 'VERIFIED';
      tripDos[idx].photo_urls = photoUrls;
      saveDb(KEYS.TRIP_DOS, tripDos);

      // Update DO status
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const doIdx = dos.findIndex(d => d.id === Number(doId));
      if (doIdx !== -1) {
        dos[doIdx].status = 'DELIVERED';
        dos[doIdx].updated_at = new Date().toISOString();
        saveDb(KEYS.DELIVERY_ORDERS, dos);
      }

      addAuditLog('DO_DELIVERED', 'DeliveryOrder', doId, `Giao hàng thành công (Có OTP và Hình ảnh)`);

      // Mock Trigger Billing Notification (US6)
      const bills = getDb(KEYS.BILLING_NOTIFICATIONS, INITIAL_BILLING_NOTIFICATIONS);
      bills.push({
        id: Date.now(),
        do_id: Number(doId),
        do_number: dos[doIdx]?.do_number || 'UNKNOWN',
        dealer_id: dos[doIdx]?.dealer_id,
        dealer_name: dos[doIdx]?.dealer_name,
        status: 'PENDING_BILLING',
        created_at: new Date().toISOString()
      });
      saveDb(KEYS.BILLING_NOTIFICATIONS, bills);

      return { success: true };
    }
    const response = await apiClient.post(`/trips/${tripId}/delivery-orders/${doId}/deliver`, { otpCode, photoUrls });
    return response.data;
  },

  reportDeliveryFailure: async (tripId, doId, reason, photoUrls = []) => {
    if (useMock) {
      await new Promise(resolve => setTimeout(resolve, 500));
      const tripDos = getDb(KEYS.TRIP_DOS, INITIAL_TRIP_DOS);
      const idx = tripDos.findIndex(td => td.trip_id === Number(tripId) && td.do_id === Number(doId));
      if (idx === -1) throw new Error('TRIP_DO_NOT_FOUND');

      tripDos[idx].delivery_status = 'FAILED';
      tripDos[idx].failure_reason = reason;
      tripDos[idx].photo_urls = photoUrls;
      saveDb(KEYS.TRIP_DOS, tripDos);

      // Change DO status to RETURNED
      const dos = getDb(KEYS.DELIVERY_ORDERS, INITIAL_DELIVERY_ORDERS);
      const doIdx = dos.findIndex(d => d.id === Number(doId));
      if (doIdx !== -1) {
        dos[doIdx].status = 'RETURNED';
        dos[doIdx].updated_at = new Date().toISOString();
        saveDb(KEYS.DELIVERY_ORDERS, dos);
      }

      addAuditLog('DO_DELIVERY_FAILED', 'DeliveryOrder', doId, `Giao hàng thất bại: ${reason}`);
      return { success: true };
    }
    const response = await apiClient.post(`/trips/${tripId}/delivery-orders/${doId}/fail`, { reason, photoUrls });
    return response.data;
  },
};
