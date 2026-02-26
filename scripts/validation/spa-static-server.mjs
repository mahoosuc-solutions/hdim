#!/usr/bin/env node
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

function parseArgs(argv) {
  const args = { port: 4210, dir: path.resolve(__dirname, '../../dist/apps/clinical-portal/browser') };
  for (let i = 2; i < argv.length; i += 1) {
    const token = argv[i];
    if (token === '--port') {
      args.port = Number(argv[i + 1] ?? args.port);
      i += 1;
      continue;
    }
    if (token === '--dir') {
      args.dir = path.resolve(argv[i + 1] ?? args.dir);
      i += 1;
    }
  }
  return args;
}

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.mjs': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.ico': 'image/x-icon',
  '.txt': 'text/plain; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
};

function safeJoin(root, reqPath) {
  const normalized = path.normalize(reqPath).replace(/^([/\\])+/, '');
  const full = path.join(root, normalized);
  const rootResolved = path.resolve(root);
  const fullResolved = path.resolve(full);
  if (!fullResolved.startsWith(rootResolved)) return null;
  return fullResolved;
}

function sendFile(res, filePath, statusCode = 200) {
  const ext = path.extname(filePath).toLowerCase();
  const type = MIME[ext] ?? 'application/octet-stream';
  res.writeHead(statusCode, { 'Content-Type': type, 'Cache-Control': 'no-store' });
  fs.createReadStream(filePath).pipe(res);
}

function startServer({ port, dir }) {
  if (!fs.existsSync(path.join(dir, 'index.html'))) {
    console.error(`[spa-static-server] Missing index.html in: ${dir}`);
    process.exit(1);
  }

  const server = http.createServer((req, res) => {
    const url = new URL(req.url ?? '/', `http://${req.headers.host ?? 'localhost'}`);
    const pathname = decodeURIComponent(url.pathname || '/');

    if (req.method !== 'GET' && req.method !== 'HEAD') {
      res.writeHead(405, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('Method Not Allowed');
      return;
    }

    let target = pathname === '/' ? '/index.html' : pathname;
    const candidate = safeJoin(dir, target);

    if (candidate && fs.existsSync(candidate) && fs.statSync(candidate).isFile()) {
      if (req.method === 'HEAD') {
        const ext = path.extname(candidate).toLowerCase();
        const type = MIME[ext] ?? 'application/octet-stream';
        res.writeHead(200, { 'Content-Type': type, 'Cache-Control': 'no-store' });
        res.end();
        return;
      }
      sendFile(res, candidate, 200);
      return;
    }

    const spaIndex = path.join(dir, 'index.html');
    if (req.method === 'HEAD') {
      res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8', 'Cache-Control': 'no-store' });
      res.end();
      return;
    }
    sendFile(res, spaIndex, 200);
  });

  server.listen(port, '0.0.0.0', () => {
    console.log(`[spa-static-server] Serving ${dir} on http://localhost:${port}`);
  });

  process.on('SIGINT', () => server.close(() => process.exit(0)));
  process.on('SIGTERM', () => server.close(() => process.exit(0)));
}

startServer(parseArgs(process.argv));
