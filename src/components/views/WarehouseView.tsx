import React, { useState } from 'react';
import { Warehouse, StockTransfer, Product, Batch } from '../../types';
import { Warehouse as WarehouseIcon, ArrowRightLeft, ThermometerSnowflake, ShieldCheck, Check, Plus, AlertCircle, X } from 'lucide-react';

interface WarehouseViewProps {
  warehouses: Warehouse[];
  transfers: StockTransfer[];
  products: Product[];
  batches: Batch[];
  onCreateTransfer: (transferData: any) => Promise<void>;
}

export const WarehouseView: React.FC<WarehouseViewProps> = ({
  warehouses,
  transfers,
  products,
  batches,
  onCreateTransfer,
}) => {
  const [showTransferModal, setShowTransferModal] = useState(false);
  const [fromWh, setFromWh] = useState(warehouses[0]?.id || '');
  const [toWh, setToWh] = useState(warehouses[1]?.id || '');
  const [selectedProductId, setSelectedProductId] = useState(products[0]?.id || '');
  const [selectedBatchId, setSelectedBatchId] = useState('');
  const [quantity, setQuantity] = useState(50);
  const [notes, setNotes] = useState('');

  // Available batches for selected product and source warehouse
  const availableBatches = batches.filter(b => b.productId === selectedProductId && b.warehouseId === fromWh && b.quantityAvailable > 0);

  const handleTransferSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const batchToUse = selectedBatchId || availableBatches[0]?.id;
    if (!batchToUse) {
      alert('No available batch in source warehouse for transfer');
      return;
    }

    await onCreateTransfer({
      fromWarehouseId: fromWh,
      toWarehouseId: toWh,
      productId: selectedProductId,
      batchId: batchToUse,
      quantity,
      notes,
    });

    setShowTransferModal(false);
  };

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <WarehouseIcon className="h-6 w-6 text-sky-600 dark:text-sky-400" />
            Multi-Warehouse Logistics & Cold-Chain Vaults
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Cross-docking, Inter-warehouse stock transfers, and 2-8°C vault telemetry
          </p>
        </div>

        <button
          onClick={() => setShowTransferModal(true)}
          className="px-4 py-2 rounded-xl bg-sky-600 hover:bg-sky-500 text-white font-bold text-xs flex items-center gap-2 shadow-md shadow-sky-600/20"
        >
          <ArrowRightLeft className="h-4 w-4" /> Request Stock Transfer
        </button>
      </div>

      {/* Warehouse Logistics Network Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {warehouses.map(w => {
          const warehouseBatches = batches.filter(b => b.warehouseId === w.id);
          const totalUnits = warehouseBatches.reduce((acc, b) => acc + b.quantityOnHand, 0);

          return (
            <div key={w.id} className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[10px] font-mono font-bold text-slate-400">{w.code}</span>
                  <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300">
                    {w.status}
                  </span>
                </div>

                <h3 className="font-bold text-sm text-slate-900 dark:text-white">{w.name}</h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">{w.address}, {w.city}</p>

                {w.tempControl === 'COLD_CHAIN_2_8C' && (
                  <div className="mt-3 p-2 rounded-xl bg-sky-50 dark:bg-sky-950/60 border border-sky-200 dark:border-sky-800 text-sky-900 dark:text-sky-200 text-xs flex items-center gap-2">
                    <ThermometerSnowflake className="h-4 w-4 text-sky-500" />
                    <div>
                      <span className="font-bold block text-[11px]">Cold Vault Active</span>
                      <span className="text-[10px] text-sky-600 dark:text-sky-400">Sensor: +3.8°C (Optimal)</span>
                    </div>
                  </div>
                )}
              </div>

              <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs">
                <div>
                  <span className="text-[10px] text-slate-400 block">Total Stock Units</span>
                  <span className="font-bold text-slate-900 dark:text-white">{totalUnits.toLocaleString()}</span>
                </div>
                <div className="text-right">
                  <span className="text-[10px] text-slate-400 block">Facility Area</span>
                  <span className="font-bold text-slate-700 dark:text-slate-300">{w.capacitySqFt.toLocaleString()} sq.ft</span>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      {/* Inter-Warehouse Stock Transfer Ledger */}
      <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
        <h3 className="font-bold text-base text-slate-900 dark:text-white mb-4">
          Inter-Warehouse Stock Transfer Log
        </h3>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700 dark:text-slate-300">
            <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider text-[11px] border-b border-slate-200 dark:border-slate-800">
              <tr>
                <th className="px-4 py-3">Transfer #</th>
                <th className="px-4 py-3">Source Warehouse</th>
                <th className="px-4 py-3">Destination Warehouse</th>
                <th className="px-4 py-3">Product & Batch #</th>
                <th className="px-4 py-3">Quantity</th>
                <th className="px-4 py-3">Requested By</th>
                <th className="px-4 py-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {transfers.map(t => (
                <tr key={t.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors">
                  <td className="px-4 py-3 font-mono font-bold text-sky-600 dark:text-sky-400">
                    {t.transferNumber}
                  </td>
                  <td className="px-4 py-3 font-medium text-slate-900 dark:text-white">
                    {t.fromWarehouseName}
                  </td>
                  <td className="px-4 py-3 font-medium text-slate-900 dark:text-white">
                    {t.toWarehouseName}
                  </td>
                  <td className="px-4 py-3">
                    <span className="font-bold block">{t.productName}</span>
                    <span className="text-[10px] font-mono text-slate-400">Batch: {t.batchNumber}</span>
                  </td>
                  <td className="px-4 py-3 font-bold text-slate-900 dark:text-white">
                    {t.quantity} Units
                  </td>
                  <td className="px-4 py-3 text-slate-500">
                    {t.requestedBy}
                  </td>
                  <td className="px-4 py-3">
                    <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 inline-flex items-center gap-1">
                      <Check className="h-3 w-3" /> {t.status}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* STOCK TRANSFER MODAL */}
      {showTransferModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl relative">
            <button onClick={() => setShowTransferModal(false)} className="absolute top-4 right-4 text-slate-400">
              <X className="h-5 w-5" />
            </button>
            <h3 className="font-bold text-lg text-slate-900 dark:text-white mb-1">Initiate Stock Transfer</h3>
            <p className="text-xs text-slate-500 mb-4">Relocate inventory between medical fulfillment hubs.</p>

            <form onSubmit={handleTransferSubmit} className="space-y-4 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">From Warehouse</label>
                  <select
                    value={fromWh}
                    onChange={(e) => setFromWh(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  >
                    {warehouses.map(w => (
                      <option key={w.id} value={w.id}>{w.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">To Warehouse</label>
                  <select
                    value={toWh}
                    onChange={(e) => setToWh(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  >
                    {warehouses.filter(w => w.id !== fromWh).map(w => (
                      <option key={w.id} value={w.id}>{w.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">Select Medical Product</label>
                <select
                  value={selectedProductId}
                  onChange={(e) => setSelectedProductId(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                >
                  {products.map(p => (
                    <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">Source FEFO Batch</label>
                <select
                  value={selectedBatchId}
                  onChange={(e) => setSelectedBatchId(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                >
                  {availableBatches.length === 0 ? (
                    <option value="">No stock in selected source warehouse</option>
                  ) : (
                    availableBatches.map(b => (
                      <option key={b.id} value={b.id}>
                        Batch #{b.batchNumber} (Avail: {b.quantityAvailable} units | Exp: {b.expiryDate})
                      </option>
                    ))
                  )}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">Transfer Quantity</label>
                  <input
                    type="number"
                    min="1"
                    value={quantity}
                    onChange={(e) => setQuantity(Number(e.target.value))}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">Transfer Reason / Notes</label>
                  <input
                    type="text"
                    placeholder="e.g. ICU emergency replenishment"
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div className="pt-2 flex justify-end gap-3">
                <button type="button" onClick={() => setShowTransferModal(false)} className="px-4 py-2 font-semibold text-slate-500">Cancel</button>
                <button type="submit" className="px-5 py-2.5 bg-sky-600 text-white font-bold rounded-xl shadow-md">Execute Transfer</button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};
