import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-booking-modify',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Modify Booking</h1>
        @if (booking(); as b) { <div class="subtitle">{{ b.bookingRef }} · currently {{ meta.label('roomTypes', b.room?.roomType) }}, {{ b.checkInDate }} → {{ b.checkOutDate }}</div> }
      </div>
      <a class="btn btn-secondary" [routerLink]="['/booking', bookingId]">← Back to Details</a>
    </div>

    @if (error) { <div class="alert alert-danger">{{ error }}</div> }
    @if (message()) { <div class="alert {{ ok() ? 'alert-success' : 'alert-warning' }}">{{ message() }}</div> }

    @if (booking(); as b) {
      <div class="grid" style="grid-template-columns:1fr 340px; align-items:start">
        <div class="card">
          <h2 class="mb-1">New Stay Details</h2>
          <div class="grid grid-2">
            <div class="form-group">
              <label>Check-in</label>
              <input type="date" name="checkIn" [(ngModel)]="form.checkIn" [min]="today" (change)="checkAvailability()" />
            </div>
            <div class="form-group">
              <label>Check-out</label>
              <input type="date" name="checkOut" [(ngModel)]="form.checkOut" [min]="form.checkIn || today" (change)="checkAvailability()" />
            </div>
            <div class="form-group">
              <label>Room Type</label>
              <select name="roomType" [(ngModel)]="form.roomType" (change)="checkAvailability()">
                @for (t of roomTypes; track t) { <option [value]="t">{{ meta.label('roomTypes', t) }}</option> }
              </select>
            </div>
            <div class="form-group">
              <label>Guests</label>
              <div class="flex gap-1">
                <input type="number" name="adults" [(ngModel)]="form.adults" min="1" max="10" (change)="checkAvailability()" />
                <input type="number" name="children" [(ngModel)]="form.children" min="0" max="6" (change)="checkAvailability()" />
              </div>
              <div class="form-hint">adults / children</div>
            </div>
          </div>
        </div>

        <div class="card">
          <h2 class="mb-1">Draft Changes</h2>
          @if (draft(); as d) {
            <div class="summary-row"><span>New Room</span><span>{{ d.roomNumber }} ({{ d.roomType }})</span></div>
            <div class="summary-row"><span>Nights</span><span>{{ d.nights }}</span></div>
            <div class="summary-row"><span>Base</span><span>{{ d.base | inr }}</span></div>
            <div class="summary-row"><span>GST</span><span>{{ d.gst | inr }}</span></div>
            <div class="summary-row"><span>Service</span><span>{{ d.service | inr }}</span></div>
            <div class="summary-row total"><span>New Total</span><span>{{ d.total | inr }}</span></div>
            @if (d.difference > 0) {
              <div class="alert alert-warning mt-1">Additional {{ d.difference | inr }} to pay</div>
            } @else if (d.difference < 0) {
              <div class="alert alert-info mt-1">Refund of {{ -d.difference | inr }} will be processed after the change.</div>
            } @else {
              <div class="alert alert-success mt-1">No price difference.</div>
            }
            <button class="btn btn-primary mt-1" style="width:100%" (click)="confirm()">Confirm Changes</button>
          } @else {
            <div class="text-muted" style="font-size:13px">Adjust the details and the updated quote will appear here.</div>
          }
        </div>
      </div>
    }
  `
})
export class BookingModifyComponent implements OnInit {
  today = new Date().toISOString().slice(0, 10);
  form: any = { checkIn: '', checkOut: '', adults: 1, children: 0, roomType: '' };
  booking = signal<any>(null);
  draft = signal<any>(null);
  message = signal('');
  ok = signal(false);
  roomTypes: string[] = [];
  error = '';
  bookingId!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, private router: Router, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      if (m?.enums?.roomTypes) this.roomTypes = m.enums.roomTypes;
    });
    this.bookingId = Number(this.route.snapshot.paramMap.get('id'));
    this.api.modifyForm(this.bookingId).subscribe({
      next: res => {
        this.booking.set(res.booking);
        const f = res.form || {};
        this.form = {
          checkIn: f.checkIn || res.booking.checkInDate,
          checkOut: f.checkOut || res.booking.checkOutDate,
          adults: f.adults ?? res.booking.adults,
          children: f.children ?? res.booking.children,
          roomType: f.roomType || res.booking.room?.roomType,
        };
      },
      error: err => { this.error = err.error?.message || 'This booking cannot be modified.'; }
    });
  }

  checkAvailability(): void {
    this.draft.set(null);
    this.message.set('');
    this.api.modifyQuote(this.bookingId, this.form).subscribe({
      next: res => {
        this.ok.set(true);
        this.message.set(res.message);
        this.draft.set(res.draft);
      },
      error: err => {
        this.ok.set(false);
        this.message.set(err.error?.message || 'The selected options are not available.');
        this.draft.set(null);
      }
    });
  }

  confirm(): void {
    this.api.modifyConfirm(this.bookingId).subscribe({
      next: res => {
        if (res.status === 'PAYMENT_REQUIRED') {
          this.router.navigate(['/booking', this.bookingId, 'payment']);
        } else {
          this.message.set(res.message);
          this.ok.set(true);
          setTimeout(() => this.router.navigate(['/booking', this.bookingId]), 1500);
        }
      },
      error: err => { this.error = err.error?.message || 'Modification failed.'; }
    });
  }
}
