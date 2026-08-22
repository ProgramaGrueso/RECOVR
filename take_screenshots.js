const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

async function main() {
  const screenshotsDir = path.join(__dirname, 'screenshots');
  if (!fs.existsSync(screenshotsDir)) {
    fs.mkdirSync(screenshotsDir, { recursive: true });
  }

  const browser = await chromium.launch({ headless: true });
  const viewports = [
    { name: 'desktop', width: 1440, height: 900 },
    { name: 'mobile', width: 390, height: 844 }
  ];

  const generatedFiles = [];

  for (const vp of viewports) {
    const context = await browser.newContext({
      viewport: { width: vp.width, height: vp.height },
      deviceScaleFactor: 2
    });
    const page = await context.newPage();

    // 1. Home Completo (fullPage: true)
    console.log(`Capturando Home (${vp.name})...`);
    await page.goto('http://localhost:4200/', { waitUntil: 'networkidle' });
    await page.waitForTimeout(1000);
    const homePath = path.join(screenshotsDir, `home-${vp.name}.png`);
    await page.screenshot({ path: homePath, fullPage: true });
    generatedFiles.push(homePath);

    // 2. Menu Overlay Abierto
    console.log(`Capturando Menu Overlay (${vp.name})...`);
    await page.goto('http://localhost:4200/', { waitUntil: 'networkidle' });
    await page.waitForTimeout(500);
    await page.click('.menu-toggle-btn');
    await page.waitForTimeout(600);
    const overlayPath = path.join(screenshotsDir, `menu-overlay-${vp.name}.png`);
    await page.screenshot({ path: overlayPath });
    generatedFiles.push(overlayPath);

    // 3. Catálogo de Servicios
    console.log(`Capturando Catálogo (${vp.name})...`);
    await page.goto('http://localhost:4200/catalogo', { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    const catalogoPath = path.join(screenshotsDir, `catalogo-${vp.name}.png`);
    await page.screenshot({ path: catalogoPath, fullPage: true });
    generatedFiles.push(catalogoPath);

    // 4. Flujo de Reserva - Paso 1 (Selección de Protocolo)
    console.log(`Capturando Reserva Paso 1 (${vp.name})...`);
    await page.goto('http://localhost:4200/reserva', { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    // Forzar click en Paso 1 en el stepper si no estuviera activo
    const step1Btn = await page.$('.step:first-child');
    if (step1Btn) await step1Btn.click();
    await page.waitForTimeout(600);
    const paso1Path = path.join(screenshotsDir, `reserva-paso1-${vp.name}.png`);
    await page.screenshot({ path: paso1Path, fullPage: true });
    generatedFiles.push(paso1Path);

    // 4. Flujo de Reserva - Paso 2 (Selección de Profesional)
    console.log(`Capturando Reserva Paso 2 (${vp.name})...`);
    await page.goto('http://localhost:4200/reserva?serviceId=srv-01', { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    const paso2Path = path.join(screenshotsDir, `reserva-paso2-${vp.name}.png`);
    await page.screenshot({ path: paso2Path, fullPage: true });
    generatedFiles.push(paso2Path);

    // 4. Flujo de Reserva - Paso 3 (Selección de Horario)
    console.log(`Capturando Reserva Paso 3 (${vp.name})...`);
    const profOption = await page.$('.prof-item-option');
    if (profOption) await profOption.click();
    await page.waitForTimeout(800);
    const paso3Path = path.join(screenshotsDir, `reserva-paso3-${vp.name}.png`);
    await page.screenshot({ path: paso3Path, fullPage: true });
    generatedFiles.push(paso3Path);

    // 4. Flujo de Reserva - Paso 4 (Confirmación)
    console.log(`Capturando Reserva Paso 4 (${vp.name})...`);
    const slotBtn = await page.$('.slot-btn');
    if (slotBtn) await slotBtn.click();
    await page.waitForTimeout(300);
    const nextBtn = await page.$('button:has-text("Continuar")');
    if (nextBtn) await nextBtn.click();
    await page.waitForTimeout(800);
    const paso4Path = path.join(screenshotsDir, `reserva-paso4-${vp.name}.png`);
    await page.screenshot({ path: paso4Path, fullPage: true });
    generatedFiles.push(paso4Path);

    // 5. Panel Admin
    console.log(`Capturando Panel Admin (${vp.name})...`);
    await page.goto('http://localhost:4200/admin', { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    const adminPath = path.join(screenshotsDir, `admin-${vp.name}.png`);
    await page.screenshot({ path: adminPath, fullPage: true });
    generatedFiles.push(adminPath);

    await context.close();
  }

  await browser.close();

  console.log('\n========================================');
  console.log('CAPTURAS DE PANTALLA GENERADAS EXITOSAMENTE');
  console.log('========================================\n');
  generatedFiles.forEach(f => console.log(f));
}

main().catch(err => {
  console.error('Error al generar capturas:', err);
  process.exit(1);
});
