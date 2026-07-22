import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import Header from './Header';

const mocks = vi.hoisted(() => ({
  getWarehouses: vi.fn(),
  setActiveWarehouse: vi.fn(),
}));

vi.mock('../../services/masterData.service', () => ({
  masterDataService: {
    getWarehouses: mocks.getWarehouses,
  },
}));

vi.mock('../../stores/ui.store', () => ({
  useUiStore: () => ({
    toggleSidebar: vi.fn(),
    addToast: vi.fn(),
  }),
}));

vi.mock('../../stores/auth.store', () => ({
  useAuthStore: () => ({
    user: {
      id: 9,
      fullName: 'Quan ly kho HN',
      email: 'manager.hn@wms.test',
      role: 'WAREHOUSE_MANAGER',
      warehouses: [2],
      assignedWarehouses: [{ id: 2, name: 'Kho Hà Nội', code: 'WH-HN' }],
    },
    activeWarehouse: { id: 2, name: 'Kho Hà Nội', code: 'WH-HN' },
    setActiveWarehouse: mocks.setActiveWarehouse,
    logout: vi.fn(),
  }),
}));

describe('Header warehouse loading', () => {
  it('does not call admin warehouse API for warehouse manager role', async () => {
    render(
      <MemoryRouter initialEntries={['/transfers']}>
        <Header />
      </MemoryRouter>
    );

    expect(screen.getByText('WH-HN')).toBeInTheDocument();
    await waitFor(() => expect(mocks.getWarehouses).not.toHaveBeenCalled());
  });
});
