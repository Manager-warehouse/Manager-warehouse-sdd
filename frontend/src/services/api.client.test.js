import { describe, expect, it } from 'vitest';
import { isPublicAuthRequest } from './api.client';

describe('isPublicAuthRequest', () => {
  it('treats OTP checking as a public authentication request', () => {
    expect(isPublicAuthRequest('/auth/otp/check')).toBe(true);
  });

  it('does not treat protected APIs as public authentication requests', () => {
    expect(isPublicAuthRequest('/stock-takes')).toBe(false);
  });
});
