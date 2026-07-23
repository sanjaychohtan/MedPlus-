import React from 'react';
import { User, UserRole } from '../types';
import { 
  Building2, ShieldCheck, Warehouse, UserCheck, Truck, Hospital, User as UserIcon,
  Moon, Sun, ShoppingBag, AlertTriangle, Cpu, CreditCard, ChevronDown, LogOut
} from 'lucide-react';

interface NavbarProps {
  currentUser: User;
  onRoleChange: (role: UserRole) => void;
  theme: 'light' | 'dark';
  toggleTheme: () => void;
  cartCount: number;
  onOpenCart: () => void;
  activeTab: string;
  setActiveTab: (tab: string) => void;
  onSignOut: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentUser,
  onRoleChange,
  theme,
  toggleTheme,
  cartCount,
  onOpenCart,
  activeTab,
  setActiveTab,
  onSignOut,
}) => {
  const [showRoleDropdown, setShowRoleDropdown] = React.useState(false);

  const roles: { role: UserRole; label: string; icon: any; color: string }[] = [
    { role: 'SUPER_ADMIN', label: 'Super Admin', icon: ShieldCheck, color: 'text-purple-500 bg-purple-500/10' },
    { role: 'ADMIN', label: 'System Admin', icon: Building2, color: 'text-blue-500 bg-blue-500/10' },
    { role: 'WAREHOUSE_STAFF', label: 'Warehouse Staff', icon: Warehouse, color: 'text-amber-500 bg-amber-500/10' },
    { role: 'SALESMAN', label: 'Sales Executive', icon: UserCheck, color: 'text-emerald-500 bg-emerald-500/10' },
    { role: 'DELIVERY_BOY', label: 'Delivery Dispatcher', icon: Truck, color: 'text-orange-500 bg-orange-500/10' },
    { role: 'B2B_CUSTOMER', label: 'B2B Hospital Client', icon: Hospital, color: 'text-indigo-500 bg-indigo-500/10' },
    { role: 'B2C_CUSTOMER', label: 'B2C Patient Customer', icon: UserIcon, color: 'text-teal-500 bg-teal-500/10' },
  ];

  const currentRoleObj = roles.find(r => r.role === currentUser.role) || roles[0];
  const IconComponent = currentRoleObj.icon;

  return (
    <header className="sticky top-0 z-40 w-full border-b border-slate-200 dark:border-slate-800 bg-white/95 dark:bg-slate-900/95 backdrop-blur-md transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        
        {/* Brand Logo & Engine Badge */}
        <div className="flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-teal-600 dark:bg-teal-500 flex items-center justify-center text-white font-bold text-xl shadow-md shadow-teal-500/20">
            <Hospital className="h-6 w-6" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-xl tracking-tight text-slate-900 dark:text-white">
                MedSupply
              </span>
              <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full bg-teal-500/10 text-teal-600 dark:text-teal-400 border border-teal-500/20">
                <Cpu className="h-3 w-3" /> Java 21 / Spring Boot 3.5
              </span>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 font-medium hidden sm:block">
              Enterprise B2B & B2C Healthcare Supply Chain Platform
            </p>
          </div>
        </div>

        {/* Center / Right controls */}
        <div className="flex items-center gap-3">
          
          {/* B2B Credit Limit Indicator */}
          {currentUser.role === 'B2B_CUSTOMER' && currentUser.creditLimit && (
            <div className="hidden md:flex items-center gap-2 text-xs px-3 py-1.5 rounded-lg bg-indigo-50 dark:bg-indigo-950/50 border border-indigo-200 dark:border-indigo-800/60 text-indigo-900 dark:text-indigo-200">
              <CreditCard className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
              <div>
                <span className="font-semibold">Credit Line: </span>
                <span>${((currentUser.creditLimit || 0) - (currentUser.usedCredit || 0)).toLocaleString()} available</span>
                <span className="text-indigo-500 dark:text-indigo-400 text-[10px] block">
                  Term: {currentUser.creditTerms || 'NET_30'} | License: {currentUser.licenseNumber}
                </span>
              </div>
            </div>
          )}

          {/* Cart Icon Button for B2C & B2B */}
          <button
            id="cart-button"
            onClick={onOpenCart}
            className="relative p-2 rounded-xl text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            title="View Cart & Checkout"
          >
            <ShoppingBag className="h-5 w-5" />
            {cartCount > 0 && (
              <span className="absolute -top-1 -right-1 h-5 w-5 rounded-full bg-teal-600 text-white text-[11px] font-bold flex items-center justify-center animate-pulse">
                {cartCount}
              </span>
            )}
          </button>

          {/* Theme Toggle Button */}
          <button
            id="theme-toggle"
            onClick={toggleTheme}
            className="p-2 rounded-xl text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            title="Toggle Dark / Light Theme"
          >
            {theme === 'dark' ? <Sun className="h-5 w-5 text-amber-400" /> : <Moon className="h-5 w-5 text-slate-600" />}
          </button>

          {/* Standalone Sign Out Button */}
          <button
            id="sign-out-button"
            onClick={onSignOut}
            className="p-2 rounded-xl text-red-600 hover:bg-red-50 dark:hover:bg-red-950/20 transition-colors hidden md:inline-flex"
            title="Sign Out of Portal"
          >
            <LogOut className="h-5 w-5" />
          </button>

          {/* Role Switcher Dropdown */}
          <div className="relative">
            <button
              id="role-selector-button"
              onClick={() => setShowRoleDropdown(!showRoleDropdown)}
              className="flex items-center gap-2 px-3 py-2 rounded-xl border border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 bg-slate-50 dark:bg-slate-800/80 transition-all text-xs font-semibold text-slate-800 dark:text-slate-100"
            >
              <span className={`p-1 rounded-md ${currentRoleObj.color}`}>
                <IconComponent className="h-4 w-4" />
              </span>
              <div className="text-left hidden sm:block">
                <span className="block text-[10px] text-slate-400 uppercase tracking-wider font-bold">Active Role</span>
                <span>{currentRoleObj.label}</span>
              </div>
              <ChevronDown className="h-3.5 w-3.5 text-slate-400 ml-1" />
            </button>

            {showRoleDropdown && (
              <div className="absolute right-0 mt-2 w-64 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-2xl py-2 z-50">
                <div className="px-3 py-2 border-b border-slate-100 dark:border-slate-800">
                  <p className="text-xs font-bold text-slate-900 dark:text-slate-100">Switch Preview Role</p>
                  <p className="text-[11px] text-slate-500 dark:text-slate-400">Test multi-tenant RBAC permissions</p>
                </div>
                <div className="py-1 max-h-72 overflow-y-auto">
                  {roles.map((r) => {
                    const RoleIcon = r.icon;
                    const isSelected = currentUser.role === r.role;
                    return (
                      <button
                        key={r.role}
                        onClick={() => {
                          onRoleChange(r.role);
                          setShowRoleDropdown(false);
                        }}
                        className={`w-full flex items-center gap-3 px-3 py-2.5 text-xs text-left transition-colors ${
                          isSelected 
                            ? 'bg-teal-50 dark:bg-teal-950/40 text-teal-700 dark:text-teal-300 font-semibold' 
                            : 'hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                        }`}
                      >
                        <span className={`p-1.5 rounded-md ${r.color}`}>
                          <RoleIcon className="h-4 w-4" />
                        </span>
                        <div>
                          <span className="block font-medium">{r.label}</span>
                          <span className="text-[10px] text-slate-400">
                            {r.role === 'B2B_CUSTOMER' ? 'Hospital/Pharmacy POs' : r.role === 'B2C_CUSTOMER' ? 'Patient Portal' : 'Internal Staff'}
                          </span>
                        </div>
                      </button>
                    );
                  })}
                </div>
                <div className="border-t border-slate-100 dark:border-slate-800 p-2">
                  <button
                    onClick={() => {
                      onSignOut();
                      setShowRoleDropdown(false);
                    }}
                    className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-xl text-xs font-bold text-red-600 hover:bg-red-50 dark:hover:bg-red-950/20 transition-all"
                  >
                    <LogOut className="h-4 w-4" />
                    <span>Sign Out of Portal</span>
                  </button>
                </div>
              </div>
            )}
          </div>

        </div>

      </div>
    </header>
  );
};
