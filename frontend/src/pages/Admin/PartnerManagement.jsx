import React, { useEffect, useState } from 'react';
import { useAuthStore } from '../../stores/auth.store';
import { useUiStore } from '../../stores/ui.store';
import { masterDataService } from '../../services/masterData.service';
import { ROLES } from '../../utils/constants';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import Modal from '../../components/common/Modal';
import Badge from '../../components/common/Badge';
import { Plus, Search, Edit, ToggleLeft, ToggleRight, AlertCircle, Loader2, Handshake, Users, ShieldAlert } from 'lucide-react';

const PartnerManagement = () => {
  const { hasRole } = useAuthStore();
  const { addToast } = useUiStore();

  const [activeTab, setActiveTab] = useState('DEALERS'); // DEALERS or SUPPLIERS
  const [dealers, setDealers] = useState([]);
  const [suppliers, setSuppliers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  // Modals
  const [isDealerModalOpen, setIsDealerModalOpen] = useState(false);
  const [dealerModalType, setDealerModalType] = useState('ADD'); // ADD or EDIT
  const [selectedDealer, setSelectedDealer] = useState(null);
  const [dlSubmitting, setDlSubmitting] = useState(false);
  const [dlFormErrors, setDlFormErrors] = useState({});

  // Dealer Form Fields
  const [dlCode, setDlCode] = useState('');
  const [dlName, setDlName] = useState('');
  const [dlPhone, setDlPhone] = useState('');
  const [dlEmail, setDlEmail] = useState('');
  const [dlAddress, setDlAddress] = useState('');
  const [dlRegion, setDlRegion] = useState('');
  const [dlBankAccountNumber, setDlBankAccountNumber] = useState('');
  const [dlBankName, setDlBankName] = useState('');
  const [dlPaymentTerms, setDlPaymentTerms] = useState('30');
  const [dlCreditLimit, setDlCreditLimit] = useState('0');
  const [dlOriginalCreditLimit, setDlOriginalCreditLimit] = useState('0');
  const [dlOriginalPaymentTerms, setDlOriginalPaymentTerms] = useState('30');
  const [dlCreditStatus, setDlCreditStatus] = useState('ACTIVE');
  const [dlOriginalCreditStatus, setDlOriginalCreditStatus] = useState('ACTIVE');

  // Supplier Modal States
  const [isSupplierModalOpen, setIsSupplierModalOpen] = useState(false);
  const [supplierModalType, setSupplierModalType] = useState('ADD'); // ADD or EDIT
  const [selectedSupplier, setSelectedSupplier] = useState(null);
  const [splSubmitting, setSplSubmitting] = useState(false);
  const [splFormErrors, setSplFormErrors] = useState({});

  // Supplier Form Fields
  const [splCode, setSplCode] = useState('');
  const [splCompanyName, setSplCompanyName] = useState('');
  const [splTaxCode, setSplTaxCode] = useState('');
  const [splPhone, setSplPhone] = useState('');
  const [splContactPerson, setSplContactPerson] = useState('');
  const [splAddress, setSplAddress] = useState('');

  useEffect(() => {
    fetchData();
  }, [activeTab]);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (activeTab === 'DEALERS') {
        const data = await masterDataService.getDealers();
        setDealers(data);
      } else {
        const data = await masterDataService.getSuppliers();
        setSuppliers(data);
      }
    } catch (e) {
      addToast('Lỗi tải danh mục đối tác', 'error');
    } finally {
      setLoading(false);
    }
  };

  // --- DEALER ACTIONS ---
  const handleOpenAddDealer = () => {
    setDealerModalType('ADD');
    setSelectedDealer(null);
    setDlCode('');
    setDlName('');
    setDlPhone('');
    setDlEmail('');
    setDlAddress('');
    setDlRegion('');
    setDlBankAccountNumber('');
    setDlBankName('');
    setDlPaymentTerms('30');
    setDlCreditLimit('50000000');
    setDlOriginalPaymentTerms('30');
    setDlOriginalCreditLimit('50000000');
    setDlFormErrors({});
    setIsDealerModalOpen(true);
  };

  const handleOpenEditDealer = (dealer) => {
    setDealerModalType('EDIT');
    setSelectedDealer(dealer);
    setDlCode(dealer.code);
    setDlName(dealer.name);
    setDlPhone(dealer.phone || '');
    setDlEmail(dealer.email || '');
    setDlAddress(dealer.default_delivery_address || '');
    setDlRegion(dealer.region || '');
    setDlBankAccountNumber(dealer.bank_account_number || '');
    setDlBankName(dealer.bank_name || '');
    setDlPaymentTerms(String(dealer.payment_term_days || 30));
    setDlCreditLimit(String(dealer.credit_limit || 0));
    setDlOriginalPaymentTerms(String(dealer.payment_term_days || 30));
    setDlOriginalCreditLimit(String(dealer.credit_limit || 0));
    setDlCreditStatus(dealer.credit_status || 'ACTIVE');
    setDlOriginalCreditStatus(dealer.credit_status || 'ACTIVE');
    setDlFormErrors({});
    setIsDealerModalOpen(true);
  };

  const validateDlForm = () => {
    const errors = {};
    const canEditCredit = hasRole(ROLES.ACCOUNTANT_MANAGER);
    if (dealerModalType === 'ADD') {
      if (!dlCode.trim()) errors.code = 'Mã đại lý bắt buộc';
    }
    if (!dlName.trim()) errors.name = 'Tên đại lý bắt buộc';
    if (dlEmail.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(dlEmail.trim())) {
      errors.email = 'Email không hợp lệ';
    }
    if (dealerModalType === 'ADD' || canEditCredit) {
      if (Number(dlPaymentTerms) < 0) errors.payment_terms = 'Kỳ hạn thanh toán không hợp lệ';
      if (Number(dlCreditLimit) < 0) errors.credit_limit = 'Hạn mức tín dụng không được âm';
    }

    setDlFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleDlSubmit = async (e) => {
    e.preventDefault();
    if (!validateDlForm()) return;

    const canEditCredit = hasRole(ROLES.ACCOUNTANT_MANAGER);
    const creditChanged =
      dlCreditLimit !== dlOriginalCreditLimit ||
      dlPaymentTerms !== dlOriginalPaymentTerms;
    const creditStatusChanged =
      dealerModalType === 'EDIT' && canEditCredit && dlCreditStatus !== dlOriginalCreditStatus;

    setDlSubmitting(true);
    try {
      if (dealerModalType === 'ADD') {
        const dlData = {
          code: dlCode.trim(),
          name: dlName.trim(),
          phone: dlPhone.trim(),
          email: dlEmail.trim(),
          default_delivery_address: dlAddress.trim(),
          region: dlRegion.trim(),
          bank_account_number: dlBankAccountNumber.trim(),
          bank_name: dlBankName.trim(),
          payment_term_days: Number(dlPaymentTerms),
          credit_limit: parseFloat(dlCreditLimit),
        };
        await masterDataService.createDealer(dlData);
        addToast(`Tạo mới đại lý ${dlName} thành công`, 'success');
      } else {
        // Profile update (all roles)
        const dlData = {
          name: dlName.trim(),
          phone: dlPhone.trim(),
          email: dlEmail.trim(),
          default_delivery_address: dlAddress.trim(),
          region: dlRegion.trim(),
          bank_account_number: dlBankAccountNumber.trim(),
          bank_name: dlBankName.trim(),
        };
        await masterDataService.updateDealer(selectedDealer.id, dlData);

        // Credit limit update (ACCOUNTANT_MANAGER only, if changed)
        if (canEditCredit && creditChanged) {
          const limitData = {
            credit_limit: parseFloat(dlCreditLimit),
            payment_term_days: Number(dlPaymentTerms),
          };
          await masterDataService.updateDealerCreditLimit(selectedDealer.id, limitData);
        }

        // Credit status manual override (ACCOUNTANT_MANAGER only, if changed). Kept in its own
        // try/catch so the backend's specific unlock-rule rejection (balance still above the
        // buffer, or an invoice still overdue) reaches the manager instead of being swallowed
        // by the generic save-failure toast below.
        if (creditStatusChanged) {
          try {
            await masterDataService.updateDealerCreditStatus(selectedDealer.id, dlCreditStatus);
          } catch (statusErr) {
            addToast(statusErr.message || 'Không thể đổi trạng thái tín dụng', 'error');
            setIsDealerModalOpen(false);
            fetchData();
            return;
          }
        }

        addToast(`Cập nhật thông tin đại lý ${dlName} thành công`, 'success');
      }
      setIsDealerModalOpen(false);
      fetchData();
    } catch (err) {
      if (err.message === 'DUPLICATE_DEALER_CODE') {
        setDlFormErrors({ code: 'Mã đại lý này đã được sử dụng' });
      } else {
        addToast('Lỗi lưu trữ thông tin Đại lý', 'error');
      }
    } finally {
      setDlSubmitting(false);
    }
  };

  const handleToggleDlStatus = async (dealer) => {
    if (!hasRole(ROLES.ACCOUNTANT) && !hasRole(ROLES.ACCOUNTANT_MANAGER)) {
      addToast('Chỉ Kế toán viên hoặc Kế toán trưởng mới có quyền khóa/kích hoạt đối tác', 'warning');
      return;
    }
    try {
      const updated = await masterDataService.toggleDealerStatus(dealer.id, !dealer.is_active, dealer.name);
      addToast(`${updated.is_active ? 'Kích hoạt' : 'Khóa'} đại lý ${updated.name} thành công`, 'success');
      fetchData();
    } catch (e) {
      addToast('Lỗi đổi trạng thái đại lý', 'error');
    }
  };

  // --- SUPPLIER ACTIONS ---
  const handleOpenAddSupplier = () => {
    setSupplierModalType('ADD');
    setSelectedSupplier(null);
    setSplCode('');
    setSplCompanyName('');
    setSplTaxCode('');
    setSplPhone('');
    setSplContactPerson('');
    setSplAddress('');
    setSplFormErrors({});
    setIsSupplierModalOpen(true);
  };

  const handleOpenEditSupplier = (supplier) => {
    setSupplierModalType('EDIT');
    setSelectedSupplier(supplier);
    setSplCode(supplier.code);
    setSplCompanyName(supplier.company_name);
    setSplTaxCode(supplier.tax_code || '');
    setSplPhone(supplier.phone || '');
    setSplContactPerson(supplier.contact_person || '');
    setSplAddress(supplier.address || '');
    setSplFormErrors({});
    setIsSupplierModalOpen(true);
  };

  const validateSplForm = () => {
    const errors = {};
    if (supplierModalType === 'ADD') {
      if (!splCode.trim()) errors.code = 'Mã NCC bắt buộc';
    }
    if (!splCompanyName.trim()) errors.company_name = 'Tên công ty bắt buộc';
    setSplFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSplSubmit = async (e) => {
    e.preventDefault();
    if (!validateSplForm()) return;

    setSplSubmitting(true);
    const splData = {
      code: splCode.trim(),
      company_name: splCompanyName.trim(),
      tax_code: splTaxCode.trim(),
      phone: splPhone.trim(),
      contact_person: splContactPerson.trim(),
      address: splAddress.trim(),
    };

    try {
      if (supplierModalType === 'ADD') {
        await masterDataService.createSupplier(splData);
        addToast(`Tạo mới nhà cung cấp ${splCompanyName} thành công`, 'success');
      } else {
        await masterDataService.updateSupplier(selectedSupplier.id, splData);
        addToast(`Cập nhật thông tin nhà cung cấp ${splCompanyName} thành công`, 'success');
      }
      setIsSupplierModalOpen(false);
      fetchData();
    } catch (err) {
      if (err.message === 'DUPLICATE_SUPPLIER_CODE') {
        setSplFormErrors({ code: 'Mã nhà cung cấp này đã tồn tại' });
      } else {
        addToast('Lỗi lưu trữ thông tin Nhà cung cấp', 'error');
      }
    } finally {
      setSplSubmitting(false);
    }
  };

  const handleToggleSplStatus = async (supplier) => {
    if (!hasRole(ROLES.ACCOUNTANT) && !hasRole(ROLES.ACCOUNTANT_MANAGER)) {
      addToast('Chỉ Kế toán viên hoặc Kế toán trưởng mới có quyền khóa/kích hoạt nhà cung cấp', 'warning');
      return;
    }
    try {
      const updated = await masterDataService.toggleSupplierStatus(supplier.id, !supplier.is_active, supplier.company_name);
      addToast(`${updated.is_active ? 'Kích hoạt' : 'Khóa'} nhà cung cấp ${updated.company_name} thành công`, 'success');
      fetchData();
    } catch (e) {
      addToast('Lỗi đổi trạng thái nhà cung cấp', 'error');
    }
  };

  // --- FILTERS ---
  const filteredDealers = dealers.filter((d) =>
    (d.code || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (d.name || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredSuppliers = suppliers.filter((s) =>
    (s.code || '').toLowerCase().includes(searchTerm.toLowerCase()) ||
    (s.company_name || '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <span className="text-[10px] font-bold text-shade-60 uppercase tracking-widest block mb-1">
            Hệ thống / Admin
          </span>
          <h1 className="text-2xl md:text-3xl font-display font-semibold tracking-tight">
            Quản lý đối tác thương mại
          </h1>
          <p className="text-xs text-shade-50 font-light mt-1">
            Quản trị danh mục khách hàng Đại lý (kèm phê duyệt hạn mức nợ credit limit) và Nhà cung cấp Inbound của hệ thống.
          </p>
        </div>
        <div>
          {activeTab === 'DEALERS' ? (
            (hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) ? (
              <Button variant="primary" icon={Plus} onClick={handleOpenAddDealer}>
                Thêm Đại lý mới
              </Button>
            ) : null
          ) : (
            (hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) ? (
              <Button variant="primary" icon={Plus} onClick={handleOpenAddSupplier}>
                Thêm Nhà cung cấp mới
              </Button>
            ) : null
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-hairline-light mb-6">
        <button
          onClick={() => { setActiveTab('DEALERS'); setSearchTerm(''); }}
          className={`px-5 py-3 font-semibold text-sm transition-all border-b-2 flex items-center gap-2 ${
            activeTab === 'DEALERS'
              ? 'border-ink text-ink bg-canvas-light/50 rounded-t-lg'
              : 'border-transparent text-shade-50 hover:text-ink'
          }`}
        >
          <Users className="w-4 h-4" />
          Đại lý & Hạn mức nợ (Dealers)
        </button>
        <button
          onClick={() => { setActiveTab('SUPPLIERS'); setSearchTerm(''); }}
          className={`px-5 py-3 font-semibold text-sm transition-all border-b-2 flex items-center gap-2 ${
            activeTab === 'SUPPLIERS'
              ? 'border-ink text-ink bg-canvas-light/50 rounded-t-lg'
              : 'border-transparent text-shade-50 hover:text-ink'
          }`}
        >
          <Handshake className="w-4 h-4" />
          Nhà cung cấp hàng hóa (Suppliers)
        </button>
      </div>

      {/* Search Input */}
      <div className="bg-canvas-light border border-hairline-light rounded-lg p-4 mb-6 shadow-level-3">
        <div className="flex-1 w-full max-w-md">
          <Input
            type="text"
            leftIcon={Search}
            placeholder={activeTab === 'DEALERS' ? 'Tìm theo mã hoặc tên Đại lý...' : 'Tìm theo mã hoặc tên Nhà cung cấp...'}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Main Content Table */}
      {loading ? (
        <div className="flex items-center justify-center p-20">
          <Loader2 className="w-8 h-8 animate-spin text-shade-50" />
        </div>
      ) : activeTab === 'DEALERS' ? (
        filteredDealers.length === 0 ? (
          <div className="bg-canvas-light rounded-lg border border-hairline-light p-12 text-center shadow-level-3">
            <AlertCircle className="w-12 h-12 text-shade-30 mx-auto mb-4" />
            <h3 className="text-lg font-bold mb-1">Không tìm thấy Đại lý</h3>
            <p className="text-sm text-shade-50">Thử thay đổi bộ lọc tìm kiếm hoặc thêm mới đại lý.</p>
          </div>
        ) : (
          <>
            {/* Desktop/tablet: table view */}
            <div className="hidden md:block bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-canvas-cream border-b border-hairline-light">
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã Đại lý</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Tên Đại lý</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Vùng miền</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Điện thoại</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Hạn thanh toán</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Dư nợ hiện tại</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Hạn mức tín dụng</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center">Trạng thái nợ</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center">Hoạt động</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Hành động</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {filteredDealers.map((dl) => {
                      const isCreditHold = dl.credit_status === 'CREDIT_HOLD' || dl.current_balance > dl.credit_limit;
                      return (
                        <tr key={dl.id} className={`hover:bg-canvas-cream/50 transition-colors ${!dl.is_active ? 'opacity-50' : ''}`}>
                          <td className="px-6 py-4 font-mono font-bold text-ink">{dl.code}</td>
                          <td className="px-6 py-4">
                            <div className="font-semibold text-ink">{dl.name}</div>
                            {dl.email && <div className="text-[11px] text-shade-50 font-normal">{dl.email}</div>}
                          </td>
                          <td className="px-6 py-4 text-shade-60">{dl.region || 'N/A'}</td>
                          <td className="px-6 py-4 text-shade-60 font-mono">{dl.phone || 'N/A'}</td>
                          <td className="px-6 py-4 text-right font-medium">{dl.payment_term_days} ngày</td>
                          <td className="px-6 py-4 text-right font-bold text-ink">
                            {dl.current_balance?.toLocaleString('vi-VN')} VND
                          </td>
                          <td className="px-6 py-4 text-right font-bold text-shade-70">
                            {dl.credit_limit?.toLocaleString('vi-VN')} VND
                          </td>
                          <td className="px-6 py-4 text-center">
                            {isCreditHold ? (
                              <Badge type="danger">
                                <span className="inline-flex items-center gap-1">
                                  <ShieldAlert className="w-3 h-3 shrink-0" /> HOLD (Vượt nợ)
                                </span>
                              </Badge>
                            ) : (
                              <Badge type="success">ACTIVE (Tốt)</Badge>
                            )}
                          </td>
                          <td className="px-6 py-4 text-center">
                            <Badge type={dl.is_active ? 'success' : 'neutral'} className="text-[9px]">
                              {dl.is_active ? 'Hoạt động' : 'Khóa'}
                            </Badge>
                          </td>
                          <td className="px-6 py-4">
                            <div className="flex gap-3.5 justify-end items-center">
                              {(hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) && (
                                <button
                                  onClick={() => handleOpenEditDealer(dl)}
                                  className="p-1 hover:bg-canvas-cream rounded-full transition-colors shrink-0 text-shade-60 hover:text-ink"
                                  title="Sửa thông tin & Hạn mức"
                                >
                                  <Edit className="w-4 h-4" />
                                </button>
                              )}
                              {(hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) && (
                                <button
                                  onClick={() => handleToggleDlStatus(dl)}
                                  className="p-1 hover:bg-canvas-cream rounded-full transition-colors shrink-0"
                                  title={dl.is_active ? 'Khóa đại lý' : 'Kích hoạt đại lý'}
                                >
                                  {dl.is_active ? (
                                    <ToggleRight className="w-5 h-5 text-success-600" />
                                  ) : (
                                    <ToggleLeft className="w-5 h-5 text-shade-40" />
                                  )}
                                </button>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Mobile: stacked card view */}
            <div className="flex flex-col gap-3 md:hidden">
              {filteredDealers.map((dl) => {
                const isCreditHold = dl.credit_status === 'CREDIT_HOLD' || dl.current_balance > dl.credit_limit;
                return (
                  <div key={dl.id} className={`bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden ${!dl.is_active ? 'opacity-50' : ''}`}>
                    <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                      <span className="font-mono font-bold text-xs text-ink">{dl.code}</span>
                      <Badge type={dl.is_active ? 'success' : 'neutral'} className="text-[9px]">
                        {dl.is_active ? 'Hoạt động' : 'Khóa'}
                      </Badge>
                    </div>
                    <div className="p-4 flex flex-col gap-2 text-xs">
                      <div className="font-semibold text-ink">{dl.name}</div>
                      {dl.email && <div className="text-shade-50">{dl.email}</div>}
                      <p className="text-shade-50">Vùng miền: <span className="font-medium text-ink">{dl.region || 'N/A'}</span></p>
                      <p className="text-shade-50">Số TK ngân hàng: <span className="font-medium text-ink font-mono">{dl.bank_account_number ? `${dl.bank_account_number}${dl.bank_name ? ` (${dl.bank_name})` : ''}` : 'N/A'}</span></p>
                      <p className="text-shade-50">Điện thoại: <span className="font-medium text-ink font-mono">{dl.phone || 'N/A'}</span></p>
                      <p className="text-shade-50">Hạn thanh toán: <span className="font-medium text-ink">{dl.payment_term_days} ngày</span></p>
                      <p className="text-shade-50">Dư nợ hiện tại: <span className="font-bold text-ink">{dl.current_balance?.toLocaleString('vi-VN')} VND</span></p>
                      <p className="text-shade-50">Hạn mức tín dụng: <span className="font-bold text-ink">{dl.credit_limit?.toLocaleString('vi-VN')} VND</span></p>
                      <div>
                        {isCreditHold ? (
                          <Badge type="danger">
                            <span className="inline-flex items-center gap-1">
                              <ShieldAlert className="w-3 h-3 shrink-0" /> HOLD (Vượt nợ)
                            </span>
                          </Badge>
                        ) : (
                          <Badge type="success">ACTIVE (Tốt)</Badge>
                        )}
                      </div>
                    </div>
                    <div className="p-4 border-t border-hairline-light flex gap-3.5 justify-end items-center">
                      {(hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) && (
                        <button
                          onClick={() => handleOpenEditDealer(dl)}
                          className="p-1.5 hover:bg-canvas-cream rounded-full transition-colors shrink-0 text-shade-60 hover:text-ink"
                          title="Sửa thông tin & Hạn mức"
                        >
                          <Edit className="w-4 h-4" />
                        </button>
                      )}
                      {(hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) && (
                        <button
                          onClick={() => handleToggleDlStatus(dl)}
                          className="p-1.5 hover:bg-canvas-cream rounded-full transition-colors shrink-0"
                          title={dl.is_active ? 'Khóa đại lý' : 'Kích hoạt đại lý'}
                        >
                          {dl.is_active ? (
                            <ToggleRight className="w-5 h-5 text-success-600" />
                          ) : (
                            <ToggleLeft className="w-5 h-5 text-shade-40" />
                          )}
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </>
        )
      ) : (
        // SUPPLIERS TAB
        filteredSuppliers.length === 0 ? (
          <div className="bg-canvas-light rounded-lg border border-hairline-light p-12 text-center shadow-level-3">
            <AlertCircle className="w-12 h-12 text-shade-30 mx-auto mb-4" />
            <h3 className="text-lg font-bold mb-1">Không tìm thấy Nhà cung cấp</h3>
            <p className="text-sm text-shade-50">Thử thay đổi bộ lọc tìm kiếm hoặc thêm mới nhà cung cấp.</p>
          </div>
        ) : (
          <>
            {/* Desktop/tablet: table view */}
            <div className="hidden md:block bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs border-collapse">
                  <thead>
                    <tr className="bg-canvas-cream border-b border-hairline-light">
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã Nhà CC</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Tên doanh nghiệp</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Mã số thuế</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Người liên hệ</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Điện thoại</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60">Địa chỉ văn phòng</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-center">Trạng thái</th>
                      <th className="px-6 py-4 text-xs font-semibold uppercase tracking-wider text-shade-60 text-right">Hành động</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-hairline-light">
                    {filteredSuppliers.map((spl) => (
                      <tr key={spl.id} className={`hover:bg-canvas-cream/50 transition-colors ${!spl.is_active ? 'opacity-50' : ''}`}>
                        <td className="px-6 py-4 font-mono font-bold text-ink">{spl.code}</td>
                        <td className="px-6 py-4 font-semibold text-ink">{spl.company_name}</td>
                        <td className="px-6 py-4 font-mono text-shade-60">{spl.tax_code || 'N/A'}</td>
                        <td className="px-6 py-4 text-ink font-medium">{spl.contact_person || 'N/A'}</td>
                        <td className="px-6 py-4 text-shade-60 font-mono">{spl.phone || 'N/A'}</td>
                        <td className="px-6 py-4 text-shade-50 max-w-xs truncate" title={spl.address}>{spl.address || 'N/A'}</td>
                        <td className="px-6 py-4 text-center">
                          <Badge type={spl.is_active ? 'success' : 'neutral'} className="text-[9px]">
                            {spl.is_active ? 'Hoạt động' : 'Khóa'}
                          </Badge>
                        </td>
                        <td className="px-6 py-4">
                          <div className="flex gap-3 justify-end items-center">
                            {(hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) ? (
                              <button
                                onClick={() => handleOpenEditSupplier(spl)}
                                className="p-1 hover:bg-canvas-cream rounded-full transition-colors shrink-0 text-shade-60 hover:text-ink"
                                title="Sửa nhà cung cấp"
                              >
                                <Edit className="w-4 h-4" />
                              </button>
                            ) : null}
                            {(hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) && (
                              <button
                                onClick={() => handleToggleSplStatus(spl)}
                                className="p-1 hover:bg-canvas-cream rounded-full transition-colors shrink-0"
                                title={spl.is_active ? 'Khóa nhà cung cấp' : 'Kích hoạt nhà cung cấp'}
                              >
                                {spl.is_active ? (
                                  <ToggleRight className="w-5 h-5 text-success-600" />
                                ) : (
                                  <ToggleLeft className="w-5 h-5 text-shade-40" />
                                )}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Mobile: stacked card view */}
            <div className="flex flex-col gap-3 md:hidden">
              {filteredSuppliers.map((spl) => (
                <div key={spl.id} className={`bg-canvas-light rounded-lg border border-hairline-light shadow-level-3 overflow-hidden ${!spl.is_active ? 'opacity-50' : ''}`}>
                  <div className="p-4 border-b border-hairline-light bg-canvas-cream flex justify-between items-center gap-2">
                    <span className="font-mono font-bold text-xs text-ink">{spl.code}</span>
                    <Badge type={spl.is_active ? 'success' : 'neutral'} className="text-[9px]">
                      {spl.is_active ? 'Hoạt động' : 'Khóa'}
                    </Badge>
                  </div>
                  <div className="p-4 flex flex-col gap-2 text-xs">
                    <div className="font-semibold text-ink">{spl.company_name}</div>
                    <p className="text-shade-50">Mã số thuế: <span className="font-mono font-medium text-ink">{spl.tax_code || 'N/A'}</span></p>
                    <p className="text-shade-50">Người liên hệ: <span className="font-medium text-ink">{spl.contact_person || 'N/A'}</span></p>
                    <p className="text-shade-50">Điện thoại: <span className="font-mono font-medium text-ink">{spl.phone || 'N/A'}</span></p>
                    <p className="text-shade-50">Địa chỉ: <span className="font-medium text-ink">{spl.address || 'N/A'}</span></p>
                  </div>
                  <div className="p-4 border-t border-hairline-light flex gap-3 justify-end items-center">
                    {(hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) ? (
                      <button
                        onClick={() => handleOpenEditSupplier(spl)}
                        className="p-1.5 hover:bg-canvas-cream rounded-full transition-colors shrink-0 text-shade-60 hover:text-ink"
                        title="Sửa nhà cung cấp"
                      >
                        <Edit className="w-4 h-4" />
                      </button>
                    ) : null}
                    {(hasRole(ROLES.ACCOUNTANT) || hasRole(ROLES.ACCOUNTANT_MANAGER)) && (
                      <button
                        onClick={() => handleToggleSplStatus(spl)}
                        className="p-1.5 hover:bg-canvas-cream rounded-full transition-colors shrink-0"
                        title={spl.is_active ? 'Khóa nhà cung cấp' : 'Kích hoạt nhà cung cấp'}
                      >
                        {spl.is_active ? (
                          <ToggleRight className="w-5 h-5 text-success-600" />
                        ) : (
                          <ToggleLeft className="w-5 h-5 text-shade-40" />
                        )}
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </>
        )
      )}

      {/* Dealer Modal */}
      <Modal
        isOpen={isDealerModalOpen}
        onClose={() => setIsDealerModalOpen(false)}
        title={dealerModalType === 'ADD' ? 'Đăng ký Đại lý mới' : `Chỉnh sửa Đại lý: ${selectedDealer?.code}`}
        maxWidth="max-w-lg"
      >
        <form onSubmit={handleDlSubmit} className="flex flex-col gap-4">
          {/* Basic Info Section */}
          <div className="flex flex-col gap-3">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Mã Đại lý (Duy nhất)"
                value={dlCode}
                onChange={(e) => setDlCode(e.target.value.toUpperCase())}
                disabled={dealerModalType === 'EDIT'}
                error={dlFormErrors.code}
                placeholder="VD: DL-HAIPHONG-01"
                required
              />
              <Input
                label="Khu vực vùng miền"
                value={dlRegion}
                onChange={(e) => setDlRegion(e.target.value)}
                placeholder="VD: Hải Phòng"
              />
            </div>

            <Input
              label="Tên Đại lý"
              value={dlName}
              onChange={(e) => setDlName(e.target.value)}
              error={dlFormErrors.name}
              placeholder="Nhập tên đại lý..."
              required
            />

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Số điện thoại liên lạc"
                value={dlPhone}
                onChange={(e) => setDlPhone(e.target.value)}
                placeholder="VD: 0912 345 678"
              />
              <Input
                label="Email liên hệ"
                type="email"
                value={dlEmail}
                onChange={(e) => setDlEmail(e.target.value)}
                error={dlFormErrors.email}
                placeholder="VD: daily@example.com"
              />
            </div>

            <div className="grid grid-cols-1 gap-4">
              <Input
                label="Địa chỉ giao hàng mặc định"
                value={dlAddress}
                onChange={(e) => setDlAddress(e.target.value)}
                placeholder="Nhập địa chỉ giao hàng..."
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Input
                label="Số tài khoản ngân hàng"
                value={dlBankAccountNumber}
                onChange={(e) => setDlBankAccountNumber(e.target.value)}
                placeholder="VD: 0123456789"
              />
              <Input
                label="Ngân hàng"
                value={dlBankName}
                onChange={(e) => setDlBankName(e.target.value)}
                placeholder="VD: Techcombank"
              />
            </div>
          </div>

          {/* Credit Section — visible to ACCOUNTANT_MANAGER or in ADD mode */}
          {(dealerModalType === 'ADD' || hasRole(ROLES.ACCOUNTANT_MANAGER)) && (
            <div className="border-t border-hairline-light pt-4">
              <div className="flex items-center gap-2 mb-3">
                <ShieldAlert className="w-4 h-4 text-warning-600" />
                <span className="text-xs font-bold text-shade-70 uppercase tracking-wide">
                  {dealerModalType === 'ADD' ? 'Hạn mức tín dụng ban đầu' : 'Phê duyệt hạn mức nợ (ACCOUNTANT_MANAGER)'}
                </span>
              </div>

              {dealerModalType === 'EDIT' && selectedDealer?.current_balance !== undefined && (
                <div className="bg-warning-50 border border-warning-200 rounded px-3 py-2 text-xs text-warning-800 mb-3 flex items-center gap-1.5">
                  <ShieldAlert className="w-3.5 h-3.5 text-warning-600 shrink-0" />
                  Dư nợ hiện tại: <strong>{selectedDealer?.current_balance?.toLocaleString('vi-VN')} VND</strong>. Thay đổi hạn mức có thể kích hoạt CREDIT_HOLD.
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Hạn mức tín dụng (VND)"
                  type="number"
                  value={dlCreditLimit}
                  onChange={(e) => setDlCreditLimit(e.target.value)}
                  error={dlFormErrors.credit_limit}
                  min="0"
                  step="1000000"
                  required
                />
                <Input
                  label="Kỳ hạn thanh toán (ngày)"
                  type="number"
                  value={dlPaymentTerms}
                  onChange={(e) => setDlPaymentTerms(e.target.value)}
                  error={dlFormErrors.payment_terms}
                  min="0"
                  step="1"
                  required
                />
              </div>

              {dealerModalType === 'EDIT' && (
                <div className="mt-4">
                  <label className="text-xs font-semibold text-shade-70 block mb-1">
                    Trạng thái tín dụng (can thiệp thủ công)
                  </label>
                  <select
                    value={dlCreditStatus}
                    onChange={(e) => setDlCreditStatus(e.target.value)}
                    className="bg-canvas-light text-ink text-xs border border-hairline-light rounded px-3 py-2 outline-none focus:border-shade-60 w-full sm:w-auto"
                  >
                    <option value="ACTIVE">ACTIVE (Cho phép giao dịch)</option>
                    <option value="CREDIT_HOLD">CREDIT_HOLD (Khóa công nợ)</option>
                  </select>
                  <p className="text-[10px] text-shade-40 mt-1">
                    Mở khóa thủ công (chuyển sang ACTIVE) yêu cầu dư nợ dưới 80% hạn mức và không còn hóa đơn quá hạn;
                    hệ thống sẽ từ chối và báo lý do cụ thể nếu chưa đủ điều kiện. Khóa (CREDIT_HOLD) luôn được chấp nhận.
                  </p>
                </div>
              )}
            </div>
          )}

          {/* If ACCOUNTANT (no credit rights) in EDIT mode — show readonly credit info */}
          {dealerModalType === 'EDIT' && !hasRole(ROLES.ACCOUNTANT_MANAGER) && (
            <div className="border-t border-hairline-light pt-4">
              <div className="flex gap-4 text-xs">
                <div className="flex-1 bg-canvas-cream rounded px-3 py-2">
                  <div className="text-shade-50 mb-0.5">Hạn mức tín dụng</div>
                  <div className="font-bold text-ink">{selectedDealer?.credit_limit?.toLocaleString('vi-VN')} VND</div>
                </div>
                <div className="flex-1 bg-canvas-cream rounded px-3 py-2">
                  <div className="text-shade-50 mb-0.5">Kỳ hạn thanh toán</div>
                  <div className="font-bold text-ink">{selectedDealer?.payment_term_days} ngày</div>
                </div>
              </div>
              <p className="text-[10px] text-shade-40 mt-1.5">Chỉ Kế toán trưởng mới có quyền thay đổi hạn mức nợ.</p>
            </div>
          )}

          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4 mt-2">
            <Button variant="outline-light" onClick={() => setIsDealerModalOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" variant="primary" loading={dlSubmitting}>
              {dealerModalType === 'ADD' ? 'Tạo mới' : 'Lưu thay đổi'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Supplier Modal */}
      <Modal
        isOpen={isSupplierModalOpen}
        onClose={() => setIsSupplierModalOpen(false)}
        title={supplierModalType === 'ADD' ? 'Đăng ký Nhà cung cấp mới' : 'Sửa thông tin Nhà cung cấp'}
        maxWidth="max-w-md"
      >
        <form onSubmit={handleSplSubmit} className="flex flex-col gap-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Mã nhà cung cấp"
              value={splCode}
              onChange={(e) => setSplCode(e.target.value.toUpperCase())}
              disabled={supplierModalType === 'EDIT'}
              error={splFormErrors.code}
              placeholder="VD: SPL-ASUS-VN"
              required
            />
            <Input
              label="Mã số thuế"
              value={splTaxCode}
              onChange={(e) => setSplTaxCode(e.target.value)}
              placeholder="Nhập MST doanh nghiệp..."
            />
          </div>

          <Input
            label="Tên doanh nghiệp / Công ty"
            value={splCompanyName}
            onChange={(e) => setSplCompanyName(e.target.value)}
            error={splFormErrors.company_name}
            placeholder="VD: Công ty TNHH ASUS Việt Nam"
            required
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Người liên hệ kinh doanh"
              value={splContactPerson}
              onChange={(e) => setSplContactPerson(e.target.value)}
              placeholder="VD: Nguyễn Văn A"
            />
            <Input
              label="Số điện thoại NCC"
              value={splPhone}
              onChange={(e) => setSplPhone(e.target.value)}
              placeholder="VD: 028 3999 888"
            />
          </div>

          <Input
            label="Địa chỉ văn phòng / nhà máy"
            value={splAddress}
            onChange={(e) => setSplAddress(e.target.value)}
            placeholder="Nhập địa chỉ nhà cung cấp..."
          />

          <div className="flex justify-end gap-3 border-t border-hairline-light pt-4 mt-2">
            <Button variant="outline-light" onClick={() => setIsSupplierModalOpen(false)}>
              Hủy
            </Button>
            <Button type="submit" variant="primary" loading={splSubmitting}>
              {supplierModalType === 'ADD' ? 'Tạo mới' : 'Lưu thay đổi'}
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default PartnerManagement;
