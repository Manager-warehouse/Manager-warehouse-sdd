import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ReceiptList from '../../src/pages/Inbound/ReceiptList';

vi.mock('../../src/services/inbound.service', () => ({
  inboundService: {
    getReceipts: vi.fn(),
    getReceiptById: vi.fn(),
    approveReceipt: vi.fn(),
    rejectReceipt: vi.fn(),
    qcReceipt: vi.fn(),
  },
}));

vi.mock('../../src/services/masterData.service', () => ({
  masterDataService: {
    getSuppliers: vi.fn(),
    getDealers: vi.fn(),
  },
}));

vi.mock('../../src/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('../../src/stores/ui.store', () => ({
  useUiStore: () => ({ addToast: vi.fn() }),
}));

import { inboundService } from '../../src/services/inbound.service';
import { masterDataService } from '../../src/services/masterData.service';
import { useAuthStore } from '../../src/stores/auth.store';

const mockReceipts = [
  {
    id: 1,
    receipt_number: 'REC-20260722-0001',
    type: 'PURCHASE',
    status: 'PENDING_RECEIPT',
    supplier_id: 10,
    source_reference: 'PO-1001',
    document_date: '2026-07-22',
    items: [],
  },
  {
    id: 2,
    receipt_number: 'REC-20260722-0002',
    type: 'RETURN',
    status: 'APPROVED',
    dealer_id: 20,
    source_reference: 'RET-2001',
    document_date: '2026-07-22',
    items: [{ id: 1, qc_passed_qty: 5, location_id: 101 }],
  },
];

const mockSuppliers = [{ id: 10, company_name: 'Nhà cung cấp An Phát' }];
const mockDealers = [{ id: 20, name: 'Đại lý Minh Hùng' }];

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/inbound/receipts']}>
    <Routes>
      <Route path="/inbound/receipts" element={<ReceiptList />} />
    </Routes>
  </MemoryRouter>
);

describe('ReceiptList Component', () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.mockImplementation((selector) => {
      const state = {
        activeWarehouse: { id: 1, name: 'Kho Hải Phòng', code: 'WH-HP' },
        user: { id: 1, role: 'ADMIN' },
        hasRole: () => true,
      };
      return selector ? selector(state) : state;
    });

    inboundService.getReceipts.mockResolvedValue(mockReceipts);
    masterDataService.getSuppliers.mockResolvedValue(mockSuppliers);
    masterDataService.getDealers.mockResolvedValue(mockDealers);
  });

  it('renders title, active warehouse and receipts list', async () => {
    renderComponent();

    const matches1 = await screen.findAllByText('REC-20260722-0001');
    expect(matches1.length).toBeGreaterThan(0);
    const matches2 = await screen.findAllByText('REC-20260722-0002');
    expect(matches2.length).toBeGreaterThan(0);
    const suppliers = await screen.findAllByText('Nhà cung cấp An Phát');
    expect(suppliers.length).toBeGreaterThan(0);
    const dealers = await screen.findAllByText('Đại lý Minh Hùng');
    expect(dealers.length).toBeGreaterThan(0);
  });

  it('filters receipts by search term', async () => {
    renderComponent();

    await screen.findAllByText('Nhà cung cấp An Phát');

    const searchInput = screen.getByPlaceholderText('Tìm mã phiếu, số chứng từ...');
    fireEvent.change(searchInput, { target: { value: 'PO-1001' } });

    await waitFor(() => {
      expect(screen.getAllByText('REC-20260722-0001').length).toBeGreaterThan(0);
      expect(screen.queryByText('REC-20260722-0002')).not.toBeInTheDocument();
    });
  });

  it('filters receipts by status select', async () => {
    renderComponent();

    await screen.findAllByText('Nhà cung cấp An Phát');

    const statusSelect = screen.getAllByRole('combobox')[0];
    fireEvent.change(statusSelect, { target: { value: 'APPROVED' } });

    await waitFor(() => {
      expect(screen.queryByText('REC-20260722-0001')).not.toBeInTheDocument();
      expect(screen.getAllByText('REC-20260722-0002').length).toBeGreaterThan(0);
    });
  });
});
