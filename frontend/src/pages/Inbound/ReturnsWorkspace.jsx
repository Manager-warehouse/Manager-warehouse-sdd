import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import returnsService from '../../services/returns.service';
import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import { ROLES } from '../../utils/constants';
import { Loader2, Plus, Receipt, ShieldAlert, Check, Coins, FileText, ArrowRightLeft } from 'lucide-react';

const ReturnsWorkspace = () => {
  const activeWarehouse = useAuthStore((state) => state.activeWarehouse);
  const { user } = useAuthStore();
  const { addToast } = useUiStore();

  // Spec 009 only assigns the accountant the Credit Note step on already-APPROVED
  // returns; receiving/QC-splitting incoming returns is Thủ kho's job. Scope the
  // workspace down accordingly instead of exposing the full storekeeper flow.
  const isAccountingRole = user?.role === ROLES.ACCOUNTANT || user?.role === ROLES.ACCOUNTANT_MANAGER;

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
      const data = await returnsService.getReturns({ warehouse_id: activeWarehouse.id });
      // Accountants only act on returns already APPROVED by the storekeeper (the
      // Credit Note step); DRAFT/pending-QC items are outside their spec'd role.
      setReturns(isAccountingRole ? data.filter(r => r.status === 'APPROVED') : data);

      const dlData = await masterDataService.getDealers();
      setDealers(dlData);

      if (!isAccountingRole) {
        const locs = await masterDataService.getBinLocations(activeWarehouse.id);
        setRegularBins(locs.filter(l => !l.is_quarantine));
        setQuarantineBins(locs.filter(l => l.is_quarantine));

        const doData = await outboundService.getDeliveryOrders(activeWarehouse.id);
        const completedDos = doData.filter(d => d.status === 'DELIVERED' || d.status === 'COMPLETED');
        setDeliveryOrders(completedDos);
      }
    } catch (e) {
      addToast('Lỗi tải dữ liệu hàng trả', 'error');
    } finally {
      setLoading(false);
    }
  };

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
      const items = details.items.map(item => ({
        productId: item.product_id,
        sku: item.sku,
        name: item.product_name,
        maxQty: item.issued_qty || item.requested_qty,
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
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Nhận hàng hoàn trả & Khấu trừ công nợ
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            {isAccountingRole
              ? 'Sinh Credit Note khấu trừ công nợ cho các phiếu trả hàng đã được Thủ kho duyệt nhập kho.'
              : 'Xử lý hàng đại lý trả lại, phân tách QC (regular/quarantine) và sinh Credit Note khấu trừ công nợ.'}
          </p>
        </div>
        {!isAccountingRole && (
          <Button
            variant={activeTab === 'CREATE' ? 'outline-light' : 'primary'}
            icon={activeTab === 'CREATE' ? null : Plus}
            onClick={() => setActiveTab(activeTab === 'LIST' ? 'CREATE' : 'LIST')}
          >
            {activeTab === 'CREATE' ? 'Quay lại danh sách' : 'Lập phiếu trả hàng mới'}
          </Button>
        )}
      </div>

      {activeTab === 'LIST' || isAccountingRole ? (
        <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden flex flex-col">
          {loading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="w-8 h-8 animate-spin text-ink" />
              <span className="text-shade-60 text-xs font-light">Đang tải danh sách hàng trả...</span>
            </div>
          ) : returns.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 text-center px-4">
              <Receipt className="w-12 h-12 text-shade-40 mb-3" />
              <h3 className="font-semibold text-ink text-sm">Không có phiếu trả hàng nào</h3>
              <p className="text-shade-50 max-w-sm mt-1 text-xs font-light">
                Hiện tại không có phiếu nhập trả hàng nào của đại lý tại kho này.
              </p>
            </div>
          ) : (
            <>
            <div className="hidden md:block overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-canvas-cream border-b border-hairline-light">
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã phiếu trả</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">DO gốc</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Đại lý</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Ngày tạo</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Trạng thái</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Credit Note</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {returns.map((ret) => (
                    <tr key={ret.id} className="hover:bg-canvas-cream/50 transition-colors">
                      <td className="px-6 py-4 font-medium text-ink">
                        <div className="flex items-center gap-2">
                          <FileText className="w-4 h-4 text-shade-40 shrink-0" />
                          {ret.receipt_number}
                        </div>
                      </td>
                      <td className="px-6 py-4 text-shade-60 font-mono text-xs">{ret.source_order_code}</td>
                      <td className="px-6 py-4 text-shade-60 text-xs">{getDealerName(ret.dealer_id)}</td>
                      <td className="px-6 py-4 text-shade-60 text-xs">{ret.document_date}</td>
                      <td className="px-6 py-4">
                        <Badge size="sm" type={ret.status === 'APPROVED' ? 'success' : 'warning'}>
                          {ret.status === 'APPROVED' ? 'Đã duyệt nhập kho' : 'Nháp / Chờ QC'}
                        </Badge>
                      </td>
                      <td className="px-6 py-4">
                        {ret.credit_note_generated ? (
                          <span className="inline-flex items-center gap-1 text-[10px] font-semibold text-success-700">
                            <Check className="w-3.5 h-3.5" /> Đã hoàn công nợ
                          </span>
                        ) : (
                          <span className="text-shade-50 text-[10px] font-medium">Chưa hoàn</span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <div className="flex justify-end gap-2">
                          {ret.status === 'DRAFT' ? (
                            <button
                              onClick={() => openQcSplit(ret)}
                              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill border border-ink bg-canvas-light text-ink hover:bg-canvas-cream text-xs font-semibold transition-colors"
                            >
                              <ShieldAlert className="w-3.5 h-3.5" />
                              QC Phân tách & Nhập kho
                            </button>
                          ) : ret.status === 'APPROVED' && !ret.credit_note_generated ? (
                            <button
                              onClick={() => openCreditNoteModal(ret)}
                              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill btn-pill-aloe text-xs font-semibold transition-colors"
                            >
                              <Coins className="w-3.5 h-3.5" />
                              Tạo Credit Note
                            </button>
                          ) : (
                            <span className="text-shade-50 text-[10px] font-medium">Không có sẵn</span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="flex flex-col gap-3 p-4 md:hidden">
              {returns.map((ret) => (
                <div key={ret.id} className="rounded-lg border border-hairline-light bg-canvas-light p-4 shadow-level-3">
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0">
                      <div className="flex items-center gap-2 font-semibold text-ink">
                        <FileText className="h-4 w-4 shrink-0 text-shade-40" />
                        <span className="truncate">{ret.receipt_number}</span>
                      </div>
                      <div className="mt-1 font-mono text-[11px] text-shade-50">{ret.source_order_code}</div>
                    </div>
                    <Badge size="sm" type={ret.status === 'APPROVED' ? 'success' : 'warning'}>
                      {ret.status === 'APPROVED' ? 'Đã duyệt' : 'Chờ QC'}
                    </Badge>
                  </div>

                  <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
                    <div className="rounded-md bg-canvas-cream p-2">
                      <span className="block text-[10px] uppercase tracking-wider text-shade-50">Đại lý</span>
                      <span className="font-semibold text-ink">{getDealerName(ret.dealer_id)}</span>
                    </div>
                    <div className="rounded-md bg-canvas-cream p-2">
                      <span className="block text-[10px] uppercase tracking-wider text-shade-50">Ngày tạo</span>
                      <span className="font-semibold text-ink">{ret.document_date}</span>
                    </div>
                  </div>

                  <div className="mt-3 text-[11px] font-semibold text-shade-60">
                    Credit Note: {ret.credit_note_generated ? (
                      <span className="text-success-700">Đã hoàn công nợ</span>
                    ) : (
                      <span>Chưa hoàn</span>
                    )}
                  </div>

                  {ret.status === 'DRAFT' || (ret.status === 'APPROVED' && !ret.credit_note_generated) ? (
                    <div className="mt-4 flex flex-col gap-2">
                      {ret.status === 'DRAFT' && (
                        <button
                          onClick={() => openQcSplit(ret)}
                          className="btn-pill btn-pill-outline-light min-h-[44px] text-xs"
                        >
                          <ShieldAlert className="h-3.5 w-3.5" />
                          QC Phân tách & Nhập kho
                        </button>
                      )}
                      {ret.status === 'APPROVED' && !ret.credit_note_generated && (
                        <button
                          onClick={() => openCreditNoteModal(ret)}
                          className="btn-pill btn-pill-aloe min-h-[44px] text-xs"
                        >
                          <Coins className="h-3.5 w-3.5" />
                          Tạo Credit Note
                        </button>
                      )}
                    </div>
                  ) : (
                    <div className="mt-4 flex justify-end">
                      <span className="text-shade-50 text-[10px] font-medium">Không có sẵn</span>
                    </div>
                  )}
                </div>
              ))}
            </div>
            </>
          )}
        </div>
      ) : (
        <div className="card-premium flex flex-col gap-6">
          <div className="pb-4 border-b border-hairline-light">
            <h2 className="text-base font-semibold text-ink">Tạo phiếu trả hàng mới</h2>
          </div>

          <form onSubmit={handleCreateReturnReceipt} className="flex flex-col gap-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">Chọn đơn xuất hàng (DO) gốc</label>
                <Input
                  type="select"
                  required
                  value={selectedDoId}
                  onChange={(e) => handleDoChange(e.target.value)}
                  options={[
                    { value: '', label: '-- Chọn DO đã giao thành công --' },
                    ...deliveryOrders.map(d => ({ value: d.id, label: `${d.do_number} (Đại lý: ${d.dealer_name})` })),
                  ]}
                />
              </div>

              <div className="flex flex-col gap-1.5">
                <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">Đại lý nhận hoàn trả</label>
                <input
                  type="text"
                  disabled
                  value={selectedDealerId ? getDealerName(Number(selectedDealerId)) : ''}
                  placeholder="Đại lý sẽ tự động điền khi chọn DO"
                  className="w-full bg-canvas-light text-sm px-3 py-2.5 rounded-md border border-hairline-light text-shade-50 min-h-[44px] disabled:bg-canvas-cream/60 disabled:cursor-not-allowed"
                />
              </div>
            </div>

            {returnItems.length > 0 && (
              <div className="flex flex-col gap-3">
                <h3 className="text-xs font-semibold uppercase tracking-wider text-shade-60">Danh sách sản phẩm hoàn trả</h3>
                <div className="border border-hairline-light rounded-lg overflow-hidden">
                  <table className="hidden w-full text-left border-collapse md:table">
                    <thead>
                      <tr className="bg-canvas-cream border-b border-hairline-light">
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Sản phẩm</th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Số lượng đã xuất</th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 w-40">Số lượng hoàn trả</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {returnItems.map(item => (
                        <tr key={item.productId} className="hover:bg-canvas-cream/50 transition-colors">
                          <td className="px-6 py-4">
                            <div className="font-semibold text-ink text-sm">{item.name}</div>
                            <div className="text-shade-60 font-mono text-[10px] mt-0.5">{item.sku}</div>
                          </td>
                          <td className="px-6 py-4 text-shade-60 font-semibold text-sm">{item.maxQty}</td>
                          <td className="px-6 py-4">
                            <input
                              type="number"
                              min="0"
                              max={item.maxQty}
                              value={item.expectedQty || ''}
                              onChange={(e) => handleReturnQtyChange(item.productId, e.target.value)}
                              className="w-full px-3 py-1.5 bg-canvas-light border border-hairline-light rounded-md text-ink focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink text-center font-semibold text-sm transition-all"
                            />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>

                  <div className="flex flex-col gap-3 p-4 md:hidden">
                    {returnItems.map(item => (
                      <div key={item.productId} className="rounded-lg border border-hairline-light bg-canvas-light p-4 shadow-level-3">
                        <div className="flex items-start justify-between gap-3">
                          <div className="min-w-0">
                            <div className="text-sm font-semibold text-ink">{item.name}</div>
                            <div className="mt-1 font-mono text-[11px] text-shade-60">{item.sku}</div>
                          </div>
                          <div className="shrink-0 rounded-pill bg-canvas-cream px-3 py-1 text-[11px] font-bold text-shade-60">
                            Đã xuất: {item.maxQty}
                          </div>
                        </div>

                        <label className="mt-4 flex flex-col gap-1.5">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">
                            Số lượng hoàn trả
                          </span>
                          <input
                            type="number"
                            min="0"
                            max={item.maxQty}
                            value={item.expectedQty || ''}
                            onChange={(e) => handleReturnQtyChange(item.productId, e.target.value)}
                            className="text-input min-h-[44px] text-center text-base font-semibold"
                          />
                        </label>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">Ghi chú hoàn trả</label>
              <textarea
                value={returnNotes}
                onChange={(e) => setReturnNotes(e.target.value)}
                placeholder="Lý do đại lý yêu cầu trả hàng, thông tin bổ sung..."
                rows="3"
                className="w-full px-3 py-2.5 bg-canvas-light border border-hairline-light rounded-md text-ink focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink text-sm transition-all"
              />
            </div>

            <div className="flex flex-col-reverse gap-3 border-t border-hairline-light pt-4 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline-light" onClick={() => setActiveTab('LIST')}>
                Hủy
              </Button>
              <Button type="submit" variant="primary" loading={submitting} disabled={submitting}>
                Lập phiếu trả hàng
              </Button>
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
          <div className="flex flex-col gap-6">
            <div className="flex flex-col gap-1">
              <div className="text-xs text-shade-60">Mã phiếu trả hàng: <span className="font-semibold text-ink">{qcReceipt.receipt_number}</span></div>
              <div className="text-xs text-shade-60">Đơn xuất gốc: <span className="font-semibold text-ink">{qcReceipt.source_order_code}</span></div>
            </div>

            <div className="border border-hairline-light rounded-lg overflow-hidden">
              <table className="hidden w-full text-left border-collapse md:table">
                <thead>
                  <tr className="bg-canvas-cream border-b border-hairline-light">
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Sản phẩm</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 w-28 text-center">Yêu cầu trả</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 w-28 text-center">Thực tế nhận</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 w-28 text-center">QC Đạt</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 w-28 text-center">QC Lỗi</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Vị trí lưu kho</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {qcItems.map(item => (
                    <tr key={item.receiptItemId} className="hover:bg-canvas-cream/50 transition-colors">
                      <td className="px-6 py-4">
                        <div className="font-semibold text-ink text-sm">{item.name}</div>
                        <div className="text-shade-60 font-mono text-[10px] mt-0.5">{item.sku}</div>
                      </td>
                      <td className="px-6 py-4 text-center font-semibold text-shade-60 text-sm">{item.expectedQty}</td>
                      <td className="px-6 py-4">
                        <input
                          type="number"
                          min="0"
                          value={item.actualQty}
                          onChange={(e) => handleQcValueChange(item.receiptItemId, 'actualQty', e.target.value)}
                          className="w-full px-2 py-1.5 bg-canvas-light border border-hairline-light rounded-md text-ink focus:outline-none focus:ring-1 focus:ring-ink text-center font-semibold text-sm transition-all"
                        />
                      </td>
                      <td className="px-6 py-4">
                        <input
                          type="number"
                          min="0"
                          max={item.actualQty}
                          value={item.passedQty}
                          onChange={(e) => handleQcValueChange(item.receiptItemId, 'passedQty', e.target.value)}
                          className="w-full px-2 py-1.5 bg-canvas-light border border-hairline-light rounded-md text-success-700 focus:outline-none focus:ring-1 focus:ring-ink text-center font-semibold text-sm transition-all"
                        />
                      </td>
                      <td className="px-6 py-4 text-center font-semibold text-danger-600 text-sm">
                        {item.failedQty}
                      </td>
                      <td className="px-6 py-4 flex flex-col gap-2">
                        {item.passedQty > 0 && (
                          <div className="flex flex-col gap-1">
                            <span className="text-[10px] font-semibold uppercase tracking-wider text-shade-60">Vị trí đạt chuẩn</span>
                            <select
                              value={item.passedLocationId}
                              onChange={(e) => handleQcValueChange(item.receiptItemId, 'passedLocationId', e.target.value)}
                              className="w-full px-2 py-1.5 bg-canvas-light border border-hairline-light rounded-md text-ink text-xs focus:outline-none focus:ring-1 focus:ring-ink transition-all"
                            >
                              <option value="">-- Chọn vị trí --</option>
                              {regularBins.map(b => (
                                <option key={b.id} value={b.id}>{b.code} (Còn: {b.capacity_m3 - b.current_volume_m3} m³)</option>
                              ))}
                            </select>
                          </div>
                        )}
                        {item.failedQty > 0 && (
                          <div className="flex flex-col gap-1">
                            <span className="text-[10px] font-semibold uppercase tracking-wider text-shade-60">Khu cách ly lỗi</span>
                            <select
                              value={item.quarantineLocationId}
                              onChange={(e) => handleQcValueChange(item.receiptItemId, 'quarantineLocationId', e.target.value)}
                              className="w-full px-2 py-1.5 bg-canvas-light border border-danger-200 rounded-md text-ink text-xs focus:outline-none focus:ring-1 focus:ring-danger-500 transition-all"
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

              <div className="flex flex-col gap-3 p-4 md:hidden">
                {qcItems.map(item => (
                  <div key={item.receiptItemId} className="rounded-lg border border-hairline-light bg-canvas-light p-4 shadow-level-3">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="text-sm font-semibold text-ink">{item.name}</div>
                        <div className="mt-1 font-mono text-[11px] text-shade-60">{item.sku}</div>
                      </div>
                      <div className="shrink-0 rounded-pill bg-canvas-cream px-3 py-1 text-[11px] font-bold text-shade-60">
                        Yêu cầu: {item.expectedQty}
                      </div>
                    </div>

                    <div className="mt-4 grid grid-cols-2 gap-3">
                      <label className="flex flex-col gap-1.5">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">Thực nhận</span>
                        <input
                          type="number"
                          min="0"
                          value={item.actualQty}
                          onChange={(e) => handleQcValueChange(item.receiptItemId, 'actualQty', e.target.value)}
                          className="text-input min-h-[44px] text-center font-semibold"
                        />
                      </label>
                      <label className="flex flex-col gap-1.5">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">QC đạt</span>
                        <input
                          type="number"
                          min="0"
                          max={item.actualQty}
                          value={item.passedQty}
                          onChange={(e) => handleQcValueChange(item.receiptItemId, 'passedQty', e.target.value)}
                          className="text-input min-h-[44px] text-center font-semibold text-success-700"
                        />
                      </label>
                    </div>

                    <div className="mt-3 rounded-md border border-danger-100 bg-danger-50/50 px-3 py-2 text-xs font-semibold text-danger-700">
                      QC lỗi: {item.failedQty}
                    </div>

                    <div className="mt-4 flex flex-col gap-3">
                      {item.passedQty > 0 && (
                        <label className="flex flex-col gap-1.5">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">Vị trí đạt chuẩn</span>
                          <select
                            value={item.passedLocationId}
                            onChange={(e) => handleQcValueChange(item.receiptItemId, 'passedLocationId', e.target.value)}
                            className="text-input min-h-[44px] text-xs"
                          >
                            <option value="">-- Chọn vị trí --</option>
                            {regularBins.map(b => (
                              <option key={b.id} value={b.id}>{b.code} (Còn: {b.capacity_m3 - b.current_volume_m3} m³)</option>
                            ))}
                          </select>
                        </label>
                      )}
                      {item.failedQty > 0 && (
                        <label className="flex flex-col gap-1.5">
                          <span className="text-[10px] font-bold uppercase tracking-wider text-shade-60">Khu cách ly lỗi</span>
                          <select
                            value={item.quarantineLocationId}
                            onChange={(e) => handleQcValueChange(item.receiptItemId, 'quarantineLocationId', e.target.value)}
                            className="text-input min-h-[44px] border-danger-200 text-xs focus:ring-danger-500"
                          >
                            <option value="">-- Chọn vị trí cách ly --</option>
                            {quarantineBins.map(b => (
                              <option key={b.id} value={b.id}>{b.code}</option>
                            ))}
                          </select>
                        </label>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="flex flex-col-reverse gap-3 border-t border-hairline-light pt-4 sm:flex-row sm:justify-end">
              <Button type="button" variant="outline-light" onClick={() => setShowQcModal(false)}>
                Hủy
              </Button>
              <Button variant="primary" onClick={submitQcSplit} loading={submitting} disabled={submitting}>
                Xác nhận QC & Nhập kho
              </Button>
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
          <div className="flex flex-col gap-4">
            <div className="bg-canvas-cream p-4 rounded-lg border border-hairline-light flex flex-col gap-2">
              <div className="text-xs text-shade-60">Đại lý thụ hưởng: <span className="font-semibold text-ink">{getDealerName(selectedReceipt.dealer_id)}</span></div>
              <div className="text-xs text-shade-60">Phiếu nhập hàng trả: <span className="font-semibold text-ink">{selectedReceipt.receipt_number}</span></div>
            </div>

            <div className="flex flex-col gap-1.5">
              <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">Lý do tạo Credit Note</label>
              <textarea
                value={creditReason}
                onChange={(e) => setCreditReason(e.target.value)}
                placeholder="Nhập lý do hoàn trả công nợ..."
                rows="3"
                className="w-full px-3 py-2.5 bg-canvas-light border border-hairline-light rounded-md text-ink focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink text-sm transition-all"
              />
            </div>

            <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
              <Button type="button" variant="outline-light" onClick={() => setShowCreditModal(false)}>
                Hủy
              </Button>
              <Button variant="aloe" onClick={submitCreditNote} loading={submitting} disabled={submitting}>
                Xác nhận & Khấu trừ công nợ
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default ReturnsWorkspace;
