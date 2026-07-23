import React, { useState } from 'react';
import { Order, Invoice } from '../../types';
import { ShoppingCart, FileText, CheckCircle2, Truck, Clock, AlertCircle, Printer, X, ShieldCheck } from 'lucide-react';

interface OrdersViewProps {
  orders: Order[];
  invoices: Invoice[];
  onUpdateOrderStatus: (id: string, status: string) => Promise<void>;
}

export const OrdersView: React.FC<OrdersViewProps> = ({
  orders,
  invoices,
  onUpdateOrderStatus,
}) => {
  const [selectedOrder, setSelectedOrder] = useState<Order | null>(null);
  const [showInvoiceModal, setShowInvoiceModal] = useState<Invoice | null>(null);

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <ShoppingCart className="h-6 w-6 text-indigo-600 dark:text-indigo-400" />
          B2B Wholesale & B2C Order Management System
        </h2>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          State-machine order lifecycle, tax invoices, and credit limit validation
        </p>
      </div>

      {/* Orders Table */}
      <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs text-slate-700 dark:text-slate-300">
            <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-500 dark:text-slate-400 font-bold uppercase tracking-wider text-[11px] border-b border-slate-200 dark:border-slate-800">
              <tr>
                <th className="px-4 py-3">Order # & Type</th>
                <th className="px-4 py-3">Customer & PO</th>
                <th className="px-4 py-3">Items Count</th>
                <th className="px-4 py-3">Subtotal & Tax</th>
                <th className="px-4 py-3">Total Amount</th>
                <th className="px-4 py-3">Payment</th>
                <th className="px-4 py-3">Order Status</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {orders.map(o => {
                const inv = invoices.find(i => i.orderId === o.id);
                return (
                  <tr key={o.id} className="hover:bg-slate-50/80 dark:hover:bg-slate-800/50 transition-colors">
                    <td className="px-4 py-3">
                      <span className="font-mono font-bold text-slate-900 dark:text-white block">{o.orderNumber}</span>
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        o.orderType === 'B2B' ? 'bg-indigo-100 text-indigo-700 dark:bg-indigo-950 dark:text-indigo-300' : 'bg-teal-100 text-teal-700 dark:bg-teal-950 dark:text-teal-300'
                      }`}>
                        {o.orderType}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="font-bold text-slate-900 dark:text-white block">{o.customerName}</span>
                      {o.poNumber && <span className="text-[10px] text-indigo-500 font-mono font-semibold">PO: {o.poNumber}</span>}
                    </td>
                    <td className="px-4 py-3 font-semibold text-slate-800 dark:text-slate-200">
                      {o.items.length} Line Items
                    </td>
                    <td className="px-4 py-3">
                      <span className="block">${o.subtotal.toFixed(2)}</span>
                      <span className="text-[10px] text-slate-400">+ Tax ${o.taxAmount.toFixed(2)}</span>
                    </td>
                    <td className="px-4 py-3 font-extrabold text-slate-900 dark:text-white text-sm">
                      ${o.totalAmount.toFixed(2)}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        o.paymentStatus === 'PAID' ? 'bg-emerald-100 text-emerald-800' : o.paymentStatus === 'CREDIT_APPROVED' ? 'bg-indigo-100 text-indigo-800' : 'bg-amber-100 text-amber-800'
                      }`}>
                        {o.paymentStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="px-2.5 py-1 rounded-full text-[10px] font-bold bg-teal-50 text-teal-700 border border-teal-200 dark:bg-teal-950 dark:text-teal-300">
                        {o.orderStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right space-x-2">
                      {o.orderStatus === 'PENDING_APPROVAL' && (
                        <button
                          onClick={() => onUpdateOrderStatus(o.id, 'APPROVED')}
                          className="px-2.5 py-1 rounded bg-teal-600 hover:bg-teal-500 text-white font-bold text-[11px]"
                        >
                          Approve B2B
                        </button>
                      )}
                      {o.orderStatus === 'APPROVED' && (
                        <button
                          onClick={() => onUpdateOrderStatus(o.id, 'DISPATCHED')}
                          className="px-2.5 py-1 rounded bg-indigo-600 hover:bg-indigo-500 text-white font-bold text-[11px]"
                        >
                          Dispatch
                        </button>
                      )}
                      {inv && (
                        <button
                          onClick={() => setShowInvoiceModal(inv)}
                          className="px-2 py-1 rounded bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-semibold text-[11px] inline-flex items-center gap-1"
                        >
                          <FileText className="h-3 w-3" /> Invoice
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* TAX INVOICE PRINTABLE MODAL */}
      {showInvoiceModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white text-slate-900 rounded-2xl max-w-2xl w-full p-8 shadow-2xl relative max-h-[90vh] overflow-y-auto font-sans">
            <button onClick={() => setShowInvoiceModal(null)} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600">
              <X className="h-5 w-5" />
            </button>

            {/* Invoice Header */}
            <div className="flex items-start justify-between border-b pb-6">
              <div>
                <h1 className="text-2xl font-black text-teal-700 tracking-tight">MedSupply Healthcare Ltd.</h1>
                <p className="text-xs text-slate-500 mt-1">
                  1200 Logistics Blvd, Suite 400 • Chicago, IL 60601<br />
                  GSTIN: 27MEDSUPPLY99001Z • License: DL-MH-2026-HQ
                </p>
              </div>
              <div className="text-right">
                <span className="px-3 py-1 rounded bg-teal-100 text-teal-800 font-bold text-xs">OFFICIAL TAX INVOICE</span>
                <p className="font-mono font-bold text-base mt-2 text-slate-900">{showInvoiceModal.invoiceNumber}</p>
                <p className="text-xs text-slate-500">Date: {showInvoiceModal.pdfGeneratedAt.split('T')[0]}</p>
              </div>
            </div>

            {/* Billed To */}
            <div className="grid grid-cols-2 gap-4 py-4 text-xs">
              <div>
                <span className="font-bold text-slate-400 uppercase tracking-wider block text-[10px]">Billed To (B2B Client)</span>
                <p className="font-bold text-slate-900 text-sm mt-0.5">{showInvoiceModal.customerName}</p>
                <p className="text-slate-600">GSTIN: {showInvoiceModal.gstin}</p>
                <p className="text-slate-600">Payment Term: NET-30</p>
              </div>
              <div className="text-right">
                <span className="font-bold text-slate-400 uppercase tracking-wider block text-[10px]">Order References</span>
                <p className="font-mono font-bold text-slate-900">{showInvoiceModal.orderNumber}</p>
                <p className="text-slate-600">Due Date: {showInvoiceModal.paymentDueDate.split('T')[0]}</p>
              </div>
            </div>

            {/* Tax Breakdown */}
            <div className="border-t border-b py-4 space-y-2 text-xs">
              <div className="flex justify-between font-semibold">
                <span>Medical Subtotal</span>
                <span>${showInvoiceModal.subtotal.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>CGST (6%)</span>
                <span>${showInvoiceModal.cgst.toFixed(2)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>SGST (6%)</span>
                <span>${showInvoiceModal.sgst.toFixed(2)}</span>
              </div>
              <div className="flex justify-between font-extrabold text-base pt-2 border-t text-slate-900">
                <span>Total Amount Due</span>
                <span>${showInvoiceModal.totalAmount.toFixed(2)}</span>
              </div>
            </div>

            {/* Footer Buttons */}
            <div className="mt-6 flex items-center justify-between text-xs">
              <span className="text-slate-400 italic">Digitally Verified Tax Document • Java 21 Engine</span>
              <button
                onClick={() => window.print()}
                className="px-4 py-2 rounded-xl bg-slate-900 text-white font-bold flex items-center gap-2 hover:bg-slate-800"
              >
                <Printer className="h-4 w-4" /> Print Tax Invoice
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
};
