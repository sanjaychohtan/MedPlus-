import React, { useState } from 'react';
import { DeliveryTask } from '../../types';
import { Truck, Navigation, CheckCircle2, Phone, MapPin, KeyRound, ShieldAlert } from 'lucide-react';

interface DeliveryViewProps {
  deliveries: DeliveryTask[];
  onUpdateDeliveryStatus: (id: string, updates: any) => Promise<void>;
}

export const DeliveryView: React.FC<DeliveryViewProps> = ({
  deliveries,
  onUpdateDeliveryStatus,
}) => {
  const [selectedTask, setSelectedTask] = useState<DeliveryTask | null>(deliveries[0] || null);
  const [otpInput, setOtpInput] = useState('');
  const [errorMsg, setErrorMsg] = useState('');

  const handleVerifyOtpAndDeliver = async () => {
    if (!selectedTask) return;
    try {
      setErrorMsg('');
      await onUpdateDeliveryStatus(selectedTask.id, {
        status: 'DELIVERED',
        otpCode: otpInput,
      });
      alert('Delivery verified successfully! Order marked DELIVERED.');
      setOtpInput('');
    } catch (err: any) {
      setErrorMsg(err.message || 'OTP verification failed');
    }
  };

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div>
        <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <Truck className="h-6 w-6 text-orange-600 dark:text-orange-400" />
          Delivery Dispatch & Cold-Chain Last-Mile GPS Tracking
        </h2>
        <p className="text-xs text-slate-500 dark:text-slate-400">
          Real-time courier GPS positioning, OTP handoff verification, and insulated container monitoring
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Task List */}
        <div className="space-y-3">
          <h3 className="font-bold text-sm text-slate-900 dark:text-white mb-2">Assigned Dispatches</h3>
          {deliveries.map(d => (
            <div
              key={d.id}
              onClick={() => setSelectedTask(d)}
              className={`p-4 rounded-2xl border transition-all cursor-pointer ${
                selectedTask?.id === d.id
                  ? 'border-orange-500 bg-orange-50/30 dark:bg-orange-950/20 shadow-md'
                  : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:border-slate-300'
              }`}
            >
              <div className="flex items-center justify-between mb-1">
                <span className="font-mono font-bold text-xs text-orange-600 dark:text-orange-400">{d.deliveryNumber}</span>
                <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300">
                  {d.status}
                </span>
              </div>
              <p className="font-bold text-sm text-slate-900 dark:text-white">{d.customerName}</p>
              <p className="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-1 mt-1">
                <MapPin className="h-3.5 w-3.5 text-slate-400" /> {d.deliveryAddress}
              </p>
            </div>
          ))}
        </div>

        {/* Dispatch Detail & GPS Simulator */}
        {selectedTask ? (
          <div className="lg:col-span-2 p-6 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-5">
            <div className="flex items-center justify-between border-b pb-4 border-slate-100 dark:border-slate-800">
              <div>
                <span className="text-xs text-slate-400 font-mono">Active Dispatch Task</span>
                <h3 className="font-extrabold text-lg text-slate-900 dark:text-white">{selectedTask.customerName}</h3>
              </div>
              <div className="text-right">
                <span className="text-xs font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/50 px-3 py-1 rounded-lg inline-flex items-center gap-1">
                  <Navigation className="h-3.5 w-3.5" /> ETA {selectedTask.estimatedArrivalMinutes} Mins
                </span>
              </div>
            </div>

            {/* GPS Map Graphic Simulation */}
            <div className="h-48 rounded-2xl bg-slate-950 relative overflow-hidden flex items-center justify-center p-4 border border-slate-800">
              <div className="absolute inset-0 opacity-20 bg-[radial-gradient(#38bdf8_1px,transparent_1px)] [background-size:16px_16px]" />
              <div className="relative text-center space-y-2 z-10">
                <div className="inline-flex p-3 rounded-2xl bg-orange-500 text-white shadow-xl animate-bounce">
                  <Truck className="h-8 w-8" />
                </div>
                <p className="font-mono font-bold text-xs text-sky-400">
                  GPS Latitude: {selectedTask.currentLat.toFixed(4)} | Longitude: {selectedTask.currentLng.toFixed(4)}
                </p>
                <p className="text-slate-400 text-xs">Live Telemetry Route: Logistics Hub -&gt; Receiving Dock</p>
              </div>
            </div>

            {/* Verification & Actions */}
            {selectedTask.status === 'IN_TRANSIT' && (
              <div className="p-4 rounded-xl bg-orange-50 dark:bg-orange-950/30 border border-orange-200 dark:border-orange-800/60 space-y-3">
                <div className="flex items-center gap-2 font-bold text-xs text-orange-900 dark:text-orange-200">
                  <KeyRound className="h-4 w-4 text-orange-600" />
                  <span>Delivery Handover OTP Verification (Required)</span>
                </div>
                <p className="text-xs text-slate-600 dark:text-slate-400">
                  Ask hospital receiving staff for 4-digit security OTP code (Demo OTP: <strong className="font-mono text-orange-600 dark:text-orange-400">{selectedTask.otpCode}</strong>).
                </p>

                {errorMsg && <p className="text-xs text-rose-600 font-bold">{errorMsg}</p>}

                <div className="flex items-center gap-2">
                  <input
                    type="text"
                    maxLength={4}
                    placeholder="Enter 4-digit OTP"
                    value={otpInput}
                    onChange={(e) => setOtpInput(e.target.value)}
                    className="p-2.5 rounded-xl border border-orange-300 dark:border-orange-700 bg-white dark:bg-slate-900 font-mono font-bold text-slate-900 dark:text-white text-sm w-40 text-center tracking-widest"
                  />
                  <button
                    onClick={handleVerifyOtpAndDeliver}
                    className="px-5 py-2.5 rounded-xl bg-orange-600 hover:bg-orange-500 text-white font-bold text-xs shadow-md shadow-orange-600/20"
                  >
                    Verify & Confirm Delivery
                  </button>
                </div>
              </div>
            )}

            {selectedTask.status === 'DELIVERED' && (
              <div className="p-4 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 flex items-center gap-3">
                <CheckCircle2 className="h-6 w-6 text-emerald-600 dark:text-emerald-400" />
                <div>
                  <span className="font-bold text-sm text-emerald-900 dark:text-emerald-200 block">Dispatch Successfully Handed Over</span>
                  <span className="text-xs text-emerald-700 dark:text-emerald-400">Insulated cold tote temperature validated at dock.</span>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="lg:col-span-2 p-12 text-center text-slate-400 font-medium">Select a delivery task to view telemetry</div>
        )}

      </div>

    </div>
  );
};
