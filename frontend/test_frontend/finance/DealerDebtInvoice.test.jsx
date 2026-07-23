import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import DealerDebtInvoice from '../../src/pages/Finance/DealerDebtInvoice';

vi.mock('../../src/services/finance.service', () => ({
  financeService: {
    getInvoices: vi.fn(),
    createInvoice: vi.fn(),
    getAgingReport: vi.fn(),
    getAccountingPeriods: vi.fn(),
    getInvoiceById: vi.fn(),
    getBillingNotifications: vi.fn(),
  },
}));

vi.mock('../../src/services/masterData.service', () => ({
  masterDataService: {
    getDealers: vi.fn(),
    getProducts: vi.fn(),
  },
}));

vi.mock('../../src/services/pricing.service', () => ({
  pricingService: {
    getDealerPrices: vi.fn(),
  },
}));

vi.mock('../../src/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('../../src/stores/ui.store', () => ({
  useUiStore: () => ({ addToast: vi.fn() }),
}));

import { financeService } from '../../src/services/finance.service';
import { masterDataService } from '../../src/services/masterData.service';
import { useAuthStore } from '../../src/stores/auth.store';

const mockInvoices = [
  {
    id: 1,
    invoice_number: 'INV-20260722-0001',
    dealer_id: 10,
    dealer_name: 'Đại lý Minh Hùng',
    issue_date: '2026-07-22',
    due_date: '2026-08-22',
    total_amount: 15000000,
    paid_amount: 5000000,
    remaining_amount: 10000000,
    status: 'PARTIAL',
  },
];

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/finance/dealer-debts']}>
    <Routes>
      <Route path="/finance/dealer-debts" element={<DealerDebtInvoice />} />
    </Routes>
  </MemoryRouter>
);

describe('DealerDebtInvoice Component', () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.mockImplementation((selector) => {
      const state = {
        activeWarehouse: { id: 1, name: 'Kho Hải Phòng', code: 'WH-HP' },
        user: { id: 1, role: 'ACCOUNTANT' },
        hasRole: () => true,
      };
      return selector ? selector(state) : state;
    });

    financeService.getInvoices.mockResolvedValue(mockInvoices);
    financeService.getBillingNotifications.mockResolvedValue([]);
    financeService.getAgingReport.mockResolvedValue([]);
    financeService.getAccountingPeriods.mockResolvedValue([{ id: 1, name: 'Tháng 07/2026', is_closed: false }]);
    masterDataService.getDealers.mockResolvedValue([{ id: 10, name: 'Đại lý Minh Hùng' }]);
    masterDataService.getProducts.mockResolvedValue([]);
  });

  it('renders title and invoices section', async () => {
    renderComponent();

    expect(await screen.findByText('Hóa đơn Bán hàng & Công nợ Đại lý')).toBeInTheDocument();
    const invoiceTab = screen.getByRole('button', { name: /Danh sách hóa đơn/i });
    fireEvent.click(invoiceTab);

    const invoiceMatches = await screen.findAllByText('INV-20260722-0001');
    expect(invoiceMatches.length).toBeGreaterThan(0);
    expect(screen.getAllByText('Đại lý Minh Hùng').length).toBeGreaterThan(0);
  });

  it('switches to aging report tab', async () => {
    renderComponent();

    await screen.findByText('Hóa đơn Bán hàng & Công nợ Đại lý');
    const tabBtn = screen.getByRole('button', { name: /Danh sách hóa đơn/i });
    fireEvent.click(tabBtn);

    await waitFor(() => {
      expect(financeService.getInvoices).toHaveBeenCalled();
    });
  });
});
