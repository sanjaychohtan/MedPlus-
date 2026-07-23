import React, { useState } from 'react';
import { Cpu, Server, Database, Cloud, Lock, Code2, Layers, CheckCircle2 } from 'lucide-react';

interface ArchitectureSpecViewProps {
  specData: any;
}

export const ArchitectureSpecView: React.FC<ArchitectureSpecViewProps> = ({ specData }) => {
  const [activeTab, setActiveTab] = useState<'spring' | 'sql' | 'aws'>('spring');

  const psqlSchemaDDL = `-- PostgreSQL 16 DDL Schema for MedSupply Enterprise Platform
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- USERS TABLE
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(120) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    role VARCHAR(30) NOT NULL,
    phone VARCHAR(30),
    license_number VARCHAR(100),
    gstin VARCHAR(30),
    credit_limit NUMERIC(12, 2) DEFAULT 0.00,
    used_credit NUMERIC(12, 2) DEFAULT 0.00,
    credit_terms VARCHAR(20) DEFAULT 'NET_30',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- PRODUCTS TABLE
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    sku VARCHAR(50) UNIQUE NOT NULL,
    hsn_code VARCHAR(20) NOT NULL,
    category_id UUID,
    brand_id UUID,
    b2c_price NUMERIC(10, 2) NOT NULL,
    b2b_price_tier1 NUMERIC(10, 2) NOT NULL,
    b2b_price_tier2 NUMERIC(10, 2) NOT NULL,
    mrp NUMERIC(10, 2) NOT NULL,
    tax_rate_percent NUMERIC(5, 2) DEFAULT 12.00,
    prescription_required BOOLEAN DEFAULT FALSE,
    storage_condition VARCHAR(30) DEFAULT 'ROOM_TEMP',
    is_deleted BOOLEAN DEFAULT FALSE
);

-- FEFO BATCHES TABLE
CREATE TABLE batches (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_id UUID REFERENCES products(id),
    warehouse_id UUID REFERENCES warehouses(id),
    batch_number VARCHAR(50) NOT NULL,
    manufacturing_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    quantity_on_hand INT DEFAULT 0,
    quantity_reserved INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_batches_fefo ON batches(product_id, expiry_date ASC);
CREATE INDEX idx_batches_warehouse ON batches(warehouse_id);`;

  return (
    <div className="space-y-6">
      
      {/* Header */}
      <div className="p-6 rounded-2xl bg-slate-900 text-white shadow-xl flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-teal-400 font-bold text-xs uppercase tracking-wider">
            <Cpu className="h-4 w-4" /> Enterprise Monolith Specification
          </div>
          <h1 className="text-2xl font-black mt-1">Java 21 • Spring Boot 3.5.x • AWS Cloud Specs</h1>
          <p className="text-xs text-slate-300 mt-1">
            Production-grade DDD clean architecture, Spring Security 6 JWT specs, PostgreSQL 16 DDL, and AWS infrastructure topology.
          </p>
        </div>

        <div className="flex items-center gap-2 bg-slate-800 p-1 rounded-xl">
          <button
            onClick={() => setActiveTab('spring')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${activeTab === 'spring' ? 'bg-teal-500 text-slate-950' : 'text-slate-300'}`}
          >
            Spring Boot 3.5
          </button>
          <button
            onClick={() => setActiveTab('sql')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${activeTab === 'sql' ? 'bg-teal-500 text-slate-950' : 'text-slate-300'}`}
          >
            PostgreSQL 16 DDL
          </button>
          <button
            onClick={() => setActiveTab('aws')}
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-all ${activeTab === 'aws' ? 'bg-teal-500 text-slate-950' : 'text-slate-300'}`}
          >
            AWS Cloud
          </button>
        </div>
      </div>

      {/* SPRING BOOT TAB */}
      {activeTab === 'spring' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-3">
            <h3 className="font-bold text-sm text-slate-900 dark:text-white flex items-center gap-2">
              <Code2 className="h-4 w-4 text-teal-600" /> Maven pom.xml Specification
            </h3>
            <pre className="p-4 rounded-xl bg-slate-950 text-teal-400 font-mono text-[11px] overflow-x-auto max-h-96">
              {specData?.javaCodeSamples?.pomXml || 'Loading Maven POM...'}
            </pre>
          </div>

          <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-3">
            <h3 className="font-bold text-sm text-slate-900 dark:text-white flex items-center gap-2">
              <Lock className="h-4 w-4 text-indigo-600" /> Spring Security 6 JWT Filter
            </h3>
            <pre className="p-4 rounded-xl bg-slate-950 text-indigo-300 font-mono text-[11px] overflow-x-auto max-h-96">
              {specData?.javaCodeSamples?.securityConfig || 'Loading Security Config...'}
            </pre>
          </div>
        </div>
      )}

      {/* POSTGRESQL DDL TAB */}
      {activeTab === 'sql' && (
        <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-3">
          <h3 className="font-bold text-sm text-slate-900 dark:text-white flex items-center gap-2">
            <Database className="h-4 w-4 text-sky-600" /> PostgreSQL 16 DDL Schema with Audit Columns & UUID Keys
          </h3>
          <pre className="p-4 rounded-xl bg-slate-950 text-sky-300 font-mono text-[11px] overflow-x-auto max-h-[500px]">
            {psqlSchemaDDL}
          </pre>
        </div>
      )}

      {/* AWS CLOUD ARCHITECTURE TAB */}
      {activeTab === 'aws' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-2">
            <div className="p-2.5 rounded-xl bg-orange-100 text-orange-800 font-bold text-xs w-fit">AWS EC2</div>
            <h4 className="font-bold text-sm text-slate-900 dark:text-white">Application Monolith Cluster</h4>
            <p className="text-xs text-slate-500">Auto Scaling Group running Java 21 Spring Boot fat JAR behind Application Load Balancer.</p>
          </div>

          <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-2">
            <div className="p-2.5 rounded-xl bg-sky-100 text-sky-800 font-bold text-xs w-fit">AWS RDS PostgreSQL</div>
            <h4 className="font-bold text-sm text-slate-900 dark:text-white">Multi-AZ Database</h4>
            <p className="text-xs text-slate-500">PostgreSQL 16 with Read Replicas, automated daily snapshots, and encrypted storage at rest.</p>
          </div>

          <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-2">
            <div className="p-2.5 rounded-xl bg-emerald-100 text-emerald-800 font-bold text-xs w-fit">AWS S3 + CloudFront</div>
            <h4 className="font-bold text-sm text-slate-900 dark:text-white">Prescriptions & Invoices Bucket</h4>
            <p className="text-xs text-slate-500">Encrypted medical prescription files and generated PDF tax invoices with signed URLs.</p>
          </div>

          <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-2">
            <div className="p-2.5 rounded-xl bg-purple-100 text-purple-800 font-bold text-xs w-fit">AWS SES & FCM</div>
            <h4 className="font-bold text-sm text-slate-900 dark:text-white">Notification Pipeline</h4>
            <p className="text-xs text-slate-500">Transactional dispatch alerts, B2B invoice emails, and Firebase mobile notifications.</p>
          </div>
        </div>
      )}

    </div>
  );
};
