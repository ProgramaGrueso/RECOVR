import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { ServiceItem } from '../models/service.model';
import { SmoothScrollService } from './smooth-scroll.service';

/**
 * Puente entre la asistente y la página: le permite llevar al usuario a una sección o abrir un tratamiento
 * aunque esté en otra ruta. La portada registra cómo se abre un tratamiento (su modal de video).
 */
@Injectable({ providedIn: 'root' })
export class AccionesPaginaService {
  private router = inject(Router);
  private scroll = inject(SmoothScrollService);
  private abrirTratamientoEnPortada?: (servicio: ServiceItem) => void;

  registrarPortada(abrir: (servicio: ServiceItem) => void): () => void {
    this.abrirTratamientoEnPortada = abrir;
    return () => {
      if (this.abrirTratamientoEnPortada === abrir) this.abrirTratamientoEnPortada = undefined;
    };
  }

  irASeccion(id: string): void {
    this.enPortada(() => {
      const el = document.getElementById(id);
      if (el) this.scroll.scrollTo(el, -80);
    });
  }

  abrirTratamiento(servicio: ServiceItem): void {
    this.enPortada(() => this.abrirTratamientoEnPortada?.(servicio));
  }

  irA(ruta: string): void {
    void this.router.navigateByUrl(ruta);
  }

  /** Ejecuta la acción en la portada; si el usuario está en otra ruta, primero vuelve a ella. */
  private enPortada(accion: () => void): void {
    const ruta = this.router.url.split(/[?#]/)[0];
    if (ruta === '/' || ruta === '') {
      accion();
      return;
    }
    void this.router.navigateByUrl('/').then(() => setTimeout(accion, 450));
  }
}
