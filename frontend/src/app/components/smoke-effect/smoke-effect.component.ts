import {
  Component,
  ElementRef,
  ViewChild,
  AfterViewInit,
  OnDestroy,
  inject,
  NgZone
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as THREE from 'three';
import { BookingService } from '../../services/booking.service';

interface SmokeParticle {
  mesh: THREE.Mesh;
  baseScale: number;
  speedY: number;
  speedX: number;
  rotSpeed: number;
  maxOpacity: number;
  minY: number;
  maxY: number;
  phase: number;
  swaySpeed: number;
}

@Component({
  selector: 'app-smoke-effect',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './smoke-effect.component.html',
  styleUrls: ['./smoke-effect.component.scss']
})
export class SmokeEffectComponent implements AfterViewInit, OnDestroy {
  @ViewChild('container') containerRef!: ElementRef<HTMLDivElement>;
  @ViewChild('smokeCanvas') canvasRef!: ElementRef<HTMLCanvasElement>;

  private ngZone = inject(NgZone);
  private bookingService = inject(BookingService);

  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private animationFrameId: number | null = null;
  private isVisible: boolean = true;

  private smokeParticles: SmokeParticle[] = [];
  private textures: THREE.CanvasTexture[] = [];
  private materials: THREE.Material[] = [];
  private geometries: THREE.BufferGeometry[] = [];

  private lightPink!: THREE.PointLight;
  private lightPurple!: THREE.PointLight;
  private lightCyan!: THREE.PointLight;

  private emberPoints!: THREE.Points;
  private emberPositions!: Float32Array;
  private emberSpeeds!: Float32Array;
  private emberCount = 45;

  private mouseX = 0;
  private mouseY = 0;
  private targetMouseX = 0;
  private targetMouseY = 0;

  private resizeObserver?: ResizeObserver;
  private intersectionObserver?: IntersectionObserver;
  private boundOnPointerMove = this.onPointerMove.bind(this);

  ngAfterViewInit(): void {
    if (typeof window === 'undefined') return;

    this.initThree();
    this.setupResizeObserver();
    this.setupIntersectionObserver();
    this.setupPointerEvents();

    this.ngZone.runOutsideAngular(() => {
      this.animate();
    });
  }

  ngOnDestroy(): void {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }

    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
    }

    if (this.intersectionObserver) {
      this.intersectionObserver.disconnect();
    }

    if (this.containerRef?.nativeElement) {
      this.containerRef.nativeElement.removeEventListener('pointermove', this.boundOnPointerMove);
    }

    this.disposeScene();
  }

  onReserveClick(): void {
    this.bookingService.openBookingModal();
  }

  private initThree(): void {
    const canvas = this.canvasRef.nativeElement;
    const container = this.containerRef.nativeElement;
    const width = container.clientWidth || window.innerWidth;
    const height = container.clientHeight || 520;

    // 1. Escena
    this.scene = new THREE.Scene();

    // 2. Cámara de perspectiva
    this.camera = new THREE.PerspectiveCamera(60, width / height, 1, 1500);
    this.camera.position.z = 750;
    this.camera.position.y = 0;

    // 3. Renderer WebGL optimizado
    this.renderer = new THREE.WebGLRenderer({
      canvas,
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance'
    });
    this.renderer.setSize(width, height);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    this.renderer.setClearColor(0x000000, 0);

    // 4. Luces 3D Bubblegum Neón
    const ambientLight = new THREE.AmbientLight(0x201032, 1.8);
    this.scene.add(ambientLight);

    // Luz Neón Rosa Bubblegum
    this.lightPink = new THREE.PointLight(0xff2a85, 9.0, 950);
    this.lightPink.position.set(-180, -120, 180);
    this.scene.add(this.lightPink);

    // Luz Neón Púrpura Orquídea
    this.lightPurple = new THREE.PointLight(0xa855f7, 8.5, 900);
    this.lightPurple.position.set(190, -80, 140);
    this.scene.add(this.lightPurple);

    // Luz de acento Cian suave
    this.lightCyan = new THREE.PointLight(0x38bdf8, 4.0, 800);
    this.lightCyan.position.set(0, -200, 100);
    this.scene.add(this.lightCyan);

    // 5. Generar texturas de humo procedurales
    const smokeTex1 = this.createSmokeTexture(0);
    const smokeTex2 = this.createSmokeTexture(1);
    this.textures.push(smokeTex1, smokeTex2);

    // 6. Crear partículas de humo 3D que emergen desde abajo
    this.createSmokePlumes(smokeTex1, smokeTex2, width);

    // 7. Micro-destellos luminosos ascendentes (Brasas Spa / Neón Stardust)
    this.createAscendingEmbers();
  }

  /**
   * Genera texturas volumétricas de humo hiperrealistas en canvas offscreen
   */
  private createSmokeTexture(type: number): THREE.CanvasTexture {
    const size = 512;
    const canvas = document.createElement('canvas');
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d')!;

    const cx = size / 2;
    const cy = size / 2;

    // Núcleo suave
    const baseRadius = 220;
    const baseGrad = ctx.createRadialGradient(cx, cy, 10, cx, cy, baseRadius);
    baseGrad.addColorStop(0, 'rgba(255, 255, 255, 0.7)');
    baseGrad.addColorStop(0.35, 'rgba(240, 230, 255, 0.35)');
    baseGrad.addColorStop(0.7, 'rgba(210, 190, 240, 0.1)');
    baseGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');

    ctx.fillStyle = baseGrad;
    ctx.beginPath();
    ctx.arc(cx, cy, baseRadius, 0, Math.PI * 2);
    ctx.fill();

    // Capas secundarias orgánicas para generar volutas y bordes de humo
    const puffs = type === 0 ? 14 : 18;
    for (let i = 0; i < puffs; i++) {
      const angle = (i / puffs) * Math.PI * 2 + (Math.random() - 0.5) * 0.5;
      const dist = 40 + Math.random() * 110;
      const px = cx + Math.cos(angle) * dist;
      const py = cy + Math.sin(angle) * dist;
      const radius = 60 + Math.random() * 95;

      const puffGrad = ctx.createRadialGradient(px, py, 5, px, py, radius);
      puffGrad.addColorStop(0, 'rgba(255, 255, 255, 0.32)');
      puffGrad.addColorStop(0.45, 'rgba(235, 215, 255, 0.14)');
      puffGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');

      ctx.fillStyle = puffGrad;
      ctx.beginPath();
      ctx.arc(px, py, radius, 0, Math.PI * 2);
      ctx.fill();
    }

    const texture = new THREE.CanvasTexture(canvas);
    texture.wrapS = THREE.ClampToEdgeWrapping;
    texture.wrapT = THREE.ClampToEdgeWrapping;
    texture.generateMipmaps = true;
    texture.minFilter = THREE.LinearMipmapLinearFilter;
    return texture;
  }

  /**
   * Crea la nube de planos 3D para el humo ascendente
   */
  private createSmokePlumes(tex1: THREE.CanvasTexture, tex2: THREE.CanvasTexture, viewportWidth: number): void {
    const particleCount = 55;
    const geometry = new THREE.PlaneGeometry(360, 360);
    this.geometries.push(geometry);

    // Rango horizontal adaptado al ancho de pantalla
    const spreadX = Math.max(viewportWidth * 0.9, 850);
    const minY = -280;
    const maxY = 320;

    for (let i = 0; i < particleCount; i++) {
      const texture = i % 2 === 0 ? tex1 : tex2;
      const material = new THREE.MeshLambertMaterial({
        map: texture,
        transparent: true,
        opacity: 0,
        depthWrite: false,
        blending: THREE.NormalBlending,
        side: THREE.DoubleSide
      });
      this.materials.push(material);

      const mesh = new THREE.Mesh(geometry, material);

      // Distribución inicial: mayormente en la parte inferior emergiendo hacia arriba
      const initialProgress = Math.random();
      const posX = (Math.random() - 0.5) * spreadX;
      const posY = minY + initialProgress * (maxY - minY);
      const posZ = -320 + Math.random() * 420;

      mesh.position.set(posX, posY, posZ);
      mesh.rotation.z = Math.random() * Math.PI * 2;

      const baseScale = 0.85 + Math.random() * 0.75;
      mesh.scale.set(baseScale, baseScale, 1);

      this.scene.add(mesh);

      this.smokeParticles.push({
        mesh,
        baseScale,
        speedY: 0.45 + Math.random() * 0.65, // Velocidad de ascenso suave
        speedX: (Math.random() - 0.5) * 0.25,
        rotSpeed: (Math.random() - 0.5) * 0.0035, // Giro continuo lento
        maxOpacity: 0.18 + Math.random() * 0.16,
        minY,
        maxY,
        phase: Math.random() * Math.PI * 2,
        swaySpeed: 0.008 + Math.random() * 0.012
      });
    }
  }

  /**
   * Crea micro-destellos o chispas luminosas que flotan suavemente con el humo
   */
  private createAscendingEmbers(): void {
    const geometry = new THREE.BufferGeometry();
    const positions = new Float32Array(this.emberCount * 3);
    const colors = new Float32Array(this.emberCount * 3);
    this.emberSpeeds = new Float32Array(this.emberCount);

    const pinkColor = new THREE.Color(0xff6eaa);
    const purpleColor = new THREE.Color(0xc084fc);
    const cyanColor = new THREE.Color(0x38bdf8);

    for (let i = 0; i < this.emberCount; i++) {
      const i3 = i * 3;
      positions[i3] = (Math.random() - 0.5) * 800;
      positions[i3 + 1] = -260 + Math.random() * 500;
      positions[i3 + 2] = -200 + Math.random() * 300;

      const colorChoice = Math.random();
      const col = colorChoice < 0.55 ? pinkColor : colorChoice < 0.85 ? purpleColor : cyanColor;
      colors[i3] = col.r;
      colors[i3 + 1] = col.g;
      colors[i3 + 2] = col.b;

      this.emberSpeeds[i] = 0.4 + Math.random() * 0.8;
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(positions, 3));
    geometry.setAttribute('color', new THREE.BufferAttribute(colors, 3));
    this.geometries.push(geometry);

    const emberCanvas = document.createElement('canvas');
    emberCanvas.width = 64;
    emberCanvas.height = 64;
    const eCtx = emberCanvas.getContext('2d')!;
    const eGrad = eCtx.createRadialGradient(32, 32, 0, 32, 32, 30);
    eGrad.addColorStop(0, 'rgba(255, 255, 255, 1)');
    eGrad.addColorStop(0.3, 'rgba(255, 200, 240, 0.8)');
    eGrad.addColorStop(0.7, 'rgba(255, 110, 170, 0.25)');
    eGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');
    eCtx.fillStyle = eGrad;
    eCtx.beginPath();
    eCtx.arc(32, 32, 30, 0, Math.PI * 2);
    eCtx.fill();

    const emberTexture = new THREE.CanvasTexture(emberCanvas);
    this.textures.push(emberTexture);

    const material = new THREE.PointsMaterial({
      size: 14,
      map: emberTexture,
      vertexColors: true,
      transparent: true,
      opacity: 0.85,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    });
    this.materials.push(material);

    this.emberPoints = new THREE.Points(geometry, material);
    this.emberPositions = positions;
    this.scene.add(this.emberPoints);
  }

  private setupPointerEvents(): void {
    if (this.containerRef?.nativeElement) {
      this.containerRef.nativeElement.addEventListener('pointermove', this.boundOnPointerMove, {
        passive: true
      });
    }
  }

  private onPointerMove(e: PointerEvent): void {
    const rect = this.containerRef.nativeElement.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    // Normalizado de -1 a 1
    this.targetMouseX = (x / rect.width) * 2 - 1;
    this.targetMouseY = -(y / rect.height) * 2 + 1;
  }

  private setupResizeObserver(): void {
    if (!this.containerRef?.nativeElement) return;

    this.resizeObserver = new ResizeObserver(entries => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        if (width > 0 && height > 0) {
          this.onResize(width, height);
        }
      }
    });

    this.resizeObserver.observe(this.containerRef.nativeElement);
  }

  private setupIntersectionObserver(): void {
    if (!this.containerRef?.nativeElement) return;

    this.intersectionObserver = new IntersectionObserver(
      entries => {
        for (const entry of entries) {
          this.isVisible = entry.isIntersecting;
        }
      },
      { rootMargin: '100px 0px' }
    );

    this.intersectionObserver.observe(this.containerRef.nativeElement);
  }

  private onResize(width: number, height: number): void {
    if (!this.camera || !this.renderer) return;

    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(width, height);
  }

  private animate(): void {
    this.animationFrameId = requestAnimationFrame(() => this.animate());

    if (!this.isVisible) return;

    // Interpolación suave del cursor
    this.mouseX += (this.targetMouseX - this.mouseX) * 0.04;
    this.mouseY += (this.targetMouseY - this.mouseY) * 0.04;

    const time = performance.now() * 0.001;

    // 1. Dinamismo de luces Neón orbitando suavemente
    if (this.lightPink) {
      this.lightPink.position.x = -180 + Math.sin(time * 0.7) * 90 + this.mouseX * 120;
      this.lightPink.position.y = -120 + Math.cos(time * 0.6) * 40 - this.mouseY * 60;
    }

    if (this.lightPurple) {
      this.lightPurple.position.x = 190 + Math.cos(time * 0.65) * 80 + this.mouseX * 100;
      this.lightPurple.position.y = -80 + Math.sin(time * 0.8) * 50 - this.mouseY * 50;
    }

    if (this.lightCyan) {
      this.lightCyan.position.x = Math.sin(time * 0.5) * 110;
      this.lightCyan.position.y = -190 + Math.sin(time * 0.9) * 30;
    }

    // 2. Parallax de cámara muy sutil
    if (this.camera) {
      this.camera.position.x += (this.mouseX * 35 - this.camera.position.x) * 0.03;
      this.camera.position.y += (-this.mouseY * 20 - this.camera.position.y) * 0.03;
    }

    // 3. Actualizar partículas de humo (ascenso, expansión, rotación y opacidad)
    for (const p of this.smokeParticles) {
      // Ascenso continuo
      p.mesh.position.y += p.speedY;

      // Ondulación horizontal armónica orgánica
      p.phase += p.swaySpeed;
      p.mesh.position.x += Math.sin(p.phase) * 0.35 + p.speedX;

      // Rotación continua de la voluta
      p.mesh.rotation.z += p.rotSpeed;

      // Cálculo del progreso vertical (0 en el fondo, 1 al llegar arriba)
      const range = p.maxY - p.minY;
      const progress = Math.min(Math.max((p.mesh.position.y - p.minY) / range, 0), 1);

      // Expansión a medida que asciende el humo
      const scale = p.baseScale * (1 + progress * 0.9);
      p.mesh.scale.set(scale, scale, 1);

      // Curva de opacidad: entrada suave abajo, permanencia al centro, disipación arriba
      let alpha = 0;
      if (progress < 0.28) {
        alpha = (progress / 0.28) * p.maxOpacity;
      } else if (progress < 0.7) {
        alpha = p.maxOpacity;
      } else {
        alpha = ((1 - progress) / 0.3) * p.maxOpacity;
      }

      const mat = p.mesh.material as THREE.MeshLambertMaterial;
      mat.opacity = Math.max(0, alpha);

      // Reset al pasar el límite superior: reaparece abajo con ligeras variaciones
      if (p.mesh.position.y > p.maxY) {
        p.mesh.position.y = p.minY - Math.random() * 40;
        p.mesh.position.x = (Math.random() - 0.5) * Math.max(window.innerWidth * 0.85, 800);
        p.phase = Math.random() * Math.PI * 2;
        p.speedY = 0.45 + Math.random() * 0.65;
        mat.opacity = 0;
      }
    }

    // 4. Actualizar partículas luminosas (ascuas / destellos)
    if (this.emberPoints && this.emberPositions && this.emberSpeeds) {
      const pos = this.emberPositions;
      for (let i = 0; i < this.emberCount; i++) {
        const i3 = i * 3;
        pos[i3 + 1] += this.emberSpeeds[i]; // Ascenso
        pos[i3] += Math.sin(time + i) * 0.25; // Brisa lateral

        // Reset
        if (pos[i3 + 1] > 300) {
          pos[i3 + 1] = -260;
          pos[i3] = (Math.random() - 0.5) * 800;
        }
      }
      this.emberPoints.geometry.attributes['position'].needsUpdate = true;
    }

    // Render
    this.renderer.render(this.scene, this.camera);
  }

  private disposeScene(): void {
    this.smokeParticles.forEach(p => {
      this.scene.remove(p.mesh);
    });
    this.smokeParticles = [];

    if (this.emberPoints) {
      this.scene.remove(this.emberPoints);
    }

    this.textures.forEach(t => t.dispose());
    this.textures = [];

    this.materials.forEach(m => m.dispose());
    this.materials = [];

    this.geometries.forEach(g => g.dispose());
    this.geometries = [];

    if (this.renderer) {
      this.renderer.dispose();
    }
  }
}
