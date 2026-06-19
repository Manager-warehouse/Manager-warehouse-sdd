import apiClient, { useMock } from './api.client';

const KEYS = {
  TRANSFERS: 'wms_db_transfers',
};

const today = () => new Date().toISOString().slice(0, 10);

const initialTransfers = [
  {
    id: 1,
    transferNumber: 'TRF-20260616-0001',
    externalInstructionCode: 'CTM-DC-20260616-01',
    sourceWarehouseId: 1,
    sourceWarehouseCode: 'HP-01',
    destinationWarehouseId: 2,
    destinationWarehouseCode: 'HN-01',
    status: 'NEW',
    documentDate: today(),
    plannedDate: today(),
    notes: 'Điều chuyển thủ công theo lệnh công ty mẹ',
    items: [
      {
        id: 11,
        productId: 1,
        productSku: 'SKU-PA-001',
        productName: 'Màn hình ASUS ProArt PA278CV',
        plannedQty: 10,
      },
    ],
  },
];

const readMockTransfers = () => {
  const raw = localStorage.getItem(KEYS.TRANSFERS);
  if (!raw) {
    localStorage.setItem(KEYS.TRANSFERS, JSON.stringify(initialTransfers));
    return initialTransfers;
  }
  return JSON.parse(raw);
};

const writeMockTransfers = (transfers) => {
  localStorage.setItem(KEYS.TRANSFERS, JSON.stringify(transfers));
};

const nextId = (rows) => (rows.length ? Math.max(...rows.map((row) => row.id)) + 1 : 1);

const readMockInventories = () => {
  const raw = localStorage.getItem('wms_db_inventories');
  if (!raw) return [];
  try {
    return JSON.parse(raw);
  } catch (error) {
    return [];
  }
};

const readMockDrivers = () => {
  const raw = localStorage.getItem('wms_db_drivers');
  if (!raw) return [];
  try {
    return JSON.parse(raw);
  } catch (error) {
    return [];
  }
};

const updateMockStatus = async (id, status, patch = {}) => {
  const transfers = readMockTransfers();
  const index = transfers.findIndex((transfer) => transfer.id === Number(id));
  if (index === -1) throw new Error('TRANSFER_NOT_FOUND');
  transfers[index] = { ...transfers[index], status, ...patch };
  writeMockTransfers(transfers);
  return transfers[index];
};

export const transferService = {
  getAvailability: async (warehouseId, productId) => {
    if (useMock) {
      const rows = readMockInventories().filter((item) =>
        Number(item.warehouse_id ?? item.warehouseId) === Number(warehouseId)
        && Number(item.product_id ?? item.productId) === Number(productId)
      );
      const totalQty = rows.reduce((sum, item) => sum + Number(item.total_qty ?? item.totalQty ?? 0), 0);
      const reservedQty = rows.reduce((sum, item) => sum + Number(item.reserved_qty ?? item.reservedQty ?? 0), 0);
      return {
        warehouseId: Number(warehouseId),
        productId: Number(productId),
        totalQty,
        reservedQty,
        availableQty: totalQty - reservedQty,
      };
    }
    const response = await apiClient.get('/warehouse-stock/availability', {
      params: { warehouseId, productId },
    });
    return response.data;
  },

  getTransfers: async () => {
    if (useMock) {
      return readMockTransfers();
    }
    const response = await apiClient.get('/transfers');
    return response.data;
  },

  getTransferById: async (id) => {
    if (useMock) {
      const transfer = readMockTransfers().find((item) => item.id === Number(id));
      if (!transfer) throw new Error('TRANSFER_NOT_FOUND');
      return transfer;
    }
    const response = await apiClient.get(`/transfers/${id}`);
    return response.data;
  },

  createTransfer: async (payload) => {
    if (useMock) {
      const transfers = readMockTransfers();
      const id = nextId(transfers);
      const transfer = {
        ...payload,
        id,
        transferNumber: `TRF-${today().replaceAll('-', '')}-${String(id).padStart(4, '0')}`,
        sourceWarehouseCode: payload.sourceWarehouseCode,
        destinationWarehouseCode: payload.destinationWarehouseCode,
        status: 'NEW',
        items: payload.items.map((item, index) => ({
          ...item,
          id: id * 100 + index + 1,
          sentQty: null,
          workerReceivedQty: null,
          receivedQty: null,
          qcPassedQty: null,
          qcFailedQty: null,
        })),
      };
      writeMockTransfers([transfer, ...transfers]);
      return transfer;
    }
    const response = await apiClient.post('/transfers', payload);
    return response.data;
  },

  updateTransfer: async (id, payload) => {
    if (useMock) {
      const transfers = readMockTransfers();
      const index = transfers.findIndex((transfer) => transfer.id === Number(id));
      if (index === -1) throw new Error('TRANSFER_NOT_FOUND');
      if (transfers[index].status !== 'NEW') throw new Error('INVALID_TRANSFER_STATUS');
      transfers[index] = { ...transfers[index], ...payload };
      writeMockTransfers(transfers);
      return transfers[index];
    }
    const response = await apiClient.put(`/transfers/${id}`, payload);
    return response.data;
  },

  cancelTransfer: async (id, reason) => {
    if (useMock) return updateMockStatus(id, 'CANCELLED', { rejectionReason: reason });
    const response = await apiClient.post(`/transfers/${id}/cancel`, { reason });
    return response.data;
  },

  approveTransfer: async (id) => {
    if (useMock) return updateMockStatus(id, 'APPROVED');
    const response = await apiClient.post(`/transfers/${id}/approve`);
    return response.data;
  },

  rejectTransfer: async (id, reason) => {
    if (useMock) return updateMockStatus(id, 'REJECTED', { rejectionReason: reason });
    const response = await apiClient.post(`/transfers/${id}/reject`, { reason });
    return response.data;
  },

  assignTrip: async (id, payload) => {
    if (useMock) {
      const driver = readMockDrivers().find((item) => Number(item.id) === Number(payload.driverId));
      return updateMockStatus(id, 'APPROVED', {
        tripId: Number(id) * 10,
        tripNumber: `TTR-${today().replaceAll('-', '')}-${String(id).padStart(4, '0')}`,
        driverId: payload.driverId,
        driverUserId: driver?.userId ?? driver?.user_id ?? null,
        driverName: driver?.fullName ?? driver?.full_name ?? null,
        trip: payload,
      });
    }
    const response = await apiClient.post(`/transfers/${id}/trip`, payload);
    return response.data;
  },

  shipTransfer: async (id) => {
    if (useMock) {
      const transfer = await transferService.getTransferById(id);
      return updateMockStatus(id, 'APPROVED', {
        items: transfer.items.map((item) => ({ ...item, sentQty: item.plannedQty })),
      });
    }
    const response = await apiClient.post(`/transfers/${id}/ship`);
    return response.data;
  },

  unshipTransfer: async (id) => {
    if (useMock) {
      const transfer = await transferService.getTransferById(id);
      return updateMockStatus(id, 'APPROVED', {
        items: transfer.items.map((item) => ({ ...item, sentQty: null })),
      });
    }
    const response = await apiClient.post(`/transfers/${id}/unship`);
    return response.data;
  },

  departTransfer: async (id) => {
    if (useMock) {
      const transfer = await transferService.getTransferById(id);
      const allItemsLoaded = transfer.items?.length > 0 && transfer.items.every(
        (item) => item.sentQty != null && Number(item.sentQty) === Number(item.plannedQty)
      );
      if (!allItemsLoaded) throw new Error('SENT_QTY_REQUIRED');
      return updateMockStatus(id, 'IN_TRANSIT');
    }
    const response = await apiClient.post(`/transfers/${id}/depart`);
    return response.data;
  },

  receiveCount: async (id, items) => {
    if (useMock) {
      const transfer = await transferService.getTransferById(id);
      const byId = Object.fromEntries(items.map((item) => [Number(item.transferItemId), item]));
      return updateMockStatus(id, 'IN_TRANSIT', {
        items: transfer.items.map((item) => ({
          ...item,
          workerReceivedQty: byId[item.id]?.receivedQty ?? item.workerReceivedQty,
          issueReason: byId[item.id]?.issueReason ?? item.issueReason,
        })),
      });
    }
    const response = await apiClient.put(`/transfers/${id}/receive-count`, { items });
    return response.data;
  },

  receiveCheck: async (id, items) => {
    if (useMock) {
      const transfer = await transferService.getTransferById(id);
      const byId = Object.fromEntries(items.map((item) => [Number(item.transferItemId), item]));
      return updateMockStatus(id, 'IN_TRANSIT', {
        items: transfer.items.map((item) => ({
          ...item,
          receivedQty: byId[item.id]?.confirmedQty ?? item.receivedQty,
          qcPassedQty: byId[item.id]?.qcPassedQty ?? item.qcPassedQty,
          qcFailedQty: byId[item.id]?.qcFailedQty ?? item.qcFailedQty,
          checkerNote: byId[item.id]?.checkerNote ?? item.checkerNote,
          qcFailureReason: byId[item.id]?.qcFailureReason ?? item.qcFailureReason,
          destinationLocationId: byId[item.id]?.destinationLocationId ?? item.destinationLocationId,
          varianceQty: (byId[item.id]?.confirmedQty ?? 0) - (item.sentQty ?? 0),
        })),
      });
    }
    const response = await apiClient.put(`/transfers/${id}/receive-check`, { items });
    return response.data;
  },

  finalReceive: async (id, discrepancyReason) => {
    if (useMock) {
      const transfer = await transferService.getTransferById(id);
      const hasShortage = transfer.items.some((item) => Number(item.receivedQty) < Number(item.sentQty));
      return updateMockStatus(id, hasShortage ? 'COMPLETED_WITH_DISCREPANCY' : 'COMPLETED', {
        discrepancyReason,
        actualReceivedDate: today(),
      });
    }
    const response = await apiClient.post(`/transfers/${id}/final-receive`, { discrepancyReason });
    return response.data;
  },
};
