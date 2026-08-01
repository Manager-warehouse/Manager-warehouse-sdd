import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "../../stores/auth.store";
import { useUiStore } from "../../stores/ui.store";
import { inboundService } from "../../services/inbound.service";
import { masterDataService } from "../../services/masterData.service";
import { ROLES } from "../../utils/constants";
import {
  Plus,
  Search,
  FileText,
  CheckCircle2,
  AlertTriangle,
  Eye,
  Check,
  X,
  Loader2,
} from "lucide-react";
import Input from "../../components/common/Input";
import Badge from "../../components/common/Badge";
import Button from "../../components/common/Button";

const ReceiptList = () => {
  const navigate = useNavigate();
  const activeWarehouse = useAuthStore((state) => state.activeWarehouse);
  const { user, hasRole } = useAuthStore();
  const { addToast } = useUiStore();

  const [receipts, setReceipts] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [dealers, setDealers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");

  // Approval Modal State
  const [showApprovalModal, setShowApprovalModal] = useState(false);
  const [selectedReceipt, setSelectedReceipt] = useState(null);
  const [approvalNotes, setApprovalNotes] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");
  const [submittingApproval, setSubmittingApproval] = useState(false);
  const [isRejecting, setIsRejecting] = useState(false);

  // Edit Count Modal State
  const [showEditModal, setShowEditModal] = useState(false);
  const [editItems, setEditItems] = useState([]);
  const [showWarningModal, setShowWarningModal] = useState(false);
  const [submittingEditCount, setSubmittingEditCount] = useState(false);

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
      addToast("Lỗi khi tải dữ liệu phiếu nhập kho", "error");
    } finally {
      setLoading(false);
    }
  };

  const isPutawayCompleted = (receipt) => {
    if (!receipt) return false;
    if (receipt.status === "PUTAWAY_COMPLETED") return true;
    if (receipt.putaway_completed_at || receipt.putawayCompletedAt) return true;
    return false;
  };

  const getPartnerName = (receipt) => {
    if (receipt.type === "PURCHASE") {
      const supplier = suppliers.find((s) => s.id === receipt.supplier_id);
      return supplier
        ? supplier.company_name
        : `NCC ID: ${receipt.supplier_id}`;
    } else {
      const dealer = dealers.find((d) => d.id === receipt.dealer_id);
      return dealer ? dealer.name : `Đại lý ID: ${receipt.dealer_id}`;
    }
  };

  const isAwaitingPreReceiveApproval = (receipt) => {
    if (!receipt) return false;
    if (receipt.status === "PENDING_MANAGER_APPROVAL") return true;
    if (receipt.status !== "PENDING_RECEIPT" || receipt.type !== "PURCHASE") {
      return false;
    }
    if (receipt.pre_receive_approved_at || receipt.preReceiveApprovedAt) {
      return false;
    }
    const items = receipt.items || [];
    return (
      items.length === 0 ||
      items.every((item) => item.actual_qty == null && item.actualQty == null)
    );
  };

  // Filter & Search logic
  const filteredReceipts = receipts.filter((receipt) => {
    const needle = searchTerm.toLowerCase();
    const partnerName = getPartnerName(receipt);
    const matchesSearch =
      (receipt.receipt_number || "").toLowerCase().includes(needle) ||
      (partnerName || "").toLowerCase().includes(needle);
    const matchesStatus =
      statusFilter === "ALL" ||
      receipt.status === statusFilter ||
      (statusFilter === "PENDING_MANAGER_APPROVAL" &&
        isAwaitingPreReceiveApproval(receipt));
    return matchesSearch && matchesStatus;
  });
  const pendingManagerApprovalReceipts = receipts.filter((receipt) =>
    isAwaitingPreReceiveApproval(receipt),
  );
  const canPreReceiveApprove = hasRole(ROLES.WAREHOUSE_MANAGER);
  const canStorekeeperReview = (receipt) =>
    receipt?.status === "PENDING_STOREKEEPER_REVIEW" &&
    (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN));

  const hasReceiptPrimaryActions = (receipt) => {
    if (!receipt) return false;
    if (isAwaitingPreReceiveApproval(receipt) && canPreReceiveApprove)
      return true;
    if (receipt.status === "REVISION_REQUIRED" && hasRole(ROLES.PLANNER)) {
      return true;
    }
    if (
      receipt.status === "PENDING_RECEIPT" &&
      !isAwaitingPreReceiveApproval(receipt) &&
      (hasRole(ROLES.WAREHOUSE_STAFF) || hasRole(ROLES.ADMIN))
    ) {
      return true;
    }
    if (
      (receipt.status === "DRAFT" || receipt.status === "RECOUNT_REQUIRED") &&
      (hasRole(ROLES.WAREHOUSE_STAFF) || hasRole(ROLES.ADMIN))
    ) {
      return true;
    }
    if (canStorekeeperReview(receipt)) {
      return true;
    }
    if (
      (receipt.status === "QC_COMPLETED" || receipt.status === "QC_FAILED") &&
      (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN))
    ) {
      return true;
    }
    if (
      receipt.status === "RETURN_TO_SUPPLIER_PENDING" &&
      (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN))
    ) {
      return true;
    }
    if (
      (receipt.status === "APPROVED" ||
        receipt.status === "PARTIALLY_APPROVED") &&
      !isPutawayCompleted(receipt) &&
      (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN))
    ) {
      return true;
    }
    if (
      (receipt.status === "PENDING_MANAGER_APPROVAL" ||
        receipt.status === "REVISION_REQUIRED" ||
        receipt.status === "PENDING_RECEIPT" ||
        receipt.status === "DRAFT") &&
      (hasRole(ROLES.PLANNER) ||
        hasRole(ROLES.WAREHOUSE_MANAGER) ||
        hasRole(ROLES.ADMIN))
    ) {
      return true;
    }
    return (
      (((receipt.status === "APPROVED" ||
        receipt.status === "PARTIALLY_APPROVED") &&
        !isPutawayCompleted(receipt)) ||
        receipt.status === "RETURN_TO_SUPPLIER_PENDING") &&
      (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN))
    );
  };

  const hasTableActions = filteredReceipts.some(hasReceiptPrimaryActions);

  const getStatusBadge = (receipt) => {
    if (!receipt) return null;
    if (isAwaitingPreReceiveApproval(receipt)) {
      return (
        <Badge
          size="sm"
          colorClassName="bg-warning-50 text-warning-800 border-warning-300"
        >
          Chờ Quản Lý Duyệt
        </Badge>
      );
    }
    if (isPutawayCompleted(receipt)) {
      if (receipt.status === "PARTIALLY_APPROVED") {
        return (
          <Badge
            size="sm"
            colorClassName="bg-warning-50 text-warning-800 border-warning-300"
          >
            Đã nhập một phần
          </Badge>
        );
      }
      return (
        <Badge
          size="sm"
          colorClassName="bg-success-100 text-success-800 border-success-300"
        >
          Đã nhập kho
        </Badge>
      );
    }
    switch (receipt.status) {
      case "PENDING_MANAGER_APPROVAL":
        return (
          <Badge
            size="sm"
            colorClassName="bg-warning-50 text-warning-800 border-warning-300"
          >
            Chờ Quản Lý Duyệt
          </Badge>
        );
      case "REVISION_REQUIRED":
        return (
          <Badge
            size="sm"
            colorClassName="bg-danger-50 text-danger-700 border-danger-200"
          >
            Can chinh sua
          </Badge>
        );
      case "PENDING_RECEIPT":
        return (
          <Badge
            size="sm"
            colorClassName="bg-canvas-cream text-shade-70 border-hairline-light"
          >
            Chờ nhận
          </Badge>
        );
      case "PENDING_STOREKEEPER_REVIEW":
        return (
          <Badge
            size="sm"
            colorClassName="bg-warning-50 text-warning-800 border-warning-300"
          >
            Cho thu kho duyet
          </Badge>
        );
      case "RECOUNT_REQUIRED":
        return (
          <Badge
            size="sm"
            colorClassName="bg-danger-50 text-danger-700 border-danger-200"
          >
            Can dem lai
          </Badge>
        );
      case "DRAFT":
        return (
          <Badge
            size="sm"
            colorClassName="bg-info-50 text-info-700 border-info-200"
          >
            Đã đếm (nháp)
          </Badge>
        );
      case "QC_COMPLETED":
        return (
          <Badge
            size="sm"
            colorClassName="bg-warning-50 text-warning-700 border-warning-200"
          >
            Đã QC
          </Badge>
        );
      case "APPROVED":
        return (
          <Badge
            size="sm"
            colorClassName="bg-info-50 text-info-700 border-info-200"
          >
            Chờ cất hàng
          </Badge>
        );
      case "PARTIALLY_APPROVED":
        return (
          <Badge
            size="sm"
            colorClassName="bg-warning-50 text-warning-800 border-warning-300"
          >
            Chờ cất phần duyệt
          </Badge>
        );
      case "QC_FAILED":
        return (
          <Badge
            size="sm"
            colorClassName="bg-danger-50 text-danger-700 border-danger-200"
          >
            QC có hàng lỗi
          </Badge>
        );
      case "REJECTED":
        return (
          <Badge
            size="sm"
            colorClassName="bg-danger-50 text-danger-700 border-danger-200"
          >
            Từ chối
          </Badge>
        );
      case "RETURN_TO_SUPPLIER_PENDING":
        return (
          <Badge
            size="sm"
            colorClassName="bg-danger-100 text-danger-800 border-danger-300"
          >
            Chờ trả NCC (Đơn bị từ chối)
          </Badge>
        );
      case "RETURNED_TO_SUPPLIER":
        return (
          <Badge
            size="sm"
            colorClassName="bg-shade-20 text-shade-80 border-shade-40"
          >
            Đã trả NCC
          </Badge>
        );
      case "CANCELLED":
        return (
          <Badge
            size="sm"
            colorClassName="bg-shade-20 text-shade-60 border-shade-40"
          >
            Đã hủy
          </Badge>
        );
      case "IN_TRANSIT":
        return (
          <Badge
            size="sm"
            colorClassName="bg-warning-50 text-warning-700 border-warning-200"
          >
            Chờ nhận nội bộ
          </Badge>
        );
      case "COMPLETED":
        return (
          <Badge
            size="sm"
            colorClassName="bg-aloe-10 text-success-900 border-success-300"
          >
            Đã nhập kho
          </Badge>
        );
      case "COMPLETED_WITH_DISCREPANCY":
        return (
          <Badge
            size="sm"
            colorClassName="bg-warning-50 text-warning-700 border-warning-200"
          >
            Đã nhập có lệch
          </Badge>
        );
      default:
        return (
          <Badge
            size="sm"
            colorClassName="bg-canvas-cream text-shade-70 border-hairline-light"
          >
            {receipt.status}
          </Badge>
        );
    }
  };

  // Approval Handlers
  const handleOpenApproval = async (receiptId) => {
    try {
      const detail = await inboundService.getReceiptById(receiptId);
      setSelectedReceipt(detail);
      setApprovalNotes("");
      setRejectionReason("");
      setIsRejecting(false);
      setShowApprovalModal(true);
    } catch (error) {
      addToast("Không thể lấy chi tiết phiếu nhập", "error");
    }
  };

  const handleCancelReceipt = async (receipt) => {
    const reason = window.prompt(
      `Nhập lý do hủy phiếu ${receipt.receipt_number}:`,
    );
    if (reason === null) return;
    try {
      await inboundService.cancelReceipt(
        receipt.id,
        reason,
        receipt.version || 0,
      );
      addToast(`Đã hủy phiếu nhập kho ${receipt.receipt_number}`, "success");
      setShowApprovalModal(false);
      fetchData();
    } catch (error) {
      const msg =
        error.response?.data?.message ||
        error.message ||
        "Lỗi khi hủy phiếu nhập kho";
      addToast(msg, "error");
    }
  };

  const handleReopenReceipt = async (receipt) => {
    if (
      !window.confirm(
        `Lưu ý: Reopen phiếu ${receipt.receipt_number} sẽ xóa kết quả QC cũ và đưa phiếu về DRAFT để thực hiện lại. Bạn có chắc chắn?`,
      )
    )
      return;
    try {
      await inboundService.reopenReceipt(
        receipt.id,
        "Reopen phiếu về DRAFT",
        receipt.version || 0,
      );
      addToast(
        `Đã reopen phiếu ${receipt.receipt_number} về trạng thái DRAFT`,
        "success",
      );
      setShowApprovalModal(false);
      fetchData();
    } catch (error) {
      const msg =
        error.response?.data?.message ||
        error.message ||
        "Lỗi khi reopen phiếu nhập kho";
      addToast(msg, "error");
    }
  };

  const submitApprove = async () => {
    setSubmittingApproval(true);
    try {
      if (isAwaitingPreReceiveApproval(selectedReceipt)) {
        await inboundService.decidePreReceiveApproval(
          selectedReceipt.id,
          "APPROVE",
          "",
          selectedReceipt.version || 0,
        );
      } else {
        await inboundService.approveReceipt(
          selectedReceipt.id,
          approvalNotes,
          selectedReceipt.version,
        );
      }
      addToast(
        `Đã phê duyệt phiếu nhập ${selectedReceipt.receipt_number} thành công`,
        "success",
      );
      setShowApprovalModal(false);
      fetchData();
    } catch (error) {
      addToast(
        error.message === "RECEIPT_ALREADY_APPROVED"
          ? "Phiếu này đã được duyệt trước đó"
          : "Lỗi phê duyệt",
        "error",
      );
    } finally {
      setSubmittingApproval(false);
    }
  };

  const handleConfirmQc = async (receipt) => {
    try {
      await inboundService.qcReceipt(receipt.id, {
        action: "CONFIRM",
        expectedVersion: receipt.version || 0,
      });
      addToast(`Đã xác nhận QC cho phiếu ${receipt.receipt_number}`, "success");
      fetchData();
    } catch (error) {
      const serverMessage =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message;
      const message =
        serverMessage === "QC_NOT_YET_SUBMITTED"
          ? "Chưa có kết quả QC để xác nhận"
          : serverMessage;
      addToast(message || "Lỗi xác nhận QC", "error");
    }
  };

  const submitReject = async () => {
    if (!rejectionReason.trim()) {
      addToast("Vui lòng nhập lý do từ chối", "warning");
      return;
    }
    setSubmittingApproval(true);
    try {
      if (isAwaitingPreReceiveApproval(selectedReceipt)) {
        await inboundService.decidePreReceiveApproval(
          selectedReceipt.id,
          "REJECT",
          rejectionReason,
          selectedReceipt.version || 0,
        );
      } else {
        await inboundService.rejectReceipt(
          selectedReceipt.id,
          rejectionReason,
          selectedReceipt.version,
        );
      }
      addToast(
        `Đã từ chối phiếu nhập ${selectedReceipt.receipt_number}`,
        "info",
      );
      setShowApprovalModal(false);
      fetchData();
    } catch (error) {
      addToast("Lỗi từ chối phê duyệt", "error");
    } finally {
      setSubmittingApproval(false);
    }
  };

  const getProductName = (item) => {
    if (item && (item.product_name || item.productName)) {
      return item.product_name || item.productName;
    }
    const productId = typeof item === "object" ? item.product_id : item;
    return productId === 1
      ? "Màn hình ASUS ProArt 27K"
      : "Chuột Logitech MX Master 3S";
  };

  const getProductSku = (item) => {
    if (item && item.product_sku) return item.product_sku;
    const productId = typeof item === "object" ? item.product_id : item;
    return productId === 1 ? "SKU-PA-001" : "SKU-LOGI-MX3";
  };

  const getExpectedQty = (item) =>
    Number(item?.expected_qty ?? item?.expectedQty ?? 0);

  const formatQty = (qty) =>
    Number(qty || 0).toLocaleString("vi-VN", { maximumFractionDigits: 2 });

  const getReceiptItems = (receipt) => receipt?.items || [];

  const getReceiptExpectedQty = (receipt) =>
    getReceiptItems(receipt).reduce(
      (sum, item) => sum + getExpectedQty(item),
      0,
    );

  const renderProductSummary = (receipt) => {
    const items = getReceiptItems(receipt);
    if (items.length === 0) {
      return <span className="text-shade-40 italic">Chưa có dòng hàng</span>;
    }

    return (
      <div className="flex flex-col gap-1">
        {items.slice(0, 2).map((item, index) => (
          <div
            key={item.receipt_item_id || item.id || `${receipt.id}-${index}`}
            className="min-w-0"
          >
            <span className="block truncate font-semibold text-ink">
              {getProductName(item)}
            </span>
            <span className="block text-[11px] text-shade-50">
              {getProductSku(item)}
            </span>
          </div>
        ))}
        {items.length > 2 && (
          <span className="text-[11px] font-semibold text-shade-50">
            +{items.length - 2} sản phẩm khác
          </span>
        )}
      </div>
    );
  };

  const openDetail = async (receipt) => {
    try {
      const detail = await inboundService.getReceiptById(receipt.id);
      setSelectedReceipt(detail);
      setIsRejecting(false);
      setApprovalNotes("");
      setShowApprovalModal(true);
    } catch (e) {
      addToast("Lỗi xem chi tiết", "error");
    }
  };

  const handleConfirmReturnToSupplier = async (receipt) => {
    try {
      await inboundService.confirmReturnToSupplier(
        receipt.id,
        "Xác nhận bàn giao trả hàng cho xe NCC",
        receipt.version || 0,
      );
      addToast(
        `Đã xác nhận trả toàn bộ hàng cho Nhà cung cấp (${receipt.receipt_number})`,
        "success",
      );
      setShowApprovalModal(false);
      fetchData();
    } catch (error) {
      addToast("Lỗi xác nhận trả hàng cho Nhà cung cấp", "error");
    }
  };

  const handleOpenEditCount = async (receipt) => {
    try {
      const detail = await inboundService.getReceiptById(receipt.id);
      setSelectedReceipt(detail);
      const itemsToEdit = (detail.items || []).map((item) => ({
        receipt_item_id: item.id || item.receipt_item_id,
        product_name: getProductName(item),
        product_sku: getProductSku(item),
        expected_qty: item.expected_qty,
        counted_qty:
          item.actual_qty !== null && item.actual_qty !== undefined
            ? item.actual_qty
            : item.expected_qty,
      }));
      setEditItems(itemsToEdit);
      setShowApprovalModal(false);
      setShowEditModal(true);
    } catch (e) {
      addToast("Không thể lấy chi tiết phiếu để sửa số lượng đếm", "error");
    }
  };

  const handleEditCountChange = (itemId, newQty) => {
    const parsed = Math.max(0, parseInt(newQty, 10) || 0);
    setEditItems((prev) =>
      prev.map((item) =>
        item.receipt_item_id === itemId
          ? { ...item, counted_qty: parsed }
          : item,
      ),
    );
  };

  const handleSaveEditCountClick = () => {
    if (!selectedReceipt) return;
    const hasExistingQc =
      (selectedReceipt.items || []).some(
        (item) =>
          item.qc_result !== null &&
          item.qc_result !== undefined &&
          item.qc_result !== "PENDING",
      ) ||
      selectedReceipt.status === "QC_COMPLETED" ||
      selectedReceipt.status === "QC_FAILED";

    if (hasExistingQc) {
      setShowWarningModal(true);
    } else {
      executeSaveEditCount();
    }
  };

  const executeSaveEditCount = async () => {
    if (!selectedReceipt) return;
    setSubmittingEditCount(true);
    try {
      const payload = {
        expected_version: selectedReceipt.version || 0,
        items: editItems.map((item) => ({
          receipt_item_id: item.receipt_item_id,
          counted_qty: item.counted_qty,
        })),
      };
      await inboundService.receiveReceipt(selectedReceipt.id, payload);
      addToast(
        `Đã cập nhật số lượng đếm thực tế cho phiếu ${selectedReceipt.receipt_number}`,
        "success",
      );
      setShowWarningModal(false);
      setShowEditModal(false);
      fetchData();
    } catch (error) {
      const msg =
        error.response?.data?.message ||
        error.message ||
        "Lỗi khi cập nhật số lượng đếm";
      addToast(msg, "error");
    } finally {
      setSubmittingEditCount(false);
    }
  };

  const hasQcInspected = (receipt) => {
    if (!receipt) return false;
    return receipt.status === "QC_COMPLETED" || receipt.status === "QC_FAILED";
  };

  const handleStorekeeperReview = async (receipt, decision) => {
    try {
      const reason =
        decision === "REQUEST_RECOUNT"
          ? window.prompt("Nhap ly do yeu cau dem lai") || ""
          : "";
      if (decision === "REQUEST_RECOUNT" && !reason.trim()) {
        addToast("Can nhap ly do dem lai", "error");
        return;
      }
      const updatedReceipt = await inboundService.reviewStorekeeperCountQc(
        receipt.id,
        {
          decision,
          reason,
          expectedVersion: receipt.version || 0,
        },
      );
      if (selectedReceipt?.id === receipt.id) {
        setSelectedReceipt(updatedReceipt);
      }
      addToast(
        decision === "APPROVE" ? "Da duyet kiem dem/QC" : "Da yeu cau dem lai",
        "success",
      );
      fetchData();
    } catch (error) {
      const serverMessage =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.message;
      addToast(serverMessage || "Loi duyet kiem dem/QC", "error");
    }
  };

  const renderReceiptActions = (receipt, includeDetail = true) => (
    <>
      {isAwaitingPreReceiveApproval(receipt) && canPreReceiveApprove && (
        <button
          aria-label="pre-receive-approval"
          onClick={() => handleOpenApproval(receipt.id)}
          className="inline-flex items-center justify-center rounded-full bg-ink text-onPrimary hover:bg-shade-70 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
        >
          Duyệt Kế Hoạch Nhập Kho
        </button>
      )}

      {receipt.status === "REVISION_REQUIRED" && hasRole(ROLES.PLANNER) && (
        <button
          aria-label="revise-receipt"
          onClick={() => navigate(`/inbound/receipts/${receipt.id}/revision`)}
          className="inline-flex items-center justify-center rounded-full bg-ink text-onPrimary hover:bg-shade-70 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
        >
          Chỉnh sửa
        </button>
      )}

      {receipt.status === "PENDING_RECEIPT" &&
        !isAwaitingPreReceiveApproval(receipt) &&
        (hasRole(ROLES.WAREHOUSE_STAFF) || hasRole(ROLES.ADMIN)) && (
          <button
            aria-label="receive-receipt"
            onClick={() => navigate(`/inbound/receive/${receipt.id}`)}
            className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light text-ink hover:bg-canvas-cream px-3 py-1 text-[0px] font-semibold whitespace-nowrap transition-colors duration-150"
          >
            <span className="text-xs">Nhận hàng & QC</span>
            Đếm số lượng
          </button>
        )}

      {(receipt.status === "DRAFT" || receipt.status === "RECOUNT_REQUIRED") &&
        (hasRole(ROLES.WAREHOUSE_STAFF) || hasRole(ROLES.ADMIN)) && (
          <button
            aria-label="receive-qc-receipt"
            onClick={() => navigate(`/inbound/receive/${receipt.id}`)}
            className="inline-flex items-center justify-center rounded-full border border-ink bg-canvas-light text-ink hover:bg-canvas-cream px-3 py-1 text-[0px] font-semibold whitespace-nowrap transition-colors duration-150"
          >
            <span className="text-xs">Nhận hàng & QC</span>
            Đếm số lượng
          </button>
        )}

      {canStorekeeperReview(receipt) && (
        <>
          <button
            aria-label="approve-storekeeper-review"
            onClick={() => handleStorekeeperReview(receipt, "APPROVE")}
            className="inline-flex items-center justify-center rounded-full bg-aloe-10 text-success-950 border border-success-300 hover:bg-success-100 px-3 py-1 text-xs font-bold whitespace-nowrap transition-colors duration-150"
          >
            Duyet kiem dem
          </button>
          <button
            aria-label="request-recount"
            onClick={() => handleStorekeeperReview(receipt, "REQUEST_RECOUNT")}
            className="inline-flex items-center justify-center rounded-full border border-danger-300 bg-danger-50 text-danger-700 hover:bg-danger-100 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
          >
            Bat dem lai
          </button>
        </>
      )}

      {false &&
        (receipt.status === "DRAFT" ||
          receipt.status === "QC_COMPLETED" ||
          receipt.status === "QC_FAILED") &&
        (hasRole(ROLES.WAREHOUSE_STAFF) ||
          hasRole(ROLES.STOREKEEPER) ||
          hasRole(ROLES.WAREHOUSE_MANAGER) ||
          hasRole(ROLES.ADMIN)) && (
          <button
            aria-label="inspect-receipt-qc"
            onClick={() => navigate(`/inbound/qc/${receipt.id}`)}
            className={`inline-flex items-center justify-center rounded-full border px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150 ${
              hasQcInspected(receipt)
                ? "border-success-300 bg-success-50 text-success-800 hover:bg-success-100"
                : "border-ink bg-canvas-light text-ink hover:bg-canvas-cream"
            }`}
          >
            {hasQcInspected(receipt) ? "Đã Kiểm QC" : "Kiểm QC"}
          </button>
        )}

      {false &&
        receipt.status === "DRAFT" &&
        (hasRole(ROLES.STOREKEEPER) ||
          hasRole(ROLES.WAREHOUSE_MANAGER) ||
          hasRole(ROLES.ADMIN)) && (
          <button
            aria-label="confirm-receipt-qc"
            onClick={() => handleConfirmQc(receipt)}
            className="inline-flex items-center justify-center rounded-full bg-ink text-onPrimary hover:bg-shade-70 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
          >
            Xác nhận QC
          </button>
        )}

      {(receipt.status === "QC_COMPLETED" || receipt.status === "QC_FAILED") &&
        (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) && (
          <button
            aria-label="approve-receipt"
            onClick={() => handleOpenApproval(receipt.id)}
            className="inline-flex items-center justify-center rounded-full bg-aloe-10 text-success-950 border border-success-300 hover:bg-success-100 px-3 py-1 text-xs font-bold whitespace-nowrap transition-colors duration-150"
          >
            Duyệt phiếu
          </button>
        )}

      {receipt.status === "RETURN_TO_SUPPLIER_PENDING" &&
        (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) && (
          <button
            aria-label="confirm-receipt-return"
            onClick={() => handleConfirmReturnToSupplier(receipt)}
            className="inline-flex items-center justify-center rounded-full bg-danger-600 text-white hover:bg-danger-700 px-3 py-1 text-xs font-bold whitespace-nowrap transition-colors duration-150 shadow-sm"
          >
            Xác nhận trả NCC
          </button>
        )}

      {(receipt.status === "APPROVED" ||
        receipt.status === "PARTIALLY_APPROVED") &&
        !isPutawayCompleted(receipt) &&
        (hasRole(ROLES.STOREKEEPER) || hasRole(ROLES.ADMIN)) && (
          <button
            aria-label="putaway-receipt"
            onClick={() => navigate(`/inbound/putaway/${receipt.id}`)}
            className="inline-flex items-center justify-center rounded-full bg-ink text-onPrimary hover:bg-shade-70 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
          >
            Cất hàng
          </button>
        )}

      {(receipt.status === "PENDING_MANAGER_APPROVAL" ||
        receipt.status === "REVISION_REQUIRED" ||
        receipt.status === "PENDING_RECEIPT" ||
        receipt.status === "DRAFT") &&
        (hasRole(ROLES.PLANNER) ||
          hasRole(ROLES.WAREHOUSE_MANAGER) ||
          hasRole(ROLES.ADMIN)) && (
          <button
            aria-label="cancel-receipt"
            onClick={() => handleCancelReceipt(receipt)}
            className="inline-flex items-center justify-center rounded-full border border-danger-300 bg-danger-50 text-danger-700 hover:bg-danger-100 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
          >
            Hủy phiếu
          </button>
        )}

      {(((receipt.status === "APPROVED" ||
        receipt.status === "PARTIALLY_APPROVED") &&
        !isPutawayCompleted(receipt)) ||
        receipt.status === "RETURN_TO_SUPPLIER_PENDING") &&
        (hasRole(ROLES.WAREHOUSE_MANAGER) || hasRole(ROLES.ADMIN)) && (
          <button
            aria-label="reopen-receipt"
            onClick={() => handleReopenReceipt(receipt)}
            className="inline-flex items-center justify-center rounded-full border border-warning-300 bg-warning-50 text-warning-800 hover:bg-warning-100 px-3 py-1 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
          >
            Reopen
          </button>
        )}

      {includeDetail && (
        <button
          onClick={() => openDetail(receipt)}
          className="p-1.5 hover:bg-canvas-cream rounded-full text-shade-50 hover:text-ink transition-colors flex items-center justify-center"
          title="Xem chi tiết"
        >
          <Eye className="w-4 h-4" />
        </button>
      )}
    </>
  );

  return (
    <div className="mobile-page">
      {/* Header section */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Vận hành / Nhập kho
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Nhập hàng & Kiểm định
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản lý nhập mua, nhập trả và kiểm định tại kho{" "}
            <span className="font-semibold text-ink">
              {activeWarehouse?.name} ({activeWarehouse?.code})
            </span>
            . Mã phiếu nhập thuộc luồng nhập kho; điều chuyển nội bộ được xử lý
            ở màn Điều chuyển nội bộ riêng.
          </p>
        </div>

        {(hasRole(ROLES.PLANNER) || hasRole(ROLES.ADMIN)) && (
          <Button
            aria-label="create-receipt"
            onClick={() => navigate("/inbound/create")}
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
            placeholder="Tìm mã phiếu, đối tác..."
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
                { value: "ALL", label: "Tất cả trạng thái" },
                {
                  value: "PENDING_MANAGER_APPROVAL",
                  label: "Chờ quản lý duyệt",
                },
                { value: "REVISION_REQUIRED", label: "Cần chỉnh sửa" },
                { value: "PENDING_RECEIPT", label: "Chờ nhận" },
                {
                  value: "PENDING_STOREKEEPER_REVIEW",
                  label: "Cho thu kho duyet",
                },
                { value: "RECOUNT_REQUIRED", label: "Can dem lai" },
                { value: "DRAFT", label: "Đã đếm (Nháp)" },
                { value: "QC_COMPLETED", label: "Đã QC" },
                { value: "QC_FAILED", label: "QC có hàng lỗi" },
                { value: "APPROVED", label: "Chờ cất hàng" },
                {
                  value: "PARTIALLY_APPROVED",
                  label: "Chờ cất phần duyệt",
                },
                { value: "PUTAWAY_COMPLETED", label: "Đã nhập kho" },
                { value: "REJECTED", label: "Từ chối" },
                {
                  value: "RETURN_TO_SUPPLIER_PENDING",
                  label: "Chờ trả NCC (Bị từ chối)",
                },
                { value: "RETURNED_TO_SUPPLIER", label: "Đã trả NCC" },
              ]}
            />
          </div>
        </div>
      </div>

      {canPreReceiveApprove && pendingManagerApprovalReceipts.length > 0 && (
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3 rounded-lg border border-warning-300 bg-warning-50 px-4 py-3 mb-2 md:mb-6">
          <div>
            <p className="text-sm font-bold text-warning-900">
              Phiếu quản lý kho duyệt
            </p>
            <p className="text-xs text-warning-800">
              {pendingManagerApprovalReceipts.length} phiếu cần duyệt trước khi
              nhận hàng.
            </p>
          </div>
          <button
            type="button"
            onClick={() => setStatusFilter("PENDING_MANAGER_APPROVAL")}
            className="inline-flex items-center justify-center rounded-full bg-ink text-onPrimary hover:bg-shade-70 px-4 py-2 text-xs font-semibold whitespace-nowrap transition-colors duration-150"
          >
            Xem phiếu cần duyệt
          </button>
        </div>
      )}

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
              <h3 className="text-lg font-bold mb-1">
                Không tìm thấy phiếu nhập kho nào
              </h3>
              <p className="text-sm text-shade-50">
                Thử đổi bộ lọc để xem phiếu nhập mua hoặc phiếu nhập trả.
              </p>
            </div>
          ) : (
            <>
              {/* Desktop/tablet: table view */}
              <div className="hidden md:block bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
                <div className="overflow-x-auto">
                  <table className="data-table-grid w-full table-fixed text-left border-collapse">
                    <thead>
                      <tr className="bg-canvas-cream border-b border-hairline-light">
                        <th
                          className={`px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 ${hasTableActions ? "w-[12%]" : "w-[13%]"}`}
                        >
                          Mã phiếu
                        </th>
                        <th
                          className={`px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 ${hasTableActions ? "w-[21%]" : "w-[24%]"}`}
                        >
                          Đối tác
                        </th>
                        <th
                          className={`pl-4 pr-2 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 ${hasTableActions ? "w-[18%]" : "w-[21%]"}`}
                        >
                          Sản phẩm nhập
                        </th>
                        <th
                          className={`pl-1 pr-3 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right ${hasTableActions ? "w-[6%]" : "w-[7%]"}`}
                        >
                          SL dự kiến
                        </th>
                        <th
                          className={`px-3 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 ${hasTableActions ? "w-[8%]" : "w-[9%]"}`}
                        >
                          Ngày Nhập Hàng
                        </th>
                        <th
                          className={`px-3 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center ${hasTableActions ? "w-[17%]" : "w-[23%]"}`}
                        >
                          Trạng thái
                        </th>
                        {hasTableActions && (
                          <th className="px-3 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center w-[15%]">
                            Hành động
                          </th>
                        )}
                        <th className="px-2 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center w-[3%]"></th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {filteredReceipts.map((receipt) => (
                        <tr
                          key={receipt.id}
                          className="hover:bg-canvas-cream/50 transition-colors"
                        >
                          <td className="px-4 py-4 text-xs font-bold break-words">
                            {receipt.receipt_number}
                          </td>
                          <td className="px-4 py-4 text-xs font-semibold leading-snug">
                            <span className="line-clamp-2">
                              {getPartnerName(receipt)}
                            </span>
                          </td>
                          <td className="pl-4 pr-2 py-4 text-xs">
                            {renderProductSummary(receipt)}
                          </td>
                          <td className="pl-1 pr-3 py-4 text-xs text-right font-bold text-ink">
                            {formatQty(getReceiptExpectedQty(receipt))}
                          </td>
                          <td className="px-3 py-4 text-xs text-shade-50 whitespace-nowrap">
                            {receipt.document_date}
                          </td>
                          <td className="px-3 py-4 text-center">
                            {getStatusBadge(receipt)}
                          </td>
                          {hasTableActions && (
                            <td className="px-3 py-4 text-center">
                              <div className="flex flex-wrap gap-2 justify-center items-center">
                                {renderReceiptActions(receipt, false)}
                              </div>
                            </td>
                          )}
                          <td className="px-2 py-4 text-center">
                            <button
                              onClick={() => openDetail(receipt)}
                              className="mx-auto p-1.5 hover:bg-canvas-cream rounded-full text-shade-50 hover:text-ink transition-colors flex items-center justify-center"
                              title="Xem chi tiết"
                            >
                              <Eye className="w-4 h-4" />
                            </button>
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
                  <div
                    key={receipt.id}
                    className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden"
                  >
                    <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                      <span className="text-xs font-bold text-ink">
                        {receipt.receipt_number}
                      </span>
                      {getStatusBadge(receipt)}
                    </div>
                    <div className="p-4 flex flex-col gap-2 text-xs">
                      <p className="text-shade-50">
                        Đối tác:{" "}
                        <span className="font-semibold text-ink">
                          {getPartnerName(receipt)}
                        </span>
                      </p>
                      <div className="text-shade-50">
                        <span className="block mb-1">Sản phẩm nhập:</span>
                        {renderProductSummary(receipt)}
                      </div>
                      <p className="text-shade-50">
                        SL dự kiến:{" "}
                        <span className="font-semibold text-ink">
                          {formatQty(getReceiptExpectedQty(receipt))}
                        </span>
                      </p>
                      <p className="text-shade-50">
                        Ngày Nhập Hàng:{" "}
                        <span className="font-semibold text-ink">
                          {receipt.document_date}
                        </span>
                      </p>
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
                <span className="text-[10px] font-bold text-shade-40 uppercase tracking-widest block mb-1">
                  Chi tiết phiếu
                </span>
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
                  <span className="text-shade-50 block mb-0.5">Đối tác:</span>
                  <span className="font-bold">
                    {getPartnerName(selectedReceipt)}
                  </span>
                </div>
                <div>
                  <span className="text-shade-50 block mb-0.5">
                    Ngày Nhập Hàng:
                  </span>
                  <span className="font-bold">
                    {selectedReceipt.document_date}
                  </span>
                </div>
                {selectedReceipt.approved_at && (
                  <div className="col-span-2 bg-success-50 border border-success-200 text-success-950 p-2.5 rounded-md flex gap-2 items-center">
                    <CheckCircle2 className="w-4 h-4 text-success-600 flex-shrink-0" />
                    <span>
                      Phiếu đã được duyệt bởi Trưởng kho lúc{" "}
                      {new Date(selectedReceipt.approved_at).toLocaleString(
                        "vi-VN",
                      )}
                    </span>
                  </div>
                )}
                {selectedReceipt.rejection_reason && (
                  <div className="col-span-2 bg-danger-50 border border-danger-200 text-danger-950 p-2.5 rounded-md flex gap-2 items-center">
                    <AlertTriangle className="w-4 h-4 text-danger-600 flex-shrink-0" />
                    <span>
                      Bị từ chối duyệt. Lý do:{" "}
                      <strong className="font-semibold">
                        {selectedReceipt.rejection_reason}
                      </strong>
                    </span>
                  </div>
                )}
              </div>

              {/* Items List */}
              <div>
                <h4 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3">
                  {isAwaitingPreReceiveApproval(selectedReceipt)
                    ? "Danh sách sản phẩm nhập"
                    : "Danh sách sản phẩm kiểm định"}
                </h4>
                <div className="border border-hairline-light rounded-lg overflow-hidden bg-canvas-light shadow-inner">
                  <table className="data-table-grid w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-canvas-cream border-b border-hairline-light">
                        <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">
                          Sản phẩm
                        </th>
                        <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">
                          Dự kiến
                        </th>
                        {!isAwaitingPreReceiveApproval(selectedReceipt) && (
                          <>
                            <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">
                              Đếm số lượng
                            </th>
                            <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">
                              Đạt kiểm định
                            </th>
                            <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">
                              Hàng không đạt
                            </th>
                            <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">
                              Chi tiết kiểm định
                            </th>
                          </>
                        )}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {(selectedReceipt.items || []).map((item) => (
                        <tr
                          key={item.id || item.receipt_item_id}
                          className="hover:bg-canvas-cream/50 transition-colors"
                        >
                          <td className="px-4 py-3">
                            <span className="font-semibold block">
                              {getProductName(item)}
                            </span>
                            <span className="text-[10px] text-shade-40 font-mono block">
                              {getProductSku(item)}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-right font-semibold">
                            {formatQty(getExpectedQty(item))}
                          </td>
                          {!isAwaitingPreReceiveApproval(selectedReceipt) && (
                            <>
                              <td className="px-4 py-3 text-right font-semibold">
                                {item.actual_qty ?? item.actualQty ?? "-"}
                              </td>
                              <td className="px-4 py-3 text-right font-bold text-success-600">
                                {item.qc_passed_qty ?? item.qcPassedQty ?? "-"}
                              </td>
                              <td className="px-4 py-3 text-right font-bold text-danger-600">
                                {item.qc_failed_qty ?? item.qcFailedQty ?? "-"}
                              </td>
                              <td className="px-4 py-3">
                                {item.qc_result ? (
                                  <div className="flex flex-col gap-0.5">
                                    <div className="flex gap-1.5 items-center">
                                      <span
                                        className={`text-[9px] font-bold ${item.qc_result === "PASSED" ? "text-success-700" : item.qc_result === "FAILED" ? "text-danger-700" : "text-warning-700"}`}
                                      >
                                        {item.qc_result}
                                      </span>
                                    </div>
                                    {item.qc_failure_reason && (
                                      <span className="text-[10px] text-danger-600 italic block">
                                        {item.qc_failure_reason}
                                      </span>
                                    )}
                                  </div>
                                ) : (
                                  <span className="text-shade-40 italic">
                                    Chưa kiểm định
                                  </span>
                                )}
                              </td>
                            </>
                          )}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>

              {/* Form Input for approval notes / rejection reason */}
              {(isAwaitingPreReceiveApproval(selectedReceipt) ||
                selectedReceipt.status === "QC_COMPLETED" ||
                selectedReceipt.status === "QC_FAILED" ||
                isRejecting) &&
                ((isAwaitingPreReceiveApproval(selectedReceipt) &&
                  canPreReceiveApprove) ||
                  (!isAwaitingPreReceiveApproval(selectedReceipt) &&
                    (hasRole(ROLES.WAREHOUSE_MANAGER) ||
                      hasRole(ROLES.ADMIN)))) && (
                  <div className="bg-canvas-light p-4 border border-hairline-light rounded-lg shadow-level-3">
                    {isRejecting ? (
                      <div className="flex flex-col gap-2">
                        <label className="text-xs font-bold text-danger-700 flex items-center gap-1.5">
                          <AlertTriangle className="w-3.5 h-3.5" />
                          {isAwaitingPreReceiveApproval(selectedReceipt)
                            ? "Lý do yêu cầu chỉnh sửa (Bắt buộc)"
                            : "Lý do từ chối phê duyệt (Bắt buộc)"}
                        </label>
                        <textarea
                          value={rejectionReason}
                          onChange={(e) => setRejectionReason(e.target.value)}
                          placeholder={
                            isAwaitingPreReceiveApproval(selectedReceipt)
                              ? "Nhập nội dung cần Planner chỉnh sửa trước khi nhận hàng..."
                              : "Nhập lý do chi tiết từ chối phiếu nhập này..."
                          }
                          className="text-input text-xs h-20 resize-none border-danger-300 focus:border-danger-500 focus:ring-danger-100"
                          required
                        />
                      </div>
                    ) : (
                      <div className="flex flex-col gap-2">
                        <label className="text-xs font-bold text-ink">
                          {isAwaitingPreReceiveApproval(selectedReceipt)
                            ? "Ghi chú duyệt kế hoạch (Không bắt buộc)"
                            : "Ghi chú phê duyệt (Không bắt buộc)"}
                        </label>
                        <textarea
                          value={approvalNotes}
                          onChange={(e) => setApprovalNotes(e.target.value)}
                          placeholder={
                            isAwaitingPreReceiveApproval(selectedReceipt)
                              ? "Nhập ý kiến duyệt kế hoạch nhập kho..."
                              : "Nhập ý kiến phê duyệt của bạn..."
                          }
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

              {canStorekeeperReview(selectedReceipt) && (
                <div className="flex gap-2">
                  <button
                    aria-label="approve-storekeeper-review-detail"
                    onClick={() =>
                      handleStorekeeperReview(selectedReceipt, "APPROVE")
                    }
                    className="btn-pill btn-pill-aloe text-xs py-1.5 px-4 font-bold"
                  >
                    Duyet kiem dem
                  </button>
                  <button
                    aria-label="request-recount-detail"
                    onClick={() =>
                      handleStorekeeperReview(
                        selectedReceipt,
                        "REQUEST_RECOUNT",
                      )
                    }
                    className="btn-pill btn-pill-outline-light border-danger-500 hover:bg-danger-50 text-danger-600 text-xs py-1.5 px-4"
                  >
                    Bat dem lai
                  </button>
                </div>
              )}

              {false &&
                (selectedReceipt.status === "DRAFT" ||
                  selectedReceipt.status === "QC_COMPLETED" ||
                  selectedReceipt.status === "QC_FAILED") &&
                (hasRole(ROLES.WAREHOUSE_STAFF) ||
                  hasRole(ROLES.STOREKEEPER) ||
                  hasRole(ROLES.WAREHOUSE_MANAGER) ||
                  hasRole(ROLES.ADMIN)) && (
                  <button
                    onClick={() => handleOpenEditCount(selectedReceipt)}
                    className="btn-pill btn-pill-outline-light text-xs py-1.5 px-4"
                  >
                    Đếm số lượng
                  </button>
                )}

              {(isAwaitingPreReceiveApproval(selectedReceipt) ||
                selectedReceipt.status === "QC_COMPLETED" ||
                selectedReceipt.status === "QC_FAILED") &&
                ((isAwaitingPreReceiveApproval(selectedReceipt) &&
                  canPreReceiveApprove) ||
                  (!isAwaitingPreReceiveApproval(selectedReceipt) &&
                    (hasRole(ROLES.WAREHOUSE_MANAGER) ||
                      hasRole(ROLES.ADMIN)))) && (
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
                          {submittingApproval
                            ? isAwaitingPreReceiveApproval(selectedReceipt)
                              ? "Đang gửi..."
                              : "Đang từ chối..."
                            : isAwaitingPreReceiveApproval(selectedReceipt)
                              ? "Gửi yêu cầu chỉnh sửa"
                              : "Xác nhận từ chối"}
                        </button>
                      </>
                    ) : (
                      <>
                        <button
                          onClick={() => setIsRejecting(true)}
                          className="btn-pill btn-pill-outline-light border-danger-500 hover:bg-danger-50 text-danger-600 text-xs py-1.5 px-4"
                        >
                          {isAwaitingPreReceiveApproval(selectedReceipt)
                            ? "Yêu cầu chỉnh sửa"
                            : "Từ chối"}
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
                              {isAwaitingPreReceiveApproval(selectedReceipt)
                                ? "Duyệt kế hoạch"
                                : "Duyệt nhập kho"}
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

      {/* Edit Count Modal */}
      {showEditModal && selectedReceipt && (
        <div className="fixed inset-0 bg-canvas-night/40 backdrop-blur-sm flex items-center justify-center p-4 z-50">
          <div className="bg-canvas-cream rounded-lg max-w-2xl w-full border border-hairline-light shadow-level-4 overflow-hidden flex flex-col max-h-[85vh]">
            <div className="p-6 border-b border-hairline-light flex items-center justify-between bg-canvas-cream">
              <div>
                <span className="text-[10px] font-bold text-shade-40 uppercase tracking-widest block mb-1">
                  Chỉnh sửa đếm thực tế
                </span>
                <h3 className="text-xl font-bold flex items-center gap-3">
                  {selectedReceipt.receipt_number}
                </h3>
              </div>
              <button
                onClick={() => setShowEditModal(false)}
                className="p-1 hover:bg-canvas-cream rounded-full transition-colors text-shade-50 hover:text-ink"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto flex-1 flex flex-col gap-4">
              <p className="text-xs text-shade-60">
                Nhập lại số lượng hàng đếm được thực tế cho tất cả các dòng sản
                phẩm trong phiếu:
              </p>

              <div className="border border-hairline-light rounded-lg overflow-hidden bg-canvas-light">
                <table className="data-table-grid w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-canvas-cream border-b border-hairline-light">
                      <th className="px-4 py-3 font-semibold text-shade-60 uppercase">
                        Sản phẩm
                      </th>
                      <th className="px-4 py-3 font-semibold text-shade-60 text-right uppercase">
                        Dự kiến
                      </th>
                      <th className="px-4 py-3 font-semibold text-shade-60 text-right uppercase">
                        Số lượng đếm mới
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {editItems.map((item) => (
                      <tr key={item.receipt_item_id}>
                        <td className="px-4 py-3">
                          <span className="font-semibold block">
                            {item.product_name}
                          </span>
                          <span className="text-[10px] text-shade-40 font-mono block">
                            {item.product_sku}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right font-semibold">
                          {item.expected_qty}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <input
                            type="number"
                            min="0"
                            value={item.counted_qty}
                            onChange={(e) =>
                              handleEditCountChange(
                                item.receipt_item_id,
                                e.target.value,
                              )
                            }
                            className="w-24 text-right px-2 py-1 border border-hairline-light rounded text-xs font-bold focus:ring-1 focus:ring-ink"
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            <div className="p-4 border-t border-hairline-light bg-canvas-cream flex justify-between items-center gap-3">
              <button
                onClick={() => setShowEditModal(false)}
                className="btn-pill btn-pill-outline-light text-xs"
              >
                Hủy
              </button>
              <button
                onClick={handleSaveEditCountClick}
                disabled={submittingEditCount}
                className="btn-pill bg-ink text-onPrimary hover:bg-shade-70 text-xs px-5 py-2 font-bold disabled:opacity-50 flex items-center gap-2"
              >
                {submittingEditCount ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  "Lưu số lượng đếm"
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* QC Warning Confirmation Modal */}
      {showWarningModal && (
        <div className="fixed inset-0 bg-canvas-night/60 backdrop-blur-sm flex items-center justify-center p-4 z-[60]">
          <div className="bg-canvas-cream rounded-lg max-w-md w-full border border-danger-300 shadow-level-4 overflow-hidden p-6 flex flex-col gap-4">
            <div className="flex items-start gap-3">
              <div className="p-2 bg-danger-100 rounded-full text-danger-700 flex-shrink-0">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <div>
                <h4 className="text-base font-bold text-ink">
                  Xác nhận làm mới dữ liệu QC?
                </h4>
                <p className="text-xs text-shade-60 mt-1">
                  Lưu ý: Việc sửa lại số lượng đếm sẽ xóa toàn bộ kết quả kiểm
                  tra chất lượng (QC) hiện tại và đưa phiếu về DRAFT để thực
                  hiện QC lại từ đầu. Bạn có chắc chắn muốn lưu?
                </p>
              </div>
            </div>

            <div className="flex justify-end gap-3 pt-2 border-t border-hairline-light">
              <button
                onClick={() => setShowWarningModal(false)}
                className="btn-pill btn-pill-outline-light text-xs"
              >
                Hủy bỏ
              </button>
              <button
                onClick={executeSaveEditCount}
                disabled={submittingEditCount}
                className="btn-pill bg-danger-600 hover:bg-danger-700 text-white text-xs px-4 py-2 font-bold disabled:opacity-50 flex items-center gap-1.5"
              >
                {submittingEditCount ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    Đang lưu...
                  </>
                ) : (
                  "Đồng ý reset QC & Lưu"
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ReceiptList;
