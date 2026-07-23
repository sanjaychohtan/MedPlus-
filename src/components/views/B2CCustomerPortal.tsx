import React, { useState } from 'react';
import { Product, Coupon } from '../../types';
import { ShoppingBag, Search, ShieldCheck, Upload, CreditCard, Tag, Check, CheckCircle2 } from 'lucide-react';

interface B2CCustomerPortalProps {
  products: Product[];
  cart: { product: Product; quantity: number }[];
  onAddToCart: (product: Product, qty: number) => void;
  onRemoveFromCart: (productId: string) => void;
  onCheckout: (couponCode?: string) => Promise<void>;
}

export const B2CCustomerPortal: React.FC<B2CCustomerPortalProps> = ({
  products,
  cart,
  onAddToCart,
  onRemoveFromCart,
  onCheckout,
}) => {
  const [search, setSearch] = useState('');
  const [selectedRxFile, setSelectedRxFile] = useState<string | null>(null);
  const [couponCode, setCouponCode] = useState('');
  const [discountAmount, setDiscountAmount] = useState(0);
  const [couponMsg, setCouponMsg] = useState('');
  const [isProcessingRazorpay, setIsProcessingRazorpay] = useState(false);
  const [orderComplete, setOrderComplete] = useState(false);

  const filteredProducts = products.filter(p => p.name.toLowerCase().includes(search.toLowerCase()));

  const subtotal = cart.reduce((acc, item) => acc + item.product.b2cPrice * item.quantity, 0);
  const tax = subtotal * 0.08;
  const total = Math.max(0, subtotal + tax - discountAmount);

  const handleApplyCoupon = () => {
    if (couponCode.toUpperCase() === 'MEDSAVE10') {
      const disc = Math.min(subtotal * 0.1, 50);
      setDiscountAmount(disc);
      setCouponMsg('10% Discount Applied!');
    } else {
      setCouponMsg('Invalid promo code. Try MEDSAVE10');
    }
  };

  const handleSimulateRazorpay = async () => {
    if (cart.length === 0) return;
    setIsProcessingRazorpay(true);
    await new Promise(r => setTimeout(r, 1200)); // Simulate Razorpay gateway modal ping
    await onCheckout(couponCode);
    setIsProcessingRazorpay(false);
    setOrderComplete(true);
  };

  return (
    <div className="space-y-6">
      
      {/* Banner */}
      <div className="p-6 rounded-2xl bg-gradient-to-r from-teal-800 to-slate-900 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-teal-300 font-bold text-xs uppercase tracking-wider">
            <ShoppingBag className="h-4 w-4" /> Patient Retail Store & Medicine Delivery
          </div>
          <h1 className="text-2xl font-black mt-1">Order Authentic Medical Supplies Directly</h1>
          <p className="text-xs text-slate-300 mt-0.5">
            Verified cold-chain delivery, OTC medicines, and prescription uploads
          </p>
        </div>

        {/* Prescription Upload Card */}
        <div className="p-3 rounded-xl bg-white/10 border border-white/20 backdrop-blur-md text-xs flex items-center gap-3">
          <Upload className="h-5 w-5 text-teal-300" />
          <div>
            <span className="font-bold block">Upload Prescription (Rx)</span>
            <span className="text-[10px] text-slate-300">Fast 15-min pharmacist verification</span>
          </div>
          <button 
            onClick={() => setSelectedRxFile('rx_prescription_sample.pdf')}
            className="px-3 py-1 rounded-lg bg-teal-400 text-slate-950 font-bold text-[11px]"
          >
            {selectedRxFile ? 'Rx Attached ✓' : 'Attach Rx'}
          </button>
        </div>
      </div>

      {orderComplete && (
        <div className="p-4 rounded-2xl bg-emerald-50 dark:bg-emerald-950/60 border border-emerald-300 text-emerald-900 dark:text-emerald-200 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <CheckCircle2 className="h-6 w-6 text-emerald-600" />
            <div>
              <span className="font-bold text-sm block">Razorpay Payment Authorized ($ {total.toFixed(2)})!</span>
              <span className="text-xs text-slate-600 dark:text-slate-300">Order dispatched for immediate delivery.</span>
            </div>
          </div>
          <button onClick={() => setOrderComplete(false)} className="text-xs font-bold underline">Dismiss</button>
        </div>
      )}

      {/* Catalog & Search */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        <div className="lg:col-span-2 space-y-4">
          <div className="relative">
            <Search className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search OTC medicines, vaccines, gloves, syringes..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2.5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-teal-500"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {filteredProducts.map(p => (
              <div key={p.id} className="p-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col justify-between">
                <div>
                  <img src={p.imageUrl} alt={p.name} className="h-32 w-full rounded-xl object-cover mb-3" />
                  <span className="text-[10px] font-bold text-teal-600 dark:text-teal-400 uppercase tracking-wider block">
                    {p.categoryName}
                  </span>
                  <h3 className="font-bold text-sm text-slate-900 dark:text-white mt-0.5">{p.name}</h3>
                  <p className="text-xs text-slate-500 mt-1 line-clamp-2">{p.description}</p>
                </div>

                <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                  <div>
                    <span className="text-xs font-extrabold text-slate-900 dark:text-white block">${p.b2cPrice.toFixed(2)}</span>
                    <span className="text-[10px] text-slate-400 line-through">MRP ${p.mrp.toFixed(2)}</span>
                  </div>

                  <button
                    onClick={() => onAddToCart(p, 1)}
                    className="px-3.5 py-1.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold text-xs shadow-md shadow-teal-600/20"
                  >
                    Add to Cart
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Patient Cart & Razorpay Checkout */}
        <div className="p-6 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-5 h-fit sticky top-20">
          <h3 className="font-bold text-base text-slate-900 dark:text-white flex items-center gap-2">
            <ShoppingBag className="h-5 w-5 text-teal-600" />
            Your Cart ({cart.length})
          </h3>

          <div className="space-y-3 max-h-56 overflow-y-auto text-xs">
            {cart.length === 0 ? (
              <p className="text-slate-400 italic text-center py-6">Your medical supply cart is empty</p>
            ) : (
              cart.map(item => (
                <div key={item.product.id} className="flex items-center justify-between border-b pb-2 border-slate-100 dark:border-slate-800">
                  <div>
                    <span className="font-bold text-slate-900 dark:text-white block">{item.product.name}</span>
                    <span className="text-[10px] text-slate-400">{item.quantity} x ${item.product.b2cPrice.toFixed(2)}</span>
                  </div>
                  <button onClick={() => onRemoveFromCart(item.product.id)} className="text-rose-500 text-[11px]">Remove</button>
                </div>
              ))
            )}
          </div>

          {/* Coupon Input */}
          <div className="pt-2 border-t border-slate-100 dark:border-slate-800 text-xs">
            <div className="flex items-center gap-2">
              <input
                type="text"
                placeholder="Promo Code (MEDSAVE10)"
                value={couponCode}
                onChange={(e) => setCouponCode(e.target.value)}
                className="flex-1 p-2 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
              />
              <button onClick={handleApplyCoupon} className="px-3 py-2 rounded-xl bg-slate-900 dark:bg-slate-800 text-white font-bold">Apply</button>
            </div>
            {couponMsg && <p className="text-[11px] font-semibold text-teal-600 mt-1">{couponMsg}</p>}
          </div>

          {/* Total */}
          <div className="space-y-1 text-xs border-t pt-3">
            <div className="flex justify-between text-slate-500"><span>Subtotal</span><span>${subtotal.toFixed(2)}</span></div>
            <div className="flex justify-between text-slate-500"><span>Discount</span><span className="text-teal-600 font-bold">-${discountAmount.toFixed(2)}</span></div>
            <div className="flex justify-between font-extrabold text-slate-900 dark:text-white text-base pt-2 border-t"><span>Total</span><span>${total.toFixed(2)}</span></div>
          </div>

          <button
            onClick={handleSimulateRazorpay}
            disabled={cart.length === 0 || isProcessingRazorpay}
            className="w-full py-3 rounded-xl bg-teal-600 hover:bg-teal-500 disabled:opacity-50 text-white font-bold text-xs shadow-lg shadow-teal-600/20 flex items-center justify-center gap-2"
          >
            <CreditCard className="h-4 w-4" />
            {isProcessingRazorpay ? 'Authorizing Razorpay Gateway...' : 'Pay Now via Razorpay'}
          </button>
        </div>

      </div>

    </div>
  );
};
