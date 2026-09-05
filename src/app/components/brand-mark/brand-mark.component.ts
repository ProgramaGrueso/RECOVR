import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-brand-mark',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './brand-mark.component.html',
  styleUrls: ['./brand-mark.component.scss']
})
export class BrandMarkComponent {
  @Input() size: 'sm' | 'md' | 'lg' | number = 'sm';
  @Input() color: string = 'currentColor';

  get pixelSize(): string {
    if (typeof this.size === 'number') {
      return `${this.size}px`;
    }
    switch (this.size) {
      case 'sm': return '20px';
      case 'md': return '32px';
      case 'lg': return '48px';
      default: return '20px';
    }
  }
}
