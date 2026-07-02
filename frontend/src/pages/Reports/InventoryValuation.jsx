import React, { useEffect, useState } from 'react';
import reportService from '../../services/report.service';
import { DollarSign, RefreshCw, Warehouse, FileSpreadsheet, AlertCircle, Loader2 } from 'lucide-react';
import { WAREHOUSES } from '../../utils/constants';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';

const InventoryValuation = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selectedWarehouse, setSelectedWarehouse] = useState('');

  useEffect(() => {
    fetchData();
  }, [selectedWarehouse]);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await reportService.getInventoryValuation(selectedWarehouse || null);
      setData(res);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Không có quyền truy cập hoặc lỗi khi tải báo cáo định giá.');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (val) => {
    if (val === undefined || val === null) return '0 VNĐ';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  // Lọc chỉ lấy các kho vật lý
  const physicalWarehouses = WAREHOUSES ? Object.values(WAREHOUSES).filter(w => w.type !== 'IN_TRANSIT') : [];

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Báo cáo tài chính kho
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Báo cáo Giá trị Tồn kho
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Định giá trị tồn kho thời gian thực dựa trên giá vốn kỳ (standard cost) đang có hiệu lực.
          </p>
        </div>

        <div className="flex gap-2 items-start">
          {/* Warehouse Filter */}
          <div className="w-48">
            <Input
              type="select"
              value={selectedWarehouse}
              onChange={(e) => setSelectedWarehouse(e.target.value)}
              options={[
                { value: '', label: 'Tất cả kho vật lý' },
                { value: '1', label: 'Kho Hải Phòng' },
                { value: '2', label: 'Kho Hà Nội' },
                { value: '3', label: 'Kho Hồ Chí Minh' },
              ]}
            />
          </div>

          <Button variant="outline-light" icon={RefreshCw} onClick={fetchData}>Làm mới</Button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center min-h-[300px]">
          <Loader2 className="w-8 h-8 animate-spin text-ink" />
          <span className="ml-3 text-sm text-shade-60">Đang tổng hợp dữ liệu tồn kho...</span>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-2xl mx-auto my-12 text-center">
          <AlertCircle className="w-12 h-12 text-red-600 mx-auto mb-4" />
          <h3 className="text-lg font-bold text-red-800">Lỗi Tải Báo Cáo</h3>
          <p className="text-sm text-red-600 mt-2">{error}</p>
        </div>
      ) : (
        <>
          {/* Summary Banner */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="card-premium p-6 flex items-center gap-4">
              <div className="p-3 bg-indigo-50 text-indigo-700 rounded-full">
                <Warehouse className="w-6 h-6" />
              </div>
              <div>
                <span className="text-[10px] font-semibold text-shade-60 uppercase block">Tổng số SKU/Lô hàng</span>
                <span className="text-xl font-semibold text-ink block mt-1">{data.summary.total_items} dòng</span>
              </div>
            </div>

            <div className="card-premium p-6 flex items-center gap-4">
              <div className="p-3 bg-emerald-50 text-emerald-700 rounded-full">
                <RefreshCw className="w-6 h-6" />
              </div>
              <div>
                <span className="text-[10px] font-semibold text-shade-60 uppercase block">Tổng sản lượng tồn kho</span>
                <span className="text-xl font-semibold text-ink block mt-1">
                  {new Intl.NumberFormat('vi-VN').format(data.summary.total_qty)} cái
                </span>
              </div>
            </div>

            <div className="bg-canvas-night text-onPrimary rounded-lg border border-hairline-dark p-6 shadow-level-3 hover:shadow-lg transition-all duration-200 flex items-center gap-4">
              <div className="p-3 bg-canvas-nightElevated text-onPrimary rounded-full">
                <DollarSign className="w-6 h-6 text-onPrimary" />
              </div>
              <div>
                <span className="text-[10px] font-semibold text-shade-30 uppercase block">Tổng giá trị định giá</span>
                <span className="text-xl font-semibold block mt-1 text-onPrimary">
                  {formatCurrency(data.summary.total_valuation)}
                </span>
              </div>
            </div>
          </div>

          {/* Details Table */}
          <div className="card-premium flex flex-col gap-4 overflow-hidden">
            <div className="flex items-center justify-between border-b border-hairline-light pb-3">
              <h3 className="text-sm font-semibold text-shade-60 uppercase tracking-wider">
                Bảng phân tích định giá chi tiết
              </h3>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-hairline-light bg-canvas-cream text-shade-60 font-semibold uppercase tracking-wider">
                    <th className="px-6 py-4">Kho vật lý</th>
                    <th className="px-6 py-4">Mã SKU</th>
                    <th className="px-6 py-4">Tên sản phẩm</th>
                    <th className="px-6 py-4">Số lô hàng</th>
                    <th className="px-6 py-4 text-right">Tồn thực tế</th>
                    <th className="px-6 py-4 text-right">Đơn giá vốn</th>
                    <th className="px-6 py-4 text-right">Tổng giá trị</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light font-light text-shade-60">
                  {data.records.length === 0 ? (
                    <tr>
                      <td colSpan="7" className="py-8 text-center text-shade-50">
                        Không có sản phẩm nào có số dư tồn kho khả dụng lớn hơn 0.
                      </td>
                    </tr>
                  ) : (
                    data.records.map((r, idx) => (
                      <tr key={idx} className="hover:bg-canvas-cream/50 transition-colors">
                        <td className="px-6 py-3 font-semibold text-ink">{r.warehouse_name}</td>
                        <td className="px-6 py-3 font-mono font-medium">{r.product_sku}</td>
                        <td className="px-6 py-3">{r.product_name}</td>
                        <td className="px-6 py-3 font-mono text-[11px] text-shade-60">{r.batch_number}</td>
                        <td className="px-6 py-3 text-right font-medium">
                          {new Intl.NumberFormat('vi-VN').format(r.total_qty)}
                        </td>
                        <td className="px-6 py-3 text-right text-shade-60">
                          {formatCurrency(r.unit_cost)}
                        </td>
                        <td className="px-6 py-3 text-right font-semibold text-ink">
                          {formatCurrency(r.valuation_amount)}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default InventoryValuation;
