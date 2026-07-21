import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import ProductManagement from '../../src/pages/Admin/ProductManagement';

vi.mock('../../src/services/masterData.service', () => ({
  masterDataService: {
    getProducts: vi.fn(),
    createProduct: vi.fn(),
    updateProduct: vi.fn(),
  },
}));

vi.mock('../../src/stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('../../src/stores/ui.store', () => ({
  useUiStore: () => ({ addToast: vi.fn() }),
}));

import { masterDataService } from '../../src/services/masterData.service';
import { useAuthStore } from '../../src/stores/auth.store';

const mockProducts = [
  {
    id: 1,
    sku: 'SKU-POT-001',
    name: 'Nồi Inox 3 Đáy Sunhouse',
    category: 'Gia dụng bếp',
    unit: 'Cái',
    min_stock_level: 10,
    is_active: true,
  },
  {
    id: 2,
    sku: 'SKU-PAN-002',
    name: 'Chảo Chống Dính Tefal 26cm',
    category: 'Gia dụng bếp',
    unit: 'Cái',
    min_stock_level: 15,
    is_active: true,
  },
];

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/admin/products']}>
    <Routes>
      <Route path="/admin/products" element={<ProductManagement />} />
    </Routes>
  </MemoryRouter>
);

describe('ProductManagement Component', () => {
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

    masterDataService.getProducts.mockResolvedValue(mockProducts);
  });

  it('renders title and product list', async () => {
    renderComponent();

    expect(await screen.findByText('Danh mục SKU & Sản phẩm')).toBeInTheDocument();
    expect(screen.getAllByText('SKU-POT-001').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Nồi Inox 3 Đáy Sunhouse').length).toBeGreaterThan(0);
    expect(screen.getAllByText('Chảo Chống Dính Tefal 26cm').length).toBeGreaterThan(0);
  });

  it('opens add product modal on button click', async () => {
    renderComponent();

    const addBtn = await screen.findByRole('button', { name: /Thêm sản phẩm/i });
    fireEvent.click(addBtn);

    expect(await screen.findByText('Thêm sản phẩm mới')).toBeInTheDocument();
  });
});
