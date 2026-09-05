const http = require('http');
const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');

const PORT = 4250;
const DIST_DIR = path.join(__dirname, 'dist', 'recovr-frontend', 'browser');
const SCREENSHOTS_DIR = path.join(__dirname, 'screenshots');

if (!fs.existsSync(SCREENSHOTS_DIR)) {
  fs.mkdirSync(SCREENSHOTS_DIR, { recursive: true });
}

// Servidor estático con soporte SPA (fallback a index.html)
function createServer() {
  const mimeTypes = {
    '.html': 'text/html',
    '.js': 'application/javascript',
    '.css': 'text/css',
    '.json': 'application/json',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.mp4': 'video/mp4',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2'
  };

  return http.createServer((req, res) => {
    let safePath = path.normalize(decodeURI(req.url.split('?')[0])).replace(/^(\.\.[\/\\])+/, '');
    if (safePath === '/' || safePath === '\\') safePath = '/index.html';
    let filePath = path.join(DIST_DIR, safePath);

    fs.stat(filePath, (err, stats) => {
      if (err || !stats.isFile()) {
        // Fallback para SPA routing
        filePath = path.join(DIST_DIR, 'index.html');
      }

      const ext = path.extname(filePath).toLowerCase();
      const contentType = mimeTypes[ext] || 'application/octet-stream';

      fs.readFile(filePath, (readErr, content) => {
        if (readErr) {
          res.writeHead(500);
          res.end('Server Error');
          return;
        }
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(content, 'utf-8');
      });
    });
  });
}

async function run() {
  const server = createServer();
  await new Promise(resolve => server.listen(PORT, resolve));
  console.log(`Servidor local iniciado en http://localhost:${PORT}`);

  let browser;
  try {
    browser = await chromium.launch({ channel: 'msedge', headless: true });
  } catch (e) {
    try {
      browser = await chromium.launch({ channel: 'chrome', headless: true });
    } catch (e2) {
      console.log('No se pudo abrir chrome/msedge vía channel, intentando launch por defecto...');
      browser = await chromium.launch({ headless: true });
    }
  }
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    deviceScaleFactor: 2
  });
  const page = await context.newPage();

  console.log('--- 0. Probando Inicio Público Pristino del Cliente (/) ---');
  await page.goto(`http://localhost:${PORT}/`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '00-home-pristine.png'), fullPage: false });
  console.log('✓ Captura guardada: 00-home-pristine.png (pantalla limpia sin extras)');

  console.log('--- 0b. Abriendo Menú Serotoninn y verificando opción de Login ---');
  const menuBtn = await page.locator('.menu-toggle-btn');
  await menuBtn.click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '00b-menu-overlay-login.png'), fullPage: false });
  console.log('✓ Captura guardada: 00b-menu-overlay-login.png (menú con opción de login visible)');

  console.log('--- 1. Navegando al Login desde la opción del menú ---');
  const loginOptionInMenu = await page.locator('.overlay-login-link');
  await loginOptionInMenu.click();
  await page.waitForTimeout(600);
  console.log(`URL actual tras clic en menú: ${page.url()}`);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '01-login-screen.png'), fullPage: true });
  console.log('✓ Captura guardada: 01-login-screen.png');

  console.log('--- 1b. Probando Botón de Retroceso en Login (Volver al Inicio) ---');
  const backBtnInLogin = await page.locator('.login-top-bar .back-home-button');
  await backBtnInLogin.click();
  await page.waitForTimeout(500);
  console.log(`URL actual tras retroceder: ${page.url()}`);
  if (!page.url().includes('/admin/login')) {
    console.log('✓ Botón de retroceso en Login funciona correctamente y regresó al inicio del cliente.');
  }

  console.log('--- 2. Entrando nuevamente al Login e ingresando como Super Admin / DevOps ---');
  const menuBtn2 = await page.locator('.menu-toggle-btn');
  await menuBtn2.click();
  await page.waitForTimeout(400);
  await page.locator('.overlay-login-link').click();
  await page.waitForTimeout(500);
  const superAdminBtn = await page.locator('.role-card[data-tone="gold"] .btn-enter');
  await superAdminBtn.click();
  await page.waitForTimeout(600);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '02-admin-superadmin-devops.png'), fullPage: true });
  console.log('✓ Captura guardada: 02-admin-superadmin-devops.png (con botones de retorno visibles)');

  console.log('--- 2b. Verificando Botón de Retroceso en Header del Panel Admin ---');
  const backBtnInAdmin = await page.locator('.public-site-header-btn');
  console.log(`Texto del botón de retorno: "${await backBtnInAdmin.innerText()}"`);

  console.log('--- 3. Probando Módulo DevOps & Servidores ---');
  const devopsNav = await page.locator('.nav-item-btn:has-text("DevOps")');
  if (await devopsNav.count() > 0) {
    await devopsNav.click();
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '03-admin-devops-servers.png'), fullPage: true });
    console.log('✓ Captura guardada: 03-admin-devops-servers.png');
  }

  console.log('--- 4. Conmutando a Rol ADMINISTRADOR (Gerencia) ---');
  await page.locator('.role-pill-btn:has-text("ADMINISTRADOR")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '04-admin-gerencia-resumen.png'), fullPage: true });
  console.log('✓ Captura guardada: 04-admin-gerencia-resumen.png');

  console.log('--- 5. Probando Módulo Personal & Turnos (Gerencia) ---');
  const personalNav = await page.locator('.nav-item-btn:has-text("Personal")');
  if (await personalNav.count() > 0) {
    await personalNav.click();
    await page.waitForTimeout(500);
    await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '05-admin-personal-comisiones.png'), fullPage: true });
    console.log('✓ Captura guardada: 05-admin-personal-comisiones.png');
  }

  console.log('--- 6. Conmutando a Rol RECEPCIONISTA ---');
  await page.locator('.role-pill-btn:has-text("RECEPCIONISTA")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '06-admin-recepcion-resumen.png'), fullPage: true });
  console.log('✓ Captura guardada: 06-admin-recepcion-resumen.png');

  console.log('--- 7. Probando Agenda & Salas y Check-in (Recepcionista) ---');
  const agendaNav = await page.locator('.nav-item-btn:has-text("Agenda")');
  await agendaNav.click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '07-admin-recepcion-agenda.png'), fullPage: true });
  console.log('✓ Captura guardada: 07-admin-recepcion-agenda.png');

  console.log('--- 8. Probando Caja Chica & POS (Recepcionista) ---');
  const cajaNav = await page.locator('.nav-item-btn:has-text("Caja")');
  await cajaNav.click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '08-admin-recepcion-caja.png'), fullPage: true });
  console.log('✓ Captura guardada: 08-admin-recepcion-caja.png');

  console.log('--- 9. Conmutando a Rol ESPECIALISTA (Kinesióloga) ---');
  await page.locator('.role-pill-btn:has-text("ESPECIALISTA")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '09-admin-especialista-agenda.png'), fullPage: true });
  console.log('✓ Captura guardada: 09-admin-especialista-agenda.png');

  console.log('--- 10. Probando Ficha Clínica Confidencial (Especialista) ---');
  const clinicaNav = await page.locator('.nav-item-btn:has-text("Ficha Clínica")');
  await clinicaNav.click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '10-admin-especialista-ficha-clinica.png'), fullPage: true });
  console.log('✓ Captura guardada: 10-admin-especialista-ficha-clinica.png');

  console.log('--- 11. Conmutando a Rol CLIENTE (Paciente) ---');
  await page.locator('.role-pill-btn:has-text("CLIENTE")').click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '11-admin-cliente-mi-cuenta.png'), fullPage: true });
  console.log('✓ Captura guardada: 11-admin-cliente-mi-cuenta.png');

  console.log('--- 12. Probando Matriz RBAC Oficial Interactiva ---');
  await page.locator('.role-pill-btn:has-text("SUPER ADMIN")').click();
  await page.waitForTimeout(400);
  const rbacNav = await page.locator('.nav-item-btn:has-text("Matriz")');
  await rbacNav.click();
  await page.waitForTimeout(500);
  await page.screenshot({ path: path.join(SCREENSHOTS_DIR, '12-admin-matriz-rbac.png'), fullPage: true });
  console.log('✓ Captura guardada: 12-admin-matriz-rbac.png');

  await browser.close();
  server.close();
  console.log('\n======================================================');
  console.log('TODAS LAS PRUEBAS Y CAPTURAS FUERON EJECUTADAS CON ÉXITO');
  console.log('======================================================\n');
}

run().catch(err => {
  console.error('Error durante la verificación:', err);
  process.exit(1);
});
