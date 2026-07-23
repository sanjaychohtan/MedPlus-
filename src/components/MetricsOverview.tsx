import React from 'react';
import { SystemMetrics } from '../types';
import { DollarSign, AlertTriangle, Warehouse, Truck, ShoppingCart, ShieldAlert, ThermometerSnowflake } from 'lucide-react';

interface MetricsOverviewProps {
  metrics: SystemMetrics | null;
  onSelectMetricFilter?: (filterKey: string) => void;
}

export const MetricsOverview: React.FC<MetricsOverviewProps> = ({ metrics, onSelectMetricFilter }) => {
  if (!metrics) return null;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      
      {/* Revenue Card */}
      <div className="p-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
            Total Gross Revenue
          </p>
          <p className="text-2xl font-bold text-slate-900 dark:text-white mt-1">
            ${(metrics.totalRevenueB2B + metrics.totalRevenueB2C).toLocaleString('en-US', { minimumFractionDigits: 2 })}
          </p>
          <div className="flex items-center gap-2 mt-2 text-xs">
            <span className="text-indigo-600 dark:text-indigo-400 font-semibold bg-indigo-50 dark:bg-indigo-950/50 px-2 py-0.5 rounded-md">
              B2B: ${metrics.totalRevenueB2B.toLocaleString()}
            </span>
            <span className="text-teal-600 dark:text-teal-400 font-semibold bg-teal-50 dark:bg-teal-950/50 px-2 py-0.5 rounded-md">
              B2C: ${metrics.totalRevenueB2C.toLocaleString()}
            </span>
          </div>
        </div>
        <div className="p-3 rounded-xl bg-teal-50 dark:bg-teal-950/50 text-teal-600 dark:text-teal-400">
          <DollarSign className="h-6 w-6" />
        </div>
      </div>

      {/* FEFO Expiry Risk Alert Card */}
      <div 
        onClick={() => onSelectMetricFilter?.('NEAR_EXPIRY')}
        className={`p-4 rounded-2xl border bg-white dark:bg-slate-900 shadow-sm flex items-start justify-between cursor-pointer transition-all hover:scale-[1.01] ${
          metrics.nearExpiryCount > 0 
            ? 'border-amber-300 dark:border-amber-800/80 bg-amber-50/30 dark:bg-amber-950/20' 
            : 'border-slate-200 dark:border-slate-800'
        }`}
      >
        <div>
          <p className="text-xs font-semibold text-amber-700 dark:text-amber-400 uppercase tracking-wider flex items-center gap-1">
            <AlertTriangle className="h-3.5 w-3.5 text-amber-500" /> FEFO Expiry Risk (&lt;60d)
          </p>
          <p className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-1">
            {metrics.nearExpiryCount} Batches
          </p>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-2">
            First Expired First Out allocation enabled
          </p>
        </div>
        <div className="p-3 rounded-xl bg-amber-100 dark:bg-amber-900/40 text-amber-600 dark:text-amber-400">
          <ShieldAlert className="h-6 w-6" />
        </div>
      </div>

      {/* Warehouse & Cold Storage Vaults */}
      <div className="p-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
            Active Warehouses & Vaults
          </p>
          <p className="text-2xl font-bold text-slate-900 dark:text-white mt-1">
            {metrics.activeWarehousesCount} Locations
          </p>
          <div className="flex items-center gap-1 text-xs text-sky-600 dark:text-sky-400 font-semibold mt-2">
            <ThermometerSnowflake className="h-3.5 w-3.5" />
            <span>2-8°C Vault Active (Wh-02)</span>
          </div>
        </div>
        <div className="p-3 rounded-xl bg-sky-50 dark:bg-sky-950/50 text-sky-600 dark:text-sky-400">
          <Warehouse className="h-6 w-6" />
        </div>
      </div>

      {/* Active Deliveries & Order Queue */}
      <div className="p-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
            Active Order Queue
          </p>
          <p className="text-2xl font-bold text-slate-900 dark:text-white mt-1">
            {metrics.totalOrdersCount} Total Orders
          </p>
          <div className="flex items-center gap-2 mt-2 text-xs">
            <span className="text-orange-600 dark:text-orange-400 font-semibold bg-orange-50 dark:bg-orange-950/50 px-2 py-0.5 rounded-md flex items-center gap-1">
              <Truck className="h-3 w-3" /> {metrics.activeDeliveriesCount} In Transit
            </span>
            <span className="text-amber-600 dark:text-amber-400 font-semibold bg-amber-50 dark:bg-amber-950/50 px-2 py-0.5 rounded-md">
              {metrics.pendingOrdersCount} Pending
            </span>
          </div>
        </div>
        <div className="p-3 rounded-xl bg-orange-50 dark:bg-orange-950/50 text-orange-600 dark:text-orange-400">
          <ShoppingCart className="h-6 w-6" />
        </div>
      </div>

    </div>
  );
};
