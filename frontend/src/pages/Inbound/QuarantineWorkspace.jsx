import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';
import { ROLES } from '../../utils/constants';
import { Loader2, ArrowRightLeft, Trash2, ShieldAlert, Check, X } from 'lucide-react';

const QuarantineWorkspace = () => {
  const activeWarehouse = useAuthStore((state) => state.activeWarehouse);
  const { user, hasRole } = useAuthStore();
  const { addToast } = useUiStore();

  const [quarantineItems, setQuarantineItems] = useState([]);
  const [pendingDisposals, setPendingDisposals] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('WORKSPACE'); // WORKSPACE or APPROVALS

  // Modal State
  const [showRtvModal, setShowRtvModal] = useState(false);
  const [showDisposalModal, setShowDisposalModal] = useState(false);
  const [selectedItem, setSelectedItem] = useState(null);
  const [actionNotes, setActionNotes] = useState('');
  const [disposalImageUrl, setDisposalImageUrl] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchData();
  }, [activeWarehouse, activeTab]);

  const fetchData = async () => {
    if (!activeWarehouse) return;
    setLoading(true);
    try {
      const suppliersData = await masterDataService.getSuppliers();
      setSuppliers(suppliersData);

      if (activeTab === 'WORKSPACE') {
        const data = await inboundService.getQuarantineItems(activeWarehouse.id);
        setQuarantineItems(data);
      } else {
        const data = await inboundService.getPendingDisposals();
        // Filter by warehouse
        setPendingDisposals(data.filter(adj => adj.warehouse_id === activeWarehouse.id));
      }
    } catch (e) {
      addToast('Lỗi tải dữ liệu khu cách ly', 'error');
    } finally {
      setLoading(false);
    }
  };

  const getSupplierName = (supplierId) => {
    const s = suppliers.find(sup => sup.id === supplierId);
    return s ? s.company_name : `NCC ID: ${supplierId}`;
  };

  const handleRtvClick = (item) => {
    setSelectedItem(item);
    setActionNotes('');
    setShowRtvModal(true);
  };

  const handleDisposalClick = (item) => {
    setSelectedItem(item);
    setActionNotes('');
    setDisposalImageUrl('');
    setShowDisposalModal(true);
  };

  const submitRtv = async () => {
    setSubmitting(true);
    try {
      await inboundService.handleRtv(selectedItem.id, actionNotes);
      addToast('Đã lập phiếu xuất trả hàng NCC thành công. Debit Note đã được khởi tạo.', 'success');
      setShowRtvModal(false);
      fetchData();
    } catch (e) {
      addToast('Lỗi xử lý xuất trả hàng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const submitDisposal = async () => {
    if (!actionNotes.trim()) {
      addToast('Vui lòng nhập lý do tiêu hủy hàng lỗi', 'warning');
      return;
    }
    setSubmitting(true);
    try {
      const res = await inboundService.handleDisposal(selectedItem.id, actionNotes, disposalImageUrl);
      if (res.autoApproved) {
        addToast('Đã tiêu hủy sản phẩm thành công (Tự động duyệt do giá trị thấp < 5M)', 'success');
      } else {
        addToast('Đã gửi yêu cầu tiêu hủy hàng hỏng lên Trưởng kho/CEO chờ phê duyệt', 'info');
      }
      setShowDisposalModal(false);
      fetchData();
    } catch (e) {
      addToast('Lỗi xử lý yêu cầu tiêu hủy', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleApproveDisposal = async (adjId, value) => {
    // Check authority: Manager <= 100M, CEO any
    if (value > 100000000 && !hasRole(ROLES.CEO) && !hasRole(ROLES.ADMIN)) {
      addToast('Yêu cầu tiêu hủy này vượt quá hạn mức phê duyệt của bạn (>100M VND), phải do CEO duyệt', 'warning');
      return;
    }

    setLoading(true);
    try {
      await inboundService.approveDisposal(adjId);
      addToast('Đã phê duyệt tiêu hủy hàng hỏng thành công. Tồn kho cách ly đã được khấu trừ.', 'success');
      fetchData();
    } catch (e) {
      addToast('Lỗi phê duyệt yêu cầu', 'error');
    } finally {
      setLoading(false);
    }
  };

  // Finance Threshold Badge Renderer
  const getDisposalThresholdBadge = (value) => {
    if (value < 5000000) {
      return <span className="text-[10px] font-bold bg-emerald-50 text-emerald-700 border border-emerald-200 px-2 py-0.5 rounded-pill">Tự động duyệt (&lt; 5M)</span>;
    }
    if (value <= 100000000) {
      return <span className="text-[10px] font-bold bg-amber-50 text-amber-700 border border-amber-200 px-2 py-0.5 rounded-pill">Trưởng kho duyệt (5M - 100M)</span>;
    }
    return <span className="text-[10px] font-bold bg-red-50 text-red-700 border border-red-200 px-2 py-0.5 rounded-pill font-mono">Bắt buộc CEO duyệt (&gt; 100M)</span>;
  };

  const getDisposalApprovalAuthority = (value) => {
    if (value > 100000000) {
      return hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN);
    }
    return hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN);
  };

  return (
    <div className="p-6 bg-canvas-cream min-h-screen text-ink font-sans">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-4xl font-display font-light leading-tight tracking-tight mb-2">
          Quản Lý Hàng Lỗi Cách Ly
        </h1>
        <p className="text-sm text-shade-50">
          Xem xét, kiểm soát và ra quyết định xử lý hàng hóa lỗi QC trong Quarantine Zone tại kho <span className="font-semibold text-ink">{activeWarehouse?.name}</span>.
        </p>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-hairline-light mb-6">
        <button
          onClick={() => setActiveTab('WORKSPACE')}
          className={`px-4 py-2.5 font-semibold text-sm transition-colors border-b-2 ${
            activeTab === 'WORKSPACE'
              ? 'border-ink text-ink'
              : 'border-transparent text-shade-50 hover:text-ink'
          }`}
        >
          Khu vực xử lý (Quarantine Zone)
        </button>
        {(hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.CEO) || hasRole(ROLES.ADMIN)) && (
          <button
            onClick={() => setActiveTab('APPROVALS')}
            className={`px-4 py-2.5 font-semibold text-sm transition-colors border-b-2 ${
              activeTab === 'APPROVALS'
                ? 'border-ink text-ink'
                : 'border-transparent text-shade-50 hover:text-ink'
            }`}
          >
            Duyệt yêu cầu tiêu hủy chờ xử lý
          </button>
        )}
      </div>

      {/* Main Content Area */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : activeTab === 'WORKSPACE' ? (
        // Workspace List
        quarantineItems.length === 0 ? (
          <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
            <ShieldAlert className="w-12 h-12 text-shade-30 mx-auto mb-4" />
            <h3 className="text-lg font-bold mb-1">Khu vực cách ly hiện đang trống</h3>
            <p className="text-sm text-shade-50">Không có hàng hóa QC hỏng cần xử lý tại kho này.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {quarantineItems.map((item) => (
              <div 
                key={item.id} 
                className="bg-white border border-hairline-light rounded-lg p-6 shadow-sm card-premium flex flex-col justify-between"
              >
                <div>
                  <div className="flex justify-between items-start gap-4 mb-3 border-b border-hairline-light pb-2">
                    <div>
                      <span className="text-[10px] font-bold text-shade-40 uppercase font-mono block">{item.product_sku}</span>
                      <h4 className="font-bold text-sm text-ink">{item.product_name}</h4>
                    </div>
                    <span className="text-red-700 bg-red-50 border border-red-200 px-2 py-0.5 rounded-pill font-bold text-xs">
                      Lỗi: {item.qc_failed_qty} {item.unit}
                    </span>
                  </div>

                  <div className="flex flex-col gap-1.5 text-xs text-shade-60 mb-5">
                    <div>
                      <span className="font-semibold text-shade-50">Phiếu nhập gốc:</span> {item.receipt_number}
                    </div>
                    <div>
                      <span className="font-semibold text-shade-50">Nhà cung cấp:</span> {getSupplierName(item.supplier_id)}
                    </div>
                    <div>
                      <span className="font-semibold text-shade-50">Lý do lỗi QC:</span> <span className="text-red-600 font-semibold italic">{item.qc_failure_reason || 'Không rõ'}</span>
                    </div>
                    <div>
                      <span className="font-semibold text-shade-50">Trị giá hàng lỗi:</span> <span className="font-bold text-ink">{item.total_value.toLocaleString('vi-VN')} VND</span>
                    </div>
                  </div>
                </div>

                <div className="flex gap-2 border-t border-zinc-100 pt-4 justify-end">
                  <button
                    onClick={() => handleRtvClick(item)}
                    className="btn-pill btn-pill-outline-light text-xs flex items-center gap-1.5 py-1.5"
                  >
                    <ArrowRightLeft className="w-3.5 h-3.5" />
                    <span>Trả hàng NCC (RTV)</span>
                  </button>
                  <button
                    onClick={() => handleDisposalClick(item)}
                    className="btn-pill bg-zinc-900 hover:bg-zinc-800 text-white text-xs flex items-center gap-1.5 py-1.5"
                  >
                    <Trash2 className="w-3.5 h-3.5 text-red-400" />
                    <span>Yêu cầu tiêu hủy</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )
      ) : (
        // Approvals List
        pendingDisposals.length === 0 ? (
          <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
            <Check className="w-12 h-12 text-emerald-500 mx-auto mb-4 bg-emerald-50 p-2.5 rounded-full" />
            <h3 className="text-lg font-bold mb-1">Không có yêu cầu tiêu hủy nào chờ duyệt</h3>
            <p className="text-sm text-shade-50">Mọi yêu cầu tiêu hủy hàng hỏng đã được xử lý xong.</p>
          </div>
        ) : (
          <div className="bg-white rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-zinc-50 border-b border-hairline-light">
                  <th className="px-6 py-3.5 font-bold text-shade-60">Sản phẩm</th>
                  <th className="px-6 py-3.5 font-bold text-shade-60 text-right">Số lượng hủy</th>
                  <th className="px-6 py-3.5 font-bold text-shade-60 text-right">Trị giá</th>
                  <th className="px-6 py-3.5 font-bold text-shade-60">Lý do tiêu hủy</th>
                  <th className="px-6 py-3.5 font-bold text-shade-60">Thẩm quyền duyệt</th>
                  <th className="px-6 py-3.5 font-bold text-shade-60 text-right">Hành động</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {pendingDisposals.map((adj) => {
                  const isAuthorized = getDisposalApprovalAuthority(adj.total_value);
                  return (
                    <tr key={adj.id} className="hover:bg-zinc-50/50">
                      <td className="px-6 py-4">
                        <span className="font-bold block">{adj.product_sku}</span>
                        <span className="text-shade-50 block">{adj.product_name}</span>
                      </td>
                      <td className="px-6 py-4 text-right font-semibold text-red-600">{adj.failed_qty}</td>
                      <td className="px-6 py-4 text-right font-bold">{adj.total_value.toLocaleString('vi-VN')} VND</td>
                      <td className="px-6 py-4 text-shade-60 italic">{adj.cause}</td>
                      <td className="px-6 py-4">{getDisposalThresholdBadge(adj.total_value)}</td>
                      <td className="px-6 py-4 text-right whitespace-nowrap">
                        {isAuthorized ? (
                          <button
                            onClick={() => handleApproveDisposal(adj.id, adj.total_value)}
                            className="inline-flex items-center justify-center rounded-full bg-aloe-10 text-emerald-950 border border-emerald-300 hover:bg-emerald-100 px-3.5 py-1 text-xs font-bold whitespace-nowrap transition-colors duration-150"
                          >
                            Phê duyệt
                          </button>
                        ) : (
                          <span className="text-[10px] text-red-500 font-semibold bg-red-50 border border-red-100 px-2 py-0.5 rounded whitespace-nowrap">
                            Chờ cấp trên duyệt
                          </span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )
      )}

      {/* RTV Modal */}
      {showRtvModal && selectedItem && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-cream border border-hairline-light rounded-lg max-w-md w-full shadow-2xl overflow-hidden">
            <div className="p-5 border-b border-hairline-light bg-white flex justify-between items-center">
              <h3 className="font-bold text-base flex items-center gap-2">
                <ArrowRightLeft className="w-5 h-5 text-indigo-600" />
                Xác nhận xuất trả hàng lỗi NCC
              </h3>
              <button onClick={() => setShowRtvModal(false)} className="p-1 hover:bg-zinc-100 rounded-full">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-5 text-xs flex flex-col gap-4">
              <div className="bg-white p-3 rounded border border-hairline-light shadow-inner flex flex-col gap-2">
                <div><span className="text-shade-50">Sản phẩm:</span> <strong className="text-ink">{selectedItem.product_sku} - {selectedItem.product_name}</strong></div>
                <div><span className="text-shade-50">Nhà cung cấp:</span> <strong>{getSupplierName(selectedItem.supplier_id)}</strong></div>
                <div><span className="text-shade-50">Số lượng lỗi QC xuất trả:</span> <strong className="text-red-600">{selectedItem.qc_failed_qty}</strong></div>
                <div><span className="text-shade-50">Tổng tiền đòi bồi hoàn (Debit Note):</span> <strong className="text-ink text-sm">{selectedItem.total_value.toLocaleString('vi-VN')} VND</strong></div>
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="font-bold">Ghi chú xuất trả (Không bắt buộc)</label>
                <textarea
                  value={actionNotes}
                  onChange={(e) => setActionNotes(e.target.value)}
                  placeholder="Nhập lý do chi tiết xuất trả..."
                  className="text-input h-16 resize-none"
                />
              </div>
            </div>
            <div className="p-4 border-t border-hairline-light bg-zinc-50 flex justify-end gap-2">
              <button onClick={() => setShowRtvModal(false)} className="btn-pill btn-pill-outline-light text-xs py-1.5 px-4">
                Hủy
              </button>
              <button
                onClick={submitRtv}
                disabled={submitting}
                className="btn-pill btn-pill-aloe text-xs py-1.5 px-4 font-bold disabled:opacity-50"
              >
                {submitting ? 'Đang xuất...' : 'Xác nhận RTV & Đòi tiền'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Disposal Request Modal */}
      {showDisposalModal && selectedItem && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-cream border border-hairline-light rounded-lg max-w-md w-full shadow-2xl overflow-hidden">
            <div className="p-5 border-b border-hairline-light bg-white flex justify-between items-center">
              <h3 className="font-bold text-base flex items-center gap-2">
                <Trash2 className="w-5 h-5 text-red-600" />
                Yêu cầu tiêu hủy hàng hỏng
              </h3>
              <button onClick={() => setShowDisposalModal(false)} className="p-1 hover:bg-zinc-100 rounded-full">
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="p-5 text-xs flex flex-col gap-4">
              <div className="bg-white p-3 rounded border border-hairline-light shadow-inner flex flex-col gap-2">
                <div><span className="text-shade-50">Sản phẩm:</span> <strong>{selectedItem.product_sku} - {selectedItem.product_name}</strong></div>
                <div><span className="text-shade-50">Số lượng hủy:</span> <strong className="text-red-600">{selectedItem.qc_failed_qty}</strong></div>
                <div><span className="text-shade-50">Tổng trị giá:</span> <strong>{selectedItem.total_value.toLocaleString('vi-VN')} VND</strong></div>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-shade-50">Thẩm quyền:</span>
                  {getDisposalThresholdBadge(selectedItem.total_value)}
                </div>
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="font-bold">Lý do tiêu hủy (Bắt buộc)</label>
                <textarea
                  value={actionNotes}
                  onChange={(e) => setActionNotes(e.target.value)}
                  placeholder="Nhập nguyên nhân hỏng hóc, mốc rỉ nghiêm trọng không thể sửa..."
                  className="text-input h-20 resize-none"
                  required
                />
              </div>
              <div className="flex flex-col gap-1.5">
                <label className="font-bold">Đường dẫn ảnh chụp minh chứng (Không bắt buộc)</label>
                <input
                  type="text"
                  placeholder="https://imgur.com/link_anh.jpg"
                  value={disposalImageUrl}
                  onChange={(e) => setDisposalImageUrl(e.target.value)}
                  className="text-input"
                />
              </div>
            </div>
            <div className="p-4 border-t border-hairline-light bg-zinc-50 flex justify-end gap-2">
              <button onClick={() => setShowDisposalModal(false)} className="btn-pill btn-pill-outline-light text-xs py-1.5 px-4">
                Hủy
              </button>
              <button
                onClick={submitDisposal}
                disabled={submitting || !actionNotes.trim()}
                className="btn-pill btn-pill-primary text-xs py-1.5 px-4 disabled:opacity-50"
              >
                {submitting ? 'Đang xử lý...' : 'Gửi yêu cầu'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default QuarantineWorkspace;
