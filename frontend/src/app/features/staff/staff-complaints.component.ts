import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { MetaService } from '../../core/meta.service';
import { FmtDatePipe } from '../../shared/pipes';

@Component({
  selector: 'app-staff-complaints',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, FmtDatePipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Manage Complaints</h1>
        <div class="subtitle">{{ complaints().length }} complaint(s) {{ auth.isAdmin ? 'across the hotel' : 'assigned to you' }}</div>
      </div>
    </div>

    <div class="card mb-2">
      <form (ngSubmit)="load()">
        <div class="filter-bar">
          <div class="form-group">
            <label>Date From</label>
            <input type="date" name="dateFrom" [(ngModel)]="filter.dateFrom" />
          </div>
          <div class="form-group">
            <label>Date To</label>
            <input type="date" name="dateTo" [(ngModel)]="filter.dateTo" />
          </div>
          <div class="form-group">
            <label>Category</label>
            <select name="category" [(ngModel)]="filter.category">
              <option value="">All</option>
              @for (c of categories; track c) { <option [value]="c">{{ meta.label('complaintCategories', c) }}</option> }
            </select>
          </div>
          <div class="form-group">
            <label>Status</label>
            <select name="status" [(ngModel)]="filter.status">
              <option value="">All</option>
              @for (s of statuses; track s) { <option [value]="s">{{ meta.label('complaintStatuses', s) }}</option> }
            </select>
          </div>
          <div class="form-group">
            <label>Customer</label>
            <input type="text" name="customer" [(ngModel)]="filter.customer" placeholder="Name…" />
          </div>
          @if (auth.isAdmin) {
            <div class="form-group">
              <label>Assigned Staff</label>
              <select name="assignedTo" [(ngModel)]="filter.assignedTo">
                <option value="">All</option>
                @for (s of staffList(); track s.id) { <option [ngValue]="s.id">{{ s.fullName }}</option> }
              </select>
            </div>
          }
          <button class="btn btn-primary" [disabled]="loading">Filter</button>
          <button type="button" class="btn btn-secondary" (click)="reset()">Reset</button>
        </div>
      </form>
    </div>

    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    @if (loading) { <div class="loading-center"><span class="spinner spinner-lg"></span></div> }

    @if (!loading) {
      <div class="table-wrap card">
        <table>
          <thead>
            <tr>
              <th>Reference</th>
              <th>Customer</th>
              <th>Category</th>
              <th>Title</th>
              <th>Status</th>
              <th>Priority</th>
              @if (auth.isAdmin) { <th>Assigned</th> }
              <th>Submitted</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            @for (c of complaints(); track c.id) {
              <tr>
                <td><strong>#{{ c.reference }}</strong></td>
                <td>{{ c.customer?.fullName }}</td>
                <td>{{ meta.label('complaintCategories', c.category) }}</td>
                <td>{{ c.title }}</td>
                <td><span class="badge {{ statusClass(c.status) }}">{{ meta.label('complaintStatuses', c.status) }}</span></td>
                <td><span class="badge {{ priorityClass(c.priority) }}">{{ meta.label('complaintPriorities', c.priority) }}</span></td>
                @if (auth.isAdmin) { <td>{{ c.assignedTo?.fullName || '—' }}</td> }
                <td>{{ c.submittedAt | fmtDate }}</td>
                <td><a class="btn btn-sm btn-primary" [routerLink]="['/staff/complaints', c.id]">Handle</a></td>
              </tr>
            } @empty {
              <tr><td colspan="9" class="text-center text-muted" style="padding:32px">No complaints match your filters.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `
})
export class StaffComplaintsComponent implements OnInit {
  complaints = signal<any[]>([]);
  staffList = signal<any[]>([]);
  categories: string[] = [];
  statuses: string[] = [];
  filter: any = { dateFrom: '', dateTo: '', category: '', status: '', customer: '', assignedTo: null };
  loading = true;
  error = '';

  constructor(private api: ApiService, public auth: AuthService, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      this.categories = m?.enums?.complaintCategories || [];
      this.statuses = m?.enums?.complaintStatuses || [];
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    const base = this.auth.isAdmin ? 'admin' : 'staff';
    this.api.manageComplaints(base, this.filter).subscribe({
      next: res => {
        this.complaints.set(res.complaints || []);
        if (res.staffList) this.staffList.set(res.staffList);
        this.loading = false;
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Could not load complaints.';
      }
    });
  }

  reset(): void {
    this.filter = { dateFrom: '', dateTo: '', category: '', status: '', customer: '', assignedTo: null };
    this.load();
  }

  statusClass(s: string): string {
    switch (s) {
      case 'OPEN': return 'badge-blue';
      case 'IN_PROGRESS': return 'badge-yellow';
      case 'ESCALATED': return 'badge-red';
      case 'RESOLVED': return 'badge-green';
      case 'CLOSED': return 'badge-gray';
      default: return 'badge-gray';
    }
  }

  priorityClass(p: string): string {
    switch (p) {
      case 'HIGH': return 'badge-red';
      case 'MEDIUM': return 'badge-yellow';
      case 'LOW': return 'badge-gray';
      default: return 'badge-gray';
    }
  }
}
