import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { FmtDatePipe } from '../../shared/pipes';

@Component({
  selector: 'app-complaint-details',
  standalone: true,
  imports: [CommonModule, RouterLink, FmtDatePipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Complaint #{{ complaint()?.reference }}</h1>
        @if (complaint(); as c) { <div class="subtitle">{{ meta.label('complaintCategories', c.category) }} · {{ meta.label('complaintStatuses', c.status) }}</div> }
      </div>
      <a routerLink="/complaints" class="btn btn-secondary">← Back to Complaints</a>
    </div>

    @if (error) { <div class="alert alert-danger">{{ error }}</div> }
    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }

    @if (complaint(); as c) {
      <div class="grid" style="grid-template-columns:1fr 320px; align-items:start">
        <div>
          <div class="card">
            <h2 class="mb-1">{{ c.title }}</h2>
            <p style="white-space:pre-wrap">{{ c.description }}</p>
            <div class="mt-2 text-muted" style="font-size:13px">
              Submitted {{ c.submittedAt | fmtDate }}
              @if (c.bookingRef) { · Booking <a [routerLink]="['/booking', c.bookingId]">{{ c.bookingRef }}</a> }
            </div>
          </div>

          @if (c.actions.length) {
            <div class="card mt-2">
              <h2 class="mb-2">Activity Timeline</h2>
              <div class="timeline">
                @for (a of c.actions; track a.id) {
                  <div class="timeline-item">
                    <div class="tl-head">{{ a.type === 'STATUS_CHANGE' ? 'Status Change' : meta.label('actionTypes', a.type) }}</div>
                    <div class="tl-meta">{{ a.actor || 'System' }} · {{ a.createdAt | fmtDate }}</div>
                    <div class="tl-body" style="white-space:pre-wrap">{{ a.details }}</div>
                  </div>
                }
              </div>
            </div>
          }
        </div>

        <div>
          <div class="card">
            <h2 class="mb-1">Status</h2>
            <div class="summary-row"><span>Status</span><span class="badge {{ statusClass(c.status) }}">{{ meta.label('complaintStatuses', c.status) }}</span></div>
            <div class="summary-row"><span>Priority</span><span class="badge {{ priorityClass(c.priority) }}">{{ meta.label('complaintPriorities', c.priority) }}</span></div>
            @if (c.assignedTo) {
              <div class="summary-row"><span>Assigned To</span><span>{{ c.assignedTo.fullName }}</span></div>
            }
            @if (c.expectedResolutionDate) {
              <div class="summary-row"><span>Expected Resolution</span><span>{{ c.expectedResolutionDate }}</span></div>
            }
            @if (c.resolutionNotes) {
              <div class="mt-2">
                <div class="text-muted" style="font-size:12px">Resolution Notes</div>
                <p style="white-space:pre-wrap">{{ c.resolutionNotes }}</p>
              </div>
            }
          </div>

          <div class="card mt-2">
            @if (c.status === 'RESOLVED') {
              <h2 class="mb-1">Resolution</h2>
              <p class="text-muted mb-1" style="font-size:13px">Are you satisfied with the resolution?</p>
              <button class="btn btn-success" style="width:100%" (click)="confirm()">Yes, Close This Complaint</button>
            } @else if (c.status === 'CLOSED') {
              <h2 class="mb-1">Not satisfied?</h2>
              <p class="text-muted mb-1" style="font-size:13px">Reopen this complaint if the issue was not fully resolved.</p>
              <button class="btn btn-secondary" style="width:100%" (click)="reopen()">Reopen Complaint</button>
            } @else if (c.status === 'OPEN') {
              <a class="btn btn-secondary" style="width:100%" [routerLink]="['/complaints', c.id, 'edit']">Edit Complaint</a>
            }
          </div>
        </div>
      </div>
    }
  `
})
export class ComplaintDetailsComponent implements OnInit {
  complaint = signal<any>(null);
  message = signal('');
  error = '';
  private id!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe();
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.load();
  }

  load(): void {
    this.api.complaintDetails(this.id).subscribe({
      next: c => this.complaint.set(c),
      error: err => { this.error = err.error?.message || 'Could not load complaint.'; }
    });
  }

  confirm(): void {
    this.api.complaintConfirm(this.id).subscribe({
      next: res => { this.message.set(res.message); this.load(); },
      error: err => { this.error = err.error?.message || 'Action failed.'; }
    });
  }

  reopen(): void {
    this.api.complaintReopen(this.id).subscribe({
      next: res => { this.message.set(res.message); this.load(); },
      error: err => { this.error = err.error?.message || 'Action failed.'; }
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
