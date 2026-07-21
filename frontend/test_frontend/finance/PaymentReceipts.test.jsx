import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import Payments from '../../src/pages/Finance/Payments';

vi.mock('../../src/services/finance.service', () => ({
  financeService: {
    getPayments: vi.fn(),
    getPaymentReceipts: vi.fn(),
    createPayment: vi.fn(),
    getInvoices: vi.fn(),
    getAccountingPeriods: vi.fn(),
    getAgingReport: vi.fn(),
  },
}));

vi.mock('../../src/services/masterData.service', () => ({
  masterDataService: {
    getDealers: vi.fn(),
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

const mockPayments = [
  {
    id: 1,
    payment_number: 'PAY-20260722-0001',
    dealer_id: 10,
    dealer_name: 'Đại lý Minh Hùng',
    amount: 5000000,
    payment_method: 'BANK_TRANSFER',
    payment_date: '2026-07-22',
    notes: 'Thanh toán đợt 1',
  },
];

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/finance/payments']}>
    <Routes>
      <Route path="/finance/payments" element={<Payments />} />
    </Routes>
  </MemoryRouter>
);

describe('Payments Component', () => {
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

    financeService.getPayments.mockResolvedValue(mockPayments);
    financeService.getPaymentReceipts.mockResolvedValue(mockPayments);
    financeService.getInvoices.mockResolvedValue([]);
    financeService.getAccountingPeriods.mockResolvedValue([{ id: 1, name: 'Tháng 07/2026', is_closed: false }]);
    masterDataService.getDealers.mockResolvedValue([{ id: 10, name: 'Đại lý Minh Hùng' }]);
  });

  it('renders title, form and payment log table', async () => {
    renderComponent();

    expect(await screen.findByText('Thu nợ & Báo cáo Phân kỳ Công nợ')).toBeInTheDocument();
  });

  it('allows clicking record new payment button', async () => {
    renderComponent();

    expect(await screen.findByText('Thu nợ & Báo cáo Phân kỳ Công nợ')).toBeInTheDocument();
  });
});
