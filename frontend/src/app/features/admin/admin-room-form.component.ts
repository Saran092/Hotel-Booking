import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';

@Component({
  selector: 'app-admin-room-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <h1>{{ isEdit ? 'Edit Room' : 'Add New Room' }}</h1>
        @if (!isEdit && nextNumber()) { <div class="subtitle">Next room number will be {{ nextNumber() }}</div> }
      </div>
      <a routerLink="/admin/rooms" class="btn btn-secondary">← Back to Rooms</a>
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
            <label>Room Type <span class="req">*</span></label>
            <select name="roomType" [(ngModel)]="form.roomType" required>
              <option value="">Select…</option>
              @for (t of roomTypes; track t) { <option [value]="t">{{ meta.label('roomTypes', t) }}</option> }
            </select>
          </div>
          <div class="form-group">
            <label>Bed Type</label>
            <select name="bedType" [(ngModel)]="form.bedType">
              <option value="">Select…</option>
              @for (b of bedTypes; track b) { <option [value]="b">{{ b }}</option> }
            </select>
          </div>
          <div class="form-group">
            <label>Price per Night (₹) <span class="req">*</span></label>
            <input type="number" name="pricePerNight" [(ngModel)]="form.pricePerNight" min="0.01" step="0.01" required />
          </div>
          <div class="form-group">
            <label>Max Occupancy <span class="req">*</span></label>
            <input type="number" name="maxOccupancy" [(ngModel)]="form.maxOccupancy" min="1" max="10" required />
          </div>
          <div class="form-group">
            <label>Status <span class="req">*</span></label>
            <select name="status" [(ngModel)]="form.status" required>
              @for (s of roomStatuses; track s) { <option [value]="s">{{ meta.label('roomStatuses', s) }}</option> }
            </select>
          </div>
        </div>

        <div class="form-group">
          <label>Amenities</label>
          <div class="chip-list">
            @for (a of amenityList; track a) {
              <span class="chip clickable" [class.selected]="form.amenities.includes(a)" (click)="toggleAmenity(a)">{{ a }}</span>
            }
          </div>
        </div>

        <div class="form-group">
          <label>Description</label>
          <textarea name="description" [(ngModel)]="form.description" rows="3" maxlength="500"></textarea>
        </div>

        <button class="btn btn-primary btn-lg" [disabled]="loading || f.invalid">
          @if (loading) { <span class="spinner"></span> Saving… } @else { {{ isEdit ? 'Save Changes' : 'Create Room' }} }
        </button>
      </form>
    </div>
  `
})
export class AdminRoomFormComponent implements OnInit {
  form: any = { roomType: '', bedType: '', pricePerNight: null, maxOccupancy: 2, status: 'AVAILABLE', amenities: [], description: '' };
  roomTypes: string[] = [];
  roomStatuses: string[] = ['AVAILABLE', 'OCCUPIED', 'UNDER_MAINTENANCE'];
  bedTypes: string[] = [];
  amenityList: string[] = [];
  loading = false;
  message = signal('');
  error = '';
  fieldErrors: Record<string, string> | null = null;
  nextNumber = signal('');
  isEdit = false;
  private id!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, private router: Router, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      this.roomTypes = m?.enums?.roomTypes || [];
      this.roomStatuses = m?.enums?.roomStatuses || this.roomStatuses;
      this.bedTypes = m?.lists?.bedTypes || [];
      this.amenityList = m?.lists?.amenityList || [];
    });
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = Number(idParam);
      this.api.adminRooms({}).subscribe({
        next: res => {
          const r = res.rooms.find((x: any) => x.id === this.id);
          if (r) this.form = { roomType: r.roomType, bedType: r.bedType || '', pricePerNight: r.pricePerNight, maxOccupancy: r.maxOccupancy, status: r.status, amenities: [...r.amenities], description: r.description || '' };
        }
      });
    } else {
      this.api.nextRoomNumber().subscribe(res => this.nextNumber.set(res.nextNumber));
    }
  }

  fieldErrorList(): [string, string][] { return Object.entries(this.fieldErrors || {}); }

  toggleAmenity(a: string): void {
    const i = this.form.amenities.indexOf(a);
    if (i >= 0) this.form.amenities.splice(i, 1);
    else this.form.amenities.push(a);
  }

  save(): void {
    this.loading = true;
    this.error = '';
    this.fieldErrors = null;
    const obs = this.isEdit ? this.api.updateRoom(this.id, this.form) : this.api.createRoom(this.form);
    obs.subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/admin/rooms']);
      },
      error: err => {
        this.loading = false;
        if (err.error?.errors) this.fieldErrors = err.error.errors;
        else this.error = err.error?.message || 'Save failed.';
      }
    });
  }
}
