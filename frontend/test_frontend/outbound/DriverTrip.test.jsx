import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import DriverTrip from '../../src/pages/Outbound/DriverTrip';

vi.mock('../../src/services/outbound.service', () => ({
  outboundService: {
    getDriverTrips: vi.fn(),
    getMyTrips: vi.fn(),
    getTrips: vi.fn(),
    startTrip: vi.fn(),
    departTrip: vi.fn(),
    deliverTrip: vi.fn(),
  },
}));

vi.mock('../../src/services/inter-warehouse-transfer.service', () => ({
  interWarehouseTransferService: {
    getTransfers: vi.fn(),
  },
  toTransferDriverTripSummary: (t) => t,
}));

vi.mock('../../src/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('../../src/stores/ui.store', () => ({
  useUiStore: () => ({ addToast: vi.fn() }),
}));

import { outboundService } from '../../src/services/outbound.service';
import { interWarehouseTransferService } from '../../src/services/inter-warehouse-transfer.service';
import { useAuthStore } from '../../src/stores/auth.store';

const mockTrips = [
  {
    id: 1,
    trip_number: 'TRIP-20260722-0001',
    status: 'PLANNED',
    vehicle_plate: '29C-12345',
    driver_name: 'Nguyễn Văn Tài',
    do_count: 2,
    planned_date: '2026-07-22T08:00:00Z',
  },
];

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/outbound/driver/trips']}>
    <Routes>
      <Route path="/outbound/driver/trips" element={<DriverTrip />} />
    </Routes>
  </MemoryRouter>
);

describe('DriverTrip Component', () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.mockImplementation((selector) => {
      const state = {
        activeWarehouse: { id: 1, name: 'Kho Hải Phòng', code: 'WH-HP' },
        user: { id: 1, role: 'DRIVER' },
        hasRole: () => true,
      };
      return selector ? selector(state) : state;
    });

    outboundService.getMyTrips.mockResolvedValue(mockTrips);
    outboundService.getTrips.mockResolvedValue(mockTrips);
    interWarehouseTransferService.getTransfers.mockResolvedValue([]);
  });

  it('renders title and assigned trips', async () => {
    renderComponent();

    expect(await screen.findByText('Chuyến xe của tôi')).toBeInTheDocument();
    expect(await screen.findByText('TRIP-20260722-0001')).toBeInTheDocument();
    expect(screen.getByText('29C-12345')).toBeInTheDocument();
  });

  it('renders driver trips list correctly', async () => {
    renderComponent();

    expect(await screen.findByText('Chuyến xe của tôi')).toBeInTheDocument();
    expect(await screen.findByText('TRIP-20260722-0001')).toBeInTheDocument();
  });
});
