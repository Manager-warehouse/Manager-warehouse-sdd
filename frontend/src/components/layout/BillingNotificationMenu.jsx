import React, { useState, useEffect } from 'react';
import { FileText, Bell } from 'lucide-react';
import { getDb } from '../../services/outbound.service';

const KEYS = { BILLING_NOTIFICATIONS: 'wms_db_billing_notifications' };

export default function BillingNotificationMenu() {
  const [notifications, setNotifications] = useState([]);
  const [showDropdown, setShowDropdown] = useState(false);

  useEffect(() => {
    // Poll for demo purposes
    const interval = setInterval(() => {
      const dbStr = localStorage.getItem(KEYS.BILLING_NOTIFICATIONS);
      if (dbStr) {
        setNotifications(JSON.parse(dbStr));
      }
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  const pendingCount = notifications.filter(n => n.status === 'PENDING_BILLING').length;

  return (
    <div className="relative">
      <button 
        onClick={() => setShowDropdown(!showDropdown)}
        className="relative p-2 text-zinc-500 hover:text-zinc-700 bg-zinc-100 hover:bg-zinc-200 rounded-full transition-colors"
      >
        <Bell className="w-5 h-5" />
        {pendingCount > 0 && (
          <span className="absolute top-0 right-0 w-4 h-4 bg-red-500 text-white text-[10px] font-bold rounded-full flex items-center justify-center border-2 border-white">
            {pendingCount}
          </span>
        )}
      </button>

      {showDropdown && (
        <div className="absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-xl border border-zinc-200 overflow-hidden z-50">
          <div className="p-3 bg-zinc-50 border-b border-zinc-200 flex justify-between items-center">
            <h3 className="font-bold text-zinc-900 text-sm">Chờ xuất hóa đơn</h3>
            <span className="text-xs bg-brand-primary text-white px-2 py-0.5 rounded-full font-medium">{pendingCount}</span>
          </div>
          <div className="max-h-[300px] overflow-y-auto">
            {notifications.length === 0 ? (
              <p className="p-4 text-center text-sm text-zinc-500">Không có thông báo mới</p>
            ) : (
              notifications.map(n => (
                <div key={n.id} className="p-3 border-b border-zinc-100 hover:bg-zinc-50 cursor-pointer">
                  <div className="flex justify-between items-start mb-1">
                    <p className="text-sm font-bold text-zinc-900">{n.do_number}</p>
                    <span className="text-[10px] text-zinc-500">{new Date(n.created_at).toLocaleTimeString('vi-VN')}</span>
                  </div>
                  <p className="text-xs text-zinc-600 truncate">{n.dealer_name}</p>
                  <div className="mt-2 flex justify-end">
                    <button className="flex items-center gap-1 text-[11px] font-bold px-2 py-1 bg-emerald-50 text-emerald-600 border border-emerald-200 rounded hover:bg-emerald-100 transition-colors">
                      <FileText className="w-3 h-3" />
                      TẠO HÓA ĐƠN
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
