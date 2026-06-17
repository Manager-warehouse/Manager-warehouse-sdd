import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  MapPin, Phone, CheckCircle2, AlertTriangle, X, Camera,
  Truck, Calendar, Package, Play, ArrowLeft, Loader2, Search, User
} from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { useUiStore } from '../../stores/ui.store';
import { useAuthStore } from '../../stores/auth.store';
import OTPInput from '../../components/warehouse/OTPInput';

const DO_DELIVERY_STATUS_MAP = {
  READY_TO_SHIP: { label: 'Chờ giao',       color: 'bg-amber-50 text-amber-700 border-amber-200' },
  IN_TRANSIT:    { label: 'Đang giao',       color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  DELIVERED:     { label: 'Đã giao',         color: 'bg-aloe-10 text-emerald-900 border-emerald-300' },
  FAILED:        { label: 'Thất bại',        color: 'bg-red-50 text-red-700 border-red-200' },
};

const StatusBadge = ({ status }) => {
  const base = 'text-[10px] font-semibold px-2 py-0.5 rounded-full border uppercase tracking-wider whitespace-nowrap';
  const { label, color } = DO_DELIVERY_STATUS_MAP[status] ?? { label: status, color: 'bg-zinc-100 text-zinc-800 border-zinc-200' };
  return <span className={`${base} ${color}`}>{label}</span>;
};

function OTPCountdown({ expiresAt, onExpired }) {
  const [remaining, setRemaining] = useState(0);

  useEffect(() => {
    if (!expiresAt) return;
    const update = () => {
      const secs = Math.max(0, Math.floor((new Date(expiresAt) - Date.now()) / 1000));
      setRemaining(secs);
      if (secs === 0) onExpired?.();
    };
    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [expiresAt]);

  if (!expiresAt) return null;
  const mm = String(Math.floor(remaining / 60)).padStart(2, '0');
  const ss = String(remaining % 60).padStart(2, '0');
  return (
    <span className={`font-mono font-bold text-sm ${remaining < 60 ? 'text-red-600' : 'text-shade-50'}`}>
      {mm}:{ss}
    </span>
  );
}

export default function DriverTrip() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();
  const { user } = useAuthStore();

  // List state (when no id)
  const [trips, setTrips] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');

  // Detail state (when id is present)
  const [trip, setTrip] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);

  // POD flow
  const [activeDO, setActiveDO] = useState(null);
  const [modalType, setModalType] = useState(null); // 'DELIVER' | 'FAIL'
  const [otpSent, setOtpSent] = useState(false);
  const [otpExpiresAt, setOtpExpiresAt] = useState(null);
  const [otpExpired, setOtpExpired] = useState(false);
  const [photos, setPhotos] = useState([]);
  const [failureReason, setFailureReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (id) {
      fetchTrip(id);
    } else {
      fetchDriverTrips();
    }
  }, [id]);

  const fetchDriverTrips = async () => {
    setListLoading(true);
    try {
      const allTrips = await outboundService.getTrips(null, {});
      // Drivers only see their own trips; other roles (admin, dispatcher) see all
      const isDriver = user?.role === 'DRIVER';
      const myTrips = isDriver
        ? allTrips.filter(t => t.driver_id === user.id)
        : allTrips;
      setTrips(myTrips);
    } catch {
      addToast('Lỗi khi tải danh sách chuyến xe', 'error');
    } finally {
      setListLoading(false);
    }
  };

  const fetchTrip = async (tripId) => {
    setDetailLoading(true);
    try {
      const data = await outboundService.getTripById(tripId);
      setTrip(data);
    } catch {
      addToast('Không tìm thấy chuyến xe', 'error');
      navigate('/outbound/driver/trips');
    } finally {
      setDetailLoading(false);
    }
  };

  const handleDepart = async () => {
    setSubmitting(true);
    try {
      await outboundService.departTrip(trip.id);
      addToast('Chuyến xe đã xuất phát!', 'success');
      fetchTrip(trip.id);
    } catch {
      addToast('Lỗi khi xác nhận xuất phát', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRequestOTP = async () => {
    setSubmitting(true);
    try {
      await outboundService.requestOTP(trip.id, activeDO.do_id);
      const expiresAt = new Date(Date.now() + 5 * 60000).toISOString();
      setOtpExpiresAt(expiresAt);
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
      const uploadedPhotos = photos.length > 0 ? photos : ['mock_photo_1.jpg'];
      await outboundService.verifyOTPAndDeliver(trip.id, activeDO.do_id, code, uploadedPhotos);
      addToast('Giao hàng thành công!', 'success');
      closeModal();
      fetchTrip(trip.id);
    } catch (error) {
      addToast(error.message || 'Mã OTP không đúng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleFailDelivery = async () => {
    if (!failureReason) { addToast('Vui lòng nhập lý do giao thất bại', 'error'); return; }
    setSubmitting(true);
    try {
      await outboundService.reportDeliveryFailure(trip.id, activeDO.do_id, failureReason, photos);
      addToast('Đã ghi nhận giao hàng thất bại', 'success');
      closeModal();
      fetchTrip(trip.id);
    } catch (error) {
      addToast(error.message || 'Lỗi khi ghi nhận', 'error');
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
    setPhotos([]);
    setFailureReason('');
  };

  const handleMockTakePhoto = () => {
    if (photos.length >= 3) { addToast('Tối đa 3 hình ảnh', 'warning'); return; }
    setPhotos([...photos, `photo_${Date.now()}.jpg`]);
  };

  // ─── List View (no id) ───────────────────────────────────────────────────────
  if (!id) {
    const TRIP_STATUS_MAP = {
      PLANNED:    { label: 'Lên kế hoạch', color: 'bg-zinc-100 text-zinc-800 border-zinc-200' },
      IN_TRANSIT: { label: 'Đang giao',    color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
      COMPLETED:  { label: 'Hoàn thành',   color: 'bg-aloe-10 text-emerald-900 border-emerald-300' },
      CANCELLED:  { label: 'Đã hủy',       color: 'bg-red-50 text-red-700 border-red-200' },
    };
    const getTripBadge = (status) => {
      const base = 'text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase tracking-wider whitespace-nowrap';
      const { label, color } = TRIP_STATUS_MAP[status] ?? { label: status, color: 'bg-zinc-100 text-zinc-800 border-zinc-200' };
      return <span className={`${base} ${color}`}>{label}</span>;
    };

    const filteredTrips = trips.filter(t => {
      const matchesStatus = statusFilter === 'ALL' || t.status === statusFilter;
      const q = search.toLowerCase();
      const matchesSearch = !search || t.trip_number?.toLowerCase().includes(q) || t.vehicle_plate?.toLowerCase().includes(q) || t.driver_name?.toLowerCase().includes(q);
      return matchesStatus && matchesSearch;
    });

    return (
      <div className="flex flex-col gap-6">
        {/* Page Header */}
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Giao hàng / Chuyến của tôi
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Chuyến xe của tôi
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Các chuyến xe được phân công cho bạn.
          </p>
        </div>

        {/* Filters */}
        <div className="bg-white rounded-lg border border-hairline-light p-4 shadow-sm flex flex-col md:flex-row gap-4 items-center justify-between">
          <div className="relative w-full md:w-80">
            <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-shade-40" />
            <input
              type="text"
              placeholder="Tìm mã chuyến, biển số xe..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full text-input pl-10"
            />
          </div>
          <div className="flex items-center gap-2 w-full md:w-auto justify-end">
            <span className="text-xs font-semibold text-shade-50">Trạng thái:</span>
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="text-input text-xs py-1.5">
              <option value="ALL">Tất cả</option>
              <option value="PLANNED">Lên kế hoạch</option>
              <option value="IN_TRANSIT">Đang giao</option>
              <option value="COMPLETED">Hoàn thành</option>
            </select>
          </div>
        </div>

        {listLoading ? (
          <div className="flex items-center justify-center p-20">
            <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
          </div>
        ) : filteredTrips.length === 0 ? (
          <div className="bg-white rounded-lg border border-hairline-light p-12 text-center shadow-sm">
            <Truck className="w-12 h-12 text-shade-30 mx-auto mb-4" />
            <h3 className="text-lg font-bold mb-1">Không tìm thấy chuyến xe nào</h3>
            <p className="text-sm text-shade-50">Thay đổi bộ lọc hoặc chờ phân công chuyến mới.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredTrips.map(t => (
              <div key={t.id} className="bg-white rounded-lg border border-hairline-light shadow-sm hover:shadow-md transition-shadow card-premium overflow-hidden cursor-pointer" onClick={() => navigate(`/outbound/driver/trips/${t.id}`)}>
                <div className="p-4 border-b border-hairline-light bg-zinc-50 flex justify-between items-center">
                  <span className="text-xs font-bold text-ink">{t.trip_number}</span>
                  {getTripBadge(t.status)}
                </div>
                <div className="p-4 space-y-2">
                  <div className="flex items-center gap-2 text-xs">
                    <Truck className="w-3.5 h-3.5 text-shade-40 shrink-0" />
                    <span className="text-shade-50">Xe:</span>
                    <span className="font-semibold text-ink">{t.vehicle_plate}</span>
                  </div>
                  <div className="flex items-center gap-2 text-xs">
                    <User className="w-3.5 h-3.5 text-shade-40 shrink-0" />
                    <span className="text-shade-50">Tài xế:</span>
                    <span className="font-semibold text-ink">{t.driver_name}</span>
                  </div>
                  <div className="flex items-center gap-2 text-xs">
                    <Calendar className="w-3.5 h-3.5 text-shade-40 shrink-0" />
                    <span className="text-shade-50">Ngày giao:</span>
                    <span className="font-semibold text-ink">{new Date(t.planned_date).toLocaleDateString('vi-VN')}</span>
                  </div>
                </div>
                <div className="px-4 pb-4">
                  <button className="w-full inline-flex items-center justify-center gap-1.5 rounded-full border border-ink bg-canvas-light text-ink hover:bg-zinc-100 px-3 py-1.5 text-xs font-semibold transition-colors">
                    Xem chi tiết
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    );
  }

  // ─── Detail View (with id) ───────────────────────────────────────────────────
  if (detailLoading) {
    return (
      <div className="flex items-center justify-center p-20">
        <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
      </div>
    );
  }
  if (!trip) return null;

  const deliveredCount = trip.delivery_orders?.filter(d => d.delivery_status === 'DELIVERED').length ?? 0;
  const totalCount = trip.delivery_orders?.length ?? 0;

  return (
    <div className="flex flex-col gap-6">
      {/* Back + Header */}
      <div className="flex items-start gap-3 pt-1">
        <button
          onClick={() => navigate('/outbound/driver/trips')}
          className="mt-0.5 p-1.5 hover:bg-zinc-200 rounded-full transition-colors text-shade-50 hover:text-ink shrink-0"
        >
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Giao hàng / Chuyến của tôi
          </span>
          <h1 className="text-xl font-display font-semibold tracking-tight">{trip.trip_number}</h1>
          <p className="text-xs text-shade-50 mt-0.5">
            {deliveredCount}/{totalCount} điểm đã giao
          </p>
        </div>
      </div>

      {/* Body: summary sidebar + stops list */}
      <div className="flex flex-col lg:flex-row gap-6 items-start">

        {/* Left: Trip summary card */}
        <div className="w-full lg:w-80 shrink-0 bg-white rounded-lg border border-hairline-light shadow-sm p-5 card-premium">
          <div className="flex justify-between items-center mb-4">
            <h2 className="text-xs font-bold uppercase tracking-wider text-shade-40">Thông tin chuyến</h2>
            <span className={`text-[10px] font-semibold px-2 py-0.5 rounded-pill border uppercase tracking-wider ${trip.status === 'IN_TRANSIT' ? 'bg-indigo-50 text-indigo-700 border-indigo-200' : trip.status === 'COMPLETED' ? 'bg-aloe-10 text-emerald-900 border-emerald-300' : 'bg-zinc-100 text-zinc-800 border-zinc-200'}`}>
              {trip.status === 'IN_TRANSIT' ? 'Đang giao' : trip.status === 'COMPLETED' ? 'Hoàn thành' : 'Lên kế hoạch'}
            </span>
          </div>
          <div className="space-y-2 text-xs mb-4">
            <p className="flex items-center gap-2 text-shade-50"><Truck className="w-3.5 h-3.5 text-shade-40 shrink-0" /> Xe: <span className="font-semibold text-ink">{trip.vehicle_plate}</span></p>
            <p className="flex items-center gap-2 text-shade-50"><User className="w-3.5 h-3.5 text-shade-40 shrink-0" /> Tài xế: <span className="font-semibold text-ink">{trip.driver_name}</span></p>
            <p className="flex items-center gap-2 text-shade-50"><Calendar className="w-3.5 h-3.5 text-shade-40 shrink-0" /> Ngày: <span className="font-semibold text-ink">{new Date(trip.planned_date).toLocaleDateString('vi-VN')}</span></p>
          </div>

          {/* Depart button for PLANNED trips */}
          {trip.status === 'PLANNED' && (
            <button
              onClick={handleDepart}
              disabled={submitting}
              className="w-full btn-pill btn-pill-primary flex items-center justify-center gap-2 py-2.5 text-sm disabled:opacity-50"
            >
              {submitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Play className="w-4 h-4" />}
              Xác nhận xuất phát
            </button>
          )}

          {/* Progress bar */}
          {totalCount > 0 && (
            <div className="mt-4 pt-4 border-t border-hairline-light">
              <div className="flex justify-between text-[11px] text-shade-50 mb-2">
                <span>Tiến độ giao hàng</span>
                <span className="font-semibold text-ink">{deliveredCount}/{totalCount}</span>
              </div>
              <div className="w-full bg-zinc-100 rounded-full h-2">
                <div
                  className="bg-emerald-500 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${totalCount > 0 ? (deliveredCount / totalCount) * 100 : 0}%` }}
                />
              </div>
            </div>
          )}
        </div>

        {/* Right: Stops list */}
        <div className="flex-1 flex flex-col gap-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-shade-40">
          Danh sách điểm giao ({totalCount} điểm)
        </h3>

        {trip.delivery_orders?.map((doItem, index) => {
          const isDelivered = doItem.delivery_status === 'DELIVERED';
          const isFailed = doItem.delivery_status === 'FAILED';
          const isPending = !isDelivered && !isFailed;

          return (
            <div
              key={doItem.id}
              className={`rounded-lg border overflow-hidden ${isDelivered ? 'bg-aloe-10 border-emerald-300' : isFailed ? 'bg-red-50 border-red-200' : 'bg-white border-hairline-light shadow-sm'}`}
            >
              <div className="p-4 flex gap-3">
                <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 font-bold text-sm ${isDelivered ? 'bg-emerald-600 text-white' : isFailed ? 'bg-red-500 text-white' : 'bg-ink text-white'}`}>
                  {isDelivered ? <CheckCircle2 className="w-4 h-4" /> : index + 1}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex justify-between items-start gap-2">
                    <h4 className="text-sm font-bold text-ink leading-tight">{doItem.dealer_name}</h4>
                    <StatusBadge status={doItem.delivery_status} />
                  </div>
                  {doItem.dealer_address && (
                    <p className="text-xs text-shade-40 mt-1 flex items-start gap-1">
                      <MapPin className="w-3.5 h-3.5 shrink-0 mt-0.5" />
                      {doItem.dealer_address}
                    </p>
                  )}
                  <p className="text-xs text-shade-40 mt-0.5 font-mono">{doItem.do_number}</p>

                  {/* Action buttons for pending stops */}
                  {isPending && trip.status === 'IN_TRANSIT' && (
                    <div className="flex gap-2 mt-4">
                      <button
                        onClick={() => { setActiveDO(doItem); setModalType('DELIVER'); }}
                        className="flex-1 py-2.5 bg-ink text-white rounded-full text-sm font-semibold hover:bg-shade-70 active:scale-95 transition-all"
                      >
                        Giao hàng (OTP)
                      </button>
                      <button
                        onClick={() => { setActiveDO(doItem); setModalType('FAIL'); }}
                        className="px-3.5 py-2.5 bg-red-50 text-red-700 border border-red-200 rounded-full hover:bg-red-100 active:scale-95 transition-all"
                        title="Báo giao thất bại"
                      >
                        <AlertTriangle className="w-4 h-4" />
                      </button>
                    </div>
                  )}

                  {/* Result messages */}
                  {isFailed && doItem.failure_reason && (
                    <p className="text-xs text-red-600 mt-2 flex items-center gap-1">
                      <X className="w-3.5 h-3.5 shrink-0" /> Thất bại: {doItem.failure_reason}
                    </p>
                  )}
                </div>
              </div>

              {/* Quick contact row */}
              {isPending && (
                <div className="px-4 py-2.5 border-t border-hairline-light flex gap-4 bg-zinc-50">
                  <button className="flex items-center gap-1.5 text-xs text-ink font-medium">
                    <Phone className="w-3.5 h-3.5" /> Gọi điện
                  </button>
                </div>
              )}
            </div>
          );
        })}
        </div> {/* end stops list */}
      </div> {/* end body flex row */}

      {/* POD Delivery Modal */}
      {activeDO && modalType === 'DELIVER' && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white w-full sm:max-w-md rounded-t-2xl sm:rounded-xl max-h-[90vh] flex flex-col shadow-2xl">
            <div className="px-5 py-4 border-b border-hairline-light flex items-center justify-between">
              <h3 className="text-base font-bold text-ink">Xác nhận giao hàng</h3>
              <button onClick={closeModal} className="p-1.5 hover:bg-zinc-100 rounded-full text-shade-50 hover:text-ink transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 overflow-y-auto flex-1 flex flex-col gap-5">
              {/* Context */}
              <div className="bg-indigo-50 border border-indigo-100 rounded-lg p-3 text-sm text-indigo-800">
                Đơn <strong className="font-bold">{activeDO.do_number}</strong> — <strong>{activeDO.dealer_name}</strong>
              </div>

              {/* Photo upload */}
              <div>
                <label className="block text-xs font-bold text-shade-60 mb-2 uppercase tracking-wider">
                  Ảnh bằng chứng (POD) *
                </label>
                <div className="flex gap-2 flex-wrap">
                  {photos.map((p, i) => (
                    <div key={i} className="w-20 h-20 bg-zinc-100 rounded-lg shrink-0 flex items-center justify-center relative border border-hairline-light">
                      <Package className="w-6 h-6 text-shade-30" />
                      <button
                        onClick={() => setPhotos(photos.filter((_, idx) => idx !== i))}
                        className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-0.5 w-5 h-5 flex items-center justify-center"
                      >
                        <X className="w-3 h-3" />
                      </button>
                    </div>
                  ))}
                  {photos.length < 3 && (
                    <button
                      onClick={handleMockTakePhoto}
                      className="w-20 h-20 bg-zinc-50 rounded-lg shrink-0 flex flex-col items-center justify-center border-2 border-dashed border-shade-30 text-shade-40 hover:bg-zinc-100 transition-colors"
                    >
                      <Camera className="w-5 h-5 mb-1" />
                      <span className="text-[11px] font-medium">Chụp ảnh</span>
                    </button>
                  )}
                </div>
              </div>

              {/* OTP section */}
              <div className="border-t border-hairline-light pt-5">
                <label className="block text-xs font-bold text-shade-60 mb-3 uppercase tracking-wider">
                  Xác thực OTP từ đại lý
                </label>
                {!otpSent ? (
                  <button
                    onClick={handleRequestOTP}
                    disabled={submitting}
                    className="w-full py-3.5 bg-ink text-white font-bold rounded-full text-sm hover:bg-shade-70 disabled:opacity-50 active:scale-95 transition-all"
                  >
                    {submitting ? <Loader2 className="w-4 h-4 animate-spin mx-auto" /> : 'Gửi mã OTP cho đại lý'}
                  </button>
                ) : (
                  <div className="flex flex-col gap-4">
                    <div className="flex justify-between items-center">
                      <p className="text-sm text-shade-50">Nhập mã 6 số <span className="text-[11px] text-shade-40">(Mock: 123456)</span></p>
                      <OTPCountdown expiresAt={otpExpiresAt} onExpired={() => setOtpExpired(true)} />
                    </div>
                    <OTPInput length={6} onComplete={handleVerifyOTP} />
                    {otpExpired && (
                      <p className="text-xs text-red-600 text-center font-medium">Mã OTP đã hết hạn.</p>
                    )}
                    <button
                      onClick={handleRequestOTP}
                      disabled={submitting || !otpExpired}
                      className="text-sm text-ink font-semibold underline disabled:opacity-40 disabled:no-underline text-center"
                    >
                      Gửi lại OTP
                    </button>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Failure Modal */}
      {activeDO && modalType === 'FAIL' && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4">
          <div className="bg-white w-full sm:max-w-md rounded-t-2xl sm:rounded-xl max-h-[90vh] flex flex-col shadow-2xl">
            <div className="px-5 py-4 border-b border-hairline-light flex items-center justify-between">
              <h3 className="text-base font-bold text-red-600 flex items-center gap-2">
                <AlertTriangle className="w-5 h-5" /> Báo giao thất bại
              </h3>
              <button onClick={closeModal} className="p-1.5 hover:bg-zinc-100 rounded-full text-shade-50 hover:text-ink transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-5 overflow-y-auto flex-1 flex flex-col gap-5">
              <div className="bg-red-50 border border-red-100 rounded-lg p-3 text-sm text-red-800">
                Đơn <strong>{activeDO.do_number}</strong> — {activeDO.dealer_name}
              </div>

              <div>
                <label className="block text-xs font-bold text-shade-60 mb-2 uppercase tracking-wider">
                  Lý do thất bại *
                </label>
                <textarea
                  className="w-full text-input text-sm h-28 resize-none"
                  placeholder="Ví dụ: Đại lý đóng cửa, từ chối nhận, sai địa chỉ..."
                  value={failureReason}
                  onChange={(e) => setFailureReason(e.target.value)}
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-shade-60 mb-2 uppercase tracking-wider">
                  Ảnh hiện trường (tùy chọn)
                </label>
                <div className="flex gap-2 flex-wrap">
                  {photos.map((p, i) => (
                    <div key={i} className="w-20 h-20 bg-zinc-100 rounded-lg flex items-center justify-center border border-hairline-light">
                      <Package className="w-6 h-6 text-shade-30" />
                    </div>
                  ))}
                  {photos.length < 3 && (
                    <button
                      onClick={handleMockTakePhoto}
                      className="w-20 h-20 bg-zinc-50 rounded-lg flex flex-col items-center justify-center border-2 border-dashed border-shade-30 text-shade-40 hover:bg-zinc-100 transition-colors"
                    >
                      <Camera className="w-5 h-5 mb-1" />
                      <span className="text-[11px] font-medium">Chụp ảnh</span>
                    </button>
                  )}
                </div>
              </div>

              <button
                onClick={handleFailDelivery}
                disabled={!failureReason || submitting}
                className="w-full py-3.5 bg-red-600 text-white font-bold rounded-full text-sm hover:bg-red-700 disabled:opacity-50 active:scale-95 transition-all"
              >
                {submitting ? <Loader2 className="w-4 h-4 animate-spin mx-auto" /> : 'Xác nhận thất bại'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
