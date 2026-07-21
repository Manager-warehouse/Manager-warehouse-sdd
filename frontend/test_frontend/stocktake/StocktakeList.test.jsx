import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import StocktakeList from '../../src/pages/Stocktake/StocktakeList';

vi.mock('../../src/services/stocktake.service', () => ({
  stocktakeService: {
    getStockTakes: vi.fn(),
    createStocktake: vi.fn(),
  },
}));

vi.mock('../../src/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('../../src/stores/ui.store', () => ({
  useUiStore: () => ({ addToast: vi.fn(), showToast: vi.fn() }),
}));

import { stocktakeService } from '../../src/services/stocktake.service';
import { useAuthStore } from '../../src/stores/auth.store';

const mockStocktakes = [
  {
    id: 1,
    stock_take_number: 'ST-20260722-0001',
    status: 'COMPLETED',
    warehouse_id: 1,
    conducted_by_name: 'Nguyễn Văn Đếm',
    stock_take_date: '2026-07-22',
    total_variance_value: 0,
  },
  {
    id: 2,
    stock_take_number: 'ST-20260722-0002',
    status: 'IN_PROGRESS',
    warehouse_id: 1,
    conducted_by_name: 'Trần Văn Kiểm',
    stock_take_date: '2026-07-22',
    total_variance_value: 0,
  },
];

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/stocktake/list']}>
    <Routes>
      <Route path="/stocktake/list" element={<StocktakeList />} />
    </Routes>
  </MemoryRouter>
);

describe('StocktakeList Component', () => {
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

    stocktakeService.getStockTakes.mockResolvedValue({
      content: mockStocktakes,
      totalPages: 1,
      totalElements: 2,
    });
  });

  it('renders stocktake list and headers', async () => {
    renderComponent();

    expect(await screen.findByText('Kiểm kê hàng hóa')).toBeInTheDocument();
    const st1 = await screen.findAllByText('ST-20260722-0001');
    expect(st1.length).toBeGreaterThan(0);
    const st2 = await screen.findAllByText('ST-20260722-0002');
    expect(st2.length).toBeGreaterThan(0);
  });

  it('filters by status tabs', async () => {
    renderComponent();

    await screen.findByText('Kiểm kê hàng hóa');
    const inProgressTab = screen.getByRole('button', { name: /Đang kiểm/i });
    fireEvent.click(inProgressTab);

    expect(stocktakeService.getStockTakes).toHaveBeenCalled();
  });
});
