import apiClient, { useMock } from './api.client';

const KEYS = {
  TRANSFERS: 'wms_db_transfers',
};

const today = () => new Date().toISOString().slice(0, 10);
const toDateOnly = (value) => (value ? String(value).slice(0, 10) : null);

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

const normalizeTransferTiming = (transfer) => {
  const plannedStartAt = transfer.tripPlannedStartAt || transfer.trip?.plannedStartAt || null;
  const plannedEndAt = transfer.tripPlannedEndAt || transfer.trip?.plannedEndAt || null;
  const normalized = {
    ...transfer,
    plannedDate: transfer.plannedDate || toDateOnly(plannedStartAt) || transfer.documentDate,
    tripPlannedStartAt: plannedStartAt,
    tripPlannedEndAt: plannedEndAt,
  };

  if (!plannedEndAt || !normalized.tripId) {
    return normalized;
  }

  const now = new Date();
  const plannedEnd = new Date(plannedEndAt);
  if (normalized.status === 'APPROVED' && now > plannedEnd) {
    return {
      ...normalized,
      status: 'CANCELLED',
      rejectionReason: normalized.rejectionReason || 'AUTO_CANCELLED_TRANSFER_OVERDUE',
      tripOverdue: true,
      tripWarningActive: true,
      tripWarningMessage: 'Chuyến đã quá hạn hoàn thành.',
    };
  }

  const warningStart = new Date(plannedEnd.getTime() - (3 * 24 * 60 * 60 * 1000));
  if (now >= warningStart && !String(normalized.status || '').startsWith('COMPLETED') && normalized.status !== 'CANCELLED') {
    return {
      ...normalized,
      tripWarningActive: true,
      tripOverdue: false,
      tripWarningMessage: 'Chuyến đang ở 3 ngày cuối trước hạn giao.',
    };
  }

  return {
    ...normalized,
    tripWarningActive: false,
    tripOverdue: false,
    tripWarningMessage: null,
  };
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

export const interWarehouseTransferService = {
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
      return readMockTransfers().map(normalizeTransferTiming);
    }
    const response = await apiClient.get('/inter-warehouse-transfers');
    return response.data;
  },

  getTransferById: async (id) => {
    if (useMock) {
      const transfer = readMockTransfers().find((item) => item.id === Number(id));
      if (!transfer) throw new Error('TRANSFER_NOT_FOUND');
      return normalizeTransferTiming(transfer);
    }
    const response = await apiClient.get(`/inter-warehouse-transfers/${id}`);
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
    const response = await apiClient.post('/inter-warehouse-transfers', payload);
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
    const response = await apiClient.put(`/inter-warehouse-transfers/${id}`, payload);
    return response.data;
  },

  cancelTransfer: async (id, reason) => {
    if (useMock) return updateMockStatus(id, 'CANCELLED', { rejectionReason: reason });
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/cancel`, { reason });
    return response.data;
  },

  approveTransfer: async (id) => {
    if (useMock) return updateMockStatus(id, 'APPROVED');
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/approve`);
    return response.data;
  },

  rejectTransfer: async (id, reason) => {
    if (useMock) return updateMockStatus(id, 'REJECTED', { rejectionReason: reason });
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/reject`, { reason });
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
        plannedDate: toDateOnly(payload.plannedStartAt),
        tripPlannedStartAt: payload.plannedStartAt,
        tripPlannedEndAt: payload.plannedEndAt,
        trip: payload,
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/trip`, payload);
    return response.data;
  },

  shipTransfer: async (id) => {
    if (useMock) {
      const transfer = await interWarehouseTransferService.getTransferById(id);
      if (!transfer.outboundQcPassed && !transfer.outbound_qc_passed) {
        throw new Error('OUTBOUND_QC_NOT_PASSED');
      }
      return updateMockStatus(id, 'APPROVED', {
        items: transfer.items.map((item) => ({ ...item, sentQty: item.plannedQty })),
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/ship`);
    return response.data;
  },

  unshipTransfer: async (id) => {
    if (useMock) {
      const transfer = await interWarehouseTransferService.getTransferById(id);
      return updateMockStatus(id, 'APPROVED', {
        items: transfer.items.map((item) => ({ ...item, sentQty: null })),
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/unship`);
    return response.data;
  },

  departTransfer: async (id) => {
    if (useMock) {
      const transfer = await interWarehouseTransferService.getTransferById(id);
      const allItemsLoaded = transfer.items?.length > 0 && transfer.items.every(
        (item) => item.sentQty != null && Number(item.sentQty) === Number(item.plannedQty)
      );
      if (!allItemsLoaded) throw new Error('SENT_QTY_REQUIRED');
      return updateMockStatus(id, 'IN_TRANSIT');
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/depart`);
    return response.data;
  },

  receiveCount: async (id, items) => {
    if (useMock) {
      const transfer = await interWarehouseTransferService.getTransferById(id);
      const byId = Object.fromEntries(items.map((item) => [Number(item.transferItemId), item]));
      return updateMockStatus(id, 'IN_TRANSIT', {
        items: transfer.items.map((item) => ({
          ...item,
          workerReceivedQty: byId[item.id]?.receivedQty ?? item.workerReceivedQty,
          issueReason: byId[item.id]?.issueReason ?? item.issueReason,
        })),
      });
    }
    const response = await apiClient.put(`/inter-warehouse-transfers/${id}/receive-count`, { items });
    return response.data;
  },

  receiveCheck: async (id, items) => {
    if (useMock) {
      const transfer = await interWarehouseTransferService.getTransferById(id);
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
    const response = await apiClient.put(`/inter-warehouse-transfers/${id}/receive-check`, { items });
    return response.data;
  },

  finalReceive: async (id, discrepancyReason) => {
    if (useMock) {
      const transfer = await interWarehouseTransferService.getTransferById(id);
      const hasShortage = transfer.items.some((item) => Number(item.receivedQty) < Number(item.sentQty));
      return updateMockStatus(id, hasShortage ? 'COMPLETED_WITH_DISCREPANCY' : 'COMPLETED', {
        discrepancyReason,
        actualReceivedDate: today(),
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/final-receive`, { discrepancyReason });
    return response.data;
  },

  returnToSource: async (id) => {
    if (useMock) {
      return updateMockStatus(id, 'IN_TRANSIT', { isReturned: true });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/return-to-source`);
    return response.data;
  },

  quarantineReject: async (id, reason) => {
    if (useMock) {
      const transfer = await interWarehouseTransferService.getTransferById(id);
      const qty = (item) => item.sentQty ?? item.plannedQty ?? 0;
      return updateMockStatus(id, 'QUARANTINED', {
        rejectionReason: reason,
        items: transfer.items.map((item) => ({
          ...item,
          receivedQty: qty(item),
          qcPassedQty: 0,
          qcFailedQty: qty(item),
          qcFailureReason: reason,
          checkerNote: reason,
        })),
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/quarantine-reject`, { rejectionReason: reason });
    return response.data;
  },

  uploadPhotoEvidence: async (id, file) => {
    if (useMock) {
      return {
        photoRef: `/mock/uploads/transfer/${Date.now()}-${file?.name || 'photo.jpg'}`,
      };
    }
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/photo-evidence`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000,
    });
    return response.data;
  },

  recordOutboundQc: async (id, payload) => {
    const uploaded = payload.photoFile
      ? await interWarehouseTransferService.uploadPhotoEvidence(id, payload.photoFile)
      : null;
    const request = {
      passed: payload.passed ?? payload.outboundQcPassed,
      note: payload.note ?? payload.outboundQcNote ?? '',
      photoRef: uploaded?.photoRef ?? payload.photoRef ?? payload.outboundQcPhotoRef,
    };
    if (useMock) {
      return updateMockStatus(id, 'APPROVED', {
        outboundQcPassed: request.passed,
        outboundQcNote: request.note,
        outboundQcPhotoRef: request.photoRef,
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/outbound-qc`, request);
    return response.data;
  },

  loadHandover: async (id, payload) => {
    const uploaded = payload.photoFile
      ? await interWarehouseTransferService.uploadPhotoEvidence(id, payload.photoFile)
      : null;
    const request = {
      photoRef: uploaded?.photoRef || payload.photoRef || payload.loadHandoverPhotoRef,
    };
    if (useMock) {
      return updateMockStatus(id, 'APPROVED', {
        loadHandoverPhotoRef: request.photoRef || null,
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/load-handover`, request);
    return response.data;
  },

  driverArrive: async (id) => {
    if (useMock) {
      return updateMockStatus(id, 'IN_TRANSIT', {
        driverArrivedAt: new Date().toISOString(),
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/driver-arrive`);
    return response.data;
  },

  receivingHandover: async (id, payload) => {
    const uploaded = payload.photoFile
      ? await interWarehouseTransferService.uploadPhotoEvidence(id, payload.photoFile)
      : null;
    const request = {
      ...payload,
      photoRef: uploaded?.photoRef || payload.photoRef,
    };
    if (useMock) {
      return updateMockStatus(id, 'IN_TRANSIT', {
        arrivalHandoverAt: new Date().toISOString(),
        arrivalHandoverPhotoRef: request.photoRef || null,
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/receiving-handover`, request);
    return response.data;
  },

  requestReturn: async (id, payload) => {
    if (useMock) {
      const transfers = readMockTransfers();
      const index = transfers.findIndex((t) => t.id === Number(id));
      if (index !== -1) {
        transfers[index].returnRequested = true;
        transfers[index].returnReason = payload.reason;
        transfers[index].wrongSkuItems = payload.wrongSkuItems || [];
        writeMockTransfers(transfers);
        return transfers[index];
      }
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/request-return`, payload);
    return response.data;
  },

  approveReturn: async (id) => {
    if (useMock) {
      const transfers = readMockTransfers();
      const index = transfers.findIndex((t) => t.id === Number(id));
      if (index !== -1) {
        transfers[index].returnRequested = false;
        transfers[index].isReturned = true;
        writeMockTransfers(transfers);
        return transfers[index];
      }
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/approve-return`);
    return response.data;
  },

  rejectReturn: async (id, reason) => {
    if (useMock) {
      const transfers = readMockTransfers();
      const index = transfers.findIndex((t) => t.id === Number(id));
      if (index !== -1) {
        transfers[index].returnRequested = false;
        transfers[index].returnRejectionReason = reason;
        writeMockTransfers(transfers);
        return transfers[index];
      }
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/reject-return`, { reason });
    return response.data;
  },

  returnDepart: async (id) => {
    if (useMock) {
      return updateMockStatus(id, 'IN_TRANSIT', {
        returnDepartedAt: new Date().toISOString(),
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/return-depart`);
    return response.data;
  },

  returnArrive: async (id) => {
    if (useMock) {
      return updateMockStatus(id, 'IN_TRANSIT', {
        returnArrivedAt: new Date().toISOString(),
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/return-arrive`);
    return response.data;
  },

  returnHandover: async (id, payload) => {
    const uploaded = payload.photoFile
      ? await interWarehouseTransferService.uploadPhotoEvidence(id, payload.photoFile)
      : null;
    const request = {
      ...payload,
      photoRef: uploaded?.photoRef || payload.photoRef,
    };
    if (useMock) {
      return updateMockStatus(id, 'IN_TRANSIT', {
        returnArrivalHandoverAt: new Date().toISOString(),
        returnPhotoRef: request.photoRef || null,
      });
    }
    const response = await apiClient.post(`/inter-warehouse-transfers/${id}/return-handover`, request);
    return response.data;
  },

  // --- TRANSFER REQUESTS (US4) ---
  getTransferRequests: async () => {
    if (useMock) {
      const raw = localStorage.getItem('wms_db_transfer_requests');
      return raw ? JSON.parse(raw) : [];
    }
    const response = await apiClient.get('/transfer-requests');
    return response.data;
  },

  getTransferRequestById: async (id) => {
    if (useMock) {
      const requests = JSON.parse(localStorage.getItem('wms_db_transfer_requests') || '[]');
      return requests.find(r => r.id === Number(id));
    }
    const response = await apiClient.get(`/transfer-requests/${id}`);
    return response.data;
  },

  createTransferRequest: async (payload) => {
    if (useMock) {
      const requests = JSON.parse(localStorage.getItem('wms_db_transfer_requests') || '[]');
      const id = nextId(requests);
      const newRequest = {
        ...payload,
        id,
        requestNumber: `TRQ-${today().replaceAll('-', '')}-${String(id).padStart(4, '0')}`,
        status: 'DRAFT',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      requests.push(newRequest);
      localStorage.setItem('wms_db_transfer_requests', JSON.stringify(requests));
      return newRequest;
    }
    const response = await apiClient.post('/transfer-requests', payload);
    return response.data;
  },

  updateTransferRequest: async (id, payload) => {
    if (useMock) {
      const requests = JSON.parse(localStorage.getItem('wms_db_transfer_requests') || '[]');
      const idx = requests.findIndex(r => r.id === Number(id));
      if (idx !== -1) {
        requests[idx] = { ...requests[idx], ...payload, updatedAt: new Date().toISOString() };
        localStorage.setItem('wms_db_transfer_requests', JSON.stringify(requests));
        return requests[idx];
      }
    }
    const response = await apiClient.put(`/transfer-requests/${id}`, payload);
    return response.data;
  },

  cancelTransferRequest: async (id) => {
    if (useMock) {
      const requests = JSON.parse(localStorage.getItem('wms_db_transfer_requests') || '[]');
      const idx = requests.findIndex(r => r.id === Number(id));
      if (idx !== -1) {
        requests[idx].status = 'CANCELLED';
        requests[idx].updatedAt = new Date().toISOString();
        localStorage.setItem('wms_db_transfer_requests', JSON.stringify(requests));
        return requests[idx];
      }
    }
    const response = await apiClient.post(`/transfer-requests/${id}/cancel`);
    return response.data;
  },

  submitTransferRequest: async (id) => {
    if (useMock) {
      const requests = JSON.parse(localStorage.getItem('wms_db_transfer_requests') || '[]');
      const idx = requests.findIndex(r => r.id === Number(id));
      if (idx !== -1) {
        requests[idx].status = 'SUBMITTED';
        requests[idx].submittedAt = new Date().toISOString();
        localStorage.setItem('wms_db_transfer_requests', JSON.stringify(requests));
        return requests[idx];
      }
    }
    const response = await apiClient.post(`/transfer-requests/${id}/submit`);
    return response.data;
  },

  approveTransferRequest: async (id) => {
    if (useMock) {
      const requests = JSON.parse(localStorage.getItem('wms_db_transfer_requests') || '[]');
      const idx = requests.findIndex(r => r.id === Number(id));
      if (idx !== -1) {
        requests[idx].status = 'APPROVED';
        requests[idx].approvedAt = new Date().toISOString();
        localStorage.setItem('wms_db_transfer_requests', JSON.stringify(requests));
        return requests[idx];
      }
    }
    const response = await apiClient.post(`/transfer-requests/${id}/approve`);
    return response.data;
  },

  rejectTransferRequest: async (id, reason) => {
    if (useMock) {
      const requests = JSON.parse(localStorage.getItem('wms_db_transfer_requests') || '[]');
      const idx = requests.findIndex(r => r.id === Number(id));
      if (idx !== -1) {
        requests[idx].status = 'REJECTED';
        requests[idx].rejectionReason = reason;
        requests[idx].rejectedAt = new Date().toISOString();
        localStorage.setItem('wms_db_transfer_requests', JSON.stringify(requests));
        return requests[idx];
      }
    }
    const response = await apiClient.post(`/transfer-requests/${id}/reject`, { reason });
    return response.data;
  },

  convertTransferRequest: async (id) => {
    if (useMock) {
      const requests = JSON.parse(localStorage.getItem('wms_db_transfer_requests') || '[]');
      const idx = requests.findIndex(r => r.id === Number(id));
      if (idx !== -1) {
        requests[idx].status = 'CONVERTED';
        requests[idx].convertedTransferId = requests[idx].convertedTransferId || Date.now();
        requests[idx].convertedTransferNumber = requests[idx].convertedTransferNumber || `TRF-${today().replaceAll('-', '')}-${String(id).padStart(4, '0')}`;
        requests[idx].convertedAt = new Date().toISOString();
        localStorage.setItem('wms_db_transfer_requests', JSON.stringify(requests));
        return requests[idx];
      }
    }
    const response = await apiClient.post(`/transfer-requests/${id}/convert`);
    return response.data;
  },

  stockLookup: async (productId) => {
    if (useMock) {
      const inventories = readMockInventories();
      // Group available qty by warehouse
      const warehouses = JSON.parse(localStorage.getItem('wms_db_warehouses') || '[]');
      return warehouses
        .filter(w => w.type !== 'IN_TRANSIT')
        .map(w => {
          const rows = inventories.filter(i => Number(i.warehouse_id) === Number(w.id) && Number(i.product_id) === Number(productId));
          const available = rows.reduce((sum, item) => sum + (Number(item.total_qty) - Number(item.reserved_qty)), 0);
          return {
            warehouseId: w.id,
            warehouseName: w.name,
            availableQty: available,
          };
        });
    }
    const response = await apiClient.get('/transfer-requests/stock-lookup', { params: { productId } });
    return response.data;
  },
};
