import React from 'react';
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import StocktakeList from './StocktakeList';

const mocks = vi.hoisted(() => ({
  role: 'WAREHOUSE_MANAGER',
  getStockTakes: vi.fn(),
  rejectStockTake: vi.fn(),
  rejectCeoStockTake: vi.fn(),
  showToast: vi.fn(),
}));

vi.mock('../../stores/auth.store', () => ({
  useAuthStore: () => ({
    user: { id: 10 },
    activeWarehouse: { id: 1, name: 'Kho Hải Phòng' },
    hasRole: (role) => role === mocks.role,
  }),
}));

vi.mock('../../stores/ui.store', () => ({
  useUiStore: () => ({ showToast: mocks.showToast }),
}));

vi.mock('../../services/stocktake.service', () => ({
  stocktakeService: {
    getStockTakes: mocks.getStockTakes,
    startStockTake: vi.fn(),
    cancelStockTake: vi.fn(),
    approveStockTake: vi.fn(),
    approveCeoStockTake: vi.fn(),
    rejectStockTake: mocks.rejectStockTake,
    rejectCeoStockTake: mocks.rejectCeoStockTake,
  },
}));

vi.mock('../../components/common/Pagination', () => ({
  default: () => null,
}));

const stocktake = (approvalLevel) => ({
  id: 1,
  stock_take_number: 'ST-20260727-000001',
  stock_take_date: '2026-07-27',
  conducted_by_name: 'storekeeperHP',
  status: 'PENDING_APPROVAL',
  approval_level: approvalLevel,
  total_variance_value: -100000,
});

const renderList = async (approvalLevel) => {
  mocks.getStockTakes.mockResolvedValue({
    content: [stocktake(approvalLevel)],
    totalPages: 1,
    totalElements: 1,
  });

  render(
    <MemoryRouter>
      <StocktakeList />
    </MemoryRouter>
  );

  await screen.findAllByText('ST-20260727-000001');
};

const submitReturn = async () => {
  fireEvent.click(screen.getAllByTitle('Trả lại kiểm tra')[0]);
  fireEvent.change(screen.getByPlaceholderText('Nhập lý do cần kiểm tra lại...'), {
    target: { value: '  Kiểm đếm lại số lượng  ' },
  });
  await act(async () => {
    fireEvent.click(screen.getByRole('button', { name: 'Xác nhận trả lại' }));
  });
};

describe('StocktakeList return for recount actions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mocks.role = 'WAREHOUSE_MANAGER';
  });

  afterEach(() => cleanup());

  it('lets a warehouse manager return a manager-level stocktake', async () => {
    await renderList('MANAGER');
    await submitReturn();

    await waitFor(() => expect(mocks.rejectStockTake).toHaveBeenCalledWith(
      1,
      'Kiểm đếm lại số lượng'
    ));
    expect(mocks.rejectCeoStockTake).not.toHaveBeenCalled();
  });

  it('does not show review actions to a manager for a CEO-level stocktake', async () => {
    await renderList('CEO');

    expect(screen.queryAllByTitle('Trả lại kiểm tra')).toHaveLength(0);
    expect(screen.queryAllByTitle('Phê duyệt')).toHaveLength(0);
  });

  it('lets the CEO return a CEO-level stocktake', async () => {
    mocks.role = 'CEO';
    await renderList('CEO');
    await submitReturn();

    await waitFor(() => expect(mocks.rejectCeoStockTake).toHaveBeenCalledWith(
      1,
      'Kiểm đếm lại số lượng'
    ));
    expect(mocks.rejectStockTake).not.toHaveBeenCalled();
  });
});
