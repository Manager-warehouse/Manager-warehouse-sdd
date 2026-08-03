import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { AlertCircle, ArrowLeft, Check, Loader2, PackageSearch } from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';
import { useUiStore } from '../../stores/ui.store';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';

const isEffectiveQuarantine = (location) => (
  location?.is_quarantine === true || location?.parent_is_quarantine === true
);

const isEffectiveStaging = (location) => (
  location?.is_staging === true || location?.parent_is_staging === true
);

const roundQuantity = (quantity) => Math.round((Number(quantity) + Number.EPSILON) * 100) / 100;

const buildAllocationRows = (order, locations) => {
  const stagingLocations = locations.filter((location) => !isEffectiveQuarantine(location) && isEffectiveStaging(location));
  const quarantineLocations = locations.filter(isEffectiveQuarantine);
  const defaultStagingId = stagingLocations.length === 1 ? stagingLocations[0].id : '';
  const defaultQuarantineId = quarantineLocations.length === 1 ? quarantineLocations[0].id : '';

  return (order.items || []).flatMap((item) =>
    (item.allocations || [])
      .filter((allocation) => !allocation.qc_completed)
      .map((allocation, index) => {
        const plannedQty = Number(allocation.planned_qty || item.planned_qty || item.requested_qty || 0);

        return {
          id: `${item.id}-${allocation.allocation_id || index}`,
          do_item_id: item.id,
          allocation_id: allocation.allocation_id,
          batch_id: allocation.batch_id,
          location_id: allocation.location_id,
          zone_id: allocation.zone_id,
          product_name: item.product_name,
          sku: item.sku,
          planned_qty: plannedQty,
          picked_qty: plannedQty,
          qc_fail_qty: 0,
          reason: '',
          staging_location_id: defaultStagingId,
          quarantine_location_id: defaultQuarantineId,
          notes: '',
          replacement: allocation.replacement === true,
        };
      }),
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
      addToast('Đơn này không còn dòng phân bổ mới cần lấy hàng và kiểm định.', 'error');
      return;
    }

    const invalidQty = qcRows.some((row) => (
      !Number.isFinite(Number(row.picked_qty))
      || Number(row.picked_qty) !== Number(row.planned_qty)
    ));
    if (invalidQty) {
      addToast('Số lượng thực lấy phải bằng số lượng kế hoạch của từng dòng phân bổ.', 'error');
      return;
    }

    const invalidFailQty = qcRows.some((row) => (
      !Number.isFinite(Number(row.qc_fail_qty))
      || Number(row.qc_fail_qty) < 0
      || Number(row.qc_fail_qty) > Number(row.picked_qty)
    ));
    if (invalidFailQty) {
      addToast('Số lượng không đạt phải từ 0 đến số lượng thực lấy.', 'error');
      return;
    }

    const missingFailReason = qcRows.some((row) => Number(row.qc_fail_qty) > 0 && !row.reason.trim());
    if (missingFailReason) {
      addToast('Vui lòng nhập lý do cho các dòng không đạt kiểm định.', 'error');
      return;
    }

    const missingStagingLocation = qcRows.some((row) => !row.staging_location_id);
    if (missingStagingLocation) {
      addToast('Vui lòng chọn vị trí trung chuyển cho tất cả dòng phân bổ.', 'error');
      return;
    }

    const missingQuarantineLocation = qcRows.some((row) => Number(row.qc_fail_qty) > 0 && !row.quarantine_location_id);
    if (missingQuarantineLocation) {
      addToast('Vui lòng chọn vị trí cách ly cho các dòng không đạt kiểm định.', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await outboundService.confirmQCOutbound(id, { items: qcRows });
      addToast('Hoàn tất kiểm định xuất kho', 'success');
      navigate(`/outbound/delivery-orders/${id}`);
    } catch (error) {
      addToast(error.message || 'Lỗi khi hoàn tất kiểm định', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const failedRows = qcRows.filter((row) => Number(row.qc_fail_qty) > 0);
  const failCount = failedRows.length;
  const totalFailQty = failedRows.reduce((total, row) => total + Number(row.qc_fail_qty), 0);
  const stagingOptions = locations.filter((location) => !isEffectiveQuarantine(location) && isEffectiveStaging(location));
  const quarantineOptions = locations.filter(isEffectiveQuarantine);

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
            Vận hành / Xuất kho / Kiểm định xuất kho
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Ghi nhận lấy hàng & kiểm định theo phân bổ: {order.do_number}
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Đại lý: <span className="font-semibold text-ink">{order.dealer_name}</span>
          </p>
        </div>
      </div>

      {order.cancel_reason && (
        <div className="flex items-start gap-3 rounded-lg border border-warning-200 bg-warning-50 p-4">
          <AlertCircle className="mt-0.5 h-5 w-5 shrink-0 text-warning-700" />
          <div>
            <p className="text-sm font-semibold text-warning-800">Storekeeper yêu cầu đếm/QC lại</p>
            <p className="mt-1 text-sm text-warning-700">{order.cancel_reason}</p>
          </div>
        </div>
      )}

      {failCount > 0 && (
        <div className="bg-danger-50 border border-danger-200 rounded-lg p-4 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-danger-600 shrink-0" />
          <p className="text-sm text-danger-700 font-medium">
            <span className="font-bold">{totalFailQty}</span> sản phẩm không đạt trên {failCount} dòng phân bổ.
            Nhập đầy đủ lý do và vị trí cách ly.
          </p>
        </div>
      )}

      <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden card-premium">
        <div className="px-6 py-4 bg-canvas-cream border-b border-hairline-light flex items-center gap-2">
          <PackageSearch className="w-4 h-4 text-shade-50" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-shade-60">
            Danh sách phân bổ lấy hàng mới ({qcRows.length} dòng)
          </h3>
        </div>

        {!qcRows.length ? (
          <div className="p-8 text-center text-sm text-shade-50">
            Đơn này không còn dòng phân bổ mới cần lấy hàng và kiểm định.
          </div>
        ) : (
          <div className="divide-y divide-hairline-light">
            {qcRows.map((row) => {
              const qcFailQty = Number(row.qc_fail_qty) || 0;
              const qcPassQty = Math.max(0, roundQuantity(Number(row.picked_qty) - qcFailQty));
              const hasQcFailure = qcFailQty > 0;
              return (
                <div key={row.id} className={`p-6 transition-colors ${hasQcFailure ? 'bg-danger-50/30' : 'bg-canvas-light'}`}>
                  <div className="flex flex-col gap-4">
                    <div>
                      <div>
                        <h4 className="text-sm font-bold text-ink">{row.product_name}</h4>
                        <p className="text-xs text-shade-40 mt-0.5 font-mono">SKU: {row.sku || '-'}</p>
                        <p className="text-xs text-shade-50 mt-1">
                          Phân bổ #{row.allocation_id || '-'} · Lô {row.batch_id || '-'} · Vị trí {row.location_id || '-'} · Khu {row.zone_id || '-'}
                        </p>
                        {row.replacement && (
                          <p className="mt-2 inline-flex rounded-pill border border-info-200 bg-info-50 px-3 py-1 text-[11px] font-semibold text-info-700">
                            Hàng bù do storekeeper lập kế hoạch
                          </p>
                        )}
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
                      <Input label="SL kế hoạch" type="number" value={row.planned_qty} disabled />
                      <Input
                        label="SL thực lấy"
                        type="number"
                        min="0"
                        max={row.planned_qty}
                        step="0.01"
                        value={row.picked_qty}
                        onChange={(event) => updateRow(row.id, 'picked_qty', event.target.value)}
                      />
                      <Input label="SL đạt kiểm định" type="number" value={qcPassQty} disabled />
                      <Input
                        label="SL không đạt"
                        type="number"
                        min="0"
                        max={row.picked_qty}
                        step="0.01"
                        value={row.qc_fail_qty}
                        onChange={(event) => updateRow(row.id, 'qc_fail_qty', event.target.value)}
                        error={qcFailQty > Number(row.picked_qty) ? 'Không được vượt SL thực lấy' : undefined}
                      />
                    </div>

                    <Input
                      label="Vị trí trung chuyển *"
                      type="select"
                      value={row.staging_location_id}
                      onChange={(event) => updateRow(row.id, 'staging_location_id', event.target.value)}
                      options={[
                        { value: '', label: '-- Chọn vị trí trung chuyển --' },
                        ...stagingOptions.map((location) => ({ value: location.id, label: location.code || `Location #${location.id}` })),
                      ]}
                    />

                    {hasQcFailure && (
                      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <Input
                          label="Lý do không đạt kiểm định *"
                          value={row.reason}
                          onChange={(event) => updateRow(row.id, 'reason', event.target.value)}
                          placeholder="Móp méo, trầy xước, sai mã..."
                        />
                        <Input
                          label="Vị trí cách ly *"
                          type="select"
                          value={row.quarantine_location_id}
                          onChange={(event) => updateRow(row.id, 'quarantine_location_id', event.target.value)}
                          options={[
                            { value: '', label: '-- Chọn vị trí cách ly --' },
                            ...quarantineOptions.map((location) => ({ value: location.id, label: location.code || `Location #${location.id}` })),
                          ]}
                        />
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
              <><Check className="w-3.5 h-3.5" /> Gửi kết quả lấy hàng & kiểm định</>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
