import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import CreditAgingReport from './CreditAgingReport';
import { financeService } from '../../services/finance.service';

vi.mock('../../services/finance.service', () => ({
  financeService: {
    getAgingReport: vi.fn(),
  }
}));

describe('CreditAgingReport Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    financeService.getAgingReport.mockResolvedValue([
      {
        dealer_id: 1,
        dealer_code: 'D001',
        dealer_name: 'Dai Ly Ha Noi',
        credit_limit: 500000000,
        current_balance: 150000000,
        in_term_amount: 100000000,
        overdue_1_to_30: 50000000,
        overdue_31_to_60: 0,
        overdue_61_to_90: 0,
        overdue_over_90: 0,
        risk_level: 'LOW_RISK',
        credit_status: 'ACTIVE'
      },
      {
        dealer_id: 2,
        dealer_code: 'D002',
        dealer_name: 'Dai Ly Hai Phong',
        credit_limit: 300000000,
        current_balance: 350000000,
        in_term_amount: 50000000,
        overdue_1_to_30: 100000000,
        overdue_31_to_60: 100000000,
        overdue_61_to_90: 50000000,
        overdue_over_90: 50000000,
        risk_level: 'HIGH_RISK',
        credit_status: 'CREDIT_HOLD'
      }
    ]);
  });

  it('renders credit aging report page with calculated totals and table', async () => {
    render(
      <BrowserRouter>
        <CreditAgingReport />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Báo cáo Phân kỳ Công nợ Đại lý (Aging Report)')).toBeInText();
    });

    expect(screen.getByText('Dai Ly Ha Noi')).toBeInText();
    expect(screen.getByText('Dai Ly Hai Phong')).toBeInText();
    expect(screen.getByText('1 Đại lý')).toBeInText(); // 1 High risk count
  });
});
