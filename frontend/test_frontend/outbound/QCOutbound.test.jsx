import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import QCOutbound from '../../src/pages/Outbound/QCOutbound';

vi.mock('../../src/services/outbound.service', () => ({
  outboundService: {
    getDeliveryOrderById: vi.fn(),
    confirmQCOutbound: vi.fn(),
  },
}));

vi.mock('../../src/services/masterData.service', () => ({
  masterDataService: {
    getBinLocations: vi.fn(),
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

const mockDO = {
  id: 1,
  do_number: 'DO-20260722-0001',
  status: 'PICKED',
  warehouse_id: 1,
  dealer_name: 'Đại lý An Phát',
  items: [
    {
      id: 101,
      product_name: 'Nồi Sunhouse',
      sku: 'SKU-POT-001',
      planned_qty: 10,
      allocations: [{ allocation_id: 1, location_id: 10, planned_qty: 10 }],
    },
  ],
};

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/outbound/qc/1']}>
    <Routes>
      <Route path="/outbound/qc/:id" element={<QCOutbound />} />
    </Routes>
  </MemoryRouter>
);

describe('QCOutbound Component', () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.mockImplementation((selector) => {
      const state = {
        activeWarehouse: { id: 1, name: 'Kho Hải Phòng', code: 'WH-HP' },
        user: { id: 1, role: 'STOREKEEPER' },
        hasRole: () => true,
      };
      return selector ? selector(state) : state;
    });

    outboundService.getDeliveryOrderById.mockResolvedValue(mockDO);
    masterDataService.getBinLocations.mockResolvedValue([
      { id: 10, code: 'STAGING-01', is_quarantine: false },
      { id: 99, code: 'QUAR-01', is_quarantine: true },
    ]);
  });

  it('renders title and DO details', async () => {
    renderComponent();

    expect(await screen.findByText(/Ghi nhận lấy hàng & kiểm định theo phân bổ: DO-20260722-0001/i)).toBeInTheDocument();
    expect(screen.getByText('Đại lý An Phát')).toBeInTheDocument();
    expect(screen.getByText('Nồi Sunhouse')).toBeInTheDocument();
  });

  it('allows confirming QC when clicking confirm button', async () => {
    renderComponent();

    const confirmBtn = await screen.findByRole('button', { name: /Gửi kết quả lấy hàng & kiểm định/i });
    fireEvent.click(confirmBtn);

    expect(outboundService.confirmQCOutbound).toHaveBeenCalled();
  });
});
