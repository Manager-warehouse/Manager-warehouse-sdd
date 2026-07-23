import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import InterWarehouseTransferActionPanel from './InterWarehouseTransferActionPanel';
import { ROLES } from '../../utils/constants';

vi.mock('../../stores/ui.store', () => ({
  useUiStore: () => ({ addToast: vi.fn() }),
}));

vi.mock('../../components/common/PhotoCaptureInput', () => ({
  default: ({ label, onChange }) => (
    <button type="button" onClick={() => onChange(new File(['x'], 'qc.jpg', { type: 'image/jpeg' }))}>
      {label}
    </button>
  ),
}));

const baseTransfer = {
  id: 1,
  transferNumber: 'TRF-20260722-0001',
  sourceWarehouseId: 1,
  sourceWarehouseCode: 'WH-HN',
  destinationWarehouseId: 2,
  destinationWarehouseCode: 'WH-HP',
  status: 'APPROVED',
  tripId: 10,
  driverUserId: 50,
  driverName: 'Tai xe',
  items: [
    {
      id: 101,
      productId: 201,
      productSku: 'SKU-001',
      productName: 'Noi lau dien',
      plannedQty: 10,
      loadedQty: null,
      sentQty: null,
    },
  ],
};

const renderPanel = ({
  transfer = baseTransfer,
  roles = [ROLES.WAREHOUSE_STAFF],
  activeWarehouse = { id: 1, code: 'WH-HN' },
  warehouseAccessIds = [1],
  products = [{ id: 201, sku: 'SKU-001', name: 'Noi lau dien' }, { id: 202, sku: 'SKU-002', name: 'Chao chong dinh' }],
  onAction = vi.fn(),
} = {}) => {
  const roleSet = new Set(roles);
  const warehouseAccessSet = new Set(warehouseAccessIds.map(Number));
  render(
    <InterWarehouseTransferActionPanel
      transfer={transfer}
      currentUser={{ id: 20 }}
      activeWarehouse={activeWarehouse}
      hasRole={(role) => roleSet.has(role)}
      hasWarehouseAccess={(warehouseId) => warehouseAccessSet.has(Number(warehouseId))}
      vehicles={[]}
      drivers={[]}
      locations={[]}
      products={products}
      onAction={onAction}
    />
  );
  return onAction;
};

describe('InterWarehouseTransferActionPanel source load report workflow', () => {
  afterEach(() => cleanup());

  it('shows worker load report before source outbound QC', async () => {
    const onAction = renderPanel();

    expect(screen.getByText('Chờ công nhân xếp/báo số lượng')).toBeInTheDocument();
    expect(screen.queryByText('QC Đạt')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Báo cáo số lượng đã xếp' }));

    await waitFor(() => expect(onAction).toHaveBeenCalledWith('recordSourceLoadReport', {
      items: [{ transferItemId: 101, loadedQty: 10 }],
      reworkReason: '',
    }));
  });

  it('allows storekeeper QC only after loaded quantity is reported', () => {
    renderPanel({
      roles: [ROLES.STOREKEEPER],
      transfer: {
        ...baseTransfer,
        sourceLoadedReportedAt: '2026-07-22T10:00:00Z',
        items: [{ ...baseTransfer.items[0], loadedQty: 10 }],
      },
    });

    expect(screen.getByText('Chờ kiểm tra outbound QC')).toBeInTheDocument();
    expect(screen.getByText('QC Đạt')).toBeDisabled();
    fireEvent.click(screen.getByText('Ảnh xác nhận QC'));
    expect(screen.getByText('QC Đạt')).not.toBeDisabled();
  });

  it('shows only worker rework report after QC failure', async () => {
    const onAction = renderPanel({
      transfer: {
        ...baseTransfer,
        outboundQcPassed: false,
        outboundQcNote: 'Mop meo',
        sourceLoadReworkRequired: true,
        sourceLoadReworkReason: 'Mop meo',
        items: [{ ...baseTransfer.items[0], loadedQty: 10 }],
      },
    });

    expect(screen.getByText('QC xuất kho thất bại - chờ xử lý lại')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Báo cáo lại số lượng xếp' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Xác nhận bàn giao lên xe' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hạ hàng khỏi xe' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Báo cáo lại số lượng xếp' }));
    await waitFor(() => expect(onAction).toHaveBeenCalledWith('recordSourceLoadReport', {
      items: [{ transferItemId: 101, loadedQty: 10 }],
      reworkReason: '',
    }));
  });

  it('shows storekeeper a waiting message instead of unship action after outbound QC failure', () => {
    renderPanel({
      roles: [ROLES.STOREKEEPER],
      transfer: {
        ...baseTransfer,
        outboundQcPassed: false,
        outboundQcNote: 'Mop meo',
        sourceLoadReworkRequired: true,
        sourceLoadReworkReason: 'Mop meo',
        items: [{ ...baseTransfer.items[0], loadedQty: 10 }],
      },
    });

    expect(screen.getByText('QC xuất kho thất bại - chờ xử lý lại')).toBeInTheDocument();
    expect(screen.getByText('QC xuất kho thất bại. Chờ công nhân hạ/đổi/xếp lại hàng và báo cáo lại số lượng trước khi thủ kho QC lại.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Báo cáo lại số lượng xếp' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hạ hàng khỏi xe' })).not.toBeInTheDocument();
  });

  it('does not show unship action during load handover after outbound QC passed', () => {
    renderPanel({
      roles: [ROLES.STOREKEEPER],
      transfer: {
        ...baseTransfer,
        outboundQcPassed: true,
        outboundQcPhotoRef: 'uploads/qc.jpg',
        items: [{ ...baseTransfer.items[0], loadedQty: 10, sentQty: 10 }],
      },
    });

    expect(screen.getByText('Chờ hoàn tất xếp hàng')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Xác nhận bàn giao lên xe' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hạ hàng khỏi xe' })).not.toBeInTheDocument();
  });

  it('hides source warehouse actions when viewing an outbound step from destination warehouse', () => {
    renderPanel({
      roles: [ROLES.STOREKEEPER],
      activeWarehouse: { id: 2, code: 'WH-HP' },
      warehouseAccessIds: [2],
      transfer: {
        ...baseTransfer,
        outboundQcPassed: true,
        outboundQcPhotoRef: 'uploads/qc.jpg',
        items: [{ ...baseTransfer.items[0], loadedQty: 10, sentQty: null }],
      },
    });

    expect(screen.getByText('QC đạt - chờ chốt số lượng xuất')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Hoàn tất xếp hàng' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Xác nhận bàn giao lên xe' })).not.toBeInTheDocument();
  });

  it('shows only worker count action after destination handover is sent to staff', () => {
    renderPanel({
      roles: [ROLES.ADMIN],
      activeWarehouse: { id: 2, code: 'WH-HP' },
      warehouseAccessIds: [1, 2],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: '2026-07-22T10:00:00Z',
        arrivalHandoverAt: '2026-07-22T10:05:00Z',
        arrivalHandoverPhotoRef: 'uploads/handover.jpg',
      },
    });

    expect(screen.getByText('Chờ nhập số lượng thực nhận')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nhập số lượng thực nhận' })).toBeInTheDocument();
    expect(screen.queryByText('Báo sai SKU & Yêu cầu quay đầu xe')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Gửi yêu cầu quay đầu' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Quay đầu về kho nguồn' })).not.toBeInTheDocument();
  });

  it('treats receiving handover photo as the handover gate and hides return forms during count', () => {
    renderPanel({
      roles: [ROLES.WAREHOUSE_STAFF],
      activeWarehouse: { id: 2, code: 'WH-HP' },
      warehouseAccessIds: [2],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: '2026-07-22T10:00:00Z',
        arrivalHandoverAt: null,
        arrivalHandoverPhotoRef: 'uploads/handover.jpg',
        items: [{ ...baseTransfer.items[0], sentQty: 10 }],
      },
    });

    expect(screen.getByText('Chờ nhập số lượng thực nhận')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nhập số lượng thực nhận' })).toBeInTheDocument();
    expect(screen.queryByText('Báo sai SKU & Yêu cầu quay đầu xe')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Quay đầu về kho nguồn' })).not.toBeInTheDocument();
  });

  it('does not prefill destination count before warehouse staff enters quantity', () => {
    renderPanel({
      roles: [ROLES.WAREHOUSE_STAFF],
      activeWarehouse: { id: 2, code: 'WH-HP' },
      warehouseAccessIds: [2],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: '2026-07-22T10:00:00Z',
        arrivalHandoverAt: '2026-07-22T10:05:00Z',
        arrivalHandoverPhotoRef: 'uploads/handover.jpg',
        items: [{ ...baseTransfer.items[0], sentQty: 10 }],
      },
    });

    fireEvent.click(screen.getByRole('button', { name: 'Nhập số lượng thực nhận' }));
    expect(screen.getByLabelText('Số lượng nhận')).toHaveValue(null);
    expect(screen.getByRole('button', { name: 'Hoàn tất báo cáo số lượng' })).toBeDisabled();
  });

  it('hides destination receive QC action when viewing from source warehouse', () => {
    renderPanel({
      roles: [ROLES.STOREKEEPER],
      activeWarehouse: { id: 1, code: 'WH-HN' },
      warehouseAccessIds: [1],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: '2026-07-22T10:00:00Z',
        arrivalHandoverAt: '2026-07-22T10:05:00Z',
        arrivalHandoverPhotoRef: 'uploads/handover.jpg',
        items: [{ ...baseTransfer.items[0], sentQty: 10, workerReceivedQty: 10 }],
      },
    });

    expect(screen.getByText('Chờ kiểm tra count/QC')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Kiểm tra count/QC' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Duyệt QC' })).not.toBeInTheDocument();
  });

  it('keeps direct return-to-source action for source manager only', async () => {
    renderPanel({
      roles: [ROLES.WAREHOUSE_MANAGER],
      activeWarehouse: { id: 2, code: 'WH-HP' },
      warehouseAccessIds: [2],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: null,
      },
    });

    expect(screen.queryByRole('button', { name: 'Quay đầu về kho nguồn' })).not.toBeInTheDocument();

    const onAction = renderPanel({
      roles: [ROLES.WAREHOUSE_MANAGER],
      activeWarehouse: { id: 1, code: 'WH-HN' },
      warehouseAccessIds: [1],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: null,
      },
    });

    fireEvent.change(screen.getByPlaceholderText('Lý do quay đầu bắt buộc...'), {
      target: { value: 'Xe gap su co' },
    });
    fireEvent.click(screen.getByRole('button', { name: 'Quay đầu về kho nguồn' }));

    await waitFor(() => expect(onAction).toHaveBeenCalledWith('returnToSource', {
      reason: 'Xe gap su co',
      wrongSkuItems: [],
    }));
  });

  it('keeps return handover photo confirmation for source storekeeper, not warehouse staff', async () => {
    renderPanel({
      roles: [ROLES.WAREHOUSE_STAFF],
      activeWarehouse: { id: 1, code: 'WH-HN' },
      warehouseAccessIds: [1],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        isReturned: true,
        returnDepartedAt: '2026-07-22T10:00:00Z',
        returnArrivedAt: '2026-07-22T10:30:00Z',
        returnArrivalHandoverAt: null,
      },
    });

    expect(screen.getByText('BƯỚC 3: BÀN GIAO QUAY ĐẦU TẠI KHO NGUỒN')).toBeInTheDocument();
    expect(screen.queryByText('Ảnh bàn giao quay đầu')).not.toBeInTheDocument();
    expect(screen.getByText('Đang chờ thủ kho kho nguồn xác nhận bàn giao quay đầu...')).toBeInTheDocument();
  });

  it('allows source storekeeper to confirm return handover photo after driver arrives back', async () => {
    const onAction = renderPanel({
      roles: [ROLES.STOREKEEPER],
      activeWarehouse: { id: 1, code: 'WH-HN' },
      warehouseAccessIds: [1],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        isReturned: true,
        returnDepartedAt: '2026-07-22T10:00:00Z',
        returnArrivedAt: '2026-07-22T10:30:00Z',
        returnArrivalHandoverAt: null,
      },
    });

    fireEvent.click(screen.getByText('Ảnh bàn giao quay đầu'));
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận Nhận bàn giao quay đầu' }));

    await waitFor(() => expect(onAction).toHaveBeenCalledWith('returnHandover', {
      photoFile: expect.any(File),
    }));
  });

  it('shows only wrong-SKU approval actions when a return request is pending', () => {
    renderPanel({
      roles: [ROLES.WAREHOUSE_MANAGER],
      activeWarehouse: { id: 2, code: 'WH-HP' },
      warehouseAccessIds: [2],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: '2026-07-22T10:00:00Z',
        arrivalHandoverAt: '2026-07-22T10:05:00Z',
        returnRequested: true,
        returnReason: 'Sai SKU',
      },
    });

    expect(screen.getByText('YÊU CẦU QUAY ĐẦU DO SAI SKU ĐANG CHỜ PHÊ DUYỆT')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Duyệt quay xe' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Quay đầu về kho nguồn' })).not.toBeInTheDocument();
  });

  it('shows pending wrong-SKU approval before receiving handover and hides normal handover', () => {
    renderPanel({
      roles: [ROLES.WAREHOUSE_MANAGER],
      activeWarehouse: { id: 2, code: 'WH-HP' },
      warehouseAccessIds: [2],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: '2026-07-22T10:00:00Z',
        arrivalHandoverAt: null,
        returnRequested: true,
        returnReason: 'Sai SKU',
      },
    });

    expect(screen.getByText('Chờ duyệt yêu cầu quay đầu')).toBeInTheDocument();
    expect(screen.getByText('YÊU CẦU QUAY ĐẦU DO SAI SKU ĐANG CHỜ PHÊ DUYỆT')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Duyệt quay xe' })).toBeInTheDocument();
    expect(screen.queryByText('BƯỚC 2: BÀN GIAO TẠI KHO ĐÍCH')).not.toBeInTheDocument();
  });

  it('submits wrong-SKU return request with backend DTO fields before handover', async () => {
    const onAction = renderPanel({
      roles: [ROLES.STOREKEEPER],
      activeWarehouse: { id: 2, code: 'WH-HP' },
      warehouseAccessIds: [2],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: '2026-07-22T10:00:00Z',
        arrivalHandoverAt: null,
        items: [{ ...baseTransfer.items[0], sentQty: 10 }],
      },
    });

    fireEvent.click(screen.getByText('Ảnh bàn giao nhận hàng'));
    fireEvent.click(screen.getByRole('button', { name: 'Báo sai SKU / quay đầu' }));
    fireEvent.change(screen.getByLabelText('Dòng hàng lỗi'), { target: { value: '101' } });
    fireEvent.change(screen.getByLabelText('SKU thực tế nhận'), { target: { value: '202' } });
    fireEvent.change(screen.getByLabelText('Số lượng sai'), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText('Lý do'), { target: { value: 'Nhận nhầm SKU' } });
    fireEvent.click(screen.getByRole('button', { name: 'Thêm dòng' }));
    fireEvent.change(screen.getByPlaceholderText('Nhập lý do chung...'), { target: { value: 'Sai SKU cần quay đầu' } });
    fireEvent.click(screen.getByRole('button', { name: 'Gửi yêu cầu quay đầu' }));

    await waitFor(() => expect(onAction).toHaveBeenCalledWith('requestReturn', {
      reason: 'Sai SKU cần quay đầu',
      wrongSkuItems: [{
        transferItemId: 101,
        expectedProductId: 201,
        actualProductId: 202,
        affectedQty: 2,
        reason: 'Nhận nhầm SKU',
        photoRef: null,
      }],
    }));
  });

  it('keeps return receiving QC scoped to source warehouse and hides full quarantine reject', () => {
    renderPanel({
      roles: [ROLES.STOREKEEPER],
      activeWarehouse: { id: 1, code: 'WH-HN' },
      warehouseAccessIds: [1],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        isReturned: true,
        returnDepartedAt: '2026-07-22T10:00:00Z',
        returnArrivedAt: '2026-07-22T10:30:00Z',
        returnArrivalHandoverAt: '2026-07-22T10:35:00Z',
        items: [{ ...baseTransfer.items[0], sentQty: 10, workerReceivedQty: 10 }],
      },
    });

    expect(screen.getByText('Quay đầu: Chờ kiểm tra count/QC tại kho nguồn')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Từ chối & Cách ly toàn bộ' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Kiểm tra count/QC' })).toBeInTheDocument();
  });

  it('does not show whole-transfer quarantine reject after destination count', () => {
    renderPanel({
      roles: [ROLES.STOREKEEPER],
      activeWarehouse: { id: 2, code: 'WH-HP' },
      warehouseAccessIds: [2],
      transfer: {
        ...baseTransfer,
        status: 'IN_TRANSIT',
        driverArrivedAt: '2026-07-22T10:00:00Z',
        arrivalHandoverAt: '2026-07-22T10:05:00Z',
        arrivalHandoverPhotoRef: 'uploads/handover.jpg',
        items: [{ ...baseTransfer.items[0], sentQty: 10, workerReceivedQty: 10 }],
      },
    });

    expect(screen.getByText('Chờ kiểm tra count/QC')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Kiểm tra count/QC' })).toBeInTheDocument();
    expect(screen.getByText('QC lỗi thì nhập số lượng lỗi theo từng dòng và lý do, hệ thống sẽ đưa phần lỗi vào quarantine khi quản lý xác nhận cuối.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Từ chối & Cách ly toàn bộ' })).not.toBeInTheDocument();
  });
});
