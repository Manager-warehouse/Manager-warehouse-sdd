import React, { useEffect, useState } from 'react';
import reportService from '../../services/report.service';
import { DollarSign, RefreshCw, Warehouse, FileSpreadsheet, AlertCircle } from 'lucide-react';
import { WAREHOUSES } from '../../utils/constants';

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

        <div className="flex gap-2">
          {/* Warehouse Filter */}
          <select
            value={selectedWarehouse}
            onChange={(e) => setSelectedWarehouse(e.target.value)}
            className="input-select text-xs font-semibold py-1.5 px-3 border border-hairline-light rounded bg-canvas-light text-ink"
          >
            <option value="">Tất cả kho vật lý</option>
            {/* Vì dữ liệu seed có HP (1), HN (2), HCM (3) */}
            <option value="1">Kho Hải Phòng</option>
            <option value="2">Kho Hà Nội</option>
            <option value="3">Kho Hồ Chí Minh</option>
          </select>

          <button onClick={fetchData} className="btn-secondary flex items-center gap-1 text-xs py-1.5 px-3">
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center min-h-[300px]">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-canvas-night"></div>
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
                <span className="text-[10px] font-bold text-shade-50 uppercase block">Tổng số SKU/Lô hàng</span>
                <span className="text-xl font-bold text-ink block mt-1">{data.summary.total_items} dòng</span>
              </div>
            </div>

            <div className="card-premium p-6 flex items-center gap-4">
              <div className="p-3 bg-emerald-50 text-emerald-700 rounded-full">
                <RefreshCw className="w-6 h-6" />
              </div>
              <div>
                <span className="text-[10px] font-bold text-shade-50 uppercase block">Tổng sản lượng tồn kho</span>
                <span className="text-xl font-bold text-ink block mt-1">
                  {new Intl.NumberFormat('vi-VN').format(data.summary.total_qty)} cái
                </span>
              </div>
            </div>

            <div className="card-premium p-6 flex items-center gap-4 bg-canvas-night text-onPrimary">
              <div className="p-3 bg-canvas-nightElevated text-onPrimary rounded-full">
                <DollarSign className="w-6 h-6 text-aloe-10" />
              </div>
              <div>
                <span className="text-[10px] font-bold text-shade-40 uppercase block">Tổng giá trị định giá</span>
                <span className="text-xl font-bold block mt-1 text-onPrimary">
                  {formatCurrency(data.summary.total_valuation)}
                </span>
              </div>
            </div>
          </div>

          {/* Details Table */}
          <div className="card-premium flex flex-col gap-4 overflow-hidden">
            <div className="flex items-center justify-between border-b border-hairline-light pb-3">
              <h3 className="text-sm font-bold text-shade-70 uppercase tracking-wider">
                Bảng phân tích định giá chi tiết
              </h3>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="border-b border-hairline-light bg-canvas-cream text-shade-60 font-semibold uppercase tracking-wider">
                    <th className="py-3 px-4">Kho vật lý</th>
                    <th className="py-3 px-4">Mã SKU</th>
                    <th className="py-3 px-4">Tên sản phẩm</th>
                    <th className="py-3 px-4">Số lô hàng</th>
                    <th className="py-3 px-4 text-right">Tồn thực tế</th>
                    <th className="py-3 px-4 text-right">Đơn giá vốn</th>
                    <th className="py-3 px-4 text-right">Tổng giá trị</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-hairline-light font-light text-shade-80">
                  {data.records.length === 0 ? (
                    <tr>
                      <td colSpan="7" className="py-8 text-center text-shade-50">
                        Không có sản phẩm nào có số dư tồn kho khả dụng lớn hơn 0.
                      </td>
                    </tr>
                  ) : (
                    data.records.map((r, idx) => (
                      <tr key={idx} className="hover:bg-canvas-cream/50 transition-colors">
                        <td className="py-3.5 px-4 font-semibold text-ink">{r.warehouse_name}</td>
                        <td className="py-3.5 px-4 font-mono font-medium">{r.product_sku}</td>
                        <td className="py-3.5 px-4">{r.product_name}</td>
                        <td className="py-3.5 px-4 font-mono text-[11px] text-shade-60">{r.batch_number}</td>
                        <td className="py-3.5 px-4 text-right font-medium">
                          {new Intl.NumberFormat('vi-VN').format(r.total_qty)}
                        </td>
                        <td className="py-3.5 px-4 text-right text-shade-60">
                          {formatCurrency(r.unit_cost)}
                        </td>
                        <td className="py-3.5 px-4 text-right font-semibold text-ink">
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
