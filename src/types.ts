export type UserRole = 
  | 'SUPER_ADMIN' 
  | 'ADMIN' 
  | 'WAREHOUSE_STAFF' 
  | 'SALESMAN' 
  | 'DELIVERY_BOY' 
  | 'B2B_CUSTOMER' 
  | 'B2C_CUSTOMER';

export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  phone: string;
  licenseNumber?: string; // Drug License No for B2B
  gstin?: string; // Tax ID
  creditLimit?: number; // B2B Credit Limit in USD/INR
  usedCredit?: number;
  creditTerms?: 'NET_15' | 'NET_30' | 'NET_60';
  address?: string;
  city?: string;
  state?: string;
  pincode?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'PENDING_APPROVAL';
  createdAt: string;
}

export interface Category {
  id: string;
  name: string;
  code: string;
  description: string;
  icon: string;
  isPrescriptionRequired: boolean;
}

export interface Brand {
  id: string;
  name: string;
  country: string;
  contactEmail: string;
  isGmpCertified: boolean;
}

export type StorageCondition = 'ROOM_TEMP' | 'COLD_CHAIN_2_8C' | 'FROZEN_MINUS_20C' | 'CONTROLLED_SUBSTANCE';

export interface Warehouse {
  id: string;
  name: string;
  code: string;
  type: 'CENTRAL_HUB' | 'REGIONAL_COLD_CHAIN' | 'HOSPITAL_ONSITE' | 'TRANSIT_FULFILLMENT';
  address: string;
  city: string;
  capacitySqFt: number;
  tempControl: StorageCondition;
  managerName: string;
  status: 'OPERATIONAL' | 'MAINTENANCE' | 'FULL';
}

export interface Product {
  id: string;
  name: string;
  sku: string;
  hsnCode: string;
  categoryId: string;
  categoryName?: string;
  brandId: string;
  brandName?: string;
  description: string;
  unitOfMeasure: 'STRIP' | 'BOX' | 'BOTTLE' | 'VIAL' | 'CYLINDER' | 'PACK' | 'KIT';
  b2cPrice: number;
  b2bPriceTier1: number; // Regular bulk (10-99 units)
  b2bPriceTier2: number; // Hospital wholesale (100+ units)
  mrp: number;
  taxRatePercent: number; // 5%, 12%, 18%
  prescriptionRequired: boolean;
  minStockAlert: number;
  imageUrl: string;
  storageCondition: StorageCondition;
  active: boolean;
}

export interface Batch {
  id: string;
  productId: string;
  productName?: string;
  productSku?: string;
  warehouseId: string;
  warehouseName?: string;
  batchNumber: string;
  manufacturingDate: string;
  expiryDate: string;
  mrp: number;
  b2bPrice: number;
  quantityOnHand: number;
  quantityReserved: number;
  quantityAvailable: number;
  coldChainMonitored: boolean;
  tempReadingCelsius?: number;
  status: 'ACTIVE' | 'NEAR_EXPIRY' | 'EXPIRED' | 'QUARANTINED';
}

export interface StockTransfer {
  id: string;
  transferNumber: string;
  fromWarehouseId: string;
  fromWarehouseName?: string;
  toWarehouseId: string;
  toWarehouseName?: string;
  productId: string;
  productName?: string;
  batchId: string;
  batchNumber?: string;
  quantity: number;
  requestedBy: string;
  approvedBy?: string;
  status: 'REQUESTED' | 'APPROVED' | 'IN_TRANSIT' | 'COMPLETED' | 'REJECTED';
  createdAt: string;
  notes?: string;
}

export interface OrderItem {
  id: string;
  productId: string;
  batchId?: string;
  productName: string;
  productSku: string;
  batchNumber?: string;
  quantity: number;
  unitPrice: number;
  mrp: number;
  taxRate: number;
  taxAmount: number;
  totalPrice: number;
}

export type OrderStatus = 
  | 'PENDING_APPROVAL' 
  | 'APPROVED' 
  | 'PROCESSING' 
  | 'PICKED' 
  | 'DISPATCHED' 
  | 'OUT_FOR_DELIVERY' 
  | 'DELIVERED' 
  | 'CANCELLED' 
  | 'RETURNED';

export type PaymentMethod = 'RAZORPAY' | 'CREDIT_TERM' | 'CASH_ON_DELIVERY' | 'BANK_TRANSFER';
export type PaymentStatus = 'PENDING' | 'PAID' | 'CREDIT_APPROVED' | 'REFUNDED' | 'FAILED';

export interface Order {
  id: string;
  orderNumber: string;
  orderType: 'B2B' | 'B2C';
  customerId: string;
  customerName: string;
  customerEmail: string;
  salesmanId?: string;
  salesmanName?: string;
  deliveryBoyId?: string;
  deliveryBoyName?: string;
  warehouseId: string;
  warehouseName?: string;
  items: OrderItem[];
  subtotal: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  paymentStatus: PaymentStatus;
  paymentMethod: PaymentMethod;
  orderStatus: OrderStatus;
  deliveryAddress: string;
  prescriptionUrl?: string;
  poNumber?: string; // B2B Purchase Order #
  createdAt: string;
  updatedAt: string;
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  orderId: string;
  orderNumber: string;
  customerId: string;
  customerName: string;
  gstin?: string;
  subtotal: number;
  cgst: number;
  sgst: number;
  igst: number;
  totalAmount: number;
  pdfGeneratedAt: string;
  paymentDueDate: string;
  status: 'UNPAID' | 'PARTIALLY_PAID' | 'PAID' | 'OVERDUE';
}

export interface DeliveryTask {
  id: string;
  deliveryNumber: string;
  orderId: string;
  orderNumber: string;
  deliveryBoyId: string;
  deliveryBoyName: string;
  customerName: string;
  phone: string;
  deliveryAddress: string;
  currentLat: number;
  currentLng: number;
  estimatedArrivalMinutes: number;
  status: 'ASSIGNED' | 'PICKED_UP' | 'IN_TRANSIT' | 'DELIVERED' | 'FAILED';
  otpCode: string;
  notes?: string;
  updatedAt: string;
}

export interface Coupon {
  id: string;
  code: string;
  discountPercent: number;
  maxDiscount: number;
  minOrderAmount: number;
  validUntil: string;
  usageCount: number;
  active: boolean;
}

export interface AuditLog {
  id: string;
  userId: string;
  userName: string;
  userRole: UserRole;
  action: string;
  module: string;
  details: string;
  ipAddress: string;
  timestamp: string;
}

export interface SalesmanLead {
  id: string;
  salesmanId: string;
  pharmacyName: string;
  contactPerson: string;
  phone: string;
  city: string;
  estimatedMonthlyValue: number;
  status: 'LEAD' | 'CONTACTED' | 'NEGOTIATING' | 'ONBOARDED';
}

export interface SystemMetrics {
  totalRevenueB2B: number;
  totalRevenueB2C: number;
  totalOrdersCount: number;
  pendingOrdersCount: number;
  totalBatchesCount: number;
  nearExpiryCount: number;
  expiredCount: number;
  activeWarehousesCount: number;
  activeDeliveriesCount: number;
}
