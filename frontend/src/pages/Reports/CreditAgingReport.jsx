import React, { useState, useEffect, useCallback } from 'react';
import { financeService } from '../../services/finance.service';
import { useUiStore } from '../../stores/ui.store';
import { PieChart, ShieldAlert, AlertTriangle, CheckCircle2, Building2, TrendingDown } from 'lucide-react';

const CreditAgingReport = () => {
  const { addToast } = useUiStore();
  const [agingData, setAgingData] = useState([]);
  const [loading, setLoading] = useState(false);

  const loadAgingReport = useCallback(async () => {
    setLoading(true);
    try {
      const data = await financeService.getAgingReport();
      setAgingData(data || []);
    } catch (err) {
      console.error('Failed to load aging report:', err);
      addToast('Không thể tải Báo cáo tuổi nợ Đại lý', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    loadAgingReport();
  }, [loadAgingReport]);

  const totalCurrentBalance = agingData.reduce((acc, d) => acc + (d.current_balance || 0), 0);
  const totalInTerm = agingData.reduce((acc, d) => acc + (d.in_term_amount || 0), 0);
  const totalOverdue = totalCurrentBalance - totalInTerm;
  const highRiskCount = agingData.filter(d => d.risk_level === 'HIGH_RISK' || d.credit_status === 'CREDIT_HOLD').length;

  return (
    <div className="flex flex-col gap-6">
      <div>
        <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
          Báo cáo & Phân tích / Quản trị Công nợ
        </span>
        <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
          Báo cáo Phân kỳ Công nợ Đại lý (Aging Report)
        </h1>
        <p className="text-xs text-shade-50 font-light mt-1">
          Phân tích các khoản phải thu theo từng khoảng thời gian quá hạn (1-30, 31-60, 61-90, trên 90 ngày) và cảnh báo rủi ro nợ xấu.
        </p>
      </div>

      {/* SUMMARY STAT CARDS */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-canvas-light border border-hairline-light rounded-lg p-4 flex flex-col gap-1 shadow-sm">
          <span className="text-[10px] font-bold text-shade-50 uppercase tracking-wider">Tổng dư nợ đại lý</span>
          <span className="text-xl font-bold text-ink">{totalCurrentBalance.toLocaleString()}đ</span>
        </div>
        <div className="bg-canvas-light border border-hairline-light rounded-lg p-4 flex flex-col gap-1 shadow-sm">
          <span className="text-[10px] font-bold text-emerald-700 uppercase tracking-wider">Dư nợ trong hạn</span>
          <span className="text-xl font-bold text-emerald-600">{totalInTerm.toLocaleString()}đ</span>
        </div>
        <div className="bg-canvas-light border border-hairline-light rounded-lg p-4 flex flex-col gap-1 shadow-sm">
          <span className="text-[10px] font-bold text-red-600 uppercase tracking-wider">Tổng dư nợ quá hạn</span>
          <span className="text-xl font-bold text-red-600">{(totalOverdue > 0 ? totalOverdue : 0).toLocaleString()}đ</span>
        </div>
        <div className="bg-canvas-light border border-hairline-light rounded-lg p-4 flex flex-col gap-1 shadow-sm">
          <span className="text-[10px] font-bold text-amber-700 uppercase tracking-wider">Đại lý Rủi ro cao / Khóa nợ</span>
          <span className="text-xl font-bold text-amber-600">{highRiskCount} Đại lý</span>
        </div>
      </div>

      {/* MAIN TABLE */}
      <div className="bg-canvas-light border border-hairline-light rounded-lg shadow-level-3 overflow-hidden">
        <div className="p-4 bg-canvas-cream border-b border-hairline-light flex items-center justify-between">
          <span className="text-xs font-semibold text-shade-60 uppercase tracking-wider flex items-center gap-2">
            <PieChart className="w-4 h-4 text-ink" />
            Chi tiết Phân kỳ Tuổi nợ theo Đại lý
          </span>
          <span className="text-[10px] bg-shade-70 text-onPrimary px-2.5 py-0.5 rounded-pill font-bold">
            {agingData.length} Đại lý
          </span>
        </div>

        {loading ? (
          <div className="flex items-center justify-center py-20 text-shade-50">
            <svg className="animate-spin h-6 w-6 text-ink mr-2" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            <span>Đang tính toán phân kỳ công nợ...</span>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full border-collapse text-left text-xs">
              <thead>
                <tr className="bg-canvas-light border-b border-hairline-light text-shade-60 font-semibold uppercase tracking-wider">
                  <th className="p-4">Đại lý</th>
                  <th className="p-4 text-right">Hạn mức nợ</th>
                  <th className="p-4 text-right">Tổng dư nợ</th>
                  <th className="p-4 text-right text-emerald-700">Trong hạn</th>
                  <th className="p-4 text-right text-amber-700">1 - 30 ngày</th>
                  <th className="p-4 text-right text-amber-800">31 - 60 ngày</th>
                  <th className="p-4 text-right text-red-600">61 - 90 ngày</th>
                  <th className="p-4 text-right text-red-700 font-bold">&gt; 90 ngày</th>
                  <th className="p-4 text-center">Đánh giá rủi ro</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline-light">
                {agingData.length === 0 ? (
                  <tr>
                    <td colSpan="9" className="p-8 text-center text-shade-40 italic">
                      Chưa có dữ liệu phân kỳ công nợ đại lý.
                    </td>
                  </tr>
                ) : (
                  agingData.map((d) => (
                    <tr key={d.dealer_id} className="hover:bg-canvas-cream/50">
                      <td className="p-4 font-bold text-ink">
                        <div>{d.dealer_name}</div>
                        <div className="text-[10px] text-shade-50 font-normal">Mã: {d.dealer_code}</div>
                      </td>
                      <td className="p-4 text-right font-medium text-shade-70">
                        {(d.credit_limit || 0).toLocaleString()}đ
                      </td>
                      <td className="p-4 text-right font-bold text-ink">
                        {(d.current_balance || 0).toLocaleString()}đ
                      </td>
                      <td className="p-4 text-right font-semibold text-emerald-600">
                        {(d.in_term_amount || 0).toLocaleString()}đ
                      </td>
                      <td className="p-4 text-right font-medium text-amber-700">
                        {(d.overdue_1_to_30 || 0).toLocaleString()}đ
                      </td>
                      <td className="p-4 text-right font-medium text-amber-800">
                        {(d.overdue_31_to_60 || 0).toLocaleString()}đ
                      </td>
                      <td className="p-4 text-right font-medium text-red-600">
                        {(d.overdue_61_to_90 || 0).toLocaleString()}đ
                      </td>
                      <td className="p-4 text-right font-bold text-red-700">
                        {(d.overdue_over_90 || 0).toLocaleString()}đ
                      </td>
                      <td className="p-4 text-center">
                        <span
                          className={`px-2.5 py-0.5 rounded-pill text-[9px] font-bold uppercase inline-flex items-center gap-1 ${
                            d.credit_status === 'CREDIT_HOLD' || d.risk_level === 'HIGH_RISK'
                              ? 'bg-red-100 text-red-700 border border-red-200'
                              : 'bg-aloe-10 text-ink'
                          }`}
                        >
                          {d.credit_status === 'CREDIT_HOLD' || d.risk_level === 'HIGH_RISK' ? (
                            <>
                              <ShieldAlert className="w-3 h-3 text-red-600" />
                              RỦI RO CAO / KHÓA
                            </>
                          ) : (
                            <>
                              <CheckCircle2 className="w-3 h-3 text-ink" />
                              AN TOÀN
                            </>
                          )}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default CreditAgingReport;
