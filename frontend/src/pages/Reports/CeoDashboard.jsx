import React, { useEffect, useState } from 'react';
import reportService from '../../services/report.service';
import { Package, TrendingUp, ShieldAlert, CheckCircle, ArrowRight, DollarSign, Calendar, AlertCircle } from 'lucide-react';
import Badge from '../../components/common/Badge';

const CeoDashboard = () => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await reportService.getCeoDashboard();
      setData(res);
    } catch (err) {
      console.error(err);
      setError(err.response?.data?.message || 'Không thể tải dữ liệu dashboard quản trị.');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (val) => {
    if (val === undefined || val === null) return '0 VNĐ';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-canvas-night"></div>
        <span className="ml-3 text-sm text-shade-60">Đang tải dữ liệu báo cáo quản trị...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6 max-w-2xl mx-auto my-12 text-center">
        <AlertCircle className="w-12 h-12 text-red-600 mx-auto mb-4" />
        <h3 className="text-lg font-bold text-red-800">Lỗi Truy Cập Báo Cáo</h3>
        <p className="text-sm text-red-600 mt-2">{error}</p>
        <button onClick={fetchData} className="mt-4 px-4 py-2 bg-red-600 text-onPrimary rounded-pill text-xs font-semibold hover:bg-red-700 transition-colors">
          Thử lại
        </button>
      </div>
    );
  }

  const { kpis, top_debtors, as_of_time } = data;

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Báo cáo chiến lược
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            CEO Dashboard Quản Trị
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Số liệu cập nhật tự động đến: <span className="font-semibold text-ink">{new Date(as_of_time).toLocaleString('vi-VN')}</span>
          </p>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Card 1: Inventory Value */}
        <div className="card-featured-mint flex flex-col justify-between h-40">
          <div className="flex justify-between items-start">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider block text-ink/75">
                Tổng giá trị tồn kho
              </span>
              <span className="text-xl md:text-2xl font-display font-semibold block mt-2 text-ink">
                {formatCurrency(kpis.total_inventory_value)}
              </span>
            </div>
            <div className="p-2.5 rounded-full text-ink bg-canvas-light/50">
              <Package className="w-5 h-5" />
            </div>
          </div>
          <span className="text-[11px] font-light text-ink/60">
            Tính trên giá vốn gốc (cost basis) của 3 kho vật lý
          </span>
        </div>

        {/* Card 2: QC failure rate */}
        <div className="bg-orange-50/50 rounded-lg p-6 border border-orange-200 shadow-level-3 flex flex-col justify-between h-40">
          <div className="flex justify-between items-start">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider block text-orange-800/80">
                Tỷ lệ QC lỗi trong tháng
              </span>
              <span className="text-xl md:text-2xl font-display font-semibold block mt-2 text-orange-800">
                {(kpis.qc_failure_rate * 100).toFixed(2)}%
              </span>
            </div>
            <div className="p-2.5 rounded-full text-orange-700 bg-orange-100/50">
              <ShieldAlert className="w-5 h-5" />
            </div>
          </div>
          <span className="text-[11px] font-light text-orange-700/60">
            Tỷ lệ hàng hỏng/lỗi trên tổng sản phẩm kiểm QC đầu ra & đầu vào
          </span>
        </div>

        {/* Card 3: OTD rate */}
        <div className="bg-blue-50/50 rounded-lg p-6 border border-blue-200 shadow-level-3 flex flex-col justify-between h-40">
          <div className="flex justify-between items-start">
            <div>
              <span className="text-[10px] font-bold uppercase tracking-wider block text-blue-800/80">
                Tỷ lệ giao hàng đúng hạn (OTD)
              </span>
              <span className="text-xl md:text-2xl font-display font-semibold block mt-2 text-blue-800">
                {(kpis.on_time_delivery_rate * 100).toFixed(1)}%
              </span>
            </div>
            <div className="p-2.5 rounded-full text-blue-700 bg-blue-100/50">
              <CheckCircle className="w-5 h-5" />
            </div>
          </div>
          <span className="text-[11px] font-light text-blue-700/60">
            Đơn hàng giao tới đại lý đúng hạn cam kết trên hệ thống
          </span>
        </div>
      </div>

      {/* Main Content Dashboard */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* P&L Analysis */}
        <div className="card-premium flex flex-col gap-4">
          <div className="flex items-center justify-between border-b border-hairline-light pb-3">
            <h3 className="text-sm font-bold text-shade-70 uppercase tracking-wider">
              Báo cáo Lãi/Lỗ tóm tắt (Kỳ: {kpis.p_and_l.period})
            </h3>
            <span className="bg-canvas-cream border border-hairline-light text-shade-70 px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wider rounded-pill">
              VNĐ
            </span>
          </div>

          <div className="flex flex-col gap-5 my-2">
            {/* Doanh thu */}
            <div>
              <div className="flex justify-between items-center text-xs mb-1">
                <span className="font-medium text-shade-60">Doanh thu bán hàng</span>
                <span className="font-semibold text-ink">{formatCurrency(kpis.p_and_l.revenue)}</span>
              </div>
              <div className="w-full bg-canvas-cream h-2 rounded-full overflow-hidden">
                <div className="bg-emerald-500 h-full rounded-full" style={{ width: '100%' }}></div>
              </div>
            </div>

            {/* Giá vốn */}
            <div>
              <div className="flex justify-between items-center text-xs mb-1">
                <span className="font-medium text-shade-60">Giá vốn hàng bán (COGS)</span>
                <span className="font-semibold text-ink">{formatCurrency(kpis.p_and_l.cogs)}</span>
              </div>
              <div className="w-full bg-canvas-cream h-2 rounded-full overflow-hidden">
                <div 
                  className="bg-orange-500 h-full rounded-full" 
                  style={{ width: `${kpis.p_and_l.revenue > 0 ? (kpis.p_and_l.cogs / kpis.p_and_l.revenue * 100) : 0}%` }}
                ></div>
              </div>
            </div>

            {/* Chi phí vận hành */}
            <div>
              <div className="flex justify-between items-center text-xs mb-1">
                <span className="font-medium text-shade-60">Chi phí vận chuyển đội xe</span>
                <span className="font-semibold text-ink">{formatCurrency(kpis.p_and_l.operating_costs)}</span>
              </div>
              <div className="w-full bg-canvas-cream h-2 rounded-full overflow-hidden">
                <div 
                  className="bg-rose-500 h-full rounded-full" 
                  style={{ width: `${kpis.p_and_l.revenue > 0 ? (kpis.p_and_l.operating_costs / kpis.p_and_l.revenue * 100) : 0}%` }}
                ></div>
              </div>
            </div>

            {/* Lợi nhuận ròng */}
            <div className="mt-4 pt-4 border-t border-hairline-light flex justify-between items-center">
              <div>
                <span className="text-[10px] font-bold text-shade-50 uppercase block">Lợi nhuận thuần nội bộ</span>
                <span className={`text-lg font-bold block mt-1 ${kpis.p_and_l.net_profit >= 0 ? 'text-emerald-700' : 'text-red-700'}`}>
                  {formatCurrency(kpis.p_and_l.net_profit)}
                </span>
              </div>
              <div className={`px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wider rounded-pill ${kpis.p_and_l.net_profit >= 0 ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-red-50 text-red-800 border border-red-200'}`}>
                {kpis.p_and_l.revenue > 0 ? ((kpis.p_and_l.net_profit / kpis.p_and_l.revenue) * 100).toFixed(1) : 0}% Biên LN
              </div>
            </div>
          </div>
        </div>

        {/* Top debtors */}
        <div className="card-premium flex flex-col gap-4">
          <div className="flex items-center justify-between border-b border-hairline-light pb-3">
            <h3 className="text-sm font-bold text-shade-70 uppercase tracking-wider">
              Top 5 Đại lý nợ quá hạn cao nhất
            </h3>
            <span className="text-red-600 bg-red-50 border border-red-200 px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wider rounded-pill">
              Cảnh báo công nợ
            </span>
          </div>

          {top_debtors.length === 0 ? (
            <div className="py-12 text-center text-shade-50 text-xs font-light">
              Tuyệt vời! Không có đại lý nào có hóa đơn quá hạn nợ.
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              {top_debtors.map((debtor, index) => (
                <div key={index} className="flex justify-between items-center p-3 rounded-lg border border-hairline-light hover:bg-canvas-cream transition-colors duration-150">
                  <div>
                    <span className="text-xs font-bold text-ink block">{debtor.dealer_name}</span>
                    <span className="text-[10px] font-light text-shade-50 mt-0.5 block">
                      Số ngày quá hạn lớn nhất: <span className="text-red-600 font-semibold">{debtor.max_overdue_days} ngày</span>
                    </span>
                  </div>
                  <div className="text-right">
                    <span className="text-xs font-bold text-red-700 block">
                      {formatCurrency(debtor.overdue_amount)}
                    </span>
                    <Badge type="danger" className="text-[9px] px-1.5 py-0.5 mt-0.5 inline-block">
                      Quá hạn
                    </Badge>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default CeoDashboard;
