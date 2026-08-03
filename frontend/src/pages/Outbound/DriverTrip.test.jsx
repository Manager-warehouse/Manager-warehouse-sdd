import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi, beforeEach } from 'vitest';
import DriverTrip from './DriverTrip';

vi.mock('../../services/outbound.service', () => ({
  outboundService: {
    getMyTrips: vi.fn(),
    getTrips: vi.fn(),
    getTripById: vi.fn(),
    getDriverTripById: vi.fn(),
    departSplitDeliveryPlan: vi.fn(),
    confirmSplitDealerArrival: vi.fn(),
    confirmSplitHandover: vi.fn(),
    completeSplitDeliveryPlan: vi.fn(),
    completeTrip: vi.fn(),
  },
}));

vi.mock('../../services/inter-warehouse-transfer.service', () => ({
  interWarehouseTransferService: {
    getTransfers: vi.fn(),
    getTransferById: vi.fn(),
    returnDepart: vi.fn(),
    returnArrive: vi.fn(),
  },
  toTransferDriverTripSummary: (transfer = {}) => ({
    id: `transfer-${transfer.id}`,
    transfer_id: transfer.id,
    transferId: transfer.id,
    trip_id: transfer.tripId,
    trip_type: 'TRANSFER',
    trip_type_label: 'Dieu chuyen noi bo',
    trip_number: transfer.tripNumber || transfer.transferNumber,
    status: transfer.status === 'APPROVED' ? 'PLANNED' : transfer.status,
    source_warehouse_code: transfer.sourceWarehouseCode,
    destination_warehouse_code: transfer.destinationWarehouseCode,
    vehicle_plate: transfer.vehiclePlate,
    planned_start_at: transfer.tripPlannedStartAt,
    total_weight_kg: transfer.totalWeightKg || 0,
    transfer_line_count: transfer.items?.length || 0,
    items: transfer.items || [],
    isReturned: Boolean(transfer.isReturned),
    returnReason: transfer.returnReason,
    returnDepartedAt: transfer.returnDepartedAt,
    returnArrivedAt: transfer.returnArrivedAt,
    returnArrivalHandoverAt: transfer.returnArrivalHandoverAt,
    delivery_orders: [],
  }),
}));

vi.mock('../../stores/auth.store', () => ({
  useAuthStore: () => ({ user: { id: 10, role: 'DRIVER' } }),
}));

vi.mock('../../stores/ui.store', () => ({
  useUiStore: () => ({ addToast: vi.fn() }),
}));

vi.mock('../../components/common/PhotoCaptureInput', () => ({
  default: () => <div data-testid="photo-capture" />,
}));

vi.mock('../../components/warehouse/OTPInput', () => ({
  default: () => <div data-testid="otp-input" />,
}));

import { outboundService } from '../../services/outbound.service';
import { interWarehouseTransferService } from '../../services/inter-warehouse-transfer.service';

const deliveryTrip = {
  id: 1,
  trip_number: 'TRIP-20260719-0001',
  status: 'PLANNED',
  trip_type: 'DELIVERY',
  trip_type_label: 'Giao dai ly',
  vehicle_plate: '15C-11111',
  planned_start_at: '2026-07-19T08:00:00',
  delivery_stop_count: 2,
  total_weight_kg: 20,
  delivery_orders: [{ do_id: 101 }, { do_id: 102 }],
};

const transferTrip = {
  id: 2,
  trip_number: 'TTR-20260719-0001',
  status: 'PLANNED',
  trip_type: 'TRANSFER',
  trip_type_label: 'Dieu chuyen noi bo',
  transfer_id: 500,
  vehicle_plate: '29C-22222',
  planned_start_at: '2026-07-19T09:00:00',
  source_warehouse_code: 'WH-HP',
  destination_warehouse_code: 'WH-HN',
  transfer_line_count: 3,
  total_weight_kg: 30,
  delivery_orders: [],
};

const renderList = () => render(
  <MemoryRouter initialEntries={['/outbound/driver/trips']}>
    <Routes>
      <Route path="/outbound/driver/trips" element={<DriverTrip />} />
    </Routes>
  </MemoryRouter>
);

describe('DriverTrip list filters', () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    outboundService.getMyTrips.mockResolvedValue([deliveryTrip, transferTrip]);
    interWarehouseTransferService.getTransfers.mockResolvedValue([]);
  });

  it('renders Tat ca, Noi bo, and Dai ly filters with empty filtered state', async () => {
    renderList();

    expect(await screen.findByText('TRIP-20260719-0001')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Tất cả' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nội bộ' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Đại lý' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Nội bộ' }));
    expect(screen.queryByText('TRIP-20260719-0001')).not.toBeInTheDocument();
    expect(screen.getByText('TTR-20260719-0001')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Đại lý' }));
    expect(screen.getByText('TRIP-20260719-0001')).toBeInTheDocument();
    expect(screen.queryByText('TTR-20260719-0001')).not.toBeInTheDocument();

    outboundService.getMyTrips.mockResolvedValueOnce([transferTrip]);
    interWarehouseTransferService.getTransfers.mockResolvedValueOnce([]);
    renderList();
    await waitFor(() => expect(screen.getAllByText('TTR-20260719-0001').length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByRole('button', { name: 'Đại lý' }).at(-1));
    expect(screen.getByText('Không có chuyến xe phù hợp với bộ lọc hiện tại.')).toBeInTheDocument();
  });

  it('renders transfer cards with route and no dealer POD/OTP wording', async () => {
    renderList();

    expect(await screen.findByText('TTR-20260719-0001')).toBeInTheDocument();
    expect(screen.getByText('Dieu chuyen noi bo')).toBeInTheDocument();
    expect(screen.getByText(/WH-HP/)).toHaveTextContent('WH-HP → WH-HN');
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.queryByText(/Giao hang \(OTP\)/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/POD/i)).not.toBeInTheDocument();
  });

  it('lets assigned transfer driver confirm return departure after manager return request', async () => {
    const returnedTransfer = {
      ...transferTrip,
      id: 500,
      status: 'IN_TRANSIT',
      driverUserId: 10,
      isReturned: true,
      returnReason: 'Xe gặp sự cố giữa đường',
      returnDepartedAt: null,
      returnArrivedAt: null,
      items: [{ id: 1, productSku: 'SKU-1', productName: 'Noi', plannedQty: 10, sentQty: 10 }],
    };
    interWarehouseTransferService.getTransferById.mockResolvedValue(returnedTransfer);
    interWarehouseTransferService.returnDepart.mockResolvedValueOnce({});

    render(
      <MemoryRouter initialEntries={['/outbound/driver/trips/transfer-500']}>
        <Routes>
          <Route path="/outbound/driver/trips/:id" element={<DriverTrip />} />
        </Routes>
      </MemoryRouter>
    );

    const button = await screen.findByRole('button', { name: /Xác nhận quay đầu về kho nguồn/i });
    fireEvent.click(button);

    await waitFor(() => expect(interWarehouseTransferService.returnDepart).toHaveBeenCalledWith(500));
  });

  it('lets assigned transfer driver confirm arrival back at source warehouse', async () => {
    const returnedTransfer = {
      ...transferTrip,
      id: 500,
      status: 'IN_TRANSIT',
      driverUserId: 10,
      isReturned: true,
      returnReason: 'Xe gặp sự cố giữa đường',
      returnDepartedAt: '2026-07-22T10:00:00',
      returnArrivedAt: null,
      items: [{ id: 1, productSku: 'SKU-1', productName: 'Noi', plannedQty: 10, sentQty: 10 }],
    };
    interWarehouseTransferService.getTransferById.mockResolvedValue(returnedTransfer);
    interWarehouseTransferService.returnArrive.mockResolvedValueOnce({});

    render(
      <MemoryRouter initialEntries={['/outbound/driver/trips/transfer-500']}>
        <Routes>
          <Route path="/outbound/driver/trips/:id" element={<DriverTrip />} />
        </Routes>
      </MemoryRouter>
    );

    const button = await screen.findByRole('button', { name: /Xác nhận đã về tới kho nguồn/i });
    fireEvent.click(button);

    await waitFor(() => expect(interWarehouseTransferService.returnArrive).toHaveBeenCalledWith(500));
  });

  it('lets only the split lead confirm whole-convoy arrival', async () => {
    outboundService.getDriverTripById.mockResolvedValue({
      ...deliveryTrip,
      status: 'IN_TRANSIT',
      delivery_orders: [{
        do_id: 101,
        do_number: 'DO-101',
        dealer_name: 'Dai ly A',
        delivery_status: 'IN_TRANSIT',
        split_plan_id: 900,
        split_leg_id: 901,
        is_split_lead: true,
        dealer_arrived_at: null,
      }],
    });
    outboundService.confirmSplitDealerArrival.mockResolvedValue({});

    render(
      <MemoryRouter initialEntries={['/outbound/driver/trips/1']}>
        <Routes>
          <Route path="/outbound/driver/trips/:id" element={<DriverTrip />} />
        </Routes>
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận đến đại lý' }));

    await waitFor(() => expect(outboundService.confirmSplitDealerArrival)
      .toHaveBeenCalledWith(900));
  });

  it('hides split workflow actions from a support driver', async () => {
    outboundService.getDriverTripById.mockResolvedValue({
      ...deliveryTrip,
      status: 'IN_TRANSIT',
      delivery_orders: [{
        do_id: 101,
        do_number: 'DO-101',
        dealer_name: 'Dai ly A',
        delivery_status: 'IN_TRANSIT',
        split_plan_id: 900,
        split_leg_id: 901,
        is_split_lead: false,
      }],
    });
    render(
      <MemoryRouter initialEntries={['/outbound/driver/trips/1']}>
        <Routes>
          <Route path="/outbound/driver/trips/:id" element={<DriverTrip />} />
        </Routes>
      </MemoryRouter>
    );

    await screen.findByText('DO-101');

    expect(screen.queryByRole('button', { name: 'Xác nhận đến đại lý' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Xác nhận xe đã về kho' })).not.toBeInTheDocument();
  });

  it('lets the split lead complete the whole convoy return', async () => {
    outboundService.getDriverTripById.mockResolvedValue({
      ...deliveryTrip,
      status: 'IN_TRANSIT',
      delivery_orders: [{
        do_id: 101,
        do_number: 'DO-101',
        dealer_name: 'Dai ly A',
        delivery_status: 'COMPLETED',
        split_plan_id: 900,
        split_leg_id: 901,
        is_split_lead: true,
      }],
    });
    outboundService.completeSplitDeliveryPlan.mockResolvedValue({});

    render(
      <MemoryRouter initialEntries={['/outbound/driver/trips/1']}>
        <Routes>
          <Route path="/outbound/driver/trips/:id" element={<DriverTrip />} />
        </Routes>
      </MemoryRouter>
    );

    fireEvent.click(await screen.findByRole('button', { name: 'Xác nhận xe đã về kho' }));

    await waitFor(() => expect(outboundService.completeSplitDeliveryPlan).toHaveBeenCalledWith(900));
    expect(outboundService.completeTrip).not.toHaveBeenCalled();
  });
});
