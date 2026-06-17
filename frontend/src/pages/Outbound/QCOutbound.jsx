import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  ArrowLeft, CheckCircle2, AlertCircle, PackageSearch, Loader2, Check
} from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { useUiStore } from '../../stores/ui.store';

export default function QCOutbound() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [qcData, setQcData] = useState([]);

  useEffect(() => {
    fetchOrder();
  }, [id]);

  const fetchOrder = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getDeliveryOrderById(id);
      setOrder(data);
      setQcData(data.items.map(item => ({
        id: item.id,
        result: item.qc_result || 'PASSED',
        reason: item.qc_failure_reason || '',
      })));
    } catch {
      addToast('Không tìm thấy đơn', 'error');
      navigate('/outbound/delivery-orders');
    } finally {
      setLoading(false);
    }
  };

  const updateQcItem = (itemId, field, value) => {
    setQcData(prev => prev.map(q => q.id === itemId ? { ...q, [field]: value } : q));
  };

  const handleConfirmQC = async () => {
    const invalid = qcData.some(q => q.result === 'FAILED' && !q.reason);
    if (invalid) { addToast('Vui lòng nhập lý do cho các sản phẩm KHÔNG ĐẠT', 'error'); return; }
    setSubmitting(true);
    try {
      await outboundService.confirmQCOutbound(id, { items: qcData });
      addToast('Hoàn tất QC xuất kho', 'success');
      navigate(`/outbound/delivery-orders/${id}`);
    } catch (error) {
      addToast(error.message || 'Lỗi khi hoàn tất QC', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const allChecked = qcData.length > 0;
  const failCount = qcData.filter(q => q.result === 'FAILED').length;

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
      {/* Page Header */}
      <div className="flex items-start gap-4">
        <button
          onClick={() => navigate(`/outbound/delivery-orders/${id}`)}
          className="mt-1 p-1.5 hover:bg-zinc-200 rounded-full transition-colors text-shade-50 hover:text-ink shrink-0"
        >
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Vận hành / Xuất kho / QC Outbound
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Kiểm tra QC: {order.do_number}
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Đại lý: <span className="font-semibold text-ink">{order.dealer_name}</span>
          </p>
        </div>
      </div>

      {/* Summary Bar */}
      {failCount > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center gap-3">
          <AlertCircle className="w-5 h-5 text-red-600 shrink-0" />
          <p className="text-sm text-red-700 font-medium">
            <span className="font-bold">{failCount}</span> sản phẩm không đạt QC. Vui lòng nhập lý do cho từng sản phẩm bên dưới.
          </p>
        </div>
      )}

      {/* QC Checklist */}
      <div className="bg-white rounded-lg border border-hairline-light shadow-sm overflow-hidden card-premium">
        <div className="px-6 py-4 bg-zinc-50 border-b border-hairline-light flex items-center gap-2">
          <PackageSearch className="w-4 h-4 text-shade-50" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-shade-60">
            Danh sách sản phẩm kiểm tra ({order.items.length} mặt hàng)
          </h3>
        </div>

        <div className="divide-y divide-hairline-light">
          {order.items.map((item) => {
            const qc = qcData.find(q => q.id === item.id);
            if (!qc) return null;
            const isPassed = qc.result === 'PASSED';
            const isFailed = qc.result === 'FAILED';

            return (
              <div key={item.id} className={`p-6 transition-colors ${isFailed ? 'bg-red-50/50' : isPassed ? 'bg-white' : 'bg-zinc-50'}`}>
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-start gap-4">
                  <div className="flex-1">
                    <h4 className="text-sm font-bold text-ink">{item.product_name}</h4>
                    <p className="text-xs text-shade-40 mt-0.5 font-mono">
                      SKU: {item.sku}
                    </p>
                    <p className="text-xs text-shade-50 mt-1">
                      Số lượng yêu cầu: <span className="font-semibold text-ink">{item.requested_qty}</span>
                      {item.issued_qty != null && (
                        <> · Đã soạn: <span className="font-semibold text-ink">{item.issued_qty}</span></>
                      )}
                    </p>
                    {item.serial_number && (
                      <p className="text-xs text-shade-50 mt-0.5">S/N: <span className="font-mono text-ink">{item.serial_number}</span></p>
                    )}
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => { updateQcItem(item.id, 'result', 'PASSED'); updateQcItem(item.id, 'reason', ''); }}
                      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-pill border text-xs font-semibold transition-colors ${
                        isPassed
                          ? 'bg-aloe-10 border-emerald-300 text-emerald-900'
                          : 'bg-white border-hairline-light text-shade-50 hover:bg-zinc-50'
                      }`}
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" /> Đạt QC
                    </button>
                    <button
                      onClick={() => updateQcItem(item.id, 'result', 'FAILED')}
                      className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-pill border text-xs font-semibold transition-colors ${
                        isFailed
                          ? 'bg-red-50 border-red-300 text-red-700'
                          : 'bg-white border-hairline-light text-shade-50 hover:bg-zinc-50'
                      }`}
                    >
                      <AlertCircle className="w-3.5 h-3.5" /> Không đạt
                    </button>
                  </div>
                </div>

                {isFailed && (
                  <div className="mt-4">
                    <label className="block text-xs font-bold text-red-700 mb-1.5 flex items-center gap-1">
                      <AlertCircle className="w-3 h-3" />
                      Lý do không đạt QC (bắt buộc)
                    </label>
                    <input
                      type="text"
                      className="w-full text-input text-xs border-red-300 focus:border-red-500"
                      placeholder="Ghi rõ lý do lỗi (móp méo, trầy xước, sai mã...)"
                      value={qc.reason}
                      onChange={(e) => updateQcItem(item.id, 'reason', e.target.value)}
                    />
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <div className="px-6 py-4 border-t border-hairline-light bg-zinc-50 flex justify-between items-center gap-3">
          <button
            onClick={() => navigate(`/outbound/delivery-orders/${id}`)}
            className="btn-pill btn-pill-outline-light text-xs"
          >
            Hủy bỏ
          </button>
          <button
            onClick={handleConfirmQC}
            disabled={!allChecked || submitting}
            className="btn-pill btn-pill-aloe text-xs py-1.5 px-4 font-bold disabled:opacity-50 flex items-center gap-1.5"
          >
            {submitting ? (
              <><Loader2 className="w-3.5 h-3.5 animate-spin" /> Đang gửi...</>
            ) : (
              <><Check className="w-3.5 h-3.5" /> Gửi kết quả QC</>
            )}
          </button>
        </div>
      </div>
    </div>
  );
}
