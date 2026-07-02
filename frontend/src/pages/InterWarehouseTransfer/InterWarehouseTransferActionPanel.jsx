import React, { useEffect, useMemo, useState } from 'react';
import { Check, ClipboardCheck, PackageCheck, RotateCcw, Send, Truck, X } from 'lucide-react';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { ROLES } from '../../utils/constants';

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

const InterWarehouseTransferActionPanel = ({ transfer, currentUser, activeWarehouse, hasRole, hasWarehouseAccess, vehicles, drivers, locations, onAction }) => {
  const [reason, setReason] = useState('');
  const [trip, setTrip] = useState({
    vehicleId: '',
    driverId: '',
    plannedStartAt: toDateTimeInputValue(transfer?.tripPlannedStartAt),
    plannedEndAt: toDateTimeInputValue(transfer?.tripPlannedEndAt),
  });
  const [countRows, setCountRows] = useState([]);
  const [checkRows, setCheckRows] = useState([]);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    setTrip({
      vehicleId: '',
      driverId: '',
      plannedStartAt: toDateTimeInputValue(transfer?.tripPlannedStartAt),
      plannedEndAt: toDateTimeInputValue(transfer?.tripPlannedEndAt),
    });
    setCountRows([]);
    setCheckRows([]);
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

  const canManageSourceWarehouse = hasWarehouseAccess?.(transfer.sourceWarehouseId);
  const canManageDestinationWarehouse = transfer.isReturned
    ? hasWarehouseAccess?.(transfer.sourceWarehouseId)
    : hasWarehouseAccess?.(transfer.destinationWarehouseId);
  const allItemsSent = transfer.items?.every((item) => Number(item.sentQty) === Number(item.plannedQty));
  const allItemsCounted = transfer.items?.every((item) => item.workerReceivedQty !== null && item.workerReceivedQty !== undefined);
  const allItemsChecked = transfer.items?.every((item) => item.receivedQty !== null && item.receivedQty !== undefined
    && item.qcPassedQty !== null && item.qcPassedQty !== undefined
    && item.qcFailedQty !== null && item.qcFailedQty !== undefined);
  const hasTrip = Boolean(transfer.tripId);
  const isAssignedDriver = hasRole(ROLES.DRIVER)
    && Number(transfer.driverUserId || 0) === Number(currentUser?.id || 0);
  const canDriverDepart = hasTrip && allItemsSent && isAssignedDriver;
  const sourceWarehouseId = Number(transfer.sourceWarehouseId || 0);
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
    if (transfer.status === 'APPROVED' && hasTrip && !allItemsSent) {
      return {
        title: 'Chờ xếp hàng',
        detail: `Thủ kho nguồn ${transfer.sourceWarehouseCode} xác nhận số lượng xuất lên xe.`,
      };
    }
    if (transfer.status === 'APPROVED' && hasTrip && allItemsSent) {
      return {
        title: 'Chờ tài xế rời kho',
        detail: `${transfer.driverName || 'Tài xế được gán'} xác nhận rời ${transfer.sourceWarehouseCode}.`,
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

  const ensureCountRows = () => {
    if (countRows.length) return countRows;
    const rows = transfer.items.map((item) => ({
      transferItemId: item.id,
      receivedQty: item.sentQty ?? item.plannedQty,
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
            ? 'border-red-200 bg-red-50 text-red-700'
            : 'border-amber-200 bg-amber-50 text-amber-700'
        }`}>
          {transfer.tripWarningMessage}
        </div>
      )}

      {transfer.status === 'NEW' && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <Button loading={busy} icon={Check} onClick={() => run('approve')}>Duyệt giữ chỗ</Button>
          <div className="flex gap-2">
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do từ chối/hủy" />
            <Button loading={busy} icon={X} variant="outline-light" onClick={() => run('reject', reason)}>Từ chối</Button>
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
          Phiếu mới đang chờ quản lý kho nguồn duyệt giữ chỗ. Sau khi duyệt, thủ kho sẽ xếp hàng và xác nhận số lượng xuất.
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
          <Button loading={busy} disabled={!canAssignTrip} icon={Truck} onClick={() => run('assignTrip', {
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
          Phiếu đã duyệt nhưng chưa có chuyến xe. Dispatcher cần lập chuyến trước, sau đó thủ kho mới xếp hàng lên xe.
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && hasTrip && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <Button loading={busy} disabled={allItemsSent} icon={PackageCheck} onClick={() => run('ship')}>
            {allItemsSent ? 'Đã xếp hàng' : 'Xếp hàng đúng số lượng'}
          </Button>
          <Button loading={busy} icon={RotateCcw} variant="outline-light" onClick={() => run('unship')}>Hạ hàng khỏi xe</Button>
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.DRIVER) && canDriverDepart && (
        <Button loading={busy} icon={Send} onClick={() => run('depart')}>Tài xế xác nhận rời kho</Button>
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

      {transfer.status === 'IN_TRANSIT' && !transfer.isReturned && transfer.returnRequested && (
        <div className="rounded-md border border-red-200 bg-red-50 p-3 flex flex-col gap-2.5 mb-2">
          <div className="text-xs text-red-700 font-bold flex items-center gap-1.5">
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-red-500"></span>
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
              <Button loading={busy} variant="outline-light" className="text-red-600 border-red-300 hover:bg-red-50 py-1 px-3 text-xs" onClick={() => {
                if (!reason.trim()) {
                  alert("Vui lòng điền lý do từ chối!");
                  return;
                }
                run('rejectReturn', reason);
              }}>
                Từ chối
              </Button>
              <Button loading={busy} className="bg-red-600 hover:bg-red-700 text-white font-bold py-1 px-3 text-xs" onClick={() => run('approveReturn')}>
                Duyệt quay xe
              </Button>
            </div>
          )}
          {!(hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse) && (
            <div className="text-[10px] text-shade-50 italic">Đang chờ Quản lý kho đích duyệt...</div>
          )}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && !transfer.isReturned && !transfer.returnRequested && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && (
        <div className="rounded-lg border border-hairline-light bg-canvas-cream p-4 text-sm flex flex-col gap-2 mb-2">
          <div className="text-xs text-ink font-semibold">Phát hiện gửi sai SKU, hàng hỏng hoặc sai lệch nghiêm trọng khi mở thùng?</div>
          <div className="flex gap-2">
            <Input
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="Nhập lý do gửi sai SKU..."
              className="flex-1"
            />
            <Button variant="outline-light" icon={RotateCcw} className="text-shade-50 border-hairline-light hover:bg-canvas-cream py-1.5 px-3 text-xs" loading={busy} onClick={() => {
              if (!reason.trim()) {
                alert("Vui lòng điền lý do gửi sai SKU!");
                return;
              }
              run('requestReturn', reason);
            }}>
              Báo sai SKU & Trả về
            </Button>
          </div>
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && !transfer.isReturned && (hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse) && (
        <div className="rounded-md border border-amber-200 bg-amber-50/50 p-3 flex flex-col gap-2 mb-2">
          <div className="text-xs text-amber-800 font-medium">Chuyến xe có sự cố hoặc cần quay đầu về kho nguồn?</div>
          <Button variant="outline-light" icon={RotateCcw} className="text-amber-700 border-amber-300 hover:bg-amber-100" loading={busy} onClick={() => run('returnToSource')}>
            Quay đầu về kho nguồn
          </Button>
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && (
        <div className="flex flex-col gap-3">
          {transfer.tripOverdue && !transfer.isReturned ? (
            <div className="rounded-md border border-red-200 bg-red-50 p-3 text-xs text-red-700 font-medium">
              Chuyến đi đã quá hạn giao hàng. Vui lòng liên hệ tài xế hoặc điều phối kho nguồn để thực hiện quay đầu xe về kho nguồn.
            </div>
          ) : (
            <>
              <Button variant="outline-light" icon={ClipboardCheck} onClick={ensureCountRows}>Nhập số lượng thực nhận</Button>
              {countRows.map((row) => {
                const item = transfer.items.find((line) => line.id === row.transferItemId);
                return (
                  <div key={row.transferItemId} className="grid grid-cols-1 md:grid-cols-4 gap-2 items-end">
                    <div className="text-xs font-semibold">{item.productSku}<br /><span className="text-shade-50">Gửi: {item.sentQty}</span></div>
                    <Input label="Số lượng nhận" type="number" value={row.receivedQty} onChange={(e) => setRow(countRows, setCountRows, row.transferItemId, { receivedQty: Number(e.target.value) })} />
                    <Input label="Lý do nếu lệch" value={row.issueReason} onChange={(e) => setRow(countRows, setCountRows, row.transferItemId, { issueReason: e.target.value })} />
                    <Button loading={busy} onClick={() => run('receiveCount', countRows)}>Lưu count</Button>
                  </div>
                );
              })}
            </>
          )}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && !allItemsCounted && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          {transfer.tripOverdue && !transfer.isReturned
            ? 'Chuyến đi đã quá hạn giao hàng. Yêu cầu thực hiện quay đầu xe về kho nguồn.'
            : 'Chờ công nhân kho đích nhập số lượng thực nhận trước khi thủ kho kiểm tra count/QC.'}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && allItemsCounted && (
        <div className="flex flex-col gap-3">
          {transfer.tripOverdue && !transfer.isReturned ? (
            <div className="rounded-md border border-red-200 bg-red-50 p-3 text-xs text-red-700 font-medium">
              Chuyến đi đã quá hạn giao hàng. Yêu cầu thực hiện quay đầu xe về kho nguồn.
            </div>
          ) : (
            <>
              <div className="flex flex-col md:flex-row md:items-end gap-2 border-b border-hairline-light pb-3">
                <div className="flex-1">
                  <Button variant="outline-light" icon={ClipboardCheck} onClick={ensureCheckRows}>Kiểm tra count/QC</Button>
                </div>
                <div className="flex-1 md:flex-initial flex gap-2 items-end">
                  <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do từ chối cách ly bắt buộc" />
                  <Button loading={busy} variant="outline-light" className="text-red-600 border-red-600 hover:bg-red-50" icon={X} onClick={() => {
                    if (!reason.trim()) {
                      alert("Vui lòng nhập lý do từ chối cách ly!");
                      return;
                    }
                    run('quarantineReject', reason);
                  }}>Từ chối & Cách ly toàn bộ</Button>
                </div>
              </div>
              {checkRows.map((row) => {
                const item = transfer.items.find((line) => line.id === row.transferItemId);
                return (
                  <div key={row.transferItemId} className="flex flex-col gap-2">
                     <div className="grid grid-cols-1 md:grid-cols-6 gap-2 items-end">
                       <div className="text-xs font-semibold">{item.productSku}<br /><span className="text-shade-50">CN nhập: {item.workerReceivedQty ?? '-'}</span></div>
                       <Input label="SL chốt" type="number" value={row.confirmedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { confirmedQty: Number(e.target.value) })} />
                       <Input label="QC đạt" type="number" value={row.qcPassedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcPassedQty: Number(e.target.value) })} />
                       <div className="flex flex-col gap-1">
                         <Input label="QC lỗi" type="number" value={row.qcFailedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcFailedQty: Number(e.target.value) })} />
                         {Number(row.qcFailedQty) > 0 && (
                           <div className="text-[10px] text-amber-700 bg-amber-50 border border-amber-200 rounded px-2 py-1 leading-snug">
                             {destinationQuarantineBin
                               ? <>{Number(row.qcFailedQty)} sp lỗi → <span className="font-mono font-bold">{destinationQuarantineBin.code ?? destinationQuarantineBin.code}</span> (tự động)</>
                               : '⚠ Kho đích chưa có Quarantine Bin!'}
                           </div>
                         )}
                       </div>
                       <Input type="select" label="Bin đạt QC" value={row.destinationLocationId} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { destinationLocationId: e.target.value })}
                         options={[{ value: '', label: 'Chọn bin' }, ...destinationBins.map((loc) => ({ value: loc.id, label: loc.code }))]} />
                       <Button loading={busy} onClick={() => run('receiveCheck', checkRows.map((line) => ({ ...line, destinationLocationId: Number(line.destinationLocationId) })))}>Duyệt QC</Button>
                     </div>
                     <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                       <Input label="Checker note nếu sửa count" value={row.checkerNote} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { checkerNote: e.target.value })} />
                       <Input label="Lý do QC lỗi" value={row.qcFailureReason} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcFailureReason: e.target.value })} />
                     </div>
                  </div>
                );
              })}
            </>
          )}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && !allItemsChecked && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          {transfer.tripOverdue && !transfer.isReturned
            ? 'Chuyến đi đã quá hạn giao hàng. Yêu cầu thực hiện quay đầu xe về kho nguồn.'
            : 'Chờ thủ kho đích hoàn tất kiểm tra count/QC trước khi quản lý xác nhận cuối.'}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && allItemsChecked && (
        <div className="flex gap-2">
          {transfer.tripOverdue && !transfer.isReturned ? (
            <div className="rounded-md border border-red-200 bg-red-50 p-3 text-xs text-red-700 font-medium">
              Chuyến đi đã quá hạn giao hàng. Yêu cầu thực hiện quay đầu xe về kho nguồn.
            </div>
          ) : (
            <div className="flex-1 flex flex-col md:flex-row gap-2 items-end">
              <div className="flex-1">
                <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do nếu lệch/thiếu hoặc lý do từ chối cách ly" />
              </div>
              <div className="flex gap-2">
                <Button loading={busy} icon={Check} onClick={() => run('finalReceive', reason)}>Xác nhận cuối</Button>
                <Button loading={busy} variant="outline-light" className="text-red-600 border-red-600 hover:bg-red-50" icon={X} onClick={() => {
                  if (!reason.trim()) {
                    alert("Vui lòng nhập lý do từ chối cách ly vào ô văn bản!");
                    return;
                  }
                  run('quarantineReject', reason);
                }}>Từ chối & Cách ly toàn bộ</Button>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default InterWarehouseTransferActionPanel;
