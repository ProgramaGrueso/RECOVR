const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

(async () => {
  try {
    const browser = await chromium.launch({
      headless: true,
      executablePath: '/usr/bin/chromium'
    });

    const page = await browser.newPage({
      viewport: { width: 1920, height: 1350 },
      deviceScaleFactor: 2
    });

    const htmlPath = path.resolve(__dirname, 'exposicion', 'mapa_apis.html');
    console.log('Cargando HTML desde:', htmlPath);
    await page.goto('file://' + htmlPath, { waitUntil: 'networkidle' });

    // Esperar fuentes
    await page.waitForTimeout(2000);

    const container = await page.$('.container');
    const outScreenshots1 = path.resolve(__dirname, 'screenshots', '13-mapa-apis-recovr.png');
    const outScreenshots2 = path.resolve(__dirname, 'screenshots', 'mapa_apis_recovr.png');
    const outExposicion = path.resolve(__dirname, 'exposicion', 'mapa_apis_recovr.png');

    await container.screenshot({ path: outScreenshots1 });
    console.log('Guardado en:', outScreenshots1);

    fs.copyFileSync(outScreenshots1, outScreenshots2);
    fs.copyFileSync(outScreenshots1, outExposicion);
    console.log('Copiado con éxito a screenshots y exposicion!');

    await browser.close();
    console.log('Renderizado exitoso completado.');
  } catch (err) {
    console.error('Error renderizando:', err);
    process.exit(1);
  }
})();
