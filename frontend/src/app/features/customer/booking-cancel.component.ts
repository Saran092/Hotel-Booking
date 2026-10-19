import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-booking-cancel',
  standalone: true,
  imports: [CommonModule, RouterLink, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Cancel Booking</h1>
        @if (data(); as d) { <div class="subtitle">{{ d.booking.bookingRef }}</div> }
      </div>
      <a class="btn btn-secondary" [routerLink]="['/booking', bookingId]">← Keep Booking</a>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    @if (data(); as d) {
      @if (!confirmed()) {
        <div class="card" style="max-width:600px">
          <h2 class="mb-1">Cancellation Details</h2>
          <div class="summary-row"><span>Room</span><span>{{ d.booking.room?.roomNumber }} ({{ meta.label('roomTypes', d.booking.room?.roomType) }})</span></div>
          <div class="summary-row"><span>Dates</span><span>{{ d.booking.checkInDate }} → {{ d.booking.checkOutDate }}</span></div>
          <div class="summary-row"><span>Total Paid</span><span>{{ d.booking.amountPaid | inr }}</span></div>

          <div class="alert alert-warning mt-2">{{ d.quote.message }}</div>

          @if (d.quote.allowed) {
            <div class="summary-row"><span>Refund Percent</span><span>{{ d.quote.refundPercent }}%</span></div>
            <div class="summary-row total"><span>Refund Amount</span><span>{{ d.quote.refundAmount | inr }}</span></div>
            <button class="btn btn-danger btn-lg mt-2" (click)="cancel()">Yes, Cancel This Booking</button>
          } @else {
            <div class="alert alert-danger mt-1">{{ d.quote.blockedReason || 'This booking cannot be canceled.' }}</div>
          }
        </div>
      } @else {
        <div class="card" style="max-width:600px">
          <h2>✅ Booking Canceled</h2>
          @if (d.booking.refundAmount > 0) {
            <p class="mt-1">A refund of <strong>{{ d.booking.refundAmount | inr }}</strong> will be processed within 3-5 business days.</p>
          }
          <a routerLink="/bookings" class="btn btn-primary mt-2">Back to My Bookings</a>
        </div>
      }
    }
  `
})
export class BookingCancelComponent implements OnInit {
  data = signal<any>(null);
  confirmed = signal(false);
  message = signal('');
  error = '';
  bookingId!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe();
    this.bookingId = Number(this.route.snapshot.paramMap.get('id'));
    this.api.cancelQuote(this.bookingId).subscribe({
      next: res => this.data.set(res),
      error: err => { this.error = err.error?.message || 'Could not load cancellation details.'; }
    });
  }

  cancel(): void {
    this.error = '';
    this.api.cancelBooking(this.bookingId).subscribe({
      next: res => {
        this.message.set(res.message);
        this.confirmed.set(true);
        this.data.update(d => ({ ...d, booking: res.booking }));
      },
      error: err => { this.error = err.error?.message || 'Cancellation failed.'; }
    });
  }
}
