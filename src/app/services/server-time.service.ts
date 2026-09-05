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

  getNowISOString(): string {
    return this.currentServerTime.toISOString();
  }
}
