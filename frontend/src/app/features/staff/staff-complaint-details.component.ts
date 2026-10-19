import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { MetaService } from '../../core/meta.service';
import { FmtDatePipe } from '../../shared/pipes';

@Component({
  selector: 'app-staff-complaint-details',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, FmtDatePipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Complaint #{{ complaint()?.reference }}</h1>
        @if (complaint(); as c) { <div class="subtitle">{{ c.customer?.fullName }} · {{ meta.label('complaintCategories', c.category) }}</div> }
      </div>
      <a [routerLink]="auth.isAdmin ? '/admin/complaints' : '/staff/complaints'" class="btn btn-secondary">← Back to Complaints</a>
    </div>

    @if (error) { <div class="alert alert-danger">{{ error }}</div> }
    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }

    @if (complaint(); as c) {
      <div class="grid" style="grid-template-columns:1fr 340px; align-items:start">
        <div>
          <div class="card">
            <div class="flex justify-between items-center mb-1">
              <h2>{{ c.title }}</h2>
              <span class="badge {{ priorityClass(c.priority) }}">{{ meta.label('complaintPriorities', c.priority) }}</span>
            </div>
            <p style="white-space:pre-wrap">{{ c.description }}</p>
            <div class="mt-2 text-muted" style="font-size:13px">
              Contact: {{ meta.label('contactPreferences', c.contactPreference) }} ·
              Submitted {{ c.submittedAt | fmtDate }}
              @if (c.bookingRef) { · Booking {{ c.bookingRef }} }
            </div>
          </div>

          @if (c.actions.length) {
            <div class="card mt-2">
              <h2 class="mb-2">Activity Timeline</h2>
              <div class="timeline">
                @for (a of c.actions; track a.id) {
                  <div class="timeline-item">
                    <div class="tl-head">{{ meta.label('actionTypes', a.type) }}</div>
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
            <div class="summary-row"><span>Current</span><span class="badge {{ statusClass(c.status) }}">{{ meta.label('complaintStatuses', c.status) }}</span></div>
            @if (c.assignedTo) {
              <div class="summary-row"><span>Assigned To</span><span>{{ c.assignedTo.fullName }}</span></div>
            } @else {
              <div class="summary-row"><span>Assigned To</span><span class="text-muted">Unassigned</span></div>
            }
            @if (c.expectedResolutionDate) {
              <div class="summary-row"><span>Expected Resolution</span><span>{{ c.expectedResolutionDate }}</span></div>
            }
          </div>

          @if (auth.isAdmin) {
            <div class="card mt-2">
              <h2 class="mb-1">Assign</h2>
              <select [(ngModel)]="selectedStaff">
                <option [ngValue]="null">Select staff…</option>
                @for (s of staffList(); track s.id) { <option [ngValue]="s.id">{{ s.fullName }} ({{ s.expertise }})</option> }
              </select>
              <button class="btn btn-primary mt-1" style="width:100%" (click)="assign()" [disabled]="!selectedStaff || loading">Assign</button>
            </div>
          }

            <div class="card mt-2">
            <h2 class="mb-1">Set Priority</h2>
            <select [(ngModel)]="newPriority">
              @for (p of priorities; track p) { <option [value]="p">{{ meta.label('complaintPriorities', p) }}</option> }
            </select>
            <button class="btn btn-secondary mt-1" style="width:100%" (click)="setPriority()" [disabled]="loading">Update Priority</button>
          </div>

          <div class="card mt-2">
            <h2 class="mb-1">Add Action</h2>
            <div class="form-group">
              <label>Action Type</label>
              <select [(ngModel)]="actionForm.type">
                @for (t of actionTypes; track t) { <option [value]="t">{{ meta.label('actionTypes', t) }}</option> }
              </select>
            </div>
            <div class="form-group">
              <label>Details</label>
              <textarea [(ngModel)]="actionForm.details" rows="3" maxlength="1000"></textarea>
            </div>
            <div class="form-group">
              <label>New Status (optional)</label>
              <select [(ngModel)]="actionForm.newStatus">
                <option [ngValue]="null">— No change —</option>
                @for (s of statuses; track s) { <option [value]="s">{{ meta.label('complaintStatuses', s) }}</option> }
              </select>
            </div>
            <button class="btn btn-primary" style="width:100%" (click)="addAction()" [disabled]="loading || !actionForm.details">
              @if (loading) { <span class="spinner"></span> Saving… } @else { Record Action }
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class StaffComplaintDetailsComponent implements OnInit {
  complaint = signal<any>(null);
  staffList = signal<any[]>([]);
  message = signal('');
  error = '';
  loading = false;
  selectedStaff: number | null = null;
  newPriority = 'MEDIUM';
  actionForm: any = { type: 'STEP', details: '', newStatus: null };
  actionTypes: string[] = ['STEP', 'COMMUNICATION', 'INTERNAL_NOTE', 'INVESTIGATION', 'RESOLUTION', 'STATUS_CHANGE'];
  priorities = ['LOW', 'MEDIUM', 'HIGH'];
  statuses = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'ESCALATED'];
  private id!: number;
  private base: 'staff' | 'admin' = 'staff';

  constructor(private api: ApiService, private route: ActivatedRoute, public auth: AuthService, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe();
    this.id = Number(this.route.snapshot.paramMap.get('id'));
    this.base = this.auth.isAdmin ? 'admin' : 'staff';
    this.load();
  }

  load(): void {
    this.api.manageComplaintDetails(this.base, this.id).subscribe({
      next: res => {
        this.complaint.set(res.complaint);
        if (res.staffList) this.staffList.set(res.staffList);
        this.newPriority = res.complaint?.priority || 'MEDIUM';
      },
      error: err => { this.error = err.error?.message || 'Could not load complaint.'; }
    });
  }

  assign(): void {
    if (!this.selectedStaff) return;
    this.loading = true;
    this.api.assignComplaint(this.id, this.selectedStaff).subscribe({
      next: res => { this.message.set(res.message); this.loading = false; this.load(); },
      error: err => { this.loading = false; this.error = err.error?.message || 'Assign failed.'; }
    });
  }

  setPriority(): void {
    this.loading = true;
    this.api.setComplaintPriority(this.base, this.id, this.newPriority).subscribe({
      next: res => { this.message.set(res.message); this.loading = false; this.load(); },
      error: err => { this.loading = false; this.error = err.error?.message || 'Update failed.'; }
    });
  }

  addAction(): void {
    this.loading = true;
    this.error = '';
    const form = { ...this.actionForm, newStatus: this.actionForm.newStatus || null };
    this.api.addComplaintAction(this.base, this.id, form).subscribe({
      next: () => {
        this.loading = false;
        this.message.set('Action recorded.');
        this.actionForm = { type: 'STEP', details: '', newStatus: null };
        this.load();
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Could not record action.';
      }
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
