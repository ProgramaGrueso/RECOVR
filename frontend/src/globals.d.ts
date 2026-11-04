/** Banderas y APIs del navegador que la app usa y que TypeScript no declara. */
interface Window {
  /** true mientras la intro (splash) está en pantalla; el hero espera a que termine para animarse. */
  __recovrSplash?: boolean;
}

interface Navigator {
  /** Network Information API (no estándar en todos los navegadores). */
  connection?: { saveData?: boolean; effectiveType?: string };
}
