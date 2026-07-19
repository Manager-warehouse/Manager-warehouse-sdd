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
  },
}));

vi.mock('../../services/inter-warehouse-transfer.service', () => ({
  interWarehouseTransferService: {
    getTransfers: vi.fn(),
    getTransferById: vi.fn(),
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
    expect(screen.getByRole('button', { name: 'Tat ca' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Noi bo' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Dai ly' })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Noi bo' }));
    expect(screen.queryByText('TRIP-20260719-0001')).not.toBeInTheDocument();
    expect(screen.getByText('TTR-20260719-0001')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Dai ly' }));
    expect(screen.getByText('TRIP-20260719-0001')).toBeInTheDocument();
    expect(screen.queryByText('TTR-20260719-0001')).not.toBeInTheDocument();

    outboundService.getMyTrips.mockResolvedValueOnce([transferTrip]);
    interWarehouseTransferService.getTransfers.mockResolvedValueOnce([]);
    renderList();
    await waitFor(() => expect(screen.getAllByText('TTR-20260719-0001').length).toBeGreaterThan(0));
    fireEvent.click(screen.getAllByRole('button', { name: 'Dai ly' }).at(-1));
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
});
