import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';
import { ROLES } from '../../utils/constants';
import { Plus, Search, FileText, CheckCircle2, AlertTriangle, Eye, Check, X, Loader2 } from 'lucide-react';
import Input from '../../components/common/Input';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';

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
      addToast('Lỗi khi tải dữ liệu phiếu nhập kho', 'error');
    } finally {
      setLoading(false);
    }
  };

  const isPutawayCompleted = (receipt) => {
    if (!receipt.items || receipt.items.length === 0) return false;
    const passedItems = receipt.items.filter(item => (item.qc_passed_qty || 0) > 0);
    if (passedItems.length === 0) return false;
    return passedItems.every(item => item.location_id !== null && item.location_id !== undefined);
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
  const filteredReceipts = receipts.filter((receipt) => {
    const needle = searchTerm.toLowerCase();
    const partnerName = getPartnerName(receipt);
    const sourceReference = receipt.source_reference || 'N/A';
    const matchesSearch = receipt.receipt_number.toLowerCase().includes(needle)
      || sourceReference.toLowerCase().includes(needle)
      || partnerName.toLowerCase().includes(needle);
    const matchesStatus = statusFilter === 'ALL' || receipt.status === statusFilter;
    const matchesType = typeFilter === 'ALL' || receipt.type === typeFilter;
    return matchesSearch && matchesStatus && matchesType;
  });

  const getStatusBadge = (receipt) => {
    if (!receipt) return null;
    if (receipt.status === 'APPROVED' && isPutawayCompleted(receipt)) {
      return <Badge size="sm" colorClassName="bg-success-100 text-success-800 border-success-300">Đã cất kệ</Badge>;
    }
    switch (receipt.status) {
      case 'PENDING_RECEIPT':
        return <Badge size="sm" colorClassName="bg-canvas-cream text-shade-70 border-hairline-light">Chờ nhận</Badge>;
      case 'DRAFT':
        return <Badge size="sm" colorClassName="bg-info-50 text-info-700 border-info-200">Đã đếm (nháp)</Badge>;
      case 'QC_COMPLETED':
        return <Badge size="sm" colorClassName="bg-warning-50 text-warning-700 border-warning-200">Đã QC</Badge>;
      case 'APPROVED':
        return <Badge size="sm" colorClassName="bg-aloe-10 text-success-900 border-success-300">Đã duyệt</Badge>;
      case 'REJECTED':
        return <Badge size="sm" colorClassName="bg-danger-50 text-danger-700 border-danger-200">Từ chối</Badge>;
      case 'IN_TRANSIT':
        return <Badge size="sm" colorClassName="bg-warning-50 text-warning-700 border-warning-200">Chờ nhận nội bộ</Badge>;
      case 'COMPLETED':
        return <Badge size="sm" colorClassName="bg-aloe-10 text-success-900 border-success-300">Đã nhập kho</Badge>;
      case 'COMPLETED_WITH_DISCREPANCY':
        return <Badge size="sm" colorClassName="bg-warning-50 text-warning-700 border-warning-200">Đã nhập có lệch</Badge>;
      default:
        return <Badge size="sm" colorClassName="bg-canvas-cream text-shade-70 border-hairline-light">{receipt.status}</Badge>;
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
      await inboundService.approveReceipt(selectedReceipt.id, approvalNotes, selectedReceipt.version);
      addToast(`Đã phê duyệt phiếu nhập ${selectedReceipt.receipt_number} thành công`, 'success');
      setShowApprovalModal(false);
      fetchData();
    } catch (error) {
      addToast(error.message === 'RECEIPT_ALREADY_APPROVED' ? 'Phiếu này đã được duyệt trước đó' : 'Lỗi phê duyệt', 'error');
    } finally {
      setSubmittingApproval(false);
    }
  };

  const handleConfirmQc = async (receipt) => {
    try {
      await inboundService.qcReceipt(receipt.id, { action: 'CONFIRM' });
      addToast(`Đã xác nhận QC cho phiếu ${receipt.receipt_number}`, 'success');
      fetchData();
    } catch (error) {
      const serverMessage = error.response?.data?.message || error.response?.data?.error || error.message;
      const message = serverMessage === 'QC_NOT_YET_SUBMITTED'
        ? 'Chưa có kết quả QC để xác nhận'
        : serverMessage;
      addToast(message || 'Lỗi xác nhận QC', 'error');
    }
  };

  const submitReject = async () => {
    if (!rejectionReason.trim()) {
      addToast('Vui lòng nhập lý do từ chối', 'warning');
      return;
    }
    setSubmittingApproval(true);
    try {
      await inboundService.rejectReceipt(selectedReceipt.id, rejectionReason, selectedReceipt.version);
      addToast(`Đã từ chối phiếu nhập ${selectedReceipt.receipt_number}`, 'info');
      setShowApprovalModal(false);
      fetchData();
    } catch (error) {
      addToast('Lỗi từ chối phê duyệt', 'error');
    } finally {
      setSubmittingApproval(false);
    }
  };

  const getProductName = (item) => {
    if (item && item.product_name) return item.product_name;
    const productId = typeof item === 'object' ? item.product_id : item;
    return productId === 1 ? 'Màn hình ASUS ProArt 27K' : 'Chuột Logitech MX Master 3S';
  };

  const getProductSku = (item) => {
    if (item && item.product_sku) return item.product_sku;
    const productId = typeof item === 'object' ? item.product_id : item;
    return productId === 1 ? 'SKU-PA-001' : 'SKU-LOGI-MX3';
  };

  const openDetail = async (receipt) => {
    try {
      const detail = await inboundService.getReceiptById(receipt.id);
      setSelectedReceipt(detail);
      setIsRejecting(false);
      setApprovalNotes('');
      setShowApprovalModal(true);
    } catch (e) {
      addToast('Lỗi xem chi tiết', 'error');
    }
  };

  const renderReceiptActions = (receipt) => (
    <>
      {receipt.status === 'PENDING_RECEIPT' && (hasRole(ROLES.WAREHOUSE_STAFF) || hasRole(ROLES.ADMIN)) && (
        <button
          onClick={() => navigate(`/inbound/receive/${receipt.id}`)}
          className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light text-ink hover:bg-canvas-cream px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
        >
          Đếm thực tế
        </button>
      )}

      {receipt.status === 'DRAFT' && (
        hasRole(ROLES.WAREHOUSE_STAFF)
        || hasRole(ROLES.STOREKEEPER)
        || hasRole(ROLES.WAREHOUSE_MANAGER)
        || hasRole(ROLES.ADMIN)
      ) && (
        <button
          onClick={() => navigate(`/inbound/qc/${receipt.id}`)}
          className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light text-ink hover:bg-canvas-cream px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
        >
          Kiểm QC
        </button>
      )}

      {receipt.status === 'DRAFT' && (
        hasRole(ROLES.STOREKEEPER)
        || hasRole(ROLES.WAREHOUSE_MANAGER)
        || hasRole(ROLES.ADMIN)
      ) && (
        <button
          onClick={() => handleConfirmQc(receipt)}
          className="inline-flex items-center justify-center rounded-full bg-ink text-onPrimary hover:bg-shade-70 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
        >
          Xác nhận QC
        </button>
      )}

      {receipt.status === 'QC_COMPLETED' && (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) && (
        <button
          onClick={() => handleOpenApproval(receipt.id)}
          className="inline-flex items-center justify-center rounded-full bg-aloe-10 text-success-950 border border-success-300 hover:bg-success-100 px-3 py-1 text-xs font-bold whitespace-nowrap transition-colors duration-150"
        >
          Duyệt phiếu
        </button>
      )}

      {receipt.status === 'APPROVED' && !isPutawayCompleted(receipt) && (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN)) && (
        <button
          onClick={() => navigate(`/inbound/putaway/${receipt.id}`)}
          className="inline-flex items-center justify-center rounded-full bg-ink text-onPrimary hover:bg-shade-70 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
        >
          Cất kệ
        </button>
      )}

      {receipt.status === 'APPROVED' && isPutawayCompleted(receipt) && (
        <span className="text-xs font-bold text-success-600 flex items-center gap-1.5 px-3 py-1">
          <Check className="w-3.5 h-3.5" />
          Đã cất
        </span>
      )}

      <button
        onClick={() => openDetail(receipt)}
        className="p-1.5 hover:bg-canvas-cream rounded-full text-shade-50 hover:text-ink transition-colors flex items-center justify-center"
        title="Xem chi tiết"
      >
        <Eye className="w-4 h-4" />
      </button>
    </>
  );

  return (
    <div className="mobile-page">
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
            Quản lý nhập mua, nhập trả và QC tại kho <span className="font-semibold text-ink">{activeWarehouse?.name} ({activeWarehouse?.code})</span>. Mã `RN` thuộc luồng phiếu nhập; điều chuyển nội bộ `TRF` được xử lý ở màn Điều chuyển nội bộ riêng.
          </p>
        </div>

        {(hasRole(ROLES.PLANNER) || hasRole(ROLES.ADMIN)) && (
          <Button
            onClick={() => navigate('/inbound/create')}
            variant="primary"
            icon={Plus}
          >
            Lập lệnh nhập kho
          </Button>
        )}
      </div>

      {/* Filters & search */}
      <div className="mobile-filter-bar bg-canvas-light rounded-lg border border-hairline-light p-3 md:p-4 shadow-level-3 md:flex md:flex-row md:gap-4 md:items-center md:justify-between mb-2 md:mb-6">
        <div className="w-full md:w-80">
          <Input
            type="text"
            leftIcon={Search}
            placeholder="Tìm mã phiếu, số PO..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>

        <div className="mobile-filter-bar md:flex md:flex-wrap md:gap-3 md:w-auto md:justify-end">
          <div className="w-full sm:w-48">
            <Input
              type="select"
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              options={[
                { value: 'ALL', label: 'Tất cả trạng thái' },
                { value: 'PENDING_RECEIPT', label: 'Chờ nhận' },
                { value: 'DRAFT', label: 'Đã đếm (Nháp)' },
                { value: 'QC_COMPLETED', label: 'Đã QC' },
                { value: 'APPROVED', label: 'Đã duyệt' },
                { value: 'REJECTED', label: 'Từ chối' },
              ]}
            />
          </div>
          <div className="w-full sm:w-44">
            <Input
              type="select"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              options={[
                { value: 'ALL', label: 'Tất cả loại' },
                { value: 'PURCHASE', label: 'Nhập mua (PO)' },
                { value: 'RETURN', label: 'Nhập trả (DO hoàn)' },
              ]}
            />
          </div>
        </div>
      </div>

      {/* Main Table */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : (
        <>
          {filteredReceipts.length === 0 ? (
        <div className="bg-canvas-light rounded-lg border border-hairline-light p-12 text-center shadow-level-3">
          <FileText className="w-12 h-12 text-shade-30 mx-auto mb-4" />
          <h3 className="text-lg font-bold mb-1">Không tìm thấy phiếu nhập kho nào</h3>
          <p className="text-sm text-shade-50">Thử đổi bộ lọc để xem phiếu nhập mua hoặc phiếu nhập trả.</p>
        </div>
          ) : (
        <>
          {/* Desktop/tablet: table view */}
          <div className="hidden md:block bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="bg-canvas-cream border-b border-hairline-light">
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã phiếu</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Loại</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Chứng từ gốc</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Đối tác</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Ngày chứng từ</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Trạng thái</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Hành động</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {filteredReceipts.map((receipt) => (
                    <tr key={receipt.id} className="hover:bg-canvas-cream/50 transition-colors">
                      <td className="px-6 py-4 text-xs font-bold">{receipt.receipt_number}</td>
                      <td className="px-6 py-4 text-xs font-semibold">
                        {receipt.type === 'PURCHASE' ? (
                          <span className="text-shade-70">Nhập mua (PO)</span>
                        ) : receipt.type === 'RETURN' ? (
                          <span className="text-shade-70">Nhập trả (DO hoàn)</span>
                        ) : (
                          <span>-</span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-xs text-shade-50">{receipt.source_reference || 'N/A'}</td>
                      <td className="px-6 py-4 text-xs font-semibold">{getPartnerName(receipt)}</td>
                      <td className="px-6 py-4 text-xs text-shade-50">{receipt.document_date}</td>
                      <td className="px-6 py-4">{getStatusBadge(receipt)}</td>
                      <td className="px-6 py-4 text-right whitespace-nowrap">
                        <div className="flex gap-2 justify-end items-center">
                          {renderReceiptActions(receipt)}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Mobile: stacked card view */}
          <div className="flex flex-col gap-3 md:hidden">
            {filteredReceipts.map((receipt) => (
              <div key={receipt.id} className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
                <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                  <span className="text-xs font-bold text-ink">{receipt.receipt_number}</span>
                  {getStatusBadge(receipt)}
                </div>
                <div className="p-4 flex flex-col gap-2 text-xs">
                  <p className="text-shade-50">Loại: <span className="font-semibold text-ink">
                    {receipt.type === 'PURCHASE' ? 'Nhập mua (PO)' : receipt.type === 'RETURN' ? 'Nhập trả (DO hoàn)' : '-'}
                  </span></p>
                  <p className="text-shade-50">Đối tác: <span className="font-semibold text-ink">{getPartnerName(receipt)}</span></p>
                  <p className="text-shade-50">Chứng từ gốc: <span className="font-semibold text-ink">{receipt.source_reference || 'N/A'}</span></p>
                  <p className="text-shade-50">Ngày chứng từ: <span className="font-semibold text-ink">{receipt.document_date}</span></p>
                </div>
                <div className="p-4 border-t border-hairline-light flex flex-wrap gap-2">
                  {renderReceiptActions(receipt)}
                </div>
              </div>
            ))}
          </div>
        </>
          )}
        </>
      )}

      {/* Approval & View Detail Modal */}
      {showApprovalModal && selectedReceipt && (
        <div className="fixed inset-0 bg-canvas-night/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-cream rounded-lg max-w-3xl w-full border border-hairline-light shadow-level-4 overflow-hidden flex flex-col max-h-[85vh]">
            <div className="p-6 border-b border-hairline-light flex items-center justify-between bg-canvas-cream">
              <div>
                <span className="text-[10px] font-bold text-shade-40 uppercase tracking-widest block mb-1">Chi tiết phiếu</span>
                <h3 className="text-xl font-bold flex items-center gap-3">
                  {selectedReceipt.receipt_number}
                  {getStatusBadge(selectedReceipt)}
                </h3>
              </div>
              <button
                onClick={() => setShowApprovalModal(false)}
                className="p-1 hover:bg-canvas-cream rounded-full transition-colors text-shade-50 hover:text-ink"
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
                  <span className="font-bold">{selectedReceipt.source_reference || 'N/A'}</span>
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
                  <div className="col-span-2 bg-success-50 border border-success-200 text-success-950 p-2.5 rounded-md flex gap-2 items-center">
                    <CheckCircle2 className="w-4 h-4 text-success-600 flex-shrink-0" />
                    <span>Phiếu đã được duyệt bởi Trưởng kho lúc {new Date(selectedReceipt.approved_at).toLocaleString('vi-VN')}</span>
                  </div>
                )}
                {selectedReceipt.rejection_reason && (
                  <div className="col-span-2 bg-danger-50 border border-danger-200 text-danger-950 p-2.5 rounded-md flex gap-2 items-center">
                    <AlertTriangle className="w-4 h-4 text-danger-600 flex-shrink-0" />
                    <span>Bị từ chối duyệt. Lý do: <strong className="font-semibold">{selectedReceipt.rejection_reason}</strong></span>
                  </div>
                )}
              </div>

              {/* Items List */}
              <div>
                <h4 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3">Danh sách sản phẩm kiểm định</h4>
                <div className="border border-hairline-light rounded-lg overflow-hidden bg-canvas-light shadow-inner">
                  <table className="w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-canvas-cream border-b border-hairline-light">
                        <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Sản phẩm</th>
                        <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Dự kiến</th>
                        <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Đếm thực tế</th>
                        <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Đạt QC</th>
                        <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Lỗi QC</th>
                        <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Chi tiết QC</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {selectedReceipt.items.map((item) => (
                        <tr key={item.id} className="hover:bg-canvas-cream/50 transition-colors">
                          <td className="px-4 py-3">
                            <span className="font-semibold block">{getProductName(item)}</span>
                            <span className="text-[10px] text-shade-40 font-mono block">{getProductSku(item)}</span>
                          </td>
                          <td className="px-4 py-3 text-right font-semibold">{item.expected_qty}</td>
                          <td className="px-4 py-3 text-right font-semibold">{item.actual_qty !== null ? item.actual_qty : '-'}</td>
                          <td className="px-4 py-3 text-right font-bold text-success-600">{item.qc_passed_qty !== null ? item.qc_passed_qty : '-'}</td>
                          <td className="px-4 py-3 text-right font-bold text-danger-600">{item.qc_failed_qty !== null ? item.qc_failed_qty : '-'}</td>
                          <td className="px-4 py-3">
                            {item.qc_result ? (
                              <div className="flex flex-col gap-0.5">
                                <div className="flex gap-1.5 items-center">
                                  <span className={`text-[9px] font-bold ${item.qc_result === 'PASSED' ? 'text-success-700' : item.qc_result === 'FAILED' ? 'text-danger-700' : 'text-warning-700'}`}>
                                    {item.qc_result}
                                  </span>
                                </div>
                                {item.qc_failure_reason && (
                                  <span className="text-[10px] text-danger-600 italic block">{item.qc_failure_reason}</span>
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
                <div className="bg-canvas-light p-4 border border-hairline-light rounded-lg shadow-level-3">
                  {isRejecting ? (
                    <div className="flex flex-col gap-2">
                      <label className="text-xs font-bold text-danger-700 flex items-center gap-1.5">
                        <AlertTriangle className="w-3.5 h-3.5" />
                        Lý do từ chối phê duyệt (Bắt buộc)
                      </label>
                      <textarea
                        value={rejectionReason}
                        onChange={(e) => setRejectionReason(e.target.value)}
                        placeholder="Nhập lý do chi tiết từ chối phiếu nhập này..."
                        className="text-input text-xs h-20 resize-none border-danger-300 focus:border-danger-500 focus:ring-danger-100"
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
            <div className="p-4 border-t border-hairline-light bg-canvas-cream flex justify-between gap-3">
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
                        className="btn-pill bg-danger-600 hover:bg-danger-700 text-white text-xs py-1.5 px-4 font-bold disabled:opacity-50"
                      >
                        {submittingApproval ? 'Đang từ chối...' : 'Xác nhận từ chối'}
                      </button>
                    </>
                  ) : (
                    <>
                      <button
                        onClick={() => setIsRejecting(true)}
                        className="btn-pill btn-pill-outline-light border-danger-500 hover:bg-danger-50 text-danger-600 text-xs py-1.5 px-4"
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
