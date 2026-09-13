// src/app/components/fluid-canvas/fluid-canvas.component.ts
import {
  Component,
  ElementRef,
  ViewChild,
  AfterViewInit,
  OnDestroy,
  NgZone,
  inject,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import * as THREE from 'three';

@Component({
  selector: 'app-fluid-canvas',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  template: `
    <div #fluidContainer class="fluid-canvas-container" id="webgl-bg" aria-hidden="true"></div>
  `,
  styleUrls: ['./fluid-canvas.component.scss']
})
export class FluidCanvasComponent implements AfterViewInit, OnDestroy {
  @ViewChild('fluidContainer', { static: true })
  private containerRef!: ElementRef<HTMLDivElement>;

  private ngZone = inject(NgZone);

  private scene!: THREE.Scene;
  private camera!: THREE.OrthographicCamera;
  private renderer!: THREE.WebGLRenderer;
  private clock = new THREE.Clock();

  private fluidMaterial!: THREE.ShaderMaterial;
  private mesh!: THREE.Mesh;

  private mouse = new THREE.Vector2(0.5, 0.5);
  private targetMouse = new THREE.Vector2(0.5, 0.5);
  private mouseVelocity = new THREE.Vector2(0, 0);

  private particles!: THREE.Points;
  private particlePositions!: Float32Array;
  private particleSpeeds!: Float32Array;
  private particleCount = 65;

  private animationFrameId: number | null = null;
  private boundOnMouseMove = this.onMouseMove.bind(this);
  private boundOnResize = this.onResize.bind(this);

  ngAfterViewInit(): void {
    if (typeof window === 'undefined') return;

    this.ngZone.runOutsideAngular(() => {
      this.initScene();
      this.initParticles();
      this.initEvents();
      this.animate();
    });
  }

  ngOnDestroy(): void {
    if (this.animationFrameId !== null) {
      cancelAnimationFrame(this.animationFrameId);
      this.animationFrameId = null;
    }

    if (typeof window !== 'undefined') {
      window.removeEventListener('mousemove', this.boundOnMouseMove);
      window.removeEventListener('resize', this.boundOnResize);
    }

    this.dispose();
  }

  private initScene(): void {
    const container = this.containerRef.nativeElement;
    const width = window.innerWidth;
    const height = window.innerHeight;

    this.scene = new THREE.Scene();
    this.camera = new THREE.OrthographicCamera(-1, 1, 1, -1, 0, 1);

    this.renderer = new THREE.WebGLRenderer({
      powerPreference: 'high-performance',
      antialias: true,
      alpha: true
    });
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.renderer.setSize(width, height);
    this.renderer.setClearColor(0x05030a, 1);
    container.appendChild(this.renderer.domElement);

    const vertexShader = `
      varying vec2 vUv;
      void main() {
        vUv = uv;
        gl_Position = vec4(position, 1.0);
      }
    `;

    const fragmentShader = `
      uniform float uTime;
      uniform vec2 uResolution;
      uniform vec2 uMouse;
      uniform vec2 uVelocity;
      varying vec2 vUv;

      vec4 permute(vec4 x) { return mod(((x*34.0)+1.0)*x, 289.0); }
      vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

      float snoise(vec2 v) {
        const vec4 C = vec4(0.211324865405187, 0.366025403784439,
                           -0.577350269189626, 0.024390243902439);
        vec2 i  = floor(v + dot(v, C.yy));
        vec2 x0 = v -   i + dot(i, C.xx);
        vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
        vec4 x12 = x0.xyxy + C.xxzz;
        x12.xy -= i1;
        i = mod(i, 289.0);
        vec3 p = permute(permute(i.y + vec3(0.0, i1.y, 1.0))
          + i.x + vec3(0.0, i1.x, 1.0));
        vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
        m = m*m;
        m = m*m;
        vec3 x = 2.0 * fract(p * C.www) - 1.0;
        vec3 h = abs(x) - 0.5;
        vec3 ox = floor(x + 0.5);
        vec3 a0 = x - ox;
        m *= taylorInvSqrt(a0*a0 + h*h);
        vec3 g;
        g.x  = a0.x  * x0.x  + h.x  * x0.y;
        g.yz = a0.yz * x12.xz + h.yz * x12.yw;
        return 130.0 * dot(m, g);
      }

      void main() {
        vec2 st = gl_FragCoord.xy / uResolution.xy;
        float aspect = uResolution.x / uResolution.y;
        vec2 uvCorrected = vec2(st.x * aspect, st.y);
        vec2 mouseCorrected = vec2(uMouse.x * aspect, uMouse.y);

        float distToMouse = distance(uvCorrected, mouseCorrected);
        float mouseInfluence = smoothstep(0.45, 0.0, distToMouse);

        float n1 = snoise(uvCorrected * 1.8 + vec2(uTime * 0.12, uTime * 0.08));
        float n2 = snoise(uvCorrected * 3.4 - vec2(uTime * 0.06, uTime * 0.15));
        float fluidDistort = (n1 * 0.6 + n2 * 0.4) + mouseInfluence * 0.4;

        vec3 deepOnyx = vec3(0.02, 0.012, 0.04);
        vec3 bubblePink = vec3(1.0, 0.16, 0.55);   // #ff2a85
        vec3 orchidPurple = vec3(0.66, 0.33, 0.97); // #a855f7
        vec3 electricCyan = vec3(0.0, 0.94, 1.0);  // #00f0ff

        vec3 color = deepOnyx;

        float tendril1 = smoothstep(0.15, 0.75, n1 + fluidDistort * 0.5);
        float tendril2 = smoothstep(0.25, 0.85, n2 - fluidDistort * 0.3);

        color = mix(color, orchidPurple * 0.45, tendril1 * 0.5);
        color = mix(color, bubblePink * 0.65, tendril2 * 0.4);

        float mouseGlow = exp(-distToMouse * 5.2) * (1.0 + length(uVelocity) * 12.0);
        color += mix(bubblePink, electricCyan, sin(uTime * 1.5) * 0.5 + 0.5) * mouseGlow * 0.75;

        float vignette = 1.0 - length(st - 0.5) * 0.85;
        color *= vignette;

        gl_FragColor = vec4(color, 0.92);
      }
    `;

    this.fluidMaterial = new THREE.ShaderMaterial({
      vertexShader,
      fragmentShader,
      uniforms: {
        uTime: { value: 0 },
        uResolution: { value: new THREE.Vector2(width, height) },
        uMouse: { value: this.mouse },
        uVelocity: { value: this.mouseVelocity }
      },
      depthWrite: false,
      depthTest: false
    });

    const geometry = new THREE.PlaneGeometry(2, 2);
    this.mesh = new THREE.Mesh(geometry, this.fluidMaterial);
    this.scene.add(this.mesh);
  }

  private initParticles(): void {
    const geometry = new THREE.BufferGeometry();
    this.particlePositions = new Float32Array(this.particleCount * 3);
    this.particleSpeeds = new Float32Array(this.particleCount);

    for (let i = 0; i < this.particleCount; i++) {
      const i3 = i * 3;
      this.particlePositions[i3] = (Math.random() - 0.5) * 2;
      this.particlePositions[i3 + 1] = (Math.random() - 0.5) * 2;
      this.particlePositions[i3 + 2] = 0;
      this.particleSpeeds[i] = 0.001 + Math.random() * 0.002;
    }

    geometry.setAttribute('position', new THREE.BufferAttribute(this.particlePositions, 3));

    const canvas = document.createElement('canvas');
    canvas.width = 32;
    canvas.height = 32;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      const grad = ctx.createRadialGradient(16, 16, 0, 16, 16, 16);
      grad.addColorStop(0, 'rgba(255, 42, 133, 0.9)');
      grad.addColorStop(0.5, 'rgba(168, 85, 247, 0.4)');
      grad.addColorStop(1, 'rgba(0, 0, 0, 0)');
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.arc(16, 16, 16, 0, Math.PI * 2);
      ctx.fill();
    }

    const texture = new THREE.CanvasTexture(canvas);
    const material = new THREE.PointsMaterial({
      size: 0.055,
      map: texture,
      transparent: true,
      blending: THREE.AdditiveBlending,
      depthWrite: false
    });

    this.particles = new THREE.Points(geometry, material);
    this.scene.add(this.particles);
  }

  private initEvents(): void {
    window.addEventListener('mousemove', this.boundOnMouseMove, { passive: true });
    window.addEventListener('resize', this.boundOnResize);
  }

  private onMouseMove(e: MouseEvent): void {
    this.targetMouse.x = e.clientX / window.innerWidth;
    this.targetMouse.y = 1.0 - (e.clientY / window.innerHeight);
  }

  private onResize(): void {
    if (!this.renderer || !this.fluidMaterial) return;
    const width = window.innerWidth;
    const height = window.innerHeight;
    this.renderer.setSize(width, height);
    this.renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    this.fluidMaterial.uniforms['uResolution'].value.set(width, height);
  }

  private animate = (): void => {
    this.animationFrameId = requestAnimationFrame(this.animate);

    const elapsedTime = this.clock.getElapsedTime();

    this.mouseVelocity.x = (this.targetMouse.x - this.mouse.x);
    this.mouseVelocity.y = (this.targetMouse.y - this.mouse.y);

    this.mouse.x += this.mouseVelocity.x * 0.08;
    this.mouse.y += this.mouseVelocity.y * 0.08;

    this.fluidMaterial.uniforms['uTime'].value = elapsedTime;
    this.fluidMaterial.uniforms['uMouse'].value.copy(this.mouse);
    this.fluidMaterial.uniforms['uVelocity'].value.copy(this.mouseVelocity);

    if (this.particles && this.particlePositions) {
      for (let i = 0; i < this.particleCount; i++) {
        const i3 = i * 3;
        this.particlePositions[i3 + 1] += this.particleSpeeds[i];
        this.particlePositions[i3] += Math.sin(elapsedTime * 0.5 + i) * 0.0004;

        if (this.particlePositions[i3 + 1] > 1.0) {
          this.particlePositions[i3 + 1] = -1.0;
          this.particlePositions[i3] = (Math.random() - 0.5) * 2;
        }
      }
      this.particles.geometry.attributes['position'].needsUpdate = true;
    }

    this.renderer.render(this.scene, this.camera);
  };

  private dispose(): void {
    if (this.mesh) {
      this.mesh.geometry.dispose();
      (this.mesh.material as THREE.Material).dispose();
      this.scene.remove(this.mesh);
    }
    if (this.particles) {
      this.particles.geometry.dispose();
      (this.particles.material as THREE.Material).dispose();
      this.scene.remove(this.particles);
    }
    if (this.renderer) {
      this.renderer.dispose();
      if (this.renderer.domElement && this.renderer.domElement.parentNode) {
        this.renderer.domElement.parentNode.removeChild(this.renderer.domElement);
      }
    }
  }
}
