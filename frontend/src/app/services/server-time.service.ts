import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ServerTimeService {
  // We simulate a server time that can be slightly offset from local time if needed
  // For now, it just tracks a centralized time.
  private currentServerTime = new Date();
  
  public serverTimeSignal = signal<Date>(this.currentServerTime);

  constructor() {
    setInterval(() => {
      this.currentServerTime = new Date(); // In a real app, this might sync with an API periodically
      this.serverTimeSignal.set(this.currentServerTime);
    }, 1000);
  }

  getNow(): Date {
    return this.currentServerTime;
  }

  /**
   * Fecha y hora LOCAL en formato ISO sin zona (yyyy-MM-ddTHH:mm:ss.SSS). Antes devolvía UTC, y de noche en Lima
   * el reloj del panel iba 5 horas adelantado y "hoy" caía en el día siguiente.
   */
  getNowISOString(): string {
    const d = this.currentServerTime;
    const p = (n: number, l = 2) => String(n).padStart(l, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${p(d.getMilliseconds(), 3)}`;
  }
}
