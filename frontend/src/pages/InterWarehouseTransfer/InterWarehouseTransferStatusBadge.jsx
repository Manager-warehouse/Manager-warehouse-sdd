import React from 'react';
import Badge from '../../components/common/Badge';
import { interWarehouseTransferStatusLabel } from '../../utils/interWarehouseTransferStatus';

const statusMap = {
  NEW: 'bg-canvas-cream text-shade-70 border-hairline-light',
  APPROVED: 'bg-blue-50 text-blue-700 border-blue-200',
  IN_TRANSIT: 'bg-amber-50 text-amber-800 border-amber-200',
  COMPLETED: 'bg-emerald-50 text-emerald-800 border-emerald-200',
  COMPLETED_WITH_DISCREPANCY: 'bg-orange-50 text-orange-800 border-orange-200',
  REJECTED: 'bg-red-50 text-red-700 border-red-200',
  CANCELLED: 'bg-shade-30 text-shade-70 border-hairline-light',
  QUARANTINED: 'bg-red-100 text-red-900 border-red-300',
};

const InterWarehouseTransferStatusBadge = ({ status }) => (
  <Badge size="sm" colorClassName={statusMap[status] || statusMap.NEW}>
    {interWarehouseTransferStatusLabel(status)}
  </Badge>
);

export default InterWarehouseTransferStatusBadge;
