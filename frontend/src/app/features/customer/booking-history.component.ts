import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { BookingDto } from '../../core/models';
import { InrPipe, FmtDayPipe } from '../../shared/pipes';

@Component({
  selector: 'app-booking-history',
  standalone: true,
  imports: [CommonModule, RouterLink, InrPipe, FmtDayPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>My Bookings</h1>
        <div class="subtitle">{{ upcoming().length }} upcoming · {{ past().length }} past</div>
      </div>
      <a routerLink="/" class="btn btn-primary">+ Book a Room</a>
    </div>

    @if (loading) { <div class="loading-center"><span class="spinner spinner-lg"></span></div> }

    @if (!loading) {
      <h2 class="mb-1">Upcoming Stays</h2>
      @if (upcoming().length) {
        <div class="grid" style="gap:10px">
          @for (b of upcoming(); track b.id) {
            <div class="card flex justify-between items-center wrap gap-2">
              <div>
                <strong>{{ b.bookingRef }}</strong>
                <span class="badge badge-blue" style="margin-left:8px">{{ meta.label('bookingStatus', b.effectiveStatus) }}</span>
                <div class="text-muted" style="font-size:13px">
                  Room {{ b.room?.roomNumber }} · {{ b.checkInDate }} → {{ b.checkOutDate }} · {{ b.nights }} night(s)
                </div>
                <div class="text-muted" style="font-size:13px">Total: {{ b.totalAmount | inr }} · Paid: {{ b.amountPaid | inr }}</div>
              </div>
              <div class="flex gap-1">
                <a class="btn btn-sm btn-secondary" [routerLink]="['/booking', b.id]">Details</a>
                @if (b.effectiveStatus === 'PENDING_PAYMENT') {
                  <a class="btn btn-sm btn-primary" [routerLink]="['/booking', b.id, 'payment']">Pay Now</a>
                }
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="empty-state card mb-2">
          <div class="icon">🗓️</div>
          <p>No upcoming bookings. <a routerLink="/">Book your first stay!</a></p>
        </div>
      }

      <h2 class="mt-3 mb-1">Past Stays</h2>
      @if (past().length) {
        <div class="table-wrap card">
          <table>
            <thead>
              <tr><th>Reference</th><th>Room</th><th>Dates</th><th>Status</th><th class="num">Total</th><th></th></tr>
            </thead>
            <tbody>
              @for (b of past(); track b.id) {
                <tr>
                  <td><strong>{{ b.bookingRef }}</strong></td>
                  <td>{{ b.room?.roomNumber }} ({{ meta.label('roomTypes', b.room?.roomType) }})</td>
                  <td>{{ b.checkInDate | fmtDay }} → {{ b.checkOutDate | fmtDay }}</td>
                  <td><span class="badge {{ statusClass(b.effectiveStatus) }}">{{ meta.label('bookingStatus', b.effectiveStatus) }}</span></td>
                  <td class="num">{{ b.totalAmount | inr }}</td>
                  <td><a class="btn btn-sm btn-secondary" [routerLink]="['/booking', b.id]">Details</a></td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      } @else {
        <div class="empty-state card"><p>No past stays yet.</p></div>
      }
    }
  `
})
export class BookingHistoryComponent implements OnInit {
  upcoming = signal<BookingDto[]>([]);
  past = signal<BookingDto[]>([]);
  loading = true;

  constructor(private api: ApiService, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe();
    this.api.history().subscribe({
      next: res => {
        this.upcoming.set(res.upcoming || []);
        this.past.set(res.past || []);
        this.loading = false;
      },
      error: () => (this.loading = false)
    });
  }

  statusClass(s: string): string {
    switch (s) {
      case 'CONFIRMED': case 'CHECKED_IN': return 'badge-green';
      case 'PENDING_PAYMENT': return 'badge-yellow';
      case 'CANCELLED': return 'badge-red';
      case 'CHECKED_OUT': return 'badge-gray';
      default: return 'badge-gray';
    }
  }
}
