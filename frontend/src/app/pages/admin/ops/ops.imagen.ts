/**
 * Prepara una foto de perfil en el navegador: la redimensiona (máx. 1000 px) y la convierte a JPEG, así se acepta
 * cualquier formato de imagen (JPG, PNG, WebP, etc.) y se envía liviana. El servidor vuelve a validarla y re-codificarla.
 */
export const MAX_FOTO_ORIGINAL = 15 * 1024 * 1024;

export function prepararFoto(archivo: File): Promise<Blob> {
  return new Promise((resolve, reject) => {
    if (!archivo.type.startsWith('image/')) {
      reject(new Error('El archivo seleccionado no es una imagen.'));
      return;
    }
    if (archivo.size > MAX_FOTO_ORIGINAL) {
      reject(new Error('La imagen es demasiado grande (máximo 15 MB).'));
      return;
    }
    const url = URL.createObjectURL(archivo);
    const img = new Image();
    img.onload = () => {
      const escala = Math.min(1, 1000 / Math.max(img.width, img.height));
      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(img.width * escala));
      canvas.height = Math.max(1, Math.round(img.height * escala));
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        URL.revokeObjectURL(url);
        reject(new Error('No se pudo procesar la imagen en este navegador.'));
        return;
      }
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      canvas.toBlob(blob => {
        URL.revokeObjectURL(url);
        blob ? resolve(blob) : reject(new Error('No se pudo convertir la imagen.'));
      }, 'image/jpeg', 0.85);
    };
    img.onerror = () => { URL.revokeObjectURL(url); reject(new Error('No se pudo leer la imagen.')); };
    img.src = url;
  });
}
