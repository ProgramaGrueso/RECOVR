import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { BookingFlowComponent } from '../../components/booking-flow/booking-flow.component';
import { SectionDividerComponent } from '../../components/section-divider/section-divider.component';

@Component({
  selector: 'app-booking-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    BookingFlowComponent,
    SectionDividerComponent
  ],
  templateUrl: './booking-page.component.html',
  styleUrls: ['./booking-page.component.scss']
})
export class BookingPageComponent implements OnInit, OnDestroy {

  selectedServiceIdParam: string | null = null;
  private routeSub?: Subscription;

  constructor(private route: ActivatedRoute) {}

  ngOnInit(): void {
    this.routeSub = this.route.queryParamMap.subscribe(params => {
      // Soporta tanto serviceId como servicio
      this.selectedServiceIdParam = params.get('serviceId') || params.get('servicio');
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }
}
