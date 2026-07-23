import React, { useState, useEffect } from 'react';
import { User, UserRole, AuditLog } from '../../types';
import { api } from '../../lib/api';
import { 
  Users, ShieldCheck, UserCheck, Eye, EyeOff, CheckCircle2, AlertTriangle, 
  Search, ToggleLeft, ToggleRight, Lock, Key, Activity, Clock, Layers 
} from 'lucide-react';

interface AdminViewProps {
  logs: AuditLog[];
  onRefreshData: () => Promise<void>;
}

export const AdminView: React.FC<AdminViewProps> = ({ logs, onRefreshData }) => {
  const [activeSubTab, setActiveSubTab] = useState<'users' | 'roles' | 'logs' | 'analytics'>('users');
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [selectedRoleFilter, setSelectedRoleFilter] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  // Analytics states
  const [platformMetrics, setPlatformMetrics] = useState<any>(null);
  const [lowStockAlerts, setLowStockAlerts] = useState<any[]>([]);
  const [nearExpiryLots, setNearExpiryLots] = useState<any[]>([]);
  const [salesSummary, setSalesSummary] = useState<any>(null);
  const [loadingAnalytics, setLoadingAnalytics] = useState(false);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const u = await api.getUsers();
      setUsers(u);
    } catch (err) {
      console.error('Failed to load users in Admin Portal:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadAnalytics = async () => {
    setLoadingAnalytics(true);
    try {
      const [m, e, s, sl] = await Promise.all([
        api.getPlatformMetricsReport(),
        api.getNearExpiryLotsReport(),
        api.getLowStockAlertsReport(),
        api.getSalesSummaryReport()
      ]);
      setPlatformMetrics(m);
      setNearExpiryLots(e);
      setLowStockAlerts(s);
      setSalesSummary(sl);
    } catch (err) {
      console.error('Failed to fetch corporate analytics reports:', err);
    } finally {
      setLoadingAnalytics(false);
    }
  };

  useEffect(() => {
    if (activeSubTab === 'analytics') {
      loadAnalytics();
    } else {
      loadUsers();
    }
  }, [activeSubTab]);

  const handleToggleUserStatus = async (user: User) => {
    const nextStatus = user.status === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE';
    try {
      await api.updateUserStatus(user.id, { status: nextStatus });
      setSuccessMsg(`User ${user.name} status updated to ${nextStatus}.`);
      setTimeout(() => setSuccessMsg(''), 4000);
      await loadUsers();
      await onRefreshData();
    } catch (err) {
      console.error('Failed to change user status:', err);
    }
  };

  const filteredUsers = users.filter(u => {
    const matchesSearch = u.name.toLowerCase().includes(search.toLowerCase()) || 
                          u.email.toLowerCase().includes(search.toLowerCase()) ||
                          u.phone.includes(search);
    const matchesRole = selectedRoleFilter ? u.role === selectedRoleFilter : true;
    return matchesSearch && matchesRole;
  });

  // Roles & Permissions Matrix Mock Config (Fully Documented in Spring Security 6 specs)
  const rolePermissions = [
    {
      role: 'SUPER_ADMIN' as UserRole,
      desc: 'Full global system administration, tenant partitioning, and database overrides',
      modules: ['All Modules (Inventory, Orders, Deliveries, Salesman, Finance, Audit, System Logs)'],
      permissions: ['CREATE', 'READ', 'UPDATE', 'DELETE', 'APPROVE_CREDIT', 'OVERRIDE_FEFO', 'PURGE_LOGS'],
    },
    {
      role: 'ADMIN' as UserRole,
      desc: 'Company executive controlling pricing models, stock levels, warehouse nodes, and client lines',
      modules: ['Inventory', 'Orders', 'Deliveries', 'Salesman CRM', 'Warehouses', 'Invoices', 'Audit Logs'],
      permissions: ['CREATE', 'READ', 'UPDATE', 'APPROVE_B2B_ORDERS', 'APPROVE_CREDIT', 'GENERATE_INVOICE'],
    },
    {
      role: 'WAREHOUSE_STAFF' as UserRole,
      desc: 'Supply chain operators handling FEFO lot allocation, shelf stocking, and transit operations',
      modules: ['Inventory Batches', 'Warehouse Nodes', 'Stock Transfers'],
      permissions: ['CREATE_BATCH', 'READ', 'UPDATE_BATCH_STOCK', 'INITIATE_TRANSFER', 'COMPLETE_TRANSFER'],
    },
    {
      role: 'SALESMAN' as UserRole,
      desc: 'Commissioned executives managing B2B leads, customer onboarding, and order bookings',
      modules: ['Salesman CRM', 'Lead Prospecting', 'B2B Placement'],
      permissions: ['CREATE_LEAD', 'READ_LEADS', 'UPDATE_LEAD_STATUS', 'CREATE_ORDER_FOR_CLIENT'],
    },
    {
      role: 'DELIVERY_BOY' as UserRole,
      desc: 'Last-mile dispatch couriers executing secure client drop-offs and cold-chain dock validation',
      modules: ['Delivery Dispatches', 'OTP Handover Screen'],
      permissions: ['READ_ASSIGNED_TASKS', 'UPDATE_DELIVERY_GPS', 'VERIFY_DELIVERY_OTP'],
    },
    {
      role: 'B2B_CUSTOMER' as UserRole,
      desc: 'Institutional hospitals, health networks, and clinical purchasing departments',
      modules: ['B2B Wholesale Catalog', 'NET-30 Credit Ledger', 'Purchase History'],
      permissions: ['READ_BULK_PRICING', 'PLACE_B2B_ORDER', 'VIEW_TAX_INVOICES'],
    },
    {
      role: 'B2C_CUSTOMER' as UserRole,
      desc: 'Individual retail patients purchasing standard OTC or prescription drugs',
      modules: ['B2C Pharmacy Storefront', 'Personal Cart & Checkout', 'Prescription Upload'],
      permissions: ['READ_RETAIL_PRICES', 'PLACE_B2C_ORDER', 'UPLOAD_PRESCRIPTION'],
    },
  ];

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <Users className="h-6 w-6 text-indigo-600 dark:text-indigo-400" />
            Super-Admin Console & Access Control Matrix
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Corporate IAM management, method-level spring security roles, and real-time audit tracing
          </p>
        </div>

        {/* Sub-tabs toggle */}
        <div className="flex items-center gap-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-xl">
          <button
            onClick={() => setActiveSubTab('users')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSubTab === 'users'
                ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            User Accounts
          </button>
          <button
            onClick={() => setActiveSubTab('roles')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSubTab === 'roles'
                ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            RBAC Permissions
          </button>
          <button
            onClick={() => setActiveSubTab('logs')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSubTab === 'logs'
                ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            Audit Ledger ({logs.length})
          </button>
          <button
            onClick={() => setActiveSubTab('analytics')}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-bold transition-all ${
              activeSubTab === 'analytics'
                ? 'bg-white dark:bg-slate-900 text-indigo-600 dark:text-indigo-400 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
            }`}
          >
            Enterprise Analytics
          </button>
        </div>
      </div>

      {/* SUCCESS BANNER */}
      {successMsg && (
        <div className="p-3.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-300 dark:border-emerald-800 text-emerald-900 dark:text-emerald-200 text-xs flex items-center gap-2">
          <CheckCircle2 className="h-4.5 w-4.5 text-emerald-500" />
          <span className="font-bold">{successMsg}</span>
        </div>
      )}

      {/* USER MANAGEMENT SHEET */}
      {activeSubTab === 'users' && (
        <div className="space-y-4">
          
          {/* Controls */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 p-4 rounded-2xl bg-slate-100 dark:bg-slate-800/50 border border-slate-200 dark:border-slate-800">
            <div className="relative w-full sm:w-80">
              <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search name, email, phone..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="w-full pl-9 pr-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-xs text-slate-800 dark:text-slate-100"
              />
            </div>

            <div className="flex items-center gap-2 w-full sm:w-auto justify-end text-xs">
              <span className="text-slate-400">Filter Role:</span>
              <select
                value={selectedRoleFilter}
                onChange={(e) => setSelectedRoleFilter(e.target.value)}
                className="p-1.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 font-bold"
              >
                <option value="">All Roles</option>
                <option value="SUPER_ADMIN">Super Admin</option>
                <option value="ADMIN">Admin</option>
                <option value="WAREHOUSE_STAFF">Warehouse Staff</option>
                <option value="SALESMAN">Salesman</option>
                <option value="DELIVERY_BOY">Delivery Boy</option>
                <option value="B2B_CUSTOMER">B2B Customer</option>
                <option value="B2C_CUSTOMER">B2C Patient</option>
              </select>
            </div>
          </div>

          {/* Users Table */}
          <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-700 dark:text-slate-300">
                <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider text-[11px] border-b border-slate-200 dark:border-slate-800">
                  <tr>
                    <th className="px-4 py-3">ID & User Name</th>
                    <th className="px-4 py-3">Email Address</th>
                    <th className="px-4 py-3">Assigned Role</th>
                    <th className="px-4 py-3">Phone</th>
                    <th className="px-4 py-3">B2B Credit / Drug No</th>
                    <th className="px-4 py-3">Portal Status</th>
                    <th className="px-4 py-3 text-right">Restrict / Revoke</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {loading ? (
                    <tr>
                      <td colSpan={7} className="text-center py-8 font-semibold animate-pulse text-slate-400">
                        Querying master user registries...
                      </td>
                    </tr>
                  ) : filteredUsers.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="text-center py-8 text-slate-400">
                        No matching users found
                      </td>
                    </tr>
                  ) : (
                    filteredUsers.map(u => (
                      <tr key={u.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors">
                        <td className="px-4 py-3">
                          <span className="font-mono text-[10px] text-slate-400 block">{u.id}</span>
                          <span className="font-bold text-slate-900 dark:text-white">{u.name}</span>
                        </td>
                        <td className="px-4 py-3 font-medium text-slate-600 dark:text-slate-400">
                          {u.email}
                        </td>
                        <td className="px-4 py-3">
                          <span className="px-2 py-0.5 rounded font-mono font-bold text-[10px] bg-indigo-50 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-400">
                            {u.role}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-slate-500">
                          {u.phone}
                        </td>
                        <td className="px-4 py-3">
                          {u.role === 'B2B_CUSTOMER' ? (
                            <div>
                              <span className="block text-[11px] font-bold text-teal-600">Credit: ${(u.creditLimit || 0).toLocaleString()}</span>
                              <span className="text-[10px] text-slate-400">Lic: {u.licenseNumber || 'N/A'}</span>
                            </div>
                          ) : (
                            <span className="text-slate-400 italic text-[10px]">Non-Institutional</span>
                          )}
                        </td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            u.status === 'ACTIVE' 
                              ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300' 
                              : 'bg-red-100 text-red-800 dark:bg-red-950/60 dark:text-red-300'
                          }`}>
                            {u.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-right">
                          <button
                            onClick={() => handleToggleUserStatus(u)}
                            className={`p-1 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 inline-flex items-center gap-1 text-[11px] font-bold ${
                              u.status === 'ACTIVE' ? 'text-red-600 hover:text-red-500' : 'text-emerald-600 hover:text-emerald-500'
                            }`}
                          >
                            {u.status === 'ACTIVE' ? (
                              <>
                                <Lock className="h-3.5 w-3.5" /> Suspend
                              </>
                            ) : (
                              <>
                                <Key className="h-3.5 w-3.5" /> Activate
                              </>
                            )}
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* RBAC PERMISSIONS MATRIX */}
      {activeSubTab === 'roles' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {rolePermissions.map((rp) => (
            <div key={rp.role} className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col justify-between space-y-4">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="px-2.5 py-0.5 rounded-full text-[10px] font-mono font-bold bg-indigo-100 text-indigo-800 dark:bg-indigo-950 dark:text-indigo-300">
                    {rp.role}
                  </span>
                  <ShieldCheck className="h-5 w-5 text-indigo-500" />
                </div>
                
                <h3 className="font-extrabold text-sm text-slate-900 dark:text-white">Role Definition</h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1.5 leading-relaxed">
                  {rp.desc}
                </p>

                {/* Scope list */}
                <div className="mt-4 space-y-2">
                  <h4 className="font-bold text-slate-400 uppercase tracking-wider text-[9px]">Accessible Scope Modules</h4>
                  <div className="flex flex-wrap gap-1">
                    {rp.modules.map(m => (
                      <span key={m} className="px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-[10px] text-slate-700 dark:text-slate-300">
                        {m}
                      </span>
                    ))}
                  </div>
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 dark:border-slate-800">
                <h4 className="font-bold text-slate-400 uppercase tracking-wider text-[9px] mb-2">Method Authorities Granted</h4>
                <div className="flex flex-wrap gap-1">
                  {rp.permissions.map(p => (
                    <span key={p} className="px-2 py-0.5 rounded font-mono font-bold text-[9px] bg-teal-50 text-teal-800 dark:bg-teal-950/40 dark:text-teal-300">
                      @PreAuthorize("{p}")
                    </span>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* SECURITY AUDIT LEDGER */}
      {activeSubTab === 'logs' && (
        <div className="space-y-4">
          <div className="p-4 rounded-2xl border border-purple-200 dark:border-purple-950 bg-purple-50/20 dark:bg-purple-950/10 text-purple-900 dark:text-purple-200 text-xs flex items-center gap-3">
            <Activity className="h-5 w-5 text-purple-500 flex-shrink-0" />
            <div>
              <span className="font-bold block">Immutable Blockchain-Grade Audit Ledger</span>
              <span className="text-[11px] text-slate-500 dark:text-slate-400">All inventory transfers, order handshakes, and sign-ins write directly to an append-only transaction ledger.</span>
            </div>
          </div>

          {/* Render the core table from logs */}
          <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs text-slate-700 dark:text-slate-300">
                <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider text-[11px] border-b border-slate-200 dark:border-slate-800">
                  <tr>
                    <th className="px-4 py-3">Timestamp</th>
                    <th className="px-4 py-3">Authorized Principal</th>
                    <th className="px-4 py-3">Action Identifier</th>
                    <th className="px-4 py-3">Entity Domain</th>
                    <th className="px-4 py-3">Ledger Details</th>
                    <th className="px-4 py-3">Gateway IP Address</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {logs.map(l => (
                    <tr key={l.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors">
                      <td className="px-4 py-3 font-mono text-[11px] text-slate-500">
                        {l.timestamp.replace('T', ' ').substring(0, 19)}
                      </td>
                      <td className="px-4 py-3">
                        <span className="font-bold text-slate-900 dark:text-white block">{l.userName}</span>
                        <span className="text-[10px] text-purple-600 dark:text-purple-400 font-bold">{l.userRole}</span>
                      </td>
                      <td className="px-4 py-3">
                        <span className="font-mono font-bold text-slate-900 dark:text-slate-100 px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-[10px]">
                          {l.action}
                        </span>
                      </td>
                      <td className="px-4 py-3 font-semibold text-slate-700 dark:text-slate-300">
                        {l.module}
                      </td>
                      <td className="px-4 py-3 text-slate-600 dark:text-slate-400 max-w-xs truncate">
                        {l.details}
                      </td>
                      <td className="px-4 py-3 font-mono text-slate-500 text-[11px]">
                        {l.ipAddress}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* ENTERPRISE CORPORATE ANALYTICS HUB */}
      {activeSubTab === 'analytics' && (
        <div className="space-y-6">
          {/* Overview Cards */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">Catalog Products</div>
              <div className="text-2xl font-extrabold text-slate-900 dark:text-white mt-1">
                {loadingAnalytics ? '...' : platformMetrics?.totalProducts || 0}
              </div>
              <div className="text-[10px] text-slate-400 mt-1">Active pharmaceutical catalog items</div>
            </div>
            <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">Total System Orders</div>
              <div className="text-2xl font-extrabold text-slate-900 dark:text-white mt-1">
                {loadingAnalytics ? '...' : platformMetrics?.totalOrders || 0}
              </div>
              <div className="text-[10px] text-slate-400 mt-1">
                Pending: <span className="font-bold text-amber-600">{platformMetrics?.pendingOrders || 0}</span> | Del: <span className="font-bold text-emerald-600">{platformMetrics?.completedOrders || 0}</span>
              </div>
            </div>
            <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">Total Registered Users</div>
              <div className="text-2xl font-extrabold text-slate-900 dark:text-white mt-1">
                {loadingAnalytics ? '...' : platformMetrics?.totalUsers || 0}
              </div>
              <div className="text-[10px] text-slate-400 mt-1">Staff, Salesmen, and B2B/B2C accounts</div>
            </div>
            <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <div className="text-xs font-bold text-slate-400 uppercase tracking-wider">Inventory Asset Value</div>
              <div className="text-2xl font-extrabold text-slate-900 dark:text-white mt-1">
                {loadingAnalytics ? '...' : `$${(platformMetrics?.totalInventoryValuation || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
              </div>
              <div className="text-[10px] text-slate-400 mt-1">Calculated across active FEFO lots</div>
            </div>
          </div>

          {/* Low Stock vs Near Expiry Grid */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            
            {/* Low Stock Alerts */}
            <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white text-sm">Low-Stock Active Alerts</h3>
                  <p className="text-[11px] text-slate-400">Products with available stock below threshold (50 units)</p>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-50 text-rose-700 dark:bg-rose-950 dark:text-rose-300">
                  {lowStockAlerts.length} Products
                </span>
              </div>

              <div className="max-h-[300px] overflow-y-auto space-y-2 pr-1">
                {loadingAnalytics ? (
                  <div className="text-center py-8 font-semibold animate-pulse text-slate-400 text-xs">Querying available batch volumes...</div>
                ) : lowStockAlerts.length === 0 ? (
                  <div className="text-center py-8 text-slate-400 text-xs">All products satisfy the 50-unit safety threshold!</div>
                ) : (
                  lowStockAlerts.map(a => (
                    <div key={a.productId} className="p-3 rounded-xl border border-rose-100 dark:border-rose-950 bg-rose-50/20 dark:bg-rose-950/10 flex items-center justify-between text-xs">
                      <div>
                        <span className="font-bold text-slate-900 dark:text-white">{a.productName}</span>
                        <span className="block text-[10px] text-slate-400 mt-0.5">SKU: {a.sku}</span>
                      </div>
                      <div className="text-right">
                        <span className="font-extrabold text-rose-600 block">{a.availableQuantity} Units Left</span>
                        <span className="text-[10px] text-slate-400">Threshold: {a.safetyThreshold}</span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Near Expiry Batches */}
            <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="font-bold text-slate-900 dark:text-white text-sm">Near-Expiry FEFO Inventory</h3>
                  <p className="text-[11px] text-slate-400">Physical lot batches expiring within the 90-day warning horizon</p>
                </div>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-50 text-amber-700 dark:bg-amber-950 dark:text-amber-300">
                  {nearExpiryLots.length} Lots
                </span>
              </div>

              <div className="max-h-[300px] overflow-y-auto space-y-2 pr-1">
                {loadingAnalytics ? (
                  <div className="text-center py-8 font-semibold animate-pulse text-slate-400 text-xs">Evaluating lot expirations...</div>
                ) : nearExpiryLots.length === 0 ? (
                  <div className="text-center py-8 text-slate-400 text-xs">No batches are expiring within 90 days. Perfect FEFO alignment!</div>
                ) : (
                  nearExpiryLots.map(l => (
                    <div key={l.batchId} className="p-3 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50 flex items-center justify-between text-xs">
                      <div>
                        <span className="font-bold text-slate-900 dark:text-white">{l.productName}</span>
                        <span className="block text-[10px] text-slate-400 mt-0.5">Batch: <span className="font-mono font-bold text-indigo-600">{l.batchNumber}</span> | Exp: {l.expiryDate}</span>
                      </div>
                      <div className="text-right">
                        <span className="font-extrabold text-amber-600 block">{l.daysToExpiry} Days Remaining</span>
                        <span className="text-[10px] text-slate-400">{l.quantityOnHand} UnitsOnHand</span>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

          </div>

          {/* Sales Summary Report & Cash Flow */}
          <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
            <h3 className="font-bold text-slate-900 dark:text-white text-sm mb-4">Enterprise Sales Summary & Cash Flow Overview</h3>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div className="p-4 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 text-xs flex flex-col justify-between">
                <div>
                  <span className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Settled Platform Revenue</span>
                  <div className="text-xl font-extrabold text-emerald-600 mt-1">
                    {loadingAnalytics ? '...' : `$${(salesSummary?.totalRevenue || 0).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
                  </div>
                </div>
                <p className="text-[10px] text-slate-400 mt-2">Revenue from completed and shipped wholesale/retail orders.</p>
              </div>

              <div className="p-4 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 text-xs">
                <span className="font-bold text-slate-400 uppercase tracking-wider text-[10px] block mb-2">Order Distribution States</span>
                <div className="space-y-1.5 max-h-[100px] overflow-y-auto">
                  {loadingAnalytics ? '...' : salesSummary?.ordersByStatus ? (
                    Object.entries(salesSummary.ordersByStatus).map(([status, count]: any) => (
                      <div key={status} className="flex items-center justify-between text-slate-600 dark:text-slate-300">
                        <span className="font-mono text-[10px]">{status}</span>
                        <span className="font-bold">{count} orders</span>
                      </div>
                    ))
                  ) : <span className="text-slate-400 text-[11px] italic">No data</span>}
                </div>
              </div>

              <div className="p-4 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/20 text-xs">
                <span className="font-bold text-slate-400 uppercase tracking-wider text-[10px] block mb-2">Settled Liquidity by Method</span>
                <div className="space-y-1.5 max-h-[100px] overflow-y-auto">
                  {loadingAnalytics ? '...' : salesSummary?.revenueByPaymentMethod ? (
                    Object.entries(salesSummary.revenueByPaymentMethod).map(([method, amount]: any) => (
                      <div key={method} className="flex items-center justify-between text-slate-600 dark:text-slate-300">
                        <span>{method}</span>
                        <span className="font-bold">${amount?.toFixed(2)}</span>
                      </div>
                    ))
                  ) : <span className="text-slate-400 text-[11px] italic">No data</span>}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
