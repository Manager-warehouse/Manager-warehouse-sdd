import React, { useEffect, useState } from 'react';
import reportService from '../../services/report.service';
import { AlertTriangle, CheckCircle, RefreshCw, Warehouse, HelpCircle, ChevronLeft, ChevronRight, Loader2 } from 'lucide-react';
import Badge from '../../components/common/Badge';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';

const LowStockAlerts = () => {
  const [alerts, setAlerts] = useState([]);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters
  const [warehouseId, setWarehouseId] = useState('');
  const [isResolved, setIsResolved] = useState('false'); // default xem các active alerts

  useEffect(() => {
    fetchAlerts();
  }, [page, warehouseId, isResolved]);

  const fetchAlerts = async () => {
    setLoading(true);
    setError(null);
    try {
      const resolvedParam = isResolved === 'all' ? undefined : (isResolved === 'true');
      const res = await reportService.getLowStockAlerts({
        page,
        size: 15,
        warehouseId: warehouseId || undefined,
        isResolved: resolvedParam
      });
      setAlerts(res.content || []);
      setTotalElements(res.totalElements || 0);
      setTotalPages(res.totalPages || 0);
    } catch (err) {
      setError(err.response?.data?.message || 'Không thể tải danh sách cảnh báo tồn kho.');
    } finally {
      setLoading(false);
    }
  };

  const getAlertTag = (type) => {
    if (type === 'OUT_OF_STOCK') {
      return <Badge type="danger">HẾT HÀNG</Badge>;
    }
    return <Badge type="warning">TỒN THẤP</Badge>;
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Giám sát vận hành
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Cảnh Báo Tồn Kho Dưới Định Mức
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Hệ thống tự động phát hiện khi tồn khả dụng (available qty) của sản phẩm tại kho vật lý tụt dưới ngưỡng đặt hàng lại.
          </p>
        </div>

        <div className="flex gap-2 flex-wrap md:flex-nowrap items-center">
          <Input
            type="select"
            value={warehouseId}
            onChange={(e) => { setWarehouseId(e.target.value); setPage(0); }}
            options={[
              { value: '', label: 'Tất cả kho vật lý' },
              { value: '1', label: 'Kho Hải Phòng' },
              { value: '2', label: 'Kho Hà Nội' },
              { value: '3', label: 'Kho Hồ Chí Minh' },
            ]}
            className="flex-1"
          />
          <Input
            type="select"
            value={isResolved}
            onChange={(e) => { setIsResolved(e.target.value); setPage(0); }}
            options={[
              { value: 'false', label: 'Đang cảnh báo (Chưa xử lý)' },
              { value: 'true', label: 'Đã bổ sung (Đã xử lý)' },
              { value: 'all', label: 'Tất cả lịch sử' },
            ]}
            className="flex-1"
          />
          <Button variant="outline-light" icon={RefreshCw} onClick={fetchAlerts}>
            Làm mới
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center min-h-[300px] gap-3">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
          <span className="text-sm text-shade-60">Đang quét dữ liệu tồn kho dưới định mức...</span>
        </div>
      ) : error ? (
        <div className="bg-danger-50 border border-danger-200 rounded-lg p-6 max-w-2xl mx-auto my-12 text-center">
          <AlertTriangle className="w-12 h-12 text-danger-600 mx-auto mb-4" />
          <h3 className="text-lg font-bold text-danger-800">Lỗi Hệ Thống</h3>
          <p className="text-sm text-danger-600 mt-2">{error}</p>
        </div>
      ) : (
        <>
          {/* Details Table */}
          <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden flex flex-col">
            <div className="flex items-center justify-between border-b border-hairline-light px-6 py-4">
              <h3 className="text-sm font-bold text-shade-70 uppercase tracking-wider">
                Danh sách cảnh báo ({totalElements} bản ghi)
              </h3>
            </div>

            {alerts.length === 0 ? (
              <div className="px-6 py-12 text-center text-shade-50 text-xs">
                Tuyệt vời! Không có cảnh báo tồn kho nào cần xử lý.
              </div>
            ) : (
              <>
                {/* Desktop/tablet: table view */}
                <div className="hidden md:block overflow-x-auto">
                  <table className="w-full text-left border-collapse">
                    <thead>
                      <tr className="bg-canvas-cream border-b border-hairline-light">
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Kho vật lý</th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã SKU</th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Tên sản phẩm</th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center">Loại cảnh báo</th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Tồn khả dụng</th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Định mức tối thiểu</th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center">Trạng thái</th>
                        <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Thời gian cảnh báo</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-hairline-light">
                      {alerts.map((alert) => (
                        <tr
                          key={alert.id}
                          className={`hover:bg-canvas-cream/50 transition-colors ${!alert.is_resolved ? 'bg-danger-50/20' : ''}`}
                        >
                          <td className="px-6 py-4 text-xs font-semibold text-ink">{alert.warehouse_name}</td>
                          <td className="px-6 py-4 text-xs font-mono font-medium">{alert.product_sku}</td>
                          <td className="px-6 py-4 text-xs font-medium">{alert.product_name}</td>
                          <td className="px-6 py-4 text-xs text-center">{getAlertTag(alert.alert_type)}</td>
                          <td className={`px-6 py-4 text-xs text-right font-bold ${alert.current_qty === 0 ? 'text-danger-600' : 'text-orange-600'}`}>
                            {new Intl.NumberFormat('vi-VN').format(alert.current_qty)}
                          </td>
                          <td className="px-6 py-4 text-xs text-right text-shade-60">
                            {new Intl.NumberFormat('vi-VN').format(alert.reorder_point)}
                          </td>
                          <td className="px-6 py-4 text-center">
                            <Badge size="sm" type={alert.is_resolved ? 'success' : 'danger'} className={alert.is_resolved ? '' : 'animate-pulse'}>
                              <span className="inline-flex items-center gap-1">
                                {alert.is_resolved ? <CheckCircle className="w-3 h-3" /> : <AlertTriangle className="w-3 h-3" />}
                                {alert.is_resolved ? 'Đã bổ sung' : 'Cần bổ sung'}
                              </span>
                            </Badge>
                          </td>
                          <td className="px-6 py-4 text-xs text-shade-60">
                            {new Date(alert.created_at).toLocaleString('vi-VN')}
                            {alert.is_resolved && alert.resolved_at && (
                              <span className="block text-[10px] text-success-600">
                                Bổ sung lúc: {new Date(alert.resolved_at).toLocaleString('vi-VN')}
                              </span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Mobile: stacked card view */}
                <div className="flex flex-col gap-3 p-4 md:hidden">
                  {alerts.map((alert) => (
                    <div
                      key={alert.id}
                      className={`rounded-lg border border-hairline-light overflow-hidden ${!alert.is_resolved ? 'bg-danger-50/20' : 'bg-canvas-cream/30'}`}
                    >
                      <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                        <span className="font-mono text-xs font-medium">{alert.product_sku}</span>
                        {getAlertTag(alert.alert_type)}
                      </div>
                      <div className="p-4 flex flex-col gap-2 text-xs">
                        <div className="font-semibold text-ink">{alert.product_name}</div>
                        <p className="text-shade-50">Kho vật lý: <span className="font-semibold text-ink">{alert.warehouse_name}</span></p>
                        <p className="text-shade-50">Tồn khả dụng: <span className={`font-bold ${alert.current_qty === 0 ? 'text-danger-600' : 'text-orange-600'}`}>
                          {new Intl.NumberFormat('vi-VN').format(alert.current_qty)}
                        </span></p>
                        <p className="text-shade-50">Định mức tối thiểu: <span className="text-ink">{new Intl.NumberFormat('vi-VN').format(alert.reorder_point)}</span></p>
                        <p className="text-shade-50">
                          {new Date(alert.created_at).toLocaleString('vi-VN')}
                          {alert.is_resolved && alert.resolved_at && (
                            <span className="block text-[10px] text-success-600">
                              Bổ sung lúc: {new Date(alert.resolved_at).toLocaleString('vi-VN')}
                            </span>
                          )}
                        </p>
                        <div>
                          <Badge size="sm" type={alert.is_resolved ? 'success' : 'danger'} className={alert.is_resolved ? '' : 'animate-pulse'}>
                            <span className="inline-flex items-center gap-1">
                              {alert.is_resolved ? <CheckCircle className="w-3 h-3" /> : <AlertTriangle className="w-3 h-3" />}
                              {alert.is_resolved ? 'Đã bổ sung' : 'Cần bổ sung'}
                            </span>
                          </Badge>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </>
            )}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-hairline-light px-6 py-4">
                <span className="text-xs text-shade-50">
                  Trang {page + 1} / {totalPages}
                </span>
                <div className="flex gap-2">
                  <Button
                    variant="outline-light"
                    icon={ChevronLeft}
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                  />
                  <Button
                    variant="outline-light"
                    icon={ChevronRight}
                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                    disabled={page === totalPages - 1}
                  />
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default LowStockAlerts;
