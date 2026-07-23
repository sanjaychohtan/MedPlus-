-- =========================================================================
-- FLYWAY DATABASE MIGRATION - PHASE 2: MODULE 2 & MODULE 3
-- SCHEMA LAYOUT FOR INVENTORY, WAREHOUSES, ORDERS, DELIVERIES, AND CRM LEADS
-- =========================================================================

-- 1. CATEGORIES TABLE
CREATE TABLE categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 2. BRANDS TABLE
CREATE TABLE brands (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 3. PRODUCTS TABLE
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(100) UNIQUE NOT NULL,
    hsn_code VARCHAR(20) NOT NULL,
    description TEXT,
    category_id UUID NOT NULL REFERENCES categories(id),
    brand_id UUID NOT NULL REFERENCES brands(id),
    unit_of_measure VARCHAR(20) DEFAULT 'BOX' NOT NULL,
    b2c_price DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    b2b_price_tier1 DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    b2b_price_tier2 DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    mrp DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    tax_rate_percent DECIMAL(5, 2) DEFAULT 12.00 NOT NULL,
    prescription_required BOOLEAN DEFAULT FALSE NOT NULL,
    min_stock_alert INT DEFAULT 100 NOT NULL,
    storage_condition VARCHAR(50) DEFAULT 'ROOM_TEMP' NOT NULL,
    image_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 4. WAREHOUSES TABLE
CREATE TABLE warehouses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    address VARCHAR(255) NOT NULL,
    capacity_sqft INT NOT NULL,
    temp_min DECIMAL(5, 2),
    temp_max DECIMAL(5, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 5. BATCHES TABLE (FEFO Optimized Batch Inventory Store)
CREATE TABLE batches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    batch_number VARCHAR(100) NOT NULL,
    manufacturing_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    mrp DECIMAL(12, 2) NOT NULL,
    b2b_price DECIMAL(12, 2) NOT NULL,
    quantity_on_hand INT NOT NULL DEFAULT 0,
    quantity_reserved INT NOT NULL DEFAULT 0,
    quantity_available INT NOT NULL DEFAULT 0,
    cold_chain_monitored BOOLEAN DEFAULT FALSE NOT NULL,
    temp_reading_celsius DECIMAL(5, 2),
    status VARCHAR(50) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 6. STOCK TRANSFERS TABLE
CREATE TABLE stock_transfers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transfer_number VARCHAR(50) UNIQUE NOT NULL,
    from_warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    to_warehouse_id UUID NOT NULL REFERENCES warehouses(id),
    product_id UUID NOT NULL REFERENCES products(id),
    batch_id UUID NOT NULL REFERENCES batches(id),
    quantity INT NOT NULL,
    requested_by VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 7. ORDERS TABLE
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_number VARCHAR(50) UNIQUE NOT NULL,
    order_type VARCHAR(20) NOT NULL, -- B2B, B2C
    customer_id UUID NOT NULL REFERENCES users(id),
    customer_name VARCHAR(200) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    warehouse_id UUID REFERENCES warehouses(id),
    subtotal DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    tax_amount DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    discount_amount DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    total_amount DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    payment_status VARCHAR(50) DEFAULT 'PENDING' NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    order_status VARCHAR(50) DEFAULT 'PENDING_APPROVAL' NOT NULL,
    delivery_address TEXT NOT NULL,
    po_number VARCHAR(100),
    prescription_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 8. ORDER ITEMS TABLE
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    batch_id UUID REFERENCES batches(id),
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100) NOT NULL,
    batch_number VARCHAR(100),
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    mrp DECIMAL(12, 2) NOT NULL,
    tax_rate DECIMAL(5, 2) NOT NULL,
    tax_amount DECIMAL(12, 2) NOT NULL,
    total_price DECIMAL(12, 2) NOT NULL
);

-- 9. INVOICES TABLE (Net-30 Hospital Invoicing)
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id),
    order_number VARCHAR(50) NOT NULL,
    customer_id UUID NOT NULL REFERENCES users(id),
    gstin VARCHAR(20),
    subtotal DECIMAL(12, 2) NOT NULL,
    cgst DECIMAL(12, 2) NOT NULL,
    sgst DECIMAL(12, 2) NOT NULL,
    igst DECIMAL(12, 2) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    pdf_generated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    payment_due_date TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(50) DEFAULT 'UNPAID' NOT NULL
);

-- 10. DELIVERY TASKS TABLE (Courier Handovers with OTP Check)
CREATE TABLE delivery_tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    delivery_number VARCHAR(50) UNIQUE NOT NULL,
    order_id UUID NOT NULL REFERENCES orders(id),
    order_number VARCHAR(50) NOT NULL,
    delivery_boy_id UUID NOT NULL REFERENCES users(id),
    delivery_boy_name VARCHAR(200) NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    delivery_address TEXT NOT NULL,
    current_lat DECIMAL(10, 6) DEFAULT 0.00 NOT NULL,
    current_lng DECIMAL(10, 6) DEFAULT 0.00 NOT NULL,
    estimated_arrival_minutes INT DEFAULT 30 NOT NULL,
    status VARCHAR(50) DEFAULT 'ASSIGNED' NOT NULL,
    otp_code VARCHAR(4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 11. SALESMAN LEADS TABLE
CREATE TABLE salesman_leads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    salesman_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(200) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'LEAD' NOT NULL, -- LEAD, CONTACTED, NEGOTIATING, ONBOARDED
    source VARCHAR(100),
    company VARCHAR(200),
    pipe_value DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- 12. COUPONS TABLE
CREATE TABLE coupons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(50) UNIQUE NOT NULL,
    discount_percent DECIMAL(5, 2) NOT NULL,
    max_discount DECIMAL(12, 2) NOT NULL,
    min_order_amount DECIMAL(12, 2) NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    usage_count INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- =========================================================================
-- SPECIAL INDEXES
-- =========================================================================
CREATE INDEX idx_products_sku ON products(sku) WHERE is_deleted = FALSE;
CREATE INDEX idx_batches_fefo ON batches(product_id, expiry_date ASC) WHERE is_deleted = FALSE AND status = 'ACTIVE' AND quantity_available > 0;
CREATE INDEX idx_orders_customer ON orders(customer_id);
CREATE INDEX idx_delivery_tasks_boy ON delivery_tasks(delivery_boy_id);
CREATE INDEX idx_salesman_leads_salesman ON salesman_leads(salesman_id);
