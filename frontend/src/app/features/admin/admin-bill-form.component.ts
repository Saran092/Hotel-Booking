import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-admin-bill-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>{{ isEdit ? 'Edit Bill' : 'Create Bill' }}</h1>
        @if (isEdit && bill(); as b) { <div class="subtitle">{{ b.billRef }}</div> }
      </div>
      <a routerLink="/admin/bills" class="btn btn-secondary">← Back to Bills</a>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error) { <div class="alert alert-danger">{{ error }}</div> }
    @if (fieldErrorList().length) {
      <div class="alert alert-danger">
        @for (e of fieldErrorList(); track e[0]) { <div><strong>{{ e[0] }}:</strong> {{ e[1] }}</div> }
      </div>
    }

    <form (ngSubmit)="save()" #f="ngForm">
      <div class="card mb-2">
        <h2 class="mb-1">Bill Header</h2>
        <div class="grid grid-2">
          <div class="form-group">
            <label>Customer <span class="req">*</span></label>
            <select name="customerId" [(ngModel)]="form.customerId" required>
              <option [ngValue]="null">Select…</option>
              @for (c of customers(); track c.id) { <option [ngValue]="c.id">{{ c.fullName }} ({{ c.username }})</option> }
            </select>
          </div>
          <div class="form-group">
            <label>Payment Status <span class="req">*</span></label>
            <select name="paymentStatus" [(ngModel)]="form.paymentStatus" required>
              @for (s of paymentStatuses; track s) { <option [value]="s">{{ meta.label('paymentStatuses', s) }}</option> }
            </select>
          </div>
        </div>
        <div class="form-group">
          <label>Notes</label>
          <textarea name="notes" [(ngModel)]="form.notes" rows="2" maxlength="500"></textarea>
        </div>
      </div>

      <div class="card mb-2">
        <div class="flex justify-between items-center mb-1">
          <h2>Line Items</h2>
          <button type="button" class="btn btn-secondary btn-sm" (click)="addItem()">+ Add Item</button>
        </div>
        @for (item of form.items; track $index; let i = $index) {
          <div class="card" style="background:var(--gray-50);margin-bottom:10px">
            <div class="grid grid-4">
              <div class="form-group">
                <label>Category <span class="req">*</span></label>
                <select [(ngModel)]="item.category" [name]="'cat' + i">
                  @for (c of categories; track c) { <option [value]="c">{{ meta.label('billCategories', c) }}</option> }
                </select>
              </div>
              <div class="form-group" style="grid-column:span 2">
                <label>Description <span class="req">*</span></label>
                <input type="text" [(ngModel)]="item.description" [name]="'desc' + i" />
              </div>
              @if (item.category === 'SERVICE') {
                <div class="form-group">
                  <label>Service Date/Time <span class="req">*</span></label>
                  <input type="datetime-local" [(ngModel)]="item.serviceDateTime" [name]="'sdt' + i" />
                </div>
              }
              <div class="form-group">
                <label>Qty</label>
                <input type="number" [(ngModel)]="item.quantity" [name]="'qty' + i" min="1" />
              </div>
              <div class="form-group">
                <label>Unit Price (₹)</label>
                <input type="number" [(ngModel)]="item.unitPrice" [name]="'up' + i" min="0" step="0.01" />
              </div>
              <div class="form-group">
                <label>Tax %</label>
                <input type="number" [(ngModel)]="item.taxPercent" [name]="'tax' + i" min="0" step="0.01" />
              </div>
              <div class="form-group">
                <label>Discount (₹)</label>
                <input type="number" [(ngModel)]="item.discount" [name]="'disc' + i" min="0" step="0.01" />
              </div>
            </div>
            @if (form.items.length > 1) {
              <button type="button" class="btn btn-sm btn-danger" (click)="removeItem(i)">Remove</button>
            }
          </div>
        }
        <div class="summary-row total"><span>Estimated Total</span><span>{{ estimatedTotal() | inr }}</span></div>
      </div>

      <button class="btn btn-primary btn-lg" [disabled]="loading || f.invalid">
        @if (loading) { <span class="spinner"></span> Saving… } @else { {{ isEdit ? 'Save Changes' : 'Create Bill' }} }
      </button>
    </form>
  `
})
export class AdminBillFormComponent implements OnInit {
  form: any = { customerId: null, paymentStatus: 'PENDING', notes: '', items: [this.blankItem()] };
  customers = signal<any[]>([]);
  categories = ['ROOM', 'SERVICE', 'ADDITIONAL'];
  paymentStatuses = ['PENDING', 'PARTIALLY_PAID', 'PAID', 'REFUNDED'];
  bill = signal<any>(null);
  loading = false;
  message = signal('');
  error = '';
  fieldErrors: Record<string, string> | null = null;
  isEdit = false;
  private id!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, private router: Router, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe();
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = Number(idParam);
      this.api.billEditForm(this.id).subscribe({
        next: res => {
          if (res.customers) this.customers.set(res.customers);
          this.bill.set(res.bill);
          if (res.form) {
            this.form = {
              customerId: res.form.customerId,
              paymentStatus: res.form.paymentStatus || 'PENDING',
              notes: res.form.notes || '',
              items: (res.form.items || []).map((it: any) => ({ ...it, serviceDateTime: it.serviceDateTime || null })),
            };
          }
        },
        error: err => { this.error = err.error?.message || 'Could not load bill.'; }
      });
    } else {
      this.api.adminBillFormOptions().subscribe({
        next: res => { if (res.customers) this.customers.set(res.customers); },
        error: err => { this.error = err.error?.message || 'Could not load form options.'; }
      });
    }
  }

  blankItem(): any {
    return { category: 'SERVICE', description: '', serviceDateTime: null, quantity: 1, unitPrice: null, taxPercent: 0, discount: 0 };
  }

  fieldErrorList(): [string, string][] { return Object.entries(this.fieldErrors || {}); }

  addItem(): void { this.form.items.push(this.blankItem()); }
  removeItem(i: number): void { this.form.items.splice(i, 1); }

  estimatedTotal(): number {
    return this.form.items.reduce((sum: number, it: any) => {
      const up = Number(it.unitPrice) || 0;
      const qty = Number(it.quantity) || 1;
      const tax = Number(it.taxPercent) || 0;
      const disc = Number(it.discount) || 0;
      const line = Math.max(0, up * qty + (up * qty * tax / 100) - disc);
      return sum + line;
    }, 0);
  }

  save(): void {
    this.loading = true;
    this.error = '';
    this.fieldErrors = null;
    const payload = { ...this.form, items: this.form.items.map((it: any) => ({ ...it, serviceDateTime: it.category === 'SERVICE' ? it.serviceDateTime : null })) };
    const obs = this.isEdit ? this.api.updateBill(this.id, payload) : this.api.createBill(payload);
    obs.subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/admin/bills']);
      },
      error: err => {
        this.loading = false;
        if (err.error?.errors) this.fieldErrors = err.error.errors;
        else this.error = err.error?.message || 'Save failed.';
      }
    });
  }
}
