-- Enable standard UUID generation extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================================================================
-- 1. ROLES TABLE
-- =========================================================================
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- =========================================================================
-- 2. PERMISSIONS TABLE
-- =========================================================================
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- =========================================================================
-- 3. ROLE-PERMISSION JOIN TABLE
-- =========================================================================
CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- =========================================================================
-- 4. USERS TABLE (Core Account Registry)
-- =========================================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    
    -- Hospital/Clinic Licensing and Credit Facilities
    license_number VARCHAR(100),
    gstin VARCHAR(20),
    credit_limit DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    outstanding_balance DECIMAL(12, 2) DEFAULT 0.00 NOT NULL,
    
    -- Status and Lifecycle Management
    status VARCHAR(50) DEFAULT 'PENDING_APPROVAL' NOT NULL,
    
    -- Verification and Recovery Telemetries
    otp_code VARCHAR(6),
    otp_expiry TIMESTAMP WITH TIME ZONE,
    reset_token VARCHAR(100),
    reset_token_expiry TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INT DEFAULT 0 NOT NULL,
    
    -- Global Audit Metadata
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by UUID,
    updated_by UUID,
    
    -- Soft Deletion Safeguards
    is_deleted BOOLEAN DEFAULT FALSE NOT NULL,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- =========================================================================
-- 5. USER-ROLE JOIN TABLE
-- =========================================================================
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- =========================================================================
-- 6. AUDIT LOGS TABLE (Immutable System-wide Audit Ledger)
-- =========================================================================
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    executor_id UUID REFERENCES users(id) ON DELETE SET NULL,
    executor_role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    module_name VARCHAR(100) NOT NULL,
    details TEXT,
    client_ip VARCHAR(45) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- =========================================================================
-- INDEX DEFINITIONS (Performance Optimization & Threat Detection)
-- =========================================================================
CREATE INDEX idx_users_email ON users(email) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_executor_id ON audit_logs(executor_id);

-- =========================================================================
-- INITIAL SEED DATA
-- =========================================================================

-- Seed Roles
INSERT INTO roles (id, name, description) VALUES
(uuid_generate_v4(), 'SUPER_ADMIN', 'Full global write, delete, user creation, and infrastructure logs'),
(uuid_generate_v4(), 'ADMIN', 'Core enterprise approvals, stock updates, tax invoice access'),
(uuid_generate_v4(), 'WAREHOUSE_STAFF', 'FEFO inventory logging, transfers creation, sensor telemetry inputs'),
(uuid_generate_v4(), 'SALESMAN', 'Prospect onboarding, tracking pipeline leads, booking orders for clients'),
(uuid_generate_v4(), 'DELIVERY_BOY', 'Fetching assigned last-mile routes, GPS simulation, OTP confirmation'),
(uuid_generate_v4(), 'B2B_CUSTOMER', 'Bulk supply viewing, Net-30 credit order checkout, PO management'),
(uuid_generate_v4(), 'B2C_CUSTOMER', 'OTC inventory ordering, prescription uploading, Razorpay card processing');

-- Seed Core Permissions
INSERT INTO permissions (id, name, description) VALUES
(uuid_generate_v4(), 'MANAGE_USERS', 'Ability to approve, suspend or edit users'),
(uuid_generate_v4(), 'VIEW_AUDIT_LOGS', 'Ability to inspect system modifications and security audits'),
(uuid_generate_v4(), 'MANAGE_INVENTORY', 'Ability to create products, edit batches and log stock'),
(uuid_generate_v4(), 'VIEW_INVENTORY', 'Ability to view product catalog and batch dates'),
(uuid_generate_v4(), 'MANAGE_ORDERS', 'Ability to process, pick, or ship warehouse orders'),
(uuid_generate_v4(), 'PLACE_ORDER_B2B', 'Ability to order bulk medical lines using Net-30 accounts'),
(uuid_generate_v4(), 'PLACE_ORDER_B2C', 'Ability to purchase retail medical lines with immediate payment');
