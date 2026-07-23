import React, { useEffect, useMemo, useState } from 'react';
import { Check, ClipboardCheck, PackageCheck, RotateCcw, Send, Truck, X } from 'lucide-react';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import PhotoCaptureInput from '../../components/common/PhotoCaptureInput';
import { ROLES } from '../../utils/constants';
import { useUiStore } from '../../stores/ui.store';

const hasAny = (hasRole, roles) => roles.some((role) => hasRole(role));
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
  if (!value) return '';
  return String(value).slice(0, 16);
};

const getDriverWarehouseIds = (driver) => {
  const ids = driver.warehouse_ids || driver.warehouseIds || [];
  return Array.isArray(ids) ? ids.map(Number) : [];
};

const InterWarehouseTransferActionPanel = ({ transfer, currentUser, activeWarehouse, hasRole, hasWarehouseAccess, vehicles, drivers, locations, products = [], onAction }) => {
  const { addToast } = useUiStore();
  const [reason, setReason] = useState('');
  const [trip, setTrip] = useState({
    vehicleId: '',
    driverId: '',
    plannedStartAt: toDateTimeInputValue(transfer?.tripPlannedStartAt),
    plannedEndAt: toDateTimeInputValue(transfer?.tripPlannedEndAt),
  });
  const [loadRows, setLoadRows] = useState([]);
  const [sourceLoadReworkReason, setSourceLoadReworkReason] = useState('');
  const [countRows, setCountRows] = useState([]);
  const [checkRows, setCheckRows] = useState([]);
  const [busy, setBusy] = useState(false);
  const [outboundQcPhotoFile, setOutboundQcPhotoFile] = useState(null);
  const [outboundQcPhotoName, setOutboundQcPhotoName] = useState('');
  const [outboundQcNote, setOutboundQcNote] = useState('');
  const [arrivalHandoverPhotoFile, setArrivalHandoverPhotoFile] = useState(null);
  const [receiveQcPhotoFile, setReceiveQcPhotoFile] = useState(null);
  const [returnPhotoFile, setReturnPhotoFile] = useState(null);
  const [wrongSkuItems, setWrongSkuItems] = useState([]);
  const [showWrongSkuForm, setShowWrongSkuForm] = useState(false);
  const [newWrongSku, setNewWrongSku] = useState({
    transferItemId: '',
    actualProductId: '',
    affectedQty: '',
    reason: '',
  });

  useEffect(() => {
    setTrip({
      vehicleId: '',
      driverId: '',
      plannedStartAt: toDateTimeInputValue(transfer?.tripPlannedStartAt),
      plannedEndAt: toDateTimeInputValue(transfer?.tripPlannedEndAt),
    });
    setLoadRows([]);
    setSourceLoadReworkReason('');
    setCountRows([]);
    setCheckRows([]);
    setOutboundQcPhotoFile(null);
    setOutboundQcPhotoName('');
    setOutboundQcNote('');
    setArrivalHandoverPhotoFile(null);
    setReceiveQcPhotoFile(null);
    setReturnPhotoFile(null);
    setWrongSkuItems([]);
    setShowWrongSkuForm(false);
    setNewWrongSku({
      transferItemId: '',
      actualProductId: '',
      affectedQty: '',
      reason: '',
    });
  }, [transfer?.id, transfer?.tripPlannedStartAt, transfer?.tripPlannedEndAt]);

  const destinationBins = useMemo(() => locations.filter((loc) => {
    const warehouseId = loc.warehouseId ?? loc.warehouse_id;
    const type = loc.type;
    const active = loc.isActive ?? loc.is_active;
    const quarantine = loc.isQuarantine ?? loc.is_quarantine;
    const targetWarehouseId = transfer?.isReturned ? transfer?.sourceWarehouseId : transfer?.destinationWarehouseId;
    return warehouseId === targetWarehouseId && type === 'BIN' && active && !quarantine;
  }), [locations, transfer]);

  const destinationQuarantineBin = useMemo(() => {
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

  const activeWarehouseId = Number(activeWarehouse?.id || 0);
  const sourceWarehouseId = Number(transfer.sourceWarehouseId || 0);
  const targetReceivingWarehouseId = Number(transfer.isReturned ? transfer.sourceWarehouseId : transfer.destinationWarehouseId);
  const isActiveSourceWarehouse = activeWarehouseId === sourceWarehouseId;
  const isActiveReceivingWarehouse = activeWarehouseId === targetReceivingWarehouseId;
  const canManageSourceWarehouse = isActiveSourceWarehouse && hasWarehouseAccess?.(transfer.sourceWarehouseId);
  const canManageDestinationWarehouse = isActiveReceivingWarehouse && hasWarehouseAccess?.(targetReceivingWarehouseId);
  const activeReceiveWarehouseCode = transfer.isReturned ? transfer.sourceWarehouseCode : transfer.destinationWarehouseCode;
  const activeReceiveWarehouseLabel = transfer.isReturned ? 'kho nguồn' : 'kho đích';
  const allItemsSent = transfer.items?.every((item) => Number(item.sentQty) === Number(item.plannedQty));
  const allItemsLoadedReported = transfer.items?.every((item) => item.loadedQty !== null && item.loadedQty !== undefined);
  const loadedQtyMatchesPlan = transfer.items?.every((item) => Number(item.loadedQty) === Number(item.plannedQty));
  const allItemsCounted = transfer.items?.every((item) => item.workerReceivedQty !== null && item.workerReceivedQty !== undefined);
  const allItemsChecked = transfer.items?.every((item) => item.receivedQty !== null && item.receivedQty !== undefined
    && item.qcPassedQty !== null && item.qcPassedQty !== undefined
    && item.qcFailedQty !== null && item.qcFailedQty !== undefined);
  const hasTrip = Boolean(transfer.tripId);
  const outboundQcValue = transfer.outboundQcPassed ?? transfer.outbound_qc_passed;
  const outboundQcDone = outboundQcValue !== null && outboundQcValue !== undefined;
  const outboundQcPassed = outboundQcValue === true;
  const outboundQcFailed = outboundQcValue === false;
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
  const canDriverDepart = hasTrip && allItemsSent && isAssignedDriver && outboundQcPassed && loadHandoverDone && !sourceLoadReworkRequired;
  const sourceVehicles = vehicles.filter((vehicle) => {
    const warehouseId = Number(vehicle.warehouse_id || vehicle.warehouseId || 0);
    const active = vehicle.is_active !== false && vehicle.isActive !== false;
    const status = (vehicle.status || '').toUpperCase();
    return active && status !== 'MAINTENANCE' && warehouseId === sourceWarehouseId;
  });
  const sourceDriverPool = drivers.filter((driver) => {
    const warehouseIds = getDriverWarehouseIds(driver);
    const active = driver.is_active !== false && driver.isActive !== false;
    return active && warehouseIds.includes(sourceWarehouseId);
  });
  const schedulableSourceDrivers = sourceDriverPool.filter((driver) => (driver.status || '').toUpperCase() !== 'UNAVAILABLE');
  const blockedSourceDrivers = sourceDriverPool.filter((driver) => (driver.status || '').toUpperCase() === 'UNAVAILABLE');
  const schedulableSourceVehicles = sourceVehicles.filter((vehicle) => (vehicle.status || '').toUpperCase() !== 'MAINTENANCE');
  const canAssignTrip = Boolean(trip.vehicleId) && Boolean(trip.driverId) && Boolean(trip.plannedStartAt) && Boolean(trip.plannedEndAt)
    && schedulableSourceDrivers.length > 0 && schedulableSourceVehicles.length > 0;
  const displayedLoadRows = loadRows.length ? loadRows : transfer.items.map((item) => ({
    transferItemId: item.id,
    loadedQty: item.loadedQty ?? item.plannedQty,
  }));
  const pendingReturnRequest = transfer.status === 'IN_TRANSIT' && !transfer.isReturned && Boolean(transfer.returnRequested);
  const normalReceivingHandoverDone = Boolean(transfer.driverArrivedAt && arrivalHandoverDone);
  const returnReceivingHandoverDone = Boolean(transfer.returnArrivedAt && returnHandoverDone);
  const countReady = countRows.length
    && countRows.every((row) => {
      const item = transfer.items.find((line) => line.id === row.transferItemId);
      const receivedQty = Number(row.receivedQty);
      return row.receivedQty !== ''
        && Number.isFinite(receivedQty)
        && receivedQty >= 0
        && (receivedQty === Number(item?.sentQty) || String(row.issueReason || '').trim());
    });
  const checkReady = checkRows.length
    && Boolean(receiveQcPhotoFile)
    && checkRows.every((row) => {
      const confirmedQty = Number(row.confirmedQty);
      const qcPassedQty = Number(row.qcPassedQty);
      const qcFailedQty = Number(row.qcFailedQty);
      const item = transfer.items.find((line) => line.id === row.transferItemId);
      return Number.isFinite(confirmedQty)
        && Number.isFinite(qcPassedQty)
        && Number.isFinite(qcFailedQty)
        && confirmedQty >= 0
        && qcPassedQty >= 0
        && qcFailedQty >= 0
        && qcPassedQty + qcFailedQty === confirmedQty
        && (qcPassedQty === 0 || Boolean(row.destinationLocationId))
        && (qcFailedQty === 0 || String(row.qcFailureReason || '').trim())
        && (confirmedQty === Number(item?.workerReceivedQty) || String(row.checkerNote || '').trim());
    });

  const flowInfo = (() => {
    if (transfer.status === 'NEW') {
      return {
        title: 'Chờ duyệt giữ chỗ',
        detail: `Quản lý kho nguồn ${transfer.sourceWarehouseCode} cần duyệt trước khi dispatcher lập chuyến.`,
      };
    }
    if (transfer.status === 'APPROVED' && !hasTrip) {
      return {
        title: 'Chờ lập chuyến',
        detail: `Dispatcher kho nguồn ${transfer.sourceWarehouseCode} chọn xe và tài xế trước khi thủ kho xếp hàng. Chuyến phải được lên lịch trước ít nhất 7 ngày.`,
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
    if (pendingReturnRequest) {
      return {
        title: 'Chờ duyệt yêu cầu quay đầu',
        detail: `Quản lý kho đích ${transfer.destinationWarehouseCode} duyệt hoặc từ chối yêu cầu quay đầu do sai SKU.`,
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
        title: 'Chờ thủ kho kho đích nhận bàn giao',
        detail: `Thủ kho kho đích ${transfer.destinationWarehouseCode} chụp ảnh bàn giao rồi gửi cho công nhân count, hoặc báo sai SKU để yêu cầu quay đầu.`,
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
        title: transfer.isReturned ? 'Quay đầu: Chờ xác nhận cuối tại kho nguồn' : 'Chờ xác nhận cuối',
        detail: transfer.isReturned
          ? `Quản lý kho nguồn ${transfer.sourceWarehouseCode} hoàn tất nhận hàng quay đầu.`
          : `Quản lý kho đích ${transfer.destinationWarehouseCode} hoàn tất phiếu.`,
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
    setBusy(true);
    try {
      await onAction(name, payload);
      setReason('');
    } finally {
      setBusy(false);
    }
  };

  const recordOutboundQc = (passed) => {
    if (!outboundQcPhotoFile) {
      addToast('Vui lòng chọn hoặc chụp ảnh QC.', 'error');
      return;
    }
    if (!allItemsLoadedReported) {
      addToast('Công nhân cần báo số lượng xếp trước khi QC.', 'error');
      return;
    }
    run('recordOutboundQc', { passed, note: outboundQcNote, photoFile: outboundQcPhotoFile });
  };

  const ensureCountRows = () => {
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
    if (checkRows.length) return checkRows;
    const rows = transfer.items.map((item) => ({
      transferItemId: item.id,
      confirmedQty: item.workerReceivedQty ?? item.sentQty ?? item.plannedQty,
      qcPassedQty: item.workerReceivedQty ?? item.sentQty ?? item.plannedQty,
      qcFailedQty: 0,
      destinationLocationId: destinationBins[0]?.id || '',
      checkerNote: '',
      qcFailureReason: '',
    }));
    setCheckRows(rows);
    return rows;
  };

  const setRow = (rows, setRows, id, patch) => {
    setRows(rows.map((row) => (row.transferItemId === id ? { ...row, ...patch } : row)));
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
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do từ chối/hủy" />
            <Button loading={busy} icon={X} variant="outline-light" className="py-2.5 px-4 text-xs" onClick={() => run('reject', reason)}>Từ chối</Button>
          </div>
        </div>
      )}

      {transfer.status === 'NEW' && hasRole(ROLES.PLANNER) && (
        <div className="flex gap-2">
          <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do hủy phiếu NEW" />
          <Button loading={busy} icon={X} variant="outline-light" onClick={() => run('cancel', reason)}>Hủy phiếu</Button>
        </div>
      )}

      {transfer.status === 'NEW' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Phiếu mới đang chờ quản lý kho nguồn duyệt giữ chỗ. Sau khi duyệt, công nhân sẽ xếp hàng/báo số lượng trước rồi thủ kho QC.
        </div>
      )}

      {transfer.status === 'NEW' && hasAny(hasRole, [ROLES.DISPATCHER, ROLES.ADMIN, ROLES.CEO]) && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Chưa thể sắp xếp xe vì phiếu chưa được quản lý kho nguồn duyệt.
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.DISPATCHER, ROLES.ADMIN, ROLES.CEO]) && !transfer.tripId && (
        <div className="grid grid-cols-1 md:grid-cols-5 gap-2">
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
          <Input type="datetime-local" label="Bắt đầu chuyến" value={trip.plannedStartAt} onChange={(e) => setTrip({ ...trip, plannedStartAt: e.target.value })} />
          <Input type="datetime-local" label="Kết thúc dự kiến" value={trip.plannedEndAt} onChange={(e) => setTrip({ ...trip, plannedEndAt: e.target.value })} />
          <Button loading={busy} disabled={!canAssignTrip} icon={Truck} className="py-2.5 px-4 text-xs" onClick={() => run('assignTrip', {
            vehicleId: Number(trip.vehicleId),
            driverId: Number(trip.driverId),
            plannedStartAt: trip.plannedStartAt,
            plannedEndAt: trip.plannedEndAt,
          })}>Lập chuyến</Button>
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

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && !hasTrip && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Phiếu đã duyệt nhưng chưa có chuyến xe. Dispatcher cần lập chuyến trước, sau đó công nhân mới xếp hàng lên xe.
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && hasTrip && (!allItemsLoadedReported || sourceLoadReworkRequired || outboundQcFailed) && (
        <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-3">
          <div>
            <div className="text-xs font-semibold text-ink">
              {sourceLoadReworkRequired || outboundQcFailed ? 'BƯỚC 1: XỬ LÝ LẠI HÀNG XẾP' : 'BƯỚC 1: CÔNG NHÂN XẾP HÀNG/BÁO SỐ LƯỢNG'}
            </div>
            <div className="text-xs text-shade-60 mt-1">
              Nhập số lượng thực tế đã xếp lên xe theo từng dòng. QC thất bại thì hạ/đổi/xếp lại rồi báo cáo lại ở đây.
            </div>
          </div>
          {(sourceLoadReworkRequired || outboundQcFailed) && (
            <Input
              label="Lý do xử lý lại"
              value={sourceLoadReworkReason}
              onChange={(e) => setSourceLoadReworkReason(e.target.value)}
              placeholder={transfer.sourceLoadReworkReason || transfer.outboundQcNote || 'Ví dụ: đổi hàng móp méo, xếp lại kiện...'}
            />
          )}
          <div className="grid grid-cols-1 gap-2">
            {displayedLoadRows.map((row) => {
              const item = transfer.items.find((line) => line.id === row.transferItemId);
              return (
                <div key={row.transferItemId} className="grid grid-cols-1 md:grid-cols-[1fr_140px_140px] gap-2 items-end">
                  <div className="rounded-md border border-hairline-light bg-canvas-light px-3 py-2 text-xs">
                    <div className="font-semibold text-ink">{item?.productSku} {item?.productName}</div>
                    <div className="text-shade-60">Kế hoạch: {item?.plannedQty}</div>
                  </div>
                  <Input
                    label="Thực xếp"
                    type="number"
                    min="0"
                    step="0.01"
                    value={row.loadedQty}
                    onChange={(e) => setRow(displayedLoadRows, setLoadRows, row.transferItemId, { loadedQty: e.target.value })}
                  />
                  <div className={`rounded-md border px-3 py-2 text-xs ${
                    Number(row.loadedQty) === Number(item?.plannedQty)
                      ? 'border-success-200 bg-success-50 text-success-700'
                      : 'border-warning-200 bg-warning-50 text-warning-700'
                  }`}>
                    {Number(row.loadedQty) === Number(item?.plannedQty) ? 'Khớp kế hoạch' : 'Cần chỉnh trước QC đạt'}
                  </div>
                </div>
              );
            })}
          </div>
          <Button
            loading={busy}
            icon={PackageCheck}
            onClick={() => run('recordSourceLoadReport', {
              items: displayedLoadRows.map((row) => ({ transferItemId: row.transferItemId, loadedQty: Number(row.loadedQty) })),
              reworkReason: sourceLoadReworkReason,
            })}
          >
            {sourceLoadReworkRequired || outboundQcFailed ? 'Báo cáo lại số lượng xếp' : 'Báo cáo số lượng đã xếp'}
          </Button>
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.STOREKEEPER) && canManageSourceWarehouse && hasTrip && (!allItemsLoadedReported || sourceLoadReworkRequired || outboundQcFailed) && (
        <div className={`rounded-md border px-3 py-2 text-xs ${
          sourceLoadReworkRequired || outboundQcFailed
            ? 'border-danger-200 bg-danger-50 text-danger-700'
            : 'border-hairline-light bg-canvas-cream/60 text-shade-60'
        }`}>
          {sourceLoadReworkRequired || outboundQcFailed
            ? 'QC xuất kho thất bại. Chờ công nhân hạ/đổi/xếp lại hàng và báo cáo lại số lượng trước khi thủ kho QC lại.'
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
            <Input label="Ghi chú QC" value={outboundQcNote} onChange={(e) => setOutboundQcNote(e.target.value)} placeholder="Nhập ghi chú QC..." />
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
          {transfer.driverArrivedAt && !arrivalHandoverDone && !transfer.returnRequested && (
            <div className="border border-hairline-light rounded p-3 bg-canvas-cream flex flex-col gap-2">
              <div className="text-xs font-semibold text-ink">BƯỚC 2: BÀN GIAO TẠI KHO ĐÍCH</div>
              {hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse ? (
                <>
                  <PhotoCaptureInput
                    label="Ảnh bàn giao nhận hàng"
                    output="file"
                    onChange={(file) => {
                      setArrivalHandoverPhotoFile(file);
                      setShowWrongSkuForm(false);
                    }}
                    required
                  />
                  {arrivalHandoverPhotoFile && (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                      <Button loading={busy} size="sm" icon={Send} onClick={() => {
                        run('receivingHandover', { photoFile: arrivalHandoverPhotoFile });
                      }}>
                        Gửi cho nhân viên nhập count
                      </Button>
                      <Button
                        loading={busy}
                        size="sm"
                        variant="outline-light"
                        icon={RotateCcw}
                        className="text-danger-700 border-danger-300 hover:bg-danger-50"
                        onClick={() => setShowWrongSkuForm((value) => !value)}
                      >
                        Báo sai SKU / quay đầu
                      </Button>
                    </div>
                  )}
                </>
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

      {/* Wrong SKU return request waiting for approval */}
      {pendingReturnRequest && (
        <div className="rounded-md border border-danger-200 bg-danger-50 p-3 flex flex-col gap-2.5 mb-2">
          <div className="text-xs text-danger-700 font-bold flex items-center gap-1.5">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-danger-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-danger-500"></span>
            </span>
            YÊU CẦU QUAY ĐẦU DO SAI SKU ĐANG CHỜ PHÊ DUYỆT
          </div>
          <div className="text-xs text-shade-60">
            <span className="font-semibold text-shade-50">Lý do báo:</span> "{transfer.returnReason}"
          </div>
          {hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && (
            <div className="flex gap-2 mt-1">
              <div className="flex-1 flex gap-1">
                <Input
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  placeholder="Lý do từ chối..."
                />
              </div>
              <Button loading={busy} variant="outline-light" className="text-danger-600 border-danger-300 hover:bg-danger-50 py-1 px-3 text-xs" onClick={() => {
                if (!reason.trim()) {
                  addToast('Vui lòng điền lý do từ chối!', 'error');
                  return;
                }
                run('rejectReturn', reason);
              }}>
                Từ chối
              </Button>
              <Button loading={busy} className="bg-danger-600 hover:bg-danger-700 text-white font-bold py-1 px-3 text-xs" onClick={() => run('approveReturn')}>
                Duyệt quay xe
              </Button>
            </div>
          )}
          {!(hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse) && (
            <div className="text-[10px] text-shade-50 italic">Đang chờ Quản lý kho đích duyệt...</div>
          )}
        </div>
      )}

      {/* Wrong SKU submission form */}
      {transfer.status === 'IN_TRANSIT' && !transfer.isReturned && !allItemsCounted && !transfer.returnRequested
        && showWrongSkuForm && !activeReceivingHandoverDone
        && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && (
        <div className="rounded-lg border border-hairline-light bg-canvas-cream p-4 text-sm flex flex-col gap-3 mb-2">
          <div className="text-xs text-ink font-semibold flex items-center gap-1.5 text-danger-700">
            Báo sai SKU & Yêu cầu quay đầu xe
          </div>

          {/* Form to add item */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-2 items-end border-b border-hairline-light pb-3">
            <Input type="select" label="Dòng hàng lỗi" value={newWrongSku.transferItemId} onChange={(e) => setNewWrongSku({ ...newWrongSku, transferItemId: e.target.value })}
              options={[
                { value: '', label: 'Chọn dòng hàng' },
                ...(transfer.items || []).map((item) => ({ value: item.id, label: `${item.productSku} (Yêu cầu: ${item.plannedQty})` }))
              ]} />
            <Input type="select" label="SKU thực tế nhận" value={newWrongSku.actualProductId} onChange={(e) => setNewWrongSku({ ...newWrongSku, actualProductId: e.target.value })}
              options={[
                { value: '', label: 'Chọn SKU thực tế' },
                ...(products || []).map((product) => ({ value: product.id, label: `${product.sku} - ${product.name}` }))
              ]} />
            <Input label="Số lượng sai" type="number" min="0" step="0.01" value={newWrongSku.affectedQty} onChange={(e) => setNewWrongSku({ ...newWrongSku, affectedQty: e.target.value })} />
            <Input label="Lý do" value={newWrongSku.reason} onChange={(e) => setNewWrongSku({ ...newWrongSku, reason: e.target.value })} placeholder="Nhập lý do..." />
            <div className="md:col-span-4 flex justify-end">
              <Button size="sm" variant="outline-light" onClick={() => {
                const originalItem = transfer.items.find((line) => Number(line.id) === Number(newWrongSku.transferItemId));
                const affectedQty = Number(newWrongSku.affectedQty);
                if (!originalItem || !newWrongSku.actualProductId || !newWrongSku.affectedQty || !newWrongSku.reason.trim()) {
                  addToast('Vui lòng nhập đầy đủ thông tin dòng hàng sai!', 'error');
                  return;
                }
                if (Number(originalItem.productId) === Number(newWrongSku.actualProductId)) {
                  addToast('SKU thực tế phải khác SKU dự kiến.', 'error');
                  return;
                }
                if (!Number.isFinite(affectedQty) || affectedQty <= 0 || affectedQty > Number(originalItem.sentQty ?? originalItem.plannedQty)) {
                  addToast('Số lượng sai phải lớn hơn 0 và không vượt số lượng đã gửi.', 'error');
                  return;
                }
                setWrongSkuItems([...wrongSkuItems, {
                  transferItemId: Number(originalItem.id),
                  expectedProductId: Number(originalItem.productId),
                  actualProductId: Number(newWrongSku.actualProductId),
                  affectedQty,
                  reason: newWrongSku.reason.trim(),
                  photoRef: null,
                }]);
                setNewWrongSku({ transferItemId: '', actualProductId: '', affectedQty: '', reason: '' });
              }}>Thêm dòng</Button>
            </div>
          </div>

          {/* List of items added */}
          {wrongSkuItems.length > 0 && (
            <div className="text-xs bg-canvas-light border border-hairline-light rounded p-2 flex flex-col gap-1.5">
              <div className="font-semibold">Danh sách hàng sai SKU cần trả về:</div>
              {wrongSkuItems.map((item, idx) => {
                const orig = transfer.items.find((line) => line.id === item.transferItemId);
                const actual = products.find((product) => Number(product.id) === Number(item.actualProductId));
                return (
                  <div key={idx} className="flex justify-between items-center text-[11px] text-shade-60 border-b border-hairline-light pb-1">
                    <span>- {orig?.productSku} → {actual?.sku || item.actualProductId} (SL: {item.affectedQty}): {item.reason}</span>
                    <button className="text-danger-600 hover:underline" onClick={() => setWrongSkuItems(wrongSkuItems.filter((_, i) => i !== idx))}>Xóa</button>
                  </div>
                );
              })}
            </div>
          )}

          <div className="flex gap-2">
            <Input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Nhập lý do chung..."
              className="flex-1"
            />
            <Button variant="outline-light" icon={RotateCcw} className="text-danger-700 border-danger-300 hover:bg-danger-50 py-1.5 px-3 text-xs" loading={busy} onClick={() => {
              if (!reason.trim()) {
                addToast('Vui lòng nhập lý do chung!', 'error');
                return;
              }
              if (wrongSkuItems.length === 0) {
                addToast('Vui lòng thêm ít nhất 1 dòng hàng sai SKU!', 'error');
                return;
              }
              run('requestReturn', { reason: reason.trim(), wrongSkuItems });
            }}>
              Gửi yêu cầu quay đầu
            </Button>
          </div>
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && !transfer.isReturned && !transfer.returnRequested && !transfer.driverArrivedAt && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && (
        <div className="rounded-md border border-warning-200 bg-warning-50/50 p-3 flex flex-col gap-2 mb-2">
          <div className="text-xs text-warning-800 font-medium">Chuyến xe có sự cố hoặc cần quay đầu về kho nguồn? (Yêu cầu lý do)</div>
          <div className="flex gap-2">
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do quay đầu bắt buộc..." className="flex-1" />
            <Button variant="outline-light" icon={RotateCcw} className="text-warning-700 border-warning-300 hover:bg-warning-100" loading={busy} onClick={() => {
              if (!reason.trim()) {
                addToast('Vui lòng nhập lý do quay đầu!', 'error');
                return;
              }
              run('returnToSource', { reason: reason.trim(), wrongSkuItems: [] });
            }}>
              Quay đầu về kho nguồn
            </Button>
          </div>
        </div>
      )}

      {/* Receive counting steps */}
      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && !transfer.returnRequested && hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && !allItemsCounted && (
        <div className="flex flex-col gap-3">
          <Button variant="outline-light" icon={ClipboardCheck} onClick={ensureCountRows}>Nhập số lượng thực nhận</Button>
          {countRows.map((row) => {
            const item = transfer.items.find((line) => line.id === row.transferItemId);
            return (
              <div key={row.transferItemId} className="grid grid-cols-1 md:grid-cols-3 gap-2 items-end">
                <div className="text-xs font-semibold">{item.productSku}<br /><span className="text-shade-50">Gửi: {item.sentQty}</span></div>
                <Input label="Số lượng nhận" type="number" min="0" step="0.01" value={row.receivedQty} onChange={(e) => setRow(countRows, setCountRows, row.transferItemId, { receivedQty: e.target.value })} />
                <Input label="Lý do nếu lệch" value={row.issueReason} onChange={(e) => setRow(countRows, setCountRows, row.transferItemId, { issueReason: e.target.value })} />
              </div>
            );
          })}
          {countRows.length > 0 && (
            <Button loading={busy} disabled={!countReady} className="py-2.5 px-4 text-xs" onClick={() => run('receiveCount', countRows.map((row) => ({ ...row, receivedQty: Number(row.receivedQty) })))}>
              Hoàn tất báo cáo số lượng
            </Button>
          )}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && !transfer.returnRequested && hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && allItemsCounted && !allItemsChecked && (
        <div className="rounded-md border border-success-200 bg-success-50 px-3 py-2 text-xs text-success-700">
          Đã lưu số lượng thực nhận. Chờ thủ kho kiểm tra count/QC.
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && !transfer.returnRequested && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && !allItemsCounted && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          {`Chờ công nhân ${activeReceiveWarehouseLabel} nhập số lượng thực nhận trước khi thủ kho kiểm tra count/QC.`}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && !transfer.returnRequested && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && allItemsCounted && !allItemsChecked && (
        <div className="flex flex-col gap-3">
          <>
            <div className="flex flex-col md:flex-row md:items-end gap-2 border-b border-hairline-light pb-3">
                    <div className="flex-1">
                      <Button variant="outline-light" icon={ClipboardCheck} onClick={ensureCheckRows}>Kiểm tra count/QC</Button>
                    </div>
                    <div className="text-xs text-shade-60">
                      QC lỗi thì nhập số lượng lỗi theo từng dòng và lý do, hệ thống sẽ đưa phần lỗi vào quarantine khi quản lý xác nhận cuối.
                    </div>
                  </div>
            {checkRows.map((row) => {
                const item = transfer.items.find((line) => line.id === row.transferItemId);
                return (
                  <div key={row.transferItemId} className="flex flex-col gap-2">
                         <div className="grid grid-cols-1 md:grid-cols-5 gap-2 items-end">
                           <div className="text-xs font-semibold">{item.productSku}<br /><span className="text-shade-50">CN nhập: {item.workerReceivedQty ?? '-'}</span></div>
                           <Input label="SL chốt" type="number" value={row.confirmedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { confirmedQty: Number(e.target.value) })} />
                           <Input label="QC đạt" type="number" value={row.qcPassedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcPassedQty: Number(e.target.value) })} />
                           <div className="flex flex-col gap-1">
                             <Input label="QC lỗi" type="number" value={row.qcFailedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcFailedQty: Number(e.target.value) })} />
                             {Number(row.qcFailedQty) > 0 && (
                               <div className="text-[10px] text-warning-700 bg-warning-50 border border-warning-200 rounded px-2 py-1 leading-snug">
                                 {destinationQuarantineBin
                                   ? <>{Number(row.qcFailedQty)} sp lỗi → <span className="font-mono font-bold">{destinationQuarantineBin.code ?? destinationQuarantineBin.code}</span> (tự động)</>
                                   : '⚠ Kho đích chưa có Quarantine Bin!'}
                               </div>
                             )}
                           </div>
                           <Input type="select" label="Bin đạt QC" value={row.destinationLocationId} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { destinationLocationId: e.target.value })}
                             options={[{ value: '', label: 'Chọn bin' }, ...destinationBins.map((loc) => ({ value: loc.id, label: loc.code }))]} />
                         </div>
                         <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                           <Input label="Checker note nếu sửa count" value={row.checkerNote} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { checkerNote: e.target.value })} />
                           <Input label="Lý do QC lỗi" value={row.qcFailureReason} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcFailureReason: e.target.value })} />
                         </div>
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
                <Button loading={busy} disabled={!checkReady} className="py-2.5 px-4 text-xs" onClick={() => run('receiveCheck', {
                  items: checkRows.map((line) => ({ ...line, destinationLocationId: Number(line.destinationLocationId) })),
                  photoFile: receiveQcPhotoFile,
                })}>
                  Duyệt QC
                </Button>
              )}
          </>
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && !transfer.returnRequested && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && allItemsChecked && (
        <div className="rounded-md border border-success-200 bg-success-50 p-3 text-xs text-success-700 flex flex-col gap-2">
          <div className="font-semibold flex items-center gap-1">
            <Check className="w-4 h-4" /> Đã hoàn tất kiểm tra count/QC
          </div>
          <div>Chờ quản lý {activeReceiveWarehouseLabel} {activeReceiveWarehouseCode} xác nhận cuối phiếu.</div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
            {(transfer.items || []).map((item) => (
              <div key={item.id} className="rounded border border-success-200 bg-canvas-light px-2 py-1.5">
                <div className="font-mono font-semibold text-ink">{item.productSku}</div>
                <div>SL chốt: {item.receivedQty ?? '-'}</div>
                <div>QC đạt/lỗi: {item.qcPassedQty ?? '-'} / {item.qcFailedQty ?? '-'}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && !transfer.returnRequested && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && !allItemsChecked && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          {`Chờ thủ kho ${activeReceiveWarehouseLabel} hoàn tất kiểm tra count/QC trước khi quản lý xác nhận cuối.`}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && activeReceivingHandoverDone && !transfer.returnRequested && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && allItemsChecked && (
        <div className="flex gap-2">
          <div className="flex-1 flex flex-col md:flex-row gap-2 items-end">
              <div className="flex-1">
                <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder={transfer.isReturned ? 'Lý do nếu hàng quay đầu bị lệch' : 'Lý do nếu lệch hoặc lý do từ chối cách ly'} />
              </div>
              <div className="flex gap-2">
                <Button loading={busy} icon={Check} onClick={() => run('finalReceive', reason)}>Xác nhận cuối</Button>
              </div>
            </div>
        </div>
      )}
    </div>
  );
};

export default InterWarehouseTransferActionPanel;
