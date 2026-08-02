import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import returnsService from '../../services/returns.service';

import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';
import Modal from '../../components/common/Modal';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import CorrectionVoucherButton from '../../components/common/CorrectionVoucherButton';
import { ROLES } from '../../utils/constants';
import { Loader2, Receipt, ShieldAlert, Check, Coins, FileText, Truck, Eye } from 'lucide-react';

const ReturnsWorkspace = () => {
  const activeWarehouse = useAuthStore((state) => state.activeWarehouse);
  const { user } = useAuthStore();
  const { addToast } = useUiStore();

  const isAccountingRole = user?.role === ROLES.ACCOUNTANT || user?.role === ROLES.ACCOUNTANT_MANAGER;

  const [returns, setReturns] = useState([]);
  const [loading, setLoading] = useState(true);
  // Dropdown lists
  const [dealers, setDealers] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [inboundReceipts, setInboundReceipts] = useState([]);
  const [regularBins, setRegularBins] = useState([]);
  const [quarantineBins, setQuarantineBins] = useState([]);



  // Detail View Modal State
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [selectedReturnDetail, setSelectedReturnDetail] = useState(null);
  const [loadingDetail, setLoadingDetail] = useState(false);

  // QC Split Modal State
  const [showQcModal, setShowQcModal] = useState(false);
  const [qcReceipt, setQcReceipt] = useState(null);
  const [qcItems, setQcItems] = useState([]);

  // Credit Note Modal State
  const [showCreditModal, setShowCreditModal] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState(null);
  const [creditReason, setCreditReason] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const canManageReturnOperations = ['WAREHOUSE_STAFF', 'STOREKEEPER', 'WAREHOUSE_MANAGER', 'ADMIN', 'CEO'].includes(user?.role);
  const canConfirmSupplierReturn = ['WAREHOUSE_MANAGER', 'ADMIN'].includes(user?.role);
  const supplierReturnStatuses = ['RETURN_TO_SUPPLIER_PENDING', 'RETURNED_TO_SUPPLIER'];

  const openDetailModal = async (ret) => {
    setLoadingDetail(true);
    setShowDetailModal(true);
    try {
      let details;
      if (ret.is_supplier_rtv || ret.type === 'SUPPLIER_RETURN' || ret.supplier_id) {
        const sourceId = ret.source_receipt_id || ret.id;
        details = await inboundService.getReceiptById(sourceId);
      } else {
        details = await returnsService.getReturnById(ret.id);
      }
      setSelectedReturnDetail({ ...ret, ...details });
    } catch (e) {
      addToast('Không thể tải chi tiết phiếu hoàn trả', 'error');
      setSelectedReturnDetail(ret);
    } finally {
      setLoadingDetail(false);
    }
  };

  useEffect(() => {
    if (activeWarehouse) {
      fetchData();
    }
  }, [activeWarehouse]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const data = await returnsService.getReturns({ warehouse_id: activeWarehouse.id });

      const dlData = await masterDataService.getDealers();
      setDealers(dlData || []);

      const supData = await masterDataService.getSuppliers();
      setSuppliers(supData || []);

      const receiptData = await inboundService.getReceipts(activeWarehouse.id);
      setInboundReceipts(receiptData || []);

      const supplierReturnReceipts = (receiptData || [])
        .filter(receipt => supplierReturnStatuses.includes(receipt.status))
        .map(receipt => ({
          ...receipt,
          id: `supplier-${receipt.id}`,
          source_receipt_id: receipt.id,
          type: 'SUPPLIER_RETURN',
          is_supplier_rtv: true,
        }));

      const returnRows = [...(data || []), ...supplierReturnReceipts];
      setReturns(isAccountingRole ? returnRows.filter(r => r.status === 'APPROVED') : returnRows);

      if (!isAccountingRole) {
        const locs = await masterDataService.getBinLocations(activeWarehouse.id);
        setRegularBins(locs.filter(l => !l.is_quarantine));
        setQuarantineBins(locs.filter(l => l.is_quarantine));
      }
    } catch (e) {
      addToast('Lỗi tải dữ liệu hàng trả', 'error');
    } finally {
      setLoading(false);
    }
  };



  const openQcSplit = async (receipt) => {
    try {
      const details = await returnsService.getReturnById(receipt.id);
      setQcReceipt(details);
      const items = details.items.map(item => {
        const receiptItemId = item.receipt_item_id ?? item.receiptItemId ?? item.id;
        const productId = item.product_id ?? item.productId;
        const expectedQty = item.expected_qty ?? item.expectedQty ?? 0;
        return {
          receiptItemId,
          sku: item.product_sku || item.productSku || `SKU-${productId}`,
          name: item.product_name || item.productName || `Sản phẩm ${productId}`,
          expectedQty,
          actualQty: expectedQty,
          passedQty: expectedQty,
          failedQty: 0,
          passedLocationId: regularBins[0]?.id || '',
          quarantineLocationId: quarantineBins[0]?.id || ''
        };
      });
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

  const confirmSupplierReturn = async (ret) => {
    setSubmitting(true);
    try {
      await inboundService.confirmReturnToSupplier(
        ret.source_receipt_id || ret.id,
        'Xác nhận bàn giao trả hàng cho NCC',
        ret.version || 0,
      );
      addToast(`Đã xác nhận trả NCC cho phiếu ${ret.receipt_number}`, 'success');
      fetchData();
    } catch (e) {
      addToast(e.message || 'Lỗi xác nhận trả NCC', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const getDealerName = (dealerId) => {
    const d = dealers.find(dl => dl.id === Number(dealerId));
    return d ? d.name : `Đại lý ID: ${dealerId}`;
  };

  const getSupplierName = (supplierId) => {
    const s = suppliers.find(sup => sup.id === Number(supplierId));
    return s ? s.company_name : `NCC ID: ${supplierId}`;
  };

  const filteredReturns = returns.filter(ret => ret.supplier_id || ret.type === 'SUPPLIER_RETURN');

  const renderReturnStatusBadge = (ret) => {
    if (ret.status === 'RETURN_TO_SUPPLIER_PENDING') {
      return (
        <Badge size="sm" colorClassName="bg-danger-100 text-danger-800 border-danger-300">
          Chờ trả NCC
        </Badge>
      );
    }
    if (ret.status === 'RETURNED_TO_SUPPLIER') {
      return (
        <Badge size="sm" colorClassName="bg-shade-20 text-shade-80 border-shade-40">
          Đã trả NCC
        </Badge>
      );
    }
    if (ret.status === 'APPROVED') {
      return <Badge size="sm" type="success">Đã duyệt nhập kho</Badge>;
    }
    return <Badge size="sm" type="warning">Nháp / Chờ QC</Badge>;
  };

  const renderReturnAction = (ret) => {
    if (ret.status === 'RETURN_TO_SUPPLIER_PENDING' && canConfirmSupplierReturn) {
      return (
        <button
          onClick={() => confirmSupplierReturn(ret)}
          disabled={submitting}
          className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill bg-danger-600 text-white hover:bg-danger-700 disabled:opacity-60 text-xs font-bold transition-colors"
        >
          <Truck className="w-3.5 h-3.5" />
          Xác nhận trả NCC
        </button>
      );
    }
    if (ret.status === 'DRAFT' && canManageReturnOperations) {
      return (
        <button
          onClick={() => openQcSplit(ret)}
          className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill border border-ink bg-canvas-light text-ink hover:bg-canvas-cream text-xs font-semibold transition-colors"
        >
          <ShieldAlert className="w-3.5 h-3.5" />
          QC Phân tách & Nhập kho
        </button>
      );
    }
    if (ret.status === 'APPROVED' && !ret.credit_note_generated) {
      return (
        <button
          onClick={() => openCreditNoteModal(ret)}
          className="inline-flex items-center gap-1.5 px-3 py-1 rounded-pill btn-pill-aloe text-xs font-semibold transition-colors"
        >
          <Coins className="w-3.5 h-3.5" />
          Tạo Credit Note
        </button>
      );
    }
    if (ret.credit_note_generated && ret.credit_note_id) {
      // Credit Note itself is immutable once created - a wrong amount can only be
      // fixed via Correction Voucher (US-WMS-29), same as invoices/payments.
      return (
        <CorrectionVoucherButton
          referenceType="CREDIT_NOTE"
          referenceId={ret.credit_note_id}
          documentLabel={`Credit Note của ${ret.receipt_number}`}
          onSuccess={fetchData}
        />
      );
    }
    return <span className="text-shade-50 text-[10px] font-medium">Hoàn tất</span>;
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Nhập kho</span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Trả hàng cho NCC
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            {isAccountingRole
              ? 'Sinh Debit Note khấu trừ công nợ cho các phiếu xuất trả NCC đã được duyệt.'
              : 'Danh sách phiếu xuất trả hàng về Nhà cung cấp (RTV).'}
          </p>
        </div>
      </div>

      <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden flex flex-col">
          {/* No filter tabs needed — only NCC returns shown */}

          {loading ? (
            <div className="flex flex-col items-center justify-center py-20 gap-3">
              <Loader2 className="w-8 h-8 animate-spin text-ink" />
              <span className="text-shade-60 text-xs font-light">Đang tải danh sách hàng trả...</span>
            </div>
          ) : filteredReturns.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-20 text-center px-4">
              <Receipt className="w-12 h-12 text-shade-40 mb-3" />
              <h3 className="font-semibold text-ink text-sm">Không có phiếu trả hàng nào</h3>
              <p className="text-shade-50 max-w-sm mt-1 text-xs font-light">
                Hiện tại không có phiếu hoàn trả nào thỏa mãn bộ lọc tại kho này.
              </p>
            </div>
          ) : (
            <>
              <div className="hidden md:block overflow-x-auto">
                <table className="data-table-grid w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-canvas-cream border-b border-hairline-light">
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã phiếu trả</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Loại hoàn trả</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Đối tác (Đại lý / NCC)</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Ngày tạo</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Trạng thái</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Chứng từ công nợ</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Hành động</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {filteredReturns.map((ret) => (
                      <tr key={ret.id} className="hover:bg-canvas-cream/50 transition-colors">
                        <td className="px-6 py-4 font-medium text-ink">
                          <div className="flex items-center gap-2">
                            <FileText className="w-4 h-4 text-shade-40 shrink-0" />
                            {ret.receipt_number}
                          </div>
                        </td>
                        <td className="px-6 py-4 text-xs">
                          {ret.dealer_id || ret.type === 'RETURN' ? (
                            <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-blue-700 bg-blue-50 px-2 py-0.5 rounded border border-blue-200">
                              <Building2 className="w-3 h-3" /> Đại lý trả hàng
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-amber-800 bg-amber-50 px-2 py-0.5 rounded border border-amber-200">
                              <Truck className="w-3 h-3" /> Trả hàng NCC
                            </span>
                          )}
                        </td>
                        <td className="px-6 py-4 text-shade-70 font-semibold text-xs">
                          {ret.dealer_id ? getDealerName(ret.dealer_id) : getSupplierName(ret.supplier_id)}
                        </td>
                        <td className="px-6 py-4 text-shade-60 text-xs">{ret.document_date || ret.created_at?.slice(0, 10)}</td>
                        <td className="px-6 py-4">
                          {renderReturnStatusBadge(ret)}
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
                          <div className="flex justify-end items-center gap-2">
                            {renderReturnAction(ret)}
                            <button
                              onClick={() => openDetailModal(ret)}
                              className="p-1.5 hover:bg-canvas-cream rounded-full text-shade-50 hover:text-ink transition-colors flex items-center justify-center border border-hairline-light"
                              title="Xem chi tiết phiếu trả hàng"
                            >
                              <Eye className="w-4 h-4" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="flex flex-col gap-3 p-4 md:hidden">
                {filteredReturns.map((ret) => (
                  <div key={ret.id} className="rounded-lg border border-hairline-light bg-canvas-light p-4 shadow-level-3">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <div className="flex items-center gap-2 font-semibold text-ink">
                          <FileText className="h-4 w-4 shrink-0 text-shade-40" />
                          <span className="truncate">{ret.receipt_number}</span>
                        </div>
                      </div>
                      {renderReturnStatusBadge(ret)}
                    </div>

                    <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
                      <div className="rounded-md bg-canvas-cream p-2">
                        <span className="block text-[10px] uppercase tracking-wider text-shade-50">Đối tác</span>
                        <span className="font-semibold text-ink">
                          {ret.dealer_id ? getDealerName(ret.dealer_id) : getSupplierName(ret.supplier_id)}
                        </span>
                      </div>
                      <div className="rounded-md bg-canvas-cream p-2">
                        <span className="block text-[10px] uppercase tracking-wider text-shade-50">Ngày tạo</span>
                        <span className="font-semibold text-ink">{ret.document_date || ret.created_at?.slice(0, 10)}</span>
                      </div>
                    </div>
                    <div className="mt-3 flex justify-between items-center">
                      <button
                        onClick={() => openDetailModal(ret)}
                        className="px-2.5 py-1 rounded-pill text-xs font-medium text-shade-60 hover:text-ink bg-canvas-cream flex items-center gap-1 border border-hairline-light"
                      >
                        <Eye className="w-3.5 h-3.5" />
                        <span>Xem chi tiết</span>
                      </button>
                      {renderReturnAction(ret)}
                    </div>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>

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
              <table className="data-table-grid hidden w-full text-left border-collapse md:table">
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

      {/* Detail Modal */}
      <Modal
        isOpen={showDetailModal}
        onClose={() => setShowDetailModal(false)}
        title="Chi tiết phiếu xuất / nhập hoàn trả"
        maxWidth="max-w-3xl"
      >
        {loadingDetail ? (
          <div className="flex flex-col items-center justify-center p-12 gap-3">
            <Loader2 className="w-8 h-8 animate-spin text-ink" />
            <span className="text-xs text-shade-50">Đang tải chi tiết đơn hàng...</span>
          </div>
        ) : selectedReturnDetail ? (
          <div className="flex flex-col gap-5 text-xs">
            {/* Top Header info */}
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center bg-canvas-cream p-4 rounded-lg border border-hairline-light gap-2">
              <div>
                <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider block mb-0.5">
                  Mã chứng từ / Phiếu
                </span>
                <h3 className="text-base font-bold text-ink flex items-center gap-2">
                  {selectedReturnDetail.receipt_number || selectedReturnDetail.receiptNumber}
                  {renderReturnStatusBadge(selectedReturnDetail)}
                </h3>
              </div>
              <div className="sm:text-right">
                <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider block mb-0.5">Ngày lập phiếu</span>
                <span className="font-bold text-ink">{selectedReturnDetail.document_date || selectedReturnDetail.created_at?.slice(0, 10)}</span>
              </div>
            </div>

            {/* Overview grid */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 bg-canvas-light p-4 rounded-lg border border-hairline-light">
              <div>
                <span className="text-shade-50 block mb-0.5">Loại hoàn trả:</span>
                <span className="font-bold text-ink">
                  {selectedReturnDetail.dealer_id || selectedReturnDetail.type === 'RETURN'
                    ? 'Đại lý trả hàng (Dealer Return)'
                    : 'Xuất trả Nhà cung cấp (Supplier RTV)'}
                </span>
              </div>

              <div>
                <span className="text-shade-50 block mb-0.5">Đối tác:</span>
                <span className="font-bold text-ink">
                  {selectedReturnDetail.dealer_id
                    ? getDealerName(selectedReturnDetail.dealer_id)
                    : getSupplierName(selectedReturnDetail.supplier_id)}
                </span>
              </div>

              {selectedReturnDetail.source_order_code && (
                <div>
                  <span className="text-shade-50 block mb-0.5">Đơn hàng / PO gốc:</span>
                  <span className="font-mono font-bold text-ink">{selectedReturnDetail.source_order_code}</span>
                </div>
              )}

              {(selectedReturnDetail.notes || selectedReturnDetail.rejection_reason) && (
                <div className="sm:col-span-2 mt-1">
                  <span className="text-shade-50 block mb-1 font-semibold">Lý do & Ghi chú xuất/nhập trả:</span>
                  <p className="font-medium text-shade-80 bg-canvas-cream border border-hairline-light p-2.5 rounded-md italic">
                    {selectedReturnDetail.rejection_reason || selectedReturnDetail.notes}
                  </p>
                </div>
              )}
            </div>

            {/* Items table */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <h4 className="text-xs font-bold uppercase tracking-wider text-shade-60">
                  Danh sách sản phẩm hoàn trả ({selectedReturnDetail.items?.length || 0})
                </h4>
              </div>
              <div className="border border-hairline-light rounded-lg overflow-hidden bg-canvas-light">
                <table className="data-table-grid w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-canvas-cream border-b border-hairline-light">
                      <th className="px-4 py-3 font-semibold text-shade-60 uppercase">Sản phẩm</th>
                      <th className="px-4 py-3 font-semibold text-shade-60 text-right uppercase">SL Gốc / Dự kiến</th>
                      <th className="px-4 py-3 font-semibold text-shade-60 text-right uppercase">SL Trả / Nhận</th>
                      <th className="px-4 py-3 font-semibold text-shade-60 text-right uppercase">Hàng QC Lỗi</th>
                      <th className="px-4 py-3 font-semibold text-shade-60 uppercase">Kết quả & Lý do</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {(selectedReturnDetail.items || []).map((item, idx) => {
                      const expected = item.expected_qty ?? item.expectedQty ?? 0;
                      const actual = item.actual_qty ?? item.actualQty ?? expected;
                      const failed = item.qc_failed_qty ?? item.qcFailedQty ?? item.sample_failed_qty ?? 0;
                      const reason = item.qc_failure_reason ?? item.qcFailureReason;
                      return (
                        <tr key={item.id || idx} className="hover:bg-canvas-cream/50 transition-colors">
                          <td className="px-4 py-3">
                            <span className="font-bold block text-ink">
                              {item.product_name || item.name || `Sản phẩm ID: ${item.product_id}`}
                            </span>
                            <span className="text-[10px] text-shade-40 font-mono block mt-0.5">
                              {item.product_sku || item.sku || `SKU-${item.product_id}`}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-right font-semibold text-shade-60">{expected}</td>
                          <td className="px-4 py-3 text-right font-bold text-ink">{actual}</td>
                          <td className="px-4 py-3 text-right font-bold text-danger-600">
                            {failed > 0 ? failed : '-'}
                          </td>
                          <td className="px-4 py-3">
                            {failed > 0 ? (
                              <span className="text-danger-600 font-semibold italic text-[11px]">
                                {reason || 'Lỗi QC'}
                              </span>
                            ) : (
                              <span className="text-success-700 font-semibold text-[11px]">
                                Đạt QC / Hoàn tất
                              </span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Footer */}
            <div className="flex justify-end pt-3 border-t border-hairline-light">
              <Button variant="outline-light" onClick={() => setShowDetailModal(false)}>
                Đóng
              </Button>
            </div>
          </div>
        ) : null}
      </Modal>
    </div>
  );
};

export default ReturnsWorkspace;
