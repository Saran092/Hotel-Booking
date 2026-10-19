import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe, FmtDayPipe, FmtDatePipe } from '../../shared/pipes';

@Component({
  selector: 'app-booking-details',
  standalone: true,
  imports: [CommonModule, RouterLink, InrPipe, FmtDayPipe, FmtDatePipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Booking Details</h1>
        @if (data(); as d) { <div class="subtitle">{{ d.booking.bookingRef }}</div> }
      </div>
      <a routerLink="/bookings" class="btn btn-secondary">← Back to My Bookings</a>
    </div>

    @if (loading) { <div class="loading-center"><span class="spinner spinner-lg"></span></div> }

    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    @if (data(); as d) {
      <div class="grid" style="grid-template-columns:1fr 340px; align-items:start">
        <div>
          <div class="card">
            <div class="flex justify-between items-center mb-1">
              <h2>Reservation</h2>
              <span class="badge {{ statusClass(d.booking.effectiveStatus) }}">{{ meta.label('bookingStatus', d.booking.effectiveStatus) }}</span>
            </div>
            <div class="grid grid-2">
              <div><div class="text-muted" style="font-size:12px">Room</div>{{ d.booking.room?.roomNumber }} — {{ meta.label('roomTypes', d.booking.room?.roomType) }}</div>
              <div><div class="text-muted" style="font-size:12px">Bed</div>{{ d.booking.room?.bedType || 'Standard' }}</div>
              <div><div class="text-muted" style="font-size:12px">Check-in</div>{{ d.booking.checkInDate | fmtDay }}</div>
              <div><div class="text-muted" style="font-size:12px">Check-out</div>{{ d.booking.checkOutDate | fmtDay }}</div>
              <div><div class="text-muted" style="font-size:12px">Guests</div>{{ d.booking.adults }} adult(s), {{ d.booking.children }} child(ren)</div>
              <div><div class="text-muted" style="font-size:12px">Nights</div>{{ d.booking.nights }}</div>
              <div><div class="text-muted" style="font-size:12px">Payment Method</div>{{ meta.label('paymentMethods', d.booking.paymentMethod) }}</div>
              <div><div class="text-muted" style="font-size:12px">Payment Status</div>{{ meta.label('paymentStatus', d.booking.paymentStatus) }}</div>
              @if (d.booking.specialRequests) {
                <div style="grid-column:1/-1"><div class="text-muted" style="font-size:12px">Special Requests</div>{{ d.booking.specialRequests }}</div>
              }
              <div style="grid-column:1/-1"><div class="text-muted" style="font-size:12px">Booked On</div>{{ d.booking.createdAt | fmtDate }}</div>
            </div>
          </div>

          @if (d.booking.effectiveStatus !== 'PENDING_PAYMENT' && d.booking.effectiveStatus !== 'CANCELLED') {
            <div class="card mt-2">
              <h2 class="mb-1">Documents</h2>
              <div class="flex gap-1 wrap">
                @if (d.booking.amountPaid > 0) {
                  <a class="btn btn-secondary" [href]="pdfUrl('receipt')" target="_blank">⬇️ Receipt (PDF)</a>
                }
                @if (d.booking.paymentStatus === 'PAID' || d.booking.paymentStatus === 'REFUNDED') {
                  <a class="btn btn-secondary" [href]="pdfUrl('invoice')" target="_blank">⬇️ Invoice (PDF)</a>
                }
              </div>
            </div>
          }
        </div>

        <div>
          <div class="card">
            <h2 class="mb-1">Payment Summary</h2>
            <div class="summary-row"><span>Nightly Rate</span><span>{{ d.booking.nightlyRate | inr }}</span></div>
            <div class="summary-row"><span>Base ({{ d.booking.nights }} nights)</span><span>{{ d.booking.baseAmount | inr }}</span></div>
            <div class="summary-row"><span>GST</span><span>{{ d.booking.gstAmount | inr }}</span></div>
            <div class="summary-row"><span>Service Charge</span><span>{{ d.booking.serviceChargeAmount | inr }}</span></div>
            <div class="summary-row total"><span>Total</span><span>{{ d.booking.totalAmount | inr }}</span></div>
            <div class="summary-row"><span>Amount Paid</span><span class="text-success">{{ d.booking.amountPaid | inr }}</span></div>
            @if (d.booking.refundAmount > 0) {
              <div class="summary-row"><span>Refunded</span><span class="text-danger">{{ d.booking.refundAmount | inr }}</span></div>
            }
          </div>

          @if (d.cancellation && d.cancellation.allowed) {
            <div class="card mt-2">
              <h2 class="mb-1">Cancellation</h2>
              <div class="text-muted" style="font-size:13px;margin-bottom:10px">{{ d.cancellation.message }}</div>
              <a class="btn btn-danger" [routerLink]="['/booking', d.booking.id, 'cancel']">Cancel Booking</a>
            </div>
          }

          @if (d.modifiable && d.booking.effectiveStatus === 'CONFIRMED') {
            <div class="card mt-2">
              <h2 class="mb-1">Modify</h2>
              <div class="text-muted" style="font-size:13px;margin-bottom:10px">Change dates, guests or room type.</div>
              <a class="btn btn-primary" [routerLink]="['/booking', d.booking.id, 'modify']">Modify Booking</a>
            </div>
          }

          @if (d.booking.effectiveStatus === 'PENDING_PAYMENT') {
            <div class="card mt-2">
              <div class="alert alert-warning mb-1">Payment is required to confirm this booking.</div>
              <a class="btn btn-primary" style="width:100%" [routerLink]="['/booking', d.booking.id, 'payment']">Pay Now</a>
            </div>
          }
        </div>
      </div>
    }
  `
})
export class BookingDetailsComponent implements OnInit {
  data = signal<any>(null);
  loading = true;
  error = '';
  private id!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe();
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.bookingDetails(this.id).subscribe({
      next: res => { this.data.set(res); this.loading = false; },
      error: err => { this.loading = false; this.error = err.error?.message || 'Could not load booking.'; }
    });
  }

  pdfUrl(kind: 'receipt' | 'invoice'): string {
    return `/api/bookings/${this.id}/${kind}.pdf`;
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
