import React from 'react';
import Table from '../../components/common/Table';
import Badge from '../../components/common/Badge';
import { ROLES, ROLE_LABELS, WAREHOUSES } from '../../utils/constants';
import { Edit, ToggleLeft, ToggleRight } from 'lucide-react';

const UserTable = ({ users, loading, onEdit, onToggleStatus }) => {
  return (
    <Table
      headers={['Mã NV / Họ tên', 'Tài khoản / SĐT', 'Vai trò (Role)', 'Kho phụ trách', 'Trạng thái', 'Thao tác']}
      data={users}
      loading={loading}
      renderRow={(user) => {
        const assignedWHNames = WAREHOUSES.filter((w) => user.warehouses?.includes(w.id))
          .map((w) => w.code)
          .join(', ');

        return (
          <tr key={user.id} className="hover:bg-canvas-cream/50 transition-colors">
            <td className="px-6 py-4">
              <div className="text-xs font-mono font-bold text-shade-60 mb-0.5">{user.code || 'NV-000'}</div>
              <div className="text-sm font-semibold text-ink">{user.fullName}</div>
            </td>
            <td className="px-6 py-4 text-xs text-shade-60">
              <div>{user.email || 'Không có email'}</div>
              <div className="mt-0.5">{user.phone || 'Chưa có SĐT'}</div>
            </td>
            <td className="px-6 py-4">
              {user.role && (
                <span className="text-[10px] font-bold bg-canvas-cream text-ink border border-hairline-light px-2.5 py-1 rounded-pill whitespace-nowrap uppercase tracking-wider">
                  {ROLE_LABELS[user.role] || user.role}
                </span>
              )}
            </td>
            <td className="px-6 py-4 text-xs font-medium text-shade-70 font-mono">
              {user.role === ROLES.ADMIN ? (
                <span className="italic text-shade-40">Tất cả kho (Admin)</span>
              ) : user.role === ROLES.CEO ? (
                <span className="italic text-shade-40">Tất cả kho (CEO)</span>
              ) : assignedWHNames ? (
                assignedWHNames
              ) : (
                <span className="text-red-500 font-semibold italic">Chưa gán kho</span>
              )}
            </td>
            <td className="px-6 py-4">
              <Badge type={user.isActive ? 'success' : 'neutral'}>
                {user.isActive ? 'Hoạt động' : 'Khóa'}
              </Badge>
            </td>
            <td className="px-6 py-4">
              <div className="flex justify-end items-center gap-3 whitespace-nowrap">
                <button
                  onClick={() => onEdit(user)}
                  className="p-1 text-shade-60 hover:text-ink hover:bg-canvas-cream rounded-full transition-colors shrink-0"
                  title="Chỉnh sửa tài khoản"
                >
                  <Edit className="w-4 h-4" />
                </button>
                <button
                  onClick={() => onToggleStatus(user)}
                  className={`p-1 rounded-full transition-colors shrink-0 ${user.isActive ? 'text-emerald-600 hover:bg-canvas-cream' : 'text-shade-40 hover:bg-canvas-cream'}`}
                  title={user.isActive ? 'Khóa tài khoản' : 'Kích hoạt tài khoản'}
                >
                  {user.isActive ? (
                    <ToggleRight className="w-5 h-5" />
                  ) : (
                    <ToggleLeft className="w-5 h-5" />
                  )}
                </button>
              </div>
            </td>
          </tr>
        );
      }}
      renderCard={(user) => {
        const assignedWHNames = WAREHOUSES.filter((w) => user.warehouses?.includes(w.id))
          .map((w) => w.code)
          .join(', ');

        return (
          <div key={user.id} className="rounded-lg border border-hairline-light bg-canvas-cream/30 overflow-hidden">
            <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
              <span className="font-mono text-[11px] font-bold text-shade-60">{user.code || 'NV-000'}</span>
              <Badge type={user.isActive ? 'success' : 'neutral'}>
                {user.isActive ? 'Hoạt động' : 'Khóa'}
              </Badge>
            </div>
            <div className="p-4 flex flex-col gap-2 text-xs">
              <div className="font-semibold text-ink text-sm">{user.fullName}</div>
              <p className="text-shade-50">{user.email || 'Không có email'}</p>
              <p className="text-shade-50">{user.phone || 'Chưa có SĐT'}</p>
              {user.role && (
                <span className="self-start text-[10px] font-bold bg-canvas-light text-ink border border-hairline-light px-2.5 py-1 rounded-pill whitespace-nowrap uppercase tracking-wider">
                  {ROLE_LABELS[user.role] || user.role}
                </span>
              )}
              <p className="text-shade-50 font-mono">
                {user.role === ROLES.ADMIN ? (
                  <span className="italic text-shade-40 font-sans">Tất cả kho (Admin)</span>
                ) : user.role === ROLES.CEO ? (
                  <span className="italic text-shade-40 font-sans">Tất cả kho (CEO)</span>
                ) : assignedWHNames ? (
                  assignedWHNames
                ) : (
                  <span className="text-red-500 font-semibold italic font-sans">Chưa gán kho</span>
                )}
              </p>
            </div>
            <div className="p-4 border-t border-hairline-light flex justify-end items-center gap-3">
              <button
                onClick={() => onEdit(user)}
                className="p-1.5 text-shade-60 hover:text-ink hover:bg-canvas-cream rounded-full transition-colors shrink-0"
                title="Chỉnh sửa tài khoản"
              >
                <Edit className="w-4 h-4" />
              </button>
              <button
                onClick={() => onToggleStatus(user)}
                className={`p-1.5 rounded-full transition-colors shrink-0 ${user.isActive ? 'text-emerald-600 hover:bg-canvas-cream' : 'text-shade-40 hover:bg-canvas-cream'}`}
                title={user.isActive ? 'Khóa tài khoản' : 'Kích hoạt tài khoản'}
              >
                {user.isActive ? (
                  <ToggleRight className="w-5 h-5" />
                ) : (
                  <ToggleLeft className="w-5 h-5" />
                )}
              </button>
            </div>
          </div>
        );
      }}
    />
  );
};

export default UserTable;
