import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import QuarantineWorkspace from '../../src/pages/Inbound/QuarantineWorkspace';

vi.mock('../../src/services/inbound.service', () => ({
  inboundService: {
    getQuarantineItems: vi.fn(),
    getPendingDisposals: vi.fn(),
    handleRtv: vi.fn(),
    handleDisposal: vi.fn(),
    handleDisposalFromQuarantine: vi.fn(),
    approveDisposal: vi.fn(),
  },
}));

vi.mock('../../src/services/masterData.service', () => ({
  masterDataService: {
    getSuppliers: vi.fn(),
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

const mockState = {
  activeWarehouse: { id: 1, name: 'Kho Hải Phòng', code: 'WH-HP' },
  user: { id: 1, role: 'ADMIN' },
  hasRole: () => true,
};

const mockQuarantineItems = [
  {
    id: 101,
    product_sku: 'SKU-LOGI-MX3',
    product_name: 'Chuột Logitech MX Master 3S',
    qc_failed_qty: 2,
    unit: 'Cái',
    receipt_number: 'REC-001',
    origin_type: 'RECEIPT',
    supplier_id: 10,
    qc_failure_reason: 'Trầy xước nặng',
    total_value: 3000000,
  },
];

const renderComponent = () => render(
  <MemoryRouter initialEntries={['/inbound/quarantine']}>
    <Routes>
      <Route path="/inbound/quarantine" element={<QuarantineWorkspace />} />
    </Routes>
  </MemoryRouter>
);

describe('QuarantineWorkspace Component', () => {
  afterEach(() => cleanup());

  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.mockImplementation((selector) => (selector ? selector(mockState) : mockState));

    inboundService.getQuarantineItems.mockResolvedValue(mockQuarantineItems);
    inboundService.getPendingDisposals.mockResolvedValue([]);
    masterDataService.getSuppliers.mockResolvedValue([{ id: 10, company_name: 'NCC An Phát' }]);
  });

  it('renders quarantine workspace and items', async () => {
    renderComponent();

    expect(await screen.findByText('Quản lý hàng lỗi cách ly')).toBeInTheDocument();
    expect(await screen.findByText('Chuột Logitech MX Master 3S')).toBeInTheDocument();
    expect(screen.getByText('Trầy xước nặng')).toBeInTheDocument();
  });

  it('opens RTV modal when clicking Trả hàng NCC', async () => {
    renderComponent();

    await screen.findByText('Chuột Logitech MX Master 3S');
    const rtvBtn = screen.getByText('Trả hàng NCC');
    fireEvent.click(rtvBtn);

    expect(await screen.findByText('Xác nhận xuất trả hàng lỗi NCC')).toBeInTheDocument();
  });
});
