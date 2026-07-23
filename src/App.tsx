import React, { useState, useEffect } from 'react';
import { User, UserRole, Product, Batch, Warehouse, StockTransfer, Order, Invoice, DeliveryTask, SalesmanLead, AuditLog, SystemMetrics } from './types';
import { api, setAuthToken } from './lib/api';
import { Navbar } from './components/Navbar';
import { MetricsOverview } from './components/MetricsOverview';
import { DashboardView } from './components/views/DashboardView';
import { InventoryView } from './components/views/InventoryView';
import { WarehouseView } from './components/views/WarehouseView';
import { OrdersView } from './components/views/OrdersView';
import { DeliveryView } from './components/views/DeliveryView';
import { SalesmanView } from './components/views/SalesmanView';
import { B2BCustomerPortal } from './components/views/B2BCustomerPortal';
import { B2CCustomerPortal } from './components/views/B2CCustomerPortal';
import { AuditLogView } from './components/views/AuditLogView';
import { ArchitectureSpecView } from './components/views/ArchitectureSpecView';
import { AuthView } from './components/views/AuthView';
import { AdminView } from './components/views/AdminView';

import { 
  LayoutDashboard, Pill, Warehouse as WarehouseIcon, ShoppingCart, 
  Truck, UserCheck, Hospital, ShoppingBag, ShieldCheck, Cpu 
} from 'lucide-react';

export default function App() {
  const [theme, setTheme] = useState<'light' | 'dark'>('dark');
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const [sessionLoggedOut, setSessionLoggedOut] = useState<boolean>(() => {
    return !localStorage.getItem('medsupply_logged_in');
  });
  const [activeTab, setActiveTab] = useState<string>('dashboard');

  // Domain Data State
  const [metrics, setMetrics] = useState<SystemMetrics | null>(null);
  const [products, setProducts] = useState<Product[]>([]);
  const [batches, setBatches] = useState<Batch[]>([]);
  const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
  const [transfers, setTransfers] = useState<StockTransfer[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [deliveries, setDeliveries] = useState<DeliveryTask[]>([]);
  const [salesmanLeads, setSalesmanLeads] = useState<SalesmanLead[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLog[]>([]);
  const [architectureSpec, setArchitectureSpec] = useState<any>(null);
  const [categories, setCategories] = useState<any[]>([]);
  const [brands, setBrands] = useState<any[]>([]);

  // Retail Cart
  const [cart, setCart] = useState<{ product: Product; quantity: number }[]>([]);

  // Apply Theme Class to <html>
  useEffect(() => {
    if (theme === 'dark') {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [theme]);

  const toggleTheme = () => {
    setTheme(prev => prev === 'dark' ? 'light' : 'dark');
  };

  // Initial Fetch Data
  const loadData = async () => {
    let user = null;
    try {
      const meRes = await api.getMe();
      user = meRes.user;
      setCurrentUser(user);
      setSessionLoggedOut(false);
    } catch (err) {
      // If unauthorized, do not print a loud error; just set logged out state
      setCurrentUser(null);
      setSessionLoggedOut(true);
      return;
    }

    try {
      const [m, p, b, w, tr, ord, inv, del, leads, audit, spec, cats, brs] = await Promise.all([
        api.getMetrics(),
        api.getProducts(),
        api.getBatches(),
        api.getWarehouses(),
        api.getTransfers(),
        api.getOrders(),
        api.getInvoices(),
        api.getDeliveries(),
        api.getSalesmanLeads(),
        api.getAuditLogs(),
        api.getArchitectureSpec(),
        api.getCategories(),
        api.getBrands(),
      ]);

      setMetrics(m);
      setProducts(p);
      setBatches(b);
      setWarehouses(w);
      setTransfers(tr);
      setOrders(ord);
      setInvoices(inv);
      setDeliveries(del);
      setSalesmanLeads(leads);
      setAuditLogs(audit);
      setArchitectureSpec(spec);
      setCategories(cats);
      setBrands(brs);
    } catch (err) {
      console.error('Error loading MedSupply data:', err);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Handle Role Switching
  const handleRoleChange = async (newRole: UserRole) => {
    try {
      const res = await api.switchRole(newRole);
      setAuthToken(res.token);
      setCurrentUser(res.user);

      // Automatically route user to relevant view on role switch
      if (newRole === 'B2B_CUSTOMER') setActiveTab('b2b_portal');
      else if (newRole === 'B2C_CUSTOMER') setActiveTab('b2c_portal');
      else if (newRole === 'DELIVERY_BOY') setActiveTab('deliveries');
      else if (newRole === 'SALESMAN') setActiveTab('salesman');
      else if (newRole === 'WAREHOUSE_STAFF') setActiveTab('inventory');
      else setActiveTab('dashboard');

      await loadData();
    } catch (err) {
      console.error('Failed to switch role:', err);
    }
  };

  // Handlers
  const handleCreateProduct = async (productData: any) => {
    await api.createProduct(productData);
    await loadData();
  };

  const handleCreateBatch = async (batchData: any) => {
    await api.createBatch(batchData);
    await loadData();
  };

  const handleUpdateBatch = async (id: string, updates: any) => {
    await api.updateBatch(id, updates);
    await loadData();
  };

  const handleCreateTransfer = async (transferData: any) => {
    await api.createTransfer(transferData);
    await loadData();
  };

  const handleUpdateOrderStatus = async (id: string, status: string) => {
    await api.updateOrderStatus(id, status);
    await loadData();
  };

  const handleUpdateDeliveryStatus = async (id: string, updates: any) => {
    await api.updateDeliveryStatus(id, updates);
    await loadData();
  };

  const handleSignOut = async () => {
    try {
      await api.logout();
    } catch (err) {
      console.error('Failed to logout from backend:', err);
    }
    localStorage.removeItem('medsupply_logged_in');
    setCurrentUser(null);
    setSessionLoggedOut(true);
    setActiveTab('dashboard');
  };

  const handleCreateLead = async (leadData: any) => {
    await api.createLead(leadData);
    await loadData();
  };

  const handlePlaceB2BOrder = async (orderData: any) => {
    await api.createOrder(orderData);
    await loadData();
  };

  const handleAddToCart = (product: Product, qty: number) => {
    setCart(prev => {
      const existing = prev.find(i => i.product.id === product.id);
      if (existing) {
        return prev.map(i => i.product.id === product.id ? { ...i, quantity: i.quantity + qty } : i);
      }
      return [...prev, { product, quantity: qty }];
    });
  };

  const handleRemoveFromCart = (productId: string) => {
    setCart(prev => prev.filter(i => i.product.id !== productId));
  };

  const handleB2CCheckout = async (couponCode?: string) => {
    if (!currentUser) return;
    const items = cart.map(c => ({ productId: c.product.id, quantity: c.quantity }));
    await api.createOrder({
      orderType: 'B2C',
      customerId: currentUser.id,
      items,
      paymentMethod: 'RAZORPAY',
      couponCode,
    });
    setCart([]);
    await loadData();
  };

  if (sessionLoggedOut) {
    return (
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors">
        <AuthView onSuccess={async (user) => {
          setCurrentUser(user);
          setSessionLoggedOut(false);
          await loadData();
          if (user.role === 'B2B_CUSTOMER') setActiveTab('b2b_portal');
          else if (user.role === 'B2C_CUSTOMER') setActiveTab('b2c_portal');
          else if (user.role === 'DELIVERY_BOY') setActiveTab('deliveries');
          else if (user.role === 'SALESMAN') setActiveTab('salesman');
          else if (user.role === 'WAREHOUSE_STAFF') setActiveTab('inventory');
          else setActiveTab('dashboard');
        }} />
      </div>
    );
  }

  if (!currentUser) {
    return (
      <div className="min-h-screen bg-slate-950 text-white flex items-center justify-center p-4">
        <div className="animate-pulse text-center">
          <Hospital className="h-12 w-12 text-teal-400 mx-auto mb-3" />
          <p className="font-bold text-lg">Initializing MedSupply Enterprise Platform...</p>
          <p className="text-xs text-slate-400">Loading Spring Boot 3.5 DDD Modules</p>
        </div>
      </div>
    );
  }

  // Navigation Items
  const allNavItems = [
    { id: 'dashboard', label: 'Dashboard & Analytics', icon: LayoutDashboard },
    { id: 'inventory', label: 'FEFO Inventory & Batches', icon: Pill },
    { id: 'warehouses', label: 'Warehouses & Transfers', icon: WarehouseIcon },
    { id: 'orders', label: 'Orders & Tax Invoices', icon: ShoppingCart },
    { id: 'deliveries', label: 'Delivery Dispatch & GPS', icon: Truck },
    { id: 'salesman', label: 'Salesman CRM & Leads', icon: UserCheck },
    { id: 'b2b_portal', label: 'B2B Hospital Portal', icon: Hospital },
    { id: 'b2c_portal', label: 'B2C Patient Store', icon: ShoppingBag },
    { id: 'admin_portal', label: 'Super-Admin Console', icon: ShieldCheck },
    { id: 'audit', label: 'Security Audit Logs', icon: ShieldCheck },
    { id: 'architecture', label: 'Spring Boot & AWS Specs', icon: Cpu },
  ];

  const allowedNavItems = allNavItems.filter(item => {
    const role = currentUser.role;
    if (role === 'SUPER_ADMIN') return true;
    if (role === 'ADMIN') return item.id !== 'admin_portal';
    if (role === 'WAREHOUSE_STAFF') {
      return ['dashboard', 'inventory', 'warehouses', 'audit', 'architecture'].includes(item.id);
    }
    if (role === 'SALESMAN') {
      return ['dashboard', 'salesman', 'orders', 'audit'].includes(item.id);
    }
    if (role === 'DELIVERY_BOY') {
      return ['dashboard', 'deliveries'].includes(item.id);
    }
    if (role === 'B2B_CUSTOMER') {
      return ['b2b_portal', 'orders', 'dashboard', 'architecture'].includes(item.id);
    }
    if (role === 'B2C_CUSTOMER') {
      return ['b2c_portal', 'orders'].includes(item.id);
    }
    return true;
  });

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors font-sans pb-12">
      
      {/* Top Header */}
      <Navbar
        currentUser={currentUser}
        onRoleChange={handleRoleChange}
        theme={theme}
        toggleTheme={toggleTheme}
        cartCount={cart.reduce((a, c) => a + c.quantity, 0)}
        onOpenCart={() => setActiveTab('b2c_portal')}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        onSignOut={handleSignOut}
      />

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-6">
        
        {/* Executive Metrics Overview */}
        <MetricsOverview 
          metrics={metrics} 
          onSelectMetricFilter={(f) => {
            setActiveTab('inventory');
          }} 
        />

        {/* Tab Navigation Ribbon */}
        <div className="mb-6 flex items-center gap-1 overflow-x-auto pb-2 scrollbar-none border-b border-slate-200 dark:border-slate-800">
          {allowedNavItems.map(item => {
            const Icon = item.icon;
            const isActive = activeTab === item.id;
            return (
              <button
                key={item.id}
                onClick={() => setActiveTab(item.id)}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all ${
                  isActive
                    ? 'bg-teal-600 text-white shadow-md shadow-teal-600/20'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white hover:bg-slate-200/50 dark:hover:bg-slate-800/50'
                }`}
              >
                <Icon className="h-4 w-4" />
                <span>{item.label}</span>
              </button>
            );
          })}
        </div>

        {/* Active Module View Renderer */}
        <div className="transition-all">
          {activeTab === 'dashboard' && (
            <DashboardView
              metrics={metrics}
              orders={orders}
              batches={batches}
              warehouses={warehouses}
              onNavigateTab={(tab) => setActiveTab(tab)}
            />
          )}

          {activeTab === 'inventory' && (
            <InventoryView
              products={products}
              batches={batches}
              warehouses={warehouses}
              categories={categories}
              brands={brands}
              onCreateProduct={handleCreateProduct}
              onCreateBatch={handleCreateBatch}
              onUpdateBatch={handleUpdateBatch}
            />
          )}

          {activeTab === 'warehouses' && (
            <WarehouseView
              warehouses={warehouses}
              transfers={transfers}
              products={products}
              batches={batches}
              onCreateTransfer={handleCreateTransfer}
            />
          )}

          {activeTab === 'orders' && (
            <OrdersView
              orders={orders}
              invoices={invoices}
              onUpdateOrderStatus={handleUpdateOrderStatus}
            />
          )}

          {activeTab === 'deliveries' && (
            <DeliveryView
              deliveries={deliveries}
              onUpdateDeliveryStatus={handleUpdateDeliveryStatus}
            />
          )}

          {activeTab === 'salesman' && (
            <SalesmanView
              leads={salesmanLeads}
              onCreateLead={handleCreateLead}
            />
          )}

          {activeTab === 'b2b_portal' && (
            <B2BCustomerPortal
              currentUser={currentUser}
              products={products}
              batches={batches}
              onPlaceOrder={handlePlaceB2BOrder}
            />
          )}

          {activeTab === 'b2c_portal' && (
            <B2CCustomerPortal
              products={products}
              cart={cart}
              onAddToCart={handleAddToCart}
              onRemoveFromCart={handleRemoveFromCart}
              onCheckout={handleB2CCheckout}
            />
          )}

          {activeTab === 'admin_portal' && (
            <AdminView
              logs={auditLogs}
              onRefreshData={loadData}
            />
          )}

          {activeTab === 'audit' && (
            <AuditLogView logs={auditLogs} />
          )}

          {activeTab === 'architecture' && (
            <ArchitectureSpecView specData={architectureSpec} />
          )}
        </div>

      </main>

    </div>
  );
}
