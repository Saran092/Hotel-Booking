import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Admin Dashboard</h1>
        <div class="subtitle">Hotel overview and key metrics</div>
      </div>
      <a routerLink="/admin/reports" class="btn btn-secondary">Detailed Reports →</a>
    </div>

    @if (loading) { <div class="loading-center"><span class="spinner spinner-lg"></span></div> }

    @if (stats(); as s) {
      <div class="grid grid-4 mb-2">
        <div class="stat-card">
          <div class="label">Bookings Today</div>
          <div class="value">{{ s.today }}</div>
          <div class="sub">{{ s.week }} this week · {{ s.month }} this month</div>
        </div>
        <div class="stat-card">
          <div class="label">Total Rooms</div>
          <div class="value">{{ s.totalRooms }}</div>
          <div class="sub">{{ s.availableRooms }} currently available</div>
        </div>
        <div class="stat-card">
          <div class="label">Open Complaints</div>
          <div class="value">{{ s.openComplaints }}</div>
        </div>
        <div class="stat-card">
          <div class="label">Pending Bills</div>
          <div class="value">{{ s.pendingBills }}</div>
        </div>
      </div>

      <div class="grid" style="grid-template-columns:1fr 1fr;align-items:start">
        <div class="card">
          <h2 class="mb-2">Bookings (last 7 days)</h2>
          @for (entry of entries(s.daily); track entry[0]) {
            <div class="bar-row">
              <span class="bar-label">{{ entry[0] }}</span>
              <div class="bar-track"><div class="bar-fill" [style.width.%]="pct(entry[1], s.maxDaily)"></div></div>
              <span class="bar-value">{{ entry[1] }}</span>
            </div>
          }
        </div>
        <div class="card">
          <h2 class="mb-2">Bookings by Status</h2>
          @for (entry of entries(s.byStatus); track entry[0]) {
            <div class="bar-row">
              <span class="bar-label">{{ entry[0] }}</span>
              <div class="bar-track"><div class="bar-fill {{ statusColor(entry[0]) }}" [style.width.%]="pct(entry[1], s.maxStatus)"></div></div>
              <span class="bar-value">{{ entry[1] }}</span>
            </div>
          }
        </div>
      </div>

      <div class="grid grid-3 mt-2">
        <a routerLink="/admin/reservations" class="card text-center" style="text-decoration:none">
          <div style="font-size:28px">📅</div>
          <h3 class="mt-1">Reservations</h3>
          <div class="text-muted" style="font-size:13px">View, add, edit and cancel</div>
        </a>
        <a routerLink="/admin/bills" class="card text-center" style="text-decoration:none">
          <div style="font-size:28px">🧾</div>
          <h3 class="mt-1">Bills</h3>
          <div class="text-muted" style="font-size:13px">Create and manage guest bills</div>
        </a>
        <a routerLink="/admin/users" class="card text-center" style="text-decoration:none">
          <div style="font-size:28px">👥</div>
          <h3 class="mt-1">Users</h3>
          <div class="text-muted" style="font-size:13px">Manage accounts and roles</div>
        </a>
      </div>
    }
  `
})
export class AdminDashboardComponent implements OnInit {
  stats = signal<any>(null);
  loading = true;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.adminHome().subscribe({
      next: res => { this.stats.set(res.stats); this.loading = false; },
      error: () => (this.loading = false)
    });
  }

  entries(obj: any): [string, any][] { return Object.entries(obj || {}); }
  pct(val: number, max: number): number { return max ? Math.min(100, (val / max) * 100) : 0; }
  statusColor(label: string): string {
    if (label.includes('Confirm')) return 'green';
    if (label.includes('Pending')) return 'yellow';
    if (label.includes('Cancel')) return 'red';
    if (label.includes('Check-in')) return 'cyan';
    return '';
  }
}
