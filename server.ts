import express from 'express';
import path from 'path';
import { createServer as createViteServer } from 'vite';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { store } from './src/server/db/mockStore';

async function startServer() {
  const app = express();
  const PORT = 3000;

  const useMockBackend = !process.env.SPRING_BOOT_BACKEND_URL;

  if (!useMockBackend) {
    const backendTarget = process.env.SPRING_BOOT_BACKEND_URL!;
    console.log(`[MedSupply Proxy Server] Forwarding /api requests to Spring Boot backend: ${backendTarget}`);
    app.use(
      '/api',
      createProxyMiddleware({
        target: backendTarget,
        changeOrigin: true,
        pathRewrite: {
          '^/api': '/api/v1',
        },
      })
    );
  } else {
    console.log(`[MedSupply Mock Server] Active - Handling all /api requests in-memory`);
    
    app.use(express.json());

    // Helper to get session user
    function getSessionUserId(req: express.Request): string | null {
      const cookieHeader = req.headers.cookie;
      if (!cookieHeader) return null;
      const cookies = cookieHeader.split(';').reduce((acc: any, c) => {
        const parts = c.trim().split('=');
        const name = parts[0];
        const val = parts.slice(1).join('=');
        acc[name] = val;
        return acc;
      }, {});
      return cookies['medsupply_session'] || null;
    }

    // Helper to set HttpOnly cookie
    function setSessionCookie(res: express.Response, userId: string) {
      res.setHeader('Set-Cookie', `medsupply_session=${userId}; Path=/; HttpOnly; SameSite=Strict; Max-Age=604800`);
    }

    // Helper to clear HttpOnly cookie
    function clearSessionCookie(res: express.Response) {
      res.setHeader('Set-Cookie', `medsupply_session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0`);
    }

    // Auth
    app.post('/api/auth/login', (req, res) => {
      const { email, role } = req.body;
      let user = store.users.find(u => u.email === email);
      if (!user && role) {
        user = store.users.find(u => u.role === role);
      }
      if (!user) {
        user = store.users[0]; // fallback to super admin
      }
      setSessionCookie(res, user.id);
      res.json({
        success: true,
        data: {
          accessToken: 'mock-jwt-token-xyz',
          userId: user.id,
          email: user.email,
          role: user.role,
          roles: ['ROLE_' + user.role],
          name: user.name,
          phone: user.phone,
          status: user.status,
          createdAt: user.createdAt,
          licenseNumber: user.licenseNumber || '',
          gstin: user.gstin || '',
          creditLimit: user.creditLimit || 0,
          outstandingBalance: user.usedCredit || 0,
          creditTerms: user.creditTerms || 'NET_30',
          address: user.address || '',
          city: user.city || '',
          state: user.state || '',
          pincode: user.pincode || '',
        }
      });
    });

    app.post('/api/auth/register', (req, res) => {
      const userData = req.body;
      const newUser = {
        id: `usr-${Date.now()}`,
        name: userData.name || 'New Registered User',
        email: userData.email,
        role: userData.role || 'B2C_CUSTOMER',
        phone: userData.phone || '',
        licenseNumber: userData.licenseNumber || '',
        gstin: userData.gstin || '',
        creditLimit: userData.role === 'B2B_CUSTOMER' ? 100000 : 0,
        usedCredit: 0,
        creditTerms: userData.creditTerms || 'NET_30',
        address: userData.address || '',
        city: userData.city || '',
        state: userData.state || '',
        pincode: userData.pincode || '',
        status: 'PENDING_APPROVAL',
        createdAt: new Date().toISOString(),
      };
      store.users.push(newUser as any);
      store.addAuditLog(newUser.id, newUser.name, newUser.role, 'USER_REGISTER', 'SECURITY', `User self-registered as ${newUser.role}`);
      res.json({ success: true, data: null });
    });

    app.post('/api/auth/forgot-password', (req, res) => {
      res.json({ success: true, data: { success: true, message: 'OTP sent successfully' } });
    });

    app.post('/api/auth/verify-otp', (req, res) => {
      res.json({ success: true, data: { success: true, message: 'OTP verified successfully' } });
    });

    app.post('/api/auth/reset-password', (req, res) => {
      res.json({ success: true, data: { success: true, message: 'Password reset successful' } });
    });

    app.get('/api/auth/me', (req, res) => {
      const userId = getSessionUserId(req);
      if (!userId) {
        return res.status(401).json({ success: false, error: 'Unauthorized' });
      }
      const user = store.users.find(u => u.id === userId);
      if (!user) {
        return res.status(401).json({ success: false, error: 'User session expired' });
      }
      res.json({
        success: true,
        data: {
          userId: user.id,
          id: user.id,
          email: user.email,
          role: user.role,
          roles: ['ROLE_' + user.role],
          name: user.name,
          phone: user.phone,
          status: user.status,
          createdAt: user.createdAt,
          licenseNumber: user.licenseNumber || '',
          gstin: user.gstin || '',
          creditLimit: user.creditLimit || 0,
          outstandingBalance: user.usedCredit || 0,
          creditTerms: user.creditTerms || 'NET_30',
          address: user.address || '',
          city: user.city || '',
          state: user.state || '',
          pincode: user.pincode || '',
        }
      });
    });

    app.post('/api/auth/switch-role', (req, res) => {
      const { role } = req.body;
      let user = store.users.find(u => u.role === role);
      if (!user) {
        user = {
          id: `usr-${role.toLowerCase()}-${Math.floor(Math.random()*1000)}`,
          name: `Mock ${role}`,
          email: `${role.toLowerCase()}@medsupply.com`,
          role: role,
          phone: '+1 (800) 555-1212',
          status: 'ACTIVE',
          createdAt: new Date().toISOString(),
        };
        store.users.push(user);
      }
      setSessionCookie(res, user.id);
      res.json({
        success: true,
        data: {
          accessToken: 'mock-jwt-token-xyz',
          userId: user.id,
          id: user.id,
          email: user.email,
          role: user.role,
          roles: ['ROLE_' + user.role],
          name: user.name,
          phone: user.phone,
          status: user.status,
          createdAt: user.createdAt,
          licenseNumber: user.licenseNumber || '',
          gstin: user.gstin || '',
          creditLimit: user.creditLimit || 0,
          outstandingBalance: user.usedCredit || 0,
          creditTerms: user.creditTerms || 'NET_30',
          address: user.address || '',
          city: user.city || '',
          state: user.state || '',
          pincode: user.pincode || '',
        }
      });
    });

    app.post('/api/auth/logout', (req, res) => {
      clearSessionCookie(res);
      res.json({ success: true, data: { success: true, message: 'Logged out successfully' } });
    });

    // Admin Users
    app.get('/api/admin/users', (req, res) => {
      res.json({ success: true, data: store.users });
    });

    app.put('/api/admin/users/:id', (req, res) => {
      const { id } = req.params;
      const updates = req.body;
      const user = store.users.find(u => u.id === id);
      if (user) {
        Object.assign(user, updates);
        const executorId = getSessionUserId(req) || 'usr-001';
        const executor = store.users.find(u => u.id === executorId) || store.users[0];
        store.addAuditLog(executor.id, executor.name, executor.role, 'USER_STATUS_CHANGE', 'SECURITY', `Updated user ${user.name} status to ${user.status}`);
      }
      res.json({ success: true, data: user });
    });

    // Metrics
    app.get('/api/metrics', (req, res) => {
      res.json({ success: true, data: store.getMetrics() });
    });

    // Inventory Products
    app.get('/api/inventory/products', (req, res) => {
      const { search, category, brand, prescriptionRequired } = req.query;
      let list = [...store.products];
      if (search) {
        const s = String(search).toLowerCase();
        list = list.filter(p => p.name.toLowerCase().includes(s) || p.sku.toLowerCase().includes(s));
      }
      if (category) {
        list = list.filter(p => p.categoryId === category);
      }
      if (brand) {
        list = list.filter(p => p.brandId === brand);
      }
      if (prescriptionRequired !== undefined) {
        const isReq = String(prescriptionRequired) === 'true';
        list = list.filter(p => p.prescriptionRequired === isReq);
      }
      res.json({ success: true, data: list });
    });

    app.post('/api/inventory/products', (req, res) => {
      const productData = req.body;
      const newProd = {
        id: `prod-${Date.now()}`,
        name: productData.name,
        sku: productData.sku,
        hsnCode: productData.hsnCode || '30040000',
        categoryId: productData.categoryId,
        categoryName: store.categories.find(c => c.id === productData.categoryId)?.name || 'General',
        brandId: productData.brandId,
        brandName: store.brands.find(b => b.id === productData.brandId)?.name || 'Generic',
        description: productData.description || '',
        unitOfMeasure: productData.unitOfMeasure || 'BOX',
        b2cPrice: Number(productData.b2cPrice) || 0,
        b2bPriceTier1: Number(productData.b2bPriceTier1) || 0,
        b2bPriceTier2: Number(productData.b2bPriceTier2) || 0,
        mrp: Number(productData.mrp) || 0,
        taxRatePercent: Number(productData.taxRatePercent) || 12,
        prescriptionRequired: productData.prescriptionRequired || false,
        minStockAlert: Number(productData.minStockAlert) || 50,
        imageUrl: productData.imageUrl || 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500',
        storageCondition: productData.storageCondition || 'ROOM_TEMP',
        active: true,
      };
      store.products.push(newProd);
      const executorId = getSessionUserId(req) || 'usr-001';
      const executor = store.users.find(u => u.id === executorId) || store.users[0];
      store.addAuditLog(executor.id, executor.name, executor.role, 'PRODUCT_CREATE', 'INVENTORY', `Created new SKU: ${newProd.sku}`);
      res.json({ success: true, data: newProd });
    });

    // Batches
    app.get('/api/inventory/batches', (req, res) => {
      const { warehouseId, productId, status } = req.query;
      let list = [...store.batches];
      if (warehouseId) {
        list = list.filter(b => b.warehouseId === warehouseId);
      }
      if (productId) {
        list = list.filter(b => b.productId === productId);
      }
      if (status) {
        list = list.filter(b => b.status === status);
      }
      res.json({ success: true, data: list });
    });

    app.post('/api/inventory/batches', (req, res) => {
      const data = req.body;
      const product = store.products.find(p => p.id === data.productId);
      const warehouse = store.warehouses.find(w => w.id === data.warehouseId);
      const newBatch = {
        id: `btc-${Date.now()}`,
        productId: data.productId,
        productName: product?.name || 'Unknown Product',
        productSku: product?.sku || 'SKU',
        warehouseId: data.warehouseId,
        warehouseName: warehouse?.name || 'Unknown Warehouse',
        batchNumber: data.batchNumber,
        manufacturingDate: data.manufacturingDate,
        expiryDate: data.expiryDate,
        mrp: Number(data.mrp) || product?.mrp || 0,
        b2bPrice: Number(data.b2bPrice) || product?.b2bPriceTier1 || 0,
        quantityOnHand: Number(data.quantityOnHand) || 0,
        quantityReserved: 0,
        quantityAvailable: Number(data.quantityOnHand) || 0,
        coldChainMonitored: data.coldChainMonitored || false,
        tempReadingCelsius: data.tempReadingCelsius ? Number(data.tempReadingCelsius) : undefined,
        status: data.status || 'ACTIVE',
      };
      store.batches.push(newBatch);
      const executorId = getSessionUserId(req) || 'usr-001';
      const executor = store.users.find(u => u.id === executorId) || store.users[0];
      store.addAuditLog(executor.id, executor.name, executor.role, 'BATCH_INWARD', 'INVENTORY', `Inwarded batch ${newBatch.batchNumber} for SKU ${newBatch.productSku}`);
      res.json({ success: true, data: newBatch });
    });

    app.put('/api/inventory/batches/:id', (req, res) => {
      const { id } = req.params;
      const updates = req.body;
      const batch = store.batches.find(b => b.id === id);
      if (batch) {
        Object.assign(batch, updates);
        if (updates.quantityOnHand !== undefined) {
          batch.quantityAvailable = Number(updates.quantityOnHand) - (batch.quantityReserved || 0);
        }
        const executorId = getSessionUserId(req) || 'usr-001';
        const executor = store.users.find(u => u.id === executorId) || store.users[0];
        store.addAuditLog(executor.id, executor.name, executor.role, 'BATCH_UPDATE', 'INVENTORY', `Updated batch ${batch.batchNumber} details.`);
      }
      res.json({ success: true, data: batch });
    });

    app.get('/api/inventory/categories', (req, res) => {
      res.json({ success: true, data: store.categories });
    });

    app.get('/api/inventory/brands', (req, res) => {
      res.json({ success: true, data: store.brands });
    });

    // Warehouses
    app.get('/api/warehouses', (req, res) => {
      res.json({ success: true, data: store.warehouses });
    });

    app.get('/api/warehouses/transfers', (req, res) => {
      res.json({ success: true, data: store.stockTransfers });
    });

    app.post('/api/warehouses/transfers', (req, res) => {
      const data = req.body;
      const fromWh = store.warehouses.find(w => w.id === data.fromWarehouseId);
      const toWh = store.warehouses.find(w => w.id === data.toWarehouseId);
      const batch = store.batches.find(b => b.id === data.batchId);
      const product = store.products.find(p => p.id === data.productId);

      if (batch && batch.quantityAvailable < data.quantity) {
        return res.status(400).json({ success: false, error: 'Insufficient quantity available in source batch' });
      }

      if (batch) {
        batch.quantityOnHand -= Number(data.quantity);
        batch.quantityAvailable -= Number(data.quantity);
      }

      let targetBatch = store.batches.find(b => b.productId === data.productId && b.warehouseId === data.toWarehouseId && b.batchNumber === batch?.batchNumber);
      if (!targetBatch && batch) {
        targetBatch = {
          id: `btc-${Date.now()}-target`,
          productId: batch.productId,
          productName: batch.productName,
          productSku: batch.productSku,
          warehouseId: data.toWarehouseId,
          warehouseName: toWh?.name || 'Target Warehouse',
          batchNumber: batch.batchNumber,
          manufacturingDate: batch.manufacturingDate,
          expiryDate: batch.expiryDate,
          mrp: batch.mrp,
          b2bPrice: batch.b2bPrice,
          quantityOnHand: Number(data.quantity),
          quantityReserved: 0,
          quantityAvailable: Number(data.quantity),
          coldChainMonitored: batch.coldChainMonitored,
          tempReadingCelsius: batch.tempReadingCelsius,
          status: batch.status,
        };
        store.batches.push(targetBatch);
      } else if (targetBatch) {
        targetBatch.quantityOnHand += Number(data.quantity);
        targetBatch.quantityAvailable += Number(data.quantity);
      }

      const newTransfer = {
        id: `trf-${Date.now()}`,
        transferNumber: `ST-${Date.now().toString().slice(-4)}`,
        fromWarehouseId: data.fromWarehouseId,
        fromWarehouseName: fromWh?.name || 'Source',
        toWarehouseId: data.toWarehouseId,
        toWarehouseName: toWh?.name || 'Target',
        productId: data.productId,
        productName: product?.name || 'Product',
        batchId: data.batchId,
        batchNumber: batch?.batchNumber || 'B000',
        quantity: Number(data.quantity),
        requestedBy: data.requestedBy || 'System',
        approvedBy: 'Sarah Jenkins',
        status: 'COMPLETED',
        createdAt: new Date().toISOString(),
        notes: data.notes || '',
      };

      store.stockTransfers.unshift(newTransfer as any);
      const executorId = getSessionUserId(req) || 'usr-001';
      const executor = store.users.find(u => u.id === executorId) || store.users[0];
      store.addAuditLog(executor.id, executor.name, executor.role, 'STOCK_TRANSFER', 'WAREHOUSE', `Transferred ${newTransfer.quantity} units of ${newTransfer.productName} from ${newTransfer.fromWarehouseName} to ${newTransfer.toWarehouseName}`);

      res.json({ success: true, data: newTransfer });
    });

    // Orders
    app.get('/api/orders', (req, res) => {
      const { type, customerId, status } = req.query;
      let list = [...store.orders];
      if (type) {
        list = list.filter(o => o.orderType === type);
      }
      if (customerId) {
        list = list.filter(o => o.customerId === customerId);
      }
      if (status) {
        list = list.filter(o => o.orderStatus === status);
      }
      res.json({ success: true, data: list });
    });

    app.post('/api/orders', (req, res) => {
      const data = req.body;
      const orderId = `ord-${Date.now()}`;
      const orderNumber = `MSO-${data.orderType}-${Date.now().toString().slice(-4)}`;
      const warehouse = store.warehouses[0];

      const finalItems = data.items.map((it: any, index: number) => {
        const prod = store.products.find(p => p.id === it.productId);
        const eligibleBatches = store.batches
          .filter(b => b.productId === it.productId && b.quantityAvailable >= it.quantity && b.status === 'ACTIVE')
          .sort((a, b) => new Date(a.expiryDate).getTime() - new Date(b.expiryDate).getTime());
        
        const selectedBatch = eligibleBatches[0] || store.batches.find(b => b.productId === it.productId) || store.batches[0];
        
        if (selectedBatch) {
          selectedBatch.quantityReserved = (selectedBatch.quantityReserved || 0) + Number(it.quantity);
          selectedBatch.quantityAvailable = selectedBatch.quantityOnHand - selectedBatch.quantityReserved;
        }

        const unitPrice = data.orderType === 'B2B' ? (prod?.b2bPriceTier1 || 10) : (prod?.b2cPrice || 15);
        const totalPrice = unitPrice * Number(it.quantity);
        const taxRate = prod?.taxRatePercent || 12;
        const taxAmount = (totalPrice * taxRate) / 100;

        return {
          id: `item-${Date.now()}-${index}`,
          productId: it.productId,
          batchId: selectedBatch?.id || 'btc-unknown',
          productName: prod?.name || 'Product',
          productSku: prod?.sku || 'SKU',
          batchNumber: selectedBatch?.batchNumber || 'MOCK-BATCH',
          quantity: Number(it.quantity),
          unitPrice,
          mrp: prod?.mrp || unitPrice * 1.2,
          taxRate,
          taxAmount,
          totalPrice: totalPrice + taxAmount,
        };
      });

      const subtotal = finalItems.reduce((sum: number, it: any) => sum + (it.unitPrice * it.quantity), 0);
      const taxAmount = finalItems.reduce((sum: number, it: any) => sum + it.taxAmount, 0);
      const totalAmount = subtotal + taxAmount - (Number(data.discountAmount) || 0);

      const newOrder = {
        id: orderId,
        orderNumber,
        orderType: data.orderType,
        customerId: data.customerId || 'usr-008',
        customerName: data.customerName || 'Retail Customer',
        customerEmail: data.customerEmail || 'retail@medsupply.com',
        salesmanId: data.salesmanId,
        salesmanName: data.salesmanName,
        deliveryBoyId: undefined,
        deliveryBoyName: undefined,
        warehouseId: warehouse.id,
        warehouseName: warehouse.name,
        items: finalItems,
        subtotal,
        taxAmount,
        discountAmount: Number(data.discountAmount) || 0,
        totalAmount,
        paymentStatus: data.paymentStatus || (data.orderType === 'B2B' ? 'PENDING_CREDIT' : 'PAID'),
        paymentMethod: data.paymentMethod || (data.orderType === 'B2B' ? 'CREDIT_TERM' : 'RAZORPAY'),
        orderStatus: data.orderType === 'B2B' ? 'PENDING_APPROVAL' : 'PROCESSING',
        deliveryAddress: data.deliveryAddress || '123 Medical Way',
        poNumber: data.poNumber || '',
        prescriptionUrl: data.prescriptionUrl || '',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      store.orders.unshift(newOrder as any);

      const invoice = {
        id: `inv-${Date.now()}`,
        invoiceNumber: `INV-MS-${Date.now().toString().slice(-4)}`,
        orderId,
        orderNumber,
        customerId: newOrder.customerId,
        customerName: newOrder.customerName,
        gstin: data.gstin || '27AAAAA0000A1Z5',
        subtotal,
        cgst: taxAmount / 2,
        sgst: taxAmount / 2,
        igst: 0,
        totalAmount,
        pdfGeneratedAt: new Date().toISOString(),
        paymentDueDate: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString(),
        status: newOrder.paymentStatus === 'PAID' ? 'PAID' : 'UNPAID',
      };

      store.invoices.unshift(invoice as any);

      const executorId = getSessionUserId(req) || 'usr-001';
      const executor = store.users.find(u => u.id === executorId) || store.users[0];
      store.addAuditLog(executor.id, executor.name, executor.role, 'ORDER_CREATE', 'ORDER', `Created ${newOrder.orderType} order ${orderNumber} totaling $${totalAmount.toFixed(2)}`);

      res.json({ success: true, data: { order: newOrder, invoice } });
    });

    app.put('/api/orders/:id/status', (req, res) => {
      const { id } = req.params;
      const { status } = req.body;
      const order = store.orders.find(o => o.id === id);
      if (order) {
        const prevStatus = order.orderStatus;
        order.orderStatus = status;
        order.updatedAt = new Date().toISOString();

        if ((status === 'DISPATCHED' || status === 'DELIVERED') && prevStatus !== 'DISPATCHED' && prevStatus !== 'DELIVERED') {
          order.items.forEach(it => {
            const batch = store.batches.find(b => b.id === it.batchId);
            if (batch) {
              batch.quantityReserved = Math.max(0, (batch.quantityReserved || 0) - Number(it.quantity));
              batch.quantityOnHand = Math.max(0, batch.quantityOnHand - Number(it.quantity));
              batch.quantityAvailable = batch.quantityOnHand - batch.quantityReserved;
            }
          });
        }

        if (status === 'APPROVED_B2B') {
          order.paymentStatus = 'CREDIT_APPROVED';
          order.orderStatus = 'PROCESSING';
        }

        if (status === 'DISPATCHED' || status === 'OUT_FOR_DELIVERY') {
          const dboy = store.users.find(u => u.role === 'DELIVERY_BOY') || store.users[4];
          order.deliveryBoyId = dboy.id;
          order.deliveryBoyName = dboy.name;

          const exists = store.deliveryTasks.some(d => d.orderId === order.id);
          if (!exists) {
            const task = {
              id: `dt-${Date.now()}`,
              deliveryNumber: `DEL-${Date.now().toString().slice(-4)}`,
              orderId: order.id,
              orderNumber: order.orderNumber,
              deliveryBoyId: dboy.id,
              deliveryBoyName: dboy.name,
              customerName: order.customerName,
              phone: dboy.phone,
              deliveryAddress: order.deliveryAddress,
              currentLat: 41.8781,
              currentLng: -87.6298,
              estimatedArrivalMinutes: 20,
              status: 'ASSIGNED',
              otpCode: '1234',
              notes: 'Deliver securely. Handle cold chain if applicable.',
              updatedAt: new Date().toISOString(),
            };
            store.deliveryTasks.unshift(task as any);
          }
        }

        const executorId = getSessionUserId(req) || 'usr-001';
        const executor = store.users.find(u => u.id === executorId) || store.users[0];
        store.addAuditLog(executor.id, executor.name, executor.role, 'ORDER_STATUS_UPDATE', 'ORDER', `Order ${order.orderNumber} status changed from ${prevStatus} to ${status}`);
      }
      res.json({ success: true, data: order });
    });

    // Invoices
    app.get('/api/invoices', (req, res) => {
      res.json({ success: true, data: store.invoices });
    });

    // Deliveries
    app.get('/api/deliveries', (req, res) => {
      res.json({ success: true, data: store.deliveryTasks });
    });

    app.put('/api/deliveries/:id/status', (req, res) => {
      const { id } = req.params;
      const { status, otpCode, currentLat, currentLng } = req.body;
      const task = store.deliveryTasks.find(d => d.id === id);
      if (task) {
        const order = store.orders.find(o => o.id === task.orderId);
        
        if (status === 'DELIVERED') {
          if (otpCode && otpCode !== task.otpCode) {
            return res.status(400).json({ success: false, error: 'Invalid 4-digit Handover OTP' });
          }
          task.status = 'DELIVERED';
          if (order) {
            order.orderStatus = 'DELIVERED';
            order.paymentStatus = 'PAID';
            order.updatedAt = new Date().toISOString();

            order.items.forEach(it => {
              const batch = store.batches.find(b => b.id === it.batchId);
              if (batch) {
                const reservedAlloc = Math.min(batch.quantityReserved || 0, Number(it.quantity));
                batch.quantityReserved = Math.max(0, (batch.quantityReserved || 0) - reservedAlloc);
                batch.quantityOnHand = Math.max(0, batch.quantityOnHand - (Number(it.quantity) - reservedAlloc));
                batch.quantityAvailable = batch.quantityOnHand - batch.quantityReserved;
              }
            });
          }
        } else {
          task.status = status as any;
        }

        if (currentLat) task.currentLat = Number(currentLat);
        if (currentLng) task.currentLng = Number(currentLng);
        task.updatedAt = new Date().toISOString();

        const executorId = getSessionUserId(req) || 'usr-001';
        const executor = store.users.find(u => u.id === executorId) || store.users[0];
        store.addAuditLog(executor.id, executor.name, executor.role, 'DELIVERY_UPDATE', 'DELIVERY', `Delivery ${task.deliveryNumber} status updated to ${status}`);
      }
      res.json({ success: true, data: task });
    });

    // Sales Leads
    app.get('/api/salesman/leads', (req, res) => {
      res.json({ success: true, data: store.salesmanLeads });
    });

    app.post('/api/salesman/leads', (req, res) => {
      const leadData = req.body;
      const newLead = {
        id: `lead-${Date.now()}`,
        salesmanId: leadData.salesmanId || 'usr-004',
        pharmacyName: leadData.pharmacyName,
        contactPerson: leadData.contactPerson,
        phone: leadData.phone || '',
        city: leadData.city || 'Chicago',
        estimatedMonthlyValue: Number(leadData.estimatedMonthlyValue) || 10000,
        status: leadData.status || 'PROSPECT',
      };
      store.salesmanLeads.push(newLead);
      const executorId = getSessionUserId(req) || 'usr-001';
      const executor = store.users.find(u => u.id === executorId) || store.users[0];
      store.addAuditLog(executor.id, executor.name, executor.role, 'LEAD_CREATE', 'SALESMAN', `Created CRM sales prospect for ${newLead.pharmacyName}`);
      res.json({ success: true, data: newLead });
    });

    // Coupon
    app.post('/api/coupons/validate', (req, res) => {
      const { code, amount } = req.body;
      const coupon = store.coupons.find(c => c.code.toUpperCase() === String(code).toUpperCase() && c.active);
      if (!coupon) {
        return res.json({ success: true, data: { valid: false, message: 'Coupon code not found or expired' } });
      }
      if (Number(amount) < coupon.minOrderAmount) {
        return res.json({ success: true, data: { valid: false, message: `Minimum order amount of $${coupon.minOrderAmount} required.` } });
      }
      const rawDiscount = (Number(amount) * coupon.discountPercent) / 100;
      const discount = Math.min(rawDiscount, coupon.maxDiscount);
      res.json({
        success: true,
        data: {
          valid: true,
          coupon,
          discount,
        }
      });
    });

    // Audit Logs
    app.get('/api/audit', (req, res) => {
      res.json({ success: true, data: store.auditLogs });
    });

    // Architecture
    app.get('/api/architecture/java-spring', (req, res) => {
      res.json({
        success: true,
        data: {
          architecture: 'Modular Monolith',
          framework: 'Spring Boot 3.5.1',
          jdk: 'Java 21 Enterprise JRE',
          database: 'PostgreSQL 16',
          status: 'COMPLIANT_PRODUCTION',
          verificationState: 'VERIFIED_100',
        }
      });
    });

    // Reports Reports
    app.get('/api/reports/metrics', (req, res) => {
      const activeAccts = store.users.filter(u => u.status === 'ACTIVE').length;
      const lowStock = store.products.filter(p => p.minStockAlert > 100).length || 3;
      res.json({
        success: true,
        data: {
          totalRevenueB2B: store.getMetrics().totalRevenueB2B,
          totalRevenueB2C: store.getMetrics().totalRevenueB2C,
          activeAccounts: activeAccts,
          lowStockAlerts: lowStock,
          expiredLots: store.getMetrics().expiredCount,
        }
      });
    });

    app.get('/api/reports/near-expiry', (req, res) => {
      const nearExp = store.batches.filter(b => b.status === 'NEAR_EXPIRY');
      res.json({ success: true, data: nearExp });
    });

    app.get('/api/reports/low-stock', (req, res) => {
      const lowStockAlerts = store.products.slice(0, 3).map(p => ({
        id: p.id,
        name: p.name,
        sku: p.sku,
        currentQty: 42,
        minQty: p.minStockAlert,
      }));
      res.json({ success: true, data: lowStockAlerts });
    });

    app.get('/api/reports/sales', (req, res) => {
      res.json({
        success: true,
        data: {
          labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul'],
          salesB2B: [40000, 48000, 52000, 60000, 72000, 85000, store.getMetrics().totalRevenueB2B],
          salesB2C: [12000, 14000, 18000, 19000, 24000, 26000, store.getMetrics().totalRevenueB2C],
        }
      });
    });

    // S3
    app.get('/api/s3/presigned-url', (req, res) => {
      const { key } = req.query;
      res.json({
        success: true,
        data: {
          url: `https://medsupply-s3-production.s3.amazonaws.com/uploads/${key || 'file'}`
        }
      });
    });
  }

  // Serve static assets and Vite middleware
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true, hmr: false },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`[MedSupply Proxy Server] Running on http://0.0.0.0:${PORT}`);
    if (!useMockBackend) {
      console.log(`[MedSupply Proxy Server] Forwarding /api requests to ${process.env.SPRING_BOOT_BACKEND_URL}`);
    }
  });
}

startServer();

