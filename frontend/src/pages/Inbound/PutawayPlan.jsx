import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useUiStore } from '../../stores/ui.store';
import { inboundService } from '../../services/inbound.service';
import { masterDataService } from '../../services/masterData.service';
import { ArrowLeft, Loader2, Warehouse, AlertTriangle, CheckCircle, Check, PackageCheck, Plus, Trash2 } from 'lucide-react';
import Badge from '../../components/common/Badge';

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

  // Multi-bin allocations state: mapping itemId -> array of { id, binId, qty }
  const [allocations, setAllocations] = useState({});

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
      // Filter active storage bins only (exclude quarantine and staging locations)
      setBins(
        binsData.filter(
          (b) =>
            (b.is_active ?? b.isActive) &&
            !(b.is_quarantine ?? b.isQuarantine) &&
            !(b.is_staging ?? b.isStaging),
        ),
      );

      const putawayComplete =
        receiptData.status === 'PUTAWAY_COMPLETED' ||
        Boolean(receiptData.putaway_completed_at || receiptData.putawayCompletedAt);

      // Initialize allocations
      const initialAllocations = {};
      passedItems.forEach(item => {
        const passedQty = item.qc_passed_qty || item.approved_qty || 0;
        initialAllocations[item.id] = [
          {
            id: 'alloc-' + item.id + '-1',
            binId: item.location_id ? Number(item.location_id) : '',
            qty: passedQty
          }
        ];
      });
      setAllocations(initialAllocations);
      setIsPutawayComplete(putawayComplete);
    } catch (e) {
      addToast('Lỗi tải dữ liệu cất kệ', 'error');
      navigate('/inbound/receipts');
    } finally {
      setLoading(false);
    }
  };

  const getProduct = (productId) => {
    return products.find(p => Number(p.id) === Number(productId)) || { name: 'Unknown', sku: 'Unknown', volume_m3: 0.05, weight_kg: 2 };
  };

  const handleAddAllocation = (itemId) => {
    const item = items.find(i => i.id === itemId);
    if (!item) return;
    const currentAllocs = allocations[itemId] || [];
    const totalAllocated = currentAllocs.reduce((sum, a) => sum + (Number(a.qty) || 0), 0);
    const unallocated = (item.qc_passed_qty || 0) - totalAllocated;
    const defaultQty = unallocated > 0 ? unallocated : '';

    setAllocations({
      ...allocations,
      [itemId]: [
        ...currentAllocs,
        { id: `alloc-${itemId}-${Date.now()}`, binId: '', qty: defaultQty }
      ]
    });
  };

  const handleRemoveAllocation = (itemId, index) => {
    const currentAllocs = allocations[itemId] || [];
    if (currentAllocs.length <= 1) return;
    const updated = currentAllocs.filter((_, i) => i !== index);
    setAllocations({
      ...allocations,
      [itemId]: updated
    });
  };

  const handleAllocationChange = (itemId, index, field, value) => {
    const currentAllocs = allocations[itemId] || [];
    const updated = currentAllocs.map((alloc, i) => {
      if (i === index) {
        if (field === 'qty') {
          const parsed = parseInt(value, 10);
          return {
            ...alloc,
            qty: isNaN(parsed) || value === '' ? '' : Math.max(0, parsed)
          };
        }
        return {
          ...alloc,
          [field]: value ? Number(value) : ''
        };
      }
      return alloc;
    });
    setAllocations({
      ...allocations,
      [itemId]: updated
    });
  };

  const getItemAllocationSummary = (item) => {
    const currentAllocs = allocations[item.id] || [];
    const totalAllocated = currentAllocs.reduce((sum, a) => sum + (Number(a.qty) || 0), 0);
    const passedQty = item.qc_passed_qty || 0;
    const diff = totalAllocated - passedQty;

    return {
      totalAllocated,
      passedQty,
      isValid: totalAllocated === passedQty,
      diff
    };
  };

  // Bin capacity validator helper
  const checkBinCapacity = (item, binId, qtyOverride) => {
    if (!binId) return { valid: true };
    const bin = bins.find(b => Number(b.id) === Number(binId));
    if (!bin) {
      return { 
        valid: false, 
        message: 'Không tìm thấy Bin',
        volPct: '0',
        wtPct: '0',
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
    const qty = qtyOverride !== undefined ? Number(qtyOverride) : (Number(item.qc_passed_qty) || 0);

    const itemVol = Number(prod.volume_m3 || prod.volumeM3) || 0.05;
    const itemWt = Number(prod.weight_kg || prod.weightKg) || 1.0;

    const incomingVolume = qty * itemVol;
    const incomingWeight = qty * itemWt;

    const currentVol = Number(bin.current_volume_m3) || 0;
    const currentWt = Number(bin.current_weight_kg) || 0;
    const capacityVol = Number(bin.capacity_m3) || 1;
    const capacityWt = Number(bin.capacity_kg) || 1;

    const projectedVol = currentVol + incomingVolume;
    const projectedWt = currentWt + incomingWeight;

    const rawVolPct = (projectedVol / capacityVol) * 100;
    const rawWtPct = (projectedWt / capacityWt) * 100;

    const volPct = rawVolPct > 0 && rawVolPct < 1 ? rawVolPct.toFixed(2) : Math.round(rawVolPct);
    const wtPct = rawWtPct > 0 && rawWtPct < 1 ? rawWtPct.toFixed(2) : Math.round(rawWtPct);

    const exceedsVol = projectedVol > capacityVol;
    const exceedsWt = projectedWt > capacityWt;

    return {
      valid: !exceedsVol && !exceedsWt,
      volPct,
      wtPct,
      rawVolPct,
      rawWtPct,
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
      const itemAllocs = allocations[item.id] || [];
      itemAllocs.forEach(alloc => {
        if (alloc.binId) {
          const check = checkBinCapacity(item, alloc.binId, alloc.qty);
          if (!check.valid) hasError = true;
        }
      });
    });
    return hasError;
  };

  const isFormInvalid = () => {
    let invalid = false;
    items.forEach(item => {
      const summary = getItemAllocationSummary(item);
      if (!summary.isValid) invalid = true;
      const itemAllocs = allocations[item.id] || [];
      if (itemAllocs.some(a => !a.binId || !a.qty || a.qty <= 0)) invalid = true;
    });
    return invalid || hasCapacityErrors();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (isFormInvalid()) {
      addToast('Vui lòng phân bổ đủ số lượng đạt QC và chọn vị trí kệ hợp lệ, không quá tải', 'warning');
      return;
    }

    const payload = {
      expected_version: receipt.version,
      items: items.map(item => {
        const itemAllocs = allocations[item.id] || [];
        const primary = itemAllocs.reduce((max, a) => Number(a.qty) > Number(max.qty) ? a : max, itemAllocs[0] || {});
        return {
          receipt_item_id: item.id,
          location_id: Number(primary.binId),
          quantity: item.approved_qty ?? item.approvedQty ?? item.qc_passed_qty ?? item.actual_qty
        };
      })
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

  const BinCapacityProgress = ({ item, binId, qty }) => {
    if (!binId) return <span className="text-[10px] text-shade-40 italic">Chưa chọn ô kệ</span>;
    
    try {
      const check = checkBinCapacity(item, binId, qty);
      if (!check.valid) {
        if (check.message === 'Không tìm thấy Bin') {
          return <span className="text-[10px] text-danger-500 font-semibold italic">Không tìm thấy vị trí kệ</span>;
        }
        if (check.exceedsVol || check.exceedsWt) {
          return (
            <div className="flex flex-col gap-1 text-[11px] text-danger-600 bg-danger-50 p-2 rounded border border-danger-200">
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
        <div className="flex flex-col gap-1 text-[11px] w-full min-w-[140px]">
          <div className="flex justify-between font-semibold">
            <span>Thể tích: <strong className="text-ink">{check.volPct}%</strong> <span className="text-[10px] text-success-600 font-bold">(+{check.incomingVol.toFixed(3)}m³)</span></span>
            <span className="text-shade-40">{check.capacityVol}m³</span>
          </div>
          <div className="w-full bg-shade-30 h-1.5 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full ${check.rawVolPct > 80 ? 'bg-warning-500' : 'bg-success-500'}`}
              style={{ width: `${Math.max(2, Math.min(100, check.rawVolPct))}%` }}
            />
          </div>

          <div className="flex justify-between font-semibold mt-1">
            <span>Tải trọng: <strong className="text-ink">{check.wtPct}%</strong> <span className="text-[10px] text-success-600 font-bold">(+{check.incomingWt.toFixed(1)}kg)</span></span>
            <span className="text-shade-40">{check.capacityWt}kg</span>
          </div>
          <div className="w-full bg-shade-30 h-1.5 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full ${check.rawWtPct > 80 ? 'bg-warning-500' : 'bg-success-500'}`}
              style={{ width: `${Math.max(2, Math.min(100, check.rawWtPct))}%` }}
            />
          </div>
        </div>
      );
    } catch (e) {
      return (
        <div className="text-[10px] text-danger-500 p-2 border border-danger-200 bg-danger-50 rounded">
          <strong>Lỗi render sức chứa:</strong> {e.message}
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
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Nhập kho</span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">Kế hoạch cất kệ</h1>
        </div>

        <div className="bg-success-50 border border-success-200 rounded-lg p-5 flex items-center gap-4">
          <PackageCheck className="w-8 h-8 text-success-600 flex-shrink-0" />
          <div>
            <p className="text-sm font-bold text-success-900">Phiếu này đã hoàn tất cất kệ</p>
            <p className="text-xs text-success-700 mt-0.5">Tất cả sản phẩm đạt QC đã được phân vị trí vào ô kệ. Không thể cất kệ lại.</p>
          </div>
        </div>

        <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 shadow-level-3 card-premium">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 border-b border-hairline-light pb-2 mb-4">Tóm tắt cất kệ</h3>
          <div className="hidden md:block overflow-x-auto">
            <table className="data-table-grid w-full text-left text-xs border-collapse">
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
                    <tr key={item.id} className="hover:bg-canvas-cream/50 transition-colors">
                      <td className="px-6 py-4">
                        <span className="font-bold block">{prod.sku}</span>
                        <span className="text-shade-50 block">{prod.name}</span>
                      </td>
                      <td className="px-4 py-4 text-right font-bold text-success-600 text-sm">{item.qc_passed_qty}</td>
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-2">
                          <Warehouse className="w-4 h-4 text-shade-40 flex-shrink-0" />
                          <span className="font-semibold text-ink">{bin ? bin.code : `Bin #${item.location_id}`}</span>
                        </div>
                      </td>
                      <td className="px-4 py-4 text-center">
                        <Badge size="sm" type="success">
                          <span className="inline-flex items-center gap-1">
                            <CheckCircle className="w-3 h-3" />
                            Đã cất
                          </span>
                        </Badge>
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
          Vận hành / Nhập kho
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Kế hoạch cất kệ
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="flex flex-col gap-6">
        {/* Header summary info card */}
        <div className="bg-canvas-light border border-hairline-light rounded-lg p-6 shadow-level-3 card-premium">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 border-b border-hairline-light pb-2 mb-4">
            Chứng từ nhập phê duyệt
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs font-semibold">
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Mã phiếu nhập:</span>
              <span className="text-sm font-bold text-ink">{receipt.receipt_number}</span>
            </div>
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Trạng thái:</span>
              <span className="text-success-700 bg-success-50 px-1.5 py-0.5 rounded-pill border border-success-200 uppercase font-semibold text-[10px] tracking-wider whitespace-nowrap">Đã Duyệt</span>
            </div>
            <div>
              <span className="text-shade-50 block mb-0.5 font-normal">Ngày duyệt:</span>
              <span>{new Date(receipt.approved_at).toLocaleString('vi-VN')}</span>
            </div>
          </div>
        </div>

        {/* Putaway Table with Multi-Bin Allocation */}
        <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
          <div className="flex flex-col gap-2 border-b border-hairline-light bg-canvas-cream p-4 md:flex-row md:items-center md:justify-between">
            <div>
              <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40">
                Chi tiết phân vị trí cất hàng đạt QC (Phân phối ô kệ)
              </h3>
              <p className="text-[10px] text-shade-50 font-normal mt-0.5">
                Bạn có thể chia nhỏ số lượng sản phẩm cất vào nhiều ô kệ khác nhau (ví dụ: 10 cất Kệ A, 10 cất Kệ B).
              </p>
            </div>
            <span className="text-[10px] text-shade-50 font-semibold italic">
              * Chỉ cất các sản phẩm đạt kiểm định chất lượng vào ô kệ thông thường
            </span>
          </div>

          <div className="p-4 flex flex-col gap-6">
            {items.map((item) => {
              const prod = getProduct(item.product_id);
              const itemAllocs = allocations[item.id] || [];
              const summary = getItemAllocationSummary(item);

              return (
                <div key={item.id} className="border border-hairline-light rounded-lg bg-canvas-cream/20 p-4 shadow-sm">
                  {/* Item Header */}
                  <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2 pb-3 border-b border-hairline-light mb-4">
                    <div>
                      <span className="text-[10px] font-mono font-bold text-shade-50 block">{prod.sku}</span>
                      <h4 className="font-bold text-sm text-ink">{prod.name}</h4>
                    </div>
                    <div className="flex items-center gap-3">
                      <div className="text-xs">
                        <span className="text-shade-50">SL Đạt QC: </span>
                        <strong className="text-success-600 text-sm">{item.qc_passed_qty}</strong>
                      </div>
                      {summary.isValid ? (
                        <span className="text-success-700 bg-success-50 border border-success-200 text-[10px] px-2 py-0.5 rounded-pill font-bold">
                          Đã phân bổ đủ ({summary.totalAllocated}/{summary.passedQty})
                        </span>
                      ) : summary.totalAllocated < summary.passedQty ? (
                        <span className="text-warning-800 bg-warning-50 border border-warning-200 text-[10px] px-2 py-0.5 rounded-pill font-bold">
                          Chưa phân bổ hết (Còn thiếu {summary.passedQty - summary.totalAllocated} SP)
                        </span>
                      ) : (
                        <span className="text-danger-700 bg-danger-50 border border-danger-200 text-[10px] px-2 py-0.5 rounded-pill font-bold">
                          Vượt quá SL đạt QC (Thừa {summary.diff} SP)
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Allocation Lines */}
                  <div className="flex flex-col gap-3">
                    {itemAllocs.map((alloc, idx) => (
                      <div key={alloc.id || idx} className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center bg-canvas-light p-3 rounded-lg border border-hairline-light">
                        {/* Qty Input */}
                        <div className="md:col-span-3 flex flex-col gap-1">
                          <label className="text-[10px] font-bold uppercase text-shade-50">
                            Số lượng cất vào ô #{idx + 1}
                          </label>
                          <input
                            type="number"
                            min="1"
                            max={item.qc_passed_qty}
                            value={alloc.qty}
                            onChange={(e) => handleAllocationChange(item.id, idx, 'qty', e.target.value)}
                            className="text-input text-xs font-bold text-ink h-9"
                            required
                          />
                        </div>

                        {/* Bin Select */}
                        <div className="md:col-span-5 flex flex-col gap-1">
                          <label className="text-[10px] font-bold uppercase text-shade-50">
                            Chọn ô kệ cất hàng
                          </label>
                          <div className="flex items-center gap-2">
                            <Warehouse className="w-4 h-4 text-shade-40 flex-shrink-0" />
                            <select
                              value={alloc.binId || ''}
                              onChange={(e) => handleAllocationChange(item.id, idx, 'binId', e.target.value)}
                              className="text-input text-xs font-semibold h-9"
                              required
                            >
                              <option value="">-- Chọn vị trí cất --</option>
                              {bins.map(b => (
                                <option key={b.id} value={b.id}>
                                  {b.code} (Sức chứa: {b.capacity_m3}m³, Tải: {b.capacity_kg}kg)
                                </option>
                              ))}
                            </select>
                          </div>
                        </div>

                        {/* Capacity progress indicator */}
                        <div className="md:col-span-3 flex flex-col gap-1">
                          <label className="text-[10px] font-bold uppercase text-shade-50">
                            Sức chứa ô kệ dự kiến
                          </label>
                          <BinCapacityProgress item={item} binId={alloc.binId} qty={alloc.qty} />
                        </div>

                        {/* Remove Action */}
                        <div className="md:col-span-1 flex justify-end items-center pt-3 md:pt-0">
                          {itemAllocs.length > 1 && (
                            <button
                              type="button"
                              onClick={() => handleRemoveAllocation(item.id, idx)}
                              className="p-1.5 text-danger-500 hover:bg-danger-50 rounded-full transition-colors"
                              title="Xóa dòng phân bổ kệ này"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Add Allocation Line Button */}
                  <div className="mt-3 flex justify-start">
                    <button
                      type="button"
                      onClick={() => handleAddAllocation(item.id)}
                      className="inline-flex items-center gap-1.5 text-xs font-bold text-ink hover:text-shade-70 bg-canvas-light hover:bg-canvas-cream border border-hairline-light px-3 py-1.5 rounded-full transition-colors shadow-xs"
                    >
                      <Plus className="w-3.5 h-3.5 text-success-600" />
                      <span>Thêm ô kệ phân phối cất hàng</span>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Warning panel if capacity is exceeded */}
        {hasCapacityErrors() && (
          <div className="bg-danger-50 border border-danger-200 text-danger-900 rounded-lg p-4 text-xs font-semibold flex items-center gap-2">
            <AlertTriangle className="w-4 h-4 text-danger-600 flex-shrink-0" />
            <span>
              Phát hiện vị trí kệ bị quá tải về thể tích hoặc khối lượng. Vui lòng giảm số lượng cất hoặc chọn ô kệ có dung lượng lớn hơn.
            </span>
          </div>
        )}

        {/* Actions */}
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
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
                <span>Hoàn tất cất kệ</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};

export default PutawayPlan;
