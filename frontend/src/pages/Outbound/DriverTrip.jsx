import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  Calendar,
  Camera,
  CheckCircle2,
  Loader2,
  MapPin,
  Play,
  Truck,
  User,
  X,
} from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import OTPInput from '../../components/warehouse/OTPInput';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';

const DELIVERY_STATUS_MAP = {
  WAREHOUSE_APPROVED: { label: 'Chờ giao', color: 'bg-amber-50 text-amber-700 border-amber-200' },
  IN_TRANSIT: { label: 'Đang giao', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: 'Đã giao', color: 'bg-emerald-50 text-emerald-900 border-emerald-300' },
  FAILED: { label: 'Thất bại', color: 'bg-red-50 text-red-700 border-red-200' },
  RETURNED: { label: 'Hoàn trả', color: 'bg-orange-50 text-orange-700 border-orange-200' },
};

const StatusBadge = ({ status }) => {
  const base = 'text-[10px] font-semibold px-2 py-0.5 rounded-full border uppercase tracking-wider whitespace-nowrap';
  const { label, color } = DELIVERY_STATUS_MAP[status] ?? { label: status, color: 'bg-zinc-100 text-zinc-800 border-zinc-200' };
  return <span className={`${base} ${color}`}>{label}</span>;
};

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
  return <span className={`font-mono font-bold text-sm ${remaining < 60 ? 'text-red-600' : 'text-shade-50'}`}>{mm}:{ss}</span>;
}

export default function DriverTrip() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();
  const { user } = useAuthStore();

  const [trip, setTrip] = useState(null);
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
      setLoading(false);
      return;
    }
    fetchTrip(id);
  }, [id]);

  const fetchTrip = async (tripId) => {
    setLoading(true);
    try {
      const data = await outboundService.getTripById(tripId);
      setTrip(data);
    } catch (error) {
      addToast(error.message || 'Không tìm thấy chuyến xe', 'error');
      navigate('/outbound/driver/trips');
    } finally {
      setLoading(false);
    }
  };

  const handleDepart = async () => {
    setSubmitting(true);
    try {
      await outboundService.departTrip(trip.id);
      addToast('Chuyến xe đã xuất phát', 'success');
      fetchTrip(trip.id);
    } catch (error) {
      addToast(error.message || 'Lỗi khi xác nhận xuất phát', 'error');
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
    const isDriver = user?.role === 'DRIVER';
    return (
      <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
        <Truck className="w-12 h-12 text-shade-30 mx-auto mb-4" />
        <h3 className="text-lg font-bold mb-1">{isDriver ? 'Cần mở chuyến đã được gán' : 'Không có chuyến được mở'}</h3>
        <p className="text-sm text-shade-50">
          Backend hiện tại chỉ hỗ trợ màn hình driver theo route có trip id. Hãy mở chuyến từ danh sách điều phối.
        </p>
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

  const deliveredCount = trip.delivery_orders?.filter((item) => item.delivery_status === 'COMPLETED').length ?? 0;
  const totalCount = trip.delivery_orders?.length ?? 0;

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-start gap-3 pt-1">
        <button onClick={() => navigate('/outbound/driver/trips')} className="mt-0.5 p-1.5 hover:bg-zinc-200 rounded-full transition-colors text-shade-50 hover:text-ink shrink-0">
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Giao hàng / Chuyến của tôi</span>
          <h1 className="text-xl font-display font-semibold tracking-tight">{trip.trip_number}</h1>
          <p className="text-xs text-shade-50 mt-0.5">{deliveredCount}/{totalCount} điểm đã giao</p>
        </div>
      </div>

      <div className="flex flex-col lg:flex-row gap-6 items-start">
        <div className="w-full lg:w-80 shrink-0 bg-white rounded-lg border border-hairline-light shadow-sm p-5 card-premium">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xs font-bold uppercase tracking-wider text-shade-40">Thông tin chuyến</h2>
            <StatusBadge status={trip.status} />
          </div>
          <div className="space-y-2 text-xs mb-4">
            <p className="flex items-center gap-2 text-shade-50"><Truck className="w-3.5 h-3.5 text-shade-40" /> Xe: <span className="font-semibold text-ink">{trip.vehicle_plate || '-'}</span></p>
            <p className="flex items-center gap-2 text-shade-50"><User className="w-3.5 h-3.5 text-shade-40" /> Tài xế: <span className="font-semibold text-ink">{trip.driver_name || trip.driver_id}</span></p>
            <p className="flex items-center gap-2 text-shade-50"><Calendar className="w-3.5 h-3.5 text-shade-40" /> Ngày: <span className="font-semibold text-ink">{trip.planned_date ? new Date(trip.planned_date).toLocaleDateString('vi-VN') : '-'}</span></p>
          </div>

          {trip.status === 'PLANNED' && (
            <button onClick={handleDepart} disabled={submitting} className="w-full btn-pill btn-pill-primary flex items-center justify-center gap-2 py-2.5 text-sm disabled:opacity-50">
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
              Xác nhận xuất phát
            </button>
          )}
        </div>

        <div className="flex-1 flex flex-col gap-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-shade-40">Danh sách điểm giao ({totalCount} điểm)</h3>
          {trip.delivery_orders?.map((doItem, index) => {
            const isDelivered = doItem.delivery_status === 'COMPLETED';
            const isFailed = doItem.delivery_status === 'FAILED';
            const isPending = !isDelivered && !isFailed;

            return (
              <div key={`${doItem.do_id}-${index}`} className={`rounded-lg border overflow-hidden ${isDelivered ? 'bg-emerald-50 border-emerald-300' : isFailed ? 'bg-red-50 border-red-200' : 'bg-white border-hairline-light shadow-sm'}`}>
                <div className="p-4 flex gap-3">
                  <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 font-bold text-sm ${isDelivered ? 'bg-emerald-600 text-white' : isFailed ? 'bg-red-500 text-white' : 'bg-ink text-white'}`}>
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
                        <button onClick={() => { setActiveDO(doItem); setModalType('DELIVER'); }} className="flex-1 py-2.5 bg-ink text-white rounded-full text-sm font-semibold hover:bg-shade-70 active:scale-95 transition-all">
                          Giao hàng (OTP)
                        </button>
                        <button onClick={() => { setActiveDO(doItem); setModalType('FAIL'); }} className="px-3.5 py-2.5 bg-red-50 text-red-700 border border-red-200 rounded-full hover:bg-red-100 active:scale-95 transition-all" title="Báo giao thất bại">
                          <AlertTriangle className="w-4 h-4" />
                        </button>
                      </div>
                    )}

                    {isFailed && doItem.failure_reason && <p className="text-xs text-red-600 mt-2 flex items-center gap-1"><X className="w-3.5 h-3.5 shrink-0" /> Thất bại: {doItem.failure_reason}</p>}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {activeDO && modalType === 'DELIVER' && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white w-full sm:max-w-md rounded-t-2xl sm:rounded-xl max-h-[90vh] flex flex-col shadow-2xl">
            <div className="px-5 py-4 border-b border-hairline-light flex items-center justify-between">
              <h3 className="text-base font-bold text-ink">Xác nhận giao hàng</h3>
              <button onClick={closeModal} className="p-1.5 hover:bg-zinc-100 rounded-full text-shade-50 hover:text-ink transition-colors"><X className="w-5 h-5" /></button>
            </div>

            <div className="p-5 overflow-y-auto flex-1 flex flex-col gap-5">
              <div className="bg-indigo-50 border border-indigo-100 rounded-lg p-3 text-sm text-indigo-800">
                Đơn <strong className="font-bold">{activeDO.do_number}</strong>
              </div>

              <div>
                <label className="block text-xs font-bold text-shade-60 mb-2 uppercase tracking-wider">Ảnh hàng hóa sau giao *</label>
                <input type="file" accept="image/*" onChange={(event) => setGoodsImage(event.target.files?.[0] || null)} className="text-input text-xs w-full" />
                {goodsImage && <p className="text-[11px] text-shade-50 mt-1 flex items-center gap-1"><Camera className="w-3 h-3" /> {goodsImage.name}</p>}
              </div>

              <div>
                <label className="block text-xs font-bold text-shade-60 mb-2 uppercase tracking-wider">Ảnh chữ ký/POD *</label>
                <input type="file" accept="image/*" onChange={(event) => setSignDocumentImage(event.target.files?.[0] || null)} className="text-input text-xs w-full" />
                {signDocumentImage && <p className="text-[11px] text-shade-50 mt-1 flex items-center gap-1"><Camera className="w-3 h-3" /> {signDocumentImage.name}</p>}
              </div>

              <textarea className="text-input text-sm h-20 resize-none" placeholder="Ghi chú giao hàng..." value={notes} onChange={(event) => setNotes(event.target.value)} />

              <div className="border-t border-hairline-light pt-5">
                <label className="block text-xs font-bold text-shade-60 mb-3 uppercase tracking-wider">Xác thực OTP từ đại lý</label>
                {!otpSent ? (
                  <button onClick={handleUploadPodAndRequestOTP} disabled={submitting} className="w-full py-3.5 bg-ink text-white font-bold rounded-full text-sm hover:bg-shade-70 disabled:opacity-50 active:scale-95 transition-all">
                    {submitting ? <Loader2 className="w-4 h-4 animate-spin mx-auto" /> : 'Tải POD và gửi mã OTP'}
                  </button>
                ) : (
                  <div className="flex flex-col gap-4">
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-shade-50">Nhập mã 6 số</p>
                      <OTPCountdown expiresAt={otpExpiresAt} onExpired={() => setOtpExpired(true)} />
                    </div>
                    <OTPInput length={6} onComplete={handleVerifyOTP} />
                    {otpExpired && <p className="text-xs text-red-600 text-center font-medium">Mã OTP đã hết hạn.</p>}
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
        <div className="fixed inset-0 bg-black/60 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white w-full sm:max-w-md rounded-t-2xl sm:rounded-xl max-h-[90vh] flex flex-col shadow-2xl">
            <div className="px-5 py-4 border-b border-hairline-light flex items-center justify-between">
              <h3 className="text-base font-bold text-red-600 flex items-center gap-2"><AlertTriangle className="w-5 h-5" /> Báo giao thất bại</h3>
              <button onClick={closeModal} className="p-1.5 hover:bg-zinc-100 rounded-full text-shade-50 hover:text-ink transition-colors"><X className="w-5 h-5" /></button>
            </div>

            <div className="p-5 overflow-y-auto flex-1 flex flex-col gap-5">
              <div className="bg-red-50 border border-red-100 rounded-lg p-3 text-sm text-red-800">Đơn <strong>{activeDO.do_number}</strong></div>
              <div>
                <label className="block text-xs font-bold text-shade-60 mb-2 uppercase tracking-wider">Lý do thất bại *</label>
                <textarea className="w-full text-input text-sm h-28 resize-none" placeholder="Đại lý đóng cửa, từ chối nhận..." value={failureReason} onChange={(event) => setFailureReason(event.target.value)} />
              </div>
              <textarea className="text-input text-sm h-20 resize-none" placeholder="Ghi chú bổ sung..." value={notes} onChange={(event) => setNotes(event.target.value)} />
              <button onClick={handleFailDelivery} disabled={!failureReason.trim() || submitting} className="w-full py-3.5 bg-red-600 text-white font-bold rounded-full text-sm hover:bg-red-700 disabled:opacity-50 active:scale-95 transition-all">
                {submitting ? <Loader2 className="w-4 h-4 animate-spin mx-auto" /> : 'Xác nhận thất bại'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
