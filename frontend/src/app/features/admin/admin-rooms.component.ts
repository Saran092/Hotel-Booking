import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { MetaService } from '../../core/meta.service';
import { RoomDto } from '../../core/models';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-admin-rooms',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Manage Rooms</h1>
        <div class="subtitle">{{ rooms().length }} room(s) in inventory</div>
      </div>
      <div class="flex gap-1">
        <button class="btn btn-secondary" (click)="showBulk.set(!showBulk())">Bulk Upload</button>
        <a routerLink="/admin/rooms/new" class="btn btn-primary">+ Add Room</a>
      </div>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    @if (showBulk()) {
      <div class="card mb-2">
        <h2 class="mb-1">Bulk CSV Upload</h2>
        <p class="text-muted mb-1" style="font-size:13px">
          Header must be exactly: <code>roomNumber,roomType,bedType,pricePerNight,maxOccupancy,status,amenities,description</code>
        </p>
        <input type="file" accept=".csv" (change)="onFile($event)" />
        <button class="btn btn-primary mt-1" (click)="upload()" [disabled]="!selectedFile || uploading">
          @if (uploading) { <span class="spinner"></span> Uploading… } @else { Upload }
        </button>
        @if (bulkResult(); as r) {
          <div class="alert {{ r.success ? 'alert-success' : 'alert-danger' }} mt-1">{{ r.message }}</div>
          @if (r.errors?.length) {
            <ul class="text-danger" style="font-size:13px">
              @for (e of r.errors; track e) { <li>{{ e }}</li> }
            </ul>
          }
        }
      </div>
    }

    <div class="card mb-2">
      <form (ngSubmit)="load()">
        <div class="filter-bar">
          <div class="form-group"><label>Search</label><input type="text" name="q" [(ngModel)]="filter.q" placeholder="Room number…" /></div>
          <div class="form-group">
            <label>Type</label>
            <select name="roomType" [(ngModel)]="filter.roomType">
              <option value="">All</option>
              @for (t of roomTypes; track t) { <option [value]="t">{{ meta.label('roomTypes', t) }}</option> }
            </select>
          </div>
          <div class="form-group"><label>Min Price</label><input type="number" name="minPrice" [(ngModel)]="filter.minPrice" /></div>
          <div class="form-group"><label>Max Price</label><input type="number" name="maxPrice" [(ngModel)]="filter.maxPrice" /></div>
          <div class="form-group">
            <label>Availability</label>
            <select name="availability" [(ngModel)]="filter.availability">
              <option value="">All</option>
              <option value="available">Available Now</option>
              <option value="booked">Booked</option>
            </select>
          </div>
          <div class="form-group">
            <label>Amenity</label>
            <select name="amenity" [(ngModel)]="filter.amenity">
              <option value="">All</option>
              @for (a of amenityList; track a) { <option [value]="a">{{ a }}</option> }
            </select>
          </div>
          <button class="btn btn-primary" [disabled]="loading">Filter</button>
        </div>
      </form>
    </div>

    @if (loading) { <div class="loading-center"><span class="spinner spinner-lg"></span></div> }

    @if (!loading) {
      <div class="table-wrap card">
        <table>
          <thead>
            <tr>
              <th>Room</th><th>Type</th><th>Bed</th><th class="num">Price/Night</th>
              <th>Max Occ.</th><th>Amenities</th><th>Status</th><th>Available Now</th><th></th>
            </tr>
          </thead>
          <tbody>
            @for (r of rooms(); track r.id) {
              <tr>
                <td><strong>{{ r.roomNumber }}</strong></td>
                <td>{{ meta.label('roomTypes', r.roomType) }}</td>
                <td>{{ r.bedType || '—' }}</td>
                <td class="num">{{ r.pricePerNight | inr }}</td>
                <td>{{ r.maxOccupancy }}</td>
                <td>
                  <div class="chip-list">
                    @for (a of r.amenities.slice(0,3); track a) { <span class="chip">{{ a }}</span> }
                    @if (r.amenities.length > 3) { <span class="chip">+{{ r.amenities.length - 3 }}</span> }
                  </div>
                </td>
                <td><span class="badge {{ roomStatusClass(r.status) }}">{{ meta.label('roomStatuses', r.status) }}</span></td>
                <td>
                  @if (availableNow()[r.id]) { <span class="badge badge-green">Yes</span> } @else { <span class="badge badge-gray">No</span> }
                </td>
                <td class="flex gap-1">
                  <a class="btn btn-sm btn-secondary" [routerLink]="['/admin/rooms', r.id, 'edit']">Edit</a>
                  <button class="btn btn-sm btn-danger" (click)="remove(r)">Delete</button>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="9" class="text-center text-muted" style="padding:32px">No rooms found.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `
})
export class AdminRoomsComponent implements OnInit {
  rooms = signal<RoomDto[]>([]);
  availableNow = signal<Record<number, boolean>>({});
  roomTypes: string[] = [];
  amenityList: string[] = [];
  filter: any = { q: '', roomType: '', minPrice: null, maxPrice: null, availability: '', amenity: '' };
  loading = true;
  message = signal('');
  error = '';
  showBulk = signal(false);
  selectedFile: File | null = null;
  uploading = false;
  bulkResult = signal<any>(null);

  constructor(private api: ApiService, public auth: AuthService, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      this.roomTypes = m?.enums?.roomTypes || [];
      this.amenityList = m?.lists?.amenityList || [];
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.api.adminRooms(this.filter).subscribe({
      next: res => {
        this.rooms.set(res.rooms || []);
        this.availableNow.set(res.availableNow || {});
        this.loading = false;
      },
      error: err => { this.loading = false; this.error = err.error?.message || 'Could not load rooms.'; }
    });
  }

  remove(r: RoomDto): void {
    if (!confirm(`Delete room ${r.roomNumber}? This cannot be undone.`)) return;
    this.api.deleteRoom(r.id).subscribe({
      next: res => { this.message.set(res.message); this.load(); },
      error: err => { this.error = err.error?.message || 'Delete failed.'; }
    });
  }

  onFile(e: Event): void {
    const input = e.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] || null;
    this.bulkResult.set(null);
  }

  upload(): void {
    if (!this.selectedFile) return;
    this.uploading = true;
    this.bulkResult.set(null);
    this.api.bulkUpload(this.selectedFile).subscribe({
      next: res => { this.uploading = false; this.bulkResult.set(res); this.load(); },
      error: err => { this.uploading = false; this.bulkResult.set({ success: false, message: err.error?.message || 'Upload failed.', errors: [] }); }
    });
  }

  roomStatusClass(s: string): string {
    return s === 'AVAILABLE' ? 'badge-green' : s === 'OCCUPIED' ? 'badge-yellow' : 'badge-red';
  }
}
