// Input validation function mimicking the frontend validators
export const validateSystemConfig = (config) => {
  const errors = {};

  // defaultCreditLimit validation
  if (!Number.isInteger(config.defaultCreditLimit) || config.defaultCreditLimit <= 0) {
    errors.defaultCreditLimit = 'Hạn mức nợ mặc định phải là số nguyên dương';
  } else if (config.defaultCreditLimit > 10000000000) {
    errors.defaultCreditLimit = 'Hạn mức nợ tối đa là 10 tỷ VND';
  }

  // minWarningStock validation
  if (!Number.isInteger(config.minWarningStock) || config.minWarningStock <= 0) {
    errors.minWarningStock = 'Ngưỡng cảnh báo tồn kho tối thiểu phải là số nguyên dương';
  } else if (config.minWarningStock > 1000) {
    errors.minWarningStock = 'Ngưỡng cảnh báo tồn kho tối đa là 1,000';
  }

  // shiftDurationHours validation
  if (config.shiftDurationHours < 4 || config.shiftDurationHours > 24) {
    errors.shiftDurationHours = 'Thời gian ca làm việc phải từ 4 đến 24 giờ';
  }

  // monthlyClosingDay validation
  if (!Number.isInteger(config.monthlyClosingDay) || config.monthlyClosingDay < 1 || config.monthlyClosingDay > 28) {
    errors.monthlyClosingDay = 'Ngày khóa sổ kế toán phải nằm trong khoảng từ ngày 1 đến ngày 28';
  }

  // managerApprovalLimit validation
  if (!Number.isInteger(config.managerApprovalLimit) || config.managerApprovalLimit <= 0) {
    errors.managerApprovalLimit = 'Hạn mức phê duyệt tối đa của Trưởng kho phải là số nguyên dương';
  } else if (config.managerApprovalLimit > 5000000000) {
    errors.managerApprovalLimit = 'Hạn mức phê duyệt tối đa của Trưởng kho là 5 tỷ VND';
  }

  return {
    isValid: Object.keys(errors).length === 0,
    errors
  };
};

describe('System Configurations Validation Tests', () => {
  test('should pass with valid config values', () => {
    const validConfig = {
      defaultCreditLimit: 500000000,
      minWarningStock: 10,
      shiftDurationHours: 8,
      monthlyClosingDay: 5,
      managerApprovalLimit: 50000000
    };
    const result = validateSystemConfig(validConfig);
    expect(result.isValid).toBe(true);
    expect(Object.keys(result.errors).length).toBe(0);
  });

  test('should validate defaultCreditLimit limits', () => {
    const invalidConfig = {
      defaultCreditLimit: -100, // Negative
      minWarningStock: 10,
      shiftDurationHours: 8,
      monthlyClosingDay: 5,
      managerApprovalLimit: 50000000
    };
    let result = validateSystemConfig(invalidConfig);
    expect(result.isValid).toBe(false);
    expect(result.errors.defaultCreditLimit).toBeDefined();

    const tooHighConfig = {
      defaultCreditLimit: 12000000000, // > 10 billion
      minWarningStock: 10,
      shiftDurationHours: 8,
      monthlyClosingDay: 5,
      managerApprovalLimit: 50000000
    };
    result = validateSystemConfig(tooHighConfig);
    expect(result.isValid).toBe(false);
    expect(result.errors.defaultCreditLimit).toBeDefined();
  });

  test('should validate shiftDurationHours range (4 - 24)', () => {
    const tooLowConfig = {
      defaultCreditLimit: 500000000,
      minWarningStock: 10,
      shiftDurationHours: 3, // too low
      monthlyClosingDay: 5,
      managerApprovalLimit: 50000000
    };
    let result = validateSystemConfig(tooLowConfig);
    expect(result.isValid).toBe(false);
    expect(result.errors.shiftDurationHours).toBeDefined();

    const tooHighConfig = {
      defaultCreditLimit: 500000000,
      minWarningStock: 10,
      shiftDurationHours: 25, // too high
      monthlyClosingDay: 5,
      managerApprovalLimit: 50000000
    };
    result = validateSystemConfig(tooHighConfig);
    expect(result.isValid).toBe(false);
    expect(result.errors.shiftDurationHours).toBeDefined();
  });

  test('should validate monthlyClosingDay range (1 - 28)', () => {
    const tooLowConfig = {
      defaultCreditLimit: 500000000,
      minWarningStock: 10,
      shiftDurationHours: 8,
      monthlyClosingDay: 0, // too low
      managerApprovalLimit: 50000000
    };
    let result = validateSystemConfig(tooLowConfig);
    expect(result.isValid).toBe(false);
    expect(result.errors.monthlyClosingDay).toBeDefined();

    const tooHighConfig = {
      defaultCreditLimit: 500000000,
      minWarningStock: 10,
      shiftDurationHours: 8,
      monthlyClosingDay: 29, // too high (unstable months)
      managerApprovalLimit: 50000000
    };
    result = validateSystemConfig(tooHighConfig);
    expect(result.isValid).toBe(false);
    expect(result.errors.monthlyClosingDay).toBeDefined();
  });
});
