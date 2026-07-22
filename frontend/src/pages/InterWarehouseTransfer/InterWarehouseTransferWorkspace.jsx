import React, { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Plus, RefreshCw, Search, AlertCircle, Info } from 'lucide-react';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Pagination from '../../components/common/Pagination';
import { masterDataService } from '../../services/masterData.service';
import { interWarehouseTransferService } from '../../services/inter-warehouse-transfer.service';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { ROLES } from '../../utils/constants';
import InterWarehouseTransferActionPanel from './InterWarehouseTransferActionPanel';
import InterWarehouseTransferStatusBadge from './InterWarehouseTransferStatusBadge';

const normalizeId = (value) => Number(value || 0);
const isActiveRecord = (record) => record.is_active !== false && record.isActive !== false;
const isPhysicalWarehouse = (warehouse) => (warehouse.type || '').toUpperCase() !== 'IN_TRANSIT';
const hasAnyRole = (hasRole, roles) => roles.some((role) => hasRole(role));

const InterWarehouseTransferWorkspace = () => {
  const { id: routeTransferId } = useParams();
  const { user, activeWarehouse, hasRole, hasWarehouseAccess } = useAuthStore();
  const { addToast } = useUiStore();
  const [transfers, setTransfers] = useState([]);
  const [warehouses, setWarehouses] = useState([]);
  const [products, setProducts] = useState([]);
  const [locations, setLocations] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [drivers, setDrivers] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [formOpen, setFormOpen] = useState(false);
  const [availabilityByLine, setAvailabilityByLine] = useState({});
  const [selectedAvailabilityByItem, setSelectedAvailabilityByItem] = useState({});
  const [form, setForm] = useState({
    externalInstructionCode: '',
    sourceWarehouseId: '',
    destinationWarehouseId: '',
    documentDate: new Date().toISOString().slice(0, 10),
    plannedDate: new Date().toISOString().slice(0, 10),
    notes: '',
    items: [{ productId: '', plannedQty: 1 }],
  });
  const canCreateTransfer = hasRole(ROLES.PLANNER);
  const needsFleetData = hasAnyRole(hasRole, [ROLES.DISPATCHER, ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]);
  const needsLocationData = hasAnyRole(hasRole, [ROLES.STOREKEEPER, ROLES.WAREHOUSE_STAFF, ROLES.WAREHOUSE_MANAGER, ROLES.ADMIN, ROLES.CEO]);

  useEffect(() => {
    loadData();
  }, [routeTransferId, activeWarehouse?.id]);

  useEffect(() => {
    let active = true;

    const loadAvailability = async () => {
      const sourceWarehouseId = normalizeId(form.sourceWarehouseId);
      if (!sourceWarehouseId) {
        setAvailabilityByLine({});
        return;
      }

      const requests = form.items.map(async (item, index) => {
        const productId = normalizeId(item.productId);
        if (!productId) {
          return [index, null];
        }
        try {
          const availability = await interWarehouseTransferService.getAvailability(sourceWarehouseId, productId);
          return [index, availability];
        } catch (error) {
          return [index, { error: error.message || 'Không tải được tồn kho' }];
        }
      });

      const results = await Promise.all(requests);
      if (!active) return;
      setAvailabilityByLine(Object.fromEntries(results));
    };

    loadAvailability();

    return () => {
      active = false;
    };
  }, [form.sourceWarehouseId, form.items]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [transferRows, warehouseRows, productRows, locationRows, vehicleRows, driverRows] = await Promise.all([
        interWarehouseTransferService.getTransfers(),
        canCreateTransfer ? masterDataService.getWarehouses() : Promise.resolve([]),
        canCreateTransfer ? masterDataService.getProducts() : Promise.resolve([]),
        needsLocationData ? masterDataService.getBinLocations() : Promise.resolve([]),
        needsFleetData ? masterDataService.getVehicles() : Promise.resolve([]),
        needsFleetData ? masterDataService.getDrivers() : Promise.resolve([]),
      ]);
      setTransfers(transferRows);
      setWarehouses(warehouseRows.filter((warehouse) => isActiveRecord(warehouse) && isPhysicalWarehouse(warehouse)));
      setProducts(productRows);
      setLocations(locationRows);
      setVehicles(vehicleRows.filter((vehicle) => (vehicle.status || '').toUpperCase() === 'AVAILABLE'));
      setDrivers(driverRows);
      const requestedId = normalizeId(routeTransferId);
      setSelectedId((current) => (
        requestedId && transferRows.some((transfer) => transfer.id === requestedId)
          ? requestedId
          : current || transferRows[0]?.id || null
      ));
    } catch (error) {
      addToast(error.message || 'Không thể tải dữ liệu điều chuyển', 'error');
    } finally {
      setLoading(false);
    }
  };

  const visibleTransfers = useMemo(() => {
    const activeWarehouseId = normalizeId(activeWarehouse?.id);
    if (hasAnyRole(hasRole, [ROLES.ADMIN, ROLES.CEO])) {
      return transfers;
    }
    if (hasRole(ROLES.DRIVER)) {
      return transfers.filter((transfer) => Number(transfer.driverUserId || 0) === Number(user?.id || 0));
    }
    if (hasRole(ROLES.DISPATCHER)) {
      const assignedWarehouses = user?.warehouses?.map(Number) || [];
      return transfers.filter((transfer) => assignedWarehouses.includes(Number(transfer.sourceWarehouseId)));
    }
    if (hasRole(ROLES.PLANNER)) {
      const assignedWarehouses = user?.warehouses?.map(Number) || [];
      return transfers.filter((transfer) => (
        assignedWarehouses.includes(Number(transfer.sourceWarehouseId))
        || assignedWarehouses.includes(Number(transfer.destinationWarehouseId))
      ));
    }
    if (hasRole(ROLES.WAREHOUSE_STAFF)) {
      return transfers.filter((transfer) => (
        normalizeId(transfer.sourceWarehouseId) === activeWarehouseId
        || normalizeId(transfer.destinationWarehouseId) === activeWarehouseId
      ));
    }
    if (hasAnyRole(hasRole, [ROLES.STOREKEEPER, ROLES.WAREHOUSE_MANAGER])) {
      return transfers.filter((transfer) => (
        normalizeId(transfer.sourceWarehouseId) === activeWarehouseId
        || normalizeId(transfer.destinationWarehouseId) === activeWarehouseId
      ));
    }
    return [];
  }, [transfers, user, activeWarehouse, hasRole]);

  useEffect(() => {
    if (!visibleTransfers.length) {
      setSelectedId(null);
      return;
    }
    if (!visibleTransfers.some((transfer) => transfer.id === selectedId)) {
      setSelectedId(visibleTransfers[0].id);
    }
  }, [visibleTransfers, selectedId]);

  const selectedTransfer = visibleTransfers.find((transfer) => transfer.id === selectedId);

  useEffect(() => {
    let active = true;

    const loadSelectedAvailability = async () => {
      if (!selectedTransfer?.sourceWarehouseId || !selectedTransfer.items?.length) {
        setSelectedAvailabilityByItem({});
        return;
      }

      const requests = selectedTransfer.items.map(async (item) => {
        try {
          const availability = await interWarehouseTransferService.getAvailability(
            selectedTransfer.sourceWarehouseId,
            item.productId
          );
          return [item.id, availability];
        } catch (error) {
          return [item.id, { error: error.message || 'Không tải được tồn kho' }];
        }
      });

      const results = await Promise.all(requests);
      if (!active) return;
      setSelectedAvailabilityByItem(Object.fromEntries(results));
    };

    loadSelectedAvailability();

    return () => {
      active = false;
    };
  }, [selectedTransfer]);

  const filteredTransfers = useMemo(() => visibleTransfers.filter((transfer) => {
    const haystack = `${transfer.transferNumber} ${transfer.externalInstructionCode} ${transfer.sourceWarehouseCode} ${transfer.destinationWarehouseCode}`.toLowerCase();
    return haystack.includes(searchTerm.toLowerCase())
      && (statusFilter === 'ALL' || transfer.status === statusFilter);
  }), [visibleTransfers, searchTerm, statusFilter]);

  const totalPages = Math.max(1, Math.ceil(filteredTransfers.length / pageSize));
  const safePage = Math.min(currentPage, totalPages);
  const paginatedTransfers = filteredTransfers.slice((safePage - 1) * pageSize, safePage * pageSize);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, statusFilter, visibleTransfers.length]);

  const sourceWarehouseOptions = useMemo(() => {
    const list = warehouses.filter((w) => {
      if (hasRole(ROLES.ADMIN) || hasRole(ROLES.CEO)) return true;
      const assigned = user?.warehouses || [];
      return assigned.map(Number).includes(Number(w.id));
    });
    return [{ value: '', label: 'Chọn kho nguồn' }, ...list.map((w) => ({ value: w.id, label: `${w.code} - ${w.name}` }))];
  }, [warehouses, user, hasRole]);

  const destinationWarehouseOptions = useMemo(() => {
    const list = warehouses.filter((w) => Number(w.id) !== Number(form.sourceWarehouseId));
    return [{ value: '', label: 'Chọn kho đích' }, ...list.map((w) => ({ value: w.id, label: `${w.code} - ${w.name}` }))];
  }, [warehouses, form.sourceWarehouseId]);

  const setItem = (index, patch) => {
    setForm((current) => ({
      ...current,
      items: current.items.map((item, itemIndex) => (itemIndex === index ? { ...item, ...patch } : item)),
    }));
  };

  const createTransfer = async () => {
    try {
      const source = warehouses.find((warehouse) => warehouse.id === normalizeId(form.sourceWarehouseId));
      const destination = warehouses.find((warehouse) => warehouse.id === normalizeId(form.destinationWarehouseId));
      if (!source) {
        throw new Error('Vui lòng chọn kho nguồn');
      }
      if (!destination) {
        throw new Error('Vui lòng chọn kho đích');
      }
      if (source.id === destination.id) {
        throw new Error('Kho nguồn và kho đích phải khác nhau');
      }
      if (!form.items.length) {
        throw new Error('Phiếu điều chuyển cần ít nhất một dòng hàng');
      }
      const payload = {
        externalInstructionCode: form.externalInstructionCode,
        sourceWarehouseId: source.id,
        destinationWarehouseId: destination.id,
        documentDate: form.documentDate,
        plannedDate: form.plannedDate,
        notes: form.notes,
        items: form.items.map((item, index) => {
          const product = products.find((row) => row.id === normalizeId(item.productId));
          if (!product) {
            throw new Error('Vui lòng chọn sản phẩm cho tất cả dòng hàng');
          }
          if (Number(item.plannedQty) <= 0) {
            throw new Error('Số lượng đặt phải lớn hơn 0');
          }
          const availability = availabilityByLine[index];
          if (availability && !availability.error && Number(item.plannedQty) > Number(availability.availableQty ?? 0)) {
            throw new Error(`Số lượng đặt của ${product.sku} vượt tồn khả dụng`);
          }
          return {
            productId: product.id,
            plannedQty: Number(item.plannedQty),
          };
        }),
      };
      const created = await interWarehouseTransferService.createTransfer(payload);
      addToast('Đã tạo phiếu điều chuyển thành công', 'success');
      setFormOpen(false);
      await loadData();
      setSelectedId(created.id);
    } catch (error) {
      addToast(error.message || 'Không thể tạo phiếu điều chuyển', 'error');
    }
  };

  const handleAction = async (name, payload) => {
    try {
      const id = selectedTransfer.id;
      const actions = {
        approve: () => interWarehouseTransferService.approveTransfer(id),
        reject: () => interWarehouseTransferService.rejectTransfer(id, payload),
        cancel: () => interWarehouseTransferService.cancelTransfer(id, payload),
        assignTrip: () => interWarehouseTransferService.assignTrip(id, payload),
        recordSourceLoadReport: () => interWarehouseTransferService.recordSourceLoadReport(id, payload.items, payload.reworkReason),
        ship: () => interWarehouseTransferService.shipTransfer(id),
        unship: () => interWarehouseTransferService.unshipTransfer(id),
        recordOutboundQc: () => interWarehouseTransferService.recordOutboundQc(id, payload),
        loadHandover: () => interWarehouseTransferService.loadHandover(id, payload),
        depart: () => interWarehouseTransferService.departTransfer(id),
        driverArrive: () => interWarehouseTransferService.driverArrive(id),
        receivingHandover: () => interWarehouseTransferService.receivingHandover(id, payload),
        receiveCount: () => interWarehouseTransferService.receiveCount(id, payload),
        receiveCheck: () => interWarehouseTransferService.receiveCheck(id, payload),
        finalReceive: () => interWarehouseTransferService.finalReceive(id, payload),
        returnToSource: () => interWarehouseTransferService.returnToSource(id, payload),
        quarantineReject: () => interWarehouseTransferService.quarantineReject(id, payload),
        requestReturn: () => interWarehouseTransferService.requestReturn(id, payload),
        approveReturn: () => interWarehouseTransferService.approveReturn(id),
        rejectReturn: () => interWarehouseTransferService.rejectReturn(id, payload),
        returnDepart: () => interWarehouseTransferService.returnDepart(id),
        returnArrive: () => interWarehouseTransferService.returnArrive(id),
        returnHandover: () => interWarehouseTransferService.returnHandover(id, payload),
      };
      const updated = await actions[name]();
      addToast('Đã cập nhật phiếu điều chuyển', 'success');
      await loadData();
      setSelectedId(updated.id);
    } catch (error) {
      addToast(error.message || 'Thao tác điều chuyển thất bại', 'error');
    }
  };

  return (
    <div className="mobile-page">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 md:gap-4 flex-wrap">
        <div className="flex-1 min-w-0">
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Vận hành / Điều chuyển
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Điều chuyển nội bộ
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Lập phiếu thủ công từ lệnh Công ty mẹ, giữ chỗ, xuất hàng, vận chuyển và xác nhận nhận hàng.
          </p>
        </div>
        <div className="mobile-filter-bar sm:flex sm:gap-2 sm:flex-shrink-0">
          <Button icon={RefreshCw} variant="outline-light" onClick={loadData} loading={loading}>Tải lại</Button>
          {canCreateTransfer && (
            <Button icon={Plus} onClick={() => setFormOpen((value) => !value)}>Tạo phiếu</Button>
          )}
        </div>
      </div>

      {formOpen && (
        <div className="border border-hairline-light rounded-lg bg-canvas-light p-3 md:p-4 grid grid-cols-1 md:grid-cols-4 gap-3">
          <Input label="Mã lệnh Công ty mẹ" value={form.externalInstructionCode} onChange={(e) => setForm({ ...form, externalInstructionCode: e.target.value })} />
          <Input type="select" label="Kho nguồn" value={form.sourceWarehouseId} onChange={(e) => setForm({ ...form, sourceWarehouseId: e.target.value })}
            options={sourceWarehouseOptions} />
          <Input type="select" label="Kho đích" value={form.destinationWarehouseId} onChange={(e) => setForm({ ...form, destinationWarehouseId: e.target.value })}
            options={destinationWarehouseOptions} />
          <Input type="date" label="Ngày chứng từ" value={form.documentDate} onChange={(e) => setForm({ ...form, documentDate: e.target.value })} />
          <Input type="date" label="Ngày dự kiến" value={form.plannedDate} onChange={(e) => setForm({ ...form, plannedDate: e.target.value })} />
          <Input className="md:col-span-3" label="Ghi chú" value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          <div className="md:col-span-4 flex flex-col gap-2">
            {form.items.map((item, index) => (
              <div key={index} className="grid grid-cols-1 md:grid-cols-[1fr_160px_160px_120px] gap-2 items-end">
                <Input type="select" label="Sản phẩm" value={item.productId} onChange={(e) => setItem(index, { productId: e.target.value })}
                  options={[{ value: '', label: 'Chọn SKU' }, ...products.map((product) => ({ value: product.id, label: `${product.sku} - ${product.name}` }))]} />
                <Input label="Số lượng đặt" type="number" min="0.01" step="0.01" value={item.plannedQty} onChange={(e) => setItem(index, { plannedQty: e.target.value })} />
                <div className="min-h-[44px] rounded-md border border-hairline-light bg-canvas-cream/50 px-3 py-2 text-xs">
                  <div className="font-semibold uppercase tracking-wider text-shade-60">Tồn khả dụng</div>
                  {availabilityByLine[index]?.error ? (
                    <div className="text-danger-600">{availabilityByLine[index].error}</div>
                  ) : availabilityByLine[index] ? (
                    <div className={Number(item.plannedQty) > Number(availabilityByLine[index].availableQty ?? 0) ? 'text-danger-600 font-semibold' : 'text-ink'}>
                      {availabilityByLine[index].availableQty ?? 0}
                      <span className="text-shade-50 font-normal"> / giữ {availabilityByLine[index].reservedQty ?? 0}</span>
                    </div>
                  ) : (
                    <div className="text-shade-50">Chưa chọn</div>
                  )}
                </div>
                <Button variant="outline-light" onClick={() => setForm((current) => ({ ...current, items: current.items.filter((_, itemIndex) => itemIndex !== index) }))}>Xóa dòng</Button>
              </div>
            ))}
            <div className="flex gap-2">
              <Button variant="outline-light" onClick={() => setForm((current) => ({ ...current, items: [...current.items, { productId: '', plannedQty: 1 }] }))}>Thêm dòng</Button>
              <Button onClick={createTransfer}>Lưu phiếu</Button>
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_minmax(360px,0.8fr)] gap-4">
        <div className="border border-hairline-light rounded-lg bg-canvas-light shadow-level-3 overflow-hidden">
          <div className="p-4 flex flex-col md:flex-row gap-4 items-center justify-between border-b border-hairline-light">
            <div className="flex flex-col md:flex-row gap-4 items-center w-full md:w-auto">
              <div className="w-full md:w-80">
                <Input type="text" leftIcon={Search} value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} placeholder="Tìm mã phiếu, mã lệnh..." />
              </div>
              <Input
                type="select"
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                options={[
                  { value: 'ALL', label: 'Tất cả trạng thái' },
                  { value: 'NEW', label: 'NEW' },
                  { value: 'APPROVED', label: 'APPROVED' },
                  { value: 'IN_TRANSIT', label: 'IN_TRANSIT' },
                  { value: 'COMPLETED', label: 'COMPLETED' },
                  { value: 'COMPLETED_WITH_DISCREPANCY', label: 'COMPLETED_WITH_DISCREPANCY' },
                  { value: 'REJECTED', label: 'REJECTED' },
                  { value: 'CANCELLED', label: 'CANCELLED' },
                ]}
              />
            </div>
          </div>
          {!filteredTransfers.length ? (
            <div className="px-6 py-8 text-center text-shade-50 text-xs">Không có phiếu điều chuyển phù hợp.</div>
          ) : (
            <>
              {/* Desktop/tablet: table view */}
              <div className="hidden md:block overflow-x-auto">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="bg-canvas-cream border-b border-hairline-light">
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Phiếu</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Tuyến</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Trạng thái</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Dòng hàng</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {paginatedTransfers.map((transfer) => (
                      <tr key={transfer.id} onClick={() => setSelectedId(transfer.id)}
                        className={`cursor-pointer hover:bg-canvas-cream/50 transition-colors ${selectedId === transfer.id ? 'bg-aloe-10/30' : ''}`}>
                        <td className="px-6 py-4">
                          <div className="font-semibold">{transfer.transferNumber}</div>
                          <div className="text-xs text-shade-50">{transfer.externalInstructionCode}</div>
                        </td>
                        <td className="px-6 py-4 text-xs">{transfer.sourceWarehouseCode} → {transfer.destinationWarehouseCode}</td>
                        <td className="px-6 py-4"><InterWarehouseTransferStatusBadge status={transfer.status} /></td>
                        <td className="px-6 py-4 text-right">{transfer.items?.length || 0}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Mobile: stacked card view */}
              <div className="flex flex-col divide-y divide-hairline-light md:hidden">
                {paginatedTransfers.map((transfer) => (
                  <div
                    key={transfer.id}
                    onClick={() => setSelectedId(transfer.id)}
                    className={`p-4 flex flex-col gap-1.5 text-xs cursor-pointer hover:bg-canvas-cream/50 transition-colors ${selectedId === transfer.id ? 'bg-aloe-10/30' : ''}`}
                  >
                    <div className="flex justify-between items-center gap-2">
                      <span className="font-semibold">{transfer.transferNumber}</span>
                      <InterWarehouseTransferStatusBadge status={transfer.status} />
                    </div>
                    <span className="text-shade-50">{transfer.externalInstructionCode}</span>
                    <span>{transfer.sourceWarehouseCode} → {transfer.destinationWarehouseCode}</span>
                    <span className="text-shade-50">Dòng hàng: <span className="font-medium text-ink">{transfer.items?.length || 0}</span></span>
                  </div>
                ))}
              </div>
              <Pagination
                currentPage={safePage}
                totalPages={totalPages}
                totalItems={filteredTransfers.length}
                pageSize={pageSize}
                onPageChange={setCurrentPage}
                onPageSizeChange={(size) => {
                  setPageSize(size);
                  setCurrentPage(1);
                }}
                pageSizeOptions={[10, 25, 50]}
              />
            </>
          )}
        </div>

        <div className="flex flex-col gap-4">
          {selectedTransfer && (
            <div className="border border-hairline-light rounded-lg bg-canvas-light p-4">
              <div className="flex items-center justify-between mb-3">
                <div>
                  <div className="text-xs font-bold uppercase tracking-wider text-shade-60">Chi tiết hàng</div>
                  <div className="font-semibold">{selectedTransfer.transferNumber}</div>
                </div>
                <InterWarehouseTransferStatusBadge status={selectedTransfer.status} />
              </div>
              {selectedTransfer.tripId && (
                <div className="mb-3 grid grid-cols-1 md:grid-cols-3 gap-2 rounded-md border border-hairline-light bg-canvas-cream/50 px-3 py-2 text-xs">
                  <span><span className="text-shade-50">Chuyến:</span> <strong>{selectedTransfer.tripNumber || '-'}</strong></span>
                  <span><span className="text-shade-50">Xe:</span> <strong>{selectedTransfer.vehiclePlate || '-'}</strong></span>
                  <span><span className="text-shade-50">Tài xế:</span> <strong>{selectedTransfer.driverName || '-'}</strong></span>
                  <span><span className="text-shade-50">Bắt đầu:</span> <strong>{selectedTransfer.tripPlannedStartAt ? new Date(selectedTransfer.tripPlannedStartAt).toLocaleString('vi-VN') : '-'}</strong></span>
                  <span><span className="text-shade-50">Hạn giao:</span> <strong>{selectedTransfer.tripPlannedEndAt ? new Date(selectedTransfer.tripPlannedEndAt).toLocaleString('vi-VN') : '-'}</strong></span>
                </div>
              )}
              {selectedTransfer.tripWarningActive && (
                <div className={`mb-3 rounded-md border px-3 py-2 text-xs ${
                  selectedTransfer.tripOverdue
                    ? 'border-danger-200 bg-danger-50 text-danger-700'
                    : 'border-warning-200 bg-warning-50 text-warning-700'
                }`}>
                  {selectedTransfer.tripWarningMessage}
                </div>
              )}
              {selectedTransfer.status === 'COMPLETED_WITH_DISCREPANCY' && selectedTransfer.discrepancyReason && (
                <div className="mb-3 rounded-md border border-danger-200 bg-danger-50/80 px-3.5 py-2.5 text-xs text-danger-700 shadow-level-3 flex items-start gap-2 animate-in fade-in duration-200">
                  <AlertCircle className="w-4 h-4 text-danger-500 shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold block mb-0.5">Phát hiện chênh lệch thiếu hàng</span>
                    <span className="font-normal text-danger-600">Lý do: "{selectedTransfer.discrepancyReason}"</span>
                  </div>
                </div>
              )}
              <div className="divide-y divide-hairline-light">
                {selectedTransfer.items?.map((item) => {
                  const sent = item.sentQty !== null ? Number(item.sentQty) : null;
                  const received = item.receivedQty !== null ? Number(item.receivedQty) : null;
                  const hasDiscrepancy = sent !== null && received !== null && (received - sent !== 0);
                  const diff = sent !== null && received !== null ? received - sent : 0;

                  return (
                    <div key={item.id} className="py-2.5 text-xs">
                      <div className="font-semibold text-sm">{item.productSku} <span className="font-normal text-shade-60">{item.productName}</span></div>
                      <div className="grid grid-cols-2 md:grid-cols-3 gap-2 mt-2 bg-canvas-cream p-2 rounded border border-hairline-light">
                        <div><span className="text-shade-50">Kế hoạch:</span> <strong>{item.plannedQty}</strong></div>
                        <div className={Number(item.plannedQty) > Number(selectedAvailabilityByItem[item.id]?.availableQty ?? 0) ? 'text-danger-600 font-semibold' : ''}>
                          <span className="text-shade-50">Khả dụng nguồn:</span> <strong>{selectedAvailabilityByItem[item.id]?.error ? 'Lỗi tải' : (selectedAvailabilityByItem[item.id]?.availableQty ?? '-')}</strong>
                        </div>
                        <div><span className="text-shade-50">Đã xuất đi:</span> <strong className="text-ink">{item.sentQty ?? '-'}</strong></div>
                        <div><span className="text-shade-50">Thực tế nhận:</span> <strong className="text-success-700">{item.receivedQty ?? '-'}</strong></div>
                        <div><span className="text-shade-50">QC đạt/lỗi:</span> <strong>{item.qcPassedQty ?? '0'} / {item.qcFailedQty ?? '0'}</strong></div>
                        {hasDiscrepancy && (
                          <div className="text-danger-600 font-bold bg-danger-100/50 px-1.5 py-0.5 rounded border border-danger-300 w-fit">
                            Chênh lệch: {diff > 0 ? `+${diff}` : diff} cái
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}
          <InterWarehouseTransferActionPanel
            transfer={selectedTransfer}
            currentUser={user}
            hasRole={hasRole}
            hasWarehouseAccess={hasWarehouseAccess}
            vehicles={vehicles}
            drivers={drivers}
            activeWarehouse={activeWarehouse}
            locations={locations}
            onAction={handleAction}
          />
        </div>
      </div>
    </div>
  );
};

export default InterWarehouseTransferWorkspace;
