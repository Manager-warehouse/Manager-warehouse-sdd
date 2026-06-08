import React from 'react';
import { useAuthStore } from '../stores/auth.store';
import Badge from '../components/common/Badge';
import { WAREHOUSES } from '../utils/constants';
import { Package, TrendingUp, AlertCircle, RefreshCw } from 'lucide-react';

const Dashboard = () => {
  const { user, activeWarehouse } = useAuthStore();

  const mockKpis = [
    {
      title: 'Tồn kho khả dụng',
      value: '4,520 sản phẩm',
      desc: 'Kho đang vận hành ổn định',
      icon: Package,
      type: 'highlight' // aloe mint green card
    },
    {
      title: 'Giao dịch trong ngày',
      value: '38 phiếu',
      desc: '12 nhập kho & 26 xuất kho',
      icon: TrendingUp,
      type: 'premium' // black card
    },
    {
      title: 'Cảnh báo tồn kho thấp',
      value: '2 sản phẩm',
      desc: 'SKU đang dưới hạn mức tối thiểu',
      icon: AlertCircle,
      type: 'danger'
    }
  ];

  return (
    <div className="flex flex-col gap-6">
      {/* Welcome Banner */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            WMS Phúc Anh
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Xin chào, {user?.fullName || 'Người dùng'}
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Hôm nay bạn đang làm việc tại <span className="font-semibold text-ink">{activeWarehouse?.name || 'Chưa chọn kho'}</span>.
          </p>
        </div>

        <div>
          <Badge type="highlight" className="text-xs py-1 px-3">
            {activeWarehouse?.code || 'HP-01'}
          </Badge>
        </div>
      </div>

      {/* KPI Cards section */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {mockKpis.map((kpi, idx) => {
          const isHighlight = kpi.type === 'highlight';
          const isPremium = kpi.type === 'premium';
          const isDanger = kpi.type === 'danger';

          let cardStyle = 'card-premium';
          let titleStyle = 'text-shade-60';
          let valStyle = 'text-ink';
          let descStyle = 'text-shade-50';
          let iconStyle = 'text-shade-70 bg-canvas-cream';

          if (isHighlight) {
            cardStyle = 'card-featured-mint';
            titleStyle = 'text-ink/75';
            valStyle = 'text-ink';
            descStyle = 'text-ink/60';
            iconStyle = 'text-ink bg-canvas-light/50';
          } else if (isPremium) {
            cardStyle = 'bg-canvas-night text-onPrimary rounded-lg p-6 hover:shadow-lg transition-all duration-200 border border-hairline-dark';
            titleStyle = 'text-shade-40';
            valStyle = 'text-onPrimary';
            descStyle = 'text-shade-40';
            iconStyle = 'text-onPrimary bg-canvas-nightElevated';
          } else if (isDanger) {
            cardStyle = 'bg-red-50/50 rounded-lg p-6 border border-red-150 shadow-level-3 hover:shadow-lg transition-all duration-200';
            titleStyle = 'text-red-700/80';
            valStyle = 'text-red-700';
            descStyle = 'text-red-600/60';
            iconStyle = 'text-red-700 bg-red-100/50';
          }

          return (
            <div key={idx} className={`${cardStyle} flex flex-col justify-between h-40`}>
              <div className="flex justify-between items-start">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider block">
                    {kpi.title}
                  </span>
                  <span className="text-xl md:text-2xl font-display font-semibold block mt-2">
                    {kpi.value}
                  </span>
                </div>
                <div className={`p-2.5 rounded-full ${iconStyle}`}>
                  <kpi.icon className="w-5 h-5" />
                </div>
              </div>
              <span className={`text-[11px] font-light ${descStyle}`}>
                {kpi.desc}
              </span>
            </div>
          );
        })}
      </div>

      {/* Quick Access panel / Modules */}
      <div className="card-premium flex flex-col gap-4">
        <div className="flex items-center justify-between border-b border-hairline-light pb-3">
          <h3 className="text-sm font-bold text-shade-70 uppercase tracking-wider">
            Lịch sử vận hành gần đây
          </h3>
          <button className="text-xs text-shade-60 hover:text-ink flex items-center gap-1 font-semibold transition-colors">
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Làm mới</span>
          </button>
        </div>

        <div className="py-8 text-center text-shade-50 text-xs font-light">
          Chưa có hoạt động giao dịch nào được thực hiện trong kho làm việc này ngày hôm nay.
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
