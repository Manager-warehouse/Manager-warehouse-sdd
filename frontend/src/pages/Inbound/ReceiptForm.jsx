import React, { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuthStore } from "../../stores/auth.store";
import { useUiStore } from "../../stores/ui.store";
import { useDebounce } from "../../hooks/useDebounce";
import { inboundService } from "../../services/inbound.service";
import { masterDataService } from "../../services/masterData.service";
import pricingService from "../../services/pricing.service";
import { ArrowLeft, Trash2, Plus, Search, Loader2, AlertTriangle } from "lucide-react";
import Input from "../../components/common/Input";

const ReceiptForm = () => {
  const navigate = useNavigate();
  const { id: revisionReceiptId } = useParams();
  const activeWarehouse = useAuthStore((state) => state.activeWarehouse);
  const { addToast } = useUiStore();
  const isRevisionMode = Boolean(revisionReceiptId);

  const [documentDate, setDocumentDate] = useState(
    new Date().toISOString().slice(0, 10),
  );
  const [notes, setNotes] = useState("");
  const [partnerId, setPartnerId] = useState("");

  const [suppliers, setSuppliers] = useState([]);
  const [dealers, setDealers] = useState([]);
  const [products, setProducts] = useState([]);
  const [existingReceipts, setExistingReceipts] = useState([]);
  const [revisionReceipt, setRevisionReceipt] = useState(null);
  const [loading, setLoading] = useState(true);

  // Product search state
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState([]);
  const [showSearchResults, setShowSearchResults] = useState(false);

  // Selected items table state
  const [selectedItems, setSelectedItems] = useState([]);

  useEffect(() => {
    fetchMetadata();
  }, [revisionReceiptId]);

  const receiptNumberPreview = useMemo(() => {
    if (isRevisionMode) {
      return (
        revisionReceipt?.receipt_number || revisionReceipt?.receiptNumber || ""
      );
    }
    const dateStr = (
      documentDate || new Date().toISOString().slice(0, 10)
    ).replace(/-/g, "");
    const nextSequence =
      existingReceipts.filter((receipt) =>
        (receipt.receipt_number || receipt.receiptNumber || "").startsWith(
          `PO-${dateStr}`,
        ),
      ).length + 1;
    return `PO-${dateStr}-${String(nextSequence).padStart(4, "0")}`;
  }, [documentDate, existingReceipts, isRevisionMode, revisionReceipt]);

  const fetchMetadata = async () => {
    setLoading(true);
    try {
      const receiptsPromise = activeWarehouse?.id
        ? inboundService.getReceipts(activeWarehouse.id).catch(() => [])
        : Promise.resolve([]);
      const [suppliersData, dealersData, productsData, receiptsData] =
        await Promise.all([
          masterDataService.getSuppliers(),
          masterDataService.getDealers(),
          masterDataService.getProducts(),
          receiptsPromise,
        ]);
      setSuppliers(suppliersData.filter((s) => s.is_active));
      setDealers(dealersData.filter((d) => d.is_active));
      setProducts(productsData.filter((p) => p.is_active));
      setExistingReceipts(Array.isArray(receiptsData) ? receiptsData : []);
      if (isRevisionMode) {
        const detail = await inboundService.getReceiptById(revisionReceiptId);
        if (detail.status !== "REVISION_REQUIRED") {
          addToast("Phiếu này không ở trạng thái cần chỉnh sửa", "warning");
          navigate("/inbound/receipts");
          return;
        }
        setRevisionReceipt(detail);
        setPartnerId(String(detail.supplier_id || detail.supplierId || ""));
        setDocumentDate(detail.document_date || detail.documentDate || "");
        setNotes(detail.notes || "");
        setSelectedItems(
          (detail.items || []).map((item) => {
            const productId = item.product_id || item.productId;
            const product = productsData.find((p) => p.id === productId);
            return {
              receipt_item_id: item.receipt_item_id || item.id,
              product_id: productId,
              sku:
                item.product_sku ||
                item.productSku ||
                product?.sku ||
                `SKU-${productId}`,
              name:
                item.product_name ||
                item.productName ||
                product?.name ||
                `Sản phẩm ${productId}`,
              unit: product?.unit,
              expected_qty: item.expected_qty ?? item.expectedQty ?? 1,
              unit_cost: item.unit_cost ?? item.unitCost ?? 0,
            };
          }),
        );
      }
    } catch (e) {
      const status = e?.response?.status;
      const msg = e?.response?.data?.message || e?.message || "";
      if (status === 401) {
        addToast("Phiên đăng nhập hết hạn, vui lòng đăng nhập lại", "error");
      } else if (status === 403) {
        addToast("Không có quyền truy cập dữ liệu này", "error");
      } else {
        addToast(`Lỗi tải danh mục: ${msg || "Vui lòng thử lại"}`, "error");
      }
    } finally {
      setLoading(false);
    }
  };

  // Simple product search debounce
  const debouncedSearchQuery = useDebounce(searchQuery, 250);

  useEffect(() => {
    if (!debouncedSearchQuery.trim()) {
      setSearchResults([]);
      return;
    }
    const filtered = products.filter(
      (p) =>
        (p.sku || "")
          .toLowerCase()
          .includes(debouncedSearchQuery.toLowerCase()) ||
        (p.name || "")
          .toLowerCase()
          .includes(debouncedSearchQuery.toLowerCase()),
    );
    setSearchResults(filtered);
  }, [debouncedSearchQuery, products]);

  const handleAddItem = async (product) => {
    // Check if duplicate
    const exists = selectedItems.some((item) => item.product_id === product.id);
    if (exists) {
      addToast("Sản phẩm này đã có trong danh sách", "warning");
      return;
    }

    let approvedPrice;
    try {
      approvedPrice = await pricingService.lookupApproved({
        product_id: product.id,
        warehouse_id: activeWarehouse.id,
        date: documentDate,
      });
    } catch (error) {
      addToast(
        `SKU ${product.sku} chưa có giá vốn đã duyệt cho kho/ngày này`,
        "warning",
      );
      return;
    }

    setSelectedItems([
      ...selectedItems,
      {
        product_id: product.id,
        sku: product.sku,
        name: product.name,
        unit: product.unit,
        expected_qty: 1,
        unit_cost: approvedPrice.cost_price ?? approvedPrice.costPrice ?? 0,
      },
    ]);
    setSearchQuery("");
    setShowSearchResults(false);
  };

  const handleQtyChange = (index, value) => {
    const qty = parseFloat(value);
    const updated = [...selectedItems];
    updated[index].expected_qty = isNaN(qty) ? "" : qty;
    setSelectedItems(updated);
  };

  const handleRemoveItem = (index) => {
    setSelectedItems(selectedItems.filter((_, idx) => idx !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!partnerId) {
      addToast("Vui lòng chọn Nhà cung cấp", "warning");
      return;
    }

    if (!documentDate) {
      addToast("Vui long chon ngay chung tu", "warning");
      return;
    }

    const todayStr = new Date().toISOString().slice(0, 10);
    if (!isRevisionMode && documentDate < todayStr) {
      addToast("Ngày Nhập Hàng không được nhỏ hơn ngày hiện tại", "warning");
      return;
    }

    if (selectedItems.length === 0) {
      addToast("Vui lòng thêm ít nhất 1 sản phẩm vào phiếu nhập", "warning");
      return;
    }

    // Validation checks
    for (const item of selectedItems) {
      if (item.expected_qty === "" || item.expected_qty <= 0) {
        addToast(
          `Số lượng dự kiến của sản phẩm ${item.sku} phải lớn hơn 0`,
          "warning",
        );
        return;
      }
    }

    // Payload matches backend CreateReceiptRequest DTO exactly
    const payload = {
      supplier_id: Number(partnerId),
      warehouse_id: activeWarehouse.id,
      documentDate,
      notes: notes.trim(),
      items: selectedItems.map((item) => ({
        product_id: item.product_id,
        expected_qty: item.expected_qty,
      })),
    };

    setLoading(true);
    try {
      const savedReceipt = isRevisionMode
        ? await inboundService.reviseReceipt(revisionReceiptId, {
            expectedVersion: revisionReceipt?.version || 0,
            documentDate,
            notes: notes.trim(),
            items: selectedItems.map((item) => ({
              receipt_item_id: item.receipt_item_id,
              product_id: item.product_id,
              expected_qty: item.expected_qty,
            })),
          })
        : await inboundService.createReceipt(payload);
      const receiptNumber =
        savedReceipt?.receipt_number || savedReceipt?.receiptNumber;
      addToast(
        receiptNumber
          ? isRevisionMode
            ? `Đã chỉnh sửa và gửi lại phiếu: ${receiptNumber}`
            : `Kế hoạch nhập kho: ${receiptNumber}`
          : isRevisionMode
            ? "Đã chỉnh sửa và gửi lại phiếu"
            : "Kế hoạch nhập kho",
        "success",
      );
      navigate("/inbound/receipts");
    } catch (error) {
      const msg =
        error?.response?.data?.message ||
        error?.message ||
        "Lỗi khi lập lệnh nhập kho";
      addToast(msg, "error");
    } finally {
      setLoading(false);
    }
  };

  if (loading && products.length === 0) {
    return (
      <div className="flex items-center justify-center p-20">
        <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      {/* Header section */}
      <div>
        <button
          onClick={() => navigate("/inbound/receipts")}
          className="flex items-center gap-2 text-xs font-semibold text-shade-50 hover:text-ink transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại danh sách</span>
        </button>

        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Vận hành / Nhập kho
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          {isRevisionMode ? "Chỉnh sửa lệnh nhập kho" : "Lập lệnh nhập kho"}
        </h1>
        {isRevisionMode && (revisionReceipt?.pre_receive_rejection_reason || revisionReceipt?.preReceiveRejectionReason || revisionReceipt?.rejection_reason || revisionReceipt?.rejectionReason || revisionReceipt?.notes) && (
          <div className="mt-2 rounded-lg border border-danger-200 bg-danger-50 px-4 py-3 text-xs font-medium text-danger-900 flex items-start gap-2">
            <AlertTriangle className="w-4 h-4 text-danger-600 flex-shrink-0 mt-0.5" />
            <div>
              <strong className="font-bold text-danger-700 block mb-0.5">Lý do Quản lý yêu cầu chỉnh sửa:</strong>
              <span>
                {revisionReceipt.pre_receive_rejection_reason ||
                  revisionReceipt.preReceiveRejectionReason ||
                  revisionReceipt.rejection_reason ||
                  revisionReceipt.rejectionReason ||
                  revisionReceipt.notes}
              </span>
            </div>
          </div>
        )}
      </div>

      <form
        onSubmit={handleSubmit}
        className="flex flex-col lg:flex-row gap-6 items-start"
      >
        {/* Left column - Metadata */}
        <div className="w-full lg:w-1/3 bg-canvas-light border border-hairline-light rounded-lg p-6 shadow-level-3 card-premium flex flex-col gap-5">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 border-b border-hairline-light pb-2 mb-2">
            Thông tin chung
          </h3>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">
              Kho
            </label>
            <input
              type="text"
              value={activeWarehouse?.name || ""}
              disabled
              className="text-input bg-canvas-cream text-shade-50 cursor-not-allowed font-semibold"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">
              Nhà cung cấp <span className="text-danger-500">*</span>
            </label>
            <select
              value={partnerId}
              onChange={(e) => setPartnerId(e.target.value)}
              className="text-input"
              required
            >
              <option value="">-- Chọn Nhà cung cấp --</option>
              {suppliers.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.company_name} ({s.code})
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">
              Mã Nhập Hàng
            </label>
            <input
              type="text"
              value={receiptNumberPreview}
              readOnly
              className="text-input bg-canvas-cream font-semibold tracking-wide text-shade-90"
              aria-readonly="true"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">
              Ngày Nhập Hàng <span className="text-danger-500">*</span>
            </label>
            <input
              type="date"
              value={documentDate}
              min={
                isRevisionMode
                  ? undefined
                  : new Date().toISOString().slice(0, 10)
              }
              onChange={(e) => setDocumentDate(e.target.value)}
              className="text-input"
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold uppercase tracking-wider text-shade-60">
              Ghi chú
            </label>
            <textarea
              placeholder="Nhập ghi chú thêm..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="text-input h-20 resize-none"
            />
          </div>
        </div>

        {/* Right column - Products list & selection */}
        <div className="w-full lg:w-2/3 flex flex-col gap-6">
          {/* Product Search & Selector */}
          {!isRevisionMode && (
            <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 shadow-level-3 card-premium relative">
              <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-4 border-b border-hairline-light pb-2">
                Thêm sản phẩm
              </h3>

              <div className="relative">
                <Input
                  type="text"
                  leftIcon={Search}
                  placeholder="Tìm kiếm sản phẩm theo tên, SKU..."
                  value={searchQuery}
                  onChange={(e) => {
                    setSearchQuery(e.target.value);
                    setShowSearchResults(true);
                  }}
                  onFocus={() => setShowSearchResults(true)}
                />

                {/* Search results dropdown */}
                {showSearchResults && searchQuery.trim() !== "" && (
                  <div className="absolute left-0 right-0 mt-1.5 bg-canvas-light border border-hairline-light rounded-lg shadow-level-4 max-h-60 overflow-y-auto z-40">
                    {searchResults.length === 0 ? (
                      <div className="p-4 text-xs text-shade-50 text-center">
                        Không tìm thấy sản phẩm hợp lệ
                      </div>
                    ) : (
                      searchResults.map((prod) => (
                        <div
                          key={prod.id}
                          onClick={() => handleAddItem(prod)}
                          className="p-3 hover:bg-canvas-cream cursor-pointer transition-colors border-b border-hairline-light last:border-0 flex items-center justify-between text-xs"
                        >
                          <div>
                            <span className="font-bold block">{prod.sku}</span>
                            <span className="text-shade-50 block">
                              {prod.name}
                            </span>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Selected Items Table */}
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
            <div className="p-4 border-b border-hairline-light bg-canvas-cream">
              <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40">
                Chi tiết sản phẩm lập lệnh
              </h3>
            </div>

            {selectedItems.length === 0 ? (
              <div className="p-12 text-center text-sm text-shade-40">
                Chưa có sản phẩm nào được chọn. Hãy tìm kiếm và thêm sản phẩm ở
                khung phía trên.
              </div>
            ) : (
              <>
                {/* Desktop/tablet: table view */}
                <div className="hidden md:block overflow-x-auto">
                  <table className="data-table-grid w-full text-left text-xs border-collapse">
                    <thead>
                      <tr className="bg-canvas-cream border-b border-hairline-light">
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">
                          Sản phẩm
                        </th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-24">
                          Số lượng dự kiến
                        </th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-36">
                          Giá vốn đã duyệt
                        </th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-36">
                          Tổng tiền nhập
                        </th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-20">
                          Hành động
                        </th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {selectedItems.map((item, index) => (
                        <tr
                          key={item.product_id}
                          className="hover:bg-canvas-cream/50 transition-colors"
                        >
                          <td className="px-6 py-4">
                            <span className="font-bold block">{item.sku}</span>
                            <span className="text-shade-50 block">
                              {item.name}
                            </span>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <input
                              type="number"
                              min="1"
                              step="any"
                              value={item.expected_qty}
                              onChange={(e) =>
                                handleQtyChange(index, e.target.value)
                              }
                              className="text-input text-right font-bold w-20 py-1"
                              required
                            />
                          </td>
                          <td className="px-6 py-4 text-right">
                            <div className="text-input text-right font-bold w-32 py-1 bg-canvas-cream text-shade-60">
                              {formatVND(item.unit_cost)}
                            </div>
                          </td>
                          <td className="px-6 py-4 text-right">
                            <div className="text-input text-right font-bold w-32 py-1 bg-canvas-cream text-ink">
                              {formatVND(
                                (Number(item.expected_qty) || 0) *
                                  (Number(item.unit_cost) || 0),
                              )}
                            </div>
                          </td>
                          <td className="px-6 py-4 text-right">
                            {!isRevisionMode && (
                              <button
                                type="button"
                                onClick={() => handleRemoveItem(index)}
                                className="p-1 text-danger-500 hover:text-danger-700 hover:bg-danger-50 rounded-full transition-colors"
                              >
                                <Trash2 className="w-4 h-4" />
                              </button>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                    {selectedItems.length > 0 && (
                      <tfoot className="border-t-2 border-hairline-light bg-canvas-cream font-bold">
                        <tr>
                          <td className="px-6 py-3 text-xs uppercase tracking-wider text-shade-60">
                            Tổng cộng
                          </td>
                          <td className="px-6 py-3 text-right text-xs text-ink font-extrabold">
                            {selectedItems
                              .reduce(
                                (sum, item) =>
                                  sum + (Number(item.expected_qty) || 0),
                                0,
                              )
                              .toLocaleString("vi-VN")}
                          </td>
                          <td className="px-6 py-3"></td>
                          <td className="px-6 py-3 text-right text-xs text-ink font-extrabold">
                            {formatVND(
                              selectedItems.reduce(
                                (sum, item) =>
                                  sum +
                                  (Number(item.expected_qty) || 0) *
                                    (Number(item.unit_cost) || 0),
                                0,
                              ),
                            )}
                          </td>
                          <td className="px-6 py-3"></td>
                        </tr>
                      </tfoot>
                    )}
                  </table>
                </div>

                {/* Mobile: stacked card view with full-width inputs */}
                <div className="flex flex-col divide-y divide-hairline-light md:hidden">
                  {selectedItems.map((item, index) => (
                    <div
                      key={item.product_id}
                      className="p-4 flex flex-col gap-3"
                    >
                      <div className="flex justify-between items-start gap-2">
                        <div className="text-xs">
                          <span className="font-bold block">{item.sku}</span>
                          <span className="text-shade-50 block">
                            {item.name}
                          </span>
                        </div>
                        {!isRevisionMode && (
                          <button
                            type="button"
                            onClick={() => handleRemoveItem(index)}
                            className="p-1.5 text-danger-500 hover:text-danger-700 hover:bg-danger-50 rounded transition-colors shrink-0"
                            title="Xóa sản phẩm"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                      <div className="grid grid-cols-3 gap-2">
                        <div className="flex flex-col gap-1">
                          <label className="text-[10px] font-semibold uppercase tracking-wider text-shade-50">
                            Số lượng dự kiến
                          </label>
                          <input
                            type="number"
                            min="1"
                            step="any"
                            value={item.expected_qty}
                            onChange={(e) =>
                              handleQtyChange(index, e.target.value)
                            }
                            className="text-input text-right font-bold py-1.5"
                            required
                          />
                        </div>
                        <div className="flex flex-col gap-1">
                          <label className="text-[10px] font-semibold uppercase tracking-wider text-shade-50">
                            Giá vốn đã duyệt
                          </label>
                          <div className="text-input text-right font-bold py-1.5 bg-canvas-cream text-shade-60">
                            {formatVND(item.unit_cost)}
                          </div>
                        </div>
                        <div className="flex flex-col gap-1">
                          <label className="text-[10px] font-semibold uppercase tracking-wider text-shade-50">
                            Tổng tiền nhập
                          </label>
                          <div className="text-input text-right font-bold py-1.5 bg-canvas-cream text-ink">
                            {formatVND(
                              (Number(item.expected_qty) || 0) *
                                (Number(item.unit_cost) || 0),
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}
          </div>

          {/* Form Actions */}
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={() => navigate("/inbound/receipts")}
              className="btn-pill btn-pill-outline-light"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={loading || selectedItems.length === 0}
              className="btn-pill btn-pill-primary flex items-center gap-2 disabled:opacity-50"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Đang xử lý...
                </>
              ) : (
                <span>
                  {isRevisionMode
                    ? "Gửi lại cho WMS duyệt"
                    : "Lập Lệnh Nhập Kho"}
                </span>
              )}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default ReceiptForm;

function formatVND(value) {
  return `${Number(value || 0).toLocaleString("vi-VN")} đ`;
}
