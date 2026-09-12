// src/app/services/cursor.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface CursorState {
  active: boolean;
  label: string;
  type?: 'default' | 'drag' | 'view' | 'reserve';
}

@Injectable({
  providedIn: 'root'
})
export class CursorService {
  private cursorState$ = new BehaviorSubject<CursorState>({
    active: false,
    label: '',
    type: 'default'
  });

  state$ = this.cursorState$.asObservable();

  setCursor(label: string = '', active: boolean = true, type: 'default' | 'drag' | 'view' | 'reserve' = 'default') {
    this.cursorState$.next({ label, active, type });
  }

  resetCursor() {
    this.cursorState$.next({ active: false, label: '', type: 'default' });
  }
}
