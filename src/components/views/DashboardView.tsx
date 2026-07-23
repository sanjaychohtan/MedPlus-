import React from 'react';
import { SystemMetrics, Order, Batch, Warehouse } from '../../types';
import { 
  ResponsiveContainer, AreaChart, Area, XAxis, YAxis, Tooltip, CartesianGrid, BarChart, Bar, PieChart, Pie, Cell 
} from 'recharts';
import { AlertTriangle, TrendingUp, ShieldCheck, ThermometerSnowflake, FileText, ArrowRight } from 'lucide-react';

interface DashboardViewProps {
  metrics: SystemMetrics | null;
  orders: Order[];
  batches: Batch[];
  warehouses: Warehouse[];
  onNavigateTab: (tab: string) => void;
}

export const DashboardView: React.FC<DashboardViewProps> = ({
  metrics,
  orders,
  batches,
  warehouses,
  onNavigateTab,
}) => {
  // Chart Data: Monthly Revenue Trends (B2B vs B2C)
  const revenueTrend = [
    { month: 'Jan', b2b: 85000, b2c: 12000 },
    { month: 'Feb', b2b: 98000, b2c: 15400 },
    { month: 'Mar', b2b: 112000, b2c: 18200 },
    { month: 'Apr', b2b: 125000, b2c: 21000 },
    { month: 'May', b2b: 138000, b2c: 24500 },
    { month: 'Jun', b2b: 142000, b2c: 28000 },
    { month: 'Jul', b2b: metrics?.totalRevenueB2B || 150000, b2c: metrics?.totalRevenueB2C || 32000 },
  ];

  // FEFO Expiry Distribution
  const expiryCategories = [
    { name: 'Safe (>180 days)', count: batches.filter(b => b.status === 'ACTIVE').length, color: '#10b981' },
    { name: 'Near Expiry (<60 days)', count: batches.filter(b => b.status === 'NEAR_EXPIRY').length, color: '#f59e0b' },
    { name: 'Expired', count: batches.filter(b => b.status === 'EXPIRED').length, color: '#ef4444' },
    { name: 'Quarantined', count: batches.filter(b => b.status === 'QUARANTINED').length, color: '#6b7280' },
  ];

  const nearExpiryBatches = batches.filter(b => b.status === 'NEAR_EXPIRY');

  return (
    <div className="space-y-6">
      
      {/* Top Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-teal-900 via-slate-900 to-indigo-900 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-teal-400 font-semibold text-xs tracking-wider uppercase">
            <ShieldCheck className="h-4 w-4" /> MedSupply Enterprise Control Hub
          </div>
          <h1 className="text-2xl font-extrabold tracking-tight mt-1">
            Healthcare Supply Chain & FEFO Execution Center
          </h1>
          <p className="text-sm text-slate-300 mt-1 max-w-2xl">
            Real-time batch tracking, cold-chain monitoring, automated B2B credit terms, and multi-warehouse fulfillment powered by Java 21 Spring Boot DDD backend logic.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => onNavigateTab('inventory')}
            className="px-4 py-2.5 rounded-xl bg-teal-500 hover:bg-teal-400 text-slate-950 font-bold text-xs transition-all shadow-lg flex items-center gap-2"
          >
            Manage FEFO Batches <ArrowRight className="h-4 w-4" />
          </button>
          <button
            onClick={() => onNavigateTab('architecture')}
            className="px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white border border-white/20 font-semibold text-xs backdrop-blur-md transition-all"
          >
            Spring Boot & AWS Specs
          </button>
        </div>
      </div>

      {/* Analytics Grid: Revenue & FEFO Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Revenue Growth Trend Chart */}
        <div className="lg:col-span-2 p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="font-bold text-slate-900 dark:text-white text-base">
                Gross Revenue Growth (B2B vs B2C)
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Monthly medical supply sales trajectory
              </p>
            </div>
            <span className="flex items-center gap-1 text-xs font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/50 px-2.5 py-1 rounded-lg">
              <TrendingUp className="h-3.5 w-3.5" /> +24.8% YoY
            </span>
          </div>

          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={revenueTrend} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorB2B" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorB2C" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#14b8a6" stopOpacity={0.4}/>
                    <stop offset="95%" stopColor="#14b8a6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#334155" opacity={0.15} />
                <XAxis dataKey="month" stroke="#94a3b8" fontSize={12} tickLine={false} />
                <YAxis stroke="#94a3b8" fontSize={12} tickLine={false} tickFormatter={(v) => `$${v / 1000}k`} />
                <Tooltip 
                  contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '12px', color: '#fff' }}
                  formatter={(val: any) => [`$${Number(val).toLocaleString()}`, 'Amount']}
                />
                <Area type="monotone" dataKey="b2b" name="B2B Wholesale" stroke="#6366f1" strokeWidth={3} fillOpacity={1} fill="url(#colorB2B)" />
                <Area type="monotone" dataKey="b2c" name="B2C Retail" stroke="#14b8a6" strokeWidth={3} fillOpacity={1} fill="url(#colorB2C)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* FEFO Expiry Risk Pie Chart */}
        <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col justify-between">
          <div>
            <h3 className="font-bold text-slate-900 dark:text-white text-base">
              Batch Expiry Health (FEFO)
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400">
              Stock status by expiration risk horizon
            </p>

            <div className="h-48 w-full my-2">
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={expiryCategories}
                    cx="50%"
                    cy="50%"
                    innerRadius={50}
                    outerRadius={75}
                    paddingAngle={4}
                    dataKey="count"
                  >
                    {expiryCategories.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#0f172a', borderColor: '#334155', borderRadius: '8px', color: '#fff' }}
                  />
                </PieChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="space-y-1.5 text-xs">
            {expiryCategories.map((cat) => (
              <div key={cat.name} className="flex items-center justify-between text-slate-600 dark:text-slate-300">
                <div className="flex items-center gap-2">
                  <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: cat.color }} />
                  <span>{cat.name}</span>
                </div>
                <span className="font-bold">{cat.count} Batches</span>
              </div>
            ))}
          </div>
        </div>

      </div>

      {/* Near Expiry FEFO Action Required Panel */}
      {nearExpiryBatches.length > 0 && (
        <div className="p-5 rounded-2xl border border-amber-300 dark:border-amber-800/80 bg-amber-50/50 dark:bg-amber-950/20 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2 text-amber-800 dark:text-amber-300 font-bold text-sm">
              <AlertTriangle className="h-5 w-5 text-amber-500" />
              <span>FEFO Priority Action: Batches Expiring Within 60 Days</span>
            </div>
            <button
              onClick={() => onNavigateTab('inventory')}
              className="text-xs font-bold text-amber-700 dark:text-amber-400 underline hover:text-amber-800"
            >
              Dispatch Priority Orders →
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
            {nearExpiryBatches.map(b => (
              <div key={b.id} className="p-3 rounded-xl bg-white dark:bg-slate-900 border border-amber-200 dark:border-amber-900/60 flex items-center justify-between">
                <div>
                  <p className="font-bold text-xs text-slate-900 dark:text-white">{b.productName}</p>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400">
                    Batch: <span className="font-mono font-bold text-amber-600">{b.batchNumber}</span> | Exp: {b.expiryDate}
                  </p>
                </div>
                <div className="text-right">
                  <span className="text-xs font-bold text-slate-900 dark:text-white block">
                    {b.quantityOnHand} Units
                  </span>
                  <span className="text-[10px] text-slate-400">{b.warehouseName}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Warehouse Status & Recent Activity Table */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        
        {/* Multi-Warehouse Status */}
        <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <h3 className="font-bold text-slate-900 dark:text-white text-base mb-3">
            Warehouse Logistics Network
          </h3>
          <div className="space-y-3">
            {warehouses.map(wh => (
              <div key={wh.id} className="p-3.5 rounded-xl border border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-xs text-slate-900 dark:text-white">{wh.name}</span>
                    {wh.tempControl === 'COLD_CHAIN_2_8C' && (
                      <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-sky-100 dark:bg-sky-950 text-sky-700 dark:text-sky-300 flex items-center gap-1">
                        <ThermometerSnowflake className="h-3 w-3" /> 2-8°C Vault
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                    {wh.address}, {wh.city} • Manager: {wh.managerName}
                  </p>
                </div>
                <div className="text-right">
                  <span className="text-xs font-semibold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/50 px-2 py-0.5 rounded">
                    {wh.status}
                  </span>
                  <span className="block text-[11px] text-slate-400 mt-1">{wh.capacitySqFt.toLocaleString()} sq.ft</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Recent Orders Queue */}
        <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <div className="flex items-center justify-between mb-3">
            <h3 className="font-bold text-slate-900 dark:text-white text-base">
              Recent Order Stream
            </h3>
            <button 
              onClick={() => onNavigateTab('orders')}
              className="text-xs font-semibold text-teal-600 dark:text-teal-400 hover:underline"
            >
              View All
            </button>
          </div>

          <div className="space-y-3">
            {orders.slice(0, 4).map(o => (
              <div key={o.id} className="p-3 rounded-xl border border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono font-bold text-slate-900 dark:text-white">{o.orderNumber}</span>
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      o.orderType === 'B2B' ? 'bg-indigo-100 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300' : 'bg-teal-100 text-teal-700 dark:bg-teal-950 dark:text-teal-300'
                    }`}>
                      {o.orderType}
                    </span>
                  </div>
                  <p className="text-slate-500 dark:text-slate-400 mt-0.5">{o.customerName}</p>
                </div>
                <div className="text-right">
                  <span className="font-bold text-slate-900 dark:text-white block">${o.totalAmount.toFixed(2)}</span>
                  <span className="text-[10px] font-semibold text-teal-600 dark:text-teal-400">{o.orderStatus}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>

    </div>
  );
};
