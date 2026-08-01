import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  AlertTriangle,
  ArrowLeft,
  CheckCircle2,
  Clock,
  FileText,
  Loader2,
  MapPin,
  PackageSearch,
  RotateCcw,
  X,
  Info,
} from 'lucide-react';
import { outboundService } from '../../services/outbound.service';
import { masterDataService } from '../../services/masterData.service';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import PickingListTable from '../../components/warehouse/PickingListTable';
import DeliveryOrderPickingPlanEditor from '../../components/warehouse/DeliveryOrderPickingPlanEditor';
import Button from '../../components/common/Button';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import { ROLES } from '../../utils/constants';

const DO_STATUS_MAP = {
  NEW: { label: 'Mới', color: 'bg-canvas-cream text-shade-70 border-hairline-light' },
  WAITING_PICKING: { label: 'Chờ lấy hàng & kiểm định', color: 'bg-info-50 text-info-700 border-info-200' },
  QC_PENDING_APPROVAL: { label: 'Chờ duyệt kiểm định', color: 'bg-violet-50 text-violet-700 border-violet-200' },
  QC_COMPLETED: { label: 'Hoàn tất kiểm định', color: 'bg-success-50 text-success-700 border-success-200' },
  WAREHOUSE_APPROVED: { label: 'Chờ vận chuyển', color: 'bg-warning-50 text-warning-700 border-warning-200' },
  IN_TRANSIT: { label: 'Đang giao', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
  COMPLETED: { label: 'Đã giao', color: 'bg-success-50 text-success-900 border-success-300' },
  RETURNED: { label: 'Chờ hoàn về kho', color: 'bg-orange-50 text-orange-700 border-orange-200' },
  DELIVERY_FAILED: { label: 'Giao hàng thất bại', color: 'bg-danger-50 text-danger-700 border-danger-200' },
  REJECTED: { label: 'Bị từ chối', color: 'bg-rose-50 text-rose-700 border-rose-200' },
  CANCELLED: { label: 'Đã hủy', color: 'bg-danger-50 text-danger-700 border-danger-200' },
};

const getStatusBadge = (status) => {
  const { label, color } = DO_STATUS_MAP[status] ?? { label: status, color: 'bg-canvas-cream text-shade-70 border-hairline-light' };
  return <Badge size="sm" colorClassName={color}>{label}</Badge>;
};

const canStaffSubmitReturnedQc = (status) => ['COUNT_QC_PENDING', 'QC_REJECTED'].includes(status);

const buildReturnedRows = (items = []) => {
  const rowsByKey = new Map();

  items.forEach((item) => {
    const allocations = item.allocations?.length
      ? item.allocations
      : [{ batch_id: item.batch_id, planned_qty: item.qc_pass_qty || item.issued_qty || item.requested_qty }];

    allocations
      .filter((allocation) => allocation.batch_id && Number(allocation.qc_pass_qty ?? allocation.picked_qty ?? allocation.planned_qty ?? 0) > 0)
      .forEach((allocation) => {
        const qty = Number(allocation.qc_pass_qty ?? allocation.picked_qty ?? allocation.planned_qty ?? item.qc_pass_qty ?? item.issued_qty ?? item.requested_qty ?? 0);
        const key = `${item.id}-${item.product_id}-${allocation.batch_id}`;
        const existing = rowsByKey.get(key);

        if (existing) {
          const expectedQty = Number(existing.expected_qty || 0) + qty;
          rowsByKey.set(key, {
            ...existing,
            expected_qty: expectedQty,
            actual_qty: expectedQty,
            quality_pass_qty: expectedQty,
            planned_qty: expectedQty,
          });
          return;
        }

        rowsByKey.set(key, {
          key,
          do_item_id: item.id,
          product_id: item.product_id,
          product_name: item.product_name,
          sku: item.sku,
          batch_id: allocation.batch_id,
          expected_qty: qty,
          actual_qty: qty,
          quality_pass_qty: qty,
        quality_fail_qty: 0,
        quality_failure_reason: '',
        destination_location_id: '',
        failed_destination_location_id: '',
        planned_qty: qty,
        failed_planned_qty: 0,
      });
      });
  });

  return Array.from(rowsByKey.values());
};

const mergeReturnedFlowRows = (rows, flow) => {
  if (!flow?.items?.length) return rows;
  return rows.map((row) => {
    const matched = flow.items.find((item) => (
      Number(item.do_item_id) === Number(row.do_item_id)
      && Number(item.batch_id) === Number(row.batch_id)
    ));
    return matched
      ? {
          ...row,
          expected_qty: matched.expected_qty || row.expected_qty,
          actual_qty: matched.actual_qty || row.actual_qty,
          quality_pass_qty: matched.quality_pass_qty ?? row.quality_pass_qty,
          quality_fail_qty: matched.quality_fail_qty ?? row.quality_fail_qty,
          quality_failure_reason: matched.quality_failure_reason || row.quality_failure_reason,
          destination_location_id: matched.destination_location_id || row.destination_location_id,
          failed_destination_location_id: matched.failed_destination_location_id || row.failed_destination_location_id,
          planned_qty: Number(matched.planned_qty || 0) > 0
            ? matched.planned_qty
            : matched.quality_pass_qty ?? row.planned_qty,
          failed_planned_qty: Number(matched.failed_planned_qty || 0) > 0
            ? matched.failed_planned_qty
            : matched.quality_fail_qty ?? row.failed_planned_qty,
        }
      : row;
  });
};

const mergeDuplicateReturnedRows = (rows = []) => Array.from(rows.reduce((acc, row) => {
  const key = `${row.do_item_id}-${row.product_id}-${row.batch_id}`;
  const existing = acc.get(key);
  if (!existing) {
    acc.set(key, row);
    return acc;
  }

  const actualQty = Number(existing.actual_qty || 0) + Number(row.actual_qty || 0);
  const passQty = Number(existing.quality_pass_qty || 0) + Number(row.quality_pass_qty || 0);
  const failQty = Number(existing.quality_fail_qty || 0) + Number(row.quality_fail_qty || 0);
  acc.set(key, {
    ...existing,
    expected_qty: Number(existing.expected_qty || 0) + Number(row.expected_qty || 0),
    actual_qty: actualQty,
    quality_pass_qty: passQty,
    quality_fail_qty: failQty,
    quality_failure_reason: existing.quality_failure_reason || row.quality_failure_reason || '',
    planned_qty: Number(existing.planned_qty || 0) + Number(row.planned_qty || 0),
    failed_planned_qty: Number(existing.failed_planned_qty || 0) + Number(row.failed_planned_qty || 0),
    destination_location_id: existing.destination_location_id || row.destination_location_id || '',
    failed_destination_location_id: existing.failed_destination_location_id || row.failed_destination_location_id || '',
  });
  return acc;
}, new Map()).values());

export default function DeliveryOrderDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToast } = useUiStore();
  const { hasRole } = useAuthStore();

  const [order, setOrder] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [loadingCandidates, setLoadingCandidates] = useState(false);
  const [rejectModal, setRejectModal] = useState({ show: false, reason: '' });
  const [draftItems, setDraftItems] = useState([]);
  const [replacementDraftItems, setReplacementDraftItems] = useState([]);
  const [pickingCandidates, setPickingCandidates] = useState({});
  const [locations, setLocations] = useState([]);
  const [returnedFlow, setReturnedFlow] = useState(null);
  const [returnRows, setReturnRows] = useState([]);
  const [returnNotes, setReturnNotes] = useState('');
  const [returnRejectReason, setReturnRejectReason] = useState('');
  const [stockAvailabilities, setStockAvailabilities] = useState({});

  const getMissingQtySummary = () => {
    if (!order || !order.items) return 0;
    let totalMissing = 0;
    const currentStatus = order.status || order.raw_status;
    if (currentStatus !== 'QC_PENDING_APPROVAL') return 0;

    order.items.forEach((item) => {
      const requiredQty = Math.max(0, Number(item.requested_qty || 0) - Number(item.qc_pass_qty || 0));
      if (requiredQty > 0) {
        const availableInWarehouse = Number(stockAvailabilities[item.product_id] ?? 0);
        if (availableInWarehouse < requiredQty) {
          totalMissing += requiredQty - availableInWarehouse;
        }
      }
    });
    return totalMissing;
  };

  useEffect(() => {
    fetchOrder();
  }, [id]);

  const fetchOrder = async () => {
    setLoading(true);
    try {
      const data = await outboundService.getDeliveryOrderById(id);
      setOrder(data);
      setDraftItems(outboundService.createPickingPlanDraft(data.items || []));
      setReplacementDraftItems(outboundService.createReplacementPlanDraft(data.items || []));
      const baseReturnRows = mergeDuplicateReturnedRows(buildReturnedRows(data.items || []));
      setReturnRows(baseReturnRows);
      if (hasRole(ROLES.STOREKEEPER)
          && ['NEW', 'WAITING_PICKING', 'QC_PENDING_APPROVAL'].includes(data.status || data.raw_status)) {
        setLoadingCandidates(true);
        try {
          const candidates = await outboundService.getPickingCandidates(id);
          setPickingCandidates(candidates);
        } finally {
          setLoadingCandidates(false);
        }
      } else {
        setPickingCandidates({});
      }

      if (['QC_PENDING_APPROVAL'].includes(data.status || data.raw_status)) {
        const failedItems = (data.items || []).filter(
          (item) => Math.max(0, Number(item.requested_qty || 0) - Number(item.qc_pass_qty || 0)) > 0
        );
        const availabilities = {};
        await Promise.all(
          failedItems.map(async (item) => {
            try {
              const avail = await outboundService.getAvailability(data.warehouse_id, item.product_id);
              availabilities[item.product_id] = avail.available_qty;
            } catch {
              availabilities[item.product_id] = 0;
            }
          })
        );
        setStockAvailabilities(availabilities);
      }

      if ((data.status || data.raw_status) === 'RETURNED') {
        const bins = await masterDataService.getBinLocations(data.warehouse_id);
        setLocations(bins.filter((location) => location.is_active !== false));
        try {
          const flow = await outboundService.getReturnedGoodsFlow(id);
          setReturnedFlow(flow);
          setReturnRows(mergeReturnedFlowRows(baseReturnRows, flow));
        } catch {
          setReturnedFlow(null);
        }
      } else {
        setLocations([]);
        setReturnedFlow(null);
      }
    } catch (error) {
      addToast(error.message || 'Không tìm thấy đơn xuất hàng', 'error');
      navigate('/outbound/delivery-orders');
    } finally {
      setLoading(false);
    }
  };

  const handleStartPicking = async () => {
    const hasInvalidItem = draftItems.some((item) => {
      const plannedQty = (item.allocations || []).reduce(
        (sum, allocation) => sum + Number(allocation.planned_qty || 0),
        0,
      );
      const hasIncompleteAllocation = (item.allocations || []).some(
        (allocation) => Number(allocation.planned_qty || 0) > 0 && !allocation.inventory_id,
      );
      return hasIncompleteAllocation || plannedQty !== Number(item.requested_qty || 0);
    });

    if (hasInvalidItem) {
      addToast('Mỗi dòng hàng phải chọn đủ inventory và số lượng trước khi lưu.', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await outboundService.savePickingPlan(id, draftItems);
      addToast('Đã lưu kế hoạch lấy hàng', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Không thể bắt đầu soạn hàng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleApproveQuality = async () => {
    setSubmitting(true);
    try {
      await outboundService.approveQualityOutbound(id);
      addToast('Đã xác nhận chất lượng sau QC', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Lỗi khi xác nhận chất lượng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleApproveWarehouse = async () => {
    setSubmitting(true);
    try {
      await outboundService.approveWarehouseOutbound(id);
      addToast('Đã phê duyệt xuất kho', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Lỗi khi phê duyệt xuất kho', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!rejectModal.reason.trim()) {
      addToast('Vui lòng nhập lý do từ chối', 'error');
      return;
    }
    setSubmitting(true);
    try {
      await outboundService.rejectWarehouseOutbound(id, rejectModal.reason.trim(), order);
      addToast('Đã từ chối đơn xuất hàng', 'success');
      setRejectModal({ show: false, reason: '' });
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Lỗi khi từ chối đơn xuất hàng', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSaveReplacementPlan = async () => {
    const hasInvalidItem = replacementDraftItems.some((item) => {
      const plannedQty = (item.allocations || []).reduce(
        (sum, allocation) => sum + Number(allocation.planned_qty || 0),
        0,
      );
      const requiredQty = Number(item.replacement_required_qty || item.qc_fail_qty || 0);
      const availableInWarehouse = Number(stockAvailabilities[item.product_id] ?? requiredQty);
      const allowPartialPlan = availableInWarehouse < requiredQty;
      const hasIncompleteAllocation = (item.allocations || []).some((allocation) => (
        Number(allocation.planned_qty || 0) > 0
        && (!allocation.failed_inventory_id || !allocation.inventory_id || !allocation.reason?.trim())
      ));
      return hasIncompleteAllocation
        || plannedQty <= 0
        || plannedQty > requiredQty
        || (!allowPartialPlan && plannedQty !== requiredQty);
    });

    if (hasInvalidItem || replacementDraftItems.length === 0) {
      addToast('Cần chọn đủ hàng lỗi, hàng bù, số lượng và lý do trước khi lưu.', 'error');
      return;
    }

    setSubmitting(true);
    try {
      await outboundService.saveReplacementPlan(id, replacementDraftItems);
      addToast('Đã lưu kế hoạch lấy hàng bù', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Không thể lưu kế hoạch lấy hàng bù', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReturnRowChange = (rowKey, field, value) => {
    setReturnRows((previous) => previous.map((row) => {
      if (row.key !== rowKey) return row;
      if (field === 'quality_pass_qty') {
        return { ...row, quality_pass_qty: value, planned_qty: value };
      }
      if (field === 'quality_fail_qty') {
        return { ...row, quality_fail_qty: value, failed_planned_qty: value };
      }
      return { ...row, [field]: value };
    }));
  };

  const handleConfirmReturnedGoodsReceived = async () => {
    setSubmitting(true);
    try {
      const flow = await outboundService.confirmReturnedGoodsReceived(id, returnNotes);
      setReturnedFlow(flow);
      setReturnRows((previous) => mergeReturnedFlowRows(previous, flow));
      setReturnRejectReason('');
      addToast('Đã xác nhận hàng hoàn về kho. Staff có thể nhập kết quả kiểm tra.', 'success');
    } catch (error) {
      addToast(error.message || 'Không thể xác nhận hàng hoàn về kho', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmitReturnedCountQc = async () => {
    const mergedRows = mergeDuplicateReturnedRows(returnRows);
    const invalid = mergedRows.some((row) => (
      Number(row.actual_qty) < 0
      || Number(row.quality_pass_qty) < 0
      || Number(row.quality_fail_qty) < 0
      || Number(row.quality_pass_qty || 0) + Number(row.quality_fail_qty || 0) !== Number(row.actual_qty || 0)
      || (Number(row.quality_fail_qty || 0) > 0 && !String(row.quality_failure_reason || '').trim())
    ));
    if (invalid) {
      addToast('Vui lòng nhập đủ số lượng, chất lượng và lý do khi hàng không đạt.', 'error');
      return;
    }
    setSubmitting(true);
    try {
      const flow = await outboundService.submitReturnedGoodsCountQc(id, {
        notes: returnNotes,
        items: mergedRows.map((row) => ({
          do_item_id: row.do_item_id,
          product_id: row.product_id,
          batch_id: row.batch_id,
          expected_qty: Number(row.expected_qty || 0),
          actual_qty: Number(row.actual_qty || 0),
          quality_pass_qty: Number(row.quality_pass_qty || 0),
          quality_fail_qty: Number(row.quality_fail_qty || 0),
          quality_failure_reason: row.quality_failure_reason,
        })),
      });
      setReturnedFlow(flow);
      addToast('Đã gửi kết quả kiểm đếm hàng hoàn', 'success');
    } catch (error) {
      addToast(error.message || 'Không thể gửi kết quả kiểm đếm hàng hoàn', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleApproveReturnedGoods = async () => {
    setSubmitting(true);
    try {
      const flow = await outboundService.approveReturnedGoods(id, returnNotes);
      setReturnedFlow(flow);
      addToast('Đã duyệt số lượng và chất lượng hàng hoàn', 'success');
    } catch (error) {
      addToast(error.message || 'Không thể duyệt hàng hoàn', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleRejectReturnedGoodsQc = async () => {
    if (!returnRejectReason.trim()) {
      addToast('Vui lòng nhập lý do từ chối kết quả QC.', 'error');
      return;
    }
    setSubmitting(true);
    try {
      const flow = await outboundService.rejectReturnedGoodsQc(id, returnRejectReason.trim(), returnNotes);
      setReturnedFlow(flow);
      setReturnRows((previous) => mergeReturnedFlowRows(previous, flow));
      addToast('Đã từ chối kết quả QC. Staff cần kiểm tra và gửi lại.', 'success');
    } catch (error) {
      addToast(error.message || 'Không thể từ chối kết quả QC hàng hoàn', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handlePlanReturnedPutaway = async () => {
    const mergedRows = mergeDuplicateReturnedRows(returnRows);
    const invalid = mergedRows.some((row) => (
      (Number(row.quality_pass_qty || 0) > 0
        && (!row.destination_location_id || Number(row.planned_qty || 0) !== Number(row.quality_pass_qty || 0)))
      || (Number(row.quality_fail_qty || 0) > 0
        && (!row.failed_destination_location_id || Number(row.failed_planned_qty || 0) !== Number(row.quality_fail_qty || 0)))
    ));
    if (invalid) {
      addToast('Vui lòng chọn đúng vị trí cất hàng đạt và vị trí cách ly cho hàng lỗi.', 'error');
      return;
    }
    setSubmitting(true);
    try {
      const flow = await outboundService.planReturnedGoodsPutaway(id, {
        notes: returnNotes,
        items: mergedRows.map((row) => ({
          do_item_id: row.do_item_id,
          batch_id: row.batch_id,
          destination_location_id: Number(row.destination_location_id),
          planned_qty: Number(row.planned_qty || 0),
          failed_destination_location_id: row.failed_destination_location_id ? Number(row.failed_destination_location_id) : null,
          failed_planned_qty: Number(row.failed_planned_qty || 0),
        })),
      });
      setReturnedFlow(flow);
      addToast('Đã lưu kế hoạch cất hàng hoàn', 'success');
    } catch (error) {
      addToast(error.message || 'Không thể lưu kế hoạch cất hàng hoàn', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCompleteReturnedPutaway = async () => {
    setSubmitting(true);
    try {
      const flow = await outboundService.completeReturnedGoodsPutaway(id, returnNotes);
      setReturnedFlow(flow);
      addToast('Đã xác nhận cất hàng hoàn, đơn chuyển sang giao hàng thất bại', 'success');
      fetchOrder();
    } catch (error) {
      addToast(error.message || 'Không thể xác nhận cất hàng hoàn', 'error');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center p-20">
        <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
      </div>
    );
  }

  if (!order) return null;

  const currentStatus = order.status || order.raw_status;
  const canEditPickingPlan = ['NEW', 'WAITING_PICKING'].includes(currentStatus) && hasRole(ROLES.STOREKEEPER);
  const canOpenQc = currentStatus === 'WAITING_PICKING' && hasRole(ROLES.WAREHOUSE_STAFF);
  const hasUnresolvedQcFail = (order.items || []).some((item) => (
    Math.max(0, Number(item.requested_qty || 0) - Number(item.qc_pass_qty || 0)) > 0
  ));
  const canPlanReplacement = currentStatus === 'QC_PENDING_APPROVAL' && hasRole(ROLES.STOREKEEPER) && hasUnresolvedQcFail;
  const canApproveQuality = currentStatus === 'QC_PENDING_APPROVAL' && hasRole(ROLES.STOREKEEPER) && !hasUnresolvedQcFail;
  const canApproveWarehouse = currentStatus === 'QC_COMPLETED' && hasRole(ROLES.WAREHOUSE_MANAGER);
  const canHandleReturned = currentStatus === 'RETURNED' && (hasRole(ROLES.WAREHOUSE_STAFF) || hasRole(ROLES.STOREKEEPER));
  const returnFlowStatus = returnedFlow?.flow_status;
  const displayedItems = canEditPickingPlan ? draftItems : order.items;

  const handleAddAllocation = (itemId) => {
    setDraftItems((previous) => previous.map((item) => (
      Number(item.id) === Number(itemId)
        ? {
            ...item,
            allocations: [...(item.allocations || []), outboundService.createEmptyAllocationDraft()],
          }
        : item
    )));
  };

  const handleAddReplacementAllocation = (itemId) => {
    setReplacementDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;
      const defaultFailedSource = (item.failed_sources || []).length === 1 ? item.failed_sources[0] : {};
      return {
        ...item,
        allocations: [
          ...(item.allocations || []),
          outboundService.createEmptyReplacementAllocationDraft(defaultFailedSource, 0),
        ],
      };
    }));
  };

  const handleRemoveAllocation = (itemId, allocationIndex) => {
    setDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;
      const nextAllocations = (item.allocations || []).filter((_, index) => index !== allocationIndex);
      return {
        ...item,
        allocations: nextAllocations.length ? nextAllocations : [outboundService.createEmptyAllocationDraft()],
      };
    }));
  };

  const handleRemoveReplacementAllocation = (itemId, allocationIndex) => {
    setReplacementDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;
      const nextAllocations = (item.allocations || []).filter((_, index) => index !== allocationIndex);
      const defaultFailedSource = (item.failed_sources || []).length === 1 ? item.failed_sources[0] : {};
      return {
        ...item,
        allocations: nextAllocations.length
          ? nextAllocations
          : [outboundService.createEmptyReplacementAllocationDraft(defaultFailedSource, 0)],
      };
    }));
  };

  const handleAllocationChange = (itemId, allocationIndex, field, value) => {
    setDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;
      return {
        ...item,
        allocations: (item.allocations || []).map((allocation, index) => (
          index === allocationIndex
            ? {
                ...allocation,
                [field]: field === 'planned_qty' ? Number(value || 0) : value,
              }
            : allocation
        )),
      };
    }));
  };

  const handleReplacementAllocationChange = (itemId, allocationIndex, field, value) => {
    setReplacementDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;
      return {
        ...item,
        allocations: (item.allocations || []).map((allocation, index) => (
          index === allocationIndex
            ? {
                ...allocation,
                [field]: field === 'planned_qty' ? Number(value || 0) : value,
              }
            : allocation
        )),
      };
    }));
  };

  const handleCandidateSelect = (itemId, allocationIndex, inventoryId) => {
    const candidate = (pickingCandidates[itemId] || []).find(
      (row) => Number(row.inventory_id) === Number(inventoryId),
    );
    if (!candidate) {
      handleAllocationChange(itemId, allocationIndex, 'inventory_id', '');
      return;
    }

    setDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;

      const totalOtherQty = (item.allocations || []).reduce((sum, allocation, index) => (
        index === allocationIndex ? sum : sum + Number(allocation.planned_qty || 0)
      ), 0);
      const remainingQty = Math.max(0, Number(item.requested_qty || 0) - totalOtherQty);

      return {
        ...item,
        allocations: (item.allocations || []).map((allocation, index) => (
          index === allocationIndex
            ? outboundService.applyPickingCandidate(
                candidate,
                Number(allocation.planned_qty || 0),
              )
            : allocation
        )),
      };
    }));
  };

  const handleReplacementCandidateSelect = (itemId, allocationIndex, inventoryId) => {
    const candidate = (pickingCandidates[itemId] || []).find(
      (row) => Number(row.inventory_id) === Number(inventoryId),
    );
    if (!candidate) {
      handleReplacementAllocationChange(itemId, allocationIndex, 'inventory_id', '');
      return;
    }

    setReplacementDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;

      const totalOtherQty = (item.allocations || []).reduce((sum, allocation, index) => (
        index === allocationIndex ? sum : sum + Number(allocation.planned_qty || 0)
      ), 0);
      const remainingQty = Math.max(0, Number(item.replacement_required_qty || 0) - totalOtherQty);

      return {
        ...item,
        allocations: (item.allocations || []).map((allocation, index) => (
          index === allocationIndex
            ? {
                ...allocation,
                ...outboundService.applyPickingCandidate(
                  candidate,
                  Number(allocation.planned_qty || 0),
                ),
              }
            : allocation
        )),
      };
    }));
  };

  const handleReplacementFailedSourceSelect = (itemId, allocationIndex, inventoryId) => {
    setReplacementDraftItems((previous) => previous.map((item) => {
      if (Number(item.id) !== Number(itemId)) return item;
      const failedSource = (item.failed_sources || []).find(
        (row) => Number(row.inventory_id) === Number(inventoryId),
      );

      return {
        ...item,
        allocations: (item.allocations || []).map((allocation, index) => (
          index === allocationIndex
            ? {
                ...allocation,
                failed_inventory_id: failedSource?.inventory_id || '',
                failed_batch_id: failedSource?.batch_id || '',
                failed_batch_code: failedSource?.batch_code || '',
                failed_location_id: failedSource?.location_id || '',
                failed_location_code: failedSource?.location_code || '',
              }
            : allocation
        )),
      };
    }));
  };

  const hasInvalidDraft = draftItems.some((item) => {
    const plannedQty = (item.allocations || []).reduce(
      (sum, allocation) => sum + Number(allocation.planned_qty || 0),
      0,
    );
    const hasIncompleteAllocation = (item.allocations || []).some(
      (allocation) => Number(allocation.planned_qty || 0) > 0 && !allocation.inventory_id,
    );
    return hasIncompleteAllocation || plannedQty !== Number(item.requested_qty || 0);
  });

  const hasInvalidReplacementDraft = replacementDraftItems.some((item) => {
    const plannedQty = (item.allocations || []).reduce(
      (sum, allocation) => sum + Number(allocation.planned_qty || 0),
      0,
    );
    const requiredQty = Number(item.replacement_required_qty || item.qc_fail_qty || 0);
    const availableInWarehouse = Number(stockAvailabilities[item.product_id] ?? requiredQty);
    const allowPartialPlan = availableInWarehouse < requiredQty;
    const hasIncompleteAllocation = (item.allocations || []).some((allocation) => (
      Number(allocation.planned_qty || 0) > 0
      && (!allocation.failed_inventory_id || !allocation.inventory_id || !allocation.reason?.trim())
    ));
    return hasIncompleteAllocation
      || plannedQty <= 0
      || plannedQty > requiredQty
      || (!allowPartialPlan && plannedQty !== requiredQty);
  });

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
        <div className="flex items-start gap-4">
          <button
            onClick={() => navigate('/outbound/delivery-orders')}
            className="mt-1 p-1.5 hover:bg-canvas-cream rounded-full transition-colors text-shade-50 hover:text-ink shrink-0"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div>
            <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">Vận hành / Xuất kho</span>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">{order.do_number}</h1>
              {getStatusBadge(currentStatus)}
            </div>
            <p className="text-xs text-shade-50 font-light mt-1">
              Lập ngày: {order.document_date ? new Date(order.document_date).toLocaleDateString('vi-VN') : '-'}
            </p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-3 ml-10 md:ml-0">
          {canOpenQc && (
            <button onClick={() => navigate(`/outbound/qc/${id}`)} className="btn-pill btn-pill-primary flex items-center gap-2">
              <PackageSearch className="w-4 h-4" /> Nhập kết quả lấy hàng & kiểm định
            </button>
          )}

          {canApproveQuality && (
            <button disabled={submitting} onClick={handleApproveQuality} className="btn-pill btn-pill-aloe flex items-center gap-2 disabled:opacity-50">
              <CheckCircle2 className="w-4 h-4" /> Duyệt kết quả QC
            </button>
          )}

          {canApproveWarehouse && (
            <>
              <button
                disabled={submitting}
                onClick={() => setRejectModal({ show: true, reason: '' })}
                className="btn-pill border border-danger-300 text-danger-600 hover:bg-danger-50 flex items-center gap-2 disabled:opacity-50"
              >
                <X className="w-4 h-4" /> Từ chối
              </button>
              <button disabled={submitting} onClick={handleApproveWarehouse} className="btn-pill btn-pill-aloe flex items-center gap-2 disabled:opacity-50">
                <CheckCircle2 className="w-4 h-4" /> Phê duyệt xuất kho
              </button>
            </>
          )}
        </div>
      </div>

      {currentStatus === 'WAITING_PICKING' && (
        <div className="bg-info-50 border border-info-200 rounded-lg p-4 flex items-center gap-3">
          <Clock className="w-4 h-4 text-info-700 shrink-0" />
          <p className="text-xs font-semibold text-info-900">
            Kế hoạch lấy hàng được storekeeper <span className="font-bold">{order.picking_plan_saved_by_name || 'N/A'}</span> lưu. Nhân viên kho nhập số lượng đã lấy và kết quả QC theo từng dòng phân bổ.
          </p>
        </div>
      )}

      {getMissingQtySummary() > 0 && (
        <div className="bg-danger-50 border border-danger-200 rounded-lg p-4 flex items-center gap-3">
          <AlertTriangle className="w-4 h-4 text-danger-700 shrink-0" />
          <p className="text-xs font-semibold text-danger-900">
            Thiếu <span className="font-bold">{getMissingQtySummary()}</span> sản phẩm. Chờ nhập thêm hàng trong thời gian gần nhất.
          </p>
        </div>
      )}

      {currentStatus === 'QC_PENDING_APPROVAL' && (
        <div className="bg-violet-50 border border-violet-200 rounded-lg p-4 flex items-center gap-3">
          <CheckCircle2 className="w-4 h-4 text-violet-700 shrink-0" />
          <p className="text-xs font-semibold text-violet-900">
            Nhân viên kho <span className="font-bold">{order.qc_by_name || 'N/A'}</span> đã gửi kết quả lấy hàng & kiểm định. Thủ kho rà soát và duyệt chất lượng trước khi chuyển bước tiếp.
          </p>
        </div>
      )}

      {canPlanReplacement && (
        <div className="bg-warning-50 border border-warning-200 rounded-lg p-4 flex items-start gap-3">
          <AlertTriangle className="w-4 h-4 text-warning-700 shrink-0 mt-0.5" />
          <div>
            <p className="text-xs font-bold text-warning-900">Có hàng QC fail cần lập kế hoạch lấy bù.</p>
            <p className="mt-1 text-xs text-warning-800">
              Chọn dòng hàng lỗi, chọn hàng trong kho thay thế và lưu kế hoạch. Đơn sẽ quay về trạng thái chờ lấy hàng để staff xử lý vòng tiếp theo.
            </p>
          </div>
        </div>
      )}

      {order.cancel_reason && (
        <div className="bg-danger-50 border border-danger-200 rounded-lg p-4 flex items-start gap-3">
          <AlertTriangle className="w-4 h-4 text-danger-600 shrink-0 mt-0.5" />
          <div>
            <p className="text-xs font-bold text-danger-700">Ghi chú trạng thái:</p>
            <p className="text-xs text-danger-600 mt-0.5">{order.cancel_reason}</p>
          </div>
        </div>
      )}

      {canHandleReturned && (
        <div className="rounded-lg border border-orange-200 bg-orange-50/50 p-4 shadow-level-3">
          <div className="mb-4 flex flex-col gap-2 md:flex-row md:items-start md:justify-between">
            <div>
              <h2 className="flex items-center gap-2 text-sm font-bold text-orange-900">
                <RotateCcw className="h-4 w-4" /> Xử lý hàng hoàn về kho
              </h2>
              <p className="mt-1 text-xs text-orange-800">
                Đơn đang ở trạng thái RETURNED. Sau khi staff kiểm đếm, storekeeper duyệt và lập vị trí cất; staff xác nhận cất xong thì đơn chuyển sang DELIVERY_FAILED.
              </p>
            </div>
            {returnFlowStatus && <Badge size="sm" colorClassName="bg-canvas-light text-orange-700 border-orange-200">{returnFlowStatus}</Badge>}
          </div>

          <div className="overflow-x-auto rounded-lg border border-orange-100 bg-canvas-light">
            <table className="w-full min-w-[1100px] text-left text-xs">
              <thead className="bg-canvas-cream text-[10px] uppercase tracking-wider text-shade-60">
                <tr>
                  <th className="px-3 py-2">Sản phẩm</th>
                  <th className="px-3 py-2">Batch</th>
                  <th className="px-3 py-2">SL dự kiến</th>
                  <th className="px-3 py-2">SL thực</th>
                  <th className="px-3 py-2">SL đạt</th>
                  <th className="px-3 py-2">SL lỗi</th>
                  <th className="px-3 py-2">Lý do lỗi</th>
                  <th className="px-3 py-2">Vị trí hàng đạt</th>
                  <th className="px-3 py-2">SL cất hàng đạt</th>
                  <th className="px-3 py-2">Vị trí cách ly</th>
                  <th className="px-3 py-2">SL cách ly</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {returnRows.map((row) => {
                  const passLocationOptions = locations.filter((location) => (
                    location.is_quarantine !== true && location.is_staging !== true
                  ));
                  const failLocationOptions = locations.filter((location) => location.is_quarantine === true);
                  const countDisabled = !hasRole(ROLES.WAREHOUSE_STAFF) || !canStaffSubmitReturnedQc(returnFlowStatus);
                  const planDisabled = !hasRole(ROLES.STOREKEEPER) || returnFlowStatus !== 'QC_APPROVED';
                  return (
                    <tr key={row.key}>
                      <td className="px-3 py-3">
                        <p className="font-semibold text-ink">{row.product_name}</p>
                        <p className="mt-0.5 text-[11px] text-shade-50">{row.sku || `#${row.product_id}`}</p>
                      </td>
                      <td className="px-3 py-3 font-mono text-shade-60">{row.batch_id}</td>
                      <td className="px-3 py-3 font-semibold">{row.expected_qty}</td>
                      <td className="px-3 py-3">
                        <input
                          type="number"
                          min="0"
                          step="0.01"
                          disabled={countDisabled}
                          value={row.actual_qty}
                          onChange={(event) => handleReturnRowChange(row.key, 'actual_qty', event.target.value)}
                          className="w-24 rounded-md border border-hairline-light bg-canvas-light px-2 py-1.5 text-xs disabled:bg-canvas-cream"
                        />
                      </td>
                      <td className="px-3 py-3">
                        <input
                          type="number"
                          min="0"
                          step="0.01"
                          disabled={countDisabled}
                          value={row.quality_pass_qty}
                          onChange={(event) => handleReturnRowChange(row.key, 'quality_pass_qty', event.target.value)}
                          className="w-28 rounded-md border border-hairline-light bg-canvas-light px-2 py-1.5 text-xs disabled:bg-canvas-cream"
                        />
                      </td>
                      <td className="px-3 py-3">
                        <input
                          type="number"
                          min="0"
                          step="0.01"
                          disabled={countDisabled}
                          value={row.quality_fail_qty}
                          onChange={(event) => handleReturnRowChange(row.key, 'quality_fail_qty', event.target.value)}
                          className="w-24 rounded-md border border-hairline-light bg-canvas-light px-2 py-1.5 text-xs disabled:bg-canvas-cream"
                        />
                      </td>
                      <td className="px-3 py-3">
                        <input
                          disabled={countDisabled || Number(row.quality_fail_qty || 0) <= 0}
                          value={row.quality_failure_reason}
                          onChange={(event) => handleReturnRowChange(row.key, 'quality_failure_reason', event.target.value)}
                          placeholder="Khi có hàng lỗi"
                          className="w-40 rounded-md border border-hairline-light bg-canvas-light px-2 py-1.5 text-xs disabled:bg-canvas-cream"
                        />
                      </td>
                      <td className="px-3 py-3">
                        <select
                          disabled={planDisabled || Number(row.quality_pass_qty || 0) <= 0}
                          value={row.destination_location_id}
                          onChange={(event) => handleReturnRowChange(row.key, 'destination_location_id', event.target.value)}
                          className="w-44 rounded-md border border-hairline-light bg-canvas-light px-2 py-1.5 text-xs disabled:bg-canvas-cream"
                        >
                          <option value="">Chọn bin hàng đạt</option>
                          {passLocationOptions.map((location) => (
                            <option key={location.id} value={location.id}>
                              {location.code || `Bin #${location.id}`}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="px-3 py-3">
                        <input
                          type="number"
                          min="0.01"
                          step="0.01"
                          disabled={planDisabled || Number(row.quality_pass_qty || 0) <= 0}
                          value={row.planned_qty}
                          onChange={(event) => handleReturnRowChange(row.key, 'planned_qty', event.target.value)}
                          className="w-24 rounded-md border border-hairline-light bg-canvas-light px-2 py-1.5 text-xs disabled:bg-canvas-cream"
                        />
                      </td>
                      <td className="px-3 py-3">
                        <select
                          disabled={planDisabled || Number(row.quality_fail_qty || 0) <= 0}
                          value={row.failed_destination_location_id}
                          onChange={(event) => handleReturnRowChange(row.key, 'failed_destination_location_id', event.target.value)}
                          className="w-44 rounded-md border border-hairline-light bg-canvas-light px-2 py-1.5 text-xs disabled:bg-canvas-cream"
                        >
                          <option value="">Chọn khu cách ly</option>
                          {failLocationOptions.map((location) => (
                            <option key={location.id} value={location.id}>
                              {location.code || `Bin #${location.id}`}
                            </option>
                          ))}
                        </select>
                      </td>
                      <td className="px-3 py-3">
                        <input
                          type="number"
                          min="0.01"
                          step="0.01"
                          disabled={planDisabled || Number(row.quality_fail_qty || 0) <= 0}
                          value={row.failed_planned_qty}
                          onChange={(event) => handleReturnRowChange(row.key, 'failed_planned_qty', event.target.value)}
                          className="w-24 rounded-md border border-hairline-light bg-canvas-light px-2 py-1.5 text-xs disabled:bg-canvas-cream"
                        />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-[1fr_auto] md:items-end">
            <div>
              <label className="mb-1.5 block text-xs font-semibold uppercase tracking-wider text-shade-60">Ghi chú xử lý</label>
              <textarea
                rows={3}
                value={returnNotes}
                onChange={(event) => setReturnNotes(event.target.value)}
                className="w-full resize-none rounded-md border border-hairline-light bg-canvas-light px-3 py-2 text-sm"
              />
            </div>
            <div className="flex flex-wrap justify-end gap-2">
              {hasRole(ROLES.STOREKEEPER) && !returnFlowStatus && (
                <Button disabled={submitting || returnRows.length === 0} onClick={handleConfirmReturnedGoodsReceived} variant="primary">
                  Xác nhận hàng về kho
                </Button>
              )}
              {hasRole(ROLES.WAREHOUSE_STAFF) && canStaffSubmitReturnedQc(returnFlowStatus) && (
                <Button disabled={submitting || returnRows.length === 0} onClick={handleSubmitReturnedCountQc} variant="primary">
                  Gửi kiểm đếm
                </Button>
              )}
              {hasRole(ROLES.STOREKEEPER) && returnFlowStatus === 'COUNT_QC_SUBMITTED' && (
                <>
                  <input
                    value={returnRejectReason}
                    onChange={(event) => setReturnRejectReason(event.target.value)}
                    placeholder="Lý do từ chối QC"
                    className="min-w-[180px] rounded-md border border-hairline-light bg-canvas-light px-3 py-2 text-xs"
                  />
                  <Button disabled={submitting || !returnRejectReason.trim()} onClick={handleRejectReturnedGoodsQc} variant="outline-light">
                    Từ chối QC
                  </Button>
                  <Button disabled={submitting} onClick={handleApproveReturnedGoods} variant="primary">
                    Duyệt QC
                  </Button>
                </>
              )}
              {hasRole(ROLES.STOREKEEPER) && returnFlowStatus === 'QC_APPROVED' && (
                <Button disabled={submitting} onClick={handlePlanReturnedPutaway} variant="primary">
                  Lưu vị trí cất
                </Button>
              )}
              {hasRole(ROLES.WAREHOUSE_STAFF) && returnFlowStatus === 'PUTAWAY_PLANNED' && (
                <Button disabled={submitting} onClick={handleCompleteReturnedPutaway} variant="primary">
                  Xác nhận cất xong
                </Button>
              )}
            </div>
          </div>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-canvas-light rounded-lg border border-hairline-light p-5 shadow-level-3">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3 flex items-center gap-2">
            <MapPin className="w-3.5 h-3.5" /> Thông tin đại lý
          </h3>
          <div className="flex flex-col gap-1.5 text-xs">
            <p><span className="text-shade-50">Tên đại lý:</span> <span className="font-semibold text-ink">{order.dealer_name}</span></p>
            <p><span className="text-shade-50">Mã đại lý:</span> <span className="font-mono text-ink">{order.dealer_id}</span></p>
          </div>
        </div>

        <div className="bg-canvas-light rounded-lg border border-hairline-light p-5 shadow-level-3">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3 flex items-center gap-2">
            <Clock className="w-3.5 h-3.5" /> Tiến độ giao hàng
          </h3>
          <div className="flex flex-col gap-1.5 text-xs">
            <p>
              <span className="text-shade-50">Ngày giao dự kiến:</span>{' '}
              <span className="font-semibold text-ink">{order.expected_delivery_date ? new Date(order.expected_delivery_date).toLocaleDateString('vi-VN') : '-'}</span>
            </p>
            <p><span className="text-shade-50">Trạng thái backend:</span> <span className="font-semibold text-ink">{currentStatus}</span></p>
          </div>
        </div>

        <div className="bg-canvas-light rounded-lg border border-hairline-light p-5 shadow-level-3">
          <h3 className="text-xs font-bold uppercase tracking-widest text-shade-40 mb-3 flex items-center gap-2">
            <FileText className="w-3.5 h-3.5" /> Ghi chú
          </h3>
          <p className="text-xs text-shade-60 italic bg-canvas-cream p-3 rounded border border-hairline-light min-h-[48px]">
            {order.notes || 'Không có ghi chú'}
          </p>
        </div>
      </div>

      {canEditPickingPlan && (
        <div className="bg-info-50 border border-info-200 rounded-lg p-4 flex items-center gap-3 mb-4">
          <Info className="w-4 h-4 text-info-700 shrink-0" />
          <p className="text-xs font-semibold text-info-900">
            Người thao tác trước: <span className="font-bold">{order.created_by_name || 'N/A'}</span> đã tạo phiếu xuất kho ở dưới.
          </p>
        </div>
      )}

      {canEditPickingPlan && (
        <DeliveryOrderPickingPlanEditor
          items={draftItems}
          candidatesByItemId={pickingCandidates}
          submitting={submitting || loadingCandidates}
          disableSave={hasInvalidDraft}
          onAddAllocation={handleAddAllocation}
          onAllocationChange={handleAllocationChange}
          onCandidateSelect={handleCandidateSelect}
          onRemoveAllocation={handleRemoveAllocation}
          onSave={handleStartPicking}
        />
      )}

      {canPlanReplacement && (
        <DeliveryOrderPickingPlanEditor
          mode="replacement"
          title="Lập kế hoạch lấy hàng bù"
          description="Khi hàng đã lấy bị QC fail. Storekeeper chọn nguồn hàng bù để staff lấy và QC lại."
          saveLabel="Lưu kế hoạch lấy bù"
          items={replacementDraftItems}
          candidatesByItemId={pickingCandidates}
          stockAvailabilities={stockAvailabilities}
          submitting={submitting || loadingCandidates}
          disableSave={hasInvalidReplacementDraft || replacementDraftItems.length === 0}
          onAddAllocation={handleAddReplacementAllocation}
          onAllocationChange={handleReplacementAllocationChange}
          onCandidateSelect={handleReplacementCandidateSelect}
          onFailedSourceSelect={handleReplacementFailedSourceSelect}
          onRemoveAllocation={handleRemoveReplacementAllocation}
          onSave={handleSaveReplacementPlan}
        />
      )}

      {canPlanReplacement && hasInvalidReplacementDraft && (
        <div className="bg-warning-50 border border-warning-200 rounded-lg p-4 flex items-center gap-3">
          <AlertTriangle className="w-4 h-4 text-warning-700 shrink-0" />
          <p className="text-xs font-semibold text-warning-900">
            Mỗi dòng lấy bù phải chọn hàng lỗi QC, tồn kho thay thế, nhập đủ số lượng cần bù và lý do.
          </p>
        </div>
      )}

      {canEditPickingPlan && hasInvalidDraft && (
        <div className="bg-warning-50 border border-warning-200 rounded-lg p-4 flex items-center gap-3">
          <AlertTriangle className="w-4 h-4 text-warning-700 shrink-0" />
          <p className="text-xs font-semibold text-warning-900">
            Mỗi dòng hàng phải được phân bổ đủ số lượng yêu cầu.
          </p>
        </div>
      )}

      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xs font-bold uppercase tracking-widest text-shade-40 flex items-center gap-2">
            <PackageSearch className="w-3.5 h-3.5" /> Chi tiết hàng hóa ({order.items.length} mặt hàng)
          </h2>
        </div>
        <PickingListTable items={displayedItems} />
      </div>

      <Modal isOpen={rejectModal.show} onClose={() => setRejectModal({ show: false, reason: '' })} title="Từ chối đơn xuất hàng" maxWidth="max-w-md">
        <div className="flex flex-col gap-4">
          <div className="flex items-center gap-2 text-danger-600 text-sm font-semibold">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            Hành động này trả đơn về trạng thái cần xử lý lại.
          </div>
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-shade-60 mb-1.5">Lý do từ chối *</label>
            <textarea
              rows={4}
              placeholder="Nhập lý do từ chối đơn này..."
              value={rejectModal.reason}
              onChange={(event) => setRejectModal((prev) => ({ ...prev, reason: event.target.value }))}
              className="w-full bg-canvas-light text-ink text-sm px-3 py-2.5 rounded-md border border-hairline-light focus:outline-none focus:ring-1 focus:ring-ink focus:border-ink transition-all resize-none min-h-[44px]"
            />
          </div>
          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4">
            <Button variant="outline-light" onClick={() => setRejectModal({ show: false, reason: '' })}>Hủy</Button>
            <button
              disabled={!rejectModal.reason.trim() || submitting}
              onClick={handleReject}
              className="rounded-pill font-medium transition-all duration-150 inline-flex items-center justify-center gap-2 text-sm focus:outline-none focus:ring-2 focus:ring-offset-2 px-6 py-2.5 bg-danger-600 text-white hover:bg-danger-700 focus:ring-danger-500 disabled:opacity-50 disabled:pointer-events-none"
            >
              Xác nhận từ chối
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
