import { Routes } from '@angular/router';
import { HomeComponent } from './pages/home/home.component';
import { CatalogComponent } from './pages/catalog/catalog.component';
import { BookingPageComponent } from './pages/booking/booking-page.component';
import { AdminComponent } from './pages/admin/admin.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'catalog', component: CatalogComponent },
  { path: 'catalogo', component: CatalogComponent },
  { path: 'reserva', component: BookingPageComponent },
  { path: 'admin', component: AdminComponent },
  { path: '**', redirectTo: '' }
];



