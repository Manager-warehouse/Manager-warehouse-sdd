import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  Calendar,
  CheckCircle2,
  Loader2,
  MapPin,
  Play,
  Truck,
  User,
  X,
} from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { interWarehouseTransferService } from '../../services/inter-warehouse-transfer.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import OTPInput from '../../components/warehouse/OTPInput';
import Button from '../../components/common/Button';
import Badge from '../../components/common/Badge';
import PhotoCaptureInput from '../../components/common/PhotoCaptureInput';

const DELIVERY_STATUS_MAP = {
  WAREHOUSE_APPROVED: { label: 'Chờ giao', color: 'bg-warning-50 text-warning-700 border-warning-200' },
  IN_TRANSIT: { label: 'Đang giao', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: 'Đã giao', color: 'bg-success-50 text-success-900 border-success-300' },
  FAILED: { label: 'Thất bại', color: 'bg-danger-50 text-danger-700 border-danger-200' },
  RETURNED: { label: 'Hoàn trả', color: 'bg-orange-50 text-orange-700 border-orange-200' },
};

const StatusBadge = ({ status }) => {
  const { label, color } = DELIVERY_STATUS_MAP[status] ?? { label: status, color: 'bg-canvas-cream text-shade-70 border-hairline-light' };
  return <Badge size="sm" colorClassName={color}>{label}</Badge>;
};

const TRANSFER_TRIP_PREFIX = 'transfer-';

const transferDriverStatus = (status) => {
  if (status === 'APPROVED') return 'PLANNED';
  if (status === 'COMPLETED' || status === 'COMPLETED_WITH_VARIANCE') return 'COMPLETED';
  return status || 'PLANNED';
};

const toTransferDriverTrip = (transfer = {}) => ({
  id: `${TRANSFER_TRIP_PREFIX}${transfer.id}`,
  transferId: transfer.id,
  tripId: transfer.tripId,
  type: 'TRANSFER',
  trip_number: transfer.tripNumber || transfer.transferNumber,
  status: transferDriverStatus(transfer.status),
  sourceWarehouseCode: transfer.sourceWarehouseCode,
  destinationWarehouseCode: transfer.destinationWarehouseCode,
  vehicle_plate: transfer.vehiclePlate || transfer.trip?.vehiclePlate || '',
  driver_name: transfer.driverName || transfer.trip?.driverName || '',
  planned_date: transfer.tripPlannedStartAt || transfer.plannedDate || transfer.documentDate,
  planned_start_at: transfer.tripPlannedStartAt || transfer.plannedDate || transfer.documentDate,
  planned_end_at: transfer.tripPlannedEndAt || null,
  total_weight_kg: Number(transfer.totalWeightKg || transfer.trip?.totalWeightKg || 0),
  tripWarningActive: transfer.tripWarningActive,
  tripOverdue: transfer.tripOverdue,
  tripWarningMessage: transfer.tripWarningMessage,
  items: (transfer.items || []).map((item) => ({
    id: item.id,
    productSku: item.productSku,
    productName: item.productName,
    plannedQty: item.plannedQty,
    sentQty: item.sentQty,
  })),
  delivery_orders: [],
});

function OTPCountdown({ expiresAt, onExpired }) {
  const [remaining, setRemaining] = useState(0);

  useEffect(() => {
    if (!expiresAt) return undefined;
    const update = () => {
      const seconds = Math.max(0, Math.floor((new Date(expiresAt) - Date.now()) / 1000));
      setRemaining(seconds);
      if (seconds === 0) onExpired?.();
    };
    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [expiresAt, onExpired]);

  if (!expiresAt) return null;
  const mm = String(Math.floor(remaining / 60)).padStart(2, '0');
  const ss = String(remaining % 60).padStart(2, '0');
  return <span className={`font-mono font-bold text-sm ${remaining < 60 ? 'text-danger-600' : 'text-shade-50'}`}>{mm}:{ss}</span>;
}

export default function DriverTrip() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();
  const { user } = useAuthStore();

  const [trip, setTrip] = useState(null);
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [activeDO, setActiveDO] = useState(null);
  const [modalType, setModalType] = useState(null);
  const [otpSent, setOtpSent] = useState(false);
  const [otpExpiresAt, setOtpExpiresAt] = useState(null);
  const [otpExpired, setOtpExpired] = useState(false);
  const [goodsImage, setGoodsImage] = useState(null);
  const [signDocumentImage, setSignDocumentImage] = useState(null);
  const [failureReason, setFailureReason] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!id) {
      fetchTrips();
      return;
    }
    fetchTrip(id);
  }, [id]);

  const fetchTrips = async () => {
    setLoading(true);
    const loadedTrips = [];
    let loadedAnySource = false;
    try {
      const isDriver = user?.role === 'DRIVER';
      const outboundTrips = isDriver
        ? await outboundService.getMyTrips()
        : await outboundService.getTrips(null, {});
      loadedTrips.push(...outboundTrips.map((tripRow) => ({ ...tripRow, type: tripRow.type || 'OUTBOUND' })));
      loadedAnySource = true;
    } catch {
      // Outbound trips are optional on this screen while transfer trips use the transfer API.
    }

    try {
      const transfers = await interWarehouseTransferService.getTransfers();
      const transferTrips = transfers
        .filter((transfer) => transfer.tripId && Number(transfer.driverUserId || 0) === Number(user?.id || 0))
        .map(toTransferDriverTrip);
      loadedTrips.push(...transferTrips);
      loadedAnySource = true;
    } catch {
      // Surface a toast only if both trip sources failed.
    }

    try {
      if (!loadedAnySource) {
        addToast('Lỗi khi tải danh sách chuyến xe', 'error');
      }
      setTrips(loadedTrips.sort((a, b) => new Date(b.planned_date || 0) - new Date(a.planned_date || 0)));
    } finally {
      setLoading(false);
    }
  };

  const fetchTrip = async (tripId) => {
    setLoading(true);
    try {
      if (String(tripId).startsWith(TRANSFER_TRIP_PREFIX)) {
        const transferId = Number(String(tripId).replace(TRANSFER_TRIP_PREFIX, ''));
        const data = await interWarehouseTransferService.getTransferById(transferId);
        if (Number(data.driverUserId || 0) !== Number(user?.id || 0)) {
          throw new Error('TRANSFER_TRIP_NOT_ASSIGNED');
        }
        setTrip(toTransferDriverTrip(data));
      } else {
        const data = await outboundService.getTripById(tripId);
        setTrip({ ...data, type: data.type || 'OUTBOUND' });
      }
    } catch {
      addToast('Không tìm thấy chuyến xe', 'error');
      navigate('/outbound/driver/trips');
    } finally {
      setLoading(false);
    }
  };

  const handleDepart = async () => {
    setSubmitting(true);
    try {
      if (trip.type === 'TRANSFER') {
        await interWarehouseTransferService.departTransfer(trip.transferId);
      } else {
        await outboundService.departTrip(trip.id);
      }
      addToast('Chuyến xe đã xuất phát!', 'success');
      fetchTrip(trip.id);
    } catch (error) {
      const messages = {
        SENT_QTY_REQUIRED: 'Thủ kho nguồn chưa xếp đủ hàng lên xe',
        ASSIGNED_DRIVER_REQUIRED: 'Chuyến này không được phân công cho tài xế hiện tại',
        TRANSFER_TRIP_REQUIRED: 'Phiếu điều chuyển chưa được lập chuyến',
      };
      addToast(messages[error.message] || error.message || 'Lỗi khi xác nhận xuất phát', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUploadPodAndRequestOTP = async () => {
    if (!goodsImage || !signDocumentImage) {
      addToast('Vui lòng tải đủ ảnh hàng hóa và POD', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await outboundService.uploadPodEvidence(trip.id, activeDO.do_id, { goodsImage, signDocumentImage, notes });
      const otp = await outboundService.requestOTP(trip.id, activeDO.do_id, otpSent);
      setOtpExpiresAt(otp.expiresAt || otp.expires_at || new Date(Date.now() + 5 * 60000).toISOString());
      setOtpSent(true);
      setOtpExpired(false);
      addToast('Đã gửi mã OTP đến người nhận', 'success');
    } catch (error) {
      addToast(error.message || 'Lỗi khi gửi OTP', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleVerifyOTP = async (code) => {
    setSubmitting(true);
    try {
      await outboundService.verifyOTPAndDeliver(trip.id, activeDO.do_id, code, notes);
      addToast('Giao hàng thành công', 'success');
      closeModal();
      fetchTrip(trip.id);
    } catch (error) {
      addToast(error.message || 'Mã OTP không đúng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleFailDelivery = async () => {
    if (!failureReason.trim()) {
      addToast('Vui lòng nhập lý do giao thất bại', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await outboundService.reportDeliveryFailure(trip.id, activeDO.do_id, failureReason.trim(), notes);
      addToast('Đã ghi nhận giao hàng thất bại', 'success');
      closeModal();
      fetchTrip(trip.id);
    } catch (error) {
      addToast(error.message || 'Lỗi khi ghi nhận thất bại', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const closeModal = () => {
    setActiveDO(null);
    setModalType(null);
    setOtpSent(false);
    setOtpExpiresAt(null);
    setOtpExpired(false);
    setGoodsImage(null);
    setSignDocumentImage(null);
    setFailureReason('');
    setNotes('');
  };

  if (!id) {
    return (
      <div className="flex flex-col gap-6">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Giao hàng</span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">Chuyến giao hàng của tôi</h1>
          <p className="text-xs text-shade-50 font-light mt-1">Các chuyến xe được phân công cho tài xế hiện tại.</p>
        </div>

        {loading ? (
          <div className="flex items-center justify-center p-20">
            <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
          </div>
        ) : !trips.length ? (
          <div className="bg-canvas-light rounded-lg border border-hairline-light p-12 text-center shadow-level-3">
            <Truck className="w-12 h-12 text-shade-30 mx-auto mb-4" />
            <h3 className="text-lg font-bold mb-1">Không có chuyến xe nào</h3>
            <p className="text-sm text-shade-50">Hiện tại bạn chưa được gán chuyến xe nào.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {trips.map((tripItem) => (
              <div
                key={tripItem.id || tripItem.trip_number}
                onClick={() => {
                  if (!tripItem.id) {
                    addToast('Chuyến xe chưa có mã nội bộ để mở chi tiết', 'warning');
                    return;
                  }
                  navigate(`/outbound/driver/trips/${tripItem.id}`);
                }}
                className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 hover:shadow-md transition-shadow cursor-pointer overflow-hidden"
              >
                <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center">
                  <span className="text-xs font-bold text-ink">{tripItem.trip_number}</span>
                  <StatusBadge status={tripItem.status} />
                </div>
                <div className="p-4 flex flex-col gap-2 text-xs">
                  <p className="flex items-center gap-2 text-shade-50"><Truck className="w-3.5 h-3.5 text-shade-40" /> Xe: <span className="font-semibold text-ink">{tripItem.vehicle_plate || '-'}</span></p>
                  <p className="flex items-center gap-2 text-shade-50"><Calendar className="w-3.5 h-3.5 text-shade-40" /> T.gian dự kiến: <span className="font-semibold text-ink">{tripItem.planned_start_at ? new Date(tripItem.planned_start_at).toLocaleString('vi-VN', { hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit', year: 'numeric' }) : '-'}</span></p>
                  <p className="flex items-center gap-2 text-shade-50"><MapPin className="w-3.5 h-3.5 text-shade-40" /> Điểm giao: <span className="font-semibold text-ink">{tripItem.delivery_orders?.length || 0}</span></p>
                  <p className="text-xs text-shade-50 pt-1">Tổng KL: <span className="font-semibold text-ink">{tripItem.total_weight_kg || 0} kg</span></p>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center p-20">
        <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
      </div>
    );
  }

  if (!trip) return null;

  const isTransferTrip = trip.type === 'TRANSFER';
  const deliveredCount = trip.delivery_orders?.filter(d => d.delivery_status === 'COMPLETED').length ?? 0;
  const totalCount = isTransferTrip ? trip.items.length : (trip.delivery_orders?.length ?? 0);
  const transferLoaded = !isTransferTrip || (
    trip.items.length > 0
    && trip.items.every((item) => (
      item.sentQty != null
      && Number(item.sentQty) === Number(item.plannedQty)
    ))
  );

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start gap-4">
        <button
          onClick={() => navigate('/outbound/driver/trips')}
          className="mt-1 p-1.5 hover:bg-canvas-cream rounded-full transition-colors text-shade-50 hover:text-ink shrink-0"
        >
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Giao hàng</span>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">{trip.trip_number}</h1>
            <StatusBadge status={trip.status} />
          </div>
          <p className="text-xs text-shade-50 font-light mt-1">
            {isTransferTrip ? `${trip.sourceWarehouseCode} → ${trip.destinationWarehouseCode}` : `${deliveredCount}/${totalCount} điểm đã giao`}
          </p>
        </div>
      </div>

      <div className="flex flex-col lg:flex-row gap-6 items-start">
        <div className="w-full lg:w-80 shrink-0 p-5 card-premium rounded-lg">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xs font-bold uppercase tracking-wider text-shade-40">Thông tin chuyến</h2>
          </div>
          <div className="flex flex-col gap-2 text-xs mb-4">
            <p className="flex items-center gap-2 text-shade-50"><Truck className="w-3.5 h-3.5 text-shade-40 shrink-0" /> Xe: <span className="font-semibold text-ink">{trip.vehicle_plate}</span></p>
            <p className="flex items-center gap-2 text-shade-50"><User className="w-3.5 h-3.5 text-shade-40 shrink-0" /> Tài xế: <span className="font-semibold text-ink">{trip.driver_name}</span></p>
            <p className="flex items-center gap-2 text-shade-50"><Calendar className="w-3.5 h-3.5 text-shade-40 shrink-0" /> Khởi hành: <span className="font-semibold text-ink">{new Date(trip.planned_date).toLocaleString('vi-VN')}</span></p>
            {trip.planned_end_at && (
              <p className="flex items-center gap-2 text-shade-50"><Calendar className="w-3.5 h-3.5 text-shade-40 shrink-0" /> Hạn giao: <span className="font-semibold text-ink">{new Date(trip.planned_end_at).toLocaleString('vi-VN')}</span></p>
            )}
          </div>
          {trip.tripWarningActive && (
            <div className={`mb-4 rounded-md border px-3 py-2 text-[11px] ${
              trip.tripOverdue
                ? 'border-danger-200 bg-danger-50 text-danger-700'
                : 'border-warning-200 bg-warning-50 text-warning-700'
            }`}>
              {trip.tripWarningMessage}
            </div>
          )}

          {trip.status === 'PLANNED' && (
            <>
              <button
                onClick={handleDepart}
                disabled={submitting || !transferLoaded}
                className="w-full btn-pill btn-pill-primary flex items-center justify-center gap-2 py-2.5 text-sm disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
                {transferLoaded ? 'Xác nhận xuất phát' : 'Chờ xếp hàng'}
              </button>
              {!transferLoaded && (
                <p className="mt-2 text-[11px] leading-relaxed text-warning-700">
                  Thủ kho nguồn cần xác nhận xếp đủ số lượng hàng lên xe trước khi tài xế xuất phát.
                </p>
              )}
            </>
          )}

          {/* Progress bar */}
          {!isTransferTrip && totalCount > 0 && (
            <div className="mt-4 pt-4 border-t border-hairline-light">
              <div className="flex justify-between text-[11px] text-shade-50 mb-2">
                <span>Tiến độ giao hàng</span>
                <span className="font-semibold text-ink">{deliveredCount}/{totalCount}</span>
              </div>
              <div className="w-full bg-canvas-cream rounded-full h-2">
                <div
                  className="bg-success-500 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${totalCount > 0 ? (deliveredCount / totalCount) * 100 : 0}%` }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Right: transfer lines or stops list */}
        <div className="flex-1 flex flex-col gap-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-shade-40">
          {isTransferTrip ? `Danh sách hàng điều chuyển (${totalCount} dòng)` : `Danh sách điểm giao (${totalCount} điểm)`}
        </h3>

        {isTransferTrip && trip.items.map((item) => (
          <div key={item.id} className="rounded-lg border border-hairline-light bg-canvas-light shadow-level-3 p-4">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h4 className="text-sm font-bold text-ink leading-tight">{item.productSku} <span className="font-normal text-shade-50">{item.productName}</span></h4>
                <p className="text-xs text-shade-50 mt-1">Kế hoạch: {item.plannedQty} · Xuất: {item.sentQty ?? '-'}</p>
              </div>
              <span className="text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase tracking-wider bg-warning-50 text-warning-700 border-warning-200">
                Điều chuyển
              </span>
            </div>
          </div>
        ))}

        {!isTransferTrip && trip.delivery_orders?.map((doItem, index) => {
            const isDelivered = doItem.delivery_status === 'COMPLETED';
            const isFailed = doItem.delivery_status === 'FAILED';
            const isPending = !isDelivered && !isFailed;

            return (
              <div key={`${doItem.do_id}-${index}`} className={`rounded-lg border overflow-hidden ${isDelivered ? 'bg-success-50 border-success-300' : isFailed ? 'bg-danger-50 border-danger-200' : 'bg-canvas-light border-hairline-light shadow-level-3'}`}>
                <div className="p-4 flex gap-3">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 font-bold text-sm ${isDelivered ? 'bg-success-600 text-white' : isFailed ? 'bg-danger-500 text-white' : 'bg-ink text-white'}`}>
                    {isDelivered ? <CheckCircle2 className="w-4 h-4" /> : index + 1}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex justify-between items-start gap-2">
                      <h4 className="text-sm font-bold text-ink leading-tight">{doItem.dealer_name || doItem.do_number}</h4>
                      <StatusBadge status={doItem.delivery_status} />
                    </div>
                    {doItem.dealer_address && <p className="text-xs text-shade-40 mt-1 flex items-start gap-1"><MapPin className="w-3.5 h-3.5 shrink-0 mt-0.5" />{doItem.dealer_address}</p>}
                    <p className="text-xs text-shade-40 mt-0.5 font-mono">{doItem.do_number}</p>

                    {isPending && trip.status === 'IN_TRANSIT' && (
                      <div className="flex gap-2 mt-4">
                        <Button variant="primary" className="flex-1" onClick={() => { setActiveDO(doItem); setModalType('DELIVER'); }}>
                          Giao hàng (OTP)
                        </Button>
                        <button onClick={() => { setActiveDO(doItem); setModalType('FAIL'); }} className="px-3.5 py-2.5 rounded-pill bg-danger-50 text-danger-700 border border-danger-200 hover:bg-danger-100 active:scale-95 transition-all" title="Báo giao thất bại">
                          <AlertTriangle className="w-4 h-4" />
                        </button>
                      </div>
                    )}

                    {isFailed && doItem.failure_reason && <p className="text-xs text-danger-600 mt-2 flex items-center gap-1"><X className="w-3.5 h-3.5 shrink-0" /> Thất bại: {doItem.failure_reason}</p>}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {activeDO && modalType === 'DELIVER' && (
        <div className="fixed inset-0 bg-canvas-night/60 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-canvas-light w-full sm:max-w-md rounded-t-2xl sm:rounded-xl max-h-[90vh] flex flex-col shadow-level-4">
            <div className="px-5 py-4 border-b border-hairline-light flex items-center justify-between">
              <h3 className="text-base font-bold text-ink">Xác nhận giao hàng</h3>
              <button onClick={closeModal} className="p-1.5 hover:bg-canvas-cream rounded-full text-shade-50 hover:text-ink transition-colors"><X className="w-5 h-5" /></button>
            </div>

            <div className="p-5 overflow-y-auto flex-1 flex flex-col gap-5">
              <div className="bg-indigo-50 border border-indigo-100 rounded-lg p-3 text-sm text-indigo-800">
                Đơn <strong className="font-bold">{activeDO.do_number}</strong>
              </div>

              <PhotoCaptureInput
                label="Ảnh hàng hóa sau giao"
                fileName={goodsImage?.name}
                onChange={(file) => setGoodsImage(file)}
                output="file"
                required
              />

              <PhotoCaptureInput
                label="Ảnh chữ ký/POD"
                fileName={signDocumentImage?.name}
                onChange={(file) => setSignDocumentImage(file)}
                output="file"
                required
              />

              <textarea className="text-input text-sm h-20 resize-none" placeholder="Ghi chú giao hàng..." value={notes} onChange={(event) => setNotes(event.target.value)} />

              <div className="border-t border-hairline-light pt-5">
                <label className="block text-xs font-bold text-shade-60 mb-3 uppercase tracking-wider">Xác thực OTP từ đại lý</label>
                {!otpSent ? (
                  <Button
                    variant="primary"
                    className="w-full py-3.5"
                    onClick={handleUploadPodAndRequestOTP}
                    disabled={submitting || !goodsImage || !signDocumentImage}
                    loading={submitting}
                  >
                    Tải POD và gửi mã OTP
                  </Button>
                ) : (
                  <div className="flex flex-col gap-4">
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-shade-50">Nhập mã 6 số</p>
                      <OTPCountdown expiresAt={otpExpiresAt} onExpired={() => setOtpExpired(true)} />
                    </div>
                    <OTPInput length={6} onComplete={handleVerifyOTP} />
                    {otpExpired && <p className="text-xs text-danger-600 text-center font-medium">Mã OTP đã hết hạn.</p>}
                    <button onClick={handleUploadPodAndRequestOTP} disabled={submitting || !otpExpired} className="text-sm text-ink font-semibold underline disabled:opacity-40 disabled:no-underline text-center">
                      Gửi lại OTP
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {activeDO && modalType === 'FAIL' && (
        <div className="fixed inset-0 bg-canvas-night/60 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-canvas-light w-full sm:max-w-md rounded-t-2xl sm:rounded-xl max-h-[90vh] flex flex-col shadow-level-4">
            <div className="px-5 py-4 border-b border-hairline-light flex items-center justify-between">
              <h3 className="text-base font-bold text-danger-600 flex items-center gap-2"><AlertTriangle className="w-5 h-5" /> Báo giao thất bại</h3>
              <button onClick={closeModal} className="p-1.5 hover:bg-canvas-cream rounded-full text-shade-50 hover:text-ink transition-colors"><X className="w-5 h-5" /></button>
            </div>

            <div className="p-5 overflow-y-auto flex-1 flex flex-col gap-5">
              <div className="bg-danger-50 border border-danger-100 rounded-lg p-3 text-sm text-danger-800">Đơn <strong>{activeDO.do_number}</strong></div>
              <div>
                <label className="block text-xs font-bold text-shade-60 mb-2 uppercase tracking-wider">Lý do thất bại *</label>
                <textarea className="w-full text-input text-sm h-28 resize-none" placeholder="Đại lý đóng cửa, từ chối nhận..." value={failureReason} onChange={(event) => setFailureReason(event.target.value)} />
              </div>
              <textarea className="text-input text-sm h-20 resize-none" placeholder="Ghi chú bổ sung..." value={notes} onChange={(event) => setNotes(event.target.value)} />
              <button onClick={handleFailDelivery} disabled={!failureReason.trim() || submitting} className="w-full py-3.5 bg-danger-600 text-white font-bold rounded-pill text-sm hover:bg-danger-700 disabled:opacity-50 active:scale-95 transition-all">
                {submitting ? <Loader2 className="w-4 h-4 animate-spin mx-auto" /> : 'Xác nhận thất bại'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
