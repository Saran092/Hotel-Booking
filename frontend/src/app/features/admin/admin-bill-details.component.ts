import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe, FmtDatePipe } from '../../shared/pipes';

@Component({
  selector: 'app-admin-bill-details',
  standalone: true,
  imports: [CommonModule, RouterLink, InrPipe, FmtDatePipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Bill {{ bill()?.billRef }}</h1>
        @if (bill(); as b) { <div class="subtitle">{{ b.customer?.fullName }} · {{ b.issueDate | fmtDate }}</div> }
      </div>
      <div class="flex gap-1">
        <a routerLink="/admin/bills" class="btn btn-secondary">← Back</a>
        @if (bill(); as b) {
          <a class="btn btn-primary" [routerLink]="['/admin/bills', b.id, 'edit']">Edit</a>
        }
      </div>
    </div>

    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    @if (bill(); as b) {
      <div class="card mb-2">
        <div class="grid grid-2">
          <div>
            <div class="text-muted" style="font-size:12px">Customer</div>
            <div>{{ b.customer?.fullName }} ({{ b.customer?.username }})</div>
          </div>
          <div>
            <div class="text-muted" style="font-size:12px">Status</div>
            <span class="badge {{ payClass(b.paymentStatus) }}">{{ meta.label('paymentStatuses', b.paymentStatus) }}</span>
          </div>
          @if (b.bookingRef) {
            <div>
              <div class="text-muted" style="font-size:12px">Related Booking</div>
              <div>{{ b.bookingRef }}</div>
            </div>
          }
          @if (b.notes) {
            <div style="grid-column:1/-1">
              <div class="text-muted" style="font-size:12px">Notes</div>
              <div style="white-space:pre-wrap">{{ b.notes }}</div>
            </div>
          }
        </div>
      </div>

      <div class="table-wrap card">
        <table>
          <thead>
            <tr>
              <th>Category</th><th>Description</th><th>Service Date</th>
              <th class="num">Qty</th><th class="num">Unit Price</th>
              <th class="num">Tax %</th><th class="num">Discount</th><th class="num">Line Total</th>
            </tr>
          </thead>
          <tbody>
            @for (item of b.items; track item.id) {
              <tr>
                <td>{{ meta.label('billCategories', item.category) }}</td>
                <td>{{ item.description }}</td>
                <td>{{ item.serviceDateTime || '—' }}</td>
                <td class="num">{{ item.quantity }}</td>
                <td class="num">{{ item.unitPrice | inr }}</td>
                <td class="num">{{ item.taxPercent }}%</td>
                <td class="num">{{ item.discount | inr }}</td>
                <td class="num"><strong>{{ item.lineTotal | inr }}</strong></td>
              </tr>
            }
          </tbody>
          <tfoot>
            <tr>
              <td colspan="7" class="text-right"><strong>Total</strong></td>
              <td class="num"><strong style="font-size:16px">{{ b.totalAmount | inr }}</strong></td>
            </tr>
          </tfoot>
        </table>
      </div>
    }
  `
})
export class AdminBillDetailsComponent implements OnInit {
  bill = signal<any>(null);
  error = '';

  constructor(private api: ApiService, private route: ActivatedRoute, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe();
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.billDetails(id).subscribe({
      next: b => this.bill.set(b),
      error: err => { this.error = err.error?.message || 'Could not load bill.'; }
    });
  }

  payClass(s: string): string {
    return s === 'PAID' ? 'badge-green' : s === 'PENDING' ? 'badge-yellow' : s === 'REFUNDED' ? 'badge-gray' : 'badge-yellow';
  }
}
