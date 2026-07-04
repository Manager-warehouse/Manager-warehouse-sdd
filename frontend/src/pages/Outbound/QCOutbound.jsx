import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, ArrowLeft, Check, Loader2, PackageSearch } from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';
import { useUiStore } from '../../stores/ui.store';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';

const buildAllocationRows = (order, locations) => {
  const stagingLocations = locations.filter((location) => location.is_quarantine !== true);
  const quarantineLocations = locations.filter((location) => location.is_quarantine === true);
  const defaultStagingId = stagingLocations.length === 1 ? stagingLocations[0].id : '';
  const defaultQuarantineId = quarantineLocations.length === 1 ? quarantineLocations[0].id : '';

  return (order.items || []).flatMap((item) =>
    (item.allocations || []).map((allocation, index) => ({
      id: `${item.id}-${allocation.allocation_id || index}`,
      do_item_id: item.id,
      allocation_id: allocation.allocation_id,
      batch_id: allocation.batch_id,
      location_id: allocation.location_id,
      zone_id: allocation.zone_id,
      product_name: item.product_name,
      sku: item.sku,
      planned_qty: Number(allocation.planned_qty || item.planned_qty || item.requested_qty || 0),
      picked_qty: Number(allocation.planned_qty || item.planned_qty || item.requested_qty || 0),
      result: 'PASSED',
      reason: '',
      staging_location_id: defaultStagingId,
      quarantine_location_id: defaultQuarantineId,
      notes: '',
    })),
  );
};

export default function QCOutbound() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();

  const [order, setOrder] = useState(null);
  const [locations, setLocations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [qcRows, setQcRows] = useState([]);

  useEffect(() => {
    fetchOrderAndLocations();
  }, [id]);

  const fetchOrderAndLocations = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getDeliveryOrderById(id);
      const warehouseLocations = data?.warehouse_id
        ? await masterDataService.getBinLocations(data.warehouse_id)
        : [];
      setOrder(data);
      setLocations(warehouseLocations);
      setQcRows(buildAllocationRows(data, warehouseLocations));
    } catch (error) {
      addToast(error.message || 'Không tìm thấy đơn xuất hàng', 'error');
      navigate('/outbound/delivery-orders');
    } finally {
      setLoading(false);
    }
  };

  const updateRow = (rowId, field, value) => {
    setQcRows((prev) => prev.map((row) => (row.id === rowId ? { ...row, [field]: value } : row)));
  };

  const handleConfirmQC = async () => {
    if (!qcRows.length) {
      addToast('Đơn này chưa có dòng phân bổ để ghi nhận lấy hàng/QC', 'error');
      return;
    }

    const invalidQty = qcRows.some((row) => Number(row.picked_qty) < 0 || Number(row.picked_qty) > Number(row.planned_qty));
    if (invalidQty) {
      addToast('Số lượng đã lấy phải nằm trong mức phân bổ đã lập', 'error');
      return;
    }

    const missingFailReason = qcRows.some((row) => row.result === 'FAILED' && !row.reason.trim());
    if (missingFailReason) {
      addToast('Vui lòng nhập lý do cho các dòng không đạt QC', 'error');
      return;
    }

    const missingStagingLocation = qcRows.some((row) => !row.staging_location_id);
    if (missingStagingLocation) {
      addToast('Vui lòng chọn vị trí trung chuyển cho tất cả dòng phân bổ', 'error');
      return;
    }

    const missingQuarantineLocation = qcRows.some((row) => row.result === 'FAILED' && !row.quarantine_location_id);
    if (missingQuarantineLocation) {
      addToast('Vui lòng chọn vị trí cách ly cho các dòng không đạt QC', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await outboundService.confirmQCOutbound(id, { items: qcRows });
      addToast('Hoàn tất QC xuất kho', 'success');
      navigate(`/outbound/delivery-orders/${id}`);
    } catch (error) {
      addToast(error.message || 'Lỗi khi hoàn tất QC', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const failCount = qcRows.filter((row) => row.result === 'FAILED').length;
  const stagingOptions = locations.filter((location) => location.is_quarantine !== true);
  const quarantineOptions = locations.filter((location) => location.is_quarantine === true);

  if (loading) {
    return (
      <div className="flex items-center justify-center p-20">
        <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
      </div>
    );
  }

  if (!order) return null;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start gap-4">
        <button
          onClick={() => navigate(`/outbound/delivery-orders/${id}`)}
          className="mt-1 p-1.5 hover:bg-canvas-cream rounded-full transition-colors text-shade-50 hover:text-ink shrink-0"
        >
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Vận hành / Xuất kho / QC Outbound
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Ghi nhận lấy hàng/QC theo phân bổ: {order.do_number}
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Đại lý: <span className="font-semibold text-ink">{order.dealer_name}</span>
          </p>
        </div>
      </div>

      {failCount > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          <p className="text-sm text-red-700 font-medium">
            <span className="font-bold">{failCount}</span> dòng phân bổ đang được đánh dấu không đạt QC.
            Nhập đầy đủ lý do và vị trí cách ly.
          </p>
        </div>
      )}

      <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden card-premium">
        <div className="px-6 py-4 bg-canvas-cream border-b border-hairline-light flex items-center gap-2">
          <PackageSearch className="w-4 h-4 text-shade-50" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-shade-60">
            Danh sách phân bổ lấy hàng ({qcRows.length} dòng)
          </h3>
        </div>

        {!qcRows.length ? (
          <div className="p-8 text-center text-sm text-shade-50">
            Đơn này chưa có dòng phân bổ. Thủ kho cần lưu kế hoạch lấy hàng trước.
          </div>
        ) : (
          <div className="divide-y divide-hairline-light">
            {qcRows.map((row) => {
              const isFailed = row.result === 'FAILED';
              return (
                <div key={row.id} className={`p-6 transition-colors ${isFailed ? 'bg-red-50/30' : 'bg-canvas-light'}`}>
                  <div className="flex flex-col gap-4">
                    <div className="flex flex-col lg:flex-row lg:items-start lg:justify-between gap-4">
                      <div>
                        <h4 className="text-sm font-bold text-ink">{row.product_name}</h4>
                        <p className="text-xs text-shade-40 mt-0.5 font-mono">SKU: {row.sku || '-'}</p>
                        <p className="text-xs text-shade-50 mt-1">
                          Phân bổ #{row.allocation_id || '-'} · Lô {row.batch_id || '-'} · Vị trí {row.location_id || '-'} · Khu {row.zone_id || '-'}
                        </p>
                      </div>

                      <div className="flex items-center gap-2 shrink-0">
                        <button
                          onClick={() => updateRow(row.id, 'result', 'PASSED')}
                          className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-pill border text-xs font-semibold transition-colors ${
                            !isFailed
                              ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
                              : 'bg-canvas-light border-hairline-light text-shade-50 hover:bg-canvas-cream'
                          }`}
                        >
                          Đạt QC
                        </button>
                        <button
                          onClick={() => updateRow(row.id, 'result', 'FAILED')}
                          className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-pill border text-xs font-semibold transition-colors ${
                            isFailed
                              ? 'bg-red-50 border-red-300 text-red-700'
                              : 'bg-canvas-light border-hairline-light text-shade-50 hover:bg-canvas-cream'
                          }`}
                        >
                          Không đạt QC
                        </button>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                      <div>
                        <label className="block text-xs font-semibold text-shade-60 mb-1.5">SL kế hoạch</label>
                        <input disabled value={row.planned_qty} className="w-full text-input bg-canvas-cream text-xs" />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold text-shade-60 mb-1.5">SL thực lấy</label>
                        <input
                          type="number"
                          min="0"
                          max={row.planned_qty}
                          value={row.picked_qty}
                          onChange={(event) => updateRow(row.id, 'picked_qty', Number(event.target.value))}
                          className="w-full text-input text-xs"
                        />
                      </div>
                      <div>
                        <label className="block text-xs font-semibold text-shade-60 mb-1.5">Vị trí trung chuyển *</label>
                        <Input
                          type="select"
                          value={row.staging_location_id}
                          onChange={(event) => updateRow(row.id, 'staging_location_id', event.target.value)}
                          options={[
                            { value: '', label: '-- Chọn vị trí trung chuyển --' },
                            ...stagingOptions.map((location) => ({ value: location.id, label: location.code || `Location #${location.id}` })),
                          ]}
                        />
                      </div>
                    </div>

                    {isFailed && (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                          <label className="block text-xs font-semibold text-red-700 mb-1.5">Lý do không đạt QC *</label>
                          <input
                            type="text"
                            value={row.reason}
                            onChange={(event) => updateRow(row.id, 'reason', event.target.value)}
                            placeholder="Móp méo, trầy xước, sai mã..."
                            className="w-full text-input text-xs border-red-300 focus:border-red-500"
                          />
                        </div>
                        <div>
                          <label className="block text-xs font-semibold text-red-700 mb-1.5">Vị trí cách ly *</label>
                          <Input
                            type="select"
                            value={row.quarantine_location_id}
                            onChange={(event) => updateRow(row.id, 'quarantine_location_id', event.target.value)}
                            options={[
                              { value: '', label: '-- Chọn vị trí cách ly --' },
                              ...quarantineOptions.map((location) => ({ value: location.id, label: location.code || `Location #${location.id}` })),
                            ]}
                          />
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}

        <div className="px-6 py-4 border-t border-hairline-light bg-canvas-cream flex justify-between items-center gap-3">
          <Button variant="outline-light" onClick={() => navigate(`/outbound/delivery-orders/${id}`)}>
            Hủy bỏ
          </Button>
          <button
            onClick={handleConfirmQC}
            disabled={!qcRows.length || submitting}
            className="btn-pill btn-pill-aloe text-xs py-1.5 px-4 font-bold disabled:opacity-50 flex items-center gap-1.5"
          >
            {submitting ? (
              <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Đang gửi...</>
            ) : (
              <><Check className="w-3.5 h-3.5" /> Gửi kết quả lấy hàng/QC</>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
