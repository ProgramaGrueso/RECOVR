const { chromium } = require('playwright');
const path = require('path');
const fs = require('fs');

(async () => {
  const browser = await chromium.launch({
    headless: true,
    executablePath: 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'
  });

  const page = await browser.newPage({
    viewport: { width: 1600, height: 1200 },
    deviceScaleFactor: 2
  });

  const htmlPath = path.resolve(__dirname, '..', 'exposicion', 'matriz_roles.html');
  console.log('Cargando HTML desde:', htmlPath);
  await page.goto('file:///' + htmlPath.replace(/\\/g, '/'), { waitUntil: 'networkidle' });

  // Esperar a que carguen las fuentes de Google Fonts
  await page.waitForTimeout(2000);

  const container = await page.$('.container');
  const outPath1 = path.resolve(__dirname, '..', 'exposicion', 'matriz_roles_recovr.png');
  const outPath2 = path.resolve(__dirname, 'exposicion', 'matriz_roles_recovr.png');

  await container.screenshot({ path: outPath1 });
  console.log('Imagen guardada en:', outPath1);

  if (fs.existsSync(path.dirname(outPath2))) {
    fs.copyFileSync(outPath1, outPath2);
    console.log('Imagen copiada en:', outPath2);
  }

  await browser.close();
  console.log('Render finalizado con exito!');
})();
