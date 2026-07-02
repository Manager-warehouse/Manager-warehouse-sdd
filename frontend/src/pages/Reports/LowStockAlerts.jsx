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
      console.error(err);
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

        <div className="flex gap-2 flex-wrap">
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
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-2xl mx-auto my-12 text-center">
          <AlertTriangle className="w-12 h-12 text-red-600 mx-auto mb-4" />
          <h3 className="text-lg font-bold text-red-800">Lỗi Hệ Thống</h3>
          <p className="text-sm text-red-600 mt-2">{error}</p>
        </div>
      ) : (
        <>
          {/* Details Table */}
          <div className="card-premium flex flex-col gap-4 overflow-hidden">
            <div className="flex items-center justify-between border-b border-hairline-light pb-3">
              <h3 className="text-sm font-bold text-shade-70 uppercase tracking-wider">
                Danh sách cảnh báo ({totalElements} bản ghi)
              </h3>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-hairline-light bg-canvas-cream text-shade-60 font-semibold uppercase tracking-wider">
                    <th className="py-4 px-6">Kho vật lý</th>
                    <th className="py-4 px-6">Mã SKU</th>
                    <th className="py-4 px-6">Tên sản phẩm</th>
                    <th className="py-4 px-6 text-center">Loại cảnh báo</th>
                    <th className="py-4 px-6 text-right">Tồn khả dụng</th>
                    <th className="py-4 px-6 text-right">Định mức tối thiểu</th>
                    <th className="py-4 px-6 text-center">Trạng thái</th>
                    <th className="py-4 px-6">Thời gian cảnh báo</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light font-light text-shade-80">
                  {alerts.length === 0 ? (
                    <tr>
                      <td colSpan="8" className="py-12 text-center text-shade-50 font-light">
                        Tuyệt vời! Không có cảnh báo tồn kho nào cần xử lý.
                      </td>
                    </tr>
                  ) : (
                    alerts.map((alert) => (
                      <tr 
                        key={alert.id} 
                        className={`hover:bg-canvas-cream/50 transition-colors ${!alert.is_resolved ? 'bg-red-50/20' : ''}`}
                      >
                        <td className="py-3.5 px-6 font-semibold text-ink">{alert.warehouse_name}</td>
                        <td className="py-3.5 px-6 font-mono font-medium">{alert.product_sku}</td>
                        <td className="py-3.5 px-6 font-medium">{alert.product_name}</td>
                        <td className="py-3.5 px-6 text-center">{getAlertTag(alert.alert_type)}</td>
                        <td className={`py-3.5 px-6 text-right font-bold ${alert.current_qty === 0 ? 'text-red-600' : 'text-orange-600'}`}>
                          {new Intl.NumberFormat('vi-VN').format(alert.current_qty)}
                        </td>
                        <td className="py-3.5 px-6 text-right text-shade-60">
                          {new Intl.NumberFormat('vi-VN').format(alert.reorder_point)}
                        </td>
                        <td className="py-3.5 px-6 text-center">
                          {alert.is_resolved ? (
                            <span className="flex items-center justify-center gap-1 text-xs font-semibold uppercase tracking-wider whitespace-nowrap text-emerald-700 bg-emerald-50 border border-emerald-200 px-2.5 py-0.5 rounded-pill">
                              <CheckCircle className="w-3 h-3" />
                              <span>Đã bổ sung</span>
                            </span>
                          ) : (
                            <span className="flex items-center justify-center gap-1 text-xs font-semibold uppercase tracking-wider whitespace-nowrap text-red-700 bg-red-50 border border-red-200 px-2.5 py-0.5 rounded-pill animate-pulse">
                              <AlertTriangle className="w-3 h-3" />
                              <span>Cần bổ sung</span>
                            </span>
                          )}
                        </td>
                        <td className="py-3.5 px-6 text-shade-60 font-light">
                          {new Date(alert.created_at).toLocaleString('vi-VN')}
                          {alert.is_resolved && alert.resolved_at && (
                            <span className="block text-[10px] text-emerald-600">
                              Bổ sung lúc: {new Date(alert.resolved_at).toLocaleString('vi-VN')}
                            </span>
                          )}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex items-center justify-between border-t border-hairline-light pt-4 px-4 pb-2">
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
