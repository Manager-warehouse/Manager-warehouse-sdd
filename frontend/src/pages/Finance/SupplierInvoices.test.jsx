import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import SupplierInvoices from './SupplierInvoices';
import { financeService } from '../../services/finance.service';
import { masterDataService } from '../../services/masterData.service';
import { useAuthStore } from '../../stores/auth.store';

vi.mock('../../services/finance.service', () => ({
  financeService: {
    getSupplierInvoices: vi.fn(),
    getSupplierBillingNotifications: vi.fn(),
    getSupplierPayments: vi.fn(),
    createSupplierInvoice: vi.fn(),
    createSupplierPayment: vi.fn(),
    scanSupplierPaymentOcr: vi.fn(),
  }
}));

vi.mock('../../services/masterData.service', () => ({
  masterDataService: {
    getSuppliers: vi.fn(),
  }
}));

vi.mock('../../stores/auth.store', () => ({
  useAuthStore: vi.fn(),
}));

describe('SupplierInvoices Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.mockReturnValue({
      hasRole: (role) => role === 'ACCOUNTANT' || role === 'ADMIN',
    });

    masterDataService.getSuppliers.mockResolvedValue([
      { id: 10, companyName: 'Nha Cung Cap Sunhouse' }
    ]);
    financeService.getSupplierInvoices.mockResolvedValue([
      {
        id: 100,
        invoice_number: 'SINV-202607-000001',
        supplier_invoice_number: 'VAT-NCC-001',
        supplier_id: 10,
        supplier_name: 'Nha Cung Cap Sunhouse',
        total_amount: 50000000,
        status: 'UNPAID',
        document_date: '2026-07-23',
        due_date: '2026-08-23'
      }
    ]);
    financeService.getSupplierBillingNotifications.mockResolvedValue([
      {
        id: 1,
        receipt_id: 200,
        receipt_number: 'RO-2026-001',
        supplier_name: 'Nha Cung Cap Sunhouse',
        invoiceStatus: 'NOT_INVOICED',
        totalAmountEstimate: 50000000
      }
    ]);
    financeService.getSupplierPayments.mockResolvedValue([
      {
        id: 300,
        payment_number: 'SPAY-202607-000001',
        supplier_name: 'Nha Cung Cap Sunhouse',
        amount: 20000000,
        payment_date: '2026-07-23',
        payment_method: 'BANK_TRANSFER'
      }
    ]);
  });

  it('renders SupplierInvoices page and displays notification worklist', async () => {
    render(
      <BrowserRouter>
        <SupplierInvoices />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Hóa đơn Mua hàng & Công nợ Nhà cung cấp')).toBeInText();
    });

    expect(screen.getByText('RO-2026-001')).toBeInText();
    expect(screen.getByText('Nha Cung Cap Sunhouse')).toBeInText();
  });

  it('switches to Hóa đơn Mua hàng tab', async () => {
    render(
      <BrowserRouter>
        <SupplierInvoices />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Hóa đơn Mua hàng (SINV)')).toBeInText();
    });

    fireEvent.click(screen.getByText('Hóa đơn Mua hàng (SINV)'));

    await waitFor(() => {
      expect(screen.getByText('SINV-202607-000001')).toBeInText();
      expect(screen.getByText('VAT-NCC-001')).toBeInText();
    });
  });

  it('opens modal to create supplier invoice when clicking Lập Hóa đơn Mua', async () => {
    render(
      <BrowserRouter>
        <SupplierInvoices />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Lập Hóa đơn Mua')).toBeInText();
    });

    fireEvent.click(screen.getByText('Lập Hóa đơn Mua'));

    expect(screen.getByText('Lập Hóa đơn Mua hàng từ Phiếu nhập kho')).toBeInText();
    expect(screen.getByText('Lưu Hóa đơn & Ghi nợ')).toBeInText();
  });
});
