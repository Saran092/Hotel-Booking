import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-booking-success',
  standalone: true,
  imports: [CommonModule, RouterLink, InrPipe],
  template: `
    <div class="text-center" style="max-width:600px;margin:40px auto">
      <div style="font-size:56px">✅</div>
      <h1 class="mt-1">Booking Confirmed!</h1>
      <p class="text-muted mb-2">Thank you for booking with us. Your reservation is all set.</p>

      @if (booking(); as b) {
        <div class="card text-left">
          <div class="summary-row"><span>Booking Reference</span><strong>{{ b.bookingRef }}</strong></div>
          <div class="summary-row"><span>Room</span><span>{{ b.room?.roomNumber }} ({{ meta.label('roomTypes', b.room?.roomType) }})</span></div>
          <div class="summary-row"><span>Check-in</span><span>{{ b.checkInDate }}</span></div>
          <div class="summary-row"><span>Check-out</span><span>{{ b.checkOutDate }}</span></div>
          <div class="summary-row"><span>Guests</span><span>{{ b.adults }} adult(s), {{ b.children }} child(ren)</span></div>
          <div class="summary-row total"><span>Total Paid</span><span>{{ b.amountPaid | inr }}</span></div>
          @if (payment(); as p) {
            <div class="summary-row"><span>Transaction ID</span><span>{{ p.transactionId }}</span></div>
            <div class="summary-row"><span>Payment Method</span><span>{{ meta.label('paymentMethods', p.method) }}</span></div>
          }
        </div>
        <div class="mt-2 flex gap-1 justify-center wrap">
          <a class="btn btn-secondary" [href]="receiptUrl()" target="_blank">⬇️ Download Receipt</a>
          <a class="btn btn-secondary" [href]="invoiceUrl()" target="_blank">⬇️ Download Invoice</a>
          <a class="btn btn-primary" routerLink="/bookings">View My Bookings</a>
        </div>
      }
    </div>
  `
})
export class BookingSuccessComponent implements OnInit {
  booking = signal<any>(null);
  payment = signal<any>(null);
  private id!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, public meta: MetaService) {}

  ngOnInit(): void {
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.success(this.id).subscribe({
      next: res => { this.booking.set(res.booking); this.payment.set(res.payment); },
      error: () => {}
    });
  }

  receiptUrl(): string { return `/api/bookings/${this.id}/receipt.pdf`; }
  invoiceUrl(): string { return `/api/bookings/${this.id}/invoice.pdf`; }
}
