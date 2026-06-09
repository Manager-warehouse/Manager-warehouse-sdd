import apiClient, { useMock } from "./api.client";

const mapToCamelCase = (obj) => {
  if (!obj || typeof obj !== "object") return obj;
  if (Array.isArray(obj)) return obj.map(mapToCamelCase);
  const camel = {};
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      const camelKey = key.replace(/_([a-z0-9])/g, (g) => g[1].toUpperCase());
      camel[camelKey] = mapToCamelCase(obj[key]);
    }
  }
  return camel;
};

const mapToSnakeCase = (obj) => {
  if (!obj || typeof obj !== "object") return obj;
  if (Array.isArray(obj)) return obj.map(mapToSnakeCase);
  const snake = {};
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      const snakeKey = key.replace(
        /[A-Z]/g,
        (letter) => `_${letter.toLowerCase()}`,
      );
      snake[snakeKey] = mapToSnakeCase(obj[key]);
    }
  }
  return snake;
};

// Persistence keys
const KEYS = {
  PRODUCTS: "wms_db_products",
  WAREHOUSES: "wms_db_warehouses",
  LOCATIONS: "wms_db_warehouse_locations",
  DEALERS: "wms_db_dealers",
  SUPPLIERS: "wms_db_suppliers",
  VEHICLES: "wms_db_vehicles",
  DRIVERS: "wms_db_drivers",
};

// Initial Mock Data
const INITIAL_PRODUCTS = [
  {
    id: 1,
    sku: "SKU-PA-001",
    name: "Màn hình ASUS ProArt PA278CV 27 inch IPS 2K",
    unit: "Cái",
    unit_per_pack: 1,
    description: "Màn hình chuyên đồ họa màu sắc chuẩn xác Delta E < 2",
    image_url: "",
    weight_kg: 8.5,
    volume_m3: 0.085,
    has_serial: true,
    reorder_point: 10.0,
    is_active: true,
    created_at: "2026-05-30T10:00:00Z",
    updated_at: "2026-05-30T10:00:00Z",
  },
  {
    id: 2,
    sku: "SKU-LOGI-MX3",
    name: "Chuột không dây Logitech MX Master 3S",
    unit: "Hộp",
    unit_per_pack: 1,
    description: "Chuột công thái học cao cấp hỗ trợ sạc nhanh Type-C",
    image_url: "",
    weight_kg: 0.35,
    volume_m3: 0.0015,
    has_serial: false,
    reorder_point: 50.0,
    is_active: true,
    created_at: "2026-05-30T10:05:00Z",
    updated_at: "2026-05-30T10:05:00Z",
  },
];

const INITIAL_WAREHOUSES = [
  {
    id: 1,
    code: "HP-01",
    name: "Kho Hải Phòng",
    address: "Số 1 Lê Thánh Tông, Ngô Quyền, Hải Phòng",
    phone: "02253888999",
    manager_id: 3,
    type: "PHYSICAL",
    is_active: true,
  },
  {
    id: 2,
    code: "HN-01",
    name: "Kho Hà Nội",
    address: "Số 15 Cầu Giấy, Quan Hoa, Hà Nội",
    phone: "0243111222",
    manager_id: 4,
    type: "PHYSICAL",
    is_active: true,
  },
  {
    id: 3,
    code: "HCM-01",
    name: "Kho Hồ Chí Minh",
    address: "Số 100 Cộng Hòa, Tân Bình, TP. HCM",
    phone: "0283555666",
    manager_id: 2,
    type: "PHYSICAL",
    is_active: true,
  },
  {
    id: 4,
    code: "IN_TRANSIT",
    name: "Kho Trung Chuyển Nội Bộ",
    address: "Trên đường vận chuyển",
    phone: "N/A",
    manager_id: 1,
    type: "IN_TRANSIT",
    is_active: true,
  },
];

const INITIAL_LOCATIONS = [
  {
    id: 101,
    warehouse_id: 1,
    code: "HP-01.Z1.R1.S1.B01",
    type: "BIN",
    parent_id: null,
    capacity_m3: 5.0,
    capacity_kg: 1000.0,
    current_volume_m3: 2.15,
    current_weight_kg: 350.0,
    is_quarantine: false,
    is_active: true,
  },
  {
    id: 102,
    warehouse_id: 1,
    code: "HP-01.ZQ.R1.S1.B01",
    type: "BIN",
    parent_id: null,
    capacity_m3: 10.0,
    capacity_kg: 2000.0,
    current_volume_m3: 0.0,
    current_weight_kg: 0.0,
    is_quarantine: true,
    is_active: true,
  },
  {
    id: 201,
    warehouse_id: 2,
    code: "HN-01.Z1.R1.S1.B01",
    type: "BIN",
    parent_id: null,
    capacity_m3: 8.0,
    capacity_kg: 1500.0,
    current_volume_m3: 4.5,
    current_weight_kg: 900.0,
    is_quarantine: false,
    is_active: true,
  },
];

const INITIAL_DEALERS = [
  {
    id: 1,
    code: "DL-HAIPHONG-01",
    name: "Công ty TNHH Tin học Hoàng Phát",
    phone: "0912111222",
    default_delivery_address: "45 Lạch Tray, Ngô Quyền, Hải Phòng",
    region: "Hải Phòng",
    payment_term_days: 30,
    credit_limit: 500000000.0,
    current_balance: 150000000.0,
    credit_status: "ACTIVE",
    is_active: true,
    created_at: "2026-05-30T11:00:00Z",
  },
  {
    id: 2,
    code: "DL-HANOI-02",
    name: "Đại lý Máy tính Bách Khoa",
    phone: "0988333444",
    default_delivery_address: "12 Tạ Quang Bửu, Hai Bà Trưng, Hà Nội",
    region: "Hà Nội",
    payment_term_days: 15,
    credit_limit: 300000000.0,
    current_balance: 305000000.0,
    credit_status: "CREDIT_HOLD",
    is_active: true,
    created_at: "2026-05-30T11:15:00Z",
  },
];

const INITIAL_SUPPLIERS = [
  {
    id: 1,
    code: "SPL-ASUS-VN",
    company_name: "Công ty TNHH ASUS Việt Nam",
    tax_code: "0102030405",
    phone: "0283999888",
    contact_person: "Nguyễn Văn A (Sale Manager)",
    address: "Tầng 5, Tòa nhà Viettel, Quận 10, TP. HCM",
    is_active: true,
    created_at: "2026-05-30T11:30:00Z",
  },
];

const INITIAL_VEHICLES = [
  {
    id: 1,
    plate_number: "15C-234.56",
    vehicle_type: "Xe tải Hyundai H150 1.5 Tấn",
    max_weight_kg: 1500.0,
    max_volume_m3: 12.5,
    status: "AVAILABLE",
    is_active: true,
  },
  {
    id: 2,
    plate_number: "29C-789.10",
    vehicle_type: "Xe tải Isuzu NPR400 3.5 Tấn",
    max_weight_kg: 3500.0,
    max_volume_m3: 20.0,
    status: "MAINTENANCE",
    is_active: true,
  },
];

const INITIAL_DRIVERS = [
  {
    id: 1,
    user_id: 13,
    full_name: "Nguyễn Văn Tài Xế 1",
    phone: "0904445556",
    license_number: "HP-12345678",
    license_expiry: "2029-12-31",
    status: "AVAILABLE",
    is_active: true,
  },
  {
    id: 2,
    user_id: 14,
    full_name: "Trần Văn Giao Hàng",
    phone: "0905556667",
    license_number: "HN-87654321",
    license_expiry: "2027-06-15",
    status: "ON_DELIVERY",
    is_active: true,
  },
];

// Helper functions for LocalStorage mock database
const getDb = (key, initial) => {
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

const saveDb = (key, data) => {
  localStorage.setItem(key, JSON.stringify(data));
};

const addMockAuditLog = (action, entityType, entityId, details) => {
  const logs = JSON.parse(localStorage.getItem("wms_audit_logs")) || [];
  const currentUser = JSON.parse(localStorage.getItem("wms_user")) || {
    fullName: "System",
  };
  const newLog = {
    id: logs.length + 1,
    actorName: currentUser.fullName,
    action,
    entityType,
    entityId,
    details,
    createdAt: new Date().toISOString(),
  };
  localStorage.setItem("wms_audit_logs", JSON.stringify([newLog, ...logs]));
};

export const masterDataService = {
  // --- PRODUCTS (SKU) ---
  getProducts: async () => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      return getDb(KEYS.PRODUCTS, INITIAL_PRODUCTS);
    }
    const response = await apiClient.get("/products");
    const data = response.data;
    const arrayData = Array.isArray(data) ? data : (data && Array.isArray(data.content) ? data.content : []);
    return mapToSnakeCase(arrayData);
  },

  getProductById: async (id) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      const products = getDb(KEYS.PRODUCTS, INITIAL_PRODUCTS);
      const product = products.find((p) => p.id === Number(id));
      if (!product) throw new Error("PRODUCT_NOT_FOUND");
      return product;
    }
    const response = await apiClient.get(`/products/${id}`);
    return mapToSnakeCase(response.data);
  },

  createProduct: async (productData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 500));
      const products = getDb(KEYS.PRODUCTS, INITIAL_PRODUCTS);

      const exists = products.some(
        (p) =>
          p.sku.trim().toLowerCase() === productData.sku.trim().toLowerCase(),
      );
      if (exists) throw new Error("DUPLICATE_SKU");

      const newProduct = {
        id:
          products.length > 0 ? Math.max(...products.map((p) => p.id)) + 1 : 1,
        sku: productData.sku.trim().toUpperCase(),
        name: productData.name.trim(),
        unit: productData.unit,
        unit_per_pack: Number(productData.unit_per_pack) || 1,
        description: productData.description || "",
        image_url: productData.image_url || "",
        weight_kg: parseFloat(productData.weight_kg) || 0,
        volume_m3: parseFloat(productData.volume_m3) || 0,
        has_serial: !!productData.has_serial,
        reorder_point: parseFloat(productData.reorder_point) || 0,
        is_active: true,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      };

      products.push(newProduct);
      saveDb(KEYS.PRODUCTS, products);
      addMockAuditLog(
        "PRODUCT_CREATED",
        "Product",
        newProduct.id,
        `Tạo mới SKU: ${newProduct.sku} (${newProduct.name})`,
      );
      return newProduct;
    }
    const response = await apiClient.post("/products", mapToCamelCase(productData));
    return mapToSnakeCase(response.data);
  },

  updateProduct: async (id, productData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 500));
      const products = getDb(KEYS.PRODUCTS, INITIAL_PRODUCTS);
      const idx = products.findIndex((p) => p.id === Number(id));
      if (idx === -1) throw new Error("PRODUCT_NOT_FOUND");

      const skuExists = products.some(
        (p) =>
          p.id !== Number(id) &&
          p.sku.trim().toLowerCase() === productData.sku.trim().toLowerCase(),
      );
      if (skuExists) throw new Error("DUPLICATE_SKU");

      const updated = {
        ...products[idx],
        sku: productData.sku.trim().toUpperCase(),
        name: productData.name.trim(),
        unit: productData.unit,
        unit_per_pack: Number(productData.unit_per_pack) || 1,
        description: productData.description || "",
        image_url: productData.image_url || "",
        weight_kg: parseFloat(productData.weight_kg) || 0,
        volume_m3: parseFloat(productData.volume_m3) || 0,
        has_serial: !!productData.has_serial,
        reorder_point: parseFloat(productData.reorder_point) || 0,
        updated_at: new Date().toISOString(),
      };

      products[idx] = updated;
      saveDb(KEYS.PRODUCTS, products);
      addMockAuditLog(
        "PRODUCT_UPDATED",
        "Product",
        id,
        `Cập nhật SKU: ${updated.sku} (${updated.name})`,
      );
      return updated;
    }
    const response = await apiClient.put(`/products/${id}`, mapToCamelCase(productData));
    return mapToSnakeCase(response.data);
  },

  toggleProductStatus: async (id, isActive) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      const products = getDb(KEYS.PRODUCTS, INITIAL_PRODUCTS);
      const idx = products.findIndex((p) => p.id === Number(id));
      if (idx === -1) throw new Error("PRODUCT_NOT_FOUND");

      products[idx].is_active = isActive;
      products[idx].updated_at = new Date().toISOString();
      saveDb(KEYS.KEYS_PRODUCTS || KEYS.PRODUCTS, products);

      const actionName = isActive ? "PRODUCT_ACTIVATED" : "PRODUCT_DEACTIVATED";
      addMockAuditLog(
        actionName,
        "Product",
        id,
        `${isActive ? "Kích hoạt" : "Vô hiệu hóa"} sản phẩm SKU: ${products[idx].sku}`,
      );
      return products[idx];
    }
    if (!isActive) {
      const response = await apiClient.delete(`/products/${id}`);
      return { id, is_active: false };
    } else {
      throw new Error("ACTIVATION_NOT_SUPPORTED_ON_BACKEND");
    }
  },

  // --- WAREHOUSES ---
  getWarehouses: async () => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      return getDb(KEYS.WAREHOUSES, INITIAL_WAREHOUSES);
    }
    const response = await apiClient.get("/admin/warehouses");
    return mapToSnakeCase(response.data);
  },

  createWarehouse: async (whData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const warehouses = getDb(KEYS.WAREHOUSES, INITIAL_WAREHOUSES);

      const codeExists = warehouses.some(
        (w) => w.code.trim().toLowerCase() === whData.code.trim().toLowerCase(),
      );
      if (codeExists) throw new Error("DUPLICATE_WAREHOUSE_CODE");

      const newWh = {
        id:
          warehouses.length > 0
            ? Math.max(...warehouses.map((w) => w.id)) + 1
            : 1,
        code: whData.code.trim().toUpperCase(),
        name: whData.name.trim(),
        address: whData.address || "",
        phone: whData.phone || "",
        manager_id: whData.manager_id ? Number(whData.manager_id) : null,
        type: whData.type || "PHYSICAL",
        is_active: true,
        created_at: new Date().toISOString(),
      };

      warehouses.push(newWh);
      saveDb(KEYS.WAREHOUSES, warehouses);
      addMockAuditLog(
        "WAREHOUSE_CREATED",
        "Warehouse",
        newWh.id,
        `Tạo mới kho: ${newWh.name} (${newWh.code})`,
      );
      return newWh;
    }
    const response = await apiClient.post(
      "/admin/warehouses",
      mapToCamelCase(whData),
    );
    return mapToSnakeCase(response.data);
  },

  updateWarehouse: async (id, whData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const warehouses = getDb(KEYS.WAREHOUSES, INITIAL_WAREHOUSES);
      const idx = warehouses.findIndex((w) => w.id === Number(id));
      if (idx === -1) throw new Error("WAREHOUSE_NOT_FOUND");

      const codeExists = warehouses.some(
        (w) =>
          w.id !== Number(id) &&
          w.code.trim().toLowerCase() === whData.code.trim().toLowerCase(),
      );
      if (codeExists) throw new Error("DUPLICATE_WAREHOUSE_CODE");

      const updated = {
        ...warehouses[idx],
        code: whData.code.trim().toUpperCase(),
        name: whData.name.trim(),
        address: whData.address || "",
        phone: whData.phone || "",
        manager_id: whData.manager_id ? Number(whData.manager_id) : null,
        type: whData.type || "PHYSICAL",
      };

      warehouses[idx] = updated;
      saveDb(KEYS.WAREHOUSES, warehouses);
      addMockAuditLog(
        "WAREHOUSE_UPDATED",
        "Warehouse",
        id,
        `Cập nhật kho: ${updated.name} (${updated.code})`,
      );
      return updated;
    }
    const response = await apiClient.put(
      `/admin/warehouses/${id}`,
      mapToCamelCase(whData),
    );
    return mapToSnakeCase(response.data);
  },

  toggleWarehouseStatus: async (id, isActive) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      const warehouses = getDb(KEYS.WAREHOUSES, INITIAL_WAREHOUSES);
      const idx = warehouses.findIndex((w) => w.id === Number(id));
      if (idx === -1) throw new Error("WAREHOUSE_NOT_FOUND");

      warehouses[idx].is_active = isActive;
      saveDb(KEYS.WAREHOUSES, warehouses);

      const actionName = isActive
        ? "WAREHOUSE_ACTIVATED"
        : "WAREHOUSE_DEACTIVATED";
      addMockAuditLog(
        actionName,
        "Warehouse",
        id,
        `${isActive ? "Kích hoạt" : "Khóa"} kho: ${warehouses[idx].code}`,
      );
      return warehouses[idx];
    }
    if (isActive) {
      const response = await apiClient.put(
        `/admin/warehouses/${id}/reactivate`,
      );
      return mapToSnakeCase(response.data);
    } else {
      await apiClient.delete(`/admin/warehouses/${id}`);
      return { id, is_active: false };
    }
  },

  // --- BIN LOCATIONS ---
  getBinLocations: async (warehouseId) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      const locations = getDb(KEYS.LOCATIONS, INITIAL_LOCATIONS);
      if (warehouseId) {
        return locations.filter((l) => l.warehouse_id === Number(warehouseId));
      }
      return locations;
    }
    const url = warehouseId
      ? `/admin/warehouse-locations?warehouseId=${warehouseId}`
      : "/admin/warehouse-locations";
    const response = await apiClient.get(url);
    return mapToSnakeCase(response.data);
  },

  createBinLocation: async (binData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const locations = getDb(KEYS.LOCATIONS, INITIAL_LOCATIONS);
      const warehouses = getDb(KEYS.WAREHOUSES, INITIAL_WAREHOUSES);

      const wh = warehouses.find((w) => w.id === Number(binData.warehouse_id));
      if (!wh) throw new Error("WAREHOUSE_NOT_FOUND");

      // Auto-generate code: {warehouse_code}.{zone}.{rack}.{shelf}.{bin}
      const code = `${wh.code}.${binData.zone.toUpperCase()}.${binData.rack.toUpperCase()}.${binData.shelf.toUpperCase()}.${binData.bin.toUpperCase()}`;

      const exists = locations.some((l) => l.code === code);
      if (exists) throw new Error("DUPLICATE_BIN_CODE");

      const newBin = {
        id:
          locations.length > 0
            ? Math.max(...locations.map((l) => l.id)) + 1
            : 101,
        warehouse_id: Number(binData.warehouse_id),
        code,
        type: binData.type || "BIN",
        parent_id: binData.parent_id ? Number(binData.parent_id) : null,
        capacity_m3: parseFloat(binData.capacity_m3) || 0,
        capacity_kg: parseFloat(binData.capacity_kg) || 0,
        current_volume_m3: 0.0,
        current_weight_kg: 0.0,
        is_quarantine: !!binData.is_quarantine,
        is_active: true,
      };

      locations.push(newBin);
      saveDb(KEYS.LOCATIONS, locations);
      addMockAuditLog(
        "BIN_CREATED",
        "BinLocation",
        newBin.id,
        `Tạo mới vị trí Bin: ${newBin.code} tại kho ${wh.code}`,
      );
      return newBin;
    }

    // 1. Get all warehouse locations to find or create the parent ZONE
    const listResponse = await apiClient.get(
      `/admin/warehouse-locations?warehouseId=${binData.warehouse_id}`,
    );
    const locations = listResponse.data;

    // Find the warehouse code first
    const whResponse = await apiClient.get(
      `/admin/warehouses/${binData.warehouse_id}`,
    );
    const whCode = whResponse.data.code;
    const expectedZoneCode = `${whCode}.${binData.zone.toUpperCase()}`;

    let zoneId = null;
    const existingZone = locations.find(
      (loc) => loc.type === "ZONE" && loc.code === expectedZoneCode,
    );

    if (existingZone) {
      zoneId = existingZone.id;
    } else {
      // Create the zone first
      const zonePayload = {
        warehouseId: Number(binData.warehouse_id),
        code: binData.zone.toUpperCase(),
        type: "ZONE",
        isQuarantine: false,
      };
      const createdZone = await apiClient.post(
        "/admin/warehouse-locations",
        zonePayload,
      );
      zoneId = createdZone.data.id;
    }

    // 2. Create the bin location under the parent zone
    const binPayload = {
      warehouseId: Number(binData.warehouse_id),
      code: `${binData.rack.toUpperCase()}.${binData.shelf.toUpperCase()}.${binData.bin.toUpperCase()}`,
      type: "BIN",
      parentId: zoneId,
      capacityM3: parseFloat(binData.capacity_m3),
      capacityKg: parseFloat(binData.capacity_kg),
      isQuarantine: !!binData.is_quarantine,
    };

    const response = await apiClient.post(
      "/admin/warehouse-locations",
      binPayload,
    );
    return mapToSnakeCase(response.data);
  },

  updateBinLocation: async (id, binData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const locations = getDb(KEYS.LOCATIONS, INITIAL_LOCATIONS);
      const idx = locations.findIndex((l) => l.id === Number(id));
      if (idx === -1) throw new Error("BIN_NOT_FOUND");

      const oldBin = locations[idx];
      const updatedBin = {
        ...oldBin,
        capacity_m3: parseFloat(binData.capacity_m3) || oldBin.capacity_m3,
        capacity_kg: parseFloat(binData.capacity_kg) || oldBin.capacity_kg,
        is_quarantine:
          binData.is_quarantine !== undefined
            ? !!binData.is_quarantine
            : oldBin.is_quarantine,
      };

      locations[idx] = updatedBin;
      saveDb(KEYS.LOCATIONS, locations);
      addMockAuditLog(
        "BIN_UPDATED",
        "BinLocation",
        id,
        `Cập nhật cấu hình Bin: ${updatedBin.code}`,
      );
      return updatedBin;
    }

    // Get existing location details to construct full request payload
    const existingRes = await apiClient.get(`/admin/warehouse-locations/${id}`);
    const existing = existingRes.data;

    const payload = {
      warehouseId: existing.warehouseId,
      code: existing.code,
      type: existing.type,
      parentId: existing.parentId,
      capacityM3:
        binData.capacity_m3 !== undefined
          ? parseFloat(binData.capacity_m3)
          : existing.capacityM3,
      capacityKg:
        binData.capacity_kg !== undefined
          ? parseFloat(binData.capacity_kg)
          : existing.capacityKg,
      isQuarantine:
        binData.is_quarantine !== undefined
          ? !!binData.is_quarantine
          : existing.isQuarantine,
    };

    const response = await apiClient.put(
      `/admin/warehouse-locations/${id}`,
      payload,
    );
    return mapToSnakeCase(response.data);
  },

  toggleBinStatus: async (id, isActive) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      const locations = getDb(KEYS.LOCATIONS, INITIAL_LOCATIONS);
      const idx = locations.findIndex((l) => l.id === Number(id));
      if (idx === -1) throw new Error("BIN_NOT_FOUND");

      locations[idx].is_active = isActive;
      saveDb(KEYS.LOCATIONS, locations);

      const actionName = isActive ? "BIN_ACTIVATED" : "BIN_DEACTIVATED";
      addMockAuditLog(
        actionName,
        "BinLocation",
        id,
        `${isActive ? "Kích hoạt" : "Khóa"} vị trí Bin: ${locations[idx].code}`,
      );
      return locations[idx];
    }
    if (isActive) {
      const response = await apiClient.put(
        `/admin/warehouse-locations/${id}/reactivate`,
      );
      return mapToSnakeCase(response.data);
    } else {
      await apiClient.delete(`/admin/warehouse-locations/${id}`);
      return { id, is_active: false };
    }
  },

  // --- DEALERS ---
  getDealers: async () => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      return getDb(KEYS.DEALERS, INITIAL_DEALERS);
    }
    const response = await apiClient.get("/dealers");
    return response.data;
  },

  createDealer: async (dlData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const dealers = getDb(KEYS.DEALERS, INITIAL_DEALERS);

      const exists = dealers.some(
        (d) => d.code.trim().toLowerCase() === dlData.code.trim().toLowerCase(),
      );
      if (exists) throw new Error("DUPLICATE_DEALER_CODE");

      const newDl = {
        id: dealers.length > 0 ? Math.max(...dealers.map((d) => d.id)) + 1 : 1,
        code: dlData.code.trim().toUpperCase(),
        name: dlData.name.trim(),
        phone: dlData.phone || "",
        default_delivery_address: dlData.default_delivery_address || "",
        region: dlData.region || "",
        payment_term_days: Number(dlData.payment_term_days) || 30,
        credit_limit: parseFloat(dlData.credit_limit) || 0.0,
        current_balance: 0.0,
        credit_status: "ACTIVE",
        is_active: true,
        created_at: new Date().toISOString(),
      };

      dealers.push(newDl);
      saveDb(KEYS.DEALERS, dealers);
      addMockAuditLog(
        "DEALER_CREATED",
        "Dealer",
        newDl.id,
        `Tạo mới đại lý: ${newDl.name} (${newDl.code})`,
      );
      return newDl;
    }
    const response = await apiClient.post("/dealers", dlData);
    return response.data;
  },

  updateDealer: async (id, dlData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const dealers = getDb(KEYS.DEALERS, INITIAL_DEALERS);
      const idx = dealers.findIndex((d) => d.id === Number(id));
      if (idx === -1) throw new Error("DEALER_NOT_FOUND");

      const updated = {
        ...dealers[idx],
        name: dlData.name.trim(),
        phone: dlData.phone || "",
        default_delivery_address: dlData.default_delivery_address || "",
        region: dlData.region || "",
        payment_term_days: Number(dlData.payment_term_days) || 30,
      };

      dealers[idx] = updated;
      saveDb(KEYS.DEALERS, dealers);
      addMockAuditLog(
        "DEALER_UPDATED",
        "Dealer",
        id,
        `Cập nhật thông tin đại lý: ${updated.name} (${updated.code})`,
      );
      return updated;
    }
    const response = await apiClient.put(`/dealers/${id}`, dlData);
    return response.data;
  },

  updateDealerCreditLimit: async (id, creditLimitData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 500));
      const dealers = getDb(KEYS.DEALERS, INITIAL_DEALERS);
      const idx = dealers.findIndex((d) => d.id === Number(id));
      if (idx === -1) throw new Error("DEALER_NOT_FOUND");

      const oldLimit = dealers[idx].credit_limit;
      const newLimit = parseFloat(creditLimitData.credit_limit);
      const newTerm = Number(creditLimitData.payment_term_days);

      dealers[idx].credit_limit = newLimit;
      if (creditLimitData.payment_term_days !== undefined) {
        dealers[idx].payment_term_days = newTerm;
      }

      // Auto Credit hold check: Balance > limit
      if (dealers[idx].current_balance > newLimit) {
        dealers[idx].credit_status = "CREDIT_HOLD";
      } else {
        dealers[idx].credit_status = "ACTIVE";
      }

      saveDb(KEYS.DEALERS, dealers);
      addMockAuditLog(
        "DEALER_CREDIT_LIMIT_CHANGED",
        "Dealer",
        id,
        `Thay đổi hạn mức nợ Đại lý ${dealers[idx].code} từ ${oldLimit.toLocaleString("vi-VN")} VND sang ${newLimit.toLocaleString("vi-VN")} VND`,
      );
      return dealers[idx];
    }
    const response = await apiClient.put(
      `/dealers/${id}/credit-limit`,
      creditLimitData,
    );
    return response.data;
  },

  toggleDealerStatus: async (id, isActive) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      const dealers = getDb(KEYS.DEALERS, INITIAL_DEALERS);
      const idx = dealers.findIndex((d) => d.id === Number(id));
      if (idx === -1) throw new Error("DEALER_NOT_FOUND");

      dealers[idx].is_active = isActive;
      saveDb(KEYS.DEALERS, dealers);

      const actionName = isActive ? "DEALER_ACTIVATED" : "DEALER_DEACTIVATED";
      addMockAuditLog(
        actionName,
        "Dealer",
        id,
        `${isActive ? "Kích hoạt" : "Khóa"} đối tác Đại lý: ${dealers[idx].code}`,
      );
      return dealers[idx];
    }
    const response = await apiClient.put(`/dealers/${id}/status`, {
      is_active: isActive,
    });
    return response.data;
  },

  // --- SUPPLIERS ---
  getSuppliers: async () => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      return getDb(KEYS.SUPPLIERS, INITIAL_SUPPLIERS);
    }
    const response = await apiClient.get("/suppliers");
    return response.data;
  },

  createSupplier: async (splData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const suppliers = getDb(KEYS.SUPPLIERS, INITIAL_SUPPLIERS);

      const exists = suppliers.some(
        (s) =>
          s.code.trim().toLowerCase() === splData.code.trim().toLowerCase(),
      );
      if (exists) throw new Error("DUPLICATE_SUPPLIER_CODE");

      const newSpl = {
        id:
          suppliers.length > 0
            ? Math.max(...suppliers.map((s) => s.id)) + 1
            : 1,
        code: splData.code.trim().toUpperCase(),
        company_name: splData.company_name.trim(),
        tax_code: splData.tax_code || "",
        phone: splData.phone || "",
        contact_person: splData.contact_person || "",
        address: splData.address || "",
        is_active: true,
        created_at: new Date().toISOString(),
      };

      suppliers.push(newSpl);
      saveDb(KEYS.SUPPLIERS, suppliers);
      addMockAuditLog(
        "SUPPLIER_CREATED",
        "Supplier",
        newSpl.id,
        `Tạo mới nhà cung cấp: ${newSpl.company_name} (${newSpl.code})`,
      );
      return newSpl;
    }
    const response = await apiClient.post("/suppliers", splData);
    return response.data;
  },

  updateSupplier: async (id, splData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const suppliers = getDb(KEYS.SUPPLIERS, INITIAL_SUPPLIERS);
      const idx = suppliers.findIndex((s) => s.id === Number(id));
      if (idx === -1) throw new Error("SUPPLIER_NOT_FOUND");

      const updated = {
        ...suppliers[idx],
        company_name: splData.company_name.trim(),
        tax_code: splData.tax_code || "",
        phone: splData.phone || "",
        contact_person: splData.contact_person || "",
        address: splData.address || "",
      };

      suppliers[idx] = updated;
      saveDb(KEYS.SUPPLIERS, suppliers);
      addMockAuditLog(
        "SUPPLIER_UPDATED",
        "Supplier",
        id,
        `Cập nhật thông tin nhà cung cấp: ${updated.company_name} (${updated.code})`,
      );
      return updated;
    }
    const response = await apiClient.put(`/suppliers/${id}`, splData);
    return response.data;
  },

  toggleSupplierStatus: async (id, isActive) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 300));
      const suppliers = getDb(KEYS.SUPPLIERS, INITIAL_SUPPLIERS);
      const idx = suppliers.findIndex((s) => s.id === Number(id));
      if (idx === -1) throw new Error("SUPPLIER_NOT_FOUND");

      suppliers[idx].is_active = isActive;
      saveDb(KEYS.SUPPLIERS, suppliers);

      const actionName = isActive
        ? "SUPPLIER_ACTIVATED"
        : "SUPPLIER_DEACTIVATED";
      addMockAuditLog(
        actionName,
        "Supplier",
        id,
        `${isActive ? "Kích hoạt" : "Khóa"} đối tác Nhà CC: ${suppliers[idx].code}`,
      );
      return suppliers[idx];
    }
    const response = await apiClient.put(`/suppliers/${id}/status`, {
      is_active: isActive,
    });
    return response.data;
  },

  // --- VEHICLES ---
  getVehicles: async () => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      return getDb(KEYS.VEHICLES, INITIAL_VEHICLES);
    }
    const response = await apiClient.get("/dispatcher/vehicles");
    return mapToSnakeCase(response.data);
  },

  createVehicle: async (vhData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const vehicles = getDb(KEYS.VEHICLES, INITIAL_VEHICLES);

      const exists = vehicles.some(
        (v) =>
          v.plate_number.trim().toLowerCase() ===
          vhData.plate_number.trim().toLowerCase(),
      );
      if (exists) throw new Error("DUPLICATE_PLATE_NUMBER");

      const newVh = {
        id:
          vehicles.length > 0 ? Math.max(...vehicles.map((v) => v.id)) + 1 : 1,
        plate_number: vhData.plate_number.trim().toUpperCase(),
        vehicle_type: vhData.vehicle_type.trim(),
        max_weight_kg: parseFloat(vhData.max_weight_kg) || 0,
        max_volume_m3: parseFloat(vhData.max_volume_m3) || 0,
        status: vhData.status || "AVAILABLE",
        is_active: true,
      };

      vehicles.push(newVh);
      saveDb(KEYS.VEHICLES, vehicles);
      addMockAuditLog(
        "VEHICLE_CREATED",
        "Vehicle",
        newVh.id,
        `Thêm phương tiện mới: ${newVh.plate_number}`,
      );
      return newVh;
    }
    const response = await apiClient.post(
      "/dispatcher/vehicles",
      mapToCamelCase(vhData),
    );
    return mapToSnakeCase(response.data);
  },

  updateVehicle: async (id, vhData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const vehicles = getDb(KEYS.VEHICLES, INITIAL_VEHICLES);
      const idx = vehicles.findIndex((v) => v.id === Number(id));
      if (idx === -1) throw new Error("VEHICLE_NOT_FOUND");

      const plateExists = vehicles.some(
        (v) =>
          v.id !== Number(id) &&
          v.plate_number.trim().toLowerCase() ===
            vhData.plate_number.trim().toLowerCase(),
      );
      if (plateExists) throw new Error("DUPLICATE_PLATE_NUMBER");

      const updated = {
        ...vehicles[idx],
        plate_number: vhData.plate_number.trim().toUpperCase(),
        vehicle_type: vhData.vehicle_type.trim(),
        max_weight_kg:
          parseFloat(vhData.max_weight_kg) || vehicles[idx].max_weight_kg,
        max_volume_m3:
          parseFloat(vhData.max_volume_m3) || vehicles[idx].max_volume_m3,
        status: vhData.status || vehicles[idx].status,
      };

      vehicles[idx] = updated;
      saveDb(KEYS.VEHICLES, vehicles);
      addMockAuditLog(
        "VEHICLE_UPDATED",
        "Vehicle",
        id,
        `Cập nhật phương tiện: ${updated.plate_number} (Trạng thái: ${updated.status})`,
      );
      return updated;
    }
    const response = await apiClient.put(
      `/dispatcher/vehicles/${id}`,
      mapToCamelCase(vhData),
    );
    if (vhData.status) {
      await apiClient.patch(`/dispatcher/vehicles/${id}/status`, {
        status: vhData.status,
      });
      response.data.status = vhData.status;
    }
    return mapToSnakeCase(response.data);
  },

  toggleVehicleStatus: async (id, isActive) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      const vehicles = getDb(KEYS.VEHICLES, INITIAL_VEHICLES);
      const idx = vehicles.findIndex((v) => v.id === Number(id));
      if (idx === -1) throw new Error("VEHICLE_NOT_FOUND");

      vehicles[idx].is_active = isActive;
      saveDb(KEYS.VEHICLES, vehicles);

      const actionName = isActive ? "VEHICLE_ACTIVATED" : "VEHICLE_DEACTIVATED";
      addMockAuditLog(
        actionName,
        "Vehicle",
        id,
        `${isActive ? "Kích hoạt" : "Khóa"} phương tiện: ${vehicles[idx].plate_number}`,
      );
      return vehicles[idx];
    }
    if (isActive) {
      const response = await apiClient.put(
        `/dispatcher/vehicles/${id}/reactivate`,
      );
      return mapToSnakeCase(response.data);
    } else {
      await apiClient.delete(`/dispatcher/vehicles/${id}`);
      return { id, is_active: false };
    }
  },

  // --- DRIVERS ---
  getDrivers: async () => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      return getDb(KEYS.DRIVERS, INITIAL_DRIVERS);
    }
    const response = await apiClient.get("/dispatcher/drivers");
    return mapToSnakeCase(response.data);
  },

  createDriver: async (drvData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const drivers = getDb(KEYS.DRIVERS, INITIAL_DRIVERS);

      const lExists = drivers.some(
        (d) =>
          d.license_number.trim().toLowerCase() ===
          drvData.license_number.trim().toLowerCase(),
      );
      if (lExists) throw new Error("DUPLICATE_LICENSE_NUMBER");

      const newDrv = {
        id: drivers.length > 0 ? Math.max(...drivers.map((d) => d.id)) + 1 : 1,
        user_id: drvData.user_id ? Number(drvData.user_id) : 13, // Default driver user_id link
        full_name: drvData.full_name.trim(),
        phone: drvData.phone || "",
        license_number: drvData.license_number.trim().toUpperCase(),
        license_expiry: drvData.license_expiry,
        status: drvData.status || "AVAILABLE",
        is_active: true,
      };

      drivers.push(newDrv);
      saveDb(KEYS.DRIVERS, drivers);
      addMockAuditLog(
        "DRIVER_CREATED",
        "Driver",
        newDrv.id,
        `Đăng ký tài xế mới: ${newDrv.full_name} (${newDrv.license_number})`,
      );
      return newDrv;
    }
    const response = await apiClient.post(
      "/dispatcher/drivers",
      mapToCamelCase(drvData),
    );
    return mapToSnakeCase(response.data);
  },

  updateDriver: async (id, drvData) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 400));
      const drivers = getDb(KEYS.DRIVERS, INITIAL_DRIVERS);
      const idx = drivers.findIndex((d) => d.id === Number(id));
      if (idx === -1) throw new Error("DRIVER_NOT_FOUND");

      const lExists = drivers.some(
        (d) =>
          d.id !== Number(id) &&
          d.license_number.trim().toLowerCase() ===
            drvData.license_number.trim().toLowerCase(),
      );
      if (lExists) throw new Error("DUPLICATE_LICENSE_NUMBER");

      const updated = {
        ...drivers[idx],
        full_name: drvData.full_name.trim(),
        phone: drvData.phone || "",
        license_number: drvData.license_number.trim().toUpperCase(),
        license_expiry: drvData.license_expiry,
        status: drvData.status || drivers[idx].status,
      };

      drivers[idx] = updated;
      saveDb(KEYS.DRIVERS, drivers);
      addMockAuditLog(
        "DRIVER_UPDATED",
        "Driver",
        id,
        `Cập nhật tài xế: ${updated.full_name} (Trạng thái: ${updated.status})`,
      );
      return updated;
    }
    const response = await apiClient.put(
      `/dispatcher/drivers/${id}`,
      mapToCamelCase(drvData),
    );
    if (drvData.status) {
      await apiClient.patch(`/dispatcher/drivers/${id}/status`, {
        status: drvData.status,
      });
      response.data.status = drvData.status;
    }
    return mapToSnakeCase(response.data);
  },

  toggleDriverStatus: async (id, isActive) => {
    if (useMock) {
      await new Promise((resolve) => setTimeout(resolve, 200));
      const drivers = getDb(KEYS.DRIVERS, INITIAL_DRIVERS);
      const idx = drivers.findIndex((d) => d.id === Number(id));
      if (idx === -1) throw new Error("DRIVER_NOT_FOUND");

      drivers[idx].is_active = isActive;
      saveDb(KEYS.DRIVERS, drivers);

      const actionName = isActive ? "DRIVER_ACTIVATED" : "DRIVER_DEACTIVATED";
      addMockAuditLog(
        actionName,
        "Driver",
        id,
        `${isActive ? "Kích hoạt" : "Khóa"} tài xế: ${drivers[idx].full_name}`,
      );
      return drivers[idx];
    }
    if (isActive) {
      const response = await apiClient.put(
        `/dispatcher/drivers/${id}/reactivate`,
      );
      return mapToSnakeCase(response.data);
    } else {
      await apiClient.delete(`/dispatcher/drivers/${id}`);
      return { id, is_active: false };
    }
  },
};
