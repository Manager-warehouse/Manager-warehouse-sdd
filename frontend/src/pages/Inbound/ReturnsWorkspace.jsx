import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import returnsService from '../../services/returns.service';
import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';
import Modal from '../../components/common/Modal';
import { Loader2, Plus, Receipt, ShieldAlert, Check, Coins, FileText, ArrowRightLeft } from 'lucide-react';

const ReturnsWorkspace = () => {
  const activeWarehouse = useAuthStore((state) => state.activeWarehouse);
  const { user } = useAuthStore();
  const { addToast } = useUiStore();

  const [returns, setReturns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('LIST'); // LIST or CREATE

  // Dropdown lists
  const [dealers, setDealers] = useState([]);
  const [deliveryOrders, setDeliveryOrders] = useState([]);
  const [regularBins, setRegularBins] = useState([]);
  const [quarantineBins, setQuarantineBins] = useState([]);

  // Create Return Form State
  const [selectedDoId, setSelectedDoId] = useState('');
  const [selectedDoDetails, setSelectedDoDetails] = useState(null);
  const [selectedDealerId, setSelectedDealerId] = useState('');
  const [returnItems, setReturnItems] = useState([]); // [{ productId, expectedQty, maxQty, name, sku }]
  const [returnNotes, setReturnNotes] = useState('');

  // QC Split Modal State
  const [showQcModal, setShowQcModal] = useState(false);
  const [qcReceipt, setQcReceipt] = useState(null);
  const [qcItems, setQcItems] = useState([]); // [{ receiptItemId, expectedQty, actualQty, passedQty, failedQty, passedLocationId, quarantineLocationId, name, sku }]

  // Credit Note Modal State
  const [showCreditModal, setShowCreditModal] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState(null);
  const [creditReason, setCreditReason] = useState('');

  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (activeWarehouse) {
      fetchData();
    }
  }, [activeWarehouse, activeTab]);

  const fetchData = async () => {
    setLoading(true);
    try {
      // 1. Fetch return receipts
      const data = await returnsService.getReturns({ warehouse_id: activeWarehouse.id });
      setReturns(data);

      // 2. Fetch master data for dropdowns
      const dlData = await masterDataService.getDealers();
      setDealers(dlData);

      const locs = await masterDataService.getBinLocations(activeWarehouse.id);
      setRegularBins(locs.filter(l => !l.is_quarantine));
      setQuarantineBins(locs.filter(l => l.is_quarantine));

      // 3. Fetch completed delivery orders for selection
      const doData = await outboundService.getDeliveryOrders(activeWarehouse.id);
      // Filter DOs that are completed/delivered
      const completedDos = doData.filter(d => d.status === 'DELIVERED' || d.status === 'COMPLETED');
      setDeliveryOrders(completedDos);

    } catch (e) {
      console.error(e);
      addToast('Lỗi tải dữ liệu hàng trả', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Handle DO change in Create Form
  const handleDoChange = async (doId) => {
    setSelectedDoId(doId);
    if (!doId) {
      setSelectedDoDetails(null);
      setSelectedDealerId('');
      setReturnItems([]);
      return;
    }

    try {
      const details = await outboundService.getDeliveryOrderById(doId);
      setSelectedDoDetails(details);
      setSelectedDealerId(details.dealer_id);

      // Initialize return items from DO items
      const items = details.items.map(item => ({
        productId: item.product_id,
        sku: item.sku,
        name: item.product_name,
        maxQty: item.issued_qty || item.requested_qty, // Can return up to issued qty
        expectedQty: 0
      }));
      setReturnItems(items);
    } catch (e) {
      addToast('Không thể tải chi tiết DO', 'error');
    }
  };

  const handleReturnQtyChange = (productId, qty) => {
    setReturnItems(prev => prev.map(item => {
      if (item.productId === productId) {
        const val = Math.min(item.maxQty, Math.max(0, parseInt(qty) || 0));
        return { ...item, expectedQty: val };
      }
      return item;
    }));
  };

  const handleCreateReturnReceipt = async (e) => {
    e.preventDefault();
    const itemsToSubmit = returnItems.filter(item => item.expectedQty > 0);
    if (itemsToSubmit.length === 0) {
      addToast('Vui lòng nhập ít nhất một sản phẩm cần trả', 'warning');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        warehouseId: activeWarehouse.id,
        dealerId: Number(selectedDealerId),
        deliveryOrderId: Number(selectedDoId),
        notes: returnNotes,
        items: itemsToSubmit.map(item => ({
          productId: item.productId,
          expectedQty: item.expectedQty
        }))
      };

      await returnsService.createReturn(payload);
      addToast('Lập phiếu trả hàng thành công', 'success');
      setActiveTab('LIST');
      // Reset form
      setSelectedDoId('');
      setSelectedDoDetails(null);
      setSelectedDealerId('');
      setReturnItems([]);
      setReturnNotes('');
    } catch (e) {
      addToast(e.message || 'Lỗi lập phiếu trả hàng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  // Open QC Split Modal
  const openQcSplit = async (receipt) => {
    try {
      const details = await returnsService.getReturnById(receipt.id);
      setQcReceipt(details);
      
      const items = details.items.map(item => ({
        receiptItemId: item.id,
        sku: item.product_sku || `SKU-${item.product_id}`,
        name: item.product_name || `Sản phẩm ${item.product_id}`,
        expectedQty: item.expected_qty,
        actualQty: item.expected_qty,
        passedQty: item.expected_qty,
        failedQty: 0,
        passedLocationId: regularBins[0]?.id || '',
        quarantineLocationId: quarantineBins[0]?.id || ''
      }));
      setQcItems(items);
      setShowQcModal(true);
    } catch (e) {
      addToast('Không thể tải chi tiết phiếu để QC', 'error');
    }
  };

  const handleQcValueChange = (itemId, field, value) => {
    setQcItems(prev => prev.map(item => {
      if (item.receiptItemId === itemId) {
        const updated = { ...item };
        if (field === 'actualQty') {
          updated.actualQty = Math.max(0, parseInt(value) || 0);
          // auto align passed and failed
          updated.passedQty = updated.actualQty;
          updated.failedQty = 0;
        } else if (field === 'passedQty') {
          const passed = Math.min(updated.actualQty, Math.max(0, parseInt(value) || 0));
          updated.passedQty = passed;
          updated.failedQty = updated.actualQty - passed;
        } else if (field === 'passedLocationId' || field === 'quarantineLocationId') {
          updated[field] = Number(value);
        }
        return updated;
      }
      return item;
    }));
  };

  const submitQcSplit = async () => {
    // Validate locations
    const invalidItem = qcItems.find(item => !item.passedLocationId || !item.quarantineLocationId);
    if (invalidItem) {
      addToast('Vui lòng chọn đầy đủ vị trí lưu kho cho tất cả sản phẩm', 'warning');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        expectedVersion: qcReceipt.version,
        items: qcItems.map(item => ({
          receiptItemId: item.receiptItemId,
          actualQty: item.actualQty,
          passedQty: item.passedQty,
          failedQty: item.failedQty,
          passedLocationId: item.passedLocationId,
          quarantineLocationId: item.quarantineLocationId
        }))
      };

      await returnsService.processQc(qcReceipt.id, payload);
      addToast('Phân tách QC và nhập kho hàng trả thành công', 'success');
      setShowQcModal(false);
      fetchData();
    } catch (e) {
      addToast(e.message || 'Lỗi lưu kết quả QC', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  // Open Credit Note Modal
  const openCreditNoteModal = (receipt) => {
    setSelectedReceipt(receipt);
    setCreditReason('Hoàn trả tiền hàng đại lý trả lại');
    setShowCreditModal(true);
  };

  const submitCreditNote = async () => {
    if (!creditReason.trim()) {
      addToast('Vui lòng nhập lý do hoàn tiền / khấu trừ công nợ', 'warning');
      return;
    }

    setSubmitting(true);
    try {
      const res = await returnsService.createCreditNote(selectedReceipt.id, { reason: creditReason });
      addToast(`Đã tạo Credit Note ${res.creditNoteNumber} khấu trừ công nợ thành công!`, 'success');
      setShowCreditModal(false);
      fetchData();
    } catch (e) {
      addToast(e.message || 'Lỗi tạo Credit Note', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const getDealerName = (dealerId) => {
    const d = dealers.find(dl => dl.id === dealerId);
    return d ? d.name : `Đại lý ID: ${dealerId}`;
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Inbound</span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight flex items-center gap-2">
            <ArrowRightLeft className="w-7 h-7" />
            Nhận hàng hoàn trả & Khấu trừ công nợ
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Xử lý hàng đại lý trả lại, phân tách QC (regular/quarantine) và sinh Credit Note khấu trừ công nợ.
          </p>
        </div>

        <div className="flex gap-3">
          <button
            onClick={() => setActiveTab(activeTab === 'LIST' ? 'CREATE' : 'LIST')}
            className={`px-4 py-2 rounded-lg font-medium transition-colors flex items-center gap-2 ${
              activeTab === 'CREATE'
                ? 'bg-canvas-light border border-hairline hover:bg-shade-20 text-black'
                : 'bg-canvas-night hover:bg-canvas-nightElevated text-onPrimary shadow-lg shadow-black/10'
            }`}
          >
            {activeTab === 'CREATE' ? (
              <>Quay lại danh sách</>
            ) : (
              <>
                <Plus className="w-5 h-5" />
                Lập phiếu trả hàng mới
              </>
            )}
          </button>
        </div>
      </div>

      {activeTab === 'LIST' ? (
        <div className="bg-canvas-light rounded-xl border border-hairline shadow-level-3 overflow-hidden flex-1 flex flex-col">
          {loading ? (
            <div className="flex-1 flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="w-10 h-10 animate-spin text-primary" />
              <span className="text-shade-60 text-sm">Đang tải danh sách hàng trả...</span>
            </div>
          ) : returns.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center py-20 text-center px-4">
              <Receipt className="w-16 h-16 text-shade-40 mb-3" />
              <h3 className="font-semibold text-ink text-lg">Không có phiếu trả hàng nào</h3>
              <p className="text-shade-60 max-w-sm mt-1 text-sm">
                Hiện tại không có phiếu nhập trả hàng nào của đại lý tại kho này.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-canvas-cream text-shade-60 font-semibold border-b border-hairline">
                    <th className="px-6 py-4">Mã phiếu trả</th>
                    <th className="px-6 py-4">DO gốc</th>
                    <th className="px-6 py-4">Đại lý</th>
                    <th className="px-6 py-4">Ngày tạo</th>
                    <th className="px-6 py-4">Trạng thái</th>
                    <th className="px-6 py-4">Credit Note</th>
                    <th className="px-6 py-4 text-right">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline">
                  {returns.map((ret) => (
                    <tr key={ret.id} className="hover:bg-shade-10 transition-colors">
                      <td className="px-6 py-4 font-medium text-ink flex items-center gap-2">
                        <FileText className="w-4 h-4 text-primary" />
                        {ret.receipt_number}
                      </td>
                      <td className="px-6 py-4 text-shade-60 font-mono text-xs">{ret.source_order_code}</td>
                      <td className="px-6 py-4 text-shade-60">{getDealerName(ret.dealer_id)}</td>
                      <td className="px-6 py-4 text-shade-60">{ret.document_date}</td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex px-2 py-1 rounded text-xs font-semibold ${
                          ret.status === 'APPROVED'
                            ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                            : 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400'
                        }`}>
                          {ret.status === 'APPROVED' ? 'Đã duyệt nhập kho' : 'Nháp / Chờ QC'}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        {ret.credit_note_generated ? (
                          <span className="inline-flex items-center gap-1 text-green-600 dark:text-green-400 text-xs font-medium">
                            <Check className="w-3.5 h-3.5" /> Đã hoàn công nợ
                          </span>
                        ) : (
                          <span className="text-shade-40 text-xs font-medium">Chưa hoàn</span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="flex justify-end gap-2">
                          {ret.status === 'DRAFT' && (
                            <button
                              onClick={() => openQcSplit(ret)}
                              className="px-3 py-1.5 bg-canvas-night hover:bg-canvas-nightElevated text-onPrimary text-xs font-semibold rounded flex items-center gap-1 transition-colors"
                            >
                              <ShieldAlert className="w-3.5 h-3.5" />
                              QC Phân tách & Nhập kho
                            </button>
                          )}
                          {ret.status === 'APPROVED' && !ret.credit_note_generated && (
                            <button
                              onClick={() => openCreditNoteModal(ret)}
                              className="px-3 py-1.5 bg-green-600 hover:bg-green-700 text-white text-xs font-semibold rounded flex items-center gap-1 transition-colors"
                            >
                              <Coins className="w-3.5 h-3.5" />
                              Tạo Credit Note
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      ) : (
        <div className="bg-canvas-light rounded-xl border border-hairline shadow-level-3 p-6 max-w-4xl">
          <h2 className="text-lg font-bold text-ink mb-4 pb-2 border-b border-hairline">Tạo phiếu trả hàng mới</h2>
          
          <form onSubmit={handleCreateReturnReceipt} className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-semibold text-shade-60 mb-2">Chọn đơn xuất hàng (DO) gốc</label>
                <select
                  required
                  value={selectedDoId}
                  onChange={(e) => handleDoChange(e.target.value)}
                  className="text-input text-sm"
                >
                  <option value="">-- Chọn DO đã giao thành công --</option>
                  {deliveryOrders.map(d => (
                    <option key={d.id} value={d.id}>{d.do_number} (Đại lý: {d.dealer_name})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-semibold text-shade-60 mb-2">Đại lý nhận hoàn trả</label>
                <input
                  type="text"
                  disabled
                  value={selectedDealerId ? getDealerName(Number(selectedDealerId)) : ''}
                  placeholder="Đại lý sẽ tự động điền khi chọn DO"
                  className="w-full px-3 py-2 bg-shade-20 border border-hairline rounded-lg text-shade-60 text-sm focus:outline-none"
                />
              </div>
            </div>

            {returnItems.length > 0 && (
              <div className="space-y-4">
                <h3 className="text-sm font-bold text-ink">Danh sách sản phẩm hoàn trả</h3>
                <div className="border border-hairline rounded-lg overflow-hidden bg-canvas">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-canvas-cream text-shade-60 font-semibold border-b border-hairline">
                        <th className="px-4 py-3">Sản phẩm</th>
                        <th className="px-4 py-3">Số lượng đã xuất</th>
                        <th className="px-4 py-3 w-40">Số lượng hoàn trả</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline">
                      {returnItems.map(item => (
                        <tr key={item.productId} className="hover:bg-shade-10">
                          <td className="px-4 py-3">
                            <div className="font-semibold text-ink">{item.name}</div>
                            <div className="text-shade-60 font-mono text-[10px] mt-0.5">{item.sku}</div>
                          </td>
                          <td className="px-4 py-3 text-shade-60 font-semibold">{item.maxQty}</td>
                          <td className="px-4 py-3">
                            <input
                              type="number"
                              min="0"
                              max={item.maxQty}
                              value={item.expectedQty || ''}
                              onChange={(e) => handleReturnQtyChange(item.productId, e.target.value)}
                              className="w-full px-2 py-1 bg-canvas border border-hairline rounded text-ink focus:outline-none focus:border-primary text-center font-semibold text-sm"
                            />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            <div>
              <label className="block text-sm font-semibold text-shade-60 mb-2">Ghi chú hoàn trả</label>
              <textarea
                value={returnNotes}
                onChange={(e) => setReturnNotes(e.target.value)}
                placeholder="Lý do đại lý yêu cầu trả hàng, thông tin bổ sung..."
                rows="3"
                className="w-full px-3 py-2 bg-canvas border border-hairline rounded-lg text-ink focus:outline-none focus:border-primary text-sm"
              />
            </div>

            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setActiveTab('LIST')}
                className="px-4 py-2 border border-hairline rounded-lg font-medium text-ink hover:bg-shade-20 transition-colors"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="px-4 py-2 bg-canvas-night hover:bg-canvas-nightElevated text-onPrimary rounded-lg font-medium shadow-lg shadow-black/10 transition-all flex items-center gap-2"
              >
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                Lập phiếu trả hàng
              </button>
            </div>
          </form>
        </div>
      )}

      {/* QC Split Modal */}
      <Modal
        isOpen={showQcModal}
        onClose={() => setShowQcModal(false)}
        title="Ghi nhận QC & Nhập kho hàng hoàn trả"
        maxWidth="max-w-4xl"
      >
        {qcReceipt && (
          <div className="space-y-6">
            <div>
              <div className="text-xs text-shade-60">Mã phiếu trả hàng: <span className="font-semibold text-ink">{qcReceipt.receipt_number}</span></div>
              <div className="text-xs text-shade-60 mt-1">Đơn xuất gốc: <span className="font-semibold text-ink">{qcReceipt.source_order_code}</span></div>
            </div>

            <div className="border border-hairline rounded-lg overflow-hidden bg-canvas">
              <table className="w-full text-left border-collapse text-xs">
                <thead>
                  <tr className="bg-canvas-cream text-shade-60 font-semibold border-b border-hairline">
                    <th className="px-4 py-3">Sản phẩm</th>
                    <th className="px-4 py-3 w-28 text-center">Yêu cầu trả</th>
                    <th className="px-4 py-3 w-28 text-center">Thực tế nhận</th>
                    <th className="px-4 py-3 w-28 text-center">QC Đạt (Passed)</th>
                    <th className="px-4 py-3 w-28 text-center">QC Lỗi (Failed)</th>
                    <th className="px-4 py-3">Vị trí lưu kho</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline">
                  {qcItems.map(item => (
                    <tr key={item.receiptItemId} className="hover:bg-shade-10">
                      <td className="px-4 py-3">
                        <div className="font-semibold text-ink">{item.name}</div>
                        <div className="text-shade-60 font-mono text-[10px] mt-0.5">{item.sku}</div>
                      </td>
                      <td className="px-4 py-3 text-center font-bold text-shade-60">{item.expectedQty}</td>
                      <td className="px-4 py-3">
                        <input
                          type="number"
                          min="0"
                          value={item.actualQty}
                          onChange={(e) => handleQcValueChange(item.receiptItemId, 'actualQty', e.target.value)}
                          className="w-full px-2 py-1 bg-canvas border border-hairline rounded text-ink focus:outline-none text-center font-semibold"
                        />
                      </td>
                      <td className="px-4 py-3">
                        <input
                          type="number"
                          min="0"
                          max={item.actualQty}
                          value={item.passedQty}
                          onChange={(e) => handleQcValueChange(item.receiptItemId, 'passedQty', e.target.value)}
                          className="w-full px-2 py-1 bg-canvas border border-hairline rounded text-ink focus:outline-none text-center font-semibold text-green-600 dark:text-green-400"
                        />
                      </td>
                      <td className="px-4 py-3 text-center font-semibold text-red-600 dark:text-red-400">
                        {item.failedQty}
                      </td>
                      <td className="px-4 py-3 space-y-2">
                        {item.passedQty > 0 && (
                          <div>
                            <span className="text-[10px] text-shade-60 block mb-1">Vị trí đạt chuẩn:</span>
                            <select
                              value={item.passedLocationId}
                              onChange={(e) => handleQcValueChange(item.receiptItemId, 'passedLocationId', e.target.value)}
                              className="w-full p-1 bg-canvas border border-hairline rounded text-ink text-xs focus:outline-none"
                            >
                              <option value="">-- Chọn vị trí --</option>
                              {regularBins.map(b => (
                                <option key={b.id} value={b.id}>{b.code} (Còn: {b.capacity_m3 - b.current_volume_m3} m³)</option>
                              ))}
                            </select>
                          </div>
                        )}
                        {item.failedQty > 0 && (
                          <div>
                            <span className="text-[10px] text-shade-60 block mb-1">Khu cách ly lỗi:</span>
                            <select
                              value={item.quarantineLocationId}
                              onChange={(e) => handleQcValueChange(item.receiptItemId, 'quarantineLocationId', e.target.value)}
                              className="w-full p-1 bg-canvas border border-hairline rounded text-ink text-xs focus:outline-none border-red-300 focus:border-red-500"
                            >
                              <option value="">-- Chọn vị trí cách ly --</option>
                              {quarantineBins.map(b => (
                                <option key={b.id} value={b.id}>{b.code}</option>
                              ))}
                            </select>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="flex justify-end gap-3">
              <button
                type="button"
                onClick={() => setShowQcModal(false)}
                className="px-4 py-2 border border-hairline rounded-lg font-medium text-ink hover:bg-shade-20 transition-colors"
              >
                Hủy
              </button>
              <button
                onClick={submitQcSplit}
                disabled={submitting}
                className="px-4 py-2 bg-canvas-night hover:bg-canvas-nightElevated text-onPrimary rounded-lg font-medium shadow-lg shadow-black/10 transition-all flex items-center gap-2"
              >
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                Xác nhận QC & Nhập kho
              </button>
            </div>
          </div>
        )}
      </Modal>

      {/* Credit Note Modal */}
      <Modal
        isOpen={showCreditModal}
        onClose={() => setShowCreditModal(false)}
        title="Tạo Credit Note hoàn trả công nợ đại lý"
      >
        {selectedReceipt && (
          <div className="space-y-4">
            <div className="bg-canvas-cream p-4 rounded-lg border border-hairline text-sm text-shade-60 space-y-2">
              <div>Đại lý thụ hưởng: <span className="font-semibold text-ink">{getDealerName(selectedReceipt.dealer_id)}</span></div>
              <div>Phiếu nhập hàng trả: <span className="font-semibold text-ink">{selectedReceipt.receipt_number}</span></div>
            </div>

            <div>
              <label className="block text-sm font-semibold text-shade-60 mb-2">Lý do tạo Credit Note</label>
              <textarea
                value={creditReason}
                onChange={(e) => setCreditReason(e.target.value)}
                placeholder="Nhập lý do hoàn trả công nợ..."
                rows="3"
                className="w-full px-3 py-2 bg-canvas border border-hairline rounded-lg text-ink focus:outline-none focus:border-primary text-sm"
              />
            </div>

            <div className="flex justify-end gap-3 mt-6">
              <button
                type="button"
                onClick={() => setShowCreditModal(false)}
                className="px-4 py-2 border border-hairline rounded-lg font-medium text-ink hover:bg-shade-20 transition-colors"
              >
                Hủy
              </button>
              <button
                onClick={submitCreditNote}
                disabled={submitting}
                className="px-4 py-2 bg-green-600 hover:bg-green-700 text-white rounded-lg font-medium shadow-lg shadow-green-600/20 transition-all flex items-center gap-2"
              >
                {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
                Xác nhận & Khấu trừ công nợ
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default ReturnsWorkspace;
