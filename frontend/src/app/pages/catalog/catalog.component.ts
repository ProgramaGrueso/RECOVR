import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ServiceCardComponent } from '../../components/service-card/service-card.component';
import { SectionDividerComponent } from '../../components/section-divider/section-divider.component';
import { CatalogService } from '../../services/catalog.service';
import { BookingService } from '../../services/booking.service';
import { ServiceItem, ServiceCategory } from '../../models/service.model';

@Component({
  selector: 'app-catalog',
  standalone: true,
  imports: [CommonModule, ServiceCardComponent, SectionDividerComponent],
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.scss']
})
export class CatalogComponent implements OnInit {
  services: ServiceItem[] = [];
  filteredServices: ServiceItem[] = [];
  selectedCategory: ServiceCategory | 'ALL' = 'ALL';
  isLoading = true;

  constructor(
    private catalogService: CatalogService,
    private bookingService: BookingService
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    this.catalogService.getFeaturedServices().subscribe({
      next: (list) => {
        this.services = list;
        this.filteredServices = list;
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }


  setCategory(cat: ServiceCategory | 'ALL'): void {
    this.selectedCategory = cat;
    if (cat === 'ALL') {
      this.filteredServices = this.services;
    } else {
      this.filteredServices = this.services.filter(s => s.category === cat);
    }
  }

  onReserve(service: ServiceItem): void {
    this.bookingService.openBookingModal(service);
  }
}
