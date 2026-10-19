import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe, FmtDatePipe } from '../../shared/pipes';

@Component({
  selector: 'app-admin-bills',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InrPipe, FmtDatePipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Bills</h1>
        <div class="subtitle">{{ bills().length }} bill(s) found</div>
      </div>
      <a routerLink="/admin/bills/new" class="btn btn-primary">+ New Bill</a>
    </div>

    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    <div class="card mb-2">
      <form (ngSubmit)="load()">
        <div class="filter-bar">
          <div class="form-group"><label>Bill ID</label><input type="text" name="billId" [(ngModel)]="filter.billId" placeholder="BILL…" /></div>
          <div class="form-group"><label>Customer</label><input type="text" name="customer" [(ngModel)]="filter.customer" /></div>
          <div class="form-group"><label>Date From</label><input type="date" name="dateFrom" [(ngModel)]="filter.dateFrom" /></div>
          <div class="form-group"><label>Date To</label><input type="date" name="dateTo" [(ngModel)]="filter.dateTo" /></div>
          <div class="form-group">
            <label>Status</label>
            <select name="status" [(ngModel)]="filter.status">
              <option value="">All</option>
              @for (s of paymentStatuses; track s) { <option [value]="s">{{ meta.label('paymentStatuses', s) }}</option> }
            </select>
          </div>
          <div class="form-group"><label>Min Amount</label><input type="number" name="minAmount" [(ngModel)]="filter.minAmount" /></div>
          <div class="form-group"><label>Max Amount</label><input type="number" name="maxAmount" [(ngModel)]="filter.maxAmount" /></div>
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
              <th>Bill Ref</th><th>Customer</th><th>Booking</th>
              <th>Issued</th><th>Items</th><th class="num">Total</th><th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>
            @for (b of bills(); track b.id) {
              <tr>
                <td><strong>{{ b.billRef }}</strong></td>
                <td>{{ b.customer?.fullName || '—' }}</td>
                <td>{{ b.bookingRef || '—' }}</td>
                <td>{{ b.issueDate | fmtDate }}</td>
                <td>{{ b.items?.length || 0 }}</td>
                <td class="num">{{ b.totalAmount | inr }}</td>
                <td><span class="badge {{ payClass(b.paymentStatus) }}">{{ meta.label('paymentStatus', b.paymentStatus) }}</span></td>
                <td>
                  <div class="flex gap-1 wrap">
                    <a class="btn btn-sm btn-secondary" [routerLink]="['/admin/bills', b.id]">View</a>
                    <a class="btn btn-sm btn-secondary" [routerLink]="['/admin/bills', b.id, 'edit']">Edit</a>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="8" class="text-center text-muted" style="padding:32px">No bills found.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `
})
export class AdminBillsComponent implements OnInit {
  bills = signal<any[]>([]);
  paymentStatuses: string[] = ['PENDING', 'PARTIALLY_PAID', 'PAID', 'REFUNDED'];
  filter: any = { billId: '', customer: '', dateFrom: '', dateTo: '', status: '', minAmount: null, maxAmount: null };
  loading = true;
  error = '';

  constructor(private api: ApiService, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe();
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.api.adminBills(this.filter).subscribe({
      next: res => { this.bills.set(res.bills || []); this.loading = false; },
      error: err => { this.loading = false; this.error = err.error?.message || 'Could not load bills.'; }
    });
  }

  reset(): void {
    this.filter = { billId: '', customer: '', dateFrom: '', dateTo: '', status: '', minAmount: null, maxAmount: null };
    this.load();
  }

  payClass(s: string): string {
    return s === 'PAID' ? 'badge-green' : s === 'PENDING' ? 'badge-yellow' : s === 'REFUNDED' ? 'badge-gray' : 'badge-yellow';
  }
}
