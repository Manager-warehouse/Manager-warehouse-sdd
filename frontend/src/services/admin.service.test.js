import { describe, expect, it, vi } from 'vitest';

const mocks = vi.hoisted(() => ({
  get: vi.fn(),
}));

vi.mock('./api.client', () => ({
  default: {
    get: mocks.get,
  },
  useMock: false,
}));

vi.mock('../utils/constants', () => ({
  MOCK_USERS: [],
}));

describe('adminService', () => {
  it('sends only approved audit log query parameters', async () => {
    const { adminService } = await import('./admin.service');
    mocks.get.mockResolvedValue({ data: { data: [] } });

    await adminService.getAuditLogs({
      page: 2,
      pageSize: 30,
      from: '2026-07-22T08:00:00Z',
      to: '2026-07-22T09:00:00Z',
      warehouse_id: 1,
      actor: 'CEO',
      action: 'LOGIN',
      entityType: 'User',
    });

    expect(mocks.get).toHaveBeenCalledWith('/admin/audit-logs', {
      params: {
        page: 2,
        pageSize: 30,
        from: '2026-07-22T08:00:00Z',
        to: '2026-07-22T09:00:00Z',
        warehouse_id: 1,
      },
    });
  });
});
