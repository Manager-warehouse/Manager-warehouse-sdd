import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import TransferRequestWorkspace from '../../src/pages/InterWarehouseTransfer/TransferRequestWorkspace';

vi.mock('../../src/services/inter-warehouse-transfer.service', () => ({
  interWarehouseTransferService: {
    getTransferRequests: vi.fn(),
    createTransferRequest: vi.fn(),
    updateTransferRequest: vi.fn(),
    submitTransferRequest: vi.fn(),
    cancelTransferRequest: vi.fn(),
    approveTransferRequest: vi.fn(),
    rejectTransferRequest: vi.fn(),
    convertTransferRequest: vi.fn(),
    stockLookup: vi.fn(),
  },
}));

vi.mock('../../src/services/masterData.service', () => ({
  masterDataService: {
    getWarehouses: vi.fn(),
    getProducts: vi.fn(),
  },
}));

vi.mock('../../src/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('../../src/stores/ui.store', () => ({
  useUiStore: () => ({ addToast: vi.fn() }),
}));

import { interWarehouseTransferService } from '../../src/services/inter-warehouse-transfer.service';
import { masterDataService } from '../../src/services/masterData.service';
import { useAuthStore } from '../../src/stores/auth.store';

const mockTransferRequests = [
  {
    id: 1,
    requestNumber: 'TR-20260722-0001',
    sourceWarehouseId: 1,
    destinationWarehouseId: 2,
    status: 'NEW',
    priority: 'NORMAL',
    requestDate: '2026-07-22',
    note: 'Yêu cầu chuyển hàng gấp',
    items: [],
  },
];

const mockWarehouses = [
  { id: 1, name: 'Kho Hải Phòng', code: 'WH-HP' },
  { id: 2, name: 'Kho Hà Nội', code: 'WH-HN' },
];

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/inter-warehouse-transfer/requests']}>
    <Routes>
      <Route path="/inter-warehouse-transfer/requests" element={<TransferRequestWorkspace />} />
    </Routes>
  </MemoryRouter>
);

describe('TransferRequestWorkspace Component', () => {
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

    interWarehouseTransferService.getTransferRequests.mockResolvedValue(mockTransferRequests);
    masterDataService.getWarehouses.mockResolvedValue(mockWarehouses);
    masterDataService.getProducts.mockResolvedValue([]);
  });

  it('renders title, active warehouse and transfer request list', async () => {
    renderComponent();

    expect(await screen.findByText('Yêu cầu điều chuyển nội bộ')).toBeInTheDocument();
    expect(screen.getByText('TR-20260722-0001')).toBeInTheDocument();
  });

  it('opens create transfer request modal on button click', async () => {
    renderComponent();

    const createBtn = await screen.findByRole('button', { name: /Tạo yêu cầu/i });
    fireEvent.click(createBtn);

    expect(await screen.findByText('Tạo yêu cầu điều chuyển mới')).toBeInTheDocument();
  });
});
