import React from 'react';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import PodEvidenceSection from './PodEvidenceSection';
import { outboundService } from '../../services/outbound.service';

vi.mock('../../services/outbound.service', () => ({
  outboundService: { getPodEvidenceImage: vi.fn() },
}));

describe('PodEvidenceSection', () => {
  beforeEach(() => {
    let sequence = 0;
    vi.stubGlobal('URL', {
      createObjectURL: vi.fn(() => `blob:pod-${sequence += 1}`),
      revokeObjectURL: vi.fn(),
    });
    outboundService.getPodEvidenceImage.mockResolvedValue(new Blob(['image'], { type: 'image/jpeg' }));
  });

  afterEach(() => {
    cleanup();
    vi.unstubAllGlobals();
    vi.clearAllMocks();
  });

  it('does not request POD before the order is completed', () => {
    const { container } = render(
      <PodEvidenceSection deliveryOrderId="70" status="IN_TRANSIT" />,
    );

    expect(container).toBeEmptyDOMElement();
    expect(outboundService.getPodEvidenceImage).not.toHaveBeenCalled();
  });

  it('loads both images and opens a read-only preview', async () => {
    render(<PodEvidenceSection deliveryOrderId="70" status="COMPLETED" />);

    await waitFor(() => expect(screen.getByAltText('Ảnh hàng hóa khi giao')).toBeInTheDocument());
    expect(outboundService.getPodEvidenceImage).toHaveBeenNthCalledWith(1, '70', 'GOODS');
    expect(outboundService.getPodEvidenceImage).toHaveBeenNthCalledWith(2, '70', 'SIGNED_DOCUMENT');

    fireEvent.click(screen.getByTitle('Xem ảnh hàng hóa khi giao'));
    expect(screen.getAllByAltText('Ảnh hàng hóa khi giao')).toHaveLength(2);
  });

  it('shows retry after an image request fails', async () => {
    outboundService.getPodEvidenceImage.mockRejectedValueOnce(new Error('Mất kết nối'));
    render(<PodEvidenceSection deliveryOrderId="70" status="CLOSED" />);

    expect(await screen.findByRole('alert')).toHaveTextContent('Mất kết nối');
    outboundService.getPodEvidenceImage.mockResolvedValue(new Blob(['image'], { type: 'image/jpeg' }));
    fireEvent.click(screen.getByRole('button', { name: /thử lại/i }));

    await waitFor(() => expect(screen.getByAltText('Ảnh chứng từ đã ký')).toBeInTheDocument());
  });
});
