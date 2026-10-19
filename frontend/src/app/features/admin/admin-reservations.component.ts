import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe, FmtDayPipe } from '../../shared/pipes';

@Component({
  selector: 'app-admin-reservations',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InrPipe, FmtDayPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Reservations</h1>
        <div class="subtitle">{{ reservations().length }} reservation(s) found</div>
      </div>
      <a routerLink="/admin/reservations/new" class="btn btn-primary">+ New Reservation</a>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    <div class="card mb-2">
      <form (ngSubmit)="load()">
        <div class="filter-bar">
          <div class="form-group"><label>Reservation ID</label><input type="text" name="reservationId" [(ngModel)]="filter.reservationId" placeholder="BK…" /></div>
          <div class="form-group"><label>Customer</label><input type="text" name="customer" [(ngModel)]="filter.customer" /></div>
          <div class="form-group"><label>Room #</label><input type="text" name="roomNumber" [(ngModel)]="filter.roomNumber" /></div>
          <div class="form-group"><label>Check-in From</label><input type="date" name="checkIn" [(ngModel)]="filter.checkIn" /></div>
          <div class="form-group"><label>Check-out To</label><input type="date" name="checkOut" [(ngModel)]="filter.checkOut" /></div>
          <div class="form-group">
            <label>Status</label>
            <select name="status" [(ngModel)]="filter.status">
              <option value="">All</option>
              @for (s of statuses; track s) { <option [value]="s">{{ meta.label('bookingStatuses', s) }}</option> }
            </select>
          </div>
          <div class="form-group">
            <label>Room Type</label>
            <select name="roomType" [(ngModel)]="filter.roomType">
              <option value="">All</option>
              @for (t of roomTypes; track t) { <option [value]="t">{{ meta.label('roomTypes', t) }}</option> }
            </select>
          </div>
          <div class="form-group"><label>Booked On</label><input type="date" name="bookingDate" [(ngModel)]="filter.bookingDate" /></div>
          <button class="btn btn-primary" [disabled]="loading">Filter</button>
          <button type="button" class="btn btn-secondary" (click)="reset()">Reset</button>
        </div>
      </form>
    </div>

    @if (loading) { <div class="loading-center"><span class="spinner spinner-lg"></span></div> }

    @if (!loading) {
      <div class="table-wrap card">
        <table>
          <thead>
            <tr>
              <th>Ref</th><th>Customer</th><th>Room</th><th>Dates</th>
              <th class="num">Total</th><th>Status</th><th>Payment</th><th></th>
            </tr>
          </thead>
          <tbody>
            @for (r of reservations(); track r.id) {
              <tr>
                <td><strong>{{ r.bookingRef }}</strong></td>
                <td>{{ r.customer?.fullName || '—' }}</td>
                <td>{{ r.room?.roomNumber }} ({{ meta.label('roomTypes', r.room?.roomType) }})</td>
                <td>{{ r.checkInDate | fmtDay }} → {{ r.checkOutDate | fmtDay }}</td>
                <td class="num">{{ r.totalAmount | inr }}</td>
                <td><span class="badge {{ statusClass(r.effectiveStatus) }}">{{ meta.label('bookingStatus', r.effectiveStatus) }}</span></td>
                <td><span class="badge {{ payClass(r.paymentStatus) }}">{{ meta.label('paymentStatus', r.paymentStatus) }}</span></td>
                <td>
                  <div class="flex gap-1 wrap">
                    <a class="btn btn-sm btn-secondary" [routerLink]="['/admin/reservations', r.id, 'edit']">Edit</a>
                    @if (r.effectiveStatus === 'CONFIRMED' || r.effectiveStatus === 'PENDING_PAYMENT') {
                      <button class="btn btn-sm btn-danger" (click)="cancel(r)">Cancel</button>
                    }
                  </div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="text-center text-muted" style="padding:32px">No reservations found.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `
})
export class AdminReservationsComponent implements OnInit {
  reservations = signal<any[]>([]);
  statuses: string[] = ['PENDING_PAYMENT', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED'];
  roomTypes: string[] = [];
  filter: any = { reservationId: '', customer: '', roomNumber: '', checkIn: '', checkOut: '', status: '', roomType: '', bookingDate: '' };
  loading = true;
  message = signal('');
  error = '';

  constructor(private api: ApiService, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => { this.roomTypes = m?.enums?.roomTypes || []; });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.api.adminReservations(this.filter).subscribe({
      next: res => { this.reservations.set(res.reservations || []); this.loading = false; },
      error: err => { this.loading = false; this.error = err.error?.message || 'Could not load reservations.'; }
    });
  }

  reset(): void {
    this.filter = { reservationId: '', customer: '', roomNumber: '', checkIn: '', checkOut: '', status: '', roomType: '', bookingDate: '' };
    this.load();
  }

  cancel(r: any): void {
    if (!confirm(`Cancel reservation ${r.bookingRef}?`)) return;
    this.api.cancelAdminReservation(r.id).subscribe({
      next: res => { this.message.set(res.message); this.load(); },
      error: err => { this.error = err.error?.message || 'Cancel failed.'; }
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
  payClass(s: string): string {
    return s === 'PAID' ? 'badge-green' : s === 'PENDING' ? 'badge-yellow' : s === 'REFUNDED' ? 'badge-gray' : 'badge-yellow';
  }
}
