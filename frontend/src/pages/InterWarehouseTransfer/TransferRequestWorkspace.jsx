import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { interWarehouseTransferService } from '../../services/inter-warehouse-transfer.service';
import { masterDataService } from '../../services/masterData.service';
import { ROLES } from '../../utils/constants';
import { Loader2, Plus, Send, Check, X, Eye, FileText, RefreshCw, AlertCircle, Inbox, Info } from 'lucide-react';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';

const TransferRequestWorkspace = () => {
  const activeWarehouse = useAuthStore((state) => state.activeWarehouse);
  const { user, hasRole } = useAuthStore();
  const { addToast } = useUiStore();

  const [requests, setRequests] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('ALL'); // ALL, DRAFT, SUBMITTED, APPROVED, REJECTED, CONVERTED

  // Creation State
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [sourceWhId, setSourceWhId] = useState('');
  const [neededByDate, setNeededByDate] = useState('');
  const [businessReason, setBusinessReason] = useState('');
  const [notes, setNotes] = useState('');
  const [items, setItems] = useState([{ productId: '', requestedQty: '' }]);
  const [stockLookupResult, setStockLookupResult] = useState({}); // productId -> [{warehouseName, availableQty}]

  // Detail & Approval State
  const [selectedRequest, setSelectedRequest] = useState(null);
  const [showDetailModal, setShowDetailModal] = useState(false);
  const [rejectionReason, setRejectionReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchData();
  }, [activeWarehouse]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const reqs = await interWarehouseTransferService.getTransferRequests();
      setRequests(reqs);

      const whs = await masterDataService.getWarehouses();
      setWarehouses(whs.filter(w => w.type !== 'IN_TRANSIT' && w.is_active !== false));

      const prods = await masterDataService.getProducts();
      setProducts(prods);
    } catch (e) {
      addToast('Lỗi tải danh sách yêu cầu điều chuyển', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleLookupStock = async (productId, index) => {
    if (!productId) return;
    try {
      const res = await interWarehouseTransferService.stockLookup(productId);
      setStockLookupResult(prev => ({
        ...prev,
        [productId]: res
      }));
    } catch (e) {
      addToast('Lỗi tra cứu tồn kho khả dụng', 'error');
    }
  };

  const handleItemChange = (index, field, value) => {
    const updated = [...items];
    updated[index][field] = value;
    setItems(updated);

    if (field === 'productId') {
      handleLookupStock(value, index);
    }
  };

  const handleAddItem = () => {
    setItems([...items, { productId: '', requestedQty: '' }]);
  };

  const handleRemoveItem = (index) => {
    if (items.length === 1) return;
    const updated = items.filter((_, i) => i !== index);
    setItems(updated);
  };

  const submitCreateRequest = async () => {
    if (!activeWarehouse) return;
    if (!sourceWhId) {
      addToast('Vui lòng chọn kho nguồn điều chuyển', 'warning');
      return;
    }
    if (!businessReason.trim()) {
      addToast('Vui lòng nhập lý do nghiệp vụ cho yêu cầu', 'warning');
      return;
    }
    const filteredItems = items.filter(i => i.productId && Number(i.requestedQty) > 0);
    if (filteredItems.length === 0) {
      addToast('Vui lòng nhập ít nhất một sản phẩm hợp lệ', 'warning');
      return;
    }

    setSubmitting(true);
    try {
      const payload = {
        sourceWarehouseId: Number(sourceWhId),
        destinationWarehouseId: activeWarehouse.id,
        neededByDate: neededByDate || null,
        businessReason: businessReason.trim(),
        notes,
        items: filteredItems.map(i => ({
          productId: Number(i.productId),
          requestedQty: Number(i.requestedQty)
        }))
      };

      await interWarehouseTransferService.createTransferRequest(payload);
      addToast('Đã tạo yêu cầu điều chuyển thô (DRAFT)', 'success');
      setShowCreateModal(false);
      // Reset form
      setSourceWhId('');
      setNeededByDate('');
      setBusinessReason('');
      setNotes('');
      setItems([{ productId: '', requestedQty: '' }]);
      fetchData();
    } catch (e) {
      addToast(`Lỗi tạo yêu cầu: ${e.message || 'Không xác định'}`, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleViewDetails = (req) => {
    setSelectedRequest(req);
    setRejectionReason('');
    setShowDetailModal(true);
  };

  const handleSubmitRequest = async (id) => {
    setLoading(true);
    try {
      await interWarehouseTransferService.submitTransferRequest(id);
      addToast('Đã gửi yêu cầu điều chuyển lên CEO phê duyệt', 'success');
      fetchData();
    } catch (e) {
      addToast('Lỗi gửi yêu cầu duyệt', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveRequest = async (id) => {
    setSubmitting(true);
    try {
      await interWarehouseTransferService.approveTransferRequest(id);
      addToast('CEO đã duyệt yêu cầu điều chuyển', 'success');
      setShowDetailModal(false);
      fetchData();
    } catch (e) {
      addToast('Lỗi phê duyệt yêu cầu', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRejectRequest = async (id) => {
    if (!rejectionReason.trim()) {
      addToast('Vui lòng nhập lý do từ chối yêu cầu', 'warning');
      return;
    }
    setSubmitting(true);
    try {
      await interWarehouseTransferService.rejectTransferRequest(id, rejectionReason);
      addToast('Đã từ chối yêu cầu điều chuyển', 'info');
      setShowDetailModal(false);
      fetchData();
    } catch (e) {
      addToast('Lỗi từ chối yêu cầu', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleConvertRequest = async (id) => {
    setLoading(true);
    try {
      await interWarehouseTransferService.convertTransferRequest(id);
      addToast('Planner đã chuyển đổi yêu cầu thành phiếu điều chuyển TRF thành công', 'success');
      fetchData();
    } catch (e) {
      addToast('Lỗi chuyển đổi yêu cầu', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Filter requests by Tab
  const filteredRequests = requests.filter(req => {
    if (activeTab === 'ALL') return true;
    return req.status === activeTab;
  });

  const getStatusBadge = (status) => {
    const maps = {
      DRAFT: { text: 'Bản thô (DRAFT)', class: 'bg-canvas-cream text-shade-60 border-hairline-light' },
      SUBMITTED: { text: 'Chờ CEO Duyệt', class: 'bg-amber-50 text-amber-700 border-amber-200 animate-pulse' },
      APPROVED: { text: 'Đã Duyệt', class: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
      REJECTED: { text: 'Bị Từ Chối', class: 'bg-red-50 text-red-700 border-red-200' },
      CONVERTED: { text: 'Đã Chuyển TRF', class: 'bg-shade-30 text-ink border-hairline-light' }
    };
    const c = maps[status] || { text: status, class: 'bg-shade-30 text-ink' };
    return <Badge size="sm" colorClassName={c.class}>{c.text}</Badge>;
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Điều phối nội bộ / Spec 005
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Yêu cầu điều chuyển kho
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Tạo đề xuất điều phối hàng hóa từ các kho khác về kho đích hiện tại. Hỗ trợ xem tồn kho khả dụng tức thời, luồng CEO duyệt và Planner lập phiếu TRF tự động.
          </p>
        </div>
        {hasRole(ROLES.WAREHOUSE_MANAGER) && (
          <Button variant="primary" icon={Plus} onClick={() => setShowCreateModal(true)}>
            Tạo yêu cầu
          </Button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-hairline-light overflow-x-auto whitespace-nowrap scrollbar-none mb-2">
        {['ALL', 'DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CONVERTED'].map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2.5 font-semibold text-xs transition-colors border-b-2 uppercase tracking-wide ${
              activeTab === tab
                ? 'border-ink text-ink'
                : 'border-transparent text-shade-50 hover:text-ink'
            }`}
          >
            {tab === 'ALL' ? 'Tất cả' : tab}
          </button>
        ))}
      </div>

      {/* Requests Grid */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : filteredRequests.length === 0 ? (
        <div className="bg-canvas-light rounded-lg border border-hairline-light p-16 text-center shadow-level-3">
          <Inbox className="w-12 h-12 text-shade-50 mx-auto mb-4" />
          <h3 className="text-sm font-semibold text-ink mb-1">Không tìm thấy yêu cầu nào</h3>
          <p className="text-xs text-shade-50 font-light">Không có yêu cầu điều chuyển nào ở trạng thái này.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {filteredRequests.map((req) => (
            <div
              key={req.id}
              className="bg-canvas-light border border-hairline-light rounded-lg p-5 shadow-level-3 hover:shadow-md transition-shadow flex flex-col justify-between"
            >
              <div>
                <div className="flex justify-between items-start gap-4 mb-3 pb-3 border-b border-hairline-light">
                  <div>
                    <span className="text-[10px] font-bold text-shade-50 uppercase font-mono block">{req.requestNumber}</span>
                    <h4 className="font-bold text-xs text-ink mt-0.5">
                      Từ: <span className="text-blue-700 font-bold">{req.sourceWarehouseName}</span> → Đến: <span className="text-ink font-bold">{req.destinationWarehouseName}</span>
                    </h4>
                  </div>
                  {getStatusBadge(req.status)}
                </div>

                <div className="flex flex-col gap-1.5 text-xs text-shade-60 mb-4">
                  <div>
                    <span className="font-semibold text-shade-50">Sản phẩm yêu cầu:</span> {req.items?.length || 0} SKU
                  </div>
                  {req.neededByDate && (
                    <div>
                      <span className="font-semibold text-shade-50">Cần trước ngày:</span> {req.neededByDate}
                    </div>
                  )}
                  {req.businessReason && (
                    <div>
                      <span className="font-semibold text-shade-50">Lý do nghiệp vụ:</span> {req.businessReason}
                    </div>
                  )}
                  {req.convertedTransferNumber && (
                    <div className="bg-emerald-50 text-emerald-700 p-2 rounded border border-emerald-100 font-semibold">
                      Phiếu TRF đã tạo: {req.convertedTransferNumber}
                    </div>
                  )}
                  {req.notes && (
                    <div className="italic">
                      <span className="font-semibold text-shade-50 not-italic">Ghi chú:</span> "{req.notes}"
                    </div>
                  )}
                  {req.rejectionReason && (
                    <div className="text-red-600 font-semibold italic bg-red-50 p-2 rounded border border-red-100">
                      Lý do từ chối: "{req.rejectionReason}"
                    </div>
                  )}
                </div>
              </div>

              <div className="flex gap-2 border-t border-hairline-light pt-3 justify-end items-center">
                <Button variant="outline-light" icon={Eye} onClick={() => handleViewDetails(req)}>
                  Chi tiết
                </Button>

                {req.status === 'DRAFT' && hasRole(ROLES.WAREHOUSE_MANAGER) && (
                  <Button
                    variant="primary"
                    onClick={() => handleSubmitRequest(req.id)}
                    icon={Send}
                  >
                    Gửi CEO duyệt
                  </Button>
                )}

                {req.status === 'APPROVED' && (hasRole(ROLES.PLANNER) || hasRole(ROLES.ADMIN)) && (
                  <Button variant="primary" icon={RefreshCw} onClick={() => handleConvertRequest(req.id)}>
                    Tạo phiếu TRF
                  </Button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Creation Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-canvas-night/40 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="bg-canvas-light border border-hairline-light rounded-lg max-w-2xl w-full shadow-2xl overflow-hidden">
            <div className="p-5 border-b border-hairline-light bg-canvas-cream flex justify-between items-center">
              <h3 className="font-bold text-base flex items-center gap-2">
                <FileText className="w-5 h-5 text-blue-700" />
                Tạo yêu cầu điều chuyển mới về kho {activeWarehouse?.name}
              </h3>
              <button onClick={() => setShowCreateModal(false)} className="p-1 hover:bg-canvas-cream rounded-full">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 text-xs flex flex-col gap-4 max-h-[70vh] overflow-y-auto scrollbar-thin">
              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Kho nguồn (Nơi xuất hàng đi)"
                  type="select"
                  value={sourceWhId}
                  onChange={(e) => setSourceWhId(e.target.value)}
                  options={[
                    { value: '', label: '-- Chọn kho nguồn --' },
                    ...warehouses.filter(w => w.id !== activeWarehouse?.id).map(w => ({ value: w.id, label: `${w.name} (${w.code})` })),
                  ]}
                />
                <Input
                  label="Kho đích (Nhận hàng về)"
                  value={activeWarehouse?.name}
                  disabled
                  className="bg-canvas-cream opacity-75"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Ngày cần hàng"
                  type="date"
                  value={neededByDate}
                  onChange={(e) => setNeededByDate(e.target.value)}
                />
                <Input
                  label="Lý do nghiệp vụ"
                  value={businessReason}
                  onChange={(e) => setBusinessReason(e.target.value)}
                  placeholder="VD: Bù thiếu tồn bán, gom hàng cho đơn lớn..."
                />
              </div>

              {/* Items List */}
              <div className="flex flex-col gap-3">
                <div className="flex justify-between items-center">
                  <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">Danh sách sản phẩm yêu cầu</label>
                  <Button variant="outline-light" icon={Plus} onClick={handleAddItem}>
                    Thêm dòng
                  </Button>
                </div>

                <div className="flex flex-col gap-3">
                  {items.map((item, idx) => (
                    <div key={idx} className="bg-canvas-light border border-hairline-light p-3.5 rounded flex flex-col gap-3">
                      <div className="flex items-center gap-3">
                        <div className="flex-1">
                          <Input
                            type="select"
                            value={item.productId}
                            onChange={(e) => handleItemChange(idx, 'productId', e.target.value)}
                            options={[
                              { value: '', label: '-- Chọn sản phẩm --' },
                              ...products.map(p => ({ value: p.id, label: `${p.sku} - ${p.name}` })),
                            ]}
                          />
                        </div>
                        <div className="w-28">
                          <Input
                            type="number"
                            placeholder="Số lượng"
                            value={item.requestedQty}
                            onChange={(e) => handleItemChange(idx, 'requestedQty', e.target.value)}
                          />
                        </div>
                        {items.length > 1 && (
                          <button
                            onClick={() => handleRemoveItem(idx)}
                            className="p-1 hover:bg-red-50 text-red-500 rounded"
                          >
                            <X className="w-4 h-4" />
                          </button>
                        )}
                      </div>

                      {/* Stock Lookup display */}
                      {item.productId && stockLookupResult[item.productId] && (
                        <div className="bg-canvas-cream p-2.5 rounded border border-hairline-light flex flex-col gap-1">
                          <div className="text-[10px] font-bold text-shade-50 uppercase flex items-center gap-1">
                            <Info className="w-3 h-3 text-blue-700" />
                            Tồn khả dụng tại các kho khác (không tính hàng cách ly):
                          </div>
                          <div className="grid grid-cols-3 gap-2 mt-1">
                            {stockLookupResult[item.productId].map(stock => (
                              <div key={stock.warehouseId} className="bg-canvas-light px-2 py-1 rounded border border-hairline-light">
                                <span className="font-semibold block text-[10px] text-shade-50 truncate">{stock.warehouseName}</span>
                                <span className={`font-bold text-xs ${stock.availableQty > 0 ? 'text-emerald-700' : 'text-shade-50'}`}>
                                  {stock.availableQty} cái
                                </span>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>

              <div className="flex flex-col gap-1.5 mt-2">
                <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">Ghi chú bổ sung</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Nhập lý do điều phối hàng..."
                  className="text-input h-16 resize-none"
                />
              </div>
            </div>

            <div className="p-4 border-t border-hairline-light bg-canvas-cream flex justify-end gap-2">
              <Button variant="outline-light" onClick={() => setShowCreateModal(false)}>Hủy</Button>
              <Button variant="primary" onClick={submitCreateRequest} disabled={submitting} loading={submitting}>
                Tạo DRAFT
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Details & Approval Modal */}
      {showDetailModal && selectedRequest && (
        <div className="fixed inset-0 bg-canvas-night/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-light border border-hairline-light rounded-lg max-w-xl w-full shadow-2xl overflow-hidden">
            <div className="p-5 border-b border-hairline-light bg-canvas-cream flex justify-between items-center">
              <h3 className="font-bold text-base flex flex-col">
                <span className="text-[10px] font-bold text-shade-50 uppercase font-mono">{selectedRequest.requestNumber}</span>
                Chi tiết yêu cầu điều phối hàng
              </h3>
              <button onClick={() => setShowDetailModal(false)} className="p-1 hover:bg-canvas-cream rounded-full">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 text-xs flex flex-col gap-4 max-h-[60vh] overflow-y-auto">
              <div className="bg-canvas-light p-3.5 rounded border border-hairline-light shadow-level-3 flex flex-col gap-2.5">
                <div className="grid grid-cols-2 gap-2">
                  <div><span className="text-shade-50">Kho nguồn xuất:</span> <strong className="text-ink text-xs">{selectedRequest.sourceWarehouseName}</strong></div>
                  <div><span className="text-shade-50">Kho đích nhận:</span> <strong className="text-ink text-xs">{selectedRequest.destinationWarehouseName}</strong></div>
                </div>
                <div className="grid grid-cols-2 gap-2 border-t border-hairline-light pt-2 text-[11px]">
                  <div><span className="text-shade-50">Người đề xuất:</span> <span className="font-semibold">{selectedRequest.createdByName}</span></div>
                  <div><span className="text-shade-50">Trạng thái:</span> {getStatusBadge(selectedRequest.status)}</div>
                </div>
                <div className="grid grid-cols-2 gap-2 border-t border-hairline-light pt-2 text-[11px]">
                  <div><span className="text-shade-50">Ngày cần hàng:</span> <span className="font-semibold">{selectedRequest.neededByDate || 'Chưa đặt'}</span></div>
                  <div><span className="text-shade-50">TRF đã tạo:</span> <span className="font-semibold">{selectedRequest.convertedTransferNumber || 'Chưa có'}</span></div>
                </div>
                {selectedRequest.businessReason && (
                  <div className="border-t border-hairline-light pt-2">
                    <span className="text-shade-50">Lý do nghiệp vụ:</span> <span className="font-semibold">{selectedRequest.businessReason}</span>
                  </div>
                )}
                {selectedRequest.notes && (
                  <div className="border-t border-hairline-light pt-2">
                    <span className="text-shade-50">Ghi chú:</span> <span className="italic">"{selectedRequest.notes}"</span>
                  </div>
                )}
              </div>

              {/* Items list */}
              <div>
                <label className="text-xs font-semibold uppercase tracking-wider text-shade-60 block mb-2">Danh sách sản phẩm ({selectedRequest.items?.length || 0})</label>
                <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
                  {/* Desktop/tablet: table view */}
                  <div className="hidden md:block overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                      <thead>
                        <tr className="bg-canvas-cream border-b border-hairline-light">
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã SKU</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Tên sản phẩm</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Số lượng yêu cầu</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-hairline-light">
                        {selectedRequest.items?.map(item => (
                          <tr key={item.id} className="hover:bg-canvas-cream/50 transition-colors">
                            <td className="px-6 py-4 text-xs font-mono font-semibold">{item.productSku}</td>
                            <td className="px-6 py-4 text-xs text-shade-60">{item.productName}</td>
                            <td className="px-6 py-4 text-xs text-right font-semibold">{item.requestedQty} {item.productUnit}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  {/* Mobile: stacked card view */}
                  <div className="flex flex-col divide-y divide-hairline-light md:hidden">
                    {selectedRequest.items?.map(item => (
                      <div key={item.id} className="p-4 flex flex-col gap-1.5 text-xs">
                        <span className="font-mono font-semibold">{item.productSku}</span>
                        <span className="text-shade-60">{item.productName}</span>
                        <span className="font-semibold">SL yêu cầu: {item.requestedQty} {item.productUnit}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>

              {/* CEO Reject Input panel */}
              {selectedRequest.status === 'SUBMITTED' && (hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN)) && (
                <div className="bg-red-50/50 p-4 border border-red-200 rounded flex flex-col gap-2">
                  <label className="font-bold text-red-800 flex items-center gap-1">
                    <AlertCircle className="w-4 h-4" />
                    Phản hồi lý do từ chối (Bắt buộc nếu từ chối)
                  </label>
                  <textarea
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    placeholder="Nhập nguyên nhân không duyệt yêu cầu..."
                    className="text-input h-16 resize-none bg-canvas-light border-red-300"
                  />
                </div>
              )}
            </div>

            <div className="p-4 border-t border-hairline-light bg-canvas-cream flex justify-end gap-2">
              <Button variant="outline-light" onClick={() => setShowDetailModal(false)}>Đóng</Button>

              {/* CEO Actions */}
              {selectedRequest.status === 'SUBMITTED' && (hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN)) && (
                <>
                  <Button
                    variant="outline-light"
                    onClick={() => handleRejectRequest(selectedRequest.id)}
                    disabled={submitting || !rejectionReason.trim()}
                    loading={submitting}
                  >
                    Từ chối
                  </Button>
                  <Button
                    variant="primary"
                    onClick={() => handleApproveRequest(selectedRequest.id)}
                    disabled={submitting}
                    loading={submitting}
                    icon={Check}
                  >
                    Phê duyệt
                  </Button>
                </>
              )}

              {/* Planner Actions */}
              {selectedRequest.status === 'APPROVED' && (hasRole(ROLES.PLANNER) || hasRole(ROLES.ADMIN)) && (
                <Button variant="primary" icon={RefreshCw} onClick={() => handleConvertRequest(selectedRequest.id)}>
                  Chuyển đổi thành phiếu TRF
                </Button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TransferRequestWorkspace;
