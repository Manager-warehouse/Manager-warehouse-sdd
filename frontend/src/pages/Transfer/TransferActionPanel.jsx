import React, { useMemo, useState } from 'react';
import { Check, ClipboardCheck, PackageCheck, RotateCcw, Send, Truck, X } from 'lucide-react';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { ROLES } from '../../utils/constants';

const hasAny = (hasRole, roles) => roles.some((role) => hasRole(role));

const TransferActionPanel = ({ transfer, hasRole, hasWarehouseAccess, vehicles, drivers, locations, onAction }) => {
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

  const canManageSourceWarehouse = hasAny(hasRole, [ROLES.ADMIN, ROLES.CEO])
    || hasWarehouseAccess?.(transfer.sourceWarehouseId);
  const canManageDestinationWarehouse = hasAny(hasRole, [ROLES.ADMIN, ROLES.CEO])
    || hasWarehouseAccess?.(transfer.destinationWarehouseId);
  const allItemsSent = transfer.items?.every((item) => Number(item.sentQty) === Number(item.plannedQty));
  const canDriverDepart = transfer.tripId && allItemsSent;

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

      {transfer.status === 'NEW' && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <Button loading={busy} icon={Check} onClick={() => run('approve')}>Duyệt giữ chỗ</Button>
          <div className="flex gap-2">
            <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do từ chối/hủy" />
            <Button loading={busy} icon={X} variant="outline-light" onClick={() => run('reject', reason)}>Từ chối</Button>
          </div>
        </div>
      )}

      {transfer.status === 'NEW' && hasAny(hasRole, [ROLES.PLANNER, ROLES.ADMIN, ROLES.CEO]) && (
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

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.DISPATCHER, ROLES.ADMIN, ROLES.CEO]) && !transfer.tripId && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-2">
          <Input type="select" label="Xe" value={trip.vehicleId} onChange={(e) => setTrip({ ...trip, vehicleId: e.target.value })}
            options={[{ value: '', label: 'Chọn xe' }, ...vehicles.map((vehicle) => ({ value: vehicle.id, label: vehicle.plate_number || vehicle.plateNumber }))]} />
          <Input type="select" label="Tài xế" value={trip.driverId} onChange={(e) => setTrip({ ...trip, driverId: e.target.value })}
            options={[{ value: '', label: 'Chọn tài xế' }, ...drivers.map((driver) => ({ value: driver.id, label: driver.full_name || driver.fullName }))]} />
          <Input type="date" label="Ngày chuyến" value={trip.plannedDate} onChange={(e) => setTrip({ ...trip, plannedDate: e.target.value })} />
          <Button loading={busy} icon={Truck} onClick={() => run('assignTrip', {
            vehicleId: Number(trip.vehicleId),
            driverId: Number(trip.driverId),
            plannedDate: trip.plannedDate,
          })}>Lập chuyến</Button>
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageSourceWarehouse && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
          <Button loading={busy} icon={PackageCheck} onClick={() => run('ship')}>Xếp hàng đúng số lượng</Button>
          <Button loading={busy} icon={RotateCcw} variant="outline-light" onClick={() => run('unship')}>Hạ hàng khỏi xe</Button>
        </div>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.DRIVER, ROLES.ADMIN, ROLES.CEO]) && canDriverDepart && (
        <Button loading={busy} icon={Send} onClick={() => run('depart')}>Tài xế xác nhận rời kho</Button>
      )}

      {transfer.status === 'APPROVED' && hasAny(hasRole, [ROLES.DRIVER]) && !canDriverDepart && (
        <div className="rounded-md border border-hairline-light bg-canvas-cream/60 px-3 py-2 text-xs text-shade-60">
          Phiếu cần dispatcher lập chuyến và thủ kho xếp hàng đủ trước khi tài xế xác nhận rời kho.
        </div>
      )}

      {transfer.status === 'IN_TRANSIT' && hasAny(hasRole, [ROLES.WAREHOUSE_STAFF, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && (
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

      {transfer.status === 'IN_TRANSIT' && hasAny(hasRole, [ROLES.STOREKEEPER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && (
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

      {transfer.status === 'IN_TRANSIT' && hasAny(hasRole, [ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]) && canManageDestinationWarehouse && (
        <div className="flex gap-2">
          <Input value={reason} onChange={(e) => setReason(e.target.value)} placeholder="Lý do nếu thiếu/vấn đề final-level" />
          <Button loading={busy} icon={Check} onClick={() => run('finalReceive', reason)}>Xác nhận cuối</Button>
        </div>
      )}
    </div>
  );
};

export default TransferActionPanel;
