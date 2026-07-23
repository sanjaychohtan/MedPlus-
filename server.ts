import express from 'express';
import path from 'path';
import cookieParser from 'cookie-parser';
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

  // Cookie parser middleware for session tracking and verification
  app.use(cookieParser());

  // Production-ready health endpoint
  app.get('/api/health', (req, res) => {
    res.json({
      status: 'UP',
      timestamp: new Date().toISOString(),
      service: 'MedSupply Node Gateway Proxy',
      backendConnection: 'CONFIGURED'
    });
  });

  // Upstream proxy targeting the Spring Boot production backend
  console.log(`[MedSupply Proxy Server] Forwarding /api requests to Spring Boot backend: ${backendUrl}`);
  
  app.use(
    '/api',
    createProxyMiddleware({
      target: backendUrl,
      changeOrigin: true,
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
    
    res.status(err.status || 500).json({
      success: false,
      error: 'Internal Server Error',
      message: isProd ? 'An unexpected server error occurred. Please contact administrator support.' : err.message,
      correlationId
    });
  });

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`[MedSupply Proxy Server] Running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
