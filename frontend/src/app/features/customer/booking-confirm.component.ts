import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { PriceQuoteDto } from '../../core/models';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-booking-confirm',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Confirm Your Booking</h1>
        <div class="subtitle">Review the details and choose a payment method</div>
      </div>
    </div>

    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    @if (quote(); as q) {
      <div class="grid" style="grid-template-columns:1fr 360px; align-items:start">
        <div class="card">
          <h2 class="mb-1">Booking Details</h2>
          <div class="grid grid-2">
            <div>
              <div class="text-muted" style="font-size:12px">Room</div>
              <div>{{ q.room.roomNumber }} — {{ meta.label('roomTypes', q.room.roomType) }}</div>
            </div>
            <div>
              <div class="text-muted" style="font-size:12px">Bed</div>
              <div>{{ q.room.bedType || 'Standard' }}</div>
            </div>
            <div>
              <div class="text-muted" style="font-size:12px">Check-in</div>
              <div>{{ q.checkIn }}</div>
            </div>
            <div>
              <div class="text-muted" style="font-size:12px">Check-out</div>
              <div>{{ q.checkOut }}</div>
            </div>
            <div>
              <div class="text-muted" style="font-size:12px">Guests</div>
              <div>{{ request.adults }} adult(s), {{ request.children }} child(ren)</div>
            </div>
            <div>
              <div class="text-muted" style="font-size:12px">Nights</div>
              <div>{{ q.nights }}</div>
            </div>
          </div>

          <div class="form-group mt-2">
            <label>Special Requests (optional)</label>
            <textarea name="specialRequests" [(ngModel)]="request.specialRequests" rows="2" maxlength="500"></textarea>
          </div>

          <div class="form-group">
            <label>Payment Method <span class="req">*</span></label>
            <select name="paymentMethod" [(ngModel)]="request.paymentMethod" required>
              <option value="">Select…</option>
              @for (m of paymentMethods; track m) { <option [value]="m">{{ meta.label('paymentMethods', m) }}</option> }
            </select>
          </div>
        </div>

        <div class="card">
          <h2 class="mb-1">Price Summary</h2>
          <div class="summary-row"><span>{{ q.nights }} × {{ q.nightlyRate | inr }}/night</span><span>{{ q.base | inr }}</span></div>
          <div class="summary-row"><span>GST ({{ meta.getCache()?.hotel?.gstPercent || 12 }}%)</span><span>{{ q.gst | inr }}</span></div>
          <div class="summary-row"><span>Service Charge ({{ meta.getCache()?.hotel?.serviceChargePercent || 5 }}%)</span><span>{{ q.service | inr }}</span></div>
          <div class="summary-row total"><span>Total</span><span>{{ q.total | inr }}</span></div>
          <button class="btn btn-primary btn-lg mt-2" style="width:100%" (click)="confirm()" [disabled]="loading || !request.paymentMethod">
            @if (loading) { <span class="spinner"></span> Booking… } @else { Confirm Booking }
          </button>
          <div class="text-muted text-center mt-1" style="font-size:12px">You will be redirected to payment</div>
        </div>
      </div>
    } @else if (loading) {
      <div class="loading-center"><span class="spinner spinner-lg"></span></div>
    }
  `
})
export class BookingConfirmComponent implements OnInit {
  request: any = { roomId: null, checkIn: '', checkOut: '', adults: 1, children: 0, specialRequests: '', paymentMethod: '' };
  quote = signal<PriceQuoteDto | null>(null);
  paymentMethods: string[] = [];
  loading = true;
  error = '';

  constructor(private api: ApiService, private router: Router, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      if (m?.enums?.paymentMethods) this.paymentMethods = m.enums.paymentMethods;
    });
    const raw = sessionStorage.getItem('bookingRequest');
    if (!raw) {
      this.error = 'No booking request found. Please search for a room first.';
      this.loading = false;
      return;
    }
    this.request = JSON.parse(raw);
    this.api.previewBooking({ ...this.request, paymentMethod: undefined }).subscribe({
      next: q => { this.quote.set(q); this.loading = false; },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Could not load the booking preview.';
      }
    });
  }

  confirm(): void {
    this.loading = true;
    this.error = '';
    const req = { ...this.request, paymentMethod: this.request.paymentMethod };
    this.api.createBooking(req).subscribe({
      next: res => {
        sessionStorage.removeItem('bookingRequest');
        this.router.navigate(['/booking', res.bookingId, 'payment']);
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Booking failed. Please try again.';
      }
    });
  }
}
