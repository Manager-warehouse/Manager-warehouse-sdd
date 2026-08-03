export const updateReturnedGoodsRow = (row, field, value) => {
  if (field !== 'actual_qty' && field !== 'quality_pass_qty') {
    return { ...row, [field]: value };
  }

  const actualQty = Number(field === 'actual_qty' ? value : row.actual_qty || 0);
  const passQty = Number(field === 'quality_pass_qty' ? value : row.quality_pass_qty || 0);
  const failQty = Math.max(actualQty - passQty, 0);
  const shortageQty = Math.max(Number(row.expected_qty || 0) - actualQty, 0);

  return {
    ...row,
    [field]: value,
    quality_fail_qty: failQty,
    failed_planned_qty: failQty,
    planned_qty: passQty,
    shortage_qty: shortageQty,
    shortage_reason: shortageQty > 0 ? row.shortage_reason : '',
  };
};
