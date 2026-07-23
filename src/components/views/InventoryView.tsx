import React, { useState } from 'react';
import { Product, Batch, Warehouse, Category, Brand } from '../../types';
import { 
  Pill, Plus, Search, Filter, AlertTriangle, ShieldCheck, 
  ThermometerSnowflake, PackageCheck, Layers, Calendar, CheckCircle2, X
} from 'lucide-react';

interface InventoryViewProps {
  products: Product[];
  batches: Batch[];
  warehouses: Warehouse[];
  categories: Category[];
  brands: Brand[];
  onCreateProduct: (prod: any) => Promise<void>;
  onCreateBatch: (batch: any) => Promise<void>;
  onUpdateBatch: (id: string, updates: any) => Promise<void>;
}

export const InventoryView: React.FC<InventoryViewProps> = ({
  products,
  batches,
  warehouses,
  categories,
  brands,
  onCreateProduct,
  onCreateBatch,
  onUpdateBatch,
}) => {
  const [activeSubTab, setActiveSubTab] = useState<'batches' | 'products'>('batches');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('');
  const [selectedStatus, setSelectedStatus] = useState('');
  const [showAddBatchModal, setShowAddBatchModal] = useState(false);
  const [showAddProductModal, setShowAddProductModal] = useState(false);

  // New Batch Form State
  const [newBatch, setNewBatch] = useState({
    productId: products[0]?.id || '',
    warehouseId: warehouses[0]?.id || '',
    batchNumber: '',
    manufacturingDate: '2025-01-01',
    expiryDate: '2027-01-01',
    mrp: 35.0,
    b2bPrice: 18.5,
    quantityOnHand: 500,
    coldChainMonitored: false,
    tempReadingCelsius: 4.0,
  });

  // New Product Form State
  const [newProduct, setNewProduct] = useState({
    name: '',
    sku: '',
    hsnCode: '30049099',
    categoryId: categories[0]?.id || '',
    brandId: brands[0]?.id || '',
    description: '',
    unitOfMeasure: 'BOX' as const,
    b2cPrice: 30.0,
    b2bPriceTier1: 22.0,
    b2bPriceTier2: 18.0,
    mrp: 38.0,
    taxRatePercent: 12,
    prescriptionRequired: false,
    minStockAlert: 100,
    storageCondition: 'ROOM_TEMP' as const,
    imageUrl: 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500',
  });

  // Filtered Batches
  const filteredBatches = batches.filter(b => {
    const matchesSearch = b.productName?.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          b.batchNumber.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = selectedStatus ? b.status === selectedStatus : true;
    return matchesSearch && matchesStatus;
  });

  // Filtered Products
  const filteredProducts = products.filter(p => {
    const matchesSearch = p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          p.sku.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          p.hsnCode.includes(searchQuery);
    const matchesCat = selectedCategory ? p.categoryId === selectedCategory : true;
    return matchesSearch && matchesCat;
  });

  const handleCreateBatchSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onCreateBatch(newBatch);
    setShowAddBatchModal(false);
  };

  const handleCreateProductSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onCreateProduct(newProduct);
    setShowAddProductModal(false);
  };

  return (
    <div className="space-y-6">
      
      {/* Header & Controls */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <Pill className="h-6 w-6 text-teal-600 dark:text-teal-400" />
            Medical Inventory & FEFO Batch Engine
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            First Expired First Out allocation, cold-chain monitoring, and B2B pricing tiers
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            id="add-batch-button"
            onClick={() => setShowAddBatchModal(true)}
            className="px-4 py-2 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold text-xs flex items-center gap-1.5 shadow-md shadow-teal-600/20"
          >
            <Plus className="h-4 w-4" /> Add FEFO Batch
          </button>
          <button
            id="add-product-button"
            onClick={() => setShowAddProductModal(true)}
            className="px-4 py-2 rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-800 dark:text-slate-200 font-semibold text-xs flex items-center gap-1.5"
          >
            <Plus className="h-4 w-4" /> New Product
          </button>
        </div>
      </div>

      {/* Navigation Sub-Tabs & Filters */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-4 p-3 rounded-2xl bg-slate-100 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800">
        <div className="flex items-center gap-1 w-full md:w-auto">
          <button
            onClick={() => setActiveSubTab('batches')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeSubTab === 'batches'
                ? 'bg-white dark:bg-slate-900 text-teal-700 dark:text-teal-400 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            FEFO Batches ({batches.length})
          </button>
          <button
            onClick={() => setActiveSubTab('products')}
            className={`px-4 py-2 rounded-xl text-xs font-bold transition-all ${
              activeSubTab === 'products'
                ? 'bg-white dark:bg-slate-900 text-teal-700 dark:text-teal-400 shadow-sm'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            Product Catalog ({products.length})
          </button>
        </div>

        {/* Search & Filter Inputs */}
        <div className="flex items-center gap-3 w-full md:w-auto">
          <div className="relative flex-1 md:w-64">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search SKU, Lot #, Name..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-9 pr-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-xs text-slate-800 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-teal-500"
            />
          </div>

          {activeSubTab === 'batches' && (
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-xs text-slate-800 dark:text-slate-100 focus:outline-none"
            >
              <option value="">All Statuses</option>
              <option value="ACTIVE">Active (Safe)</option>
              <option value="NEAR_EXPIRY">Near Expiry (&lt;60d)</option>
              <option value="EXPIRED">Expired</option>
              <option value="QUARANTINED">Quarantined</option>
            </select>
          )}

          {activeSubTab === 'products' && (
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 text-xs text-slate-800 dark:text-slate-100 focus:outline-none"
            >
              <option value="">All Categories</option>
              {categories.map(c => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          )}
        </div>
      </div>

      {/* BATCHES VIEW (FEFO Sorted) */}
      {activeSubTab === 'batches' && (
        <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs text-slate-700 dark:text-slate-300">
              <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider text-[11px] border-b border-slate-200 dark:border-slate-800">
                <tr>
                  <th className="px-4 py-3">FEFO Priority</th>
                  <th className="px-4 py-3">Medical Product & SKU</th>
                  <th className="px-4 py-3">Batch / Lot #</th>
                  <th className="px-4 py-3">Warehouse & Temp</th>
                  <th className="px-4 py-3">Expiry Date</th>
                  <th className="px-4 py-3">On Hand / Available</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                {filteredBatches.map((b, idx) => (
                  <tr key={b.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors">
                    <td className="px-4 py-3 font-mono font-bold text-slate-400">
                      #{idx + 1}
                    </td>
                    <td className="px-4 py-3">
                      <span className="font-bold text-slate-900 dark:text-white block">{b.productName}</span>
                      <span className="text-[10px] text-slate-400 font-mono">{b.productSku}</span>
                    </td>
                    <td className="px-4 py-3 font-mono font-bold text-teal-600 dark:text-teal-400">
                      {b.batchNumber}
                    </td>
                    <td className="px-4 py-3">
                      <span className="block font-medium">{b.warehouseName}</span>
                      {b.coldChainMonitored && (
                        <span className="text-[10px] font-semibold text-sky-600 dark:text-sky-400 flex items-center gap-1">
                          <ThermometerSnowflake className="h-3 w-3" /> {b.tempReadingCelsius}°C Monitored
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`font-semibold flex items-center gap-1 ${
                        b.status === 'NEAR_EXPIRY' ? 'text-amber-600 dark:text-amber-400 font-bold' : b.status === 'EXPIRED' ? 'text-rose-600 font-bold' : ''
                      }`}>
                        <Calendar className="h-3.5 w-3.5" /> {b.expiryDate}
                      </span>
                    </td>
                    <td className="px-4 py-3 font-bold text-slate-900 dark:text-white">
                      {b.quantityOnHand} units
                      <span className="block text-[10px] font-normal text-slate-400">({b.quantityAvailable} free)</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold inline-flex items-center gap-1 ${
                        b.status === 'ACTIVE' 
                          ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/80 dark:text-emerald-300'
                          : b.status === 'NEAR_EXPIRY'
                          ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/80 dark:text-amber-300'
                          : b.status === 'EXPIRED'
                          ? 'bg-rose-100 text-rose-800 dark:bg-rose-950/80 dark:text-rose-300'
                          : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300'
                      }`}>
                        {b.status === 'NEAR_EXPIRY' && <AlertTriangle className="h-3 w-3" />}
                        {b.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => onUpdateBatch(b.id, { quantityOnHand: b.quantityOnHand + 100 })}
                        className="px-2 py-1 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-semibold text-[11px]"
                      >
                        +100 Stock
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* PRODUCTS CATALOG VIEW */}
      {activeSubTab === 'products' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredProducts.map(p => (
            <div key={p.id} className="p-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col justify-between">
              <div>
                <div className="flex items-start gap-3">
                  <img src={p.imageUrl} alt={p.name} className="h-16 w-16 rounded-xl object-cover border border-slate-100 dark:border-slate-800" />
                  <div className="flex-1">
                    <span className="text-[10px] font-bold text-teal-600 dark:text-teal-400 uppercase tracking-wider block">
                      {p.categoryName} • HSN {p.hsnCode}
                    </span>
                    <h3 className="font-bold text-sm text-slate-900 dark:text-white line-clamp-2 mt-0.5">{p.name}</h3>
                    <span className="text-[10px] font-mono text-slate-400 block">{p.sku}</span>
                  </div>
                </div>

                <p className="text-xs text-slate-500 dark:text-slate-400 mt-3 line-clamp-2">{p.description}</p>

                {/* Storage & Prescription Tags */}
                <div className="flex items-center gap-2 mt-3 text-[10px]">
                  {p.prescriptionRequired && (
                    <span className="px-2 py-0.5 rounded font-bold bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300">
                      Rx Required
                    </span>
                  )}
                  <span className={`px-2 py-0.5 rounded font-semibold ${
                    p.storageCondition === 'COLD_CHAIN_2_8C' ? 'bg-sky-100 text-sky-800 dark:bg-sky-950 dark:text-sky-300' : 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
                  }`}>
                    {p.storageCondition}
                  </span>
                </div>
              </div>

              {/* Pricing Tiers Box */}
              <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 grid grid-cols-3 gap-1 text-center text-xs">
                <div className="p-1.5 rounded bg-slate-50 dark:bg-slate-800/60">
                  <span className="text-[10px] text-slate-400 block">Retail (B2C)</span>
                  <span className="font-bold text-slate-900 dark:text-white">${p.b2cPrice.toFixed(2)}</span>
                </div>
                <div className="p-1.5 rounded bg-indigo-50/60 dark:bg-indigo-950/40">
                  <span className="text-[10px] text-indigo-500 font-medium block">B2B Tier 1</span>
                  <span className="font-bold text-indigo-600 dark:text-indigo-400">${p.b2bPriceTier1.toFixed(2)}</span>
                </div>
                <div className="p-1.5 rounded bg-indigo-100/60 dark:bg-indigo-950/80">
                  <span className="text-[10px] text-indigo-600 font-bold block">Hospital Tier 2</span>
                  <span className="font-bold text-indigo-700 dark:text-indigo-300">${p.b2bPriceTier2.toFixed(2)}</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* ADD BATCH MODAL */}
      {showAddBatchModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl relative">
            <button onClick={() => setShowAddBatchModal(false)} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600">
              <X className="h-5 w-5" />
            </button>
            <h3 className="font-bold text-lg text-slate-900 dark:text-white mb-1">Register FEFO Medical Batch</h3>
            <p className="text-xs text-slate-500 mb-4">Enter manufacturing details & expiry dates for automatic FEFO rotation.</p>

            <form onSubmit={handleCreateBatchSubmit} className="space-y-4 text-xs">
              <div>
                <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">Select Product</label>
                <select
                  value={newBatch.productId}
                  onChange={(e) => setNewBatch({ ...newBatch, productId: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                >
                  {products.map(p => (
                    <option key={p.id} value={p.id}>{p.name} ({p.sku})</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">Batch / Lot Number</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. BTC-2026-9001"
                    value={newBatch.batchNumber}
                    onChange={(e) => setNewBatch({ ...newBatch, batchNumber: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">Warehouse Location</label>
                  <select
                    value={newBatch.warehouseId}
                    onChange={(e) => setNewBatch({ ...newBatch, warehouseId: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  >
                    {warehouses.map(w => (
                      <option key={w.id} value={w.id}>{w.name}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">Manufacturing Date</label>
                  <input
                    type="date"
                    value={newBatch.manufacturingDate}
                    onChange={(e) => setNewBatch({ ...newBatch, manufacturingDate: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-bold text-amber-600 dark:text-amber-400 mb-1">Expiry Date (FEFO)</label>
                  <input
                    type="date"
                    value={newBatch.expiryDate}
                    onChange={(e) => setNewBatch({ ...newBatch, expiryDate: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-amber-300 dark:border-amber-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-bold"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">QuantityOnHand</label>
                  <input
                    type="number"
                    value={newBatch.quantityOnHand}
                    onChange={(e) => setNewBatch({ ...newBatch, quantityOnHand: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">B2B Price ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={newBatch.b2bPrice}
                    onChange={(e) => setNewBatch({ ...newBatch, b2bPrice: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">MRP ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={newBatch.mrp}
                    onChange={(e) => setNewBatch({ ...newBatch, mrp: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800 flex items-center justify-between">
                <div>
                  <span className="font-bold text-slate-900 dark:text-white block">Cold Chain Monitoring (2-8°C)</span>
                  <span className="text-[11px] text-slate-400">Track real-time temperature logs</span>
                </div>
                <input
                  type="checkbox"
                  checked={newBatch.coldChainMonitored}
                  onChange={(e) => setNewBatch({ ...newBatch, coldChainMonitored: e.target.checked })}
                  className="h-5 w-5 text-teal-600 rounded"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowAddBatchModal(false)}
                  className="px-4 py-2 rounded-xl text-slate-600 dark:text-slate-400 font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold shadow-md shadow-teal-600/20"
                >
                  Save FEFO Batch
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ADD PRODUCT MODAL */}
      {showAddProductModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl relative max-h-[90vh] overflow-y-auto">
            <button onClick={() => setShowAddProductModal(false)} className="absolute top-4 right-4 text-slate-400">
              <X className="h-5 w-5" />
            </button>
            <h3 className="font-bold text-lg text-slate-900 dark:text-white mb-1">Add New Medical Product</h3>
            <p className="text-xs text-slate-500 mb-4">Define HSN codes, tax rates, and B2B pricing tiers.</p>

            <form onSubmit={handleCreateProductSubmit} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">Product Title</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Paracetamol 650mg Infusion"
                  value={newProduct.name}
                  onChange={(e) => setNewProduct({ ...newProduct, name: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">SKU Code</label>
                  <input
                    type="text"
                    required
                    placeholder="PCM-650-INF"
                    value={newProduct.sku}
                    onChange={(e) => setNewProduct({ ...newProduct, sku: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">HSN Tax Code</label>
                  <input
                    type="text"
                    required
                    value={newProduct.hsnCode}
                    onChange={(e) => setNewProduct({ ...newProduct, hsnCode: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-2">
                <div>
                  <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">Retail B2C ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={newProduct.b2cPrice}
                    onChange={(e) => setNewProduct({ ...newProduct, b2cPrice: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">B2B Tier 1 ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={newProduct.b2bPriceTier1}
                    onChange={(e) => setNewProduct({ ...newProduct, b2bPriceTier1: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">Hospital Tier 2 ($)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={newProduct.b2bPriceTier2}
                    onChange={(e) => setNewProduct({ ...newProduct, b2bPriceTier2: Number(e.target.value) })}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold mb-1 text-slate-700 dark:text-slate-300">Description</label>
                <textarea
                  rows={2}
                  value={newProduct.description}
                  onChange={(e) => setNewProduct({ ...newProduct, description: e.target.value })}
                  className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>

              <div className="pt-2 flex justify-end gap-3">
                <button type="button" onClick={() => setShowAddProductModal(false)} className="px-4 py-2 font-semibold text-slate-500">Cancel</button>
                <button type="submit" className="px-5 py-2.5 bg-teal-600 text-white font-bold rounded-xl">Save Product</button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};
