import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { interWarehouseTransferService } from '../../services/inter-warehouse-transfer.service';
import { masterDataService } from '../../services/masterData.service';
import { ROLES } from '../../utils/constants';
import { Loader2, Plus, Send, Check, X, Eye, FileText, RefreshCw, AlertCircle, Inbox, Info } from 'lucide-react';

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
      DRAFT: { text: 'Bản thô (DRAFT)', class: 'bg-zinc-100 text-zinc-700 border-zinc-200' },
      SUBMITTED: { text: 'Chờ CEO Duyệt', class: 'bg-amber-50 text-amber-700 border-amber-200 animate-pulse' },
      APPROVED: { text: 'Đã Duyệt', class: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
      REJECTED: { text: 'Bị Từ Chối', class: 'bg-red-50 text-red-700 border-red-200' },
      CONVERTED: { text: 'Đã Chuyển TRF', class: 'bg-indigo-50 text-indigo-700 border-indigo-200' }
    };
    const c = maps[status] || { text: status, class: 'bg-gray-100 text-gray-700' };
    return <span className={`text-[10px] font-bold px-2 py-0.5 border rounded-pill whitespace-nowrap ${c.class}`}>{c.text}</span>;
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Upper Panel */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-gradient-to-r from-zinc-900 via-zinc-800 to-black text-white p-6 rounded-lg shadow-xl relative overflow-hidden">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_bottom_left,rgba(59,130,246,0.1),transparent_40%)]" />
        <div className="relative z-10">
          <span className="text-[10px] font-bold text-blue-400 uppercase tracking-widest block mb-1">
            Điều phối nội bộ / Spec 005
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight text-white">
            Yêu cầu điều chuyển kho
          </h1>
          <p className="text-xs text-zinc-300 font-light mt-1 max-w-xl">
            Tạo đề xuất điều phối hàng hóa từ các kho khác về kho đích hiện tại. Hỗ trợ xem tồn kho khả dụng tức thời, luồng CEO duyệt và Planner lập phiếu TRF tự động.
          </p>
        </div>

        {hasRole(ROLES.WAREHOUSE_MANAGER) && (
          <button
            onClick={() => setShowCreateModal(true)}
            className="btn bg-white hover:bg-zinc-100 text-zinc-950 font-bold text-xs flex items-center gap-1.5 px-4 py-2.5 rounded-lg shadow-lg relative z-10 transition-transform hover:-translate-y-0.5 active:translate-y-0 duration-150"
          >
            <Plus className="w-4 h-4" />
            <span>Tạo yêu cầu</span>
          </button>
        )}
      </div>

      {/* Tabs */}
      <div className="flex border-b border-hairline-light overflow-x-auto whitespace-nowrap scrollbar-none mb-2">
        {['ALL', 'DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'CONVERTED'].map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2.5 font-bold text-xs transition-colors border-b-2 uppercase tracking-wide ${
              activeTab === tab
                ? 'border-zinc-950 text-zinc-950'
                : 'border-transparent text-shade-50 hover:text-zinc-950'
            }`}
          >
            {tab === 'ALL' ? 'Tất cả' : tab}
          </button>
        ))}
      </div>

      {/* Requests Grid */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-40" />
        </div>
      ) : filteredRequests.length === 0 ? (
        <div className="bg-white rounded-lg border border-hairline-light p-16 text-center shadow-sm">
          <Inbox className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy yêu cầu nào</h3>
          <p className="text-sm text-shade-50 font-light">Không có yêu cầu điều chuyển nào ở trạng thái này.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {filteredRequests.map((req) => (
            <div 
              key={req.id} 
              className="bg-white border border-hairline-light rounded-lg p-5 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between"
            >
              <div>
                <div className="flex justify-between items-start gap-4 mb-3 pb-3 border-b border-hairline-light">
                  <div>
                    <span className="text-[10px] font-bold text-shade-40 uppercase font-mono block">{req.requestNumber}</span>
                    <h4 className="font-bold text-xs text-ink mt-0.5">
                      Từ: <span className="text-blue-600 font-bold">{req.sourceWarehouseName}</span> → Đến: <span className="text-zinc-950 font-bold">{req.destinationWarehouseName}</span>
                    </h4>
                  </div>
                  {getStatusBadge(req.status)}
                </div>

                <div className="flex flex-col gap-1.5 text-xs text-shade-60 mb-4">
                  <div>
                    <span className="font-semibold text-shade-50">Sản phẩm yêu cầu:</span> {req.items?.length || 0} SKU
                  </div>
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

              <div className="flex gap-2 border-t border-zinc-100 pt-3 justify-end items-center">
                <button
                  onClick={() => handleViewDetails(req)}
                  className="btn-pill btn-pill-outline-light text-[11px] flex items-center gap-1 py-1 px-3"
                >
                  <Eye className="w-3.5 h-3.5" />
                  <span>Chi tiết</span>
                </button>

                {req.status === 'DRAFT' && hasRole(ROLES.WAREHOUSE_MANAGER) && (
                  <button
                    onClick={() => handleSubmitRequest(req.id)}
                    className="btn-pill bg-zinc-900 hover:bg-zinc-800 text-white text-[11px] flex items-center gap-1 py-1 px-3"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>Gửi CEO duyệt</span>
                  </button>
                )}

                {req.status === 'APPROVED' && (hasRole(ROLES.PLANNER) || hasRole(ROLES.ADMIN)) && (
                  <button
                    onClick={() => handleConvertRequest(req.id)}
                    className="btn-pill bg-indigo-650 hover:bg-indigo-750 text-white text-[11px] flex items-center gap-1 py-1 px-3 font-semibold"
                  >
                    <RefreshCw className="w-3.5 h-3.5" />
                    <span>Tạo phiếu TRF</span>
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Creation Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="bg-canvas-cream border border-hairline-light rounded-lg max-w-2xl w-full shadow-2xl overflow-hidden">
            <div className="p-5 border-b border-hairline-light bg-white flex justify-between items-center">
              <h3 className="font-bold text-base flex items-center gap-2">
                <FileText className="w-5 h-5 text-blue-600" />
                Tạo yêu cầu điều chuyển mới về kho {activeWarehouse?.name}
              </h3>
              <button onClick={() => setShowCreateModal(false)} className="p-1 hover:bg-zinc-100 rounded-full">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 text-xs flex flex-col gap-4 max-h-[70vh] overflow-y-auto scrollbar-thin">
              <div className="grid grid-cols-2 gap-4">
                <div className="flex flex-col gap-1.5">
                  <label className="font-bold text-shade-60">Kho nguồn (Nơi xuất hàng đi)</label>
                  <select
                    value={sourceWhId}
                    onChange={(e) => setSourceWhId(e.target.value)}
                    className="text-input"
                  >
                    <option value="">-- Chọn kho nguồn --</option>
                    {warehouses.filter(w => w.id !== activeWarehouse?.id).map(w => (
                      <option key={w.id} value={w.id}>{w.name} ({w.code})</option>
                    ))}
                  </select>
                </div>
                <div className="flex flex-col gap-1.5">
                  <label className="font-bold text-shade-60">Kho đích (Nhận hàng về)</label>
                  <input type="text" value={activeWarehouse?.name} disabled className="text-input bg-zinc-50 opacity-75" />
                </div>
              </div>

              {/* Items List */}
              <div className="flex flex-col gap-3">
                <div className="flex justify-between items-center">
                  <label className="font-bold text-zinc-800 text-sm">Danh sách sản phẩm yêu cầu</label>
                  <button
                    onClick={handleAddItem}
                    className="text-blue-600 hover:text-blue-800 font-semibold flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Thêm dòng
                  </button>
                </div>

                <div className="flex flex-col gap-3">
                  {items.map((item, idx) => (
                    <div key={idx} className="bg-white border border-hairline-light p-3.5 rounded flex flex-col gap-3">
                      <div className="flex items-center gap-3">
                        <div className="flex-1">
                          <select
                            value={item.productId}
                            onChange={(e) => handleItemChange(idx, 'productId', e.target.value)}
                            className="text-input"
                          >
                            <option value="">-- Chọn sản phẩm --</option>
                            {products.map(p => (
                              <option key={p.id} value={p.id}>{p.sku} - {p.name}</option>
                            ))}
                          </select>
                        </div>
                        <div className="w-28">
                          <input
                            type="number"
                            placeholder="Số lượng"
                            value={item.requestedQty}
                            onChange={(e) => handleItemChange(idx, 'requestedQty', e.target.value)}
                            className="text-input"
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
                        <div className="bg-zinc-50 p-2.5 rounded border border-hairline-light flex flex-col gap-1">
                          <div className="text-[10px] font-bold text-zinc-500 uppercase flex items-center gap-1">
                            <Info className="w-3 h-3 text-blue-500" />
                            Tồn khả dụng tại các kho khác (không tính hàng cách ly):
                          </div>
                          <div className="grid grid-cols-3 gap-2 mt-1">
                            {stockLookupResult[item.productId].map(stock => (
                              <div key={stock.warehouseId} className="bg-white px-2 py-1 rounded border border-zinc-200">
                                <span className="font-semibold block text-[10px] text-shade-50 truncate">{stock.warehouseName}</span>
                                <span className={`font-bold text-xs ${stock.availableQty > 0 ? 'text-emerald-700' : 'text-shade-40'}`}>
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
                <label className="font-bold text-shade-60">Ghi chú lý do điều chuyển</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Nhập lý do điều phối hàng..."
                  className="text-input h-16 resize-none"
                />
              </div>
            </div>

            <div className="p-4 border-t border-hairline-light bg-zinc-50 flex justify-end gap-2">
              <button onClick={() => setShowCreateModal(false)} className="btn-pill btn-pill-outline-light text-xs py-1.5 px-4">
                Hủy
              </button>
              <button
                onClick={submitCreateRequest}
                disabled={submitting}
                className="btn-pill bg-zinc-950 hover:bg-zinc-800 text-white text-xs py-1.5 px-4 font-bold disabled:opacity-50"
              >
                {submitting ? 'Đang tạo...' : 'Tạo DRAFT'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Details & Approval Modal */}
      {showDetailModal && selectedRequest && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-cream border border-hairline-light rounded-lg max-w-xl w-full shadow-2xl overflow-hidden">
            <div className="p-5 border-b border-hairline-light bg-white flex justify-between items-center">
              <h3 className="font-bold text-base flex flex-col">
                <span className="text-[10px] font-bold text-shade-40 uppercase font-mono">{selectedRequest.requestNumber}</span>
                Chi tiết yêu cầu điều phối hàng
              </h3>
              <button onClick={() => setShowDetailModal(false)} className="p-1 hover:bg-zinc-100 rounded-full">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 text-xs flex flex-col gap-4 max-h-[60vh] overflow-y-auto">
              <div className="bg-white p-3.5 rounded border border-hairline-light shadow-sm flex flex-col gap-2.5">
                <div className="grid grid-cols-2 gap-2">
                  <div><span className="text-shade-50">Kho nguồn xuất:</span> <strong className="text-ink text-xs">{selectedRequest.sourceWarehouseName}</strong></div>
                  <div><span className="text-shade-50">Kho đích nhận:</span> <strong className="text-ink text-xs">{selectedRequest.destinationWarehouseName}</strong></div>
                </div>
                <div className="grid grid-cols-2 gap-2 border-t border-zinc-100 pt-2 text-[11px]">
                  <div><span className="text-shade-50">Người đề xuất:</span> <span className="font-semibold">{selectedRequest.createdByName}</span></div>
                  <div><span className="text-shade-50">Trạng thái:</span> {getStatusBadge(selectedRequest.status)}</div>
                </div>
                {selectedRequest.notes && (
                  <div className="border-t border-zinc-100 pt-2">
                    <span className="text-shade-50">Ghi chú:</span> <span className="italic">"{selectedRequest.notes}"</span>
                  </div>
                )}
              </div>

              {/* Items list */}
              <div>
                <label className="font-bold text-zinc-800 text-xs block mb-2">Danh sách sản phẩm ({selectedRequest.items?.length || 0})</label>
                <div className="border border-hairline-light rounded overflow-hidden">
                  <table className="w-full text-left text-xs">
                    <thead>
                      <tr className="bg-zinc-50 border-b border-hairline-light font-bold text-shade-60">
                        <th className="px-4 py-2">Mã SKU</th>
                        <th className="px-4 py-2">Tên sản phẩm</th>
                        <th className="px-4 py-2 text-right">Số lượng yêu cầu</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light bg-white">
                      {selectedRequest.items?.map(item => (
                        <tr key={item.id} className="hover:bg-zinc-50/50">
                          <td className="px-4 py-2 font-mono font-bold text-[11px]">{item.productSku}</td>
                          <td className="px-4 py-2 text-shade-60">{item.productName}</td>
                          <td className="px-4 py-2 text-right font-bold">{item.requestedQty} {item.productUnit}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
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
                    className="text-input h-16 resize-none bg-white border-red-300"
                  />
                </div>
              )}
            </div>

            <div className="p-4 border-t border-hairline-light bg-zinc-50 flex justify-end gap-2">
              <button onClick={() => setShowDetailModal(false)} className="btn-pill btn-pill-outline-light text-xs py-1.5 px-4">
                Đóng
              </button>

              {/* CEO Actions */}
              {selectedRequest.status === 'SUBMITTED' && (hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN)) && (
                <>
                  <button
                    onClick={() => handleRejectRequest(selectedRequest.id)}
                    disabled={submitting || !rejectionReason.trim()}
                    className="btn-pill bg-red-600 hover:bg-red-700 text-white text-xs py-1.5 px-4 font-bold disabled:opacity-50"
                  >
                    Từ chối
                  </button>
                  <button
                    onClick={() => handleApproveRequest(selectedRequest.id)}
                    disabled={submitting}
                    className="btn-pill bg-emerald-650 hover:bg-emerald-700 text-white text-xs py-1.5 px-4 font-bold"
                  >
                    Phê duyệt
                  </button>
                </>
              )}

              {/* Planner Actions */}
              {selectedRequest.status === 'APPROVED' && (hasRole(ROLES.PLANNER) || hasRole(ROLES.ADMIN)) && (
                <button
                  onClick={() => handleConvertRequest(selectedRequest.id)}
                  className="btn-pill bg-indigo-650 hover:bg-indigo-750 text-white text-xs py-1.5 px-4 font-bold flex items-center gap-1"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Chuyển đổi thành phiếu TRF</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TransferRequestWorkspace;
