import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService, AdminRole } from '../../services/auth.service';
import { BrandMarkComponent } from '../../components/brand-mark/brand-mark.component';

@Component({
  selector: 'app-admin-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, BrandMarkComponent],
  templateUrl: './admin-login.component.html',
  styleUrls: ['./admin-login.component.scss']
})
export class AdminLoginComponent implements OnInit {
  loginMode: 'CLIENT' | 'STAFF' = 'CLIENT';
  clientTab: 'LOGIN' | 'REGISTER' = 'LOGIN';

  // Campos Login Cliente
  clientEmail = 'carlos.mendoza@gmail.com';
  clientPassword = 'recovr2026';

  // Campos Registro Cliente
  regName = '';
  regEmail = '';
  regPhone = '';
  regPassword = '';
  regConfirmPassword = '';

  // Campos Login Staff
  selectedRole: AdminRole = 'SUPER ADMIN';
  staffUserId = 'devops';
  staffPassword = 'recovr2026';

  rememberMe = true;
  showPassword = false;
  error = '';
  infoMessage = '';
  successMessage = '';

  auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    const modeParam = this.route.snapshot.queryParams['mode'];
    const roleParam = this.route.snapshot.queryParams['role'] as AdminRole | undefined;

    if (modeParam === 'staff') {
      this.loginMode = 'STAFF';
    } else if (modeParam === 'register') {
      this.loginMode = 'CLIENT';
      this.clientTab = 'REGISTER';
    } else if (roleParam && roleParam !== 'CLIENTE') {
      this.loginMode = 'STAFF';
      this.selectedRole = roleParam;
      this.staffUserId = this.selectedStaffUser.id;
      this.staffPassword = this.selectedStaffUser.password;
    } else {
      this.loginMode = 'CLIENT';
      this.clientTab = 'LOGIN';
    }
  }

  setLoginMode(mode: 'CLIENT' | 'STAFF'): void {
    this.loginMode = mode;
    this.error = '';
    this.infoMessage = '';
    this.successMessage = '';
  }

  setClientTab(tab: 'LOGIN' | 'REGISTER'): void {
    this.clientTab = tab;
    this.error = '';
    this.infoMessage = '';
    this.successMessage = '';
  }

  get staffDemoUsers() {
    return this.auth.demoUsersList.filter(u => u.role !== 'CLIENTE');
  }

  get selectedStaffUser() {
    return this.staffDemoUsers.find(user => user.role === this.selectedRole) ?? this.staffDemoUsers[0];
  }

  onStaffRoleChange(): void {
    this.staffUserId = this.selectedStaffUser.id;
    this.staffPassword = this.selectedStaffUser.password;
    this.error = '';
    this.infoMessage = '';
  }

  prefillStaffCredentials(role: AdminRole): void {
    this.selectedRole = role;
    this.staffUserId = this.selectedStaffUser.id;
    this.staffPassword = this.selectedStaffUser.password;
    this.error = '';
    this.infoMessage = '';
  }

  submitClientLogin(): void {
    this.error = '';
    this.infoMessage = '';
    this.successMessage = '';

    if (!this.clientEmail || !this.clientPassword) {
      this.error = 'Por favor ingresa tu correo y contraseña.';
      return;
    }

    const success = this.auth.login(this.clientEmail, this.clientPassword);
    const currentUser = this.auth.currentUser();
    if (success && currentUser?.role === 'CLIENTE') {
      const returnUrl = this.route.snapshot.queryParams['returnUrl'];
      this.router.navigate([returnUrl || '/admin']);
    } else if (success && currentUser?.role !== 'CLIENTE') {
      this.error = 'Esta cuenta pertenece al personal del spa. Usa la pestaña "Acceso Personal & Staff".';
    } else {
      this.error = 'Correo o contraseña incorrectos. Si no tienes cuenta, regístrate en "Crear Cuenta".';
    }
  }

  quickLoginClientDemo(): void {
    this.auth.loginAsRole('CLIENTE');
    const returnUrl = this.route.snapshot.queryParams['returnUrl'];
    this.router.navigate([returnUrl || '/admin']);
  }

  submitClientRegister(): void {
    this.error = '';
    this.infoMessage = '';
    this.successMessage = '';

    if (!this.regName || this.regName.trim().length < 3) {
      this.error = 'Ingresa tu nombre y apellido completo (mínimo 3 caracteres).';
      return;
    }
    if (!this.regEmail || !this.regEmail.includes('@')) {
      this.error = 'Ingresa un correo electrónico válido.';
      return;
    }
    if (!this.regPhone || this.regPhone.trim().length < 8) {
      this.error = 'Ingresa un número telefónico de contacto válido.';
      return;
    }
    if (!this.regPassword || this.regPassword.length < 6) {
      this.error = 'La contraseña debe tener al menos 6 caracteres.';
      return;
    }
    if (this.regPassword !== this.regConfirmPassword) {
      this.error = 'Las contraseñas no coinciden. Verifica e intenta nuevamente.';
      return;
    }

    const res = this.auth.registerClient({
      name: this.regName,
      email: this.regEmail,
      phone: this.regPhone,
      password: this.regPassword
    });

    if (res.success) {
      this.successMessage = res.message;
      setTimeout(() => {
        const returnUrl = this.route.snapshot.queryParams['returnUrl'];
        this.router.navigate([returnUrl || '/admin']);
      }, 700);
    } else {
      this.error = res.message;
    }
  }

  submitStaffLogin(): void {
    this.error = '';
    this.infoMessage = '';
    this.successMessage = '';

    if (!this.staffUserId || !this.staffPassword) {
      this.error = 'Ingresa tus credenciales de empleado del spa.';
      return;
    }

    // Auto-detección por si escribe correo o id directo
    const matched = this.staffDemoUsers.find(
      u => u.id === this.staffUserId || u.email.toLowerCase() === this.staffUserId.toLowerCase()
    );
    if (matched) {
      this.selectedRole = matched.role;
    }

    if (this.auth.loginWithRole(this.selectedRole, this.staffUserId, this.staffPassword, this.rememberMe)) {
      this.router.navigate(['/admin']);
    } else {
      this.error = 'Credenciales de personal no autorizadas o incorrectas.';
    }
  }

  forgotPassword(): void {
    this.error = '';
    this.infoMessage = 'Para restablecer tu clave, acércate a recepción o contacta al administrador.';
  }
}
