/**
 * Inter-Warehouse Transfer Validation Tests
 *
 * Covers validation logic in InterWarehouseTransferWorkspace.jsx and InterWarehouseTransferActionPanel.jsx.
 */

// --- Validators extracted from the components ---

const validateCreateTransfer = (form, warehouses, products, availabilityByLine) => {
  const source = warehouses.find((w) => w.id === Number(form.sourceWarehouseId));
  const destination = warehouses.find((w) => w.id === Number(form.destinationWarehouseId));

  if (!form.sourceWarehouseId) {
    return { isValid: false, error: 'Vui lòng chọn kho nguồn' };
  }
  if (!form.destinationWarehouseId) {
    return { isValid: false, error: 'Vui lòng chọn kho đích' };
  }
  if (source && destination && source.id === destination.id) {
    return { isValid: false, error: 'Kho nguồn và kho đích phải khác nhau' };
  }
  if (!form.items || !form.items.length) {
    return { isValid: false, error: 'Phiếu điều chuyển cần ít nhất một dòng hàng' };
  }

  for (let index = 0; index < form.items.length; index++) {
    const item = form.items[index];
    const product = products.find((p) => p.id === Number(item.productId));
    if (!item.productId) {
      return { isValid: false, error: 'Vui lòng chọn sản phẩm cho tất cả dòng hàng' };
    }
    if (Number(item.plannedQty) <= 0) {
      return { isValid: false, error: 'Số lượng đặt phải lớn hơn 0' };
    }

    const availability = availabilityByLine[index];
    if (availability && !availability.error && Number(item.plannedQty) > Number(availability.availableQty ?? 0)) {
      return { isValid: false, error: `Số lượng đặt của ${product ? product.sku : 'sản phẩm'} vượt tồn khả dụng` };
    }
  }

  return { isValid: true };
};

const validateTripAssignment = (trip, driver, sourceWarehouseId) => {
  if (!trip.vehicleId) {
    return { isValid: false, error: 'Vui lòng chọn xe' };
  }
  if (!trip.driverId) {
    return { isValid: false, error: 'Vui lòng chọn tài xế' };
  }
  if (!trip.plannedStartAt || !trip.plannedEndAt) {
    return { isValid: false, error: 'Vui lòng nhập đầy đủ thời gian' };
  }

  const start = new Date(trip.plannedStartAt);
  const end = new Date(trip.plannedEndAt);

  if (start >= end) {
    return { isValid: false, error: 'Thời gian kết thúc phải sau thời gian bắt đầu' };
  }

  const driverWhIds = driver.warehouse_ids || driver.warehouseIds || [];
  if (!driverWhIds.map(Number).includes(Number(sourceWarehouseId))) {
    return { isValid: false, error: 'Tài xế phải thuộc phạm vi kho nguồn' };
  }

  return { isValid: true };
};

const validateReceiveCheck = (rows, locations) => {
  for (const row of rows) {
    if (Number(row.confirmedQty) < 0 || Number(row.qcPassedQty) < 0 || Number(row.qcFailedQty) < 0) {
      return { isValid: false, error: 'Số lượng không được âm' };
    }

    const totalQc = Number(row.qcPassedQty) + Number(row.qcFailedQty);
    if (totalQc !== Number(row.confirmedQty)) {
      return { isValid: false, error: 'Tổng số lượng đạt và lỗi phải bằng số lượng chốt' };
    }

    if (Number(row.qcFailedQty) > 0 && !row.qcFailureReason?.trim()) {
      return { isValid: false, error: 'Vui lòng nhập lý do QC lỗi' };
    }

    if (Number(row.qcPassedQty) > 0) {
      if (!row.destinationLocationId) {
        return { isValid: false, error: 'Vui lòng chọn vị trí nhập kho cho hàng đạt' };
      }
      const bin = locations.find(l => l.id === Number(row.destinationLocationId));
      if (bin && (bin.isQuarantine || bin.is_quarantine)) {
        return { isValid: false, error: 'Vị trí hàng đạt không được là bin quarantine' };
      }
    }
  }
  return { isValid: true };
};

const validateOutboundQc = (payload) => {
  if (payload.outboundQcPassed === undefined || payload.outboundQcPassed === null) {
    return { isValid: false, error: 'Vui lòng chọn kết quả QC' };
  }
  if (!payload.outboundQcPhotoRef || !payload.outboundQcPhotoRef.trim()) {
    return { isValid: false, error: 'Vui lòng nhập link ảnh QC' };
  }
  return { isValid: true };
};

const validateLoadHandover = (payload) => {
  if (!payload.loadHandoverPhotoRef || !payload.loadHandoverPhotoRef.trim()) {
    return { isValid: false, error: 'Vui lòng nhập link ảnh bàn giao' };
  }
  return { isValid: true };
};

const validateReceivingHandover = (payload) => {
  if (!payload.photoRef || !payload.photoRef.trim()) {
    return { isValid: false, error: 'Vui lòng nhập link ảnh bàn giao xe' };
  }
  return { isValid: true };
};

const validateWrongSkuReport = (payload) => {
  if (!payload.reason || !payload.reason.trim()) {
    return { isValid: false, error: 'Vui lòng nhập lý do chung' };
  }
  if (!payload.wrongSkuItems || payload.wrongSkuItems.length === 0) {
    return { isValid: false, error: 'Vui lòng thêm ít nhất 1 dòng hàng sai SKU' };
  }
  for (const item of payload.wrongSkuItems) {
    if (!item.transferItemId) {
      return { isValid: false, error: 'Thiếu thông tin dòng hàng' };
    }
    if (!item.actualProductSku || !item.actualProductSku.trim()) {
      return { isValid: false, error: 'Vui lòng nhập SKU thực tế' };
    }
    if (!item.quantity || Number(item.quantity) <= 0) {
      return { isValid: false, error: 'Số lượng sai phải lớn hơn 0' };
    }
    if (!item.reason || !item.reason.trim()) {
      return { isValid: false, error: 'Vui lòng nhập lý do dòng hàng' };
    }
  }
  return { isValid: true };
};

// --- Test Suite ---

describe('Inter-Warehouse Transfer Frontend Validations', () => {
  const MOCK_WAREHOUSES = [
    { id: 1, code: 'HP-01', name: 'Hải Phòng' },
    { id: 2, code: 'HN-01', name: 'Hà Nội' }
  ];

  const MOCK_PRODUCTS = [
    { id: 100, sku: 'PAN-001', name: 'Chảo chống dính' }
  ];

  describe('validateCreateTransfer', () => {
    test('passes with valid parameters', () => {
      const form = {
        sourceWarehouseId: '1',
        destinationWarehouseId: '2',
        items: [{ productId: '100', plannedQty: 5 }]
      };
      const availabilityByLine = {
        0: { availableQty: 10 }
      };

      const result = validateCreateTransfer(form, MOCK_WAREHOUSES, MOCK_PRODUCTS, availabilityByLine);
      expect(result.isValid).toBe(true);
    });

    test('fails when source warehouse is missing', () => {
      const form = {
        sourceWarehouseId: '',
        destinationWarehouseId: '2',
        items: [{ productId: '100', plannedQty: 5 }]
      };
      const result = validateCreateTransfer(form, MOCK_WAREHOUSES, MOCK_PRODUCTS, {});
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Vui lòng chọn kho nguồn');
    });

    test('fails when destination warehouse is missing', () => {
      const form = {
        sourceWarehouseId: '1',
        destinationWarehouseId: '',
        items: [{ productId: '100', plannedQty: 5 }]
      };
      const result = validateCreateTransfer(form, MOCK_WAREHOUSES, MOCK_PRODUCTS, {});
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Vui lòng chọn kho đích');
    });

    test('fails when source matches destination', () => {
      const form = {
        sourceWarehouseId: '1',
        destinationWarehouseId: '1',
        items: [{ productId: '100', plannedQty: 5 }]
      };
      const result = validateCreateTransfer(form, MOCK_WAREHOUSES, MOCK_PRODUCTS, {});
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Kho nguồn và kho đích phải khác nhau');
    });

    test('fails with empty items list', () => {
      const form = {
        sourceWarehouseId: '1',
        destinationWarehouseId: '2',
        items: []
      };
      const result = validateCreateTransfer(form, MOCK_WAREHOUSES, MOCK_PRODUCTS, {});
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Phiếu điều chuyển cần ít nhất một dòng hàng');
    });

    test('fails when item plannedQty is negative or zero', () => {
      const formZero = {
        sourceWarehouseId: '1',
        destinationWarehouseId: '2',
        items: [{ productId: '100', plannedQty: 0 }]
      };
      const resultZero = validateCreateTransfer(formZero, MOCK_WAREHOUSES, MOCK_PRODUCTS, {});
      expect(resultZero.isValid).toBe(false);
      expect(resultZero.error).toBe('Số lượng đặt phải lớn hơn 0');

      const formNeg = {
        sourceWarehouseId: '1',
        destinationWarehouseId: '2',
        items: [{ productId: '100', plannedQty: -5 }]
      };
      const resultNeg = validateCreateTransfer(formNeg, MOCK_WAREHOUSES, MOCK_PRODUCTS, {});
      expect(resultNeg.isValid).toBe(false);
      expect(resultNeg.error).toBe('Số lượng đặt phải lớn hơn 0');
    });

    test('fails when item plannedQty exceeds availableQty', () => {
      const form = {
        sourceWarehouseId: '1',
        destinationWarehouseId: '2',
        items: [{ productId: '100', plannedQty: 15 }]
      };
      const availabilityByLine = {
        0: { availableQty: 10 }
      };
      const result = validateCreateTransfer(form, MOCK_WAREHOUSES, MOCK_PRODUCTS, availabilityByLine);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Số lượng đặt của PAN-001 vượt tồn khả dụng');
    });
  });

  describe('validateTripAssignment', () => {
    const tripData = {
      vehicleId: '501',
      driverId: '601',
      plannedStartAt: '2026-07-11T12:00',
      plannedEndAt: '2026-07-11T15:00'
    };
    const driverData = {
      id: 601,
      warehouseIds: [1]
    };

    test('passes with valid trip assignments', () => {
      const result = validateTripAssignment(tripData, driverData, 1);
      expect(result.isValid).toBe(true);
    });

    test('fails when vehicle is missing', () => {
      const result = validateTripAssignment({ ...tripData, vehicleId: '' }, driverData, 1);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Vui lòng chọn xe');
    });

    test('fails when driver is missing', () => {
      const result = validateTripAssignment({ ...tripData, driverId: '' }, driverData, 1);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Vui lòng chọn tài xế');
    });

    test('fails when start time is after end time', () => {
      const result = validateTripAssignment({
        ...tripData,
        plannedStartAt: '2026-07-11T15:00',
        plannedEndAt: '2026-07-11T12:00'
      }, driverData, 1);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Thời gian kết thúc phải sau thời gian bắt đầu');
    });

    test('fails when driver is not in source warehouse scope', () => {
      const result = validateTripAssignment(tripData, driverData, 2);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Tài xế phải thuộc phạm vi kho nguồn');
    });
  });

  describe('validateReceiveCheck', () => {
    const locations = [
      { id: 10, code: 'BIN-01', isQuarantine: false },
      { id: 11, code: 'BIN-QUAR', isQuarantine: true }
    ];

    test('passes with valid receive check rows', () => {
      const rows = [{
        transferItemId: 1,
        confirmedQty: 5,
        qcPassedQty: 5,
        qcFailedQty: 0,
        destinationLocationId: '10'
      }];
      const result = validateReceiveCheck(rows, locations);
      expect(result.isValid).toBe(true);
    });

    test('fails when qc quantities do not sum up to confirmed quantity', () => {
      const rows = [{
        transferItemId: 1,
        confirmedQty: 5,
        qcPassedQty: 3,
        qcFailedQty: 1,
        destinationLocationId: '10'
      }];
      const result = validateReceiveCheck(rows, locations);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Tổng số lượng đạt và lỗi phải bằng số lượng chốt');
    });

    test('fails when qc failed quantity > 0 but reason is blank', () => {
      const rows = [{
        transferItemId: 1,
        confirmedQty: 5,
        qcPassedQty: 3,
        qcFailedQty: 2,
        destinationLocationId: '10',
        qcFailureReason: ''
      }];
      const result = validateReceiveCheck(rows, locations);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Vui lòng nhập lý do QC lỗi');
    });

    test('fails when destination location is missing for qc passed stock', () => {
      const rows = [{
        transferItemId: 1,
        confirmedQty: 5,
        qcPassedQty: 5,
        qcFailedQty: 0,
        destinationLocationId: ''
      }];
      const result = validateReceiveCheck(rows, locations);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Vui lòng chọn vị trí nhập kho cho hàng đạt');
    });

    test('fails when selected destination bin is a quarantine bin', () => {
      const rows = [{
        transferItemId: 1,
        confirmedQty: 5,
        qcPassedQty: 5,
        qcFailedQty: 0,
        destinationLocationId: '11' // quarantine bin ID
      }];
      const result = validateReceiveCheck(rows, locations);
      expect(result.isValid).toBe(false);
      expect(result.error).toBe('Vị trí hàng đạt không được là bin quarantine');
    });
  });

  describe('new workflow validations', () => {
    test('validateOutboundQc validates photo reference', () => {
      expect(validateOutboundQc({ outboundQcPassed: true, outboundQcPhotoRef: '' }).isValid).toBe(false);
      expect(validateOutboundQc({ outboundQcPassed: true, outboundQcPhotoRef: 'photo.jpg' }).isValid).toBe(true);
    });

    test('validateLoadHandover validates photo reference', () => {
      expect(validateLoadHandover({ loadHandoverPhotoRef: '' }).isValid).toBe(false);
      expect(validateLoadHandover({ loadHandoverPhotoRef: 'load.jpg' }).isValid).toBe(true);
    });

    test('validateReceivingHandover validates photo reference', () => {
      expect(validateReceivingHandover({ photoRef: '' }).isValid).toBe(false);
      expect(validateReceivingHandover({ photoRef: 'arrive.jpg' }).isValid).toBe(true);
    });

    test('validateWrongSkuReport validates general reason and items', () => {
      const payloadNoReason = { reason: '', wrongSkuItems: [{ transferItemId: 1, actualProductSku: 'SKU2', quantity: 2, reason: 'Wrong color' }] };
      expect(validateWrongSkuReport(payloadNoReason).isValid).toBe(false);

      const payloadNoItems = { reason: 'Wrong SKU delivered', wrongSkuItems: [] };
      expect(validateWrongSkuReport(payloadNoItems).isValid).toBe(false);

      const payloadValid = {
        reason: 'Wrong SKU delivered',
        wrongSkuItems: [{ transferItemId: 1, actualProductSku: 'SKU2', quantity: 2, reason: 'Wrong color' }]
      };
      expect(validateWrongSkuReport(payloadValid).isValid).toBe(true);
    });
  });
});
