import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import PeriodClosing from './PeriodClosing';
import { financeService } from '../../services/finance.service';
import { useAuthStore } from '../../stores/auth.store';

vi.mock('../../services/finance.service', () => ({
  financeService: {
    getAccountingPeriods: vi.fn(),
    closeAccountingPeriod: vi.fn(),
  }
}));

vi.mock('../../stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

describe('PeriodClosing Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.mockReturnValue({
      hasRole: (role) => role === 'ACCOUNTANT_MANAGER' || role === 'ADMIN',
    });

    financeService.getAccountingPeriods.mockResolvedValue([
      {
        id: 1,
        period_name: '2026-07',
        start_date: '2026-07-01',
        end_date: '2026-07-31',
        status: 'OPEN'
      },
      {
        id: 2,
        period_name: '2026-06',
        start_date: '2026-06-01',
        end_date: '2026-06-30',
        status: 'CLOSED',
        closed_by_name: 'Ke Toan Truong',
        closed_at: '2026-07-01T08:00:00Z'
      }
    ]);
  });

  it('renders accounting periods list correctly', async () => {
    render(
      <BrowserRouter>
        <PeriodClosing />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Quản lý Kỳ Kế toán & Chốt sổ')).toBeInText();
    });

    expect(screen.getByText('2026-07')).toBeInText();
    expect(screen.getByText('2026-06')).toBeInText();
    expect(screen.getByText('Khóa kỳ kế toán')).toBeInText();
  });

  it('opens confirmation modal and closes period on confirm', async () => {
    financeService.closeAccountingPeriod.mockResolvedValue({ id: 1, status: 'CLOSED' });

    render(
      <BrowserRouter>
        <PeriodClosing />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Khóa kỳ kế toán')).toBeInText();
    });

    fireEvent.click(screen.getByText('Khóa kỳ kế toán'));

    expect(screen.getByText('Xác nhận Khóa Kỳ Kế Toán 2026-07?')).toBeInText();

    fireEvent.click(screen.getByText('Xác nhận Khóa sổ'));

    await waitFor(() => {
      expect(financeService.closeAccountingPeriod).toHaveBeenCalledWith(1);
    });
  });
});
