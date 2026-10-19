import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { ComplaintDto } from '../../core/models';
import { FmtDatePipe } from '../../shared/pipes';

@Component({
  selector: 'app-complaint-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FmtDatePipe],
  template: `
    <div class="page-header">
      <div>
        <h1>My Complaints</h1>
        <div class="subtitle">{{ complaints().length }} complaint(s) submitted</div>
      </div>
      <a routerLink="/complaints/new" class="btn btn-primary">+ File a Complaint</a>
    </div>

    @if (loading) { <div class="loading-center"><span class="spinner spinner-lg"></span></div> }

    @if (!loading) {
      @if (complaints().length) {
        <div class="table-wrap card">
          <table>
            <thead>
              <tr><th>Reference</th><th>Category</th><th>Title</th><th>Status</th><th>Priority</th><th>Submitted</th><th></th></tr>
            </thead>
            <tbody>
              @for (c of complaints(); track c.id) {
                <tr>
                  <td><strong>#{{ c.reference }}</strong></td>
                  <td>{{ meta.label('complaintCategories', c.category) }}</td>
                  <td>{{ c.title }}</td>
                  <td><span class="badge {{ statusClass(c.status) }}">{{ meta.label('complaintStatuses', c.status) }}</span></td>
                  <td><span class="badge {{ priorityClass(c.priority) }}">{{ meta.label('complaintPriorities', c.priority) }}</span></td>
                  <td>{{ c.submittedAt | fmtDate }}</td>
                  <td>
                    <a class="btn btn-sm btn-secondary" [routerLink]="['/complaints', c.id]">View</a>
                    @if (c.status === 'OPEN') {
                      <a class="btn btn-sm btn-secondary" [routerLink]="['/complaints', c.id, 'edit']">Edit</a>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      } @else {
        <div class="empty-state card">
          <div class="icon">📋</div>
          <p>You haven't filed any complaints yet.</p>
          <a routerLink="/complaints/new" class="btn btn-primary mt-2">File a Complaint</a>
        </div>
      }
    }
  `
})
export class ComplaintListComponent implements OnInit {
  complaints = signal<ComplaintDto[]>([]);
  loading = true;

  constructor(private api: ApiService, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe();
    this.api.myComplaints().subscribe({
      next: list => { this.complaints.set(list); this.loading = false; },
      error: () => (this.loading = false)
    });
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
