import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';
import { ArrowLeft, Trash2, Plus, Search, Loader2 } from 'lucide-react';

const ReceiptForm = () => {
  const navigate = useNavigate();
  const activeWarehouse = useAuthStore((state) => state.activeWarehouse);
  const { addToast } = useUiStore();

  const [type, setType] = useState('PURCHASE');
  const [sourceReference, setSourceReference] = useState('');
  const [sourceChannel, setSourceChannel] = useState('ZALO');
  const [contactPerson, setContactPerson] = useState('');
  const [documentDate, setDocumentDate] = useState(new Date().toISOString().slice(0, 10));
  const [notes, setNotes] = useState('');
  const [partnerId, setPartnerId] = useState('');

  const [suppliers, setSuppliers] = useState([]);
  const [dealers, setDealers] = useState([]);
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);

  // Product search state
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [showSearchResults, setShowSearchResults] = useState(false);

  // Selected items table state
  const [selectedItems, setSelectedItems] = useState([]);

  useEffect(() => {
    fetchMetadata();
  }, []);

  const fetchMetadata = async () => {
    setLoading(true);
    try {
      const [suppliersData, dealersData, productsData] = await Promise.all([
        masterDataService.getSuppliers(),
        masterDataService.getDealers(),
        masterDataService.getProducts(),
      ]);
      setSuppliers(suppliersData.filter(s => s.is_active));
      setDealers(dealersData.filter(d => d.is_active));
      setProducts(productsData.filter(p => p.is_active));
    } catch (e) {
      const status = e?.response?.status;
      const msg = e?.response?.data?.message || e?.message || '';
      if (status === 401) {
        addToast('Phiên đăng nhập hết hạn, vui lòng đăng nhập lại', 'error');
      } else if (status === 403) {
        addToast('Không có quyền truy cập dữ liệu này', 'error');
      } else {
        addToast(`Lỗi tải danh mục: ${msg || 'Vui lòng thử lại'}`, 'error');
      }
      console.error('[ReceiptForm] fetchMetadata error:', status, msg, e);
    } finally {
      setLoading(false);
    }
  };


  // Simple product search debounce
  useEffect(() => {
    if (!searchQuery.trim()) {
      setSearchResults([]);
      return;
    }
    const delayDebounce = setTimeout(() => {
      const filtered = products.filter(p =>
        p.sku.toLowerCase().includes(searchQuery.toLowerCase()) ||
        p.name.toLowerCase().includes(searchQuery.toLowerCase())
      );
      setSearchResults(filtered);
    }, 250);

    return () => clearTimeout(delayDebounce);
  }, [searchQuery, products]);

  const handleAddItem = (product) => {
    // Check if duplicate
    const exists = selectedItems.some(item => item.product_id === product.id);
    if (exists) {
      addToast('Sản phẩm này đã có trong danh sách', 'warning');
      return;
    }

    setSelectedItems([
      ...selectedItems,
      {
        product_id: product.id,
        sku: product.sku,
        name: product.name,
        unit: product.unit,
        has_serial: product.has_serial,
        expected_qty: 1,
        unit_cost: 0.00
      }
    ]);
    setSearchQuery('');
    setShowSearchResults(false);
  };

  const handleQtyChange = (index, value) => {
    const qty = parseFloat(value);
    const updated = [...selectedItems];
    updated[index].expected_qty = isNaN(qty) ? '' : qty;
    setSelectedItems(updated);
  };

  const handleCostChange = (index, value) => {
    const cost = parseFloat(value);
    const updated = [...selectedItems];
    updated[index].unit_cost = isNaN(cost) ? '' : cost;
    setSelectedItems(updated);
  };

  const handleRemoveItem = (index) => {
    setSelectedItems(selectedItems.filter((_, idx) => idx !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!partnerId) {
      addToast(type === 'PURCHASE' ? 'Vui lòng chọn Nhà cung cấp' : 'Vui lòng chọn Đại lý', 'warning');
      return;
    }

    if (!contactPerson.trim()) {
      addToast('Vui lòng nhập tên người liên hệ', 'warning');
      return;
    }

    if (!sourceReference.trim()) {
      addToast('Vui lòng nhập mã chứng từ nguồn (PO/DO hoàn)', 'warning');
      return;
    }

    if (selectedItems.length === 0) {
      addToast('Vui lòng thêm ít nhất 1 sản phẩm vào phiếu nhập', 'warning');
      return;
    }

    // Validation checks
    for (const item of selectedItems) {
      if (item.expected_qty === '' || item.expected_qty <= 0) {
        addToast(`Số lượng dự kiến của sản phẩm ${item.sku} phải lớn hơn 0`, 'warning');
        return;
      }
      if (item.unit_cost === '' || item.unit_cost < 0) {
        addToast(`Đơn giá sản phẩm ${item.sku} không được phép âm`, 'warning');
        return;
      }
    }

    // Payload matches backend CreateReceiptRequest DTO exactly
    const payload = {
      supplier_id: Number(partnerId),
      warehouse_id: activeWarehouse.id,
      source_reference: sourceReference.trim(),
      source_channel: sourceChannel,
      contact_person: contactPerson.trim(),
      notes: notes.trim(),
      items: selectedItems.map(item => ({
        product_id: item.product_id,
        expected_qty: item.expected_qty,
        unit_cost: item.unit_cost
      }))
    };

    setLoading(true);
    try {
      await inboundService.createReceipt(payload);
      addToast('Lập lệnh nhập kho thô thành công', 'success');
      navigate('/inbound/receipts');
    } catch (error) {
      const msg = error?.response?.data?.message || error?.message || 'Lỗi khi lập lệnh nhập kho';
      addToast(msg, 'error');
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
          onClick={() => navigate('/inbound/receipts')}
          className="flex items-center gap-2 text-xs font-semibold text-shade-50 hover:text-ink transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại danh sách</span>
        </button>

        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Vận hành / Inbound
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Lập lệnh nhập kho thô
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col lg:flex-row gap-6 items-start">
        {/* Left column - Metadata */}
        <div className="w-full lg:w-1/3 bg-white border border-hairline-light rounded-lg p-6 shadow-sm card-premium flex flex-col gap-5">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 border-b border-hairline-light pb-2 mb-2">
            Thông tin chung
          </h3>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold">Loại nhập kho</label>
            <select
              value={type}
              onChange={(e) => {
                setType(e.target.value);
                setPartnerId('');
              }}
              className="text-input"
            >
              <option value="PURCHASE">Nhập mua (PO)</option>
              <option value="RETURN">Nhập trả (DO hoàn)</option>
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold">Kho đích nhận</label>
            <input
              type="text"
              value={activeWarehouse?.name || ''}
              disabled
              className="text-input bg-zinc-50 text-shade-50 cursor-not-allowed font-semibold"
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold">
              {type === 'PURCHASE' ? 'Nhà cung cấp' : 'Đại lý trả hàng'}
            </label>
            <select
              value={partnerId}
              onChange={(e) => setPartnerId(e.target.value)}
              className="text-input"
              required
            >
              <option value="">-- Chọn đối tác --</option>
              {type === 'PURCHASE'
                ? suppliers.map(s => <option key={s.id} value={s.id}>{s.company_name} ({s.code})</option>)
                : dealers.map(d => <option key={d.id} value={d.id}>{d.name} ({d.code})</option>)
              }
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold">Người liên hệ <span className="text-red-500">*</span></label>
            <input
              type="text"
              placeholder="VD: Nguyễn Văn A"
              value={contactPerson}
              onChange={(e) => setContactPerson(e.target.value)}
              className="text-input"
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold">Mã chứng từ nguồn (PO/DO hoàn) <span className="text-red-500">*</span></label>
            <input
              type="text"
              placeholder="VD: PO-2026-0005"
              value={sourceReference}
              onChange={(e) => setSourceReference(e.target.value)}
              className="text-input"
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold">Kênh thông tin</label>
            <select
              value={sourceChannel}
              onChange={(e) => setSourceChannel(e.target.value)}
              className="text-input"
            >
              <option value="ZALO">Zalo</option>
              <option value="EMAIL">Email</option>
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold">Ngày chứng từ</label>
            <input
              type="date"
              value={documentDate}
              onChange={(e) => setDocumentDate(e.target.value)}
              className="text-input"
              required
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-bold">Ghi chú</label>
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
          <div className="bg-white border border-hairline-light rounded-lg p-6 shadow-sm card-premium relative">
            <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-4 border-b border-hairline-light pb-2">
              Thêm sản phẩm
            </h3>

            <div className="relative">
              <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
              <input
                type="text"
                placeholder="Tìm kiếm sản phẩm theo tên, SKU..."
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setShowSearchResults(true);
                }}
                onFocus={() => setShowSearchResults(true)}
                className="w-full text-input pl-10"
              />

              {/* Search results dropdown */}
              {showSearchResults && searchQuery.trim() !== '' && (
                <div className="absolute left-0 right-0 mt-1.5 bg-white border border-hairline-light rounded-lg shadow-xl max-h-60 overflow-y-auto z-40">
                  {searchResults.length === 0 ? (
                    <div className="p-4 text-xs text-shade-50 text-center">Không tìm thấy sản phẩm hợp lệ</div>
                  ) : (
                    searchResults.map(prod => (
                      <div
                        key={prod.id}
                        onClick={() => handleAddItem(prod)}
                        className="p-3 hover:bg-zinc-50 cursor-pointer transition-colors border-b border-hairline-light last:border-0 flex items-center justify-between text-xs"
                      >
                        <div>
                          <span className="font-bold block">{prod.sku}</span>
                          <span className="text-shade-50 block">{prod.name}</span>
                        </div>

                      </div>
                    ))
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Selected Items Table */}
          <div className="bg-white border border-hairline-light rounded-lg shadow-sm card-premium overflow-hidden">
            <div className="p-4 border-b border-hairline-light bg-zinc-50">
              <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40">
                Chi tiết sản phẩm lập lệnh
              </h3>
            </div>

            {selectedItems.length === 0 ? (
              <div className="p-12 text-center text-sm text-shade-40">
                Chưa có sản phẩm nào được chọn. Hãy tìm kiếm và thêm sản phẩm ở khung phía trên.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-zinc-50 border-b border-hairline-light">
                      <th className="px-6 py-3 font-bold text-shade-60">Sản phẩm</th>
                      <th className="px-6 py-3 font-bold text-shade-60 text-right w-24">Số lượng dự kiến</th>
                      <th className="px-6 py-3 font-bold text-shade-60 text-right w-36">Đơn giá nhập (VND)</th>
                      <th className="px-6 py-3 font-bold text-shade-60 text-right w-20">Hành động</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {selectedItems.map((item, index) => (
                      <tr key={item.product_id} className="hover:bg-zinc-50/50">
                        <td className="px-6 py-4">
                          <span className="font-bold block">{item.sku}</span>
                          <span className="text-shade-50 block">{item.name}</span>

                        </td>
                        <td className="px-6 py-4 text-right">
                          <input
                            type="number"
                            min="1"
                            step="any"
                            value={item.expected_qty}
                            onChange={(e) => handleQtyChange(index, e.target.value)}
                            className="text-input text-right font-bold w-20 py-1"
                            required
                          />
                        </td>
                        <td className="px-6 py-4 text-right">
                          <input
                            type="number"
                            min="0"
                            step="any"
                            value={item.unit_cost}
                            onChange={(e) => handleCostChange(index, e.target.value)}
                            className="text-input text-right font-bold w-32 py-1"
                            required
                          />
                        </td>
                        <td className="px-6 py-4 text-right">
                          <button
                            type="button"
                            onClick={() => handleRemoveItem(index)}
                            className="p-1 text-red-500 hover:text-red-700 hover:bg-red-50 rounded transition-colors"
                          >
                            <Trash2 className="w-4 h-4 mx-auto" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          {/* Form Actions */}
          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={() => navigate('/inbound/receipts')}
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
                <span>Lập Lệnh Nhập Kho</span>
              )}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
};

export default ReceiptForm;
