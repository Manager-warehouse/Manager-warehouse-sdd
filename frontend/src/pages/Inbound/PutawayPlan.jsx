import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';
import { ArrowLeft, Loader2, Warehouse, AlertTriangle, CheckCircle, Check, PackageCheck } from 'lucide-react';

const PutawayPlan = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();

  const [receipt, setReceipt] = useState(null);
  const [items, setItems] = useState([]);
  const [products, setProducts] = useState([]);
  const [bins, setBins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [isPutawayComplete, setIsPutawayComplete] = useState(false);

  // Selected bin mapping: key is item.id, value is bin.id
  const [selectedBins, setSelectedBins] = useState({});

  useEffect(() => {
    fetchData();
  }, [id]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const receiptData = await inboundService.getReceiptById(id);
      setReceipt(receiptData);
      
      // Filter out items with no passed quantity
      const passedItems = receiptData.items
        .filter(item => item.qc_passed_qty > 0)
        .map(item => ({
          ...item,
          id: item.receipt_item_id
        }));
      setItems(passedItems);

      const [productsData, binsData] = await Promise.all([
        masterDataService.getProducts(),
        masterDataService.getBinLocations(receiptData.warehouse_id),
      ]);
      setProducts(productsData);
      // Filter active non-quarantine bins
      setBins(binsData.filter(b => b.is_active && !b.is_quarantine));

      // Prefill bins if already assigned
      const prefilled = {};
      passedItems.forEach(item => {
        if (item.location_id) prefilled[item.id] = item.location_id;
      });
      setSelectedBins(prefilled);

      // Check if all QC-passed items are already put away
      const allPutaway = passedItems.length > 0 && passedItems.every(item => !!item.location_id);
      setIsPutawayComplete(allPutaway);
    } catch (e) {
      addToast('Lỗi tải dữ liệu cất kệ', 'error');
      navigate('/inbound/receipts');
    } finally {
      setLoading(false);
    }
  };

  const getProduct = (productId) => {
    return products.find(p => Number(p.id) === Number(productId)) || { name: 'Unknown', sku: 'Unknown', volume_m3: 0.1, weight_kg: 10 };
  };

  const handleBinChange = (itemId, binId) => {
    setSelectedBins({
      ...selectedBins,
      [itemId]: binId ? Number(binId) : ''
    });
  };

  // Bin capacity validator helper
  const checkBinCapacity = (item, binId) => {
    if (!binId) return { valid: true };
    const bin = bins.find(b => Number(b.id) === Number(binId));
    if (!bin) {
      return { 
        valid: false, 
        message: 'Không tìm thấy Bin',
        volPct: 0,
        wtPct: 0,
        currentVol: 0,
        currentWt: 0,
        capacityVol: 1,
        capacityWt: 1,
        incomingVol: 0,
        incomingWt: 0,
        exceedsVol: false,
        exceedsWt: false
      };
    }

    const prod = getProduct(item.product_id);
    const qty = Number(item.qc_passed_qty) || 0;

    const incomingVolume = qty * (Number(prod.volume_m3) || 0);
    const incomingWeight = qty * (Number(prod.weight_kg) || 0);

    const currentVol = Number(bin.current_volume_m3) || 0;
    const currentWt = Number(bin.current_weight_kg) || 0;
    const capacityVol = Number(bin.capacity_m3) || 1; // avoid division by zero
    const capacityWt = Number(bin.capacity_kg) || 1;

    const projectedVol = currentVol + incomingVolume;
    const projectedWt = currentWt + incomingWeight;

    const volPct = (projectedVol / capacityVol) * 100;
    const wtPct = (projectedWt / capacityWt) * 100;

    const exceedsVol = projectedVol > capacityVol;
    const exceedsWt = projectedWt > capacityWt;

    return {
      valid: !exceedsVol && !exceedsWt,
      volPct: Math.round(volPct),
      wtPct: Math.round(wtPct),
      currentVol,
      currentWt,
      capacityVol,
      capacityWt,
      incomingVol: incomingVolume,
      incomingWt: incomingWeight,
      exceedsVol,
      exceedsWt
    };
  };

  const hasCapacityErrors = () => {
    let hasError = false;
    items.forEach(item => {
      const binId = selectedBins[item.id];
      if (binId) {
        const check = checkBinCapacity(item, binId);
        if (!check.valid) hasError = true;
      }
    });
    return hasError;
  };

  const isFormInvalid = () => {
    // Check if any item has not selected a bin
    const anyEmpty = items.some(item => !selectedBins[item.id]);
    return anyEmpty || hasCapacityErrors();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (isFormInvalid()) {
      addToast('Vui lòng chọn đầy đủ ô kệ hợp lệ và không bị quá tải', 'warning');
      return;
    }

    const payload = {
      expected_version: receipt.version,
      items: items.map(item => ({
        receipt_item_id: item.id,
        location_id: selectedBins[item.id]
      }))
    };

    setSubmitting(true);
    try {
      await inboundService.putawayReceipt(id, payload);
      addToast('Cất kệ hàng hóa thành công', 'success');
      navigate('/inbound/receipts');
    } catch (error) {
      addToast(error.message === 'BIN_CAPACITY_EXCEEDED' ? 'Vị trí kệ đã quá tải tải trọng' : 'Lỗi cất kệ', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  // Bin capacity progress indicator component
  const BinCapacityProgress = ({ item, binId }) => {
    if (!binId) return <span className="text-[10px] text-shade-40 italic">Chưa chọn ô kệ</span>;
    
    try {
      const check = checkBinCapacity(item, binId);
      if (!check.valid) {
        if (check.message === 'Không tìm thấy Bin') {
          return <span className="text-[10px] text-red-500 font-semibold italic">Không tìm thấy vị trí kệ</span>;
        }
        if (check.exceedsVol || check.exceedsWt) {
          return (
            <div className="flex flex-col gap-1 text-[11px] text-red-600 bg-red-50 p-2 rounded border border-red-200">
              <span className="font-bold flex items-center gap-1">
                <AlertTriangle className="w-3.5 h-3.5" />
                Vượt quá sức chứa!
              </span>
              {check.exceedsVol && (
                <span>Thể tích: {check.volPct}% ({ (check.currentVol + check.incomingVol).toFixed(2) }/{ check.capacityVol } m3)</span>
              )}
              {check.exceedsWt && (
                <span>Khối lượng: {check.wtPct}% ({ (check.currentWt + check.incomingWt).toFixed(1) }/{ check.capacityWt } kg)</span>
              )}
            </div>
          );
        }
      }

      return (
        <div className="flex flex-col gap-1 text-[11px] w-full min-w-[120px]">
          <div className="flex justify-between font-semibold">
            <span>Thể tích: {check.volPct}%</span>
            <span className="text-shade-40">{check.currentVol.toFixed(2)}m3</span>
          </div>
          <div className="w-full bg-shade-30 h-1.5 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full ${check.volPct > 80 ? 'bg-amber-500' : 'bg-emerald-500'}`}
              style={{ width: `${Math.min(100, check.volPct)}%` }}
            />
          </div>

          <div className="flex justify-between font-semibold mt-1">
            <span>Tải trọng: {check.wtPct}%</span>
            <span className="text-shade-40">{check.currentWt}kg</span>
          </div>
          <div className="w-full bg-shade-30 h-1.5 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full ${check.wtPct > 80 ? 'bg-amber-500' : 'bg-emerald-500'}`}
              style={{ width: `${Math.min(100, check.wtPct)}%` }}
            />
          </div>
        </div>
      );
    } catch (e) {
      console.error("BinCapacityProgress Error:", e);
      return (
        <div className="text-[10px] text-red-500 p-2 border border-red-200 bg-red-50 rounded">
          <strong>Lỗi render sức chứa:</strong> {e.message}
          <pre className="text-[8px] mt-1 overflow-x-auto max-w-[200px]">{e.stack}</pre>
        </div>
      );
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-20">
        <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
      </div>
    );
  }

  // Read-only view when putaway is already complete
  if (isPutawayComplete) {
    return (
      <div className="flex flex-col gap-6">
        <div>
          <button
            onClick={() => navigate('/inbound/receipts')}
            className="flex items-center gap-2 text-xs font-semibold text-shade-50 hover:text-ink transition-colors mb-4"
          >
            <ArrowLeft className="w-4 h-4" />
            <span>Quay lại danh sách</span>
          </button>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Inbound</span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">Kế hoạch cất kệ (Putaway)</h1>
        </div>

        {/* Already completed banner */}
        <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-5 flex items-center gap-4">
          <PackageCheck className="w-8 h-8 text-emerald-600 flex-shrink-0" />
          <div>
            <p className="text-sm font-bold text-emerald-900">Phiếu này đã hoàn tất cất kệ</p>
            <p className="text-xs text-emerald-700 mt-0.5">Tất cả sản phẩm đạt QC đã được phân vị trí vào ô kệ. Không thể cất kệ lại.</p>
          </div>
        </div>

        {/* Summary info */}
        <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 shadow-level-3 card-premium">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 border-b border-hairline-light pb-2 mb-4">Tóm tắt cất kệ</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="bg-canvas-cream border-b border-hairline-light">
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Sản phẩm</th>
                  <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-28">Số lượng đạt</th>
                  <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Vị trí ô kệ đã cất</th>
                  <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center w-24">Trạng thái</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {items.map(item => {
                  const prod = getProduct(item.product_id);
                  const bin = bins.find(b => Number(b.id) === Number(item.location_id));
                  return (
                    <tr key={item.id} className="hover:bg-canvas-cream">
                      <td className="px-6 py-4">
                        <span className="font-bold block">{prod.sku}</span>
                        <span className="text-shade-50 block">{prod.name}</span>
                      </td>
                      <td className="px-4 py-4 text-right font-bold text-emerald-600 text-sm">{item.qc_passed_qty}</td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <Warehouse className="w-4 h-4 text-shade-40 flex-shrink-0" />
                          <span className="font-semibold text-ink">{bin ? bin.code : `Bin #${item.location_id}`}</span>
                        </div>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <span className="inline-flex items-center gap-1 text-[10px] font-semibold uppercase tracking-wider whitespace-nowrap text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-pill">
                          <CheckCircle className="w-3 h-3" />
                          Đã cất
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        <div className="flex justify-end">
          <button
            onClick={() => navigate('/inbound/receipts')}
            className="btn-pill btn-pill-outline-light"
          >
            Quay lại danh sách
          </button>
        </div>
      </div>
    );
  }

  try {
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
            Kế hoạch cất kệ (Putaway)
          </h1>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-6">
          {/* Header summary info card */}
          <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 shadow-level-3 card-premium">
            <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 border-b border-hairline-light pb-2 mb-4">
              Chứng từ nhập phê duyệt
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 text-xs font-semibold">
              <div>
                <span className="text-shade-50 block mb-0.5 font-normal">Mã phiếu nhập:</span>
                <span className="text-sm font-bold text-ink">{receipt.receipt_number}</span>
              </div>
              <div>
                <span className="text-shade-50 block mb-0.5 font-normal">Chừng từ gốc (PO/DO):</span>
                <span>{receipt.source_reference || receipt.source_order_code || 'N/A'}</span>
              </div>
              <div>
                <span className="text-shade-50 block mb-0.5 font-normal">Trạng thái:</span>
                <span className="text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded-pill border border-emerald-200 uppercase font-semibold text-[10px] tracking-wider whitespace-nowrap">Đã Duyệt</span>
              </div>
              <div>
                <span className="text-shade-50 block mb-0.5 font-normal">Ngày duyệt:</span>
                <span>{new Date(receipt.approved_at).toLocaleString('vi-VN')}</span>
              </div>
            </div>
          </div>

          {/* Putaway Table */}
          <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 card-premium overflow-hidden">
            <div className="p-4 border-b border-hairline-light bg-canvas-cream flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40">
                Chi tiết phân vị trí cất hàng đạt QC
              </h3>
              <span className="text-[10px] text-shade-50 font-semibold italic">
                * Chỉ cất các sản phẩm đạt kiểm định chất lượng vào ô kệ thông thường
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-canvas-cream border-b border-hairline-light">
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Sản phẩm</th>
                    <th className="px-4 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right w-24">Số lượng đạt</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 w-56">Chọn ô kệ cất hàng (Bin)</th>
                    <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 w-72">Sức chứa ô kệ dự kiến</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light">
                  {items.map((item) => {
                    const prod = getProduct(item.product_id);
                    const binId = selectedBins[item.id];
                    
                    return (
                      <tr key={item.id} className="hover:bg-canvas-cream">
                        <td className="px-6 py-4">
                          <span className="font-bold block">{prod.sku}</span>
                          <span className="text-shade-50 block mb-1">{prod.name}</span>
                        </td>
                        <td className="px-4 py-4 text-right font-bold text-emerald-600 text-sm">
                          {item.qc_passed_qty}
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <Warehouse className="w-4 h-4 text-shade-40 flex-shrink-0" />
                            <select
                              value={binId || ''}
                              onChange={(e) => handleBinChange(item.id, e.target.value)}
                              className="text-input text-xs font-semibold py-1.5"
                              required
                            >
                              <option value="">-- Chọn vị trí cất --</option>
                              {bins.map(b => (
                                <option key={b.id} value={b.id}>
                                  {b.code} (Thể tích: {b.capacity_m3}m3, Tải: {b.capacity_kg}kg)
                                </option>
                              ))}
                            </select>
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <BinCapacityProgress item={item} binId={binId} />
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Warning panel if capacity is exceeded */}
          {hasCapacityErrors() && (
            <div className="bg-red-50 border border-red-200 text-red-900 rounded-lg p-4 text-xs font-semibold flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-red-600 flex-shrink-0" />
              <span>
                Phát hiện ô kệ bị quá tải về thể tích hoặc khối lượng. Vui lòng chọn ô kệ khác có dung lượng lớn hơn để cất hàng.
              </span>
            </div>
          )}

          {/* Actions */}
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
              disabled={submitting || isFormInvalid()}
              className="btn-pill btn-pill-aloe flex items-center gap-2 disabled:opacity-50 font-bold"
            >
              {submitting ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  Đang xử lý...
                </>
              ) : (
                <>
                  <Check className="w-4 h-4" />
                  <span>Hoàn tất cất kệ (Putaway)</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    );
  } catch (e) {
    console.error("PutawayPlan Render Error:", e);
    return (
      <div className="p-6 bg-red-50 border border-red-200 text-red-700 rounded-lg">
        <h2 className="text-lg font-bold">Đã xảy ra lỗi giao diện (Putaway Plan Render Error)</h2>
        <p className="text-sm mt-1">{e.message}</p>
        <pre className="text-xs mt-4 p-4 bg-zinc-950 text-zinc-100 rounded overflow-auto">{e.stack}</pre>
      </div>
    );
  }
};

export default PutawayPlan;
