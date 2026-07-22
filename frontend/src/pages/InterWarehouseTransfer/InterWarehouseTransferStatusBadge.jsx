import React from 'react';
import Badge from '../../components/common/Badge';
import { interWarehouseTransferStatusLabel, interWarehouseTransferStepLabel } from '../../utils/interWarehouseTransferStatus';

const statusMap = {
  NEW: 'bg-canvas-cream text-shade-70 border-hairline-light',
  APPROVED: 'bg-info-50 text-info-700 border-info-200',
  IN_TRANSIT: 'bg-warning-50 text-warning-800 border-warning-200',
  COMPLETED: 'bg-success-50 text-success-800 border-success-200',
  COMPLETED_WITH_DISCREPANCY: 'bg-orange-50 text-orange-800 border-orange-200',
  REJECTED: 'bg-danger-50 text-danger-700 border-danger-200',
  CANCELLED: 'bg-shade-30 text-shade-70 border-hairline-light',
  QUARANTINED: 'bg-danger-100 text-danger-900 border-danger-300',
  SOURCE_LOAD_PENDING: 'bg-warning-50 text-warning-800 border-warning-200',
  OUTBOUND_QC_PENDING: 'bg-info-50 text-info-700 border-info-200',
  SOURCE_LOAD_REWORK_REQUIRED: 'bg-danger-50 text-danger-700 border-danger-200',
};

const InterWarehouseTransferStatusBadge = ({ status, step }) => (
  <Badge size="sm" colorClassName={statusMap[step || status] || statusMap.NEW}>
    {step ? interWarehouseTransferStepLabel(step) : interWarehouseTransferStatusLabel(status)}
  </Badge>
);

export default InterWarehouseTransferStatusBadge;
