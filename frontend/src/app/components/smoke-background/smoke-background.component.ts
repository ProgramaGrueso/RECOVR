import {
  Component,
  ElementRef,
  ViewChild,
  AfterViewInit,
  OnDestroy,
  NgZone,
  inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as THREE from 'three';

interface ParticleData {
  mesh: THREE.Mesh;
  speedY: number;
  rotSpeed: number;
  swaySpeed: number;
  swayAmp: number;
  phase: number;
}

@Component({
  selector: 'app-smoke-background',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="smoke-bg-wrapper">
      <canvas #smokeCanvas class="smoke-canvas"></canvas>
    </div>
  `,
  styleUrls: ['./smoke-background.component.scss']
})
export class SmokeBackgroundComponent implements AfterViewInit, OnDestroy {
  @ViewChild('smokeCanvas', { static: true })
  private canvasRef!: ElementRef<HTMLCanvasElement>;

  private ngZone = inject(NgZone);

  private scene!: THREE.Scene;
  private camera!: THREE.PerspectiveCamera;
  private renderer!: THREE.WebGLRenderer;
  private animationFrameId: number | null = null;

  private particles: ParticleData[] = [];
  private smokeGeometry?: THREE.PlaneGeometry;
  private smokeMaterial?: THREE.MeshLambertMaterial;
  private smokeTexture?: THREE.Texture;

  private dirLight!: THREE.DirectionalLight;
  private ambientLight!: THREE.AmbientLight;

  private mouseX = 0;
  private mouseY = 0;
  private targetMouseX = 0;
  private targetMouseY = 0;

  private boundOnWindowResize = this.onWindowResize.bind(this);
  private boundOnPointerMove = this.onPointerMove.bind(this);

  ngAfterViewInit(): void {
    if (typeof window === 'undefined') return;

    this.initThree();
    window.addEventListener('resize', this.boundOnWindowResize);
    window.addEventListener('pointermove', this.boundOnPointerMove, { passive: true });

    // Ejecutar render loop fuera de Angular Zone para máximo rendimiento a 60fps
    this.ngZone.runOutsideAngular(() => {
      this.animate();
    });
  }

  ngOnDestroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.boundOnWindowResize);
      window.removeEventListener('pointermove', this.boundOnPointerMove);
    }

    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }

    this.disposeThree();
  }

  private onPointerMove(e: PointerEvent): void {
    const halfW = window.innerWidth / 2;
    const halfH = window.innerHeight / 2;
    this.targetMouseX = (e.clientX - halfW) / halfW;
    this.targetMouseY = (e.clientY - halfH) / halfH;
  }

  private initThree(): void {
    const width = window.innerWidth;
    const height = window.innerHeight;

    // 1. Escena
    this.scene = new THREE.Scene();

    // 2. Cámara con perspectiva (Técnica Midudev para La Velada del Año)
    this.camera = new THREE.PerspectiveCamera(75, width / height, 1, 1500);
    this.camera.position.z = 1000;

    // 3. Renderer con fondo transparente
    this.renderer = new THREE.WebGLRenderer({
      canvas: this.canvasRef.nativeElement,
      alpha: true,
      antialias: true,
      powerPreference: 'high-performance'
    });
    this.renderer.setSize(width, height);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));

    // 4. Luces intensas en tonalidad rosa / magenta
    this.ambientLight = new THREE.AmbientLight(0xff69b4, 1.8);
    this.scene.add(this.ambientLight);

    this.dirLight = new THREE.DirectionalLight(0xff1493, 2.8);
    this.dirLight.position.set(-1, 0.5, 1);
    this.scene.add(this.dirLight);

    // Luz secundaria para realce de volumen 3D
    const backLight = new THREE.DirectionalLight(0xb05cf7, 1.5);
    backLight.position.set(1, -0.5, 0.5);
    this.scene.add(backLight);

    // 5. Crear textura de humo procedural inmediatamente (sin depender de latencia de red)
    const proceduralTexture = this.generateSmokeTexture();
    this.smokeTexture = proceduralTexture;
    this.createParticles(proceduralTexture);

    // Opcional: intentar cargar assets/smoke.png si está disponible y reemplazar
    const loader = new THREE.TextureLoader();
    loader.load(
      'assets/smoke.png',
      loadedTex => {
        if (this.smokeMaterial) {
          this.smokeMaterial.map = loadedTex;
          this.smokeMaterial.needsUpdate = true;
          this.smokeTexture = loadedTex;
        }
      },
      undefined,
      () => {
        // Procedural texture already in use
      }
    );
  }

  /**
   * Genera una textura de humo de alta resolución con canal alfa suave
   */
  private generateSmokeTexture(): THREE.CanvasTexture {
    const canvas = document.createElement('canvas');
    canvas.width = 512;
    canvas.height = 512;
    const ctx = canvas.getContext('2d')!;

    const cx = 256;
    const cy = 256;

    // Gradiente radial central
    const baseGrad = ctx.createRadialGradient(cx, cy, 10, cx, cy, 230);
    baseGrad.addColorStop(0, 'rgba(255, 255, 255, 0.85)');
    baseGrad.addColorStop(0.35, 'rgba(255, 230, 245, 0.45)');
    baseGrad.addColorStop(0.7, 'rgba(255, 200, 235, 0.15)');
    baseGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');

    ctx.fillStyle = baseGrad;
    ctx.beginPath();
    ctx.arc(cx, cy, 230, 0, Math.PI * 2);
    ctx.fill();

    // 16 volutas secundarias aleatorias para dar forma esponjosa de nube
    for (let i = 0; i < 18; i++) {
      const angle = Math.random() * Math.PI * 2;
      const dist = 30 + Math.random() * 110;
      const px = cx + Math.cos(angle) * dist;
      const py = cy + Math.sin(angle) * dist;
      const radius = 60 + Math.random() * 90;

      const puffGrad = ctx.createRadialGradient(px, py, 0, px, py, radius);
      puffGrad.addColorStop(0, 'rgba(255, 255, 255, 0.35)');
      puffGrad.addColorStop(0.5, 'rgba(255, 220, 240, 0.14)');
      puffGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');

      ctx.fillStyle = puffGrad;
      ctx.beginPath();
      ctx.arc(px, py, radius, 0, Math.PI * 2);
      ctx.fill();
    }

    const texture = new THREE.CanvasTexture(canvas);
    texture.wrapS = THREE.ClampToEdgeWrapping;
    texture.wrapT = THREE.ClampToEdgeWrapping;
    texture.needsUpdate = true;
    return texture;
  }

  private createParticles(texture: THREE.Texture): void {
    this.smokeGeometry = new THREE.PlaneGeometry(350, 350);
    this.smokeMaterial = new THREE.MeshLambertMaterial({
      map: texture,
      transparent: true,
      color: 0xff69b4, // Tonalidad rosa / magenta
      opacity: 0.48,   // Sensación de densidad y suavidad
      depthWrite: false
    });

    // 180 partículas distribuidas en X (-250 a 250), Y (-250 a 250) y Z (-100 a 800)
    const particleCount = 180;
    for (let p = 0; p < particleCount; p++) {
      const mesh = new THREE.Mesh(this.smokeGeometry, this.smokeMaterial);
      mesh.position.set(
        Math.random() * 600 - 300,
        Math.random() * 600 - 300,
        Math.random() * 900 - 100
      );
      mesh.rotation.z = Math.random() * Math.PI * 2;

      this.scene.add(mesh);

      this.particles.push({
        mesh,
        // Velocidad de ascenso vertical continua (movimiento de humo real)
        speedY: 0.25 + Math.random() * 0.45,
        // Rotación continua claramente perceptible (positiva o negativa)
        rotSpeed: (Math.random() > 0.5 ? 1 : -1) * (0.0025 + Math.random() * 0.0035),
        // Ondulación lateral orgánica
        swaySpeed: 0.001 + Math.random() * 0.002,
        swayAmp: 0.3 + Math.random() * 0.5,
        phase: Math.random() * Math.PI * 2
      });
    }
  }

  private onWindowResize(): void {
    if (!this.camera || !this.renderer) return;

    const width = window.innerWidth;
    const height = window.innerHeight;

    this.camera.aspect = width / height;
    this.camera.updateProjectionMatrix();
    this.renderer.setSize(width, height);
  }

  private animate(): void {
    this.animationFrameId = requestAnimationFrame(() => this.animate());

    // Suavizado de cursor
    this.mouseX += (this.targetMouseX - this.mouseX) * 0.05;
    this.mouseY += (this.targetMouseY - this.mouseY) * 0.05;

    const time = performance.now() * 0.001;

    // Movimiento fluido del humo en 3D
    const count = this.particles.length;
    for (let i = 0; i < count; i++) {
      const p = this.particles[i];

      // 1. Rotación continua sobre el eje Z (técnica de midudev)
      p.mesh.rotation.z += p.rotSpeed;

      // 2. Ascenso vertical continuo (el humo fluye hacia arriba)
      p.mesh.position.y += p.speedY;

      // 3. Brisa horizontal oscilante
      p.phase += p.swaySpeed;
      p.mesh.position.x += Math.sin(p.phase) * p.swayAmp;

      // Si la partícula sube demasiado alto, reaparece suavemente desde abajo
      if (p.mesh.position.y > 350) {
        p.mesh.position.y = -350;
        p.mesh.position.x = Math.random() * 600 - 300;
      }
    }

    // Parallax muy sutil con el movimiento del ratón
    if (this.camera) {
      this.camera.position.x += (this.mouseX * 40 - this.camera.position.x) * 0.04;
      this.camera.position.y += (-this.mouseY * 30 - this.camera.position.y) * 0.04;
    }

    this.renderer.render(this.scene, this.camera);
  }

  private disposeThree(): void {
    for (const p of this.particles) {
      this.scene.remove(p.mesh);
    }
    this.particles = [];

    this.smokeGeometry?.dispose();
    this.smokeMaterial?.dispose();
    this.smokeTexture?.dispose();
    this.renderer?.dispose();
  }
}
