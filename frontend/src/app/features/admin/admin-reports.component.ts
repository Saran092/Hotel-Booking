import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-admin-reports',
  standalone: true,
  imports: [CommonModule, RouterLink, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Reports</h1>
        <div class="subtitle">Revenue and complaint analytics</div>
      </div>
      <a routerLink="/admin" class="btn btn-secondary">← Dashboard</a>
    </div>

    @if (loading) { <div class="loading-center"><span class="spinner spinner-lg"></span></div> }

    @if (stats(); as s) {
      <div class="card mb-2">
        <h2 class="mb-2">Revenue by Room Type</h2>
        @for (entry of entries(s.revenue); track entry[0]) {
          <div class="bar-row">
            <span class="bar-label">{{ entry[0] }}</span>
            <div class="bar-track"><div class="bar-fill green" [style.width.%]="pctNum(entry[1], s.maxRevenue)"></div></div>
            <span class="bar-value" style="width:90px">{{ entry[1] | inr }}</span>
          </div>
        }
      </div>

      <div class="card">
        <h2 class="mb-2">Complaints by Status</h2>
        @for (entry of entries(s.complaintsByStatus); track entry[0]) {
          <div class="bar-row">
            <span class="bar-label">{{ entry[0] }}</span>
            <div class="bar-track"><div class="bar-fill {{ statusColor(entry[0]) }}" [style.width.%]="pct(entry[1], s.maxComplaints)"></div></div>
            <span class="bar-value">{{ entry[1] }}</span>
          </div>
        }
      </div>
    }
  `
})
export class AdminReportsComponent implements OnInit {
  stats = signal<any>(null);
  loading = true;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.adminReports().subscribe({
      next: res => { this.stats.set(res.stats); this.loading = false; },
      error: () => (this.loading = false)
    });
  }

  entries(obj: any): [string, any][] { return Object.entries(obj || {}); }
  pct(val: number, max: number): number { return max ? Math.min(100, (val / max) * 100) : 0; }
  pctNum(val: any, max: any): number {
    const v = Number(val); const m = Number(max);
    return m ? Math.min(100, (v / m) * 100) : 0;
  }
  statusColor(label: string): string {
    if (label.includes('Open')) return 'cyan';
    if (label.includes('Progress')) return 'yellow';
    if (label.includes('Escalated')) return 'red';
    if (label.includes('Resolved')) return 'green';
    return '';
  }
}
