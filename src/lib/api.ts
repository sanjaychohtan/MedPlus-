import axios from 'axios';
import { 
  User, UserRole, Product, Batch, Warehouse, StockTransfer, 
  Order, Invoice, DeliveryTask, Coupon, AuditLog, SalesmanLead, SystemMetrics 
} from '../types';

const getAuthToken = (): string | null => {
  return localStorage.getItem('medsupply_logged_in');
};

export const setAuthToken = (token: string | null) => {
  if (token) {
    localStorage.setItem('medsupply_logged_in', 'true');
  } else {
    localStorage.removeItem('medsupply_logged_in');
  }
};

// Environment-based Spring Boot API configuration
const API_BASE_URL = (import.meta as any).env.PROD 
  ? 'https://api.medsupply.com/api' 
  : '/api';

const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

// Attach JWT authentication tokens automatically via Axios request interceptor if we have a non-cookie token (or empty)
axiosInstance.interceptors.request.use(
  (config) => {
    // Cookies are automatically sent because of withCredentials: true
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

function mapBackendUser(bUser: any): User {
  if (!bUser) return {} as User;
  
  // Handle LoginResponse payload (which lacks full entity fields but has roles list)
  let resolvedRole: UserRole = 'B2C_CUSTOMER';
  if (bUser.roles && bUser.roles.length > 0) {
    const rawRole = typeof bUser.roles[0] === 'object' ? bUser.roles[0].name : bUser.roles[0];
    resolvedRole = String(rawRole).replace('ROLE_', '') as UserRole;
  } else if (bUser.role) {
    resolvedRole = String(bUser.role).replace('ROLE_', '') as UserRole;
  }

  // Handle name mapping
  let resolvedName = '';
  if (bUser.firstName || bUser.lastName) {
    resolvedName = `${bUser.firstName || ''} ${bUser.lastName || ''}`.trim();
  } else if (bUser.name) {
    resolvedName = bUser.name;
  } else {
    resolvedName = bUser.email ? bUser.email.split('@')[0] : 'MedSupply User';
  }

  return {
    id: bUser.userId || bUser.id || '',
    name: resolvedName,
    email: bUser.email || '',
    role: resolvedRole,
    phone: bUser.phone || '',
    licenseNumber: bUser.licenseNumber || '',
    gstin: bUser.gstin || '',
    creditLimit: bUser.creditLimit ? Number(bUser.creditLimit) : 0,
    usedCredit: bUser.outstandingBalance ? Number(bUser.outstandingBalance) : 0,
    creditTerms: bUser.creditTerms || 'NET_30',
    address: bUser.address || '',
    city: bUser.city || '',
    state: bUser.state || '',
    pincode: bUser.pincode || '',
    status: bUser.status || 'ACTIVE',
    createdAt: bUser.createdAt || new Date().toISOString(),
  };
}

async function apiFetch<T>(endpoint: string, options: any = {}): Promise<T> {
  // Strip '/api' from start of relative URL since baseURL has '/api'
  let url = endpoint;
  if (url.startsWith('/api')) {
    url = url.substring(4);
  }

  try {
    const response = await axiosInstance({
      url,
      method: options.method || 'GET',
      data: options.body ? JSON.parse(options.body) : undefined,
      ...options,
    });
    
    // Transparently unwrap Spring Boot standard ApiResponse wrapper
    if (response.data && typeof response.data === 'object' && 'success' in response.data && 'data' in response.data) {
      return response.data.data;
    }
    
    return response.data;
  } catch (error: any) {
    const errData = error.response?.data || { error: 'API Request failed' };
    throw new Error(errData.error || errData.message || `HTTP Error ${error.response?.status || error.message}`);
  }
}

export const api = {
  // Auth
  login: async (email?: string, role?: UserRole) => {
    const res = await apiFetch<any>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, role }),
    });
    const mappedUser = mapBackendUser(res);
    setAuthToken('true');
    return {
      token: res.accessToken,
      user: mappedUser,
    };
  },

  register: async (userData: any) => {
    // Backend register returns ApiResponse<Void>
    await apiFetch<any>('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify(userData),
    });
    // Return empty token and default mock user until they verify OTP and log in
    return {
      token: '',
      user: {} as User
    };
  },

  forgotPassword: (email: string) =>
    apiFetch<{ success: boolean; message: string }>('/api/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  verifyOtp: (email: string, otpCode: string) =>
    apiFetch<{ success: boolean; message: string }>('/api/auth/verify-otp', {
      method: 'POST',
      body: JSON.stringify({ email, otpCode }),
    }),

  resetPassword: (email: string, password: string) =>
    apiFetch<{ success: boolean; message: string }>('/api/auth/reset-password', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),

  getMe: async () => {
    const res = await apiFetch<any>('/api/auth/me');
    return {
      user: mapBackendUser(res),
    };
  },

  switchRole: async (role: UserRole) => {
    const res = await apiFetch<any>('/api/auth/switch-role', {
      method: 'POST',
      body: JSON.stringify({ role }),
    });
    return {
      token: res.accessToken,
      user: mapBackendUser(res),
    };
  },

  logout: () =>
    apiFetch<{ success: boolean; message: string }>('/api/auth/logout', {
      method: 'POST',
    }),

  // Admin User Management
  getUsers: () => apiFetch<User[]>('/api/admin/users'),

  updateUserStatus: (id: string, updates: Partial<User>) =>
    apiFetch<User>(`/api/admin/users/${id}`, {
      method: 'PUT',
      body: JSON.stringify(updates),
    }),

  // Metrics
  getMetrics: () => apiFetch<SystemMetrics>('/api/metrics'),

  // Products & Inventory
  getProducts: (params: { search?: string; category?: string; brand?: string; prescriptionRequired?: boolean } = {}) => {
    const query = new URLSearchParams();
    if (params.search) query.set('search', params.search);
    if (params.category) query.set('category', params.category);
    if (params.brand) query.set('brand', params.brand);
    if (params.prescriptionRequired !== undefined) query.set('prescriptionRequired', String(params.prescriptionRequired));
    return apiFetch<Product[]>(`/api/inventory/products?${query.toString()}`);
  },

  createProduct: (productData: Partial<Product>) => 
    apiFetch<Product>('/api/inventory/products', {
      method: 'POST',
      body: JSON.stringify(productData),
    }),

  getBatches: (params: { warehouseId?: string; productId?: string; status?: string } = {}) => {
    const query = new URLSearchParams();
    if (params.warehouseId) query.set('warehouseId', params.warehouseId);
    if (params.productId) query.set('productId', params.productId);
    if (params.status) query.set('status', params.status);
    return apiFetch<Batch[]>(`/api/inventory/batches?${query.toString()}`);
  },

  createBatch: (batchData: any) => 
    apiFetch<Batch>('/api/inventory/batches', {
      method: 'POST',
      body: JSON.stringify(batchData),
    }),

  updateBatch: (id: string, updates: Partial<Batch>) => 
    apiFetch<Batch>(`/api/inventory/batches/${id}`, {
      method: 'PUT',
      body: JSON.stringify(updates),
    }),

  getCategories: () => apiFetch<any[]>('/api/inventory/categories'),
  getBrands: () => apiFetch<any[]>('/api/inventory/brands'),

  // Warehouses
  getWarehouses: () => apiFetch<Warehouse[]>('/api/warehouses'),
  getTransfers: () => apiFetch<StockTransfer[]>('/api/warehouses/transfers'),
  createTransfer: (transferData: any) => 
    apiFetch<StockTransfer>('/api/warehouses/transfers', {
      method: 'POST',
      body: JSON.stringify(transferData),
    }),

  // Orders
  getOrders: (params: { type?: 'B2B' | 'B2C'; customerId?: string; status?: string } = {}) => {
    const query = new URLSearchParams();
    if (params.type) query.set('type', params.type);
    if (params.customerId) query.set('customerId', params.customerId);
    if (params.status) query.set('status', params.status);
    return apiFetch<Order[]>(`/api/orders?${query.toString()}`);
  },

  createOrder: (orderData: any) => 
    apiFetch<{ order: Order; invoice: Invoice }>('/api/orders', {
      method: 'POST',
      body: JSON.stringify(orderData),
    }),

  updateOrderStatus: (id: string, status: string) => 
    apiFetch<Order>(`/api/orders/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify({ status }),
    }),

  // Invoices
  getInvoices: () => apiFetch<Invoice[]>('/api/invoices'),

  // Deliveries
  getDeliveries: () => apiFetch<DeliveryTask[]>('/api/deliveries'),
  updateDeliveryStatus: (id: string, updates: { status: string; otpCode?: string; currentLat?: number; currentLng?: number }) => 
    apiFetch<DeliveryTask>(`/api/deliveries/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify(updates),
    }),

  // Salesman Leads
  getSalesmanLeads: () => apiFetch<SalesmanLead[]>('/api/salesman/leads'),
  createLead: (leadData: Partial<SalesmanLead>) => 
    apiFetch<SalesmanLead>('/api/salesman/leads', {
      method: 'POST',
      body: JSON.stringify(leadData),
    }),

  // Coupons
  validateCoupon: (code: string, amount: number) => 
    apiFetch<{ valid: boolean; coupon?: Coupon; discount?: number; message?: string }>('/api/coupons/validate', {
      method: 'POST',
      body: JSON.stringify({ code, amount }),
    }),

  // Audit Logs
  getAuditLogs: () => apiFetch<AuditLog[]>('/api/audit'),

  // Architecture Spec
  getArchitectureSpec: () => apiFetch<any>('/api/architecture/java-spring'),

  // Reports & Analytics APIs
  getPlatformMetricsReport: () => apiFetch<any>('/api/reports/metrics'),
  getNearExpiryLotsReport: () => apiFetch<any[]>('/api/reports/near-expiry'),
  getLowStockAlertsReport: () => apiFetch<any[]>('/api/reports/low-stock'),
  getSalesSummaryReport: () => apiFetch<any>('/api/reports/sales'),

  // S3 File Handling
  getS3PresignedUrl: (key: string, expiryMinutes: number = 15) => 
    apiFetch<{ url: string }>(`/api/s3/presigned-url?key=${encodeURIComponent(key)}&expiryMinutes=${expiryMinutes}`),
};
