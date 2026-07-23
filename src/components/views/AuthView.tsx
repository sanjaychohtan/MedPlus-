import React, { useState } from 'react';
import { User, UserRole } from '../../types';
import { api, setAuthToken } from '../../lib/api';
import { Hospital, ShieldCheck, Mail, Lock, User as UserIcon, Phone, FileText, CheckCircle2, AlertCircle, KeyRound, ArrowLeft } from 'lucide-react';

interface AuthViewProps {
  onSuccess: (user: User) => void;
}

type AuthMode = 'login' | 'register' | 'forgot' | 'otp' | 'reset';

export const AuthView: React.FC<AuthViewProps> = ({ onSuccess }) => {
  const [mode, setMode] = useState<AuthMode>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<UserRole>('B2C_CUSTOMER');
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');
  const [otpCode, setOtpCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Hospital Specific Register Fields
  const [licenseNumber, setLicenseNumber] = useState('');
  const [gstin, setGstin] = useState('');
  const [address, setAddress] = useState('');
  const [city, setCity] = useState('');
  const [stateField, setStateField] = useState('');
  const [pincode, setPincode] = useState('');

  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [infoMsg, setInfoMsg] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg('');
    try {
      const res = await api.login(email, role);
      setAuthToken(res.token);
      onSuccess(res.user);
    } catch (err: any) {
      setErrorMsg(err.message || 'Login failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg('');
    try {
      const regData = {
        name,
        email,
        role,
        phone,
        ...(role === 'B2B_CUSTOMER' ? {
          licenseNumber,
          gstin,
          address,
          city,
          state: stateField,
          pincode,
        } : {})
      };
      const res = await api.register(regData);
      setAuthToken(res.token);
      onSuccess(res.user);
    } catch (err: any) {
      setErrorMsg(err.message || 'Registration failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg('');
    try {
      const res = await api.forgotPassword(email);
      setInfoMsg(res.message);
      setMode('otp');
    } catch (err: any) {
      setErrorMsg(err.message || 'Email lookup failed.');
    } finally {
      setLoading(false);
    }
  };

  const handleVerifyOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg('');
    try {
      await api.verifyOtp(email, otpCode);
      setInfoMsg('Code verified. Set your new secure password.');
      setMode('reset');
    } catch (err: any) {
      setErrorMsg(err.message || 'Invalid OTP code.');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setErrorMsg('Passwords do not match');
      return;
    }
    setLoading(true);
    setErrorMsg('');
    try {
      await api.resetPassword(email, newPassword);
      setInfoMsg('Password reset successfully! Please sign in with your new credentials.');
      setMode('login');
      setPassword('');
    } catch (err: any) {
      setErrorMsg(err.message || 'Reset failed.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-[85vh] flex items-center justify-center py-10 px-4">
      <div className="max-w-md w-full rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-xl overflow-hidden p-8 space-y-6">
        
        {/* Logo / Header Banner */}
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 rounded-2xl bg-teal-500/10 text-teal-600 dark:text-teal-400">
            <Hospital className="h-10 w-10" />
          </div>
          <h2 className="text-2xl font-black tracking-tight text-slate-900 dark:text-white">
            MedSupply Enterprise
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            Secure Healthcare Logistics & FEFO Inventory Gateway
          </p>
        </div>

        {/* Global Notifications */}
        {errorMsg && (
          <div className="p-3 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 text-red-950 dark:text-red-200 text-xs flex items-center gap-2">
            <AlertCircle className="h-4 w-4 text-red-500 flex-shrink-0" />
            <span className="font-semibold">{errorMsg}</span>
          </div>
        )}

        {infoMsg && (
          <div className="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-emerald-950 dark:text-emerald-200 text-xs flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-emerald-500 flex-shrink-0" />
            <span className="font-semibold">{infoMsg}</span>
          </div>
        )}

        {/* LOGIN MODE */}
        {mode === 'login' && (
          <form onSubmit={handleLogin} className="space-y-4 text-xs">
            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">Sign-in Role</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
                className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-semibold"
              >
                <option value="SUPER_ADMIN">Super Administrator</option>
                <option value="ADMIN">Company Administrator</option>
                <option value="WAREHOUSE_STAFF">Warehouse Operator</option>
                <option value="SALESMAN">Sales Executive</option>
                <option value="DELIVERY_BOY">Last-Mile Delivery Boy</option>
                <option value="B2B_CUSTOMER">B2B Institutional Hospital</option>
                <option value="B2C_CUSTOMER">B2C Retail Patient</option>
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">Email Address</label>
              <div className="relative">
                <Mail className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="email"
                  required
                  placeholder="e.g. administrator@medsupply.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <div className="flex justify-between items-center">
                <label className="block font-bold text-slate-700 dark:text-slate-300">Password</label>
                <button
                  type="button"
                  onClick={() => setMode('forgot')}
                  className="text-teal-600 dark:text-teal-400 font-bold hover:underline"
                >
                  Forgot Password?
                </button>
              </div>
              <div className="relative">
                <Lock className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="password"
                  placeholder="••••••••"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold transition-all shadow-md shadow-teal-600/20 flex items-center justify-center gap-1.5 text-xs"
            >
              {loading ? 'Authenticating...' : 'Sign In to Portal'}
            </button>

            <div className="text-center pt-2">
              <span className="text-slate-500">New B2B or B2C Partner? </span>
              <button
                type="button"
                onClick={() => setMode('register')}
                className="text-teal-600 dark:text-teal-400 font-bold hover:underline"
              >
                Create Account
              </button>
            </div>
          </form>
        )}

        {/* REGISTER MODE */}
        {mode === 'register' && (
          <form onSubmit={handleRegister} className="space-y-4 text-xs max-h-[60vh] overflow-y-auto pr-1">
            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">Account Type</label>
              <select
                value={role}
                onChange={(e) => setRole(e.target.value as UserRole)}
                className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-semibold"
              >
                <option value="B2C_CUSTOMER">B2C Retail Patient</option>
                <option value="B2B_CUSTOMER">B2B Institutional Hospital</option>
                <option value="SALESMAN">Sales Executive</option>
                <option value="WAREHOUSE_STAFF">Warehouse Operator</option>
              </select>
            </div>

            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">Full Name / Clinic Name</label>
              <div className="relative">
                <UserIcon className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="text"
                  required
                  placeholder="e.g. John Doe / Apollo Labs"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">Email Address</label>
              <div className="relative">
                <Mail className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="email"
                  required
                  placeholder="e.g. health@clinic.org"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">Contact Number</label>
              <div className="relative">
                <Phone className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="tel"
                  required
                  placeholder="+1 (555)..."
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>
            </div>

            {/* B2B Customer Fields */}
            {role === 'B2B_CUSTOMER' && (
              <div className="space-y-3 pt-2 border-t border-slate-200 dark:border-slate-800">
                <h4 className="font-bold text-slate-400 uppercase tracking-wider text-[10px]">Hospital & Compliance Verification</h4>
                
                <div className="grid grid-cols-2 gap-2">
                  <div className="space-y-1.5">
                    <label className="block font-bold text-slate-700 dark:text-slate-300">Drug License No</label>
                    <input
                      type="text"
                      required
                      placeholder="DL-XX-..."
                      value={licenseNumber}
                      onChange={(e) => setLicenseNumber(e.target.value)}
                      className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-mono"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="block font-bold text-slate-700 dark:text-slate-300">GSTIN Tax ID</label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. 27AAAAA..."
                      value={gstin}
                      onChange={(e) => setGstin(e.target.value)}
                      className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white font-mono"
                    />
                  </div>
                </div>

                <div className="space-y-1.5">
                  <label className="block font-bold text-slate-700 dark:text-slate-300">Street Address</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. 100 Hospital Parkway"
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                  />
                </div>

                <div className="grid grid-cols-3 gap-2">
                  <div className="space-y-1.5">
                    <label className="block font-bold text-slate-700 dark:text-slate-300">City</label>
                    <input
                      type="text"
                      required
                      placeholder="Chicago"
                      value={city}
                      onChange={(e) => setCity(e.target.value)}
                      className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="block font-bold text-slate-700 dark:text-slate-300">State</label>
                    <input
                      type="text"
                      required
                      placeholder="IL"
                      value={stateField}
                      onChange={(e) => setStateField(e.target.value)}
                      className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                    />
                  </div>
                  <div className="space-y-1.5">
                    <label className="block font-bold text-slate-700 dark:text-slate-300">Pincode</label>
                    <input
                      type="text"
                      required
                      placeholder="60601"
                      value={pincode}
                      onChange={(e) => setPincode(e.target.value)}
                      className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                    />
                  </div>
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold transition-all shadow-md shadow-teal-600/20 flex items-center justify-center gap-1.5 text-xs mt-2"
            >
              {loading ? 'Creating Account...' : 'Register Corporate Account'}
            </button>

            <button
              type="button"
              onClick={() => setMode('login')}
              className="w-full py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-all flex items-center justify-center gap-1 text-xs"
            >
              <ArrowLeft className="h-4 w-4" /> Back to Sign In
            </button>
          </form>
        )}

        {/* FORGOT PASSWORD MODE */}
        {mode === 'forgot' && (
          <form onSubmit={handleForgotPassword} className="space-y-4 text-xs">
            <p className="text-slate-500 text-center text-xs">
              Enter your email address below, and we will send you a secure OTP authorization code to verify identity and reset password.
            </p>

            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">Verification Email</label>
              <div className="relative">
                <Mail className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="email"
                  required
                  placeholder="e.g. superadmin@medsupply.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold transition-all shadow-md shadow-teal-600/20 flex items-center justify-center gap-1.5 text-xs"
            >
              {loading ? 'Sending OTP...' : 'Send Authorization OTP'}
            </button>

            <button
              type="button"
              onClick={() => setMode('login')}
              className="w-full py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-all flex items-center justify-center gap-1 text-xs"
            >
              <ArrowLeft className="h-4 w-4" /> Cancel & Back
            </button>
          </form>
        )}

        {/* OTP VERIFICATION MODE */}
        {mode === 'otp' && (
          <form onSubmit={handleVerifyOtp} className="space-y-4 text-xs">
            <div className="p-3 rounded-xl bg-sky-50 dark:bg-sky-950/40 border border-sky-200 dark:border-sky-800/60 space-y-1.5">
              <div className="flex items-center gap-2 font-bold text-xs text-sky-900 dark:text-sky-200">
                <KeyRound className="h-4 w-4 text-sky-500" />
                <span>Authorization OTP Sent!</span>
              </div>
              <p className="text-[11px] text-slate-600 dark:text-slate-400">
                An authorization code was dispatched to {email}.<br />
                Demo Verification Code: <strong className="font-mono text-sky-600 dark:text-sky-400">1234</strong>
              </p>
            </div>

            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">Enter Verification Code</label>
              <input
                type="text"
                required
                maxLength={4}
                placeholder="Enter 4-digit code"
                value={otpCode}
                onChange={(e) => setOtpCode(e.target.value)}
                className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 font-mono text-center text-lg font-bold tracking-widest text-slate-900 dark:text-white"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold transition-all shadow-md shadow-teal-600/20 flex items-center justify-center gap-1.5 text-xs"
            >
              {loading ? 'Verifying...' : 'Verify Secure Code'}
            </button>

            <button
              type="button"
              onClick={() => setMode('forgot')}
              className="w-full py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 font-semibold hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-all flex items-center justify-center gap-1 text-xs"
            >
              <ArrowLeft className="h-4 w-4" /> Re-enter Email
            </button>
          </form>
        )}

        {/* RESET PASSWORD MODE */}
        {mode === 'reset' && (
          <form onSubmit={handleResetPassword} className="space-y-4 text-xs">
            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">New Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="password"
                  required
                  placeholder="At least 8 characters"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label className="block font-bold text-slate-700 dark:text-slate-300">Confirm New Password</label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 h-4 w-4 text-slate-400" />
                <input
                  type="password"
                  required
                  placeholder="Confirm new password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full pl-9 pr-3 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 rounded-xl bg-teal-600 hover:bg-teal-500 text-white font-bold transition-all shadow-md shadow-teal-600/20 flex items-center justify-center gap-1.5 text-xs"
            >
              {loading ? 'Updating Password...' : 'Reset & Save Password'}
            </button>
          </form>
        )}

      </div>
    </div>
  );
};
