import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Payment</h1>
        <div class="subtitle">
          @if (form(); as f) {
            {{ f.type === 'MODIFICATION' ? 'Pay modification difference' : 'Complete your booking payment' }}
          }
        </div>
      </div>
    </div>

    @if (message()) { <div class="alert {{ status()==='SUCCESS' ? 'alert-success' : status()==='DECLINED' || status()==='INVALID' || status()==='EXPIRED' ? 'alert-danger' : 'alert-info' }}">{{ message() }}</div> }

    @if (countdown() !== null) {
      <div class="alert alert-warning">
        ⏳ Payment session expires in <span class="countdown">{{ countdown() }}</span>
      </div>
    }

    @if (form(); as f) {
      <div class="grid" style="grid-template-columns:1fr 340px; align-items:start">
        <div class="card">
          <h2 class="mb-1">Enter Payment Details</h2>
          @if (method() === 'CREDIT_CARD' || method() === 'DEBIT_CARD') {
            <div class="form-group">
              <label>Cardholder Name <span class="req">*</span></label>
              <input type="text" name="cardHolder" [(ngModel)]="payForm.cardHolder" />
              @if (errors()['cardHolder']) { <div class="field-error">{{ errors()['cardHolder'] }}</div> }
            </div>
            <div class="grid grid-2">
              <div class="form-group">
                <label>Card Number <span class="req">*</span></label>
                <input type="text" name="cardNumber" [(ngModel)]="payForm.cardNumber" maxlength="19" placeholder="4111 1111 1111 1111" />
                @if (errors()['cardNumber']) { <div class="field-error">{{ errors()['cardNumber'] }}</div> }
                <div class="form-hint">Test card: 4111111111111111 (success), …0000 (declined)</div>
              </div>
              <div class="form-group">
                <label>Billing Address <span class="req">*</span></label>
                <input type="text" name="billingAddress" [(ngModel)]="payForm.billingAddress" />
                @if (errors()['billingAddress']) { <div class="field-error">{{ errors()['billingAddress'] }}</div> }
              </div>
              <div class="form-group">
                <label>Expiry (MM/YY) <span class="req">*</span></label>
                <input type="text" name="expiry" [(ngModel)]="payForm.expiry" maxlength="5" placeholder="12/28" />
                @if (errors()['expiry']) { <div class="field-error">{{ errors()['expiry'] }}</div> }
              </div>
              <div class="form-group">
                <label>CVV <span class="req">*</span></label>
                <input type="password" name="cvv" [(ngModel)]="payForm.cvv" maxlength="4" />
                @if (errors()['cvv']) { <div class="field-error">{{ errors()['cvv'] }}</div> }
              </div>
            </div>
          } @else if (method() === 'UPI') {
            <div class="form-group">
              <label>UPI ID <span class="req">*</span></label>
              <input type="text" name="upiId" [(ngModel)]="payForm.upiId" placeholder="name@bank" />
              @if (errors()['upiId']) { <div class="field-error">{{ errors()['upiId'] }}</div> }
              <div class="form-hint">Use UPI ID ending in &#64;fail to simulate a declined payment</div>
            </div>
          } @else if (method() === 'WALLET') {
            <div class="form-group">
              <label>Select Wallet <span class="req">*</span></label>
              <select name="wallet" [(ngModel)]="payForm.wallet">
                <option value="">Select…</option>
                @for (w of wallets; track w) { <option [value]="w">{{ w }}</option> }
              </select>
              @if (errors()['wallet']) { <div class="field-error">{{ errors()['wallet'] }}</div> }
            </div>
          } @else if (method() === 'NET_BANKING') {
            <div class="form-group">
              <label>Select Bank <span class="req">*</span></label>
              <select name="bank" [(ngModel)]="payForm.bank">
                <option value="">Select…</option>
                @for (b of banks; track b) { <option [value]="b">{{ b }}</option> }
              </select>
              @if (errors()['bank']) { <div class="field-error">{{ errors()['bank'] }}</div> }
            </div>
          }

          @if (status() !== 'SUCCESS') {
            <button class="btn btn-primary btn-lg mt-1" (click)="pay()" [disabled]="loading">
              @if (loading) { <span class="spinner"></span> Processing… } @else { Pay {{ amount() | inr }} }
            </button>
          }
        </div>

        <div class="card">
          <h2 class="mb-1">Order Summary</h2>
          @if (f.booking) {
            <div class="summary-row"><span>Booking</span><span>{{ f.booking.bookingRef }}</span></div>
            <div class="summary-row"><span>Room</span><span>{{ f.booking.room?.roomNumber }} ({{ f.booking.room?.roomType }})</span></div>
            <div class="summary-row"><span>Dates</span><span>{{ f.booking.checkInDate }} → {{ f.booking.checkOutDate }}</span></div>
          }
          <div class="summary-row total"><span>Amount Due</span><span>{{ amount() | inr }}</span></div>
          <div class="text-muted mt-1" style="font-size:12px">Session length: {{ f.paymentSessionMinutes }} minutes</div>
        </div>
      </div>
    } @else if (loading) {
      <div class="loading-center"><span class="spinner spinner-lg"></span></div>
    } @else if (!message()) {
      <div class="alert alert-danger">There is no payment in progress for this booking.</div>
      <a routerLink="/bookings" class="btn btn-secondary">Back to My Bookings</a>
    }
  `
})
export class PaymentComponent implements OnInit, OnDestroy {
  form = signal<any>(null);
  amount = signal(0);
  method = signal('');
  payForm: any = { cardHolder: '', cardNumber: '', expiry: '', cvv: '', billingAddress: '', upiId: '', wallet: '', bank: '' };
  wallets: string[] = [];
  banks: string[] = [];
  loading = true;
  status = signal('');
  message = signal('');
  errors = signal<Record<string, string>>({});
  countdown = signal<string | null>(null);
  private timer: any;
  private bookingId!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, private router: Router, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      this.wallets = m?.lists?.wallets || [];
      this.banks = m?.lists?.banks || [];
    });
    this.bookingId = Number(this.route.snapshot.paramMap.get('id'));
    this.api.paymentForm(this.bookingId).subscribe({
      next: res => {
        this.form.set(res);
        this.amount.set(res.amount);
        this.method.set(res.booking?.paymentMethod || '');
        this.loading = false;
        this.startCountdown(res.holdExpiresAt || res.booking?.holdExpiresAt);
      },
      error: err => {
        this.loading = false;
        this.message.set(err.error?.message || 'Could not load payment form.');
      }
    });
  }

  ngOnDestroy(): void { if (this.timer) clearInterval(this.timer); }

  private startCountdown(expiresAt: string | null): void {
    if (!expiresAt) return;
    const update = () => {
      const ms = new Date(expiresAt).getTime() - Date.now();
      if (ms <= 0) { this.countdown.set('00:00'); if (this.timer) clearInterval(this.timer); return; }
      const m = Math.floor(ms / 60000);
      const s = Math.floor((ms % 60000) / 1000);
      this.countdown.set(`${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`);
    };
    update();
    this.timer = setInterval(update, 1000);
  }

  pay(): void {
    this.loading = true;
    this.errors.set({});
    this.status.set('');
    this.message.set('');
    this.api.pay(this.bookingId, this.payForm).subscribe({
      next: res => {
        this.loading = false;
        this.status.set(res.status);
        this.message.set(res.message);
        if (res.status === 'SUCCESS') {
          if (this.timer) clearInterval(this.timer);
          setTimeout(() => this.router.navigate(['/booking', this.bookingId, 'success']), 1500);
        }
      },
      error: err => {
        this.loading = false;
        if (err.error?.errors) this.errors.set(err.error.errors);
        this.message.set(err.error?.message || 'Payment failed. Please try again.');
        this.status.set('ERROR');
      }
    });
  }
}
