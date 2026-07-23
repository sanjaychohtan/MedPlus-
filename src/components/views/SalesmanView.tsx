import React, { useState } from 'react';
import { SalesmanLead } from '../../types';
import { UserCheck, Target, Plus, Phone, Building, DollarSign, CheckCircle2, X } from 'lucide-react';

interface SalesmanViewProps {
  leads: SalesmanLead[];
  onCreateLead: (leadData: any) => Promise<void>;
}

export const SalesmanView: React.FC<SalesmanViewProps> = ({
  leads,
  onCreateLead,
}) => {
  const [showAddLeadModal, setShowAddLeadModal] = useState(false);
  const [pharmacyName, setPharmacyName] = useState('');
  const [contactPerson, setContactPerson] = useState('');
  const [phone, setPhone] = useState('');
  const [city, setCity] = useState('Chicago');
  const [estimatedVal, setEstimatedVal] = useState(15000);

  const totalPipeline = leads.reduce((acc, l) => acc + l.estimatedMonthlyValue, 0);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await onCreateLead({
      pharmacyName,
      contactPerson,
      phone,
      city,
      estimatedMonthlyValue: Number(estimatedVal),
    });
    setShowAddLeadModal(false);
    setPharmacyName('');
  };

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
            <UserCheck className="h-6 w-6 text-emerald-600 dark:text-emerald-400" />
            Sales Executive Portal & B2B Client CRM
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Pharmacy account onboarding, commission tracking, and order placement on behalf of clients
          </p>
        </div>

        <button
          onClick={() => setShowAddLeadModal(true)}
          className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white font-bold text-xs flex items-center gap-2 shadow-md shadow-emerald-600/20"
        >
          <Plus className="h-4 w-4" /> Add B2B Client Prospect
        </button>
      </div>

      {/* Target Progress Bar */}
      <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block">Monthly Target Progress</span>
          <p className="text-2xl font-extrabold text-slate-900 dark:text-white mt-0.5">
            ${totalPipeline.toLocaleString()} <span className="text-xs text-slate-400 font-normal">/ $100,000 Quota</span>
          </p>
        </div>

        <div className="w-full md:w-64 bg-slate-100 dark:bg-slate-800 rounded-full h-3 overflow-hidden">
          <div className="bg-emerald-500 h-full rounded-full" style={{ width: `${Math.min(100, (totalPipeline / 100000) * 100)}%` }} />
        </div>
      </div>

      {/* Lead Pipeline */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {leads.map(l => (
          <div key={l.id} className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm flex flex-col justify-between">
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400 flex items-center gap-1">
                  <Building className="h-3.5 w-3.5" /> {l.city}
                </span>
                <span className={`px-2.5 py-0.5 rounded text-[10px] font-bold ${
                  l.status === 'ONBOARDED' ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'
                }`}>
                  {l.status}
                </span>
              </div>

              <h3 className="font-extrabold text-base text-slate-900 dark:text-white">{l.pharmacyName}</h3>
              <p className="text-xs text-slate-500 mt-1">Contact: {l.contactPerson}</p>
              <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
                <Phone className="h-3 w-3" /> {l.phone}
              </p>
            </div>

            <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs">
              <div>
                <span className="text-[10px] text-slate-400 block">Est. Monthly Vol</span>
                <span className="font-bold text-slate-900 dark:text-white">${l.estimatedMonthlyValue.toLocaleString()}</span>
              </div>
              <span className="text-[10px] text-emerald-600 font-bold bg-emerald-50 dark:bg-emerald-950/50 px-2 py-1 rounded">
                Comm: ${(l.estimatedMonthlyValue * 0.03).toFixed(0)} / mo
              </span>
            </div>
          </div>
        ))}
      </div>

      {/* ADD LEAD MODAL */}
      {showAddLeadModal && (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl relative">
            <button onClick={() => setShowAddLeadModal(false)} className="absolute top-4 right-4 text-slate-400">
              <X className="h-5 w-5" />
            </button>
            <h3 className="font-bold text-lg text-slate-900 dark:text-white mb-1">Add Pharmacy Prospect</h3>
            <p className="text-xs text-slate-500 mb-4">Onboard a new clinic or pharmacy B2B lead.</p>

            <form onSubmit={handleSubmit} className="space-y-3 text-xs">
              <div>
                <label className="block font-bold mb-1">Pharmacy / Clinic Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. St. Jude Clinic"
                  value={pharmacyName}
                  onChange={(e) => setPharmacyName(e.target.value)}
                  className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block font-bold mb-1">Contact Person</label>
                  <input
                    type="text"
                    required
                    placeholder="Dr. Michael"
                    value={contactPerson}
                    onChange={(e) => setContactPerson(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-bold mb-1">Phone Number</label>
                  <input
                    type="text"
                    required
                    placeholder="+1 (800)..."
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div>
                <label className="block font-bold mb-1">Estimated Monthly Volume ($)</label>
                <input
                  type="number"
                  value={estimatedVal}
                  onChange={(e) => setEstimatedVal(Number(e.target.value))}
                  className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>

              <div className="pt-2 flex justify-end gap-3">
                <button type="button" onClick={() => setShowAddLeadModal(false)} className="px-4 py-2 font-semibold text-slate-500">Cancel</button>
                <button type="submit" className="px-5 py-2.5 bg-emerald-600 text-white font-bold rounded-xl shadow-md">Add Prospect</button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
};
