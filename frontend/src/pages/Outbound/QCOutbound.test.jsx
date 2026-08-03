import React from 'react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import QCOutbound from './QCOutbound';

vi.mock('../../services/outbound.service', () => ({
  outboundService: {
    getDeliveryOrderById: vi.fn(),
    confirmQCOutbound: vi.fn(),
    requestPickingPlanAdjustment: vi.fn(),
  },
}));

vi.mock('../../services/masterData.service', () => ({
  masterDataService: {
    getBinLocations: vi.fn(),
  },
}));

const addToast = vi.fn();
vi.mock('../../stores/ui.store', () => ({
  useUiStore: () => ({ addToast }),
}));

import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';

const order = {
  id: 100,
  do_number: 'DO-100',
  dealer_name: 'Đại lý A',
  warehouse_id: 20,
  items: [{
    id: 200,
    product_name: 'Nồi inox',
    sku: 'NOI-01',
    requested_qty: 10,
    allocations: [{
      allocation_id: 900,
      batch_id: 71,
      location_id: 800,
      zone_id: 30,
      planned_qty: 10,
      qc_completed: false,
    }],
  }],
};

const locations = [
  { id: 880, code: 'STG-01', is_staging: true, is_quarantine: false },
  { id: 990, code: 'QTN-01', is_staging: false, is_quarantine: true },
];

const renderPage = () => render(
  <MemoryRouter initialEntries={['/outbound/delivery-orders/100/qc']}>
    <Routes>
      <Route path="/outbound/delivery-orders/:id/qc" element={<QCOutbound />} />
      <Route path="/outbound/delivery-orders/:id" element={<div>Chi tiết đơn</div>} />
    </Routes>
  </MemoryRouter>,
);

describe('QCOutbound', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    outboundService.getDeliveryOrderById.mockResolvedValue(order);
    outboundService.confirmQCOutbound.mockResolvedValue({});
    outboundService.requestPickingPlanAdjustment.mockResolvedValue({});
    masterDataService.getBinLocations.mockResolvedValue(locations);
  });

  afterEach(() => cleanup());

  it('allows entering a failed quantity and derives the passed quantity', async () => {
    renderPage();

    const failedInput = await screen.findByLabelText('SL không đạt');
    fireEvent.change(failedInput, { target: { value: '2' } });

    expect(screen.getByLabelText('SL đạt kiểm định')).toHaveValue(8);
    fireEvent.change(screen.getByLabelText('Lý do không đạt kiểm định *'), {
      target: { value: 'Móp méo' },
    });
    fireEvent.click(screen.getByRole('button', { name: /Gửi kết quả/ }));

    await waitFor(() => expect(outboundService.confirmQCOutbound).toHaveBeenCalledWith('100', {
      items: [expect.objectContaining({
        picked_qty: 10,
        qc_fail_qty: '2',
        reason: 'Móp méo',
        staging_location_id: 880,
        quarantine_location_id: 990,
      })],
    }));
  });

  it('shows the Storekeeper reason when the order is returned for recount', async () => {
    outboundService.getDeliveryOrderById.mockResolvedValue({
      ...order,
      cancel_reason: 'So luong thuc te khong khop',
    });

    renderPage();

    expect(await screen.findByText('Storekeeper yêu cầu đếm/QC lại')).toBeInTheDocument();
    expect(screen.getByText('So luong thuc te khong khop')).toBeInTheDocument();
  });

  it('prefills the rejected QC quantities for recount without marking the allocation completed', async () => {
    outboundService.getDeliveryOrderById.mockResolvedValue({
      ...order,
      items: [{
        ...order.items[0],
        allocations: [{
          ...order.items[0].allocations[0],
          qc_pass_qty: 8,
          qc_fail_qty: 2,
          qc_fail_reason: 'Mop meo',
          staging_location_id: 880,
          quarantine_location_id: 990,
          qc_completed: false,
        }],
      }],
    });

    renderPage();

    expect(await screen.findByLabelText('SL không đạt')).toHaveValue(2);
    expect(screen.getByLabelText('SL đạt kiểm định')).toHaveValue(8);
    expect(screen.getByLabelText('Lý do không đạt kiểm định *')).toHaveValue('Mop meo');
  });

  it('lets Staff request replanning when replacement allocations exceed the requested quantity', async () => {
    outboundService.getDeliveryOrderById.mockResolvedValue({
      ...order,
      items: [{
        ...order.items[0],
        allocations: [
          order.items[0].allocations[0],
          { ...order.items[0].allocations[0], allocation_id: 901, planned_qty: 2, replacement: true },
        ],
      }],
    });

    renderPage();

    const requestButton = await screen.findByRole('button', { name: /Yêu cầu Storekeeper phân bổ lại/ });
    expect(screen.getByRole('button', { name: /Gửi kết quả/ })).toBeDisabled();
    fireEvent.click(requestButton);

    await waitFor(() => expect(outboundService.requestPickingPlanAdjustment).toHaveBeenCalledWith(
      '100',
      expect.stringContaining('12'),
    ));
  });
});
