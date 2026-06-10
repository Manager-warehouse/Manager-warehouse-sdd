import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';
import { ROLES } from '../../utils/constants';
import { Plus, Search, FileText, CheckCircle2, AlertTriangle, Eye, Check, X, Loader2 } from 'lucide-react';

const ReceiptList = () => {
  const navigate = useNavigate();
  const activeWarehouse = useAuthStore((state) => state.activeWarehouse);
  const { user, hasRole } = useAuthStore();
  const { addToast } = useUiStore();

  const [receipts, setReceipts] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [dealers, setDealers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');

  // Approval Modal State
  const [showApprovalModal, setShowApprovalModal] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState(null);
  const [approvalNotes, setApprovalNotes] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [submittingApproval, setSubmittingApproval] = useState(false);
  const [isRejecting, setIsRejecting] = useState(false);

  useEffect(() => {
    fetchData();
  }, [activeWarehouse]);

  const fetchData = async () => {
    if (!activeWarehouse) return;
    setLoading(true);
    try {
      const [receiptsData, suppliersData, dealersData] = await Promise.all([
        inboundService.getReceipts(activeWarehouse.id),
        masterDataService.getSuppliers(),
        masterDataService.getDealers(),
      ]);
      setReceipts(receiptsData);
      setSuppliers(suppliersData);
      setDealers(dealersData);
    } catch (error) {
      console.error(error);
      addToast('Lỗi khi tải dữ liệu phiếu nhập kho', 'error');
    } finally {
      setLoading(false);
    }
  };

  const getPartnerName = (receipt) => {
    if (receipt.type === 'PURCHASE') {
      const supplier = suppliers.find(s => s.id === receipt.supplier_id);
      return supplier ? supplier.company_name : `NCC ID: ${receipt.supplier_id}`;
    } else {
      const dealer = dealers.find(d => d.id === receipt.dealer_id);
      return dealer ? dealer.name : `Đại lý ID: ${receipt.dealer_id}`;
    }
  };

  // Filter & Search logic
  const filteredReceipts = receipts.filter(r => {
    const matchesSearch = r.receipt_number.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (r.source_order_code && r.source_order_code.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesStatus = statusFilter === 'ALL' || r.status === statusFilter;
    const matchesType = typeFilter === 'ALL' || r.type === typeFilter;
    return matchesSearch && matchesStatus && matchesType;
  });

  const getStatusBadge = (status) => {
    const baseStyle = "text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase tracking-wider whitespace-nowrap";
    switch (status) {
      case 'PENDING_RECEIPT':
        return <span className={`${baseStyle} bg-zinc-100 text-zinc-800 border-zinc-200`}>Chờ nhận</span>;
      case 'DRAFT':
        return <span className={`${baseStyle} bg-blue-50 text-blue-700 border-blue-200`}>Đã đếm (nháp)</span>;
      case 'QC_COMPLETED':
        return <span className={`${baseStyle} bg-amber-50 text-amber-700 border-amber-200`}>Đã QC</span>;
      case 'APPROVED':
        return <span className={`${baseStyle} bg-aloe-10 text-emerald-900 border-emerald-300`}>Đã duyệt</span>;
      case 'REJECTED':
        return <span className={`${baseStyle} bg-red-50 text-red-700 border-red-200`}>Từ chối</span>;
      default:
        return <span className={`${baseStyle} bg-zinc-100 text-zinc-800 border-zinc-200`}>{status}</span>;
    }
  };

  // Approval Handlers
  const handleOpenApproval = async (receiptId) => {
    try {
      const detail = await inboundService.getReceiptById(receiptId);
      setSelectedReceipt(detail);
      setApprovalNotes('');
      setRejectionReason('');
      setIsRejecting(false);
      setShowApprovalModal(true);
    } catch (error) {
      addToast('Không thể lấy chi tiết phiếu nhập', 'error');
    }
  };

  const submitApprove = async () => {
    setSubmittingApproval(true);
    try {
      await inboundService.approveReceipt(selectedReceipt.id, approvalNotes);
      addToast(`Đã phê duyệt phiếu nhập ${selectedReceipt.receipt_number} thành công`, 'success');
      setShowApprovalModal(false);
      fetchData();
    } catch (error) {
      addToast(error.message === 'RECEIPT_ALREADY_APPROVED' ? 'Phiếu này đã được duyệt trước đó' : 'Lỗi phê duyệt', 'error');
    } finally {
      setSubmittingApproval(false);
    }
  };

  const submitReject = async () => {
    if (!rejectionReason.trim()) {
      addToast('Vui lòng nhập lý do từ chối', 'warning');
      return;
    }
    setSubmittingApproval(true);
    try {
      await inboundService.rejectReceipt(selectedReceipt.id, rejectionReason);
      addToast(`Đã từ chối phiếu nhập ${selectedReceipt.receipt_number}`, 'info');
      setShowApprovalModal(false);
      fetchData();
    } catch (error) {
      addToast('Lỗi từ chối phê duyệt', 'error');
    } finally {
      setSubmittingApproval(false);
    }
  };

  const getProductName = (productId) => {
    // A simple product mapping if necessary, or just display ID/SKU if mock
    return productId === 1 ? 'Màn hình ASUS ProArt 27K' : 'Chuột Logitech MX Master 3S';
  };

  const getProductSku = (productId) => {
    return productId === 1 ? 'SKU-PA-001' : 'SKU-LOGI-MX3';
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header section */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Vận hành / Inbound
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Nhập hàng & QC Inbound
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản lý tiếp nhận hàng, kiểm đếm thực tế, QC và cất kệ tại kho <span className="font-semibold text-ink">{activeWarehouse?.name} ({activeWarehouse?.code})</span>.
          </p>
        </div>

        {(hasRole(ROLES.PLANNER) || hasRole(ROLES.ADMIN)) && (
          <button
            onClick={() => navigate('/inbound/create')}
            className="btn-pill btn-pill-primary flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            <span>Lập lệnh nhập kho</span>
          </button>
        )}
      </div>

      {/* Filters & search */}
      <div className="bg-white rounded-lg border border-hairline-light p-4 shadow-sm flex flex-col md:flex-row gap-4 items-center justify-between mb-6">
        <div className="relative w-full md:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
          <input
            type="text"
            placeholder="Tìm mã phiếu, số PO..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full text-input pl-10"
          />
        </div>

        <div className="flex flex-wrap gap-3 w-full md:w-auto justify-end">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-shade-50">Trạng thái:</span>
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="text-input text-xs py-1.5"
            >
              <option value="ALL">Tất cả</option>
              <option value="PENDING_RECEIPT">Chờ nhận</option>
              <option value="DRAFT">Đã đếm (Nháp)</option>
              <option value="QC_COMPLETED">Đã QC</option>
              <option value="APPROVED">Đã duyệt</option>
              <option value="REJECTED">Từ chối</option>
            </select>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-shade-50">Phân loại:</span>
            <select
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              className="text-input text-xs py-1.5"
            >
              <option value="ALL">Tất cả</option>
              <option value="PURCHASE">Nhập mua (PO)</option>
              <option value="RETURN">Nhập trả (DO hoàn)</option>
            </select>
          </div>
        </div>
      </div>

      {/* Main Table */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : filteredReceipts.length === 0 ? (
        <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
          <FileText className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy phiếu nhập kho nào</h3>
          <p className="text-sm text-shade-50">Thay đổi bộ lọc hoặc tạo một phiếu mới để bắt đầu.</p>
        </div>
      ) : (
        <div className="bg-white rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-zinc-50 border-b border-hairline-light">
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Mã phiếu</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Loại</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Chứng từ gốc</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Đối tác</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Ngày chứng từ</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider">Trạng thái</th>
                  <th className="px-6 py-3.5 text-xs font-bold text-shade-60 uppercase tracking-wider text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {filteredReceipts.map((receipt) => (
                  <tr key={receipt.id} className="hover:bg-zinc-50 transition-colors">
                    <td className="px-6 py-4 text-xs font-bold">{receipt.receipt_number}</td>
                    <td className="px-6 py-4 text-xs font-semibold">
                      {receipt.type === 'PURCHASE' ? (
                        <span className="text-indigo-600">Nhập mua (PO)</span>
                      ) : (
                        <span className="text-teal-600">Trả hàng (DO hoàn)</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-xs text-shade-50">{receipt.source_order_code || 'N/A'}</td>
                    <td className="px-6 py-4 text-xs font-semibold">{getPartnerName(receipt)}</td>
                    <td className="px-6 py-4 text-xs text-shade-50">{receipt.document_date}</td>
                    <td className="px-6 py-4">{getStatusBadge(receipt.status)}</td>
                    <td className="px-6 py-4 text-right whitespace-nowrap">
                      <div className="flex gap-2 justify-end items-center">
                        {/* 1. Receive Action: status=PENDING_RECEIPT, role=STOREKEEPER/ADMIN */}
                        {receipt.status === 'PENDING_RECEIPT' && (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN)) && (
                          <button
                            onClick={() => navigate(`/inbound/receive/${receipt.id}`)}
                            className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light text-ink hover:bg-zinc-100 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
                          >
                            Đếm thực tế
                          </button>
                        )}

                        {/* 2. QC Action: status=DRAFT, role=WAREHOUSE_STAFF/ADMIN */}
                        {receipt.status === 'DRAFT' && (hasRole(ROLES.WAREHOUSE_STAFF) || hasRole(ROLES.ADMIN)) && (
                          <button
                            onClick={() => navigate(`/inbound/qc/${receipt.id}`)}
                            className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light text-ink hover:bg-zinc-100 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
                          >
                            Kiểm QC
                          </button>
                        )}

                        {/* 3. Approval Action: status=QC_COMPLETED, role=WAREHOUSE_MANAGER/ADMIN */}
                        {receipt.status === 'QC_COMPLETED' && (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) && (
                          <button
                            onClick={() => handleOpenApproval(receipt.id)}
                            className="inline-flex items-center justify-center rounded-full bg-aloe-10 text-emerald-950 border border-emerald-300 hover:bg-emerald-100 px-3 py-1 text-xs font-bold whitespace-nowrap transition-colors duration-150"
                          >
                            Duyệt phiếu
                          </button>
                        )}

                        {/* 4. Putaway Action: status=APPROVED, role=STOREKEEPER/ADMIN */}
                        {receipt.status === 'APPROVED' && (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN)) && (
                          <button
                            onClick={() => navigate(`/inbound/putaway/${receipt.id}`)}
                            className="inline-flex items-center justify-center rounded-full bg-ink text-onPrimary hover:bg-shade-70 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
                          >
                            Cất kệ
                          </button>
                        )}

                        {/* View only fallback */}
                        <button
                          onClick={async () => {
                            try {
                              const detail = await inboundService.getReceiptById(receipt.id);
                              setSelectedReceipt(detail);
                              setIsRejecting(false);
                              setApprovalNotes('');
                              setShowApprovalModal(true);
                            } catch (e) {
                              addToast('Lỗi xem chi tiết', 'error');
                            }
                          }}
                          className="p-1.5 hover:bg-zinc-200 rounded-full text-shade-50 hover:text-ink transition-colors flex items-center justify-center"
                          title="Xem chi tiết"
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
        </div>
      )}

      {/* Approval & View Detail Modal */}
      {showApprovalModal && selectedReceipt && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-cream rounded-lg max-w-3xl w-full border border-hairline-light shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
            <div className="p-6 border-b border-hairline-light flex items-center justify-between bg-white">
              <div>
                <span className="text-[10px] font-bold text-shade-40 uppercase tracking-widest block mb-1">Chi tiết phiếu</span>
                <h3 className="text-xl font-bold flex items-center gap-3">
                  {selectedReceipt.receipt_number}
                  {getStatusBadge(selectedReceipt.status)}
                </h3>
              </div>
              <button
                onClick={() => setShowApprovalModal(false)}
                className="p-1 hover:bg-zinc-100 rounded-full transition-colors text-shade-50 hover:text-ink"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6 overflow-y-auto flex-1 flex flex-col gap-6">
              {/* Receipt Info */}
              <div className="grid grid-cols-2 gap-4 text-xs">
                <div>
                  <span className="text-shade-50 block mb-0.5">Loại nhập kho:</span>
                  <span className="font-bold">{selectedReceipt.type === 'PURCHASE' ? 'Nhập mua (PO)' : 'Trả hàng (DO hoàn)'}</span>
                </div>
                <div>
                  <span className="text-shade-50 block mb-0.5">Chứng từ gốc:</span>
                  <span className="font-bold">{selectedReceipt.source_order_code || 'N/A'}</span>
                </div>
                <div>
                  <span className="text-shade-50 block mb-0.5">Đối tác:</span>
                  <span className="font-bold">{getPartnerName(selectedReceipt)}</span>
                </div>
                <div>
                  <span className="text-shade-50 block mb-0.5">Ngày chứng từ:</span>
                  <span className="font-bold">{selectedReceipt.document_date}</span>
                </div>
                {selectedReceipt.approved_at && (
                  <div className="col-span-2 bg-emerald-50 border border-emerald-200 text-emerald-950 p-2.5 rounded-md flex gap-2 items-center">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
                    <span>Phiếu đã được duyệt bởi Trưởng kho lúc {new Date(selectedReceipt.approved_at).toLocaleString('vi-VN')}</span>
                  </div>
                )}
                {selectedReceipt.rejection_reason && (
                  <div className="col-span-2 bg-red-50 border border-red-200 text-red-950 p-2.5 rounded-md flex gap-2 items-center">
                    <AlertTriangle className="w-4 h-4 text-red-600 flex-shrink-0" />
                    <span>Bị từ chối duyệt. Lý do: <strong className="font-semibold">{selectedReceipt.rejection_reason}</strong></span>
                  </div>
                )}
              </div>

              {/* Items List */}
              <div>
                <h4 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3">Danh sách sản phẩm kiểm định</h4>
                <div className="border border-hairline-light rounded-lg overflow-hidden bg-white shadow-inner">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-zinc-50 border-b border-hairline-light">
                        <th className="px-4 py-2.5 font-bold text-shade-60">Sản phẩm</th>
                        <th className="px-4 py-2.5 font-bold text-shade-60 text-right">Dự kiến</th>
                        <th className="px-4 py-2.5 font-bold text-shade-60 text-right">Đếm thực tế</th>
                        <th className="px-4 py-2.5 font-bold text-shade-60 text-right">Đạt QC</th>
                        <th className="px-4 py-2.5 font-bold text-shade-60 text-right">Lỗi QC</th>
                        <th className="px-4 py-2.5 font-bold text-shade-60">Grade/Chi tiết</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {selectedReceipt.items.map((item) => (
                        <tr key={item.id} className="hover:bg-zinc-50/50">
                          <td className="px-4 py-3">
                            <span className="font-semibold block">{getProductName(item.product_id)}</span>
                            <span className="text-[10px] text-shade-40 font-mono block">{getProductSku(item.product_id)}</span>
                          </td>
                          <td className="px-4 py-3 text-right font-semibold">{item.expected_qty}</td>
                          <td className="px-4 py-3 text-right font-semibold">{item.actual_qty !== null ? item.actual_qty : '-'}</td>
                          <td className="px-4 py-3 text-right font-bold text-emerald-600">{item.qc_passed_qty !== null ? item.qc_passed_qty : '-'}</td>
                          <td className="px-4 py-3 text-right font-bold text-red-600">{item.qc_failed_qty !== null ? item.qc_failed_qty : '-'}</td>
                          <td className="px-4 py-3">
                            {item.qc_result ? (
                              <div className="flex flex-col gap-0.5">
                                <div className="flex gap-1.5 items-center">
                                  <span className="text-[10px] font-bold uppercase text-indigo-700 bg-indigo-50 px-1 py-0.2 rounded border border-indigo-200">
                                    Grade {item.grade || 'A'}
                                  </span>
                                  <span className={`text-[9px] font-bold ${item.qc_result === 'PASSED' ? 'text-emerald-700' : item.qc_result === 'FAILED' ? 'text-red-700' : 'text-amber-700'}`}>
                                    {item.qc_result}
                                  </span>
                                </div>
                                {item.qc_failure_reason && (
                                  <span className="text-[10px] text-red-600 italic block">{item.qc_failure_reason}</span>
                                )}
                              </div>
                            ) : (
                              <span className="text-shade-40 italic">Chưa kiểm định</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Form Input for approval notes / rejection reason */}
              {selectedReceipt.status === 'QC_COMPLETED' && (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) && (
                <div className="bg-white p-4 border border-hairline-light rounded-lg shadow-sm">
                  {isRejecting ? (
                    <div className="flex flex-col gap-2">
                      <label className="text-xs font-bold text-red-700 flex items-center gap-1.5">
                        <AlertTriangle className="w-3.5 h-3.5" />
                        Lý do từ chối phê duyệt (Bắt buộc)
                      </label>
                      <textarea
                        value={rejectionReason}
                        onChange={(e) => setRejectionReason(e.target.value)}
                        placeholder="Nhập lý do chi tiết từ chối phiếu nhập này..."
                        className="text-input text-xs h-20 resize-none border-red-300 focus:border-red-500 focus:ring-red-100"
                        required
                      />
                    </div>
                  ) : (
                    <div className="flex flex-col gap-2">
                      <label className="text-xs font-bold text-ink">Ghi chú phê duyệt (Không bắt buộc)</label>
                      <textarea
                        value={approvalNotes}
                        onChange={(e) => setApprovalNotes(e.target.value)}
                        placeholder="Nhập ý kiến phê duyệt của bạn..."
                        className="text-input text-xs h-20 resize-none"
                      />
                    </div>
                  )}
                </div>
              )}
            </div>

            {/* Modal Footer */}
            <div className="p-4 border-t border-hairline-light bg-zinc-50 flex justify-between gap-3">
              <button
                onClick={() => setShowApprovalModal(false)}
                className="btn-pill btn-pill-outline-light text-xs"
              >
                Đóng
              </button>

              {selectedReceipt.status === 'QC_COMPLETED' && (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) && (
                <div className="flex gap-2">
                  {isRejecting ? (
                    <>
                      <button
                        onClick={() => setIsRejecting(false)}
                        className="btn-pill btn-pill-outline-light text-xs py-1.5 px-4"
                      >
                        Quay lại
                      </button>
                      <button
                        onClick={submitReject}
                        disabled={submittingApproval}
                        className="btn-pill bg-red-600 hover:bg-red-700 text-white text-xs py-1.5 px-4 font-bold disabled:opacity-50"
                      >
                        {submittingApproval ? 'Đang từ chối...' : 'Xác nhận từ chối'}
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={() => setIsRejecting(true)}
                        className="btn-pill btn-pill-outline-light border-red-500 hover:bg-red-50 text-red-600 text-xs py-1.5 px-4"
                      >
                        Từ chối
                      </button>
                      <button
                        onClick={submitApprove}
                        disabled={submittingApproval}
                        className="btn-pill btn-pill-aloe text-xs py-1.5 px-4 font-bold disabled:opacity-50 flex items-center gap-1.5"
                      >
                        {submittingApproval ? (
                          <>
                            <Loader2 className="w-3.5 h-3.5 animate-spin" />
                            Đang duyệt...
                          </>
                        ) : (
                          <>
                            <Check className="w-3.5 h-3.5" />
                            Duyệt nhập kho
                          </>
                        )}
                      </button>
                    </>
                  )}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReceiptList;
