import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BrandMarkComponent } from '../brand-mark/brand-mark.component';

@Component({
  selector: 'app-section-divider',
  standalone: true,
  imports: [CommonModule, BrandMarkComponent],
  templateUrl: './section-divider.component.html',
  styleUrls: ['./section-divider.component.scss']
})
export class SectionDividerComponent {}
