import express from 'express';
import path from 'path';
import cookieParser from 'cookie-parser';
import compression from 'compression';
import { createServer as createViteServer } from 'vite';
import { createProxyMiddleware } from 'http-proxy-middleware';

async function startServer() {
  // Validate required environment variables at startup
  const portVal = process.env.PORT || '3000';
  if (isNaN(Number(portVal))) {
    console.error(`[Startup Error] PORT "${portVal}" is not a valid number.`);
    process.exit(1);
  }

  let backendUrl = process.env.SPRING_BOOT_BACKEND_URL;
  if (!backendUrl) {
    console.warn('[Startup Warning] SPRING_BOOT_BACKEND_URL is not defined. Defaulting to "http://localhost:8080" for development/local testing.');
    backendUrl = 'http://localhost:8080';
  }

  try {
    new URL(backendUrl);
  } catch (e) {
    console.error(`[Startup Error] SPRING_BOOT_BACKEND_URL "${backendUrl}" is not a valid URL.`);
    process.exit(1);
  }

  const app = express();
  const PORT = Number(portVal);
  const isProd = process.env.NODE_ENV === 'production';

  // Security Hardenings
  app.disable('x-powered-by');
  app.set('trust proxy', 1);

  // Enable gzip/deflate response compression for all traffic
  app.use(compression());

  // Force HTTPS in production behind reverse proxies
  if (isProd) {
    app.use((req, res, next) => {
      const proto = req.headers['x-forwarded-proto'];
      if (proto && proto !== 'https') {
        return res.redirect(301, `https://${req.headers.host}${req.url}`);
      }
      next();
    });
  }

  // Request & Correlation ID logging middleware with strict secure response headers
  app.use((req, res, next) => {
    const correlationId = req.headers['x-correlation-id'] as string || `corr-${Math.random().toString(36).substring(2, 11)}-${Date.now()}`;
    (req as any).correlationId = correlationId;
    res.setHeader('X-Correlation-ID', correlationId);

    // Hardened OWASP Secure Response Headers
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('X-Frame-Options', 'DENY');
    res.setHeader('X-XSS-Protection', '1; mode=block');
    res.setHeader('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
    res.setHeader('Content-Security-Policy', "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob: https:; font-src 'self' data: https:; connect-src 'self' https: ws: wss:; frame-ancestors 'none';");
    res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin');
    res.setHeader('Permissions-Policy', 'camera=(), microphone=(), geolocation=(), interest-cohort=()');

    console.log(`[${new Date().toISOString()}] [ID: ${correlationId}] ${req.method} ${req.originalUrl} - IP: ${req.ip}`);
    next();
  });

  // Simple IP rate limiting middleware for production (sliding window implementation)
  const rateLimitMap = new Map<string, { count: number; resetTime: number }>();
  app.use('/api', (req, res, next) => {
    if (!isProd) {
      return next(); // Skip rate-limiting in development for frictionless testing
    }
    const ip = req.ip || req.headers['x-forwarded-for'] as string || 'unknown';
    const now = Date.now();
    const limit = 150; // Max 150 requests per minute per IP
    const windowMs = 60 * 1000;

    let record = rateLimitMap.get(ip);
    if (!record || now > record.resetTime) {
      record = { count: 0, resetTime: now + windowMs };
    }

    record.count++;
    rateLimitMap.set(ip, record);

    // Occasional cleanup of the cache
    if (rateLimitMap.size > 10000) {
      for (const [k, v] of rateLimitMap.entries()) {
        if (now > v.resetTime) {
          rateLimitMap.delete(k);
        }
      }
    }

    if (record.count > limit) {
      return res.status(429).json({
        success: false,
        error: 'Too Many Requests',
        message: 'Gateway API rate limit exceeded. Please try again later.',
        correlationId: (req as any).correlationId
      });
    }
    next();
  });

  // Cookie parser middleware for session tracking and verification
  app.use(cookieParser());

  // Dynamic, enterprise-ready active health check endpoint checking Spring Boot
  app.get('/api/health', async (req, res) => {
    let backendStatus = 'DOWN';
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 2000); // 2 second timeout for active ping
      const backendHealthUrl = `${backendUrl}/health`;
      const response = await fetch(backendHealthUrl, { signal: controller.signal });
      clearTimeout(timeoutId);
      if (response.ok) {
        backendStatus = 'UP';
      }
    } catch (err) {
      backendStatus = 'DOWN';
    }

    const status = backendStatus === 'UP' ? 'UP' : 'DEGRADED';
    res.status(status === 'UP' ? 200 : 503).json({
      status,
      timestamp: new Date().toISOString(),
      service: 'MedSupply Node Gateway Proxy',
      backendConnection: backendStatus
    });
  });

  // Upstream proxy targeting the Spring Boot production backend
  console.log(`[MedSupply Proxy Server] Forwarding /api requests to Spring Boot backend: ${backendUrl}`);
  
  app.use(
    '/api',
    createProxyMiddleware({
      target: backendUrl,
      changeOrigin: true,
      xfwd: true, // Forward client IP, host, and protocol headers (X-Forwarded-For etc.)
      proxyTimeout: 60000, // Timeout for backend response (60s)
      timeout: 60000, // Timeout for establishing connection (60s)
      pathRewrite: {
        '^/api': '/api/v1',
      },
      on: {
        error: (err, req, res: any) => {
          const correlationId = (req as any).correlationId || 'unknown';
          console.error(`[Proxy Error] [ID: ${correlationId}] Failed to forward request to Spring Boot backend:`, err.message);
          if (res && typeof res.status === 'function') {
            res.status(502).json({
              success: false,
              error: 'Service Temporarily Unavailable (Gateway Error)',
              message: 'Failed to establish connection with upstream Spring Boot server. Please ensure the backend services are fully booted.',
              correlationId
            });
          } else if (res && typeof res.writeHead === 'function') {
            res.writeHead(502, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
              success: false,
              error: 'Service Temporarily Unavailable (Gateway Error)',
              message: 'Failed to establish connection with upstream Spring Boot server. Please ensure the backend services are fully booted.',
              correlationId
            }));
          }
        }
      }
    })
  );

  // Client static assets delivery / Vite dev server integration
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

  // Centralized production error handling middleware
  app.use((err: any, req: express.Request, res: express.Response, next: express.NextFunction) => {
    const correlationId = (req as any).correlationId || 'unknown';
    console.error(`[Unhandled Error] [ID: ${correlationId}] Error occurred:`, err);
    
    // Delegate to the default Express error handler if headers have already been sent to the client
    if (res.headersSent) {
      return next(err);
    }

    res.status(err.status || 500).json({
      success: false,
      error: 'Internal Server Error',
      message: isProd ? 'An unexpected server error occurred. Please contact administrator support.' : err.message,
      correlationId
    });
  });

  const server = app.listen(PORT, '0.0.0.0', () => {
    console.log(`[MedSupply Proxy Server] Running on http://0.0.0.0:${PORT}`);
  });

  // Graceful shutdown protocol
  const shutdown = (signal: string) => {
    console.log(`[MedSupply Proxy Server] Received ${signal}. Initiating graceful shutdown...`);
    server.close(() => {
      console.log('[MedSupply Proxy Server] Closed all active gateway connections. Exiting process.');
      process.exit(0);
    });

    // Enforce shutdown after a maximum wait time of 10 seconds
    setTimeout(() => {
      console.error('[MedSupply Proxy Server] Forced shutdown due to pending connections.');
      process.exit(1);
    }, 10000);
  };

  process.on('SIGTERM', () => shutdown('SIGTERM'));
  process.on('SIGINT', () => shutdown('SIGINT'));
}

startServer();
