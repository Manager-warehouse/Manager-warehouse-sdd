/**
 * System Configuration Validation Tests
 *
 * Mirrors the validate() function in SystemConfig.jsx.
 * All rules come from data-model.md (feature-admin-system-config.md).
 *
 * Fields & constraints:
 *   defaultCreditLimit         > 0  (positive integer)
 *   defaultPaymentTermDays     > 0  (positive integer)
 *   creditHoldOverdueDays      > 0  (positive integer)
 *   creditUnlockBufferPct      in (0, 1]  (decimal)
 *   monthlyClosingDay          in [1, 31] (integer)
 *   minInventoryWarningThreshold >= 0 (integer)
 */

// ─── Validator (mirrors SystemConfig.jsx validate()) ─────────────────────────

const validateSystemConfig = (form) => {
  const errs = {};

  const creditLimit = Number(form.defaultCreditLimit);
  if (!Number.isFinite(creditLimit) || !Number.isInteger(creditLimit) || creditLimit <= 0) {
    errs.defaultCreditLimit = 'Hạn mức nợ phải là số nguyên dương (> 0)';
  }

  const paymentTerm = Number(form.defaultPaymentTermDays);
  if (!Number.isFinite(paymentTerm) || !Number.isInteger(paymentTerm) || paymentTerm <= 0) {
    errs.defaultPaymentTermDays = 'Thời hạn thanh toán phải là số nguyên dương (> 0)';
  }

  const overdueDays = Number(form.creditHoldOverdueDays);
  if (!Number.isFinite(overdueDays) || !Number.isInteger(overdueDays) || overdueDays <= 0) {
    errs.creditHoldOverdueDays = 'Số ngày trễ hạn phải là số nguyên dương (> 0)';
  }

  const bufferPct = Number(form.creditUnlockBufferPct);
  if (!Number.isFinite(bufferPct) || bufferPct <= 0 || bufferPct > 1) {
    errs.creditUnlockBufferPct = 'Hệ số đệm mở khóa phải là số thập phân trong khoảng (0, 1]';
  }

  const closingDay = Number(form.monthlyClosingDay);
  if (!Number.isFinite(closingDay) || !Number.isInteger(closingDay) || closingDay < 1 || closingDay > 31) {
    errs.monthlyClosingDay = 'Ngày khóa sổ phải là số nguyên từ 1 đến 31';
  }

  const threshold = Number(form.minInventoryWarningThreshold);
  if (!Number.isFinite(threshold) || !Number.isInteger(threshold) || threshold < 0) {
    errs.minInventoryWarningThreshold = 'Ngưỡng cảnh báo tồn kho phải là số nguyên >= 0';
  }

  return { isValid: Object.keys(errs).length === 0, errors: errs };
};

// ─── Fixture ─────────────────────────────────────────────────────────────────

const VALID_CONFIG = {
  defaultCreditLimit: 500000000,
  defaultPaymentTermDays: 30,
  creditHoldOverdueDays: 30,
  creditUnlockBufferPct: 0.8,
  monthlyClosingDay: 5,
  minInventoryWarningThreshold: 10
};

// ─── Tests ───────────────────────────────────────────────────────────────────

describe('SystemConfig — validate()', () => {

  test('passes with all valid defaults', () => {
    const { isValid, errors } = validateSystemConfig(VALID_CONFIG);
    expect(isValid).toBe(true);
    expect(Object.keys(errors)).toHaveLength(0);
  });

  // ── defaultCreditLimit ─────────────────────────────────────────────────────

  describe('defaultCreditLimit', () => {
    test('fails when zero', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, defaultCreditLimit: 0 });
      expect(errors.defaultCreditLimit).toBeDefined();
    });

    test('fails when negative', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, defaultCreditLimit: -1000 });
      expect(errors.defaultCreditLimit).toBeDefined();
    });

    test('fails when decimal', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, defaultCreditLimit: 500000.5 });
      expect(errors.defaultCreditLimit).toBeDefined();
    });

    test('passes at boundary: 1', () => {
      const { isValid } = validateSystemConfig({ ...VALID_CONFIG, defaultCreditLimit: 1 });
      expect(isValid).toBe(true);
    });
  });

  // ── defaultPaymentTermDays ─────────────────────────────────────────────────

  describe('defaultPaymentTermDays', () => {
    test('fails when zero', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, defaultPaymentTermDays: 0 });
      expect(errors.defaultPaymentTermDays).toBeDefined();
    });

    test('fails when negative', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, defaultPaymentTermDays: -5 });
      expect(errors.defaultPaymentTermDays).toBeDefined();
    });

    test('passes at boundary: 1', () => {
      const { isValid } = validateSystemConfig({ ...VALID_CONFIG, defaultPaymentTermDays: 1 });
      expect(isValid).toBe(true);
    });
  });

  // ── creditHoldOverdueDays ──────────────────────────────────────────────────

  describe('creditHoldOverdueDays', () => {
    test('fails when zero', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, creditHoldOverdueDays: 0 });
      expect(errors.creditHoldOverdueDays).toBeDefined();
    });

    test('fails when negative', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, creditHoldOverdueDays: -1 });
      expect(errors.creditHoldOverdueDays).toBeDefined();
    });

    test('passes at boundary: 1', () => {
      const { isValid } = validateSystemConfig({ ...VALID_CONFIG, creditHoldOverdueDays: 1 });
      expect(isValid).toBe(true);
    });
  });

  // ── creditUnlockBufferPct ──────────────────────────────────────────────────

  describe('creditUnlockBufferPct', () => {
    test('fails when 0 (exclusive lower bound)', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, creditUnlockBufferPct: 0 });
      expect(errors.creditUnlockBufferPct).toBeDefined();
    });

    test('fails when greater than 1', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, creditUnlockBufferPct: 1.01 });
      expect(errors.creditUnlockBufferPct).toBeDefined();
    });

    test('fails when negative', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, creditUnlockBufferPct: -0.5 });
      expect(errors.creditUnlockBufferPct).toBeDefined();
    });

    test('passes at upper boundary: 1.0 (inclusive)', () => {
      const { isValid } = validateSystemConfig({ ...VALID_CONFIG, creditUnlockBufferPct: 1 });
      expect(isValid).toBe(true);
    });

    test('passes at mid-range: 0.5', () => {
      const { isValid } = validateSystemConfig({ ...VALID_CONFIG, creditUnlockBufferPct: 0.5 });
      expect(isValid).toBe(true);
    });
  });

  // ── monthlyClosingDay ──────────────────────────────────────────────────────

  describe('monthlyClosingDay', () => {
    test('fails when 0 (below lower bound)', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, monthlyClosingDay: 0 });
      expect(errors.monthlyClosingDay).toBeDefined();
    });

    test('fails when 32 (above upper bound)', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, monthlyClosingDay: 32 });
      expect(errors.monthlyClosingDay).toBeDefined();
    });

    test('passes at lower boundary: 1', () => {
      const { isValid } = validateSystemConfig({ ...VALID_CONFIG, monthlyClosingDay: 1 });
      expect(isValid).toBe(true);
    });

    test('passes at upper boundary: 31', () => {
      const { isValid } = validateSystemConfig({ ...VALID_CONFIG, monthlyClosingDay: 31 });
      expect(isValid).toBe(true);
    });
  });

  // ── minInventoryWarningThreshold ───────────────────────────────────────────

  describe('minInventoryWarningThreshold', () => {
    test('fails when negative', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, minInventoryWarningThreshold: -1 });
      expect(errors.minInventoryWarningThreshold).toBeDefined();
    });

    test('fails when decimal', () => {
      const { errors } = validateSystemConfig({ ...VALID_CONFIG, minInventoryWarningThreshold: 10.5 });
      expect(errors.minInventoryWarningThreshold).toBeDefined();
    });

    test('passes at lower boundary: 0 (no warning mode)', () => {
      const { isValid } = validateSystemConfig({ ...VALID_CONFIG, minInventoryWarningThreshold: 0 });
      expect(isValid).toBe(true);
    });

    test('passes with large threshold', () => {
      const { isValid } = validateSystemConfig({ ...VALID_CONFIG, minInventoryWarningThreshold: 9999 });
      expect(isValid).toBe(true);
    });
  });

  // ── Multiple errors ────────────────────────────────────────────────────────

  test('returns all errors simultaneously when multiple fields are invalid', () => {
    const allInvalid = {
      defaultCreditLimit: -1,
      defaultPaymentTermDays: 0,
      creditHoldOverdueDays: -10,
      creditUnlockBufferPct: 0,
      monthlyClosingDay: 0,
      minInventoryWarningThreshold: -5
    };
    const { isValid, errors } = validateSystemConfig(allInvalid);
    expect(isValid).toBe(false);
    expect(Object.keys(errors)).toHaveLength(6);
  });
});
