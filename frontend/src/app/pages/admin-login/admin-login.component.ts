import { ChangeDetectorRef, Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { BrandMarkComponent } from '../../components/brand-mark/brand-mark.component';

/**
 * Acceso único para todos los roles. Se valida contra el backend; no hay usuarios, claves ni atajos de demostración
 * en el navegador. Según el rol que devuelva el servidor, el usuario cae en su propia zona.
 */
@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, BrandMarkComponent],
  templateUrl: './admin-login.component.html',
  styleUrls: ['./admin-login.component.scss']
})
export class AdminLoginComponent implements OnInit {
  tab: 'LOGIN' | 'REGISTER' = 'LOGIN';

  correo = '';
  password = '';

  regName = '';
  regEmail = '';
  regPhone = '';
  regPassword = '';
  regConfirmPassword = '';

  showPassword = false;
  error = '';
  infoMessage = '';
  cargando = false;

  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  ngOnInit(): void {
    if (this.route.snapshot.queryParams['mode'] === 'register') {
      this.tab = 'REGISTER';
    }
    // Si ya hay una sesión válida, no se pide iniciar de nuevo
    this.auth.asegurarSesion().subscribe(ok => { if (ok) { this.entrar(); } });
  }

  setTab(tab: 'LOGIN' | 'REGISTER'): void {
    this.tab = tab;
    this.error = '';
    this.infoMessage = '';
  }

  submitLogin(): void {
    this.error = '';
    this.infoMessage = '';
    if (!this.correo.trim() || !this.password) {
      this.error = 'Ingresa tu correo y contraseña.';
      return;
    }
    this.cargando = true;
    this.auth.login(this.correo.trim(), this.password).subscribe(res => {
      this.cargando = false;
      this.password = '';
      if (res.ok) {
        this.entrar();
      } else {
        this.error = res.message ?? 'No se pudo iniciar sesión.';
      }
      this.cdr.detectChanges();
    });
  }

  submitRegister(): void {
    this.error = '';
    this.infoMessage = '';
    if (this.regName.trim().length < 3) {
      this.error = 'Ingresa tu nombre y apellido completos.';
      return;
    }
    if (!this.regEmail.includes('@')) {
      this.error = 'Ingresa un correo electrónico válido.';
      return;
    }
    if (this.regPhone.trim().length < 8) {
      this.error = 'Ingresa un teléfono de contacto válido.';
      return;
    }
    if (this.regPassword.length < 8 || !/[A-Za-z]/.test(this.regPassword) || !/\d/.test(this.regPassword)) {
      this.error = 'La contraseña debe tener al menos 8 caracteres, con letras y números.';
      return;
    }
    if (this.regPassword !== this.regConfirmPassword) {
      this.error = 'Las contraseñas no coinciden.';
      return;
    }
    this.cargando = true;
    this.auth.registrar(this.regName.trim(), this.regEmail.trim(), this.regPassword, this.regPhone.trim()).subscribe(res => {
      this.cargando = false;
      this.regPassword = '';
      this.regConfirmPassword = '';
      if (res.ok) {
        this.entrar();
      } else {
        this.error = res.message ?? 'No se pudo crear la cuenta.';
      }
      this.cdr.detectChanges();
    });
  }

  forgotPassword(): void {
    this.error = '';
    this.infoMessage = 'Para restablecer tu clave, acércate a recepción o contacta al administrador.';
  }

  /** Lleva a cada rol a su zona; un cliente que venía a reservar continúa con su reserva. */
  private entrar(): void {
    const destino = this.route.snapshot.queryParams['returnUrl'];
    const esCliente = this.auth.isClient();
    void this.router.navigateByUrl(esCliente && typeof destino === 'string' && destino.startsWith('/') ? destino : '/admin');
  }
}
