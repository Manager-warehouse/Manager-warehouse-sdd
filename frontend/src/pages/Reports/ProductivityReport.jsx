import React, { useEffect, useState } from 'react';
import reportService from '../../services/report.service';
import { ClipboardList, Users, Truck, CheckSquare, Calendar, RefreshCw, FileSpreadsheet, AlertCircle } from 'lucide-react';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { useUiStore } from '../../stores/ui.store';

const ProductivityReport = () => {
  const { addToast } = useUiStore();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState('staff');

  // Filter states (mặc định lấy 14 ngày gần nhất)
  const [warehouseId, setWarehouseId] = useState('1'); // Kho Hải Phòng mặc định
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 14);
    return d.toISOString().slice(0, 10);
  });
  const [endDate, setEndDate] = useState(() => new Date().toISOString().slice(0, 10));

  useEffect(() => {
    fetchData();
  }, [warehouseId]);

  const fetchData = async () => {
    // Validate date range max 31 days
    const start = new Date(startDate);
    const end = new Date(endDate);
    const timeDiff = end.getTime() - start.getTime();
    const dayDiff = timeDiff / (1000 * 3600 * 24);

    if (dayDiff < 0) {
      setError('Ngày bắt đầu không được lớn hơn ngày kết thúc.');
      return;
    }
    if (dayDiff > 31) {
      setError('Dải ngày báo cáo năng suất tối đa là 31 ngày để bảo đảm hiệu năng.');
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const res = await reportService.getProductivityReport(warehouseId, startDate, endDate);
      setData(res);
    } catch (err) {
      setError(err.response?.data?.message || 'Không có quyền truy cập hoặc lỗi khi tải báo cáo năng suất.');
    } finally {
      setLoading(false);
    }
  };

  const handleExport = async () => {
    try {
      await reportService.exportProductivityExcel(warehouseId, startDate, endDate);
    } catch (err) {
      addToast('Lỗi xuất báo cáo Excel.', 'error');
    }
  };

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Báo cáo hiệu suất nội bộ
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Năng Suất Hoạt Động Nhân Sự
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Thống kê sản lượng bốc xếp, kiểm QC của thủ kho và số chuyến xe hoàn thành của tài xế.
          </p>
        </div>

        {/* Filters */}
        <div className="flex gap-2 flex-wrap lg:flex-nowrap items-center w-full lg:w-auto">
          {/* Warehouse */}
          <div className="w-48">
            <Input
              type="select"
              value={warehouseId}
              onChange={(e) => setWarehouseId(e.target.value)}
              options={[
                { value: '1', label: 'Kho Hải Phòng' },
                { value: '2', label: 'Kho Hà Nội' },
                { value: '3', label: 'Kho Hồ Chí Minh' },
              ]}
            />
          </div>

          {/* Date range */}
          <div className="flex items-center gap-1.5 bg-canvas-light border border-hairline-light rounded-md px-3 py-2.5 min-h-[44px]">
            <Calendar className="w-3.5 h-3.5 text-shade-50" />
            <input
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="bg-transparent border-none text-xs text-ink font-semibold outline-none w-28"
            />
            <span className="text-xs text-shade-40">-</span>
            <input
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              className="bg-transparent border-none text-xs text-ink font-semibold outline-none w-28"
            />
          </div>

          <Button variant="outline-light" icon={RefreshCw} onClick={fetchData}>Lọc</Button>

          <Button variant="primary" icon={FileSpreadsheet} onClick={handleExport}>Xuất Excel</Button>
        </div>
      </div>

      {error && (
        <div className="bg-danger-50 border border-danger-200 rounded-lg p-4 flex items-center gap-3 text-xs text-danger-700">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center min-h-[300px]">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-canvas-night"></div>
          <span className="ml-3 text-sm text-shade-60">Đang hạch toán sản lượng nhân viên...</span>
        </div>
      ) : data ? (
        <>
          {/* Tabs header */}
          <div className="border-b border-hairline-light flex gap-6">
            <button
              onClick={() => setActiveTab('staff')}
              className={`pb-3.5 text-xs uppercase tracking-wider font-semibold border-b-2 transition-colors ${
                activeTab === 'staff' ? 'border-canvas-night text-ink' : 'border-transparent text-shade-40 hover:text-shade-60'
              }`}
            >
              <span className="flex items-center gap-2">
                <Users className="w-4 h-4" />
                <span>Nhân viên kho bốc xếp ({data.staff_productivity?.length || 0})</span>
              </span>
            </button>

            <button
              onClick={() => setActiveTab('storekeeper')}
              className={`pb-3.5 text-xs uppercase tracking-wider font-semibold border-b-2 transition-colors ${
                activeTab === 'storekeeper' ? 'border-canvas-night text-ink' : 'border-transparent text-shade-40 hover:text-shade-60'
              }`}
            >
              <span className="flex items-center gap-2">
                <CheckSquare className="w-4 h-4" />
                <span>Thủ kho QC ({data.storekeeper_productivity?.length || 0})</span>
              </span>
            </button>

            <button
              onClick={() => setActiveTab('driver')}
              className={`pb-3.5 text-xs uppercase tracking-wider font-semibold border-b-2 transition-colors ${
                activeTab === 'driver' ? 'border-canvas-night text-ink' : 'border-transparent text-shade-40 hover:text-shade-60'
              }`}
            >
              <span className="flex items-center gap-2">
                <Truck className="w-4 h-4" />
                <span>Tài xế giao hàng ({data.driver_productivity?.length || 0})</span>
              </span>
            </button>
          </div>

          {/* Tabs Content */}
          <div className="bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
            {activeTab === 'staff' && (
              data.staff_productivity.length === 0 ? (
                <div className="px-6 py-8 text-center text-shade-50 text-xs">Không có dữ liệu bốc xếp trong dải ngày này.</div>
              ) : (
                <>
                  <div className="hidden md:block overflow-x-auto">
                    <table className="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr className="bg-canvas-cream border-b border-hairline-light">
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã nhân viên</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Tên nhân viên</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Vai trò</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Số lượt soạn hàng (Picking runs)</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Tổng sản lượng soạn (Qty)</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-hairline-light">
                        {data.staff_productivity.map((p, idx) => (
                          <tr key={idx} className="hover:bg-canvas-cream/50 transition-colors">
                            <td className="px-6 py-4 text-xs font-mono font-medium">{p.employee_code}</td>
                            <td className="px-6 py-4 text-xs font-semibold text-ink">{p.full_name}</td>
                            <td className="px-6 py-4 text-xs text-shade-50 text-[10px] uppercase font-bold">{p.role}</td>
                            <td className="px-6 py-4 text-xs text-right font-medium">{p.picking_runs_count} lượt</td>
                            <td className="px-6 py-4 text-xs text-right font-bold text-ink">
                              {new Intl.NumberFormat('vi-VN').format(p.total_picked_qty)} cái
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <div className="flex flex-col gap-3 p-4 md:hidden">
                    {data.staff_productivity.map((p, idx) => (
                      <div key={idx} className="rounded-lg border border-hairline-light bg-canvas-cream/30 overflow-hidden">
                        <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                          <span className="font-mono text-xs font-medium">{p.employee_code}</span>
                          <span className="text-[10px] uppercase font-bold text-shade-50">{p.role}</span>
                        </div>
                        <div className="p-4 flex flex-col gap-2 text-xs">
                          <div className="font-semibold text-ink">{p.full_name}</div>
                          <p className="text-shade-50">Số lượt soạn hàng: <span className="font-medium text-ink">{p.picking_runs_count} lượt</span></p>
                          <p className="text-shade-50">Tổng sản lượng soạn: <span className="font-bold text-ink">{new Intl.NumberFormat('vi-VN').format(p.total_picked_qty)} cái</span></p>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )
            )}

            {activeTab === 'storekeeper' && (
              data.storekeeper_productivity.length === 0 ? (
                <div className="px-6 py-8 text-center text-shade-50 text-xs">Không có dữ liệu QC trong dải ngày này.</div>
              ) : (
                <>
                  <div className="hidden md:block overflow-x-auto">
                    <table className="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr className="bg-canvas-cream border-b border-hairline-light">
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã thủ kho</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Tên thủ kho</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Vai trò</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Số picking plans lập</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Tổng số lượng QC checked</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-hairline-light">
                        {data.storekeeper_productivity.map((p, idx) => (
                          <tr key={idx} className="hover:bg-canvas-cream/50 transition-colors">
                            <td className="px-6 py-4 text-xs font-mono font-medium">{p.employee_code}</td>
                            <td className="px-6 py-4 text-xs font-semibold text-ink">{p.full_name}</td>
                            <td className="px-6 py-4 text-xs text-shade-50 text-[10px] uppercase font-bold">{p.role}</td>
                            <td className="px-6 py-4 text-xs text-right font-medium">{p.picking_plans_created} kế hoạch</td>
                            <td className="px-6 py-4 text-xs text-right font-bold text-ink">
                              {new Intl.NumberFormat('vi-VN').format(p.total_qc_checked_qty)} cái
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <div className="flex flex-col gap-3 p-4 md:hidden">
                    {data.storekeeper_productivity.map((p, idx) => (
                      <div key={idx} className="rounded-lg border border-hairline-light bg-canvas-cream/30 overflow-hidden">
                        <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                          <span className="font-mono text-xs font-medium">{p.employee_code}</span>
                          <span className="text-[10px] uppercase font-bold text-shade-50">{p.role}</span>
                        </div>
                        <div className="p-4 flex flex-col gap-2 text-xs">
                          <div className="font-semibold text-ink">{p.full_name}</div>
                          <p className="text-shade-50">Số picking plans lập: <span className="font-medium text-ink">{p.picking_plans_created} kế hoạch</span></p>
                          <p className="text-shade-50">Tổng số lượng QC checked: <span className="font-bold text-ink">{new Intl.NumberFormat('vi-VN').format(p.total_qc_checked_qty)} cái</span></p>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )
            )}

            {activeTab === 'driver' && (
              data.driver_productivity.length === 0 ? (
                <div className="px-6 py-8 text-center text-shade-50 text-xs">Không có dữ liệu giao vận trong dải ngày này.</div>
              ) : (
                <>
                  <div className="hidden md:block overflow-x-auto">
                    <table className="w-full text-left text-xs border-collapse">
                      <thead>
                        <tr className="bg-canvas-cream border-b border-hairline-light">
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã tài xế</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Tên tài xế</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Vai trò</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Số chuyến hoàn thành</th>
                          <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Số đơn giao thành công</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-hairline-light">
                        {data.driver_productivity.map((p, idx) => (
                          <tr key={idx} className="hover:bg-canvas-cream/50 transition-colors">
                            <td className="px-6 py-4 text-xs font-mono font-medium">{p.employee_code}</td>
                            <td className="px-6 py-4 text-xs font-semibold text-ink">{p.full_name}</td>
                            <td className="px-6 py-4 text-xs text-shade-50 text-[10px] uppercase font-bold">{p.role}</td>
                            <td className="px-6 py-4 text-xs text-right font-medium">{p.trips_completed} chuyến</td>
                            <td className="px-6 py-4 text-xs text-right font-bold text-ink">{p.successful_deliveries} đơn</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>

                  <div className="flex flex-col gap-3 p-4 md:hidden">
                    {data.driver_productivity.map((p, idx) => (
                      <div key={idx} className="rounded-lg border border-hairline-light bg-canvas-cream/30 overflow-hidden">
                        <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                          <span className="font-mono text-xs font-medium">{p.employee_code}</span>
                          <span className="text-[10px] uppercase font-bold text-shade-50">{p.role}</span>
                        </div>
                        <div className="p-4 flex flex-col gap-2 text-xs">
                          <div className="font-semibold text-ink">{p.full_name}</div>
                          <p className="text-shade-50">Số chuyến hoàn thành: <span className="font-medium text-ink">{p.trips_completed} chuyến</span></p>
                          <p className="text-shade-50">Số đơn giao thành công: <span className="font-bold text-ink">{p.successful_deliveries} đơn</span></p>
                        </div>
                      </div>
                    ))}
                  </div>
                </>
              )
            )}
          </div>
        </>
      ) : null}
    </div>
  );
};

export default ProductivityReport;
