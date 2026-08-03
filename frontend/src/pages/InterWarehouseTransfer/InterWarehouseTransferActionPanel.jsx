import React, { useEffect, useMemo, useState } from 'react';
import { Check, ClipboardCheck, PackageCheck, Plus, Send, Trash2, Truck, X } from 'lucide-react';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import PhotoCaptureInput from '../../components/common/PhotoCaptureInput';
import { ROLES } from '../../utils/constants';
import { useUiStore } from '../../stores/ui.store';
import { interWarehouseTransferService } from '../../services/inter-warehouse-transfer.service';

// Helper role gate cho các nút thao tác trong từng bước TRF.
const hasAny = (hasRole, roles) => roles.some((role) => hasRole(role));
// Điều chuyển nội bộ chỉ nhận số lượng nguyên, khớp rule tồn kho backend.
const isWholeNumber = (value) => Number.isInteger(Number(value));
const nowDateTimeValue = () => {
  // Trả về datetime-local theo giờ máy để validate lập chuyến không chọn quá khứ.
  const now = new Date();
  const offsetDate = new Date(now.getTime() - now.getTimezoneOffset() * 60000);
  return offsetDate.toISOString().slice(0, 16);
};
const DRIVER_STATUS_LABELS = {
  AVAILABLE: 'Sẵn sàng',
  ON_TRIP: 'Đang chạy chuyến',
  ON_DELIVERY: 'Đang đi giao',
  UNAVAILABLE: 'Không khả dụng',
  MAINTENANCE: 'Đang bận',
};

const VEHICLE_STATUS_LABELS = {
  AVAILABLE: 'Sẵn sàng',
  ON_TRIP: 'Đang chạy chuyến',
  MAINTENANCE: 'Bảo trì',
};

const toDateTimeInputValue = (value) => {
  // Chuẩn hóa datetime từ API về format input datetime-local.
  if (!value) return '';
  return String(value).slice(0, 16);
};

const formatDateLabel = (value) => {
  if (!value) return 'Chưa đặt';
  const date = new Date(`${String(value).slice(0, 10)}T00:00:00`);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleDateString('vi-VN');
};

const deadlineExclusiveValue = (dateValue) => {
  if (!dateValue) return '';
  const date = new Date(`${String(dateValue).slice(0, 10)}T00:00:00`);
  if (Number.isNaN(date.getTime())) return '';
  date.setDate(date.getDate() + 1);
  const offsetDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return offsetDate.toISOString().slice(0, 16);
};

const getDriverWarehouseIds = (driver) => {
  // Driver có thể đến từ mock/API snake_case hoặc camelCase, nên gom về mảng number để filter.
  const ids = driver.warehouse_ids || driver.warehouseIds || [];
  return Array.isArray(ids) ? ids.map(Number) : [];
};

const InterWarehouseTransferActionPanel = ({ transfer, currentUser, activeWarehouse, hasRole, hasWarehouseAccess, vehicles, drivers, locations, onAction }) => {
  const { addToast } = useUiStore();
  // reason dùng chung cho từ chối, hủy, discrepancy, quay đầu; mỗi nút sẽ validate nội dung bắt buộc riêng.
  const [reason, setReason] = useState('');
  // trip là form lập chuyến: Dispatcher chọn xe/tài xế và lịch chạy cho TRF đã APPROVED.
  const [trip, setTrip] = useState({
    vehicleId: '',
    driverId: '',
    plannedStartAt: toDateTimeInputValue(transfer?.tripPlannedStartAt),
    plannedEndAt: toDateTimeInputValue(transfer?.tripPlannedEndAt),
  });
  // loadRows là số lượng công nhân kho nguồn thực xếp lên xe trước khi thủ kho QC.
  const [loadRows, setLoadRows] = useState([]);
  const [sourcePickCandidates, setSourcePickCandidates] = useState([]);
  // countRows là số lượng công nhân kho nhận đếm khi hàng xuống xe.
  const [countRows, setCountRows] = useState([]);
  // checkRows là số lượng thủ kho xác nhận sau QC nhận hàng.
  const [checkRows, setCheckRows] = useState([]);
  // putawayRows là kế hoạch chia số lượng QC đạt vào bin thường hoặc bin quay đầu.
  const [putawayRows, setPutawayRows] = useState([]);
  const [busy, setBusy] = useState(false);
  const [outboundQcPhotoFile, setOutboundQcPhotoFile] = useState(null);
  const [outboundQcPhotoName, setOutboundQcPhotoName] = useState('');
  const [outboundQcNote, setOutboundQcNote] = useState('');
  const [receiveQcPhotoFile, setReceiveQcPhotoFile] = useState(null);
  const [returnPhotoFile, setReturnPhotoFile] = useState(null);

  useEffect(() => {
    // Khi đổi phiếu đang chọn, reset toàn bộ form tạm để không mang dữ liệu thao tác của phiếu cũ sang phiếu mới.
    setTrip({
      vehicleId: '',
      driverId: '',
      plannedStartAt: toDateTimeInputValue(transfer?.tripPlannedStartAt),
      plannedEndAt: toDateTimeInputValue(transfer?.tripPlannedEndAt),
    });
    setLoadRows([]);
    setSourcePickCandidates([]);
    setCountRows([]);
    setCheckRows([]);
    setPutawayRows([]);
    setOutboundQcPhotoFile(null);
    setOutboundQcPhotoName('');
    setOutboundQcNote('');
    setReceiveQcPhotoFile(null);
    setReturnPhotoFile(null);
  }, [transfer?.id, transfer?.tripPlannedStartAt, transfer?.tripPlannedEndAt]);

  useEffect(() => {
    const shouldLoadCandidates = transfer?.id
      && transfer?.status === 'APPROVED'
      && Boolean(transfer?.tripId || transfer?.vehicleId)
      && (transfer.items || []).some((item) => item.loadedQty === null || item.loadedQty === undefined || transfer.sourceLoadReworkRequired || transfer.source_load_rework_required);
    if (!shouldLoadCandidates) return;
    let cancelled = false;
    interWarehouseTransferService.getSourceLoadPickCandidates(transfer.id)
      .then((response) => {
        if (cancelled) return;
        const items = response?.items || [];
        setSourcePickCandidates(items);
        setLoadRows(items.map((item) => ({
          transferItemId: item.transferItemId,
          loadedQty: Math.min(Number(item.plannedQty || 0), Number(item.candidates?.[0]?.availableQty || 0)),
          picks: item.candidates?.[0] ? [{
            inventoryId: item.candidates[0].inventoryId,
            locationId: item.candidates[0].locationId,
            quantity: Math.min(Number(item.plannedQty || 0), Number(item.candidates[0].availableQty || 0)),
          }] : [],
        })));
      })
      .catch(() => {
        if (!cancelled) setSourcePickCandidates([]);
      });
    return () => {
      cancelled = true;
    };
  }, [transfer?.id, transfer?.status, transfer?.tripId, transfer?.vehicleId, transfer?.sourceLoadReworkRequired, transfer?.source_load_rework_required]);

  const destinationBins = useMemo(() => locations.filter((loc) => {
    // Nếu phiếu quay đầu, nơi nhận thực tế là kho nguồn; nếu đi bình thường, nơi nhận là kho đích.
    const warehouseId = loc.warehouseId ?? loc.warehouse_id;
    const type = loc.type;
    const active = loc.isActive ?? loc.is_active;
    const quarantine = loc.isQuarantine ?? loc.is_quarantine;
    const staging = loc.isStaging ?? loc.is_staging;
    const targetWarehouseId = transfer?.isReturned ? transfer?.sourceWarehouseId : transfer?.destinationWarehouseId;
    return warehouseId === targetWarehouseId && type === 'BIN' && active && !quarantine && !staging;
  }), [locations, transfer]);

  const destinationQuarantineBin = useMemo(() => {
    // Bin cách ly dùng khi QC fail toàn bộ hoặc hàng quay về cần xử lý riêng.
    const targetWarehouseId = transfer?.isReturned ? transfer?.sourceWarehouseId : transfer?.destinationWarehouseId;
    return locations.find((loc) => {
      const warehouseId = loc.warehouseId ?? loc.warehouse_id;
      const active = loc.isActive ?? loc.is_active;
      const quarantine = loc.isQuarantine ?? loc.is_quarantine;
      return warehouseId === targetWarehouseId && active && quarantine;
    }) || null;
  }, [locations, transfer]);

  if (!transfer) {
    return <div className="border border-hairline-light rounded-lg p-4 text-sm text-shade-50">Chọn một phiếu điều chuyển để thao tác.</div>;
  }

  // Các biến kho active quyết định role hiện tại có đang đứng đúng kho nguồn/kho nhận để thao tác không.
  const activeWarehouseId = Number(activeWarehouse?.id || 0);
  const sourceWarehouseId = Number(transfer.sourceWarehouseId || 0);
  const targetReceivingWarehouseId = Number(transfer.isReturned ? transfer.sourceWarehouseId : transfer.destinationWarehouseId);
  const isActiveSourceWarehouse = activeWarehouseId === sourceWarehouseId;
  const isActiveReceivingWarehouse = activeWarehouseId === targetReceivingWarehouseId;
  const canManageSourceWarehouse = isActiveSourceWarehouse && hasWarehouseAccess?.(transfer.sourceWarehouseId);
  const canManageDestinationWarehouse = isActiveReceivingWarehouse && hasWarehouseAccess?.(targetReceivingWarehouseId);
  const activeReceiveWarehouseCode = transfer.isReturned ? transfer.sourceWarehouseCode : transfer.destinationWarehouseCode;
  const activeReceiveWarehouseLabel = transfer.isReturned ? 'kho nguồn' : 'kho đích';
  // allItemsSent chỉ true sau khi thủ kho nguồn chốt số lượng xuất khớp kế hoạch.
  const allItemsSent = transfer.items?.every((item) => Number(item.sentQty) === Number(item.plannedQty));
  // allItemsLoadedReported true khi công nhân đã báo thực xếp cho toàn bộ dòng.
  const allItemsLoadedReported = transfer.items?.every((item) => item.loadedQty !== null && item.loadedQty !== undefined);
  // loadedQtyMatchesPlan bắt buộc trước khi QC xuất đạt.
  const loadedQtyMatchesPlan = transfer.items?.every((item) => Number(item.loadedQty) === Number(item.plannedQty));
  // allItemsCounted/allItemsChecked tách count của công nhân và QC của thủ kho ở kho nhận.
  const allItemsCounted = transfer.items?.every((item) => item.workerReceivedQty !== null && item.workerReceivedQty !== undefined);
  const allItemsChecked = transfer.items?.every((item) => item.receivedQty !== null && item.receivedQty !== undefined
    && item.qcPassedQty !== null && item.qcPassedQty !== undefined
    && item.qcFailedQty !== null && item.qcFailedQty !== undefined);
  // hasTrip cho biết Dispatcher đã lập chuyến để hàng có thể bước sang xếp/QC/xuất.
  const hasTrip = Boolean(transfer.tripId);
  const outboundQcValue = transfer.outboundQcPassed ?? transfer.outbound_qc_passed;
  const outboundQcDone = outboundQcValue !== null && outboundQcValue !== undefined;
  const outboundQcPassed = outboundQcValue === true;
  const outboundQcFailed = outboundQcValue === false;
  // sourceLoadReworkRequired bật khi thực xếp lệch kế hoạch hoặc QC xuất thất bại.
  const sourceLoadReworkRequired = Boolean(transfer.sourceLoadReworkRequired || transfer.source_load_rework_required);
  const loadHandoverDone = Boolean(transfer.loadHandoverPhotoRef || transfer.load_handover_photo_ref || false);
  const outboundQcStoredPhotoRef = transfer.outboundQcPhotoRef || transfer.outbound_qc_photo_ref || '';
  const arrivalHandoverDone = Boolean(transfer.arrivalHandoverAt
    || transfer.arrival_handover_at
    || transfer.arrivalHandoverPhotoRef
    || transfer.arrival_handover_photo_ref);
  const returnHandoverDone = Boolean(transfer.returnArrivalHandoverAt
    || transfer.return_arrival_handover_at
    || transfer.returnArrivalHandoverPhotoRef
    || transfer.return_arrival_handover_photo_ref);
  const activeReceivingHandoverDone = transfer.isReturned
    ? Boolean(transfer.returnArrivedAt && returnHandoverDone)
    : Boolean(transfer.driverArrivedAt && arrivalHandoverDone);
  const isAssignedDriver = hasRole(ROLES.DRIVER)
    && Number(transfer.driverUserId || 0) === Number(currentUser?.id || 0);
  // Điều chuyển nội bộ: các biến gate này chỉ điều khiển nút trên UI theo thứ tự vật lý.
  // Backend vẫn check lại role, status, ảnh, trip, QC và inventory trước khi mutate dữ liệu.
  const canDriverDepart = hasTrip && allItemsSent && isAssignedDriver && outboundQcPassed && loadHandoverDone && !sourceLoadReworkRequired;
  const sourceVehicles = vehicles.filter((vehicle) => {
    // Chỉ xe thuộc kho nguồn và còn hoạt động mới được đưa vào danh sách lập chuyến.
    const warehouseId = Number(vehicle.warehouse_id || vehicle.warehouseId || 0);
    const active = vehicle.is_active !== false && vehicle.isActive !== false;
    const status = (vehicle.status || '').toUpperCase();
    return active && status !== 'MAINTENANCE' && warehouseId === sourceWarehouseId;
  });
  const sourceDriverPool = drivers.filter((driver) => {
    // Driver phải được gán với kho nguồn thì mới nhận chuyến điều chuyển xuất từ kho đó.
    const warehouseIds = getDriverWarehouseIds(driver);
    const active = driver.is_active !== false && driver.isActive !== false;
    return active && warehouseIds.includes(sourceWarehouseId);
  });
  const schedulableSourceDrivers = sourceDriverPool.filter((driver) => (driver.status || '').toUpperCase() !== 'UNAVAILABLE');
  const blockedSourceDrivers = sourceDriverPool.filter((driver) => (driver.status || '').toUpperCase() === 'UNAVAILABLE');
  const schedulableSourceVehicles = sourceVehicles.filter((vehicle) => (vehicle.status || '').toUpperCase() !== 'MAINTENANCE');
  // Ngày cần hàng của TRQ được backend đưa sang plannedDate của TRF; Dispatcher phải lập chuyến giao xong trong ngày này.
  const requiredArrivalDate = transfer.requiredArrivalDate || transfer.neededByDate || transfer.plannedDate;
  const requiredArrivalDeadline = deadlineExclusiveValue(requiredArrivalDate);
  // Nút lập chuyến chỉ bật khi đã đủ xe, tài xế, thời gian và có nguồn lực khả dụng.
  const canAssignTrip = Boolean(trip.vehicleId) && Boolean(trip.driverId) && Boolean(trip.plannedStartAt) && Boolean(trip.plannedEndAt)
    && schedulableSourceDrivers.length > 0 && schedulableSourceVehicles.length > 0;
  const sourcePickByItemId = new Map(sourcePickCandidates.map((item) => [Number(item.transferItemId), item]));
  // Nếu người dùng chưa nhập gì, UI mặc định thực xếp bằng plannedQty để thao tác nhanh khi hàng khớp.
  const displayedLoadRows = loadRows.length ? loadRows : transfer.items.map((item) => ({
    transferItemId: item.id,
    loadedQty: item.loadedQty ?? item.plannedQty,
    picks: [],
  }));
  const hasDisplayedLoadMismatch = displayedLoadRows.some((row) => {
    const item = transfer.items.find((line) => line.id === row.transferItemId);
    return Number(row.loadedQty) !== Number(item?.plannedQty);
  });
  const hasDisplayedPickMismatch = displayedLoadRows.some((row) => {
    const item = transfer.items.find((line) => line.id === row.transferItemId);
    const totalPicked = (row.picks || []).reduce((total, pick) => total + Number(pick.quantity || 0), 0);
    return totalPicked !== Number(item?.plannedQty);
  });
  const normalReceivingHandoverDone = Boolean(transfer.driverArrivedAt && arrivalHandoverDone);
  const returnReceivingHandoverDone = Boolean(transfer.returnArrivedAt && returnHandoverDone);
  const countReady = countRows.length
    && countRows.every((row) => {
      const receivedQty = Number(row.receivedQty);
      // Receive-count chỉ ghi số công nhân đếm được; nếu lệch số gửi thì final receive tự tạo hồ sơ chênh lệch.
      return row.receivedQty !== ''
        && Number.isFinite(receivedQty)
        && receivedQty >= 0
        && isWholeNumber(row.receivedQty);
    });
  const checkReady = checkRows.length
    && Boolean(receiveQcPhotoFile)
    && checkRows.every((row) => {
      const confirmedQty = Number(row.confirmedQty);
      const qcPassedQty = Number(row.qcPassedQty);
      const qcFailedQty = Number(row.qcFailedQty);
      const item = transfer.items.find((line) => line.id === row.transferItemId);
      const sentQty = Number(item?.sentQty ?? item?.plannedQty ?? 0);
      const countMismatch = Number(item?.workerReceivedQty ?? sentQty) !== sentQty;
      const expectedPutawayQty = Number(item?.workerReceivedQty ?? confirmedQty);
      // Receive-check là bước thủ kho kiểm QC; count đã do công nhân nhập nên thủ kho không sửa số lượng ở đây.
      return Number.isFinite(confirmedQty)
        && Number.isFinite(qcPassedQty)
        && Number.isFinite(qcFailedQty)
        && confirmedQty >= 0
        && qcPassedQty >= 0
        && qcFailedQty >= 0
        && isWholeNumber(row.confirmedQty)
        && isWholeNumber(row.qcPassedQty)
        && isWholeNumber(row.qcFailedQty)
        && (countMismatch
          ? qcFailedQty === 0 && qcPassedQty === expectedPutawayQty
          : qcPassedQty + qcFailedQty === confirmedQty)
        && (qcFailedQty === 0 || String(row.qcFailureReason || '').trim());
    });

  const flowInfo = (() => {
    // Xác định text "Bước hiện tại" dựa trên status và các mốc con để người vận hành biết phải làm gì tiếp.
    if (transfer.status === 'NEW') {
      return {
        title: 'Chờ duyệt giữ chỗ',
        detail: `Quản lý kho nguồn ${transfer.sourceWarehouseCode} cần duyệt trước khi dispatcher lập chuyến.`,
      };
    }
    if (transfer.status === 'APPROVED' && !hasTrip) {
      return {
        title: 'Chờ lập chuyến',
        detail: `Dispatcher kho nguồn ${transfer.sourceWarehouseCode} chọn xe và tài xế trước khi thủ kho xếp hàng.`,
      };
    }
    if (transfer.status === 'APPROVED' && hasTrip && outboundQcFailed) {
      return {
        title: 'QC xuất kho thất bại - chờ xử lý lại',
        detail: 'Công nhân kho nguồn cần hạ hàng, đổi hàng hoặc xếp lại rồi báo cáo lại số lượng trước khi thủ kho QC lại.',
      };
    }
    if (transfer.status === 'APPROVED' && hasTrip && !allItemsLoadedReported) {
      return {
        title: 'Chờ công nhân xếp/báo số lượng',
        detail: `Công nhân kho nguồn ${transfer.sourceWarehouseCode} xếp hàng lên xe và nhập số lượng thực xếp theo từng dòng.`,
      };
    }
    if (transfer.status === 'APPROVED' && hasTrip && allItemsLoadedReported && !outboundQcDone) {
      return {
        title: 'Chờ kiểm tra outbound QC',
        detail: `Thủ kho nguồn ${transfer.sourceWarehouseCode} QC trên số lượng công nhân đã xếp trước khi chốt xuất.`,
      };
    }
    if (transfer.status === 'APPROVED' && hasTrip && outboundQcPassed && !allItemsSent) {
      return {
        title: 'QC đạt - chờ chốt số lượng xuất',
        detail: `Thủ kho nguồn ${transfer.sourceWarehouseCode} xác nhận số lượng xuất lên xe.`,
      };
    }
    if (transfer.status === 'APPROVED' && hasTrip && allItemsSent && !loadHandoverDone) {
      return {
        title: 'Chờ hoàn tất xếp hàng',
        detail: 'Thủ kho xác nhận hoàn tất bàn giao hàng lên xe trước khi tài xế rời kho.',
      };
    }
    if (transfer.status === 'APPROVED' && hasTrip && allItemsSent && loadHandoverDone) {
      return {
        title: 'Đã hoàn tất xếp hàng',
        detail: `${transfer.driverName || 'Tài xế được gán'} xác nhận rời ${transfer.sourceWarehouseCode}.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && !transfer.isReturned && !transfer.driverArrivedAt) {
      return {
        title: 'Đang vận chuyển - chờ tài xế đến kho đích',
        detail: `${transfer.driverName || 'Tài xế'} cần xác nhận đã đến ${transfer.destinationWarehouseCode}.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && !transfer.isReturned && transfer.driverArrivedAt && !normalReceivingHandoverDone) {
      return {
        title: 'Chờ thủ kho bàn giao',
        detail: `Thủ kho kho đích ${transfer.destinationWarehouseCode} chụp ảnh bàn giao rồi gửi cho công nhân count.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && transfer.isReturned && !transfer.returnDepartedAt) {
      return {
        title: 'Quay đầu: Chờ tài xế xác nhận quay đầu',
        detail: `${transfer.driverName || 'Tài xế'} xác nhận bắt đầu quay về kho nguồn ${transfer.sourceWarehouseCode}.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && transfer.isReturned && transfer.returnDepartedAt && !transfer.returnArrivedAt) {
      return {
        title: 'Quay đầu: Đang về kho nguồn',
        detail: `Chờ tài xế xác nhận đã về đến ${transfer.sourceWarehouseCode}.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && transfer.isReturned && transfer.returnArrivedAt && !returnReceivingHandoverDone) {
      return {
        title: 'Quay đầu: Chờ thủ kho nguồn nhận bàn giao',
        detail: `Thủ kho nguồn ${transfer.sourceWarehouseCode} chụp ảnh bàn giao hàng quay đầu trước khi gửi cho công nhân count.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && !allItemsCounted) {
      return {
        title: transfer.isReturned ? 'Quay đầu: Chờ nhập số lượng thực nhận tại kho nguồn' : 'Chờ nhập số lượng thực nhận',
        detail: transfer.isReturned
          ? `Công nhân kho nguồn ${transfer.sourceWarehouseCode} ghi nhận số lượng hàng quay đầu.`
          : `Công nhân kho đích ${transfer.destinationWarehouseCode} ghi nhận số lượng xuống xe.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && allItemsCounted && !allItemsChecked) {
      return {
        title: transfer.isReturned ? 'Quay đầu: Chờ kiểm tra count/QC tại kho nguồn' : 'Chờ kiểm tra count/QC',
        detail: transfer.isReturned
          ? `Thủ kho nguồn ${transfer.sourceWarehouseCode} chốt số lượng quay đầu và QC.`
          : `Thủ kho đích ${transfer.destinationWarehouseCode} chốt số lượng và QC.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && allItemsChecked) {
      return {
        title: transfer.isReturned ? 'Quay đầu: Chờ lập kế hoạch cất kệ' : 'Chờ lập kế hoạch cất kệ',
        detail: transfer.isReturned
          ? `Thủ kho nguồn ${transfer.sourceWarehouseCode} chọn kệ, sau đó quản lý duyệt để nhập kho.`
          : `Thủ kho đích ${transfer.destinationWarehouseCode} chọn kệ, sau đó quản lý duyệt để nhập kho.`,
      };
    }
    if (transfer.status === 'PUTAWAY_PENDING_APPROVAL') {
      return {
        title: 'Chờ quản lý duyệt cất kệ',
        detail: `Kế hoạch cất kệ đã được thủ kho gửi. Quản lý kho ${activeReceiveWarehouseCode} duyệt thì hàng mới vào tồn kho.`,
      };
    }
    if (transfer.status === 'COMPLETED' || transfer.status === 'COMPLETED_WITH_DISCREPANCY') {
      return { title: 'Đã hoàn tất', detail: 'Phiếu đã kết thúc luồng điều chuyển.' };
    }
    if (transfer.status === 'QUARANTINED') {
      return { title: 'Đã cách ly toàn bộ', detail: transfer.rejectionReason || 'Hàng điều chuyển đã được đưa vào khu cách ly.' };
    }
    if (transfer.status === 'REJECTED') {
      return { title: 'Đã từ chối', detail: transfer.rejectionReason || 'Phiếu không tiếp tục xử lý.' };
    }
    if (transfer.status === 'CANCELLED') {
      return { title: 'Đã hủy', detail: transfer.rejectionReason || 'Phiếu không tiếp tục xử lý.' };
    }
    return { title: 'Không có bước thao tác', detail: 'Phiếu không có hành động phù hợp ở trạng thái hiện tại.' };
  })();

  const run = async (name, payload) => {
    // Wrapper gọi action từ workspace, khóa nút trong lúc submit và reset reason sau khi thành công/thất bại.
    setBusy(true);
    try {
      await onAction(name, payload);
      setReason('');
    } finally {
      setBusy(false);
    }
  };

  const recordOutboundQc = (passed) => {
    // Validate QC xuất: bắt buộc có ảnh, QC fail phải có lý do, và công nhân phải báo số lượng trước.
    if (!outboundQcPhotoFile) {
      addToast('Vui lòng chọn hoặc chụp ảnh QC.', 'error');
      return;
    }
    if (!passed && !outboundQcNote.trim()) {
      addToast('Vui lòng nhập lý do QC thất bại.', 'error');
      return;
    }
    if (!allItemsLoadedReported) {
      addToast('Công nhân cần báo số lượng xếp trước khi QC.', 'error');
      return;
    }
    run('recordOutboundQc', { passed, note: outboundQcNote.trim(), photoFile: outboundQcPhotoFile });
  };

  const ensureCountRows = () => {
    // Khởi tạo form count cho công nhân kho nhận từ danh sách item của phiếu.
    if (countRows.length) return countRows;
    const rows = transfer.items.map((item) => ({
      transferItemId: item.id,
      receivedQty: '',
      issueReason: '',
    }));
    setCountRows(rows);
    return rows;
  };

  const ensureCheckRows = () => {
    // Khởi tạo form QC nhận; count đã do công nhân nhập, thủ kho chỉ kiểm QC.
    if (checkRows.length) return checkRows;
    const rows = transfer.items.map((item) => ({
      transferItemId: item.id,
      confirmedQty: item.workerReceivedQty ?? item.sentQty ?? item.plannedQty,
      qcPassedQty: Number(item.workerReceivedQty ?? item.sentQty ?? item.plannedQty),
      qcFailedQty: 0,
      checkerNote: '',
      qcFailureReason: '',
    }));
    setCheckRows(rows);
    return rows;
  };

  const setRow = (rows, setRows, id, patch) => {
    // Cập nhật một dòng theo transferItemId, dùng chung cho load/count/check.
    setRows(rows.map((row) => (row.transferItemId === id ? { ...row, ...patch } : row)));
  };

  const setLoadPickQty = (transferItemId, pickIndex, value) => {
    setLoadRows(displayedLoadRows.map((row) => {
      if (row.transferItemId !== transferItemId) return row;
      const picks = (row.picks || []).map((pick, index) => (index === pickIndex ? { ...pick, quantity: value } : pick));
      const loadedQty = picks.reduce((total, pick) => total + Number(pick.quantity || 0), 0);
      return { ...row, picks, loadedQty };
    }));
  };

  const setLoadPickLocation = (transferItemId, pickIndex, inventoryId) => {
    setLoadRows(displayedLoadRows.map((row) => {
      if (row.transferItemId !== transferItemId) return row;
      const candidateItem = sourcePickByItemId.get(Number(transferItemId));
      const candidate = (candidateItem?.candidates || []).find((line) => Number(line.inventoryId) === Number(inventoryId));
      const picks = (row.picks || []).map((pick, index) => (index === pickIndex ? {
        ...pick,
        inventoryId: candidate?.inventoryId || '',
        locationId: candidate?.locationId || '',
        quantity: '',
      } : pick));
      const loadedQty = picks.reduce((total, pick) => total + Number(pick.quantity || 0), 0);
      return { ...row, picks, loadedQty };
    }));
  };

  const addLoadPick = (transferItemId) => {
    setLoadRows(displayedLoadRows.map((row) => {
      if (row.transferItemId !== transferItemId) return row;
      const candidateItem = sourcePickByItemId.get(Number(transferItemId));
      const usedInventoryIds = new Set((row.picks || []).map((pick) => Number(pick.inventoryId)));
      const candidate = (candidateItem?.candidates || []).find((line) => !usedInventoryIds.has(Number(line.inventoryId)));
      if (!candidate) return row;
      return {
        ...row,
        picks: [...(row.picks || []), { inventoryId: candidate.inventoryId, locationId: candidate.locationId, quantity: '' }],
      };
    }));
  };

  const removeLoadPick = (transferItemId, pickIndex) => {
    setLoadRows(displayedLoadRows.map((row) => {
      if (row.transferItemId !== transferItemId) return row;
      const picks = (row.picks || []).filter((_, index) => index !== pickIndex);
      const loadedQty = picks.reduce((total, pick) => total + Number(pick.quantity || 0), 0);
      return { ...row, picks, loadedQty };
    }));
  };

  // Mặc định cất toàn bộ số QC đạt vào bin đầu tiên; người dùng có thể chia nhiều bin.
  const displayedPutawayRows = putawayRows.length ? putawayRows : (transfer.items || [])
    .filter((item) => Number(item.qcPassedQty || 0) > 0)
    .map((item) => ({
      transferItemId: item.id,
      allocations: [{ locationId: item.destinationLocationId || destinationBins[0]?.id || '', quantity: item.qcPassedQty }],
    }));
  const hasPutawayDifference = displayedPutawayRows.some((row) => {
    // Phần cất kệ thường phải bằng đúng số QC đạt; thiếu/thừa đã được tách sang hồ sơ chênh lệch.
    const item = transfer.items.find((line) => line.id === row.transferItemId);
    const allocatedQty = row.allocations.reduce((total, allocation) => total + Number(allocation.quantity || 0), 0);
    return allocatedQty !== Number(item?.qcPassedQty || 0);
  });
  const existingDiscrepancyReason = transfer.discrepancyReason
    || transfer.items?.find((item) => item.issueReason || item.qcFailureReason)?.issueReason
    || transfer.items?.find((item) => item.issueReason || item.qcFailureReason)?.qcFailureReason
    || '';
  const hasQcPassedStock = (transfer.items || []).some((item) => Number(item.qcPassedQty || 0) > 0);
  const putawayReady = (!hasQcPassedStock && allItemsChecked) || (displayedPutawayRows.length > 0 && displayedPutawayRows.every((row) => {
    // Validate cất kệ: có bin, số lượng dương/nguyên, không trùng bin trong cùng SKU và tổng phải bằng đúng số QC đạt.
    const item = transfer.items.find((line) => line.id === row.transferItemId);
    const allocatedQty = row.allocations.reduce((total, allocation) => total + Number(allocation.quantity || 0), 0);
    // Không cho trùng bin trong cùng item để payload putaway rõ ràng và tránh cộng dồn mơ hồ.
    const locationIds = row.allocations.map((a) => String(a.locationId)).filter(Boolean);
    const hasDuplicateBin = locationIds.length !== new Set(locationIds).size;
    if (hasDuplicateBin) return false;
    const qcPassedQty = Number(item?.qcPassedQty || 0);
    return row.allocations.length > 0
      && row.allocations.every((allocation) => Boolean(allocation.locationId) && Number(allocation.quantity) > 0 && isWholeNumber(allocation.quantity))
      && allocatedQty === qcPassedQty;
  }));

  const setPutawayAllocation = (transferItemId, allocationIndex, patch) => {
    // Sửa một dòng allocation trong kế hoạch cất kệ.
    setPutawayRows(displayedPutawayRows.map((row) => {
      if (row.transferItemId !== transferItemId) return row;
      const allocations = row.allocations.map((allocation, index) => (
        index === allocationIndex ? { ...allocation, ...patch } : allocation
      ));
      return { ...row, allocations };
    }));
  };

  const addPutawayAllocation = (transferItemId) => {
    // Thêm một bin mới cho cùng SKU khi cần chia hàng ra nhiều vị trí.
    setPutawayRows(displayedPutawayRows.map((row) => (row.transferItemId === transferItemId
      ? { ...row, allocations: [...row.allocations, { locationId: '', quantity: '' }] }
      : row)));
  };

  const removePutawayAllocation = (transferItemId, allocationIndex) => {
    // Xóa một allocation khỏi kế hoạch cất kệ, chưa gọi backend cho tới khi finalReceive.
    setPutawayRows(displayedPutawayRows.map((row) => (row.transferItemId === transferItemId
      ? { ...row, allocations: row.allocations.filter((_, index) => index !== allocationIndex) }
      : row)));
  };

  return (
    <div className="border border-hairline-light rounded-lg bg-canvas-light p-4 flex flex-col gap-4">
      <div>
        <div className="text-xs font-bold uppercase tracking-wider text-shade-60">Thao tác phiếu</div>
        <div className="text-lg font-semibold">{transfer.transferNumber}</div>
      </div>

      <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2">
        <div className="text-[10px] font-bold uppercase tracking-wider text-shade-60">Bước hiện tại</div>
        <div className="text-sm font-semibold text-ink">{flowInfo.title}</div>
        <div className="text-xs text-shade-60 mt-0.5">{flowInfo.detail}</div>
      </div>

      {transfer.tripWarningActive && (
        <div className={`rounded-md border px-3 py-2 text-xs ${
          transfer.tripOverdue
            ? 'border-danger-200 bg-danger-50 text-danger-700'
            : 'border-warning-200 bg-warning-50 text-warning-700'
        }`}>
          {transfer.tripWarningMessage}
        </div>
      )}

      {transfer.status === 'NEW' && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <Button loading={busy} icon={Check} className="py-2.5 px-4 text-xs flex-none" onClick={() => run('approve')}>Duyệt giữ chỗ</Button>
          <div className="flex gap-2">
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do từ chối/hủy" maxLength={500} />
            <Button loading={busy} icon={X} variant="outline-light" className="py-2.5 px-4 text-xs" onClick={() => {
              if (!reason.trim()) {
                addToast('Vui lòng nhập lý do từ chối phiếu.', 'error');
                return;
              }
              run('reject', reason.trim());
            }}>Từ chối</Button>
          </div>
        </div>
      )}

      {transfer.status === 'NEW' && hasRole(ROLES.PLANNER) && (
        <div className="flex gap-2">
          <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do hủy phiếu NEW" maxLength={500} />
          <Button loading={busy} icon={X} variant="outline-light" onClick={() => {
            if (!reason.trim()) {
              addToast('Vui lòng nhập lý do hủy phiếu.', 'error');
              return;
            }
            run('cancel', reason.trim());
          }}>Hủy phiếu</Button>
        </div>
      )}

      {/* H3: Block cancel khi đã shipped */}
      {transfer.status === 'APPROVED' && hasRole(ROLES.PLANNER) && canManageSourceWarehouse && allItemsSent && (
        <div className="rounded-md border border-warning-200 bg-warning-50 px-3 py-2 text-xs text-warning-800">
          ⚠️ Phiếu đã xuất hàng. Cần thủ kho thực hiện Unship trước khi có thể hủy phiếu.
        </div>
      )}

      {transfer.status === 'NEW' && hasRole(ROLES.STOREKEEPER) && !hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Phiếu mới đang chờ quản lý kho nguồn duyệt giữ chỗ. Sau khi duyệt, công nhân sẽ xếp hàng/báo số lượng trước rồi thủ kho QC.
        </div>
      )}

      {transfer.status === 'NEW' && hasRole(ROLES.DISPATCHER) && !hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Chưa thể sắp xếp xe vì phiếu chưa được quản lý kho nguồn duyệt.
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.DISPATCHER, ROLES.ADMIN, ROLES.CEO]) && !transfer.tripId && (
        <div className="grid grid-cols-1 md:grid-cols-5 gap-2">
          <div className="md:col-span-5 rounded-md border border-warning-300 bg-warning-50/30 px-3 py-2 text-xs text-warning-700">
            Hạn giao trong ngày: <span className="font-semibold">{formatDateLabel(requiredArrivalDate)}</span>
          </div>
          <Input type="select" label="Xe" value={trip.vehicleId} onChange={(e) => setTrip({ ...trip, vehicleId: e.target.value })}
            options={[
              { value: '', label: schedulableSourceVehicles.length ? 'Chọn xe kho nguồn' : `Không có xe khả dụng ở ${transfer.sourceWarehouseCode || activeWarehouse?.code || 'kho nguồn'}` },
              ...schedulableSourceVehicles.map((vehicle) => {
                const status = (vehicle.status || '').toUpperCase();
                const label = vehicle.plate_number || vehicle.plateNumber;
                return {
                  value: vehicle.id,
                  label: status === 'AVAILABLE' ? label : `${label} - ${VEHICLE_STATUS_LABELS[status] || status}`,
                };
              }),
            ]} />
          <Input type="select" label="Tài xế" value={trip.driverId} onChange={(e) => setTrip({ ...trip, driverId: e.target.value })}
            options={[
              { value: '', label: schedulableSourceDrivers.length ? 'Chọn tài xế kho nguồn' : `Không có tài xế khả dụng ở ${transfer.sourceWarehouseCode}` },
              ...schedulableSourceDrivers.map((driver) => {
                const status = (driver.status || '').toUpperCase();
                const label = driver.full_name || driver.fullName;
                return {
                  value: driver.id,
                  label: status === 'AVAILABLE' ? label : `${label} - ${DRIVER_STATUS_LABELS[status] || status}`,
                };
              }),
              ...blockedSourceDrivers.map((driver) => {
                const status = (driver.status || '').toUpperCase();
                return {
                  value: `busy-${driver.id}`,
                  label: `${driver.full_name || driver.fullName} - ${DRIVER_STATUS_LABELS[status] || status}`,
                  disabled: true,
                };
              }),
            ]} />
          <Input type="datetime-local" label="Bắt đầu chuyến" min={nowDateTimeValue()} value={trip.plannedStartAt} onChange={(e) => setTrip({ ...trip, plannedStartAt: e.target.value })} />
          <Input type="datetime-local" label="Kết thúc dự kiến" min={trip.plannedStartAt || nowDateTimeValue()} value={trip.plannedEndAt} onChange={(e) => setTrip({ ...trip, plannedEndAt: e.target.value })} />
          <Button loading={busy} disabled={!canAssignTrip} icon={Truck} className="py-2.5 px-4 text-xs" onClick={() => {
            // Validate thời gian lập chuyến ở UI để Dispatcher không gửi chuyến bắt đầu trong quá khứ.
            if (trip.plannedStartAt < nowDateTimeValue()) {
              addToast('Thời gian bắt đầu chuyến không được ở quá khứ', 'error');
              return;
            }
            // Chuyến phải có thời điểm kết thúc sau thời điểm bắt đầu để backend tính deadline vận chuyển hợp lệ.
            if (trip.plannedEndAt <= trip.plannedStartAt) {
              addToast('Thời gian kết thúc dự kiến phải sau thời gian bắt đầu', 'error');
              return;
            }
            // Ngày cần hàng là deadline cứng: chuyến phải kết thúc trước 00:00 của ngày kế tiếp.
            if (requiredArrivalDeadline && trip.plannedEndAt >= requiredArrivalDeadline) {
              addToast(`Thời gian kết thúc chuyến phải nằm trong ngày cần hàng ${formatDateLabel(requiredArrivalDate)}`, 'error');
              return;
            }
            run('assignTrip', {
              vehicleId: Number(trip.vehicleId),
              driverId: Number(trip.driverId),
              plannedStartAt: trip.plannedStartAt,
              plannedEndAt: trip.plannedEndAt,
            });
          }}>Lập chuyến</Button>
          {!sourceDriverPool.length && (
            <div className="md:col-span-4 rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
              Chưa có hồ sơ tài xế được gán kho nguồn {transfer.sourceWarehouseCode}. Cần cập nhật warehouse assignment đúng kho nguồn.
            </div>
          )}
          {sourceDriverPool.length > 0 && !schedulableSourceDrivers.length && (
            <div className="md:col-span-4 rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
              Có {sourceDriverPool.length} tài xế thuộc kho nguồn {transfer.sourceWarehouseCode}, nhưng chưa có ai ở trạng thái có thể nhận lịch điều chuyển.
            </div>
          )}
          {!schedulableSourceVehicles.length && (
            <div className="md:col-span-4 rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
              Chưa có xe khả dụng ở kho nguồn {transfer.sourceWarehouseCode}. Cần gán xe vào đúng kho nguồn và tránh trạng thái MAINTENANCE.
            </div>
          )}
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.STOREKEEPER) && !hasAny(hasRole, [ROLES.DISPATCHER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && !hasTrip && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Phiếu đã duyệt nhưng chưa có chuyến xe. Dispatcher cần lập chuyến trước, sau đó công nhân mới xếp hàng lên xe.
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && hasTrip && (!allItemsLoadedReported || sourceLoadReworkRequired || outboundQcFailed) && (
        <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-3">
          <div>
            <div className="text-xs font-semibold text-ink">
              {sourceLoadReworkRequired || outboundQcFailed ? 'BƯỚC 1: XỬ LÝ LẠI HÀNG XẾP' : 'BƯỚC 1: CÔNG NHÂN CHỌN KỆ VÀ BỐC HÀNG'}
            </div>
            <div className="text-xs text-shade-60 mt-1">
              Chọn đúng kệ/bin đã giữ hàng và nhập số lượng lấy ở từng kệ. Tổng lấy phải khớp kế hoạch trước khi gửi thủ kho QC xuất.
            </div>
          </div>
          <div className="grid grid-cols-1 gap-3">
            {displayedLoadRows.map((row) => {
              const item = transfer.items.find((line) => line.id === row.transferItemId);
              const candidateItem = sourcePickByItemId.get(Number(row.transferItemId));
              const candidates = candidateItem?.candidates || [];
              const totalPicked = (row.picks || []).reduce((total, pick) => total + Number(pick.quantity || 0), 0);
              const usedInventoryIds = new Set((row.picks || []).map((pick) => Number(pick.inventoryId)).filter(Boolean));
              const canAddMorePick = candidates.some((candidate) => !usedInventoryIds.has(Number(candidate.inventoryId)));
              return (
                <div key={row.transferItemId} className="rounded-md border border-hairline-light bg-canvas-light overflow-hidden">
                  <div className="grid grid-cols-1 md:grid-cols-[1fr_auto] gap-2 items-start px-3 py-3 border-b border-hairline-light bg-canvas-cream/50">
                    <div>
                      <div className="font-semibold text-ink text-sm">{item?.productSku} - {item?.productName}</div>
                      <div className="text-xs text-shade-60 mt-0.5">Kế hoạch: {item?.plannedQty} | Đã lấy: {totalPicked} / {item?.plannedQty}</div>
                    </div>
                    <div className={`rounded-md border px-3 py-2 text-xs font-semibold ${
                      totalPicked === Number(item?.plannedQty)
                        ? 'border-success-200 bg-success-50 text-success-700'
                        : 'border-warning-200 bg-warning-50 text-warning-700'
                    }`}>
                      {totalPicked === Number(item?.plannedQty) ? 'Khớp kế hoạch' : `Còn thiếu ${Math.max(Number(item?.plannedQty || 0) - totalPicked, 0)}`}
                    </div>
                  </div>
                  <div className="px-3 py-3">
                    {candidates.length === 0 ? (
                      <div className="text-xs text-danger-700">Chưa có kệ đã giữ hàng cho dòng này.</div>
                    ) : (
                      <div className="flex flex-col gap-2">
                        {(row.picks || []).map((pick, pickIndex) => {
                          const candidate = candidates.find((line) => Number(line.inventoryId) === Number(pick.inventoryId));
                          const options = [
                            { value: '', label: 'Chọn kệ đã giữ' },
                            ...candidates.map((line) => ({
                              value: line.inventoryId,
                              label: `${line.locationCode} - đã giữ ${line.availableQty}`,
                              disabled: usedInventoryIds.has(Number(line.inventoryId)) && Number(line.inventoryId) !== Number(pick.inventoryId),
                            })),
                          ];
                          return (
                            <div key={`${row.transferItemId}-${pickIndex}`} className="grid grid-cols-1 md:grid-cols-[minmax(220px,1fr)_120px_40px] gap-2 items-end rounded-md border border-hairline-light bg-canvas-cream/40 px-2.5 py-2">
                              <div className="grid grid-cols-1 sm:grid-cols-[minmax(180px,1fr)_auto] gap-2 items-end">
                                <Input
                                  label="Kệ lấy hàng"
                                  type="select"
                                  value={pick.inventoryId || ''}
                                  options={options}
                                  onChange={(e) => setLoadPickLocation(row.transferItemId, pickIndex, e.target.value)}
                                />
                                <div className="rounded-md border border-hairline-light bg-canvas-light px-3 py-2 min-h-[44px] text-xs">
                                  <div className="font-semibold text-ink">Đã giữ: {candidate?.availableQty ?? 0}</div>
                                  <div className="text-shade-50 truncate">Batch: {candidate?.batchCode || candidate?.batchId || '-'}</div>
                                </div>
                              </div>
                              <Input
                                label="SL lấy"
                                type="number"
                                min="0"
                                step="1"
                                max={candidate?.availableQty ?? undefined}
                                value={pick.quantity ?? ''}
                                onChange={(e) => setLoadPickQty(row.transferItemId, pickIndex, e.target.value)}
                              />
                              <Button
                                type="button"
                                variant="outline-light"
                                icon={Trash2}
                                disabled={(row.picks || []).length <= 1}
                                className="h-[44px] w-10 p-0"
                                onClick={() => removeLoadPick(row.transferItemId, pickIndex)}
                              />
                            </div>
                          );
                        })}
                        <div>
                          <Button
                            type="button"
                            variant="outline-light"
                            icon={Plus}
                            disabled={!canAddMorePick}
                            className="py-2 px-3 text-xs"
                            onClick={() => addLoadPick(row.transferItemId)}
                          >
                            Thêm kệ
                          </Button>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
          {(hasDisplayedLoadMismatch || hasDisplayedPickMismatch) && (
            <div className="rounded-md border border-danger-200 bg-danger-50 px-3 py-2 text-xs text-danger-700">
              Tổng số lượng lấy theo từng kệ phải bằng đúng kế hoạch của từng dòng. Vui lòng kiểm tra lại trước khi báo cáo.
            </div>
          )}
          <Button
            loading={busy}
            icon={PackageCheck}
            onClick={() => {
              // Validate số thực xếp không âm vì đây là số hàng vật lý công nhân đưa lên xe.
              if (displayedLoadRows.some((row) => (row.picks || []).length === 0)) {
                addToast('Cần chọn kệ để lấy hàng cho mọi dòng.', 'error');
                return;
              }
              if (displayedLoadRows.some((row) => (row.picks || []).some((pick) => !Number.isFinite(Number(pick.quantity)) || Number(pick.quantity) < 0))) {
                addToast('Số lượng lấy ở từng kệ phải lớn hơn hoặc bằng 0.', 'error');
                return;
              }
              if (displayedLoadRows.some((row) => (row.picks || []).some((pick) => !isWholeNumber(pick.quantity)))) {
                addToast('Số lượng lấy ở từng kệ phải là số nguyên.', 'error');
                return;
              }
              // Công nhân phải nhập đúng số được giao; thiếu/thừa ở bước này là nhập sai hoặc chưa xếp xong.
              if (hasDisplayedLoadMismatch || hasDisplayedPickMismatch) {
                addToast('Tổng số lượng lấy đang lệch kế hoạch. Vui lòng kiểm tra và nhập lại đúng số lượng được giao.', 'error');
                return;
              }
              run('recordSourceLoadReport', {
                items: displayedLoadRows.map((row) => ({
                  transferItemId: row.transferItemId,
                  loadedQty: Number(row.loadedQty),
                  picks: (row.picks || []).map((pick) => ({
                    inventoryId: Number(pick.inventoryId),
                    locationId: Number(pick.locationId),
                    quantity: Number(pick.quantity),
                  })),
                })),
                reworkReason: '',
              });
            }}
          >
            {sourceLoadReworkRequired || outboundQcFailed ? 'Báo cáo lại số lượng xếp' : 'Báo cáo số lượng đã xếp'}
          </Button>
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.STOREKEEPER) && !hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && hasTrip && (!allItemsLoadedReported || sourceLoadReworkRequired || outboundQcFailed) && (
        <div className={`rounded-md border px-3 py-2 text-xs ${
          sourceLoadReworkRequired || outboundQcFailed
            ? 'border-danger-200 bg-danger-50 text-danger-700'
            : 'border-hairline-light bg-canvas-cream/60 text-shade-60'
        }`}>
          {sourceLoadReworkRequired || outboundQcFailed
            ? 'Phiếu đang lệch số lượng hoặc QC xuất kho thất bại. Chờ công nhân bổ sung/đổi/xếp lại hàng rồi báo cáo lại trước khi thủ kho QC.'
            : 'Chờ công nhân kho nguồn xếp hàng và báo cáo số lượng thực xếp trước khi thủ kho QC.'}
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && hasTrip && allItemsLoadedReported && !outboundQcDone && !sourceLoadReworkRequired && (
        <div className="flex flex-col gap-3">
          <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-2">
            <div className="text-xs font-semibold text-ink">BƯỚC 2: KIỂM TRA OUTBOUND QC</div>
            {!loadedQtyMatchesPlan && (
              <div className="rounded-md border border-warning-200 bg-warning-50 px-3 py-2 text-xs text-warning-700">
                Số lượng thực xếp chưa khớp kế hoạch. Cần công nhân chỉnh lại trước khi QC đạt.
              </div>
            )}
            <Input label="Ghi chú QC" value={outboundQcNote} onChange={(e) => setOutboundQcNote(e.target.value)} placeholder="Nhập ghi chú QC..." maxLength={500} />
            <PhotoCaptureInput
              label="Ảnh xác nhận QC"
              fileName={outboundQcPhotoName}
              output="file"
              onChange={(file) => {
                setOutboundQcPhotoFile(file);
                setOutboundQcPhotoName(file?.name || 'Ảnh QC đã chọn');
              }}
              required
            />
            <div className="flex gap-2">
              <Button loading={busy} size="sm" disabled={!outboundQcPhotoFile || !loadedQtyMatchesPlan} onClick={() => recordOutboundQc(true)}>QC Đạt</Button>
              <Button loading={busy} variant="outline-light" size="sm" disabled={!outboundQcPhotoFile} className="text-danger-600 border-danger-300" onClick={() => recordOutboundQc(false)}>QC Thất bại</Button>
            </div>
          </div>
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && hasTrip && outboundQcPassed && !allItemsSent && (
        <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-3">
          <div>
            <div className="text-xs font-semibold text-ink">BƯỚC 3: CHỐT SỐ LƯỢNG XUẤT</div>
            <div className="text-xs text-success-700 font-semibold flex items-center gap-1 mt-1">
              <Check className="w-4 h-4" /> Outbound QC đã đạt. Tiếp tục xác nhận số lượng hàng lên xe.
            </div>
          </div>
          <Button loading={busy} icon={PackageCheck} onClick={() => run('ship')}>
            Hoàn tất xếp hàng
          </Button>
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && hasTrip && outboundQcPassed && allItemsSent && !loadHandoverDone && (
        <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-3">
          <div>
            <div className="text-xs font-semibold text-ink">BƯỚC 4: BÀN GIAO LÊN XE</div>
            <div className="text-xs text-shade-60 mt-1">
              Xếp hàng đã xong. Xác nhận bàn giao hàng cho tài xế trước khi tài xế rời kho.
            </div>
          </div>
          <Button
            loading={busy}
            size="sm"
            disabled={!outboundQcStoredPhotoRef}
            onClick={() => run('loadHandover', { photoRef: outboundQcStoredPhotoRef })}
          >
            Xác nhận bàn giao lên xe
          </Button>
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && hasTrip && outboundQcPassed && allItemsSent && loadHandoverDone && (
        <div className="border border-success-200 rounded p-3 bg-success-50 flex flex-col gap-2">
          <div className="text-xs font-semibold text-success-700 flex items-center gap-1">
            <Check className="w-4 h-4" /> Đã hoàn tất xếp hàng và bàn giao lên xe
          </div>
          <div className="text-xs text-success-700">
            Chờ {transfer.driverName || 'tài xế được gán'} xác nhận rời {transfer.sourceWarehouseCode}.
          </div>
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.DRIVER) && hasTrip && allItemsSent && isAssignedDriver && (
        <div className="flex flex-col gap-2">
          {!(outboundQcPassed && loadHandoverDone) ? (
            <div className="rounded-md border border-warning-200 bg-warning-50 p-3 text-xs text-warning-700">
              Chờ thủ kho hoàn tất Outbound QC và bàn giao lên xe (Load Handover) trước khi tài xế xác nhận rời kho.
            </div>
          ) : (
            <Button loading={busy} icon={Send} onClick={() => run('depart')}>Tài xế xác nhận rời kho</Button>
          )}
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.DRIVER) && hasTrip && allItemsSent && !isAssignedDriver && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Chuyến này được gán cho {transfer.driverName || 'tài xế khác'}. Chỉ đúng tài xế được gán mới xác nhận rời kho.
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.DRIVER) && !canDriverDepart && (!hasTrip || !allItemsSent) && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Phiếu cần dispatcher lập chuyến và thủ kho xếp hàng đủ trước khi tài xế xác nhận rời kho.
        </div>
      )}

      {/* ----------------- IN_TRANSIT STATE ----------------- */}

      {/* Driver arrival & handover gates */}
      {transfer.status === 'IN_TRANSIT' && !transfer.isReturned && (
        <div className="flex flex-col gap-3 mb-2">
          {/* Driver Arrive step */}
          {!transfer.driverArrivedAt && (
            <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-2">
              <div className="text-xs font-semibold text-ink">BƯỚC 1: TÀI XẾ ĐẾN KHO ĐÍCH</div>
              {isAssignedDriver ? (
                <Button loading={busy} icon={Check} onClick={() => run('driverArrive')}>Tài xế xác nhận đã đến kho đích</Button>
              ) : (
                <div className="text-xs text-warning-700 italic">Đang chờ tài xế xác nhận đã đến kho đích...</div>
              )}
            </div>
          )}

          {/* Receiving Handover step */}
          {transfer.driverArrivedAt && !arrivalHandoverDone && (
            <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-2">
              <div className="text-xs font-semibold text-ink">BƯỚC 2: BÀN GIAO TẠI KHO ĐÍCH</div>
              {hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse ? (
                <Button loading={busy} size="sm" icon={Check} onClick={() => run('receivingHandover')}>
                  Xác nhận bàn giao hàng
                </Button>
              ) : (
                <div className="text-xs text-warning-700 italic">Đang chờ thủ kho kho đích xác nhận nhận bàn giao xe...</div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Return leg steps inside IN_TRANSIT (Quay đầu) */}
      {transfer.status === 'IN_TRANSIT' && transfer.isReturned && (
        <div className="flex flex-col gap-3 mb-2">
          <div className="text-xs font-bold text-danger-700 uppercase">Luồng quay đầu về kho nguồn</div>

          {/* Return Depart step */}
          {!transfer.returnDepartedAt && (
            <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-2">
              <div className="text-xs font-semibold text-ink">BƯỚC 1: TÀI XẾ XÁC NHẬN QUAY ĐẦU</div>
              {isAssignedDriver ? (
                <Button loading={busy} icon={Send} onClick={() => run('returnDepart')}>Tài xế xác nhận quay đầu về kho nguồn</Button>
              ) : (
                <div className="text-xs text-warning-700 italic">Đang chờ tài xế xác nhận quay đầu về kho nguồn...</div>
              )}
            </div>
          )}

          {/* Return Arrive step */}
          {transfer.returnDepartedAt && !transfer.returnArrivedAt && (
            <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-2">
              <div className="text-xs font-semibold text-ink">BƯỚC 2: XE QUAY VỀ ĐẾN KHO NGUỒN</div>
              {isAssignedDriver ? (
                <Button loading={busy} icon={Check} onClick={() => run('returnArrive')}>Tài xế xác nhận về đến kho nguồn</Button>
              ) : (
                <div className="text-xs text-warning-700 italic">Đang chờ xe quay về đến kho nguồn...</div>
              )}
            </div>
          )}

          {/* Return Handover step */}
          {transfer.returnArrivedAt && !returnHandoverDone && (
            <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-2">
              <div className="text-xs font-semibold text-ink">BƯỚC 3: BÀN GIAO QUAY ĐẦU TẠI KHO NGUỒN</div>
              {hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse ? (
                <>
                  <PhotoCaptureInput
                    label="Ảnh bàn giao quay đầu"
                    output="file"
                    onChange={(file) => setReturnPhotoFile(file)}
                    required
	                  />
	                  <Button loading={busy} size="sm" disabled={!returnPhotoFile} onClick={() => {
	                    // Bàn giao quay đầu bắt buộc có ảnh để chứng minh hàng đã về kho nguồn.
	                    if (!returnPhotoFile) {
	                      addToast('Vui lòng chọn hoặc chụp ảnh bàn giao!', 'error');
	                      return;
                    }
                    run('returnHandover', { photoFile: returnPhotoFile });
                  }}>Xác nhận Nhận bàn giao quay đầu</Button>
                </>
              ) : (
                <div className="text-xs text-warning-700 italic">Đang chờ thủ kho kho nguồn xác nhận bàn giao quay đầu...</div>
              )}
            </div>
          )}
        </div>
      )}

	      {/* Receive counting steps */}
	      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && !allItemsCounted && (
	        <div className="flex flex-col gap-3">
	          {/* Count chỉ ghi nhận số công nhân đếm, chưa quyết định hàng được nhập kho. */}
	          <Button variant="outline-light" icon={ClipboardCheck} onClick={ensureCountRows}>Nhập số lượng thực nhận</Button>
          {countRows.map((row) => {
            const item = transfer.items.find((line) => line.id === row.transferItemId);
            const countMismatch = row.receivedQty !== '' && Number(row.receivedQty) !== Number(item?.sentQty);
            return (
              <div key={row.transferItemId} className="grid grid-cols-1 md:grid-cols-3 gap-2 items-end">
                <div className="text-xs font-semibold">{item.productSku}<br /><span className="text-shade-50">Gửi: {item.sentQty}</span></div>
                <Input label="Số lượng nhận" type="number" min="0" step="1" value={row.receivedQty} onChange={(e) => setRow(countRows, setCountRows, row.transferItemId, { receivedQty: e.target.value })} />
                {countMismatch ? (
                  <div className="rounded-md border border-warning-200 bg-warning-50 px-3 py-2 text-xs font-semibold text-warning-800">
                    Lệch số gửi, phần thiếu/thừa sẽ vào hồ sơ chênh lệch.
                  </div>
                ) : (
                  <div className="rounded-md border border-success-200 bg-success-50 px-3 py-2 text-xs font-semibold text-success-700">
                    Khớp số gửi
                  </div>
                )}
              </div>
            );
          })}
	          {countRows.length > 0 && (
	            /* Nút chỉ bật khi mỗi dòng có số nguyên không âm; lệch số gửi được tách sang hồ sơ chênh lệch ở final receive. */
	            <Button loading={busy} disabled={!countReady} className="py-2.5 px-4 text-xs" onClick={() => run('receiveCount', countRows.map((row) => ({
              ...row,
              receivedQty: Number(row.receivedQty),
              issueReason: null,
            })))}>
              Hoàn tất báo cáo số lượng
            </Button>
          )}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && allItemsCounted && !allItemsChecked && !hasAny(hasRole, [ROLES.STOREKEEPER]) && (
        <div className="rounded-md border border-success-200 bg-success-50 px-3 py-2 text-xs text-success-700">
          Đã lưu số lượng thực nhận. Chờ thủ kho kiểm tra count/QC.
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && hasRole(ROLES.STOREKEEPER) && !hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && !allItemsCounted && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          {`Chờ công nhân ${activeReceiveWarehouseLabel} nhập số lượng thực nhận trước khi thủ kho kiểm tra count/QC.`}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && allItemsCounted && !allItemsChecked && (
        <div className="flex flex-col gap-3">
          <>
            <div className="flex flex-col md:flex-row md:items-end gap-2 border-b border-hairline-light pb-3">
                <div className="flex-1">
                  {/* QC nhận là bước thủ kho chốt lại số lượng sau count, bắt buộc có ảnh trước khi submit. */}
                  <Button variant="outline-light" icon={ClipboardCheck} onClick={ensureCheckRows}>Kiểm tra count/QC</Button>
                </div>
                <div className="text-xs text-shade-60">
                  Nếu count khớp số gửi thì có thể nhập QC lỗi; nếu count lệch, phần thiếu/thừa sẽ đi hồ sơ chênh lệch và không nhập QC lỗi ở bước này.
                </div>
              </div>
            {checkRows.map((row) => {
                const item = transfer.items.find((line) => line.id === row.transferItemId);
                const sentQty = Number(item?.sentQty ?? item?.plannedQty ?? 0);
                const confirmedQty = Number(row.confirmedQty);
                const workerReceivedQty = Number(item?.workerReceivedQty ?? sentQty);
                const countMismatch = workerReceivedQty !== sentQty;
                const expectedPutawayQty = workerReceivedQty;
                const hasQcFailure = Number(row.qcFailedQty) > 0;
                const isOverSent = workerReceivedQty > sentQty;
                return (
                  <div key={row.transferItemId} className="flex flex-col gap-2">
                         <div className="grid grid-cols-1 md:grid-cols-4 gap-2 items-end">
                           <div className="text-xs font-semibold">{item.productSku}<br /><span className="text-shade-50">Gửi: {sentQty} | CN nhập: {item.workerReceivedQty ?? '-'}</span></div>
                           <div>
                             {isOverSent && (
                               <div className="text-[10px] text-warning-800 bg-warning-50 border border-warning-200 rounded px-2 py-1 mt-1 leading-snug">
                                 CN nhập ({workerReceivedQty}) &gt; số gửi ({sentQty}). Hệ thống sẽ cất đủ {workerReceivedQty} cái và ghi phần thừa vào hồ sơ chênh lệch.
                               </div>
                             )}
                             {workerReceivedQty < sentQty && (
                               <div className="text-[10px] text-warning-800 bg-warning-50 border border-warning-200 rounded px-2 py-1 mt-1 leading-snug">
                                 CN nhập ({workerReceivedQty}) &lt; số gửi ({sentQty}). Hệ thống sẽ cất {workerReceivedQty} cái và ghi phần thiếu vào hồ sơ chênh lệch.
                               </div>
                             )}
                           </div>
                           <Input label="QC đạt" type="number" min="0" step="1" value={countMismatch ? expectedPutawayQty : row.qcPassedQty} disabled={countMismatch} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcPassedQty: Number(e.target.value) })} />
                           {countMismatch ? (
                             <div className="rounded-md border border-warning-200 bg-warning-50 px-3 py-2 text-xs text-warning-800">
                               Count lệch số gửi nên không nhập QC lỗi. Hệ thống sẽ cất đủ {expectedPutawayQty} cái công nhân đã nhập, phần lệch vào hồ sơ chênh lệch.
                             </div>
                           ) : (
                             <div className="flex flex-col gap-1">
                               <Input label="QC lỗi" type="number" min="0" step="1" value={row.qcFailedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcFailedQty: Number(e.target.value) })} />
                               {Number(row.qcFailedQty) > 0 && (
                                 <div className="text-[10px] text-warning-700 bg-warning-50 border border-warning-200 rounded px-2 py-1 leading-snug">
                                   {destinationQuarantineBin
                                     ? <>{Number(row.qcFailedQty)} sp lỗi → <span className="font-mono font-bold">{destinationQuarantineBin.code ?? destinationQuarantineBin.code}</span> (tự động)</>
                                     : 'Kho đích chưa có Quarantine Bin!'}
                                 </div>
                               )}
                             </div>
                           )}
                         </div>
                         {(!countMismatch && hasQcFailure) && (
                           <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                             {!countMismatch && hasQcFailure && (
                               <Input label="Lý do QC lỗi" value={row.qcFailureReason} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcFailureReason: e.target.value })} maxLength={500} />
                             )}
                           </div>
                         )}
                  </div>
                );
              })}
              <PhotoCaptureInput
                label="Ảnh xác nhận QC nhập điều chuyển"
                output="file"
                onChange={(file) => setReceiveQcPhotoFile(file)}
                required
              />
              {!receiveQcPhotoFile && (
                <div className="text-[10px] text-warning-700">Cần chụp/chọn ảnh QC trước khi duyệt.</div>
              )}
              {checkRows.length > 0 && (
                /* Nút chỉ bật khi pass/fail hợp lệ với số công nhân count và có ảnh QC. */
                <Button loading={busy} disabled={!checkReady} className="py-2.5 px-4 text-xs" onClick={() => run('receiveCheck', {
                  items: checkRows.map(({ destinationLocationId, ...line }) => {
                    const item = transfer.items.find((transferItem) => transferItem.id === line.transferItemId);
                    const workerReceivedQty = Number(item?.workerReceivedQty ?? item?.sentQty ?? item?.plannedQty ?? 0);
                    return {
                      ...line,
                      confirmedQty: workerReceivedQty,
                      qcPassedQty: Number(item?.workerReceivedQty) !== Number(item?.sentQty ?? item?.plannedQty ?? 0)
                        ? workerReceivedQty
                        : line.qcPassedQty,
                      qcFailedQty: Number(item?.workerReceivedQty) !== Number(item?.sentQty ?? item?.plannedQty ?? 0)
                        ? 0
                        : line.qcFailedQty,
                      checkerNote: null,
                      qcFailureReason: Number(item?.workerReceivedQty) !== Number(item?.sentQty ?? item?.plannedQty ?? 0)
                        ? null
                        : Number(line.qcFailedQty) > 0 ? line.qcFailureReason?.trim() || null : null,
                    };
                  }),
                  photoFile: receiveQcPhotoFile,
                })}>
	                  Duyệt QC
	                </Button>
              )}
          </>
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && allItemsChecked && !hasAny(hasRole, [ROLES.STOREKEEPER]) && (
        <div className="rounded-md border border-success-200 bg-success-50 p-3 text-xs text-success-700 flex flex-col gap-2">
          <div className="font-semibold flex items-center gap-1">
            <Check className="w-4 h-4" /> Đã hoàn tất kiểm tra count/QC
          </div>
          <div>Chờ thủ kho {activeReceiveWarehouseLabel} {activeReceiveWarehouseCode} cất kệ.</div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
            {(transfer.items || []).map((item) => (
              <div key={item.id} className="rounded border border-success-200 bg-canvas-light px-2 py-1.5">
                <div className="font-mono font-semibold text-ink">{item.productSku}</div>
                <div>CN nhập: {item.receivedQty ?? '-'}</div>
                <div>QC đạt/lỗi: {item.qcPassedQty ?? '-'} / {item.qcFailedQty ?? '-'}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && hasRole(ROLES.WAREHOUSE_MANAGER) && !hasRole(ROLES.STOREKEEPER) && canManageDestinationWarehouse && !allItemsChecked && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          {`Chờ thủ kho ${activeReceiveWarehouseLabel} hoàn tất kiểm tra count/QC trước khi quản lý xác nhận cuối.`}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && hasRole(ROLES.WAREHOUSE_MANAGER) && !hasRole(ROLES.STOREKEEPER) && canManageDestinationWarehouse && allItemsChecked && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Chờ thủ kho {activeReceiveWarehouseLabel} {activeReceiveWarehouseCode} gửi kế hoạch cất kệ trước khi duyệt nhập kho.
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && hasRole(ROLES.STOREKEEPER) && canManageDestinationWarehouse && allItemsChecked && (
        <div className="flex flex-col gap-3">
          {hasQcPassedStock ? (
            <>
              <div className="text-xs font-semibold">Phân bổ hàng đạt QC vào các kệ</div>
              {displayedPutawayRows.map((row) => {
                const item = transfer.items.find((line) => line.id === row.transferItemId);
                return (
                  <div key={row.transferItemId} className="rounded-md border border-hairline-light bg-canvas-cream/40 p-3 flex flex-col gap-3">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div>
                        <div className="text-xs font-semibold">{item.productSku}</div>
                        <div className="text-[11px] text-shade-60">QC đạt: {item.qcPassedQty}</div>
                      </div>
                      <Button type="button" variant="outline-light" icon={Plus} className="h-9 px-3 text-xs" onClick={() => addPutawayAllocation(row.transferItemId)}>Thêm kệ</Button>
                    </div>
                    {row.allocations.map((allocation, allocationIndex) => (
                      <div key={`${row.transferItemId}-${allocationIndex}`} className="grid grid-cols-[minmax(0,1fr)_minmax(112px,0.55fr)_36px] gap-2 items-end rounded-md border border-hairline-light bg-canvas-light p-2">
                        <Input type="select" label={`Kệ ${allocationIndex + 1}`} value={allocation.locationId} onChange={(e) => setPutawayAllocation(row.transferItemId, allocationIndex, { locationId: e.target.value })}
                          options={[{ value: '', label: 'Chọn bin' }, ...destinationBins.map((loc) => ({ value: loc.id, label: loc.code }))]} />
                        <Input label="Số lượng" type="number" min="1" step="1" value={allocation.quantity} onChange={(e) => setPutawayAllocation(row.transferItemId, allocationIndex, { quantity: e.target.value })} />
                        <button
                          type="button"
                          title="Xóa kệ"
                          aria-label={`Xóa kệ ${allocationIndex + 1}`}
                          className="h-10 w-10 rounded-pill border border-danger-200 bg-danger-50 text-danger-600 inline-flex items-center justify-center transition-colors hover:bg-danger-100 hover:border-danger-300 disabled:border-hairline-light disabled:bg-canvas-cream disabled:text-shade-40 disabled:cursor-not-allowed"
                          disabled={row.allocations.length === 1}
                          onClick={() => removePutawayAllocation(row.transferItemId, allocationIndex)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    ))}
                    <div className={`text-[11px] ${row.allocations.reduce((total, allocation) => total + Number(allocation.quantity || 0), 0) === Number(item.qcPassedQty || 0) ? 'text-shade-60' : 'text-warning-800'}`}>
                      Tổng phân bổ: {row.allocations.reduce((total, allocation) => total + Number(allocation.quantity || 0), 0)} / {Number(item.qcPassedQty || 0)}
                    </div>
                  </div>
                );
              })}
            </>
          ) : (
            <div className="rounded-md border border-warning-200 bg-warning-50 px-3 py-2 text-xs text-warning-800">
              Không có hàng đạt QC để cất kệ thường. Gửi xác nhận để quản lý kho duyệt đưa toàn bộ hàng lỗi vào quarantine.
            </div>
          )}
          {/* C1 + H5: hint khi putaway không hợp lệ */}
          {displayedPutawayRows.some((row) => {
            const locationIds = row.allocations.map((a) => String(a.locationId)).filter(Boolean);
            return locationIds.length !== new Set(locationIds).size;
          }) && (
            <div className="rounded-md border border-danger-200 bg-danger-50 px-3 py-2 text-xs text-danger-700">
              ⚠️ Một dòng hàng có bin bị trùng. Mỗi bin chỉ được chọn một lần cho cùng mặt hàng.
            </div>
          )}
          {displayedPutawayRows.some((row) => {
            const item = transfer.items.find((line) => line.id === row.transferItemId);
            const allocatedQty = row.allocations.reduce((total, a) => total + Number(a.quantity || 0), 0);
            return allocatedQty !== Number(item?.qcPassedQty || 0);
          }) && (
            <div className="rounded-md border border-warning-200 bg-warning-50 px-3 py-2 text-xs text-warning-800">
              Tổng phân bổ phải bằng đúng số lượng QC đạt. Phần thiếu/thừa đã được tách sang hồ sơ chênh lệch, không xử lý bằng cách cất thiếu kệ.
            </div>
          )}
	          {/* Gửi finalReceive với putawayItems chỉ là gửi kế hoạch; quản lý duyệt sau thì backend mới tăng tồn. */}
	          <Button loading={busy} disabled={!putawayReady} icon={Check} onClick={() => run('finalReceive', {
            discrepancyReason: existingDiscrepancyReason || null,
            putawayItems: hasQcPassedStock ? displayedPutawayRows.map((row) => ({
              transferItemId: row.transferItemId,
              allocations: row.allocations.map((allocation) => ({ locationId: Number(allocation.locationId), quantity: Number(allocation.quantity) })),
            })) : [],
          })}>{hasQcPassedStock ? 'Gửi kế hoạch cất kệ' : 'Gửi xác nhận hàng lỗi'}</Button>
          <div className="rounded-md border border-warning-200 bg-warning-50 px-3 py-2 text-xs text-warning-800">
            Hàng chưa vào kho ở bước này. Quản lý kho phải duyệt kế hoạch cất kệ trước khi hệ thống tăng tồn.
          </div>
        </div>
      )}

      {transfer.status === 'PUTAWAY_PENDING_APPROVAL' && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && (
        <div className="rounded-md border border-warning-200 bg-warning-50 p-3 flex flex-col gap-3">
          <div>
            <div className="text-xs font-semibold text-warning-900">Kế hoạch cất kệ đang chờ duyệt</div>
            <div className="text-xs text-warning-800 mt-1">Duyệt xong hệ thống mới chuyển hàng từ In-Transit vào tồn kho đích.</div>
          </div>
          {hasPutawayDifference && (
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder={transfer.isReturned ? 'Lý do nếu hàng quay đầu bị lệch' : 'Lý do nếu có chênh lệch'} maxLength={500} />
          )}
          <Button loading={busy} icon={Check} onClick={() => run('finalReceive', { discrepancyReason: hasPutawayDifference ? reason.trim() || null : null })}>
            Duyệt cất kệ và nhập kho
          </Button>
        </div>
      )}

      {transfer.status === 'PUTAWAY_PENDING_APPROVAL' && hasRole(ROLES.STOREKEEPER) && !hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Đã gửi kế hoạch cất kệ. Chờ quản lý kho {activeReceiveWarehouseCode} duyệt để nhập kho.
        </div>
      )}
    </div>
  );
};

export default InterWarehouseTransferActionPanel;
