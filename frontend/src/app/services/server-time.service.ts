import { Injectable, signal } from '@angular/core';
import { isoLima } from '../utils/hora-lima';

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

  /** Fecha y hora de Lima en ISO sin zona (yyyy-MM-ddTHH:mm:ss.SSS), sea cual sea la zona del navegador. */
  getNowISOString(): string {
    return isoLima(this.currentServerTime);
  }
}
