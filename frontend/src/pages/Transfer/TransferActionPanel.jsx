import React, { useMemo, useState } from 'react';
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

const getDriverWarehouseIds = (driver) => {
  const ids = driver.warehouse_ids || driver.warehouseIds || [];
  return Array.isArray(ids) ? ids.map(Number) : [];
};

const TransferActionPanel = ({ transfer, currentUser, activeWarehouse, hasRole, hasWarehouseAccess, vehicles, drivers, locations, onAction }) => {
  const [reason, setReason] = useState('');
  const [trip, setTrip] = useState({ vehicleId: '', driverId: '', plannedDate: transfer?.plannedDate || '' });
  const [countRows, setCountRows] = useState([]);
  const [checkRows, setCheckRows] = useState([]);
  const [busy, setBusy] = useState(false);

  const destinationBins = useMemo(() => locations.filter((loc) => {
    const warehouseId = loc.warehouseId ?? loc.warehouse_id;
    const type = loc.type;
    const active = loc.isActive ?? loc.is_active;
    const quarantine = loc.isQuarantine ?? loc.is_quarantine;
    return warehouseId === transfer?.destinationWarehouseId && type === 'BIN' && active && !quarantine;
  }), [locations, transfer]);

  if (!transfer) {
    return <div className="border border-hairline-light rounded-lg p-4 text-sm text-shade-50">Chọn một phiếu điều chuyển để thao tác.</div>;
  }

  const canManageSourceWarehouse = hasWarehouseAccess?.(transfer.sourceWarehouseId);
  const canManageDestinationWarehouse = hasWarehouseAccess?.(transfer.destinationWarehouseId);
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
    const available = (vehicle.status || '').toUpperCase() === 'AVAILABLE';
    return active && available && warehouseId === sourceWarehouseId;
  });
  const sourceDriverPool = drivers.filter((driver) => {
    const warehouseIds = getDriverWarehouseIds(driver);
    const active = driver.is_active !== false && driver.isActive !== false;
    return active && warehouseIds.includes(sourceWarehouseId);
  });
  const availableSourceDrivers = sourceDriverPool.filter((driver) => (driver.status || '').toUpperCase() === 'AVAILABLE');
  const unavailableSourceDrivers = sourceDriverPool.filter((driver) => (driver.status || '').toUpperCase() !== 'AVAILABLE');
  const canAssignTrip = Boolean(trip.vehicleId) && Boolean(trip.driverId) && Boolean(trip.plannedDate)
    && availableSourceDrivers.length > 0 && sourceVehicles.length > 0;

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
        detail: `Dispatcher kho nguồn ${transfer.sourceWarehouseCode} chọn xe và tài xế trước khi thủ kho xếp hàng.`,
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
        title: 'Chờ nhập số lượng thực nhận',
        detail: `Công nhân kho đích ${transfer.destinationWarehouseCode} ghi nhận số lượng xuống xe.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && allItemsCounted && !allItemsChecked) {
      return {
        title: 'Chờ kiểm tra count/QC',
        detail: `Thủ kho đích ${transfer.destinationWarehouseCode} chốt số lượng và QC.`,
      };
    }
    if (transfer.status === 'IN_TRANSIT' && allItemsChecked) {
      return {
        title: 'Chờ xác nhận cuối',
        detail: `Quản lý kho đích ${transfer.destinationWarehouseCode} hoàn tất phiếu.`,
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
    <div className="border border-hairline-light rounded-lg bg-white p-4 flex flex-col gap-4">
      <div>
        <div className="text-xs font-bold uppercase tracking-wider text-shade-60">Thao tác phiếu</div>
        <div className="text-lg font-semibold">{transfer.transferNumber}</div>
      </div>

      <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2">
        <div className="text-[10px] font-bold uppercase tracking-wider text-shade-60">Bước hiện tại</div>
        <div className="text-sm font-semibold text-ink">{flowInfo.title}</div>
        <div className="text-xs text-shade-60 mt-0.5">{flowInfo.detail}</div>
      </div>

      {transfer.status === 'NEW' && hasRole(ROLES.WAREHOUSE_MANAGER) && canManageSourceWarehouse && (
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

      {transfer.status === 'NEW' && hasAny(hasRole, [ROLES.STOREKEEPER]) && canManageSourceWarehouse && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Phiếu mới đang chờ quản lý kho nguồn duyệt giữ chỗ. Sau khi duyệt, thủ kho sẽ xếp hàng và xác nhận số lượng xuất.
        </div>
      )}

      {transfer.status === 'NEW' && hasRole(ROLES.DISPATCHER) && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Chưa thể sắp xếp xe vì phiếu chưa được quản lý kho nguồn duyệt.
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.DISPATCHER) && !transfer.tripId && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-2">
          <Input type="select" label="Xe" value={trip.vehicleId} onChange={(e) => setTrip({ ...trip, vehicleId: e.target.value })}
            options={[
              { value: '', label: sourceVehicles.length ? 'Chọn xe kho nguồn' : `Không có xe rảnh ở ${transfer.sourceWarehouseCode || activeWarehouse?.code || 'kho nguồn'}` },
              ...sourceVehicles.map((vehicle) => ({ value: vehicle.id, label: vehicle.plate_number || vehicle.plateNumber })),
            ]} />
          <Input type="select" label="Tài xế" value={trip.driverId} onChange={(e) => setTrip({ ...trip, driverId: e.target.value })}
            options={[
              { value: '', label: availableSourceDrivers.length ? 'Chọn tài xế kho nguồn' : `Không có tài xế rảnh ở ${transfer.sourceWarehouseCode}` },
              ...availableSourceDrivers.map((driver) => ({ value: driver.id, label: driver.full_name || driver.fullName })),
              ...unavailableSourceDrivers.map((driver) => {
                const status = (driver.status || '').toUpperCase();
                return {
                  value: `busy-${driver.id}`,
                  label: `${driver.full_name || driver.fullName} - ${DRIVER_STATUS_LABELS[status] || status}`,
                  disabled: true,
                };
              }),
            ]} />
          <Input type="date" label="Ngày chuyến" value={trip.plannedDate} onChange={(e) => setTrip({ ...trip, plannedDate: e.target.value })} />
          <Button loading={busy} disabled={!canAssignTrip} icon={Truck} onClick={() => run('assignTrip', {
            vehicleId: Number(trip.vehicleId),
            driverId: Number(trip.driverId),
            plannedDate: trip.plannedDate,
          })}>Lập chuyến</Button>
          {!sourceDriverPool.length && (
            <div className="md:col-span-4 rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
              Chưa có hồ sơ tài xế được gán kho nguồn {transfer.sourceWarehouseCode}. Cần cập nhật warehouse assignment đúng kho nguồn.
            </div>
          )}
          {sourceDriverPool.length > 0 && !availableSourceDrivers.length && (
            <div className="md:col-span-4 rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
              Có {sourceDriverPool.length} tài xế thuộc kho nguồn {transfer.sourceWarehouseCode}, nhưng chưa có ai ở trạng thái Sẵn sàng chạy chuyến.
            </div>
          )}
          {!sourceVehicles.length && (
            <div className="md:col-span-4 rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
              Chưa có xe đang rảnh ở kho nguồn {transfer.sourceWarehouseCode}. Cần gán xe vào đúng kho nguồn và đặt trạng thái AVAILABLE.
            </div>
          )}
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.STOREKEEPER) && canManageSourceWarehouse && !hasTrip && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Phiếu đã duyệt nhưng chưa có chuyến xe. Dispatcher cần lập chuyến trước, sau đó thủ kho mới xếp hàng lên xe.
        </div>
      )}

      {transfer.status === 'APPROVED' && hasRole(ROLES.STOREKEEPER) && canManageSourceWarehouse && hasTrip && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <Button loading={busy} icon={PackageCheck} onClick={() => run('ship')}>Xếp hàng đúng số lượng</Button>
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

      {transfer.status === 'IN_TRANSIT' && hasRole(ROLES.WAREHOUSE_STAFF) && canManageDestinationWarehouse && (
        <div className="flex flex-col gap-3">
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
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasRole(ROLES.STOREKEEPER) && canManageDestinationWarehouse && !allItemsCounted && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Chờ công nhân kho đích nhập số lượng thực nhận trước khi thủ kho kiểm tra count/QC.
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasRole(ROLES.STOREKEEPER) && canManageDestinationWarehouse && allItemsCounted && (
        <div className="flex flex-col gap-3">
          <Button variant="outline-light" icon={ClipboardCheck} onClick={ensureCheckRows}>Kiểm tra count/QC</Button>
          {checkRows.map((row) => {
            const item = transfer.items.find((line) => line.id === row.transferItemId);
            return (
              <div key={row.transferItemId} className="grid grid-cols-1 md:grid-cols-6 gap-2 items-end">
                <div className="text-xs font-semibold">{item.productSku}<br /><span className="text-shade-50">CN nhập: {item.workerReceivedQty ?? '-'}</span></div>
                <Input label="SL chốt" type="number" value={row.confirmedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { confirmedQty: Number(e.target.value) })} />
                <Input label="QC đạt" type="number" value={row.qcPassedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcPassedQty: Number(e.target.value) })} />
                <Input label="QC lỗi" type="number" value={row.qcFailedQty} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcFailedQty: Number(e.target.value) })} />
                <Input type="select" label="Bin đạt QC" value={row.destinationLocationId} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { destinationLocationId: e.target.value })}
                  options={[{ value: '', label: 'Chọn bin' }, ...destinationBins.map((loc) => ({ value: loc.id, label: loc.code }))]} />
                <Button loading={busy} onClick={() => run('receiveCheck', checkRows.map((line) => ({ ...line, destinationLocationId: Number(line.destinationLocationId) })))}>Duyệt QC</Button>
                <Input className="md:col-span-3" label="Checker note nếu sửa count" value={row.checkerNote} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { checkerNote: e.target.value })} />
                <Input className="md:col-span-3" label="Lý do QC lỗi" value={row.qcFailureReason} onChange={(e) => setRow(checkRows, setCheckRows, row.transferItemId, { qcFailureReason: e.target.value })} />
              </div>
            );
          })}
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasRole(ROLES.WAREHOUSE_MANAGER) && canManageDestinationWarehouse && !allItemsChecked && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Chờ thủ kho đích hoàn tất kiểm tra count/QC trước khi quản lý xác nhận cuối.
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasRole(ROLES.WAREHOUSE_MANAGER) && canManageDestinationWarehouse && allItemsChecked && (
        <div className="flex gap-2">
          <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do nếu thiếu/vấn đề final-level" />
          <Button loading={busy} icon={Check} onClick={() => run('finalReceive', reason)}>Xác nhận cuối</Button>
        </div>
      )}
    </div>
  );
};

export default TransferActionPanel;
