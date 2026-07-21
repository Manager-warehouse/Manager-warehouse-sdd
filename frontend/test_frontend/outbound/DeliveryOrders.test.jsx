import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import DeliveryOrders from '../../src/pages/Outbound/DeliveryOrders';

vi.mock('../../src/services/outbound.service', () => ({
  outboundService: {
    getDeliveryOrders: vi.fn(),
    createDeliveryOrder: vi.fn(),
    approveDeliveryOrder: vi.fn(),
    cancelDeliveryOrder: vi.fn(),
  },
}));

vi.mock('../../src/services/masterData.service', () => ({
  masterDataService: {
    getDealers: vi.fn(),
    getProducts: vi.fn(),
  },
}));

vi.mock('../../src/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('../../src/stores/ui.store', () => ({
  useUiStore: () => ({ addToast: vi.fn() }),
}));

import { outboundService } from '../../src/services/outbound.service';
import { masterDataService } from '../../src/services/masterData.service';
import { useAuthStore } from '../../src/stores/auth.store';

const mockDOs = [
  {
    id: 1,
    do_number: 'DO-20260722-0001',
    status: 'NEW',
    warehouse_id: 1,
    dealer_id: 10,
    order_date: '2026-07-22',
    delivery_address: 'Hải Phòng',
    items: [],
  },
  {
    id: 2,
    do_number: 'DO-20260722-0002',
    status: 'APPROVED',
    warehouse_id: 1,
    dealer_id: 10,
    order_date: '2026-07-22',
    delivery_address: 'Hà Nội',
    items: [],
  },
];

const mockDealers = [{ id: 10, name: 'Đại lý Minh Hùng' }];

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/outbound/orders']}>
    <Routes>
      <Route path="/outbound/orders" element={<DeliveryOrders />} />
    </Routes>
  </MemoryRouter>
);

describe('DeliveryOrders Component', () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.mockImplementation((selector) => {
      const state = {
        activeWarehouse: { id: 1, name: 'Kho Hải Phòng', code: 'WH-HP' },
        user: { id: 1, role: 'PLANNER' },
        hasRole: () => true,
      };
      return selector ? selector(state) : state;
    });

    outboundService.getDeliveryOrders.mockImplementation((whId, params) => {
      const search = params?.search;
      if (search) {
        return Promise.resolve(mockDOs.filter(d => d.do_number.includes(search)));
      }
      return Promise.resolve(mockDOs);
    });
    masterDataService.getDealers.mockResolvedValue(mockDealers);
    masterDataService.getProducts.mockResolvedValue([]);
  });

  it('renders title, stats cards and DO list', async () => {
    renderComponent();

    expect(await screen.findByText('Đơn xuất hàng')).toBeInTheDocument();
    expect(screen.getAllByText('DO-20260722-0001').length).toBeGreaterThan(0);
    expect(screen.getAllByText('DO-20260722-0002').length).toBeGreaterThan(0);
  });

  it('filters delivery orders by search input', async () => {
    renderComponent();

    await screen.findByText('Đơn xuất hàng');

    const searchInput = screen.getByPlaceholderText('Tìm mã DO, tên đại lý...');
    fireEvent.change(searchInput, { target: { value: 'DO-20260722-0001' } });

    await waitFor(() => {
      expect(screen.getAllByText('DO-20260722-0001').length).toBeGreaterThan(0);
      expect(screen.queryAllByText('DO-20260722-0002').length).toBe(0);
    }, { timeout: 2000 });
  });
});
