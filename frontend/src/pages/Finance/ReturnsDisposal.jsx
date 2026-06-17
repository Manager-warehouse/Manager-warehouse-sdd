import React, { useState, useEffect, useCallback } from 'react';
import { financeService } from '../../services/finance.service';
import { masterDataService } from '../../services/masterData.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { RefreshCw, Trash2, ClipboardCheck, DollarSign, Plus, ArrowRight, ShieldAlert, CheckCircle2, ChevronRight, Image } from 'lucide-react';

const ReturnsDisposal = () => {
  const { addToast } = useUiStore();
  const { user, activeWarehouse, hasRole } = useAuthStore();
  
  const isStorekeeper = hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN);
  const isManager = hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN);
  const isAccountant = hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ADMIN);
  const isCeo = hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN);

  const [activeTab, setActiveTab] = useState('returns');
  const [loading, setLoading] = useState(false);

  // --- Data States ---
  const [returns, setReturns] = useState([]);
  const [quarantineItems, setQuarantineItems] = useState([]);
  const [disposals, setDisposals] = useState([]);
  const [dealers, setDealers] = useState([]);
  const [deliveryOrders, setDeliveryOrders] = useState([]);
  const [products, setProducts] = useState([]);

  // --- Modal / Active Operations States ---
  const [showCreateReturn, setShowCreateReturn] = useState(false);
  const [showQcModal, setShowQcModal] = useState(false);
  const [showPutawayModal, setShowPutawayModal] = useState(false);
  const [showDisposalModal, setShowDisposalModal] = useState(false);
  const [activeReturn, setActiveReturn] = useState(null);
  
  // --- Form States: Customer Return Creation ---
  const [returnForm, setReturnForm] = useState({
    dealerId: '',
    deliveryOrderId: '',
    contactPerson: '',
    documentDate: new Date().toISOString().slice(0, 10),
    notes: '',
    items: [{ productId: '', expectedQty: '' }]
  });

  // --- Form States: Split QC ---
  const [qcForm, setQcForm] = useState({
    expectedVersion: 0,
    items: []
  });

  // --- Form States: Putaway ---
  const [putawayForm, setPutawayForm] = useState({
    expectedVersion: 0,
    putawayItems: []
  });

  // --- Form States: Scrap Disposal Proposal ---
  const [activeQuarantineItem, setActiveQuarantineItem] = useState(null);
  const [disposalForm, setDisposalForm] = useState({
    quantity: '',
    cause: '',
    imageUrl: '',
    documentDate: new Date().toISOString().slice(0, 10)
  });

  // --- Load Data ---
  const loadData = useCallback(async () => {
    if (!activeWarehouse) return;
    setLoading(true);
    try {
      if (activeTab === 'returns') {
        const returnsList = await financeService.getReturns(activeWarehouse.id);
        setReturns(returnsList || []);
        const dlrs = await masterDataService.getDealers();
        setDealers(dlrs || []);
        // Load DOs that are delivered
        const allDos = await financeService.getAgingReport(); // pricing/aging DO data
        // For mock simplification, fetch all dealers DOs
      } else {
        const qItems = await financeService.getQuarantineItems(activeWarehouse.id);
        setQuarantineItems(qItems || []);
        const dps = await financeService.getDisposals();
        setDisposals(dps.filter(d => d.warehouseId === activeWarehouse.id) || []);
      }
      const prds = await masterDataService.getProducts();
      setProducts(prds || []);
    } catch (err) {
      console.error(err);
      addToast('Không thể tải dữ liệu', 'error');
    } finally {
      setLoading(false);
    }
  }, [activeWarehouse, activeTab, addToast]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Load DOs when dealer is selected
  useEffect(() => {
    if (!returnForm.dealerId) return;
    const fetchDealerDos = async () => {
      try {
        // Fetch DOs for this dealer that are DELIVERED
        // For mock, we reuse DO list from memory
        const allInvs = await financeService.getInvoices({ dealerId: returnForm.dealerId });
        setDeliveryOrders(allInvs.map(i => ({ id: i.do_id, doNumber: i.do_number })) || []);
      } catch (err) {
        console.error(err);
      }
    };
    fetchDealerDos();
  }, [returnForm.dealerId]);

  // --- Actions: Customer Returns ---
  const handleAddReturnItem = () => {
    setReturnForm(prev => ({
      ...prev,
      items: [...prev.items, { productId: '', expectedQty: '' }]
    }));
  };

  const handleRemoveReturnItem = (index) => {
    setReturnForm(prev => ({
      ...prev,
      items: prev.items.filter((_, idx) => idx !== index)
    }));
  };

  const handleReturnItemChange = (index, field, value) => {
    const updated = [...returnForm.items];
    updated[index][field] = value;
    setReturnForm(prev => ({ ...prev, items: updated }));
  };

  const handleCreateReturnSubmit = async (e) => {
    e.preventDefault();
    if (!isStorekeeper) {
      addToast('Chỉ Thủ kho mới được quyền lập phiếu hoàn trả', 'error');
      return;
    }
    try {
      await financeService.createReturn({
        warehouseId: activeWarehouse.id,
        dealerId: returnForm.dealerId,
        deliveryOrderId: returnForm.deliveryOrderId,
        contactPerson: returnForm.contactPerson,
        documentDate: returnForm.documentDate,
        notes: returnForm.notes,
        items: returnForm.items
      });
      addToast('Lập phiếu nháp nhận hàng hoàn thành công', 'success');
      setShowCreateReturn(false);
      setReturnForm({
        dealerId: '',
        deliveryOrderId: '',
        contactPerson: '',
        documentDate: new Date().toISOString().slice(0, 10),
        notes: '',
        items: [{ productId: '', expectedQty: '' }]
      });
      loadData();
    } catch (err) {
      addToast(err.message || 'Lỗi lập phiếu hoàn trả', 'error');
    }
  };

  const openQcModal = (ret) => {
    setActiveReturn(ret);
    setQcForm({
      expectedVersion: ret.version,
      items: ret.items.map(it => ({
        receiptItemId: it.id,
        productId: it.productId,
        actualQty: it.expectedQty,
        qcPassedQty: it.expectedQty,
        qcFailedQty: 0,
        qcFailureReason: ''
      }))
    });
    setShowQcModal(true);
  };

  const handleQcSubmit = async (e) => {
    e.preventDefault();
    try {
      await financeService.submitReturnQc(activeReturn.id, qcForm);
      addToast('Ghi nhận kết quả QC hàng hoàn thành công', 'success');
      setShowQcModal(false);
      loadData();
    } catch (err) {
      addToast(err.message || 'Lỗi ghi nhận kết quả QC', 'error');
    }
  };

  const handleApproveReturn = async (ret) => {
    if (!isManager) {
      addToast('Chỉ Trưởng kho mới được duyệt phiếu hoàn trả', 'error');
      return;
    }
    try {
      await financeService.approveReturn(ret.id, ret.version);
      addToast('Duyệt nhập kho hàng hoàn thành công', 'success');
      loadData();
    } catch (err) {
      addToast(err.message || 'Lỗi phê duyệt phiếu', 'error');
    }
  };

  const openPutawayModal = (ret) => {
    setActiveReturn(ret);
    setPutawayForm({
      expectedVersion: ret.version,
      putawayItems: ret.items.map(it => ({
        receiptItemId: it.id,
        passedLocationId: '1', // regular bin ID
        failedLocationId: '2'  // quarantine bin ID
      }))
    });
    setShowPutawayModal(true);
  };

  const handlePutawaySubmit = async (e) => {
    e.preventDefault();
    try {
      await financeService.completeReturnPutaway(activeReturn.id, putawayForm);
      addToast('Putaway hoàn tất. Tồn kho đã được cập nhật.', 'success');
      setShowPutawayModal(false);
      loadData();
    } catch (err) {
      addToast(err.message || 'Lỗi hoàn tất putaway', 'error');
    }
  };

  const handleCreateCreditNote = async (ret) => {
    if (!isAccountant) {
      addToast('Chỉ Kế toán viên mới được quyền lập Credit Note', 'error');
      return;
    }
    const reason = prompt('Nhập lý do tạo Credit Note:', `Hoàn trả tiền hàng theo phiếu hoàn ${ret.receiptNumber}`);
    if (reason === null) return;
    if (!reason.trim()) {
      addToast('Lý do lập Credit Note là bắt buộc', 'error');
      return;
    }
    try {
      await financeService.createReturnCreditNote(ret.id, {
        reason,
        documentDate: new Date().toISOString().slice(0, 10),
        expectedVersion: ret.version
      });
      addToast('Lập Credit Note cấn trừ công nợ thành công', 'success');
      loadData();
    } catch (err) {
      addToast(err.message || 'Lỗi tạo Credit Note', 'error');
    }
  };

  // --- Actions: Scrap Disposal ---
  const openDisposalModal = (qItem) => {
    setActiveQuarantineItem(qItem);
    setDisposalForm({
      quantity: qItem.qcFailedQty,
      cause: '',
      imageUrl: '',
      documentDate: new Date().toISOString().slice(0, 10)
    });
    setShowDisposalModal(true);
  };

  const handleCreateDisposalSubmit = async (e) => {
    e.preventDefault();
    if (!isManager) {
      addToast('Chỉ Trưởng kho mới được đề xuất tiêu hủy', 'error');
      return;
    }
    if (Number(disposalForm.quantity) > activeQuarantineItem.qcFailedQty) {
      addToast('Số lượng tiêu hủy vượt quá tồn cách ly hiện có', 'error');
      return;
    }
    try {
      await financeService.createDisposal({
        warehouseId: activeWarehouse.id,
        productId: products.find(p => p.sku === activeQuarantineItem.productSku)?.id || 1,
        batchId: activeQuarantineItem.batchId,
        locationId: activeQuarantineItem.locationId,
        quantity: disposalForm.quantity,
        cause: disposalForm.cause,
        imageUrl: disposalForm.imageUrl,
        documentDate: disposalForm.documentDate
      });
      addToast('Lập đề xuất tiêu hủy thành công. Chờ phê duyệt.', 'success');
      setShowDisposalModal(false);
      loadData();
    } catch (err) {
      addToast(err.message || 'Lỗi đề xuất tiêu hủy', 'error');
    }
  };

  const handleApproveDisposal = async (disp) => {
    const isOverLimit = disp.totalValueEstimate > 100000000;
    if (isOverLimit && !isCeo) {
      addToast('Biên bản tiêu hủy vượt hạn mức 100M VND, chỉ CEO mới được duyệt', 'error');
      return;
    }
    if (!isOverLimit && !isManager) {
      addToast('Chỉ Trưởng kho hoặc CEO mới được duyệt tiêu hủy', 'error');
      return;
    }
    try {
      await financeService.approveDisposal(disp.id, disp.version);
      addToast('Phê duyệt tiêu hủy và giảm trừ tồn kho thành công', 'success');
      loadData();
    } catch (err) {
      addToast(err.message || 'Lỗi phê duyệt tiêu hủy', 'error');
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Nghiệp vụ Kho / Tài chính
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Hàng hoàn trả & Tiêu hủy hàng hỏng
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Quản lý tiếp nhận hàng hoàn từ Đại lý, phân loại QC, cấn trừ công nợ và phê duyệt tiêu hủy hàng hư hỏng từ Quarantine Zone.
        </p>
      </div>

      {/* Tab Selector */}
      <div className="flex border-b border-hairline-light">
        <button
          className={`px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'returns'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => setActiveTab('returns')}
        >
          Hoàn trả hàng đại lý (US-WMS-24)
        </button>
        <button
          className={`px-5 py-3 text-xs font-semibold uppercase tracking-wider border-b-2 transition-colors ${
            activeTab === 'disposal'
              ? 'border-ink text-ink font-bold'
              : 'border-transparent text-shade-40 hover:text-ink'
          }`}
          onClick={() => setActiveTab('disposal')}
        >
          Tiêu hủy hàng lỗi (US-WMS-04)
        </button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20 text-shade-50">
          <svg className="animate-spin h-6 w-6 text-ink mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          <span>Đang tải dữ liệu...</span>
        </div>
      ) : (
        <>
          {activeTab === 'returns' && (
            <div className="flex flex-col gap-4">
              <div className="flex justify-between items-center">
                <h2 className="text-sm font-semibold uppercase tracking-wider text-ink">Danh sách phiếu hoàn trả</h2>
                {isStorekeeper && (
                  <Button variant="primary" onClick={() => setShowCreateReturn(true)} className="flex items-center gap-1.5 py-2">
                    <Plus className="w-3.5 h-3.5" />
                    Lập phiếu hoàn trả
                  </Button>
                )}
              </div>

              {/* Returns List Table */}
              <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
                <table className="w-full border-collapse text-left text-xs">
                  <thead>
                    <tr className="bg-canvas-cream/50 border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                      <th className="p-4">Mã phiếu</th>
                      <th className="p-4">Đại lý</th>
                      <th className="p-4">DO tham chiếu</th>
                      <th className="p-4">Ngày nhận</th>
                      <th className="p-4">Trạng thái</th>
                      <th className="p-4 text-center">Thao tác</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {returns.length === 0 ? (
                      <tr>
                        <td colSpan="6" className="p-8 text-center text-shade-40 italic">
                          Chưa có phiếu hoàn trả nào phát sinh tại kho này.
                        </td>
                      </tr>
                    ) : (
                      returns.map(ret => (
                        <tr key={ret.id} className="hover:bg-canvas-cream/50">
                          <td className="p-4 font-semibold text-ink">{ret.receiptNumber}</td>
                          <td className="p-4">{dealers.find(d => d.id === ret.dealer_id)?.name || `Đại lý #${ret.dealer_id}`}</td>
                          <td className="p-4 text-shade-60 font-mono">#{ret.delivery_order_id}</td>
                          <td className="p-4 text-shade-50">{ret.documentDate}</td>
                          <td className="p-4">
                            <span className={`px-2 py-0.5 rounded-pill text-[9px] font-bold uppercase ${
                              ret.status === 'DRAFT' ? 'bg-gray-100 text-gray-700' :
                              ret.status === 'QC_COMPLETED' ? 'bg-yellow-100 text-yellow-800' :
                              ret.status === 'APPROVED' ? 'bg-aloe-10 text-ink' : 'bg-green-100 text-green-800'
                            }`}>
                              {ret.status}
                            </span>
                          </td>
                          <td className="p-4 text-center">
                            <div className="flex justify-center gap-2">
                              {ret.status === 'DRAFT' && isStorekeeper && (
                                <Button onClick={() => openQcModal(ret)} className="px-3 py-1 bg-yellow-100 text-yellow-800 border border-yellow-200">
                                  Kiểm QC
                                </Button>
                              )}
                              {ret.status === 'QC_COMPLETED' && isManager && (
                                <Button onClick={() => handleApproveReturn(ret)} className="px-3 py-1 bg-aloe-10 text-ink border border-aloe-10">
                                  Phê duyệt
                                </Button>
                              )}
                              {ret.status === 'APPROVED' && isStorekeeper && (
                                <Button onClick={() => openPutawayModal(ret)} className="px-3 py-1 bg-blue-100 text-blue-800 border border-blue-200">
                                  Putaway
                                </Button>
                              )}
                              {ret.status === 'APPROVED' && isAccountant && (
                                <Button onClick={() => handleCreateCreditNote(ret)} className="px-3 py-1 bg-green-100 text-green-800 border border-green-200 flex items-center gap-1">
                                  <DollarSign className="w-3 h-3" />
                                  Lập Credit Note
                                </Button>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {activeTab === 'disposal' && (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Left Column: Quarantine Items Workspace */}
              <div className="lg:col-span-2 flex flex-col gap-4">
                <h2 className="text-sm font-semibold uppercase tracking-wider text-ink">Hàng hư hỏng chờ xử lý (Quarantine Workspace)</h2>
                <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
                  <table className="w-full border-collapse text-left text-xs">
                    <thead>
                      <tr className="bg-canvas-cream/50 border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                        <th className="p-3">Sản phẩm</th>
                        <th className="p-3">Lý do QC lỗi</th>
                        <th className="p-3">Số lượng lỗi</th>
                        <th className="p-3 text-right">Giá trị ước tính</th>
                        <th className="p-3 text-center">Hành động</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {quarantineItems.length === 0 ? (
                        <tr>
                          <td colSpan="5" className="p-8 text-center text-shade-40 italic">
                            Hiện không có hàng hư hỏng cách ly nào trong Quarantine Zone.
                          </td>
                        </tr>
                      ) : (
                        quarantineItems.map(item => (
                          <tr key={item.id} className="hover:bg-canvas-cream/50">
                            <td className="p-3">
                              <div className="font-semibold text-ink">{item.productName}</div>
                              <div className="text-[10px] text-shade-40">SKU: {item.productSku} | Phiếu: {item.receiptNumber}</div>
                            </td>
                            <td className="p-3 text-red-700 italic font-medium">{item.qcFailureReason}</td>
                            <td className="p-3 font-bold text-ink">{item.qcFailedQty} {item.unit}</td>
                            <td className="p-3 text-right font-medium">{item.totalValue.toLocaleString()}đ</td>
                            <td className="p-3 text-center">
                              {isManager && (
                                <Button onClick={() => openDisposalModal(item)} className="px-3 py-1 bg-red-100 text-red-700 border border-red-200">
                                  Tiêu hủy
                                </Button>
                              )}
                            </td>
                          </tr>
                        ))
                      )}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Right Column: Pending Disposal Adjustments */}
              <div className="flex flex-col gap-4">
                <h2 className="text-sm font-semibold uppercase tracking-wider text-ink">Đề xuất tiêu hủy chờ duyệt</h2>
                <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 p-4 flex flex-col gap-4 overflow-y-auto max-h-[500px]">
                  {disposals.length === 0 ? (
                    <div className="text-center py-10 text-shade-40 italic text-xs">
                      Không có đề xuất tiêu hủy nào đang chờ duyệt.
                    </div>
                  ) : (
                    disposals.map(disp => (
                      <div key={disp.id} className="border border-hairline-light rounded p-3 bg-canvas-cream/30 flex flex-col gap-2 relative">
                        <div className="flex justify-between items-start">
                          <div>
                            <span className="text-[10px] font-bold text-ink block font-mono">{disp.adjustmentNumber}</span>
                            <span className="text-[9px] text-shade-40 block">{disp.created_at?.slice(0, 10)}</span>
                          </div>
                          <span className={`px-2 py-0.5 rounded-pill text-[9px] font-bold ${disp.confirmed ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'}`}>
                            {disp.confirmed ? 'Đã duyệt' : 'Chờ duyệt'}
                          </span>
                        </div>
                        
                        <div className="text-xs text-shade-60">
                          <div>Sản phẩm ID: <strong>{disp.productId}</strong></div>
                          <div>Số lượng tiêu hủy: <strong>{disp.quantity} cái</strong></div>
                          <div>Giá trị: <strong className="text-red-700">{disp.totalValueEstimate?.toLocaleString()}đ</strong></div>
                          <div className="mt-1 pt-1 border-t border-hairline-light">Lý do: <em>{disp.cause}</em></div>
                        </div>

                        {!disp.confirmed && (
                          <div className="mt-2 pt-2 border-t border-hairline-light flex justify-end">
                            <Button
                              onClick={() => handleApproveDisposal(disp)}
                              className="w-full text-center py-1 text-[11px] bg-red-600 text-white rounded-pill hover:bg-red-700 flex justify-center items-center gap-1"
                            >
                              <ClipboardCheck className="w-3.5 h-3.5" />
                              Phê duyệt tiêu hủy
                            </Button>
                          </div>
                        )}
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          )}
        </>
      )}

      {/* --- MODAL: Create Return --- */}
      {showCreateReturn && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 max-w-lg w-full flex flex-col gap-4 shadow-level-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-ink pb-2 border-b border-hairline-light">Tạo phiếu hoàn trả mới</h3>
            <form onSubmit={handleCreateReturnSubmit} className="flex flex-col gap-3">
              <div className="grid grid-cols-2 gap-3">
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-semibold">Đại lý</label>
                  <select
                    value={returnForm.dealerId}
                    onChange={e => setReturnForm(prev => ({ ...prev, dealerId: e.target.value }))}
                    className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none"
                    required
                  >
                    <option value="">-- Chọn đại lý --</option>
                    {dealers.map(d => (
                      <option key={d.id} value={d.id}>{d.name}</option>
                    ))}
                  </select>
                </div>
                <div className="flex flex-col gap-1">
                  <label className="text-xs font-semibold">DO gốc tham chiếu</label>
                  <select
                    value={returnForm.deliveryOrderId}
                    onChange={e => setReturnForm(prev => ({ ...prev, deliveryOrderId: e.target.value }))}
                    className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none"
                    required
                    disabled={!returnForm.dealerId}
                  >
                    <option value="">-- Chọn DO nợ --</option>
                    {deliveryOrders.map(doOrd => (
                      <option key={doOrd.id} value={doOrd.id}>{doOrd.doNumber}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex flex-col gap-1">
                <Input
                  id="contactPerson"
                  label="Người liên hệ đại lý"
                  value={returnForm.contactPerson}
                  onChange={e => setReturnForm(prev => ({ ...prev, contactPerson: e.target.value }))}
                  required
                />
              </div>

              {/* Items Section */}
              <div className="flex flex-col gap-2">
                <div className="flex justify-between items-center">
                  <label className="text-xs font-semibold">Sản phẩm hoàn trả</label>
                  <Button type="button" onClick={handleAddReturnItem} className="text-[10px] px-2 py-1 bg-canvas-cream text-ink border border-hairline-light">
                    + Thêm dòng
                  </Button>
                </div>
                <div className="max-h-[150px] overflow-y-auto flex flex-col gap-2 pr-1">
                  {returnForm.items.map((it, idx) => (
                    <div key={idx} className="flex gap-2 items-center">
                      <select
                        value={it.productId}
                        onChange={e => handleReturnItemChange(idx, 'productId', e.target.value)}
                        className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-1.5 outline-none flex-1"
                        required
                      >
                        <option value="">-- Chọn sản phẩm --</option>
                        {products.map(p => (
                          <option key={p.id} value={p.id}>{p.name}</option>
                        ))}
                      </select>
                      <input
                        type="number"
                        placeholder="SL"
                        value={it.expectedQty}
                        onChange={e => handleReturnItemChange(idx, 'expectedQty', e.target.value)}
                        className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-1.5 outline-none w-20"
                        required
                      />
                      {returnForm.items.length > 1 && (
                        <button type="button" onClick={() => handleRemoveReturnItem(idx)} className="text-red-600 hover:text-red-800">
                          <Trash2 className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold">Ghi chú</label>
                <textarea
                  value={returnForm.notes}
                  onChange={e => setReturnForm(prev => ({ ...prev, notes: e.target.value }))}
                  className="bg-canvas-light text-ink text-xs border border-hairline-light rounded p-2 outline-none"
                  placeholder="Lý do hoàn trả, tình trạng nhận..."
                />
              </div>

              <div className="flex justify-end gap-2 mt-4 pt-2 border-t border-hairline-light">
                <Button type="button" onClick={() => setShowCreateReturn(false)} className="px-4 py-2 border border-hairline-light bg-white text-ink">Hủy</Button>
                <Button type="submit" variant="primary" className="px-4 py-2">Xác nhận tạo</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* --- MODAL: QC count split --- */}
      {showQcModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 max-w-lg w-full flex flex-col gap-4 shadow-level-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-ink pb-2 border-b border-hairline-light">Kiểm định & Phân loại QC hàng hoàn</h3>
            <form onSubmit={handleQcSubmit} className="flex flex-col gap-3">
              <div className="flex flex-col gap-2 max-h-[250px] overflow-y-auto pr-1">
                {qcForm.items.map((it, idx) => (
                  <div key={idx} className="border border-hairline-light rounded p-3 bg-canvas-cream/20 flex flex-col gap-2">
                    <span className="text-xs font-bold text-ink">
                      Sản phẩm: {products.find(p => p.id === it.productId)?.name || `ID #${it.productId}`}
                    </span>
                    <div className="grid grid-cols-3 gap-2">
                      <div className="flex flex-col gap-1">
                        <label className="text-[10px] text-shade-50">Thực tế nhận</label>
                        <input
                          type="number"
                          value={it.actualQty}
                          onChange={e => {
                            const updated = [...qcForm.items];
                            updated[idx].actualQty = Number(e.target.value);
                            setQcForm(prev => ({ ...prev, items: updated }));
                          }}
                          className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-2 py-1"
                          required
                        />
                      </div>
                      <div className="flex flex-col gap-1">
                        <label className="text-[10px] text-shade-50">Số lượng Đạt (QC Pass)</label>
                        <input
                          type="number"
                          value={it.qcPassedQty}
                          onChange={e => {
                            const updated = [...qcForm.items];
                            updated[idx].qcPassedQty = Number(e.target.value);
                            setQcForm(prev => ({ ...prev, items: updated }));
                          }}
                          className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-2 py-1"
                          required
                        />
                      </div>
                      <div className="flex flex-col gap-1">
                        <label className="text-[10px] text-shade-50">Số lượng Lỗi (Quarantine)</label>
                        <input
                          type="number"
                          value={it.qcFailedQty}
                          onChange={e => {
                            const updated = [...qcForm.items];
                            updated[idx].qcFailedQty = Number(e.target.value);
                            setQcForm(prev => ({ ...prev, items: updated }));
                          }}
                          className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-2 py-1"
                          required
                        />
                      </div>
                    </div>
                    {it.qcFailedQty > 0 && (
                      <div className="flex flex-col gap-1">
                        <label className="text-[10px] text-shade-50">Mô tả lỗi hàng hóa</label>
                        <input
                          type="text"
                          value={it.qcFailureReason}
                          onChange={e => {
                            const updated = [...qcForm.items];
                            updated[idx].qcFailureReason = e.target.value;
                            setQcForm(prev => ({ ...prev, items: updated }));
                          }}
                          placeholder="Bể vỡ, rách bao bì, trầy xước..."
                          className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-2 py-1"
                        />
                      </div>
                    )}
                  </div>
                ))}
              </div>

              <div className="flex justify-end gap-2 mt-4 pt-2 border-t border-hairline-light">
                <Button type="button" onClick={() => setShowQcModal(false)} className="px-4 py-2 border border-hairline-light bg-white text-ink">Hủy</Button>
                <Button type="submit" variant="primary" className="px-4 py-2">Lưu kết quả QC</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* --- MODAL: Putaway allocation --- */}
      {showPutawayModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 max-w-lg w-full flex flex-col gap-4 shadow-level-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-ink pb-2 border-b border-hairline-light">Chỉ định vị trí cất hàng hoàn (Putaway)</h3>
            <form onSubmit={handlePutawaySubmit} className="flex flex-col gap-3">
              <div className="flex flex-col gap-2 max-h-[250px] overflow-y-auto pr-1">
                {putawayForm.putawayItems.map((it, idx) => {
                  const receiptItem = activeReturn.items.find(ri => ri.id === it.receiptItemId);
                  return (
                    <div key={idx} className="border border-hairline-light rounded p-3 bg-canvas-cream/20 flex flex-col gap-2">
                      <span className="text-xs font-bold text-ink">
                        Sản phẩm: {products.find(p => p.id === receiptItem?.productId)?.name || `ID #${receiptItem?.productId}`}
                      </span>
                      <div className="text-[10px] text-shade-50">
                        Đạt QC: <strong>{receiptItem?.qcPassedQty || 0} cái</strong> | Lỗi QC: <strong>{receiptItem?.qcFailedQty || 0} cái</strong>
                      </div>
                      <div className="grid grid-cols-2 gap-2 mt-1">
                        <div className="flex flex-col gap-1">
                          <label className="text-[10px] text-shade-50">Bin thường (Hàng Đạt)</label>
                          <select
                            value={it.passedLocationId}
                            onChange={e => {
                              const updated = [...putawayForm.putawayItems];
                              updated[idx].passedLocationId = e.target.value;
                              setPutawayForm(prev => ({ ...prev, putawayItems: updated }));
                            }}
                            className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-2 py-1"
                            disabled={!receiptItem?.qcPassedQty}
                          >
                            <option value="1">Kệ hàng A (WH-HP.A.01)</option>
                            <option value="2">Kệ hàng B (WH-HP.B.02)</option>
                          </select>
                        </div>
                        <div className="flex flex-col gap-1">
                          <label className="text-[10px] text-shade-50">Bin cách ly (Hàng Lỗi)</label>
                          <select
                            value={it.failedLocationId}
                            onChange={e => {
                              const updated = [...putawayForm.putawayItems];
                              updated[idx].failedLocationId = e.target.value;
                              setPutawayForm(prev => ({ ...prev, putawayItems: updated }));
                            }}
                            className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-2 py-1"
                            disabled={!receiptItem?.qcFailedQty}
                          >
                            <option value="99">Quarantine Zone 1 (WH-HP.Q.01)</option>
                          </select>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="flex justify-end gap-2 mt-4 pt-2 border-t border-hairline-light">
                <Button type="button" onClick={() => setShowPutawayModal(false)} className="px-4 py-2 border border-hairline-light bg-white text-ink">Hủy</Button>
                <Button type="submit" variant="primary" className="px-4 py-2">Hoàn tất Putaway</Button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* --- MODAL: Create Scrap Disposal --- */}
      {showDisposalModal && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 max-w-lg w-full flex flex-col gap-4 shadow-level-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-ink pb-2 border-b border-hairline-light flex items-center gap-2">
              <ShieldAlert className="w-5 h-5 text-red-600" />
              Lập biên bản tiêu hủy hàng lỗi
            </h3>
            <form onSubmit={handleCreateDisposalSubmit} className="flex flex-col gap-3">
              <div className="text-xs text-shade-60 bg-red-50 border border-red-100 rounded p-3">
                Đang lập đề xuất tiêu hủy cho sản phẩm <strong>{activeQuarantineItem.productName}</strong>. 
                <br />Số lượng tồn cách ly tối đa: <strong>{activeQuarantineItem.qcFailedQty} cái</strong>.
              </div>

              <div className="flex flex-col gap-1">
                <Input
                  id="quantity"
                  label="Số lượng tiêu hủy (cái)"
                  type="number"
                  value={disposalForm.quantity}
                  onChange={e => setDisposalForm(prev => ({ ...prev, quantity: e.target.value }))}
                  required
                />
              </div>

              <div className="flex flex-col gap-1">
                <Input
                  id="imageUrl"
                  label="URL hình ảnh chứng minh hư hỏng (nếu có)"
                  type="text"
                  value={disposalForm.imageUrl}
                  onChange={e => setDisposalForm(prev => ({ ...prev, imageUrl: e.target.value }))}
                  placeholder="http://..."
                />
              </div>

              <div className="flex flex-col gap-1">
                <label className="text-xs font-semibold">Lý do hư hại (Biên bản chi tiết)</label>
                <textarea
                  value={disposalForm.cause}
                  onChange={e => setDisposalForm(prev => ({ ...prev, cause: e.target.value }))}
                  className="bg-canvas-light text-ink text-xs border border-hairline-light rounded p-2 outline-none min-h-[80px]"
                  placeholder="Mô tả nguyên nhân bể vỡ, móp méo, rách bao bì, quá hạn..."
                  required
                />
              </div>

              <div className="flex justify-end gap-2 mt-4 pt-2 border-t border-hairline-light">
                <Button type="button" onClick={() => setShowDisposalModal(false)} className="px-4 py-2 border border-hairline-light bg-white text-ink">Hủy</Button>
                <Button type="submit" variant="primary" className="px-4 py-2 bg-red-600 text-white hover:bg-red-700">Tạo đề xuất hủy</Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReturnsDisposal;
