// Sirve el build de producción de Angular y reenvía /api al backend Spring Boot.
// Uso: node serve-prod.mjs  (PORT=4200 por defecto, API_TARGET=http://127.0.0.1:8095)
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';

const PORT = Number(process.env.PORT || 4200);
const ROOT = path.resolve(process.env.ROOT || 'dist/recovr-frontend/browser');
const API = new URL(process.env.API_TARGET || 'http://127.0.0.1:8095');
const AGENTE = new URL(process.env.AGENT_TARGET || 'http://127.0.0.1:4400');

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.webp': 'image/webp',
  '.avif': 'image/avif',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.mp4': 'video/mp4',
  '.webm': 'video/webm',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.txt': 'text/plain; charset=utf-8',
  '.map': 'application/json; charset=utf-8',
};

const COMPRIMIBLE = /^(text\/|application\/(json|javascript)|image\/svg)/;

function proxy(req, res, destino = API) {
  const objetivo = new URL(req.url, destino);
  const cabeceras = { ...req.headers, host: destino.host };
  const peticion = http.request(
    {
      protocol: destino.protocol,
      hostname: destino.hostname,
      port: destino.port || 80,
      path: objetivo.pathname + objetivo.search,
      method: req.method,
      headers: cabeceras,
    },
    (respuesta) => {
      res.writeHead(respuesta.statusCode || 502, respuesta.headers);
      respuesta.pipe(res);
    },
  );
  peticion.on('error', (error) => {
    if (!res.headersSent) {
      res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' });
    }
    res.end(JSON.stringify({ status: 502, error: 'Backend no disponible', detalle: error.message }));
  });
  req.pipe(peticion);
}

function servirArchivo(req, res, archivo, esIndex = false) {
  fs.stat(archivo, (error, info) => {
    if (error || !info.isFile()) {
      res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      res.end('No encontrado');
      return;
    }
    const extension = path.extname(archivo).toLowerCase();
    const tipo = MIME[extension] || 'application/octet-stream';
    const hashed = /\.[0-9a-zA-Z_-]{8,}\./.test(path.basename(archivo));
    const cabeceras = {
      'Content-Type': tipo,
      'Cache-Control': esIndex
        ? 'no-cache'
        : hashed
          ? 'public, max-age=31536000, immutable'
          : 'public, max-age=3600',
      'X-Content-Type-Options': 'nosniff',
    };

    const aceptaGzip = /\bgzip\b/.test(req.headers['accept-encoding'] || '');
    if (aceptaGzip && COMPRIMIBLE.test(tipo) && info.size > 1024) {
      cabeceras['Content-Encoding'] = 'gzip';
      cabeceras['Vary'] = 'Accept-Encoding';
      res.writeHead(200, cabeceras);
      if (req.method === 'HEAD') return res.end();
      fs.createReadStream(archivo).pipe(zlib.createGzip({ level: 6 })).pipe(res);
      return;
    }

    cabeceras['Content-Length'] = info.size;
    res.writeHead(200, cabeceras);
    if (req.method === 'HEAD') return res.end();
    fs.createReadStream(archivo).pipe(res);
  });
}

const servidor = http.createServer((req, res) => {
  const url = new URL(req.url || '/', 'http://local');
  if (url.pathname === '/api' || url.pathname.startsWith('/api/')) {
    proxy(req, res);
    return;
  }
  if (url.pathname === '/agente' || url.pathname.startsWith('/agente/')) {
    proxy(req, res, AGENTE);
    return;
  }

  const relativa = decodeURIComponent(url.pathname).replace(/^\/+/, '');
  const candidato = path.resolve(ROOT, relativa);
  const dentro = candidato === ROOT || candidato.startsWith(ROOT + path.sep);

  if (relativa && dentro) {
    fs.stat(candidato, (error, info) => {
      if (!error && info.isFile()) {
        servirArchivo(req, res, candidato);
      } else {
        servirArchivo(req, res, path.join(ROOT, 'index.html'), true);
      }
    });
    return;
  }

  servirArchivo(req, res, path.join(ROOT, 'index.html'), true);
});

servidor.listen(PORT, process.env.HOST || '0.0.0.0', () => {
  console.log(`RECOVR listo en http://0.0.0.0:${PORT} (build de producción, /api -> ${API.origin})`);
});
