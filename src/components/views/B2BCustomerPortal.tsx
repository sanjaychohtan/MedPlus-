import React, { useState } from 'react';
import { User, Product, Batch } from '../../types';
import { Hospital, CreditCard, ShieldCheck, ShoppingCart, Plus, Minus, FileText, CheckCircle2 } from 'lucide-react';

interface B2BCustomerPortalProps {
  currentUser: User;
  products: Product[];
  batches: Batch[];
  onPlaceOrder: (orderData: any) => Promise<void>;
}

export const B2BCustomerPortal: React.FC<B2BCustomerPortalProps> = ({
  currentUser,
  products,
  batches,
  onPlaceOrder,
}) => {
  const [quantities, setQuantities] = useState<Record<string, number>>({});
  const [poNumber, setPoNumber] = useState('PO-APOLLO-2026-102');
  const [poNotes, setPoNotes] = useState('Urgent replenishment for ICU & Emergency Bay');
  const [orderSuccess, setOrderSuccess] = useState(false);

  const handleQtyChange = (productId: string, delta: number) => {
    setQuantities(prev => {
      const current = prev[productId] || 0;
      const next = Math.max(0, current + delta);
      return { ...prev, [productId]: next };
    });
  };

  // Selected Order Items
  const selectedItems = Object.entries(quantities)
    .filter(([_, qty]) => (qty as number) > 0)
    .map(([prodId, qty]) => {
      const prod = products.find(p => p.id === prodId)!;
      const numQty = qty as number;
      // Wholesale pricing: Tier 2 if 100+ units, else Tier 1
      const unitPrice = numQty >= 100 ? prod.b2bPriceTier2 : prod.b2bPriceTier1;
      return {
        productId: prodId,
        quantity: numQty,
        unitPrice,
        total: unitPrice * numQty,
      };
    });

  const subtotal = selectedItems.reduce((acc, item) => acc + item.total, 0);
  const tax = subtotal * 0.12;
  const total = subtotal + tax;

  const creditLimit = currentUser.creditLimit || 150000;
  const usedCredit = currentUser.usedCredit || 42500;
  const availableCredit = Math.max(0, creditLimit - usedCredit);

  const handleExecuteB2BOrder = async () => {
    if (selectedItems.length === 0) {
      alert('Please select at least one wholesale supply line item');
      return;
    }

    if (total > availableCredit) {
      alert(`Order total ($${total.toFixed(2)}) exceeds available credit line ($${availableCredit.toFixed(2)}). Please contact account manager.`);
      return;
    }

    await onPlaceOrder({
      orderType: 'B2B',
      customerId: currentUser.id,
      items: selectedItems,
      paymentMethod: 'CREDIT_TERM',
      poNumber,
      deliveryAddress: currentUser.address || '742 Evergreen Medical Parkway, Chicago IL',
    });

    setOrderSuccess(true);
    setQuantities({});
  };

  return (
    <div className="space-y-6">
      
      {/* Hospital Account Header */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-indigo-950 via-slate-900 to-slate-900 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4 border border-indigo-900/50">
        <div>
          <div className="flex items-center gap-2 text-indigo-400 font-bold text-xs uppercase tracking-wider">
            <Hospital className="h-4 w-4" /> B2B Institutional Hospital Portal
          </div>
          <h1 className="text-2xl font-black mt-1">{currentUser.name}</h1>
          <p className="text-xs text-slate-300 mt-0.5">
            Drug License #: <span className="font-mono text-teal-400 font-bold">{currentUser.licenseNumber || 'DL-MH-994821'}</span> | GSTIN: <span className="font-mono text-teal-400 font-bold">{currentUser.gstin || '27AAAAA0000A1Z5'}</span>
          </p>
        </div>

        {/* Credit Limit Meter */}
        <div className="p-4 rounded-xl bg-indigo-900/40 border border-indigo-800/80 min-w-[260px]">
          <div className="flex items-center justify-between text-xs mb-1">
            <span className="font-semibold text-indigo-200">NET-30 Credit Facility</span>
            <span className="font-mono font-bold text-teal-400">${availableCredit.toLocaleString()} Free</span>
          </div>
          <div className="w-full bg-indigo-950 rounded-full h-2.5 overflow-hidden">
            <div 
              className="bg-teal-400 h-full rounded-full transition-all" 
              style={{ width: `${Math.min(100, (usedCredit / creditLimit) * 100)}%` }} 
            />
          </div>
          <p className="text-[10px] text-slate-400 mt-1">
            Total Facility: ${creditLimit.toLocaleString()} | Used: ${usedCredit.toLocaleString()}
          </p>
        </div>
      </div>

      {orderSuccess && (
        <div className="p-4 rounded-2xl bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-300 dark:border-emerald-800 text-emerald-900 dark:text-emerald-200 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <CheckCircle2 className="h-6 w-6 text-emerald-600" />
            <div>
              <span className="font-bold text-sm block">Purchase Order Approved & Registered!</span>
              <span className="text-xs text-slate-600 dark:text-slate-300">Tax Invoice generated and assigned for warehouse picking.</span>
            </div>
          </div>
          <button onClick={() => setOrderSuccess(false)} className="text-xs font-bold underline">Dismiss</button>
        </div>
      )}

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Quick Wholesale Ordering Sheet */}
        <div className="lg:col-span-2 space-y-4">
          <h3 className="font-bold text-base text-slate-900 dark:text-white">
            Wholesale Supply Catalog & Bulk Matrix
          </h3>

          <div className="grid grid-cols-1 gap-3">
            {products.map(p => {
              const qty = quantities[p.id] || 0;
              const unitPrice = qty >= 100 ? p.b2bPriceTier2 : p.b2bPriceTier1;

              return (
                <div key={p.id} className="p-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex items-center justify-between gap-4">
                  <div className="flex items-center gap-3">
                    <img src={p.imageUrl} alt={p.name} className="h-14 w-14 rounded-xl object-cover border" />
                    <div>
                      <span className="text-[10px] font-mono font-bold text-indigo-600 dark:text-indigo-400">{p.sku}</span>
                      <h4 className="font-bold text-sm text-slate-900 dark:text-white">{p.name}</h4>
                      <p className="text-xs text-slate-500">
                        Tier 1 (10-99): <strong className="text-slate-900 dark:text-white">${p.b2bPriceTier1}</strong> | Hospital (100+): <strong className="text-indigo-600 dark:text-indigo-400">${p.b2bPriceTier2}</strong>
                      </p>
                    </div>
                  </div>

                  {/* Quantity Stepper */}
                  <div className="flex items-center gap-3">
                    <div className="flex items-center gap-1 bg-slate-100 dark:bg-slate-800 p-1 rounded-xl">
                      <button
                        onClick={() => handleQtyChange(p.id, -10)}
                        className="p-1.5 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200"
                      >
                        <Minus className="h-4 w-4" />
                      </button>
                      <input
                        type="number"
                        value={qty}
                        onChange={(e) => setQuantities({ ...quantities, [p.id]: Math.max(0, Number(e.target.value)) })}
                        className="w-16 text-center bg-transparent font-bold text-xs text-slate-900 dark:text-white focus:outline-none"
                      />
                      <button
                        onClick={() => handleQtyChange(p.id, 10)}
                        className="p-1.5 rounded-lg hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200"
                      >
                        <Plus className="h-4 w-4" />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* PO Checkout Summary Card */}
        <div className="p-6 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-5 h-fit sticky top-20">
          <h3 className="font-bold text-base text-slate-900 dark:text-white flex items-center gap-2">
            <FileText className="h-5 w-5 text-indigo-600" />
            Purchase Order Summary
          </h3>

          <div className="space-y-3 text-xs border-b pb-4 border-slate-100 dark:border-slate-800">
            <div>
              <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">Hospital PO Reference #</label>
              <input
                type="text"
                value={poNumber}
                onChange={(e) => setPoNumber(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-mono font-bold"
              />
            </div>

            <div>
              <label className="block font-bold text-slate-700 dark:text-slate-300 mb-1">Department Notes</label>
              <input
                type="text"
                value={poNotes}
                onChange={(e) => setPoNotes(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
              />
            </div>
          </div>

          {/* Selected Lines List */}
          <div className="space-y-2 max-h-48 overflow-y-auto text-xs">
            {selectedItems.length === 0 ? (
              <p className="text-slate-400 italic text-center py-4">No supply items selected yet</p>
            ) : (
              selectedItems.map(item => {
                const prod = products.find(p => p.id === item.productId)!;
                return (
                  <div key={item.productId} className="flex items-center justify-between text-slate-700 dark:text-slate-300">
                    <div>
                      <span className="font-bold block text-slate-900 dark:text-white">{prod.name}</span>
                      <span className="text-[10px] text-slate-400">{item.quantity} units x ${item.unitPrice.toFixed(2)}</span>
                    </div>
                    <span className="font-mono font-bold">${item.total.toFixed(2)}</span>
                  </div>
                );
              })
            )}
          </div>

          <div className="space-y-1.5 text-xs border-t pt-3 border-slate-100 dark:border-slate-800">
            <div className="flex justify-between text-slate-600 dark:text-slate-400">
              <span>Subtotal</span>
              <span>${subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-slate-600 dark:text-slate-400">
              <span>Estimated Tax (12%)</span>
              <span>${tax.toFixed(2)}</span>
            </div>
            <div className="flex justify-between font-extrabold text-slate-900 dark:text-white text-base pt-2 border-t">
              <span>Order Total</span>
              <span>${total.toFixed(2)}</span>
            </div>
          </div>

          <button
            onClick={handleExecuteB2BOrder}
            disabled={selectedItems.length === 0}
            className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white font-bold text-xs shadow-lg shadow-indigo-600/20 transition-all flex items-center justify-center gap-2"
          >
            <ShieldCheck className="h-4 w-4" /> Submit B2B Order (NET-30 Credit)
          </button>
        </div>

      </div>

    </div>
  );
};
