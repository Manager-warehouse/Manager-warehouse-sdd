import apiClient, { useMock } from './api.client';

const BASE = '/returns';

// Helpers for mock local storage database
const KEYS = {
  RECEIPTS: 'wms_db_receipts',
  RECEIPT_ITEMS: 'wms_db_receipt_items',
  DELIVERY_ORDERS: 'wms_db_delivery_orders',
  DELIVERY_ORDER_ITEMS: 'wms_db_delivery_order_items',
  DEALERS: 'wms_db_dealers',
  CREDIT_NOTES: 'wms_db_credit_notes',
  INVENTORIES: 'wms_db_inventories',
  LOCATIONS: 'wms_db_warehouse_locations'
};

function getDb(key, fallback = []) {
  try {
    const val = localStorage.getItem(key);
    return val ? JSON.parse(val) : fallback;
  } catch (e) {
    return fallback;
  }
}

function saveDb(key, data) {
  try {
    localStorage.setItem(key, JSON.stringify(data));
  } catch (e) {
    console.error(e);
  }
}

const returnsService = {
  async getReturns(params = {}) {
    if (useMock) {
      await delay(300);
      const receipts = getDb(KEYS.RECEIPTS, []);
      let list = receipts.filter(r => r.type === 'RETURN');
      if (params.warehouse_id) {
        list = list.filter(r => r.warehouse_id === Number(params.warehouse_id));
      }
      return list;
    }
    const response = await apiClient.get('/receipts', {
      params: { ...params, type: 'RETURN' }
    });
    return response.data;
  },

  async getReturnById(id) {
    if (useMock) {
      await delay(200);
      const receipts = getDb(KEYS.RECEIPTS, []);
      const returnReceipt = receipts.find(r => r.id === Number(id));
      if (!returnReceipt) throw new Error('RETURN_RECEIPT_NOT_FOUND');

      const items = getDb(KEYS.RECEIPT_ITEMS, []).filter(ri => ri.receipt_id === returnReceipt.id);
      
      return {
        ...returnReceipt,
        items
      };
    }
    const response = await apiClient.get(`/receipts/${id}`);
    return response.data;
  },

  async createReturn(data) {
    if (useMock) {
      await delay(500);
      const receipts = getDb(KEYS.RECEIPTS, []);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, []);
      const dos = getDb(KEYS.DELIVERY_ORDERS, []);
      const doItems = getDb(KEYS.DELIVERY_ORDER_ITEMS, []);
      const dealers = getDb(KEYS.DEALERS, []);

      const doOrder = dos.find(d => d.id === Number(data.deliveryOrderId));
      if (!doOrder) throw new Error('DELIVERY_ORDER_NOT_FOUND');

      const dealer = dealers.find(dl => dl.id === Number(data.dealerId));
      if (!dealer) throw new Error('DEALER_NOT_FOUND');

      if (doOrder.dealer_id !== dealer.id) {
        throw new Error('DEALER_MISMATCH');
      }

      const receiptId = receipts.length > 0 ? Math.max(...receipts.map(r => r.id)) + 1 : 1;
      const receiptNumber = `REC-RET-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${Math.random().toString(36).substring(2, 8).toUpperCase()}`;

      const newReceipt = {
        id: receiptId,
        receipt_number: receiptNumber,
        source_order_code: doOrder.do_number,
        type: 'RETURN',
        warehouse_id: Number(data.warehouseId),
        dealer_id: dealer.id,
        dealer_name: dealer.name,
        delivery_order_id: doOrder.id,
        status: 'DRAFT',
        document_date: new Date().toISOString().slice(0, 10),
        notes: data.notes || '',
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
        version: 1
      };

      const newItems = data.items.map((itemReq, index) => {
        const doItem = doItems.find(doi => doi.do_id === doOrder.id && doi.product_id === Number(itemReq.productId));
        if (!doItem) throw new Error(`PRODUCT_NOT_IN_DO: Product ${itemReq.productId}`);

        return {
          id: receiptItems.length + index + 1,
          receipt_id: receiptId,
          product_id: Number(itemReq.productId),
          product_sku: doItem.product_sku || `SKU-${itemReq.productId}`,
          product_name: doItem.product_name || `Product Name ${itemReq.productId}`,
          expected_qty: Number(itemReq.expectedQty),
          actual_qty: null,
          sample_qty: null,
          sample_passed_qty: null,
          sample_failed_qty: null,
          qc_result: 'PENDING',
          unit_cost: doItem.unit_price, // store original sales price for credit calculation
          batch_id: doItem.batch_id || 1
        };
      });

      receipts.push(newReceipt);
      saveDb(KEYS.RECEIPTS, receipts);
      saveDb(KEYS.RECEIPT_ITEMS, [...receiptItems, ...newItems]);

      return {
        id: receiptId,
        receiptNumber,
        status: 'DRAFT',
        version: 1,
        message: 'Tạo phiếu trả hàng thành công (Mock)'
      };
    }
    const response = await apiClient.post(BASE, data);
    return response.data;
  },

  async processQc(id, data) {
    if (useMock) {
      await delay(600);
      const receipts = getDb(KEYS.RECEIPTS, []);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, []);
      const inventories = getDb(KEYS.INVENTORIES, []);
      const locations = getDb(KEYS.LOCATIONS, []);

      const rIdx = receipts.findIndex(r => r.id === Number(id));
      if (rIdx === -1) throw new Error('RECEIPT_NOT_FOUND');

      const receipt = receipts[rIdx];
      if (receipt.status !== 'DRAFT') throw new Error('RECEIPT_NOT_IN_DRAFT');

      data.items.forEach(qcReq => {
        const riIdx = receiptItems.findIndex(ri => ri.id === Number(qcReq.receiptItemId));
        if (riIdx === -1) return;

        const item = receiptItems[riIdx];
        item.actual_qty = Number(qcReq.actualQty);
        item.sample_qty = Number(qcReq.actualQty);
        item.sample_passed_qty = Number(qcReq.passedQty);
        item.sample_failed_qty = Number(qcReq.failedQty);
        item.qc_result = qcReq.failedQty > 0 ? 'FAILED' : 'PASSED';
        item.location_id = Number(qcReq.passedLocationId);

        // Update inventories for passed quantity
        if (qcReq.passedQty > 0) {
          const passLoc = locations.find(l => l.id === Number(qcReq.passedLocationId));
          const invIdx = inventories.findIndex(inv => 
            inv.warehouse_id === receipt.warehouse_id &&
            inv.product_id === item.product_id &&
            inv.location_id === Number(qcReq.passedLocationId)
          );

          if (invIdx !== -1) {
            inventories[invIdx].total_qty = Number(inventories[invIdx].total_qty) + qcReq.passedQty;
            inventories[invIdx].updated_at = new Date().toISOString();
          } else {
            inventories.push({
              id: inventories.length + 1,
              warehouse_id: receipt.warehouse_id,
              product_id: item.product_id,
              batch_id: item.batch_id || 1,
              location_id: Number(qcReq.passedLocationId),
              total_qty: qcReq.passedQty,
              reserved_qty: 0,
              cost_price: 85000,
              updated_at: new Date().toISOString()
            });
          }

          if (passLoc) {
            passLoc.current_volume_m3 = Number((passLoc.current_volume_m3 + qcReq.passedQty * 0.05).toFixed(3));
            passLoc.current_weight_kg = Number((passLoc.current_weight_kg + qcReq.passedQty * 1.5).toFixed(2));
          }
        }

        // Update inventories for failed quantity (quarantine)
        if (qcReq.failedQty > 0) {
          const failLoc = locations.find(l => l.id === Number(qcReq.quarantineLocationId));
          const invIdx = inventories.findIndex(inv => 
            inv.warehouse_id === receipt.warehouse_id &&
            inv.product_id === item.product_id &&
            inv.location_id === Number(qcReq.quarantineLocationId)
          );

          if (invIdx !== -1) {
            inventories[invIdx].total_qty = Number(inventories[invIdx].total_qty) + qcReq.failedQty;
            inventories[invIdx].updated_at = new Date().toISOString();
          } else {
            inventories.push({
              id: inventories.length + 1,
              warehouse_id: receipt.warehouse_id,
              product_id: item.product_id,
              batch_id: item.batch_id || 1,
              location_id: Number(qcReq.quarantineLocationId),
              total_qty: qcReq.failedQty,
              reserved_qty: 0,
              cost_price: 85000,
              updated_at: new Date().toISOString()
            });
          }

          if (failLoc) {
            failLoc.current_volume_m3 = Number((failLoc.current_volume_m3 + qcReq.failedQty * 0.05).toFixed(3));
            failLoc.current_weight_kg = Number((failLoc.current_weight_kg + qcReq.failedQty * 1.5).toFixed(2));
          }
        }
      });

      receipt.status = 'APPROVED';
      receipt.approved_at = new Date().toISOString();
      receipt.version += 1;
      receipts[rIdx] = receipt;

      saveDb(KEYS.RECEIPTS, receipts);
      saveDb(KEYS.RECEIPT_ITEMS, receiptItems);
      saveDb(KEYS.INVENTORIES, inventories);
      saveDb(KEYS.LOCATIONS, locations);

      return {
        id: receipt.id,
        receiptNumber: receipt.receipt_number,
        status: 'APPROVED',
        version: receipt.version,
        updatedAt: receipt.approved_at,
        message: 'Đã hoàn tất phân tách QC và nhập kho hàng trả (Mock)'
      };
    }
    const response = await apiClient.put(`${BASE}/${id}/qc`, data);
    return response.data;
  },

  async createCreditNote(id, data) {
    if (useMock) {
      await delay(500);
      const receipts = getDb(KEYS.RECEIPTS, []);
      const receiptItems = getDb(KEYS.RECEIPT_ITEMS, []);
      const dealers = getDb(KEYS.DEALERS, []);
      const creditNotes = getDb(KEYS.CREDIT_NOTES, []);

      const receipt = receipts.find(r => r.id === Number(id));
      if (!receipt) throw new Error('RECEIPT_NOT_FOUND');

      const dlIdx = dealers.findIndex(d => d.id === receipt.dealer_id);
      if (dlIdx === -1) throw new Error('DEALER_NOT_FOUND');

      const items = receiptItems.filter(ri => ri.receipt_id === receipt.id);
      const totalAmount = items.reduce((sum, item) => sum + (item.actual_qty || 0) * (item.unit_cost || 0), 0);

      const dealer = dealers[dlIdx];
      const oldBalance = dealer.current_balance;
      dealer.current_balance = oldBalance - totalAmount;
      dealers[dlIdx] = dealer;

      const cnId = creditNotes.length + 1;
      const creditNoteNumber = `CN-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${Math.random().toString(36).substring(2, 8).toUpperCase()}`;

      const newCreditNote = {
        creditNoteId: cnId,
        creditNoteNumber,
        dealerId: dealer.id,
        dealerName: dealer.name,
        amount: totalAmount,
        currentBalance: dealer.current_balance,
        reason: data.reason,
        documentDate: new Date().toISOString().slice(0, 10),
        createdAt: new Date().toISOString()
      };

      creditNotes.push(newCreditNote);
      
      // Update receipt to know credit note exists in mock database
      receipt.credit_note_generated = true;
      const rIdx = receipts.findIndex(r => r.id === receipt.id);
      if (rIdx !== -1) receipts[rIdx] = receipt;

      saveDb(KEYS.RECEIPTS, receipts);
      saveDb(KEYS.DEALERS, dealers);
      saveDb(KEYS.CREDIT_NOTES, creditNotes);

      return newCreditNote;
    }
    const response = await apiClient.post(`${BASE}/${id}/credit-note`, data);
    return response.data;
  }
};

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

export default returnsService;
