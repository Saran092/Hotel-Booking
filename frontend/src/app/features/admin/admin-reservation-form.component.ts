import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-admin-reservation-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>{{ isEdit ? 'Edit Reservation' : 'New Reservation' }}</h1>
        @if (isEdit && booking(); as b) { <div class="subtitle">{{ b.bookingRef }}</div> }
      </div>
      <a routerLink="/admin/reservations" class="btn btn-secondary">← Back to Reservations</a>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error) { <div class="alert alert-danger">{{ error }}</div> }
    @if (fieldErrorList().length) {
      <div class="alert alert-danger">
        @for (e of fieldErrorList(); track e[0]) { <div><strong>{{ e[0] }}:</strong> {{ e[1] }}</div> }
      </div>
    }

    <div class="card" style="max-width:720px">
      <form (ngSubmit)="save()" #f="ngForm">
        <div class="grid grid-2">
          <div class="form-group">
            <label>Customer Username <span class="req">*</span></label>
            <input type="text" name="customerUsername" [(ngModel)]="form.customerUsername" required />
          </div>
          <div class="form-group">
            <label>Room <span class="req">*</span></label>
            <select name="roomId" [(ngModel)]="form.roomId" required>
              <option [ngValue]="null">Select…</option>
              @for (r of rooms(); track r.id) {
                <option [ngValue]="r.id">#{{ r.roomNumber }} — {{ meta.label('roomTypes', r.roomType) }} (₹{{ r.pricePerNight }}/night)</option>
              }
            </select>
          </div>
          <div class="form-group">
            <label>Check-in <span class="req">*</span></label>
            <input type="date" name="checkIn" [(ngModel)]="form.checkIn" required />
          </div>
          <div class="form-group">
            <label>Check-out <span class="req">*</span></label>
            <input type="date" name="checkOut" [(ngModel)]="form.checkOut" required />
          </div>
          <div class="form-group">
            <label>Adults</label>
            <input type="number" name="adults" [(ngModel)]="form.adults" min="1" max="10" />
          </div>
          <div class="form-group">
            <label>Children</label>
            <input type="number" name="children" [(ngModel)]="form.children" min="0" max="6" />
          </div>
          @if (isEdit) {
            <div class="form-group">
              <label>Status</label>
              <select name="status" [(ngModel)]="form.status">
                @for (s of editableStatuses(); track s) { <option [value]="s">{{ meta.label('bookingStatuses', s) }}</option> }
              </select>
            </div>
          }
        </div>
        <div class="form-group">
          <label>Notes</label>
          <textarea name="notes" [(ngModel)]="form.notes" rows="2" maxlength="500"></textarea>
        </div>
        <button class="btn btn-primary btn-lg" [disabled]="loading || f.invalid">
          @if (loading) { <span class="spinner"></span> Saving… } @else { {{ isEdit ? 'Save Changes' : 'Create Reservation' }} }
        </button>
      </form>
    </div>
  `
})
export class AdminReservationFormComponent implements OnInit {
  form: any = { customerUsername: '', roomId: null, checkIn: '', checkOut: '', adults: 1, children: 0, notes: '', status: 'CONFIRMED' };
  rooms = signal<any[]>([]);
  editableStatuses = signal<string[]>(['CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT']);
  booking = signal<any>(null);
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
      this.api.adminReservationEditForm(this.id).subscribe({
        next: res => {
          if (res.allRooms) this.rooms.set(res.allRooms);
          if (res.editableStatuses) this.editableStatuses.set(res.editableStatuses);
          this.booking.set(res.booking);
          if (res.form) {
            this.form = {
              customerUsername: res.form.customerUsername,
              roomId: res.form.roomId,
              checkIn: res.form.checkIn,
              checkOut: res.form.checkOut,
              adults: res.form.adults,
              children: res.form.children,
              notes: res.form.notes || '',
              status: res.form.status || 'CONFIRMED',
            };
          }
        },
        error: err => { this.error = err.error?.message || 'Could not load reservation.'; }
      });
    } else {
      this.api.adminReservationFormOptions().subscribe({
        next: res => { if (res.allRooms) this.rooms.set(res.allRooms); },
        error: err => { this.error = err.error?.message || 'Could not load form options.'; }
      });
    }
  }

  fieldErrorList(): [string, string][] { return Object.entries(this.fieldErrors || {}); }

  save(): void {
    this.loading = true;
    this.error = '';
    this.fieldErrors = null;
    const obs = this.isEdit ? this.api.updateAdminReservation(this.id, this.form) : this.api.createAdminReservation(this.form);
    obs.subscribe({
      next: (res: any) => {
        this.loading = false;
        this.message.set(res.message || 'Saved.');
        if (!this.isEdit) setTimeout(() => this.router.navigate(['/admin/reservations']), 1200);
      },
      error: err => {
        this.loading = false;
        if (err.error?.errors) this.fieldErrors = err.error.errors;
        else this.error = err.error?.message || 'Save failed.';
      }
    });
  }
}
