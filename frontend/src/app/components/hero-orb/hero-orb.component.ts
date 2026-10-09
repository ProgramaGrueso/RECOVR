// Orbe líquido iridiscente del hero: WebGL propio, reactivo al mouse y al scroll.
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
import * as THREE from 'three';

const NOISE = /* glsl */ `
vec3 mod289(vec3 x){return x-floor(x*(1.0/289.0))*289.0;}
vec4 mod289(vec4 x){return x-floor(x*(1.0/289.0))*289.0;}
vec4 permute(vec4 x){return mod289(((x*34.0)+1.0)*x);}
vec4 taylorInvSqrt(vec4 r){return 1.79284291400159-0.85373472095314*r;}
float snoise(vec3 v){
  const vec2 C=vec2(1.0/6.0,1.0/3.0); const vec4 D=vec4(0.0,0.5,1.0,2.0);
  vec3 i=floor(v+dot(v,C.yyy)); vec3 x0=v-i+dot(i,C.xxx);
  vec3 g=step(x0.yzx,x0.xyz); vec3 l=1.0-g; vec3 i1=min(g.xyz,l.zxy); vec3 i2=max(g.xyz,l.zxy);
  vec3 x1=x0-i1+C.xxx; vec3 x2=x0-i2+C.yyy; vec3 x3=x0-D.yyy;
  i=mod289(i);
  vec4 p=permute(permute(permute(i.z+vec4(0.0,i1.z,i2.z,1.0))+i.y+vec4(0.0,i1.y,i2.y,1.0))+i.x+vec4(0.0,i1.x,i2.x,1.0));
  float n_=0.142857142857; vec3 ns=n_*D.wyz-D.xzx;
  vec4 j=p-49.0*floor(p*ns.z*ns.z); vec4 x_=floor(j*ns.z); vec4 y_=floor(j-7.0*x_);
  vec4 x=x_*ns.x+ns.yyyy; vec4 y=y_*ns.x+ns.yyyy; vec4 h=1.0-abs(x)-abs(y);
  vec4 b0=vec4(x.xy,y.xy); vec4 b1=vec4(x.zw,y.zw);
  vec4 s0=floor(b0)*2.0+1.0; vec4 s1=floor(b1)*2.0+1.0; vec4 sh=-step(h,vec4(0.0));
  vec4 a0=b0.xzyw+s0.xzyw*sh.xxyy; vec4 a1=b1.xzyw+s1.xzyw*sh.zzww;
  vec3 p0=vec3(a0.xy,h.x); vec3 p1=vec3(a0.zw,h.y); vec3 p2=vec3(a1.xy,h.z); vec3 p3=vec3(a1.zw,h.w);
  vec4 norm=taylorInvSqrt(vec4(dot(p0,p0),dot(p1,p1),dot(p2,p2),dot(p3,p3)));
  p0*=norm.x; p1*=norm.y; p2*=norm.z; p3*=norm.w;
  vec4 m=max(0.6-vec4(dot(x0,x0),dot(x1,x1),dot(x2,x2),dot(x3,x3)),0.0); m=m*m;
  return 42.0*dot(m*m,vec4(dot(p0,x0),dot(p1,x1),dot(p2,x2),dot(p3,x3)));
}`;

const ORB_VERT = /* glsl */ `
uniform float uTime; uniform float uAmp; uniform vec2 uMouse;
varying vec3 vNormal; varying vec3 vView; varying float vDisp;
${NOISE}
float disp(vec3 p){
  float t = uTime * 0.35;
  float n = snoise(p * 1.25 + vec3(t, t * 0.8, -t)) * 0.55 + snoise(p * 2.7 - vec3(t * 1.4)) * 0.18;
  float m = smoothstep(1.6, 0.0, distance(p.xy, uMouse * 1.2)) * 0.18;
  return (n + m) * uAmp;
}
void main(){
  vec3 p = position;
  float d = disp(p);
  vec3 displaced = p + normal * d;
  // normal recomputada por diferencias finitas sobre la superficie
  vec3 tA = normalize(cross(normal, vec3(0.0, 1.0, 0.0)) + 1e-4);
  vec3 tB = normalize(cross(normal, tA));
  float e = 0.02;
  vec3 pA = p + tA * e; pA += normalize(pA) * 0.0; vec3 pB = p + tB * e;
  vec3 dA = pA + normal * disp(pA); vec3 dB = pB + normal * disp(pB);
  vec3 n = normalize(cross(dA - displaced, dB - displaced));
  if (dot(n, normal) < 0.0) n = -n;
  vNormal = normalize(normalMatrix * n);
  vec4 mv = modelViewMatrix * vec4(displaced, 1.0);
  vView = normalize(-mv.xyz);
  vDisp = d;
  gl_Position = projectionMatrix * mv;
}`;

const ORB_FRAG = /* glsl */ `
uniform float uTime;
varying vec3 vNormal; varying vec3 vView; varying float vDisp;
vec3 pal(float t){ // rosa chicle -> lila -> celeste -> rosa (burbuja de jabón pastel)
  float x = fract(t) * 3.0;
  vec3 pink = vec3(1.0, 0.45, 0.72), vio = vec3(0.80, 0.62, 1.0), cy = vec3(0.50, 0.90, 1.0);
  vec3 c = mix(pink, vio, smoothstep(0.0, 1.0, x));
  c = mix(c, cy, smoothstep(1.0, 2.0, x));
  return mix(c, pink, smoothstep(2.0, 3.0, x));
}
void main(){
  vec3 N = normalize(vNormal); vec3 V = normalize(vView);
  float fres = pow(1.0 - max(dot(N, V), 0.0), 2.4);
  float film = dot(N, V) * 0.55 + vDisp * 0.9 + uTime * 0.03;
  vec3 irid = pal(film);
  vec3 L = normalize(vec3(-0.4, 0.8, 0.9));
  float spec = pow(max(dot(reflect(-L, N), V), 0.0), 70.0);
  float spec2 = pow(max(dot(reflect(-normalize(vec3(0.9, -0.3, 0.5)), N), V), 0.0), 45.0);
  vec3 body = mix(vec3(1.0, 0.86, 0.95), irid, 0.25 + fres * 0.75);
  vec3 col = body + spec * vec3(1.0) + spec2 * vec3(0.8, 0.95, 1.0) * 0.6;
  float alpha = clamp(0.30 + fres * 0.75 + spec * 0.8, 0.0, 0.95);
  gl_FragColor = vec4(col * alpha, alpha);
}`;

@Component({
  selector: 'app-hero-orb',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<canvas #cv class="orb-canvas" aria-hidden="true"></canvas>`,
  styles: [`
    :host { position: absolute; inset: 0; z-index: 0; pointer-events: none; }
    .orb-canvas { width: 100%; height: 100%; display: block; }
  `]
})
export class HeroOrbComponent implements AfterViewInit, OnDestroy {
  @ViewChild('cv', { static: true }) private cv!: ElementRef<HTMLCanvasElement>;
  private zone = inject(NgZone);
  private host = inject(ElementRef<HTMLElement>);

  private renderer?: THREE.WebGLRenderer;
  private raf = 0;
  private visible = true;
  private io?: IntersectionObserver;
  private ro?: ResizeObserver;
  private disposables: { dispose(): void }[] = [];
  private mouse = new THREE.Vector2();
  private target = new THREE.Vector2();
  private onMove = (e: PointerEvent) => {
    this.target.set((e.clientX / innerWidth) * 2 - 1, -((e.clientY / innerHeight) * 2 - 1));
  };

  ngAfterViewInit(): void {
    this.zone.runOutsideAngular(() => this.init());
  }

  private init(): void {
    const canvas = this.cv.nativeElement;
    const el = this.host.nativeElement as HTMLElement;
    const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
    const mobile = innerWidth < 768;

    try {
      this.renderer = new THREE.WebGLRenderer({ canvas, antialias: !mobile, alpha: true, premultipliedAlpha: true, powerPreference: 'high-performance' });
    } catch {
      return; // sin WebGL: el hero sigue funcionando sin el orbe
    }
    const renderer = this.renderer;
    renderer.setPixelRatio(Math.min(devicePixelRatio, mobile ? 1.5 : 2));
    renderer.setClearColor(0x000000, 0);

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(38, 1, 0.1, 50);
    camera.position.z = 5.2;

    // Orbe
    const orbMat = new THREE.ShaderMaterial({
      vertexShader: ORB_VERT,
      fragmentShader: ORB_FRAG,
      transparent: true,
      premultipliedAlpha: true,
      depthWrite: false,
      uniforms: { uTime: { value: 0 }, uAmp: { value: 0.32 }, uMouse: { value: new THREE.Vector2() } }
    });
    const orbGeo = new THREE.IcosahedronGeometry(1.05, mobile ? 40 : 72);
    const orb = new THREE.Mesh(orbGeo, orbMat);
    scene.add(orb);
    this.disposables.push(orbMat, orbGeo);
    orb.scale.setScalar(0.9);

    // Burbujas satélite (instanciadas, mismo lenguaje visual)
    const count = mobile ? 36 : 90;
    const bubGeo = new THREE.SphereGeometry(1, 20, 20);
    const bubMat = new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0.55, depthWrite: false });
    const bubbles = new THREE.InstancedMesh(bubGeo, bubMat, count);
    const seeds = Array.from({ length: count }, () => ({
      a: Math.random() * Math.PI * 2,
      r: 2.0 + Math.random() * 3.2,
      y: (Math.random() - 0.5) * 5,
      s: 0.02 + Math.pow(Math.random(), 3) * 0.16,
      v: 0.05 + Math.random() * 0.18,
      z: (Math.random() - 0.5) * 3
    }));
    scene.add(bubbles);
    this.disposables.push(bubGeo, bubMat);
    const dummy = new THREE.Object3D();
    const cols = [new THREE.Color(0xff8cc4), new THREE.Color(0xd5b0ff), new THREE.Color(0x9be0ff)];
    seeds.forEach((_, i) => bubbles.setColorAt(i, cols[i % 3]));

    const resize = () => {
      const w = el.clientWidth || 1, h = el.clientHeight || 1;
      renderer.setSize(w, h, false);
      camera.aspect = w / h;
      // en pantallas angostas el orbe pasa al fondo, centrado y más chico
      camera.position.z = w / h < 0.9 ? 7.2 : 5.2;
      camera.updateProjectionMatrix();
    };
    this.ro = new ResizeObserver(resize);
    this.ro.observe(el);
    resize();

    addEventListener('pointermove', this.onMove, { passive: true });
    this.io = new IntersectionObserver(([e]) => (this.visible = e.isIntersecting));
    this.io.observe(el);

    const clock = new THREE.Clock();
    const frame = () => {
      this.raf = requestAnimationFrame(frame);
      if (!this.visible && !reduced) return;
      const t = clock.getElapsedTime();
      this.mouse.lerp(this.target, 0.05);
      const sp = Math.min(scrollY / Math.max(el.clientHeight, 1), 1.4); // 0 en tope -> 1 al salir del hero

      orbMat.uniforms['uTime'].value = t;
      orbMat.uniforms['uMouse'].value.set(this.mouse.x * 1.3, this.mouse.y * 1.3);
      orbMat.uniforms['uAmp'].value = 0.32 + sp * 0.4 + Math.hypot(this.mouse.x - this.target.x, this.mouse.y - this.target.y) * 1.2;

      orb.rotation.y = t * 0.12 + this.mouse.x * 0.6 + sp * 2.2;
      orb.rotation.x = this.mouse.y * -0.4 + sp * 0.6;
      orb.position.set(this.mouse.x * 0.25, this.mouse.y * 0.18 - sp * 1.1, -sp * 1.6);
      const s = 0.9 - sp * 0.25;
      orb.scale.setScalar(s);

      for (let i = 0; i < count; i++) {
        const d = seeds[i];
        const a = d.a + t * d.v;
        const rr = d.r + sp * 2.5;
        dummy.position.set(Math.cos(a) * rr, d.y + Math.sin(t * d.v * 2 + i) * 0.3 - sp * 1.5, Math.sin(a) * rr * 0.5 + d.z);
        dummy.scale.setScalar(d.s);
        dummy.updateMatrix();
        bubbles.setMatrixAt(i, dummy.matrix);
      }
      bubbles.instanceMatrix.needsUpdate = true;
      bubbles.rotation.y = this.mouse.x * 0.15;

      renderer.render(scene, camera);
      if (reduced) cancelAnimationFrame(this.raf);
    };
    frame();
  }

  ngOnDestroy(): void {
    cancelAnimationFrame(this.raf);
    removeEventListener('pointermove', this.onMove);
    this.io?.disconnect();
    this.ro?.disconnect();
    this.disposables.forEach(d => d.dispose());
    this.renderer?.dispose();
  }
}
