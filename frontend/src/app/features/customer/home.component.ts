import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { MetaService } from '../../core/meta.service';
import { PriceQuoteDto, RoomDto } from '../../core/models';
import { InrPipe } from '../../shared/pipes';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, InrPipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Find Your Perfect Room</h1>
        <div class="subtitle">Search available rooms and book instantly</div>
      </div>
    </div>

    <div class="card mb-2">
      <form (ngSubmit)="search()" #f="ngForm">
        <div class="filter-bar">
          <div class="form-group">
            <label>Check-in <span class="req">*</span></label>
            <input type="date" name="checkIn" [(ngModel)]="form.checkIn" required [min]="today" />
          </div>
          <div class="form-group">
            <label>Check-out <span class="req">*</span></label>
            <input type="date" name="checkOut" [(ngModel)]="form.checkOut" required [min]="form.checkIn || today" />
          </div>
          <div class="form-group">
            <label>Room Type <span class="req">*</span></label>
            <select name="roomType" [(ngModel)]="form.roomType" required>
              <option value="">Select…</option>
              @for (t of roomTypes; track t) { <option [value]="t">{{ meta.label('roomTypes', t) }}</option> }
            </select>
          </div>
          <div class="form-group" style="max-width:100px">
            <label>Adults</label>
            <input type="number" name="adults" [(ngModel)]="form.adults" min="1" max="10" />
          </div>
          <div class="form-group" style="max-width:100px">
            <label>Children</label>
            <input type="number" name="children" [(ngModel)]="form.children" min="0" max="6" />
          </div>
          <button class="btn btn-primary" [disabled]="loading">
            @if (loading) { <span class="spinner"></span> Searching… } @else { 🔍 Search Rooms }
          </button>
        </div>
      </form>
    </div>

    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    @if (searched()) {
      <h2 class="mb-1">{{ quotes().length }} room(s) available</h2>
      <div class="grid" style="gap:12px">
        @for (q of quotes(); track q.room.id) {
          <div class="room-card">
            <div class="room-info">
              <h3>Room {{ q.room.roomNumber }} — {{ meta.label('roomTypes', q.room.roomType) }}</h3>
              <div class="text-muted mb-1">
                {{ q.room.bedType || 'Standard bed' }} · Max {{ q.room.maxOccupancy }} guest(s) · {{ q.nights }} night(s)
              </div>
              <div class="chip-list mb-1">
                @for (a of q.room.amenities; track a) { <span class="chip">{{ a }}</span> }
              </div>
              @if (q.room.description) { <div class="text-muted" style="font-size:13px">{{ q.room.description }}</div> }
            </div>
            <div class="room-price">
              <div>
                <div class="price-amount">{{ q.total | inr }}</div>
                <div class="price-per">{{ q.nightlyRate | inr }}/night × {{ q.nights }} night(s)</div>
                <div class="price-per">incl. GST + service</div>
              </div>
              <button class="btn btn-primary mt-1" (click)="book(q.room)">Book Now</button>
            </div>
          </div>
        } @empty {
          <div class="empty-state card">
            <div class="icon">🛏️</div>
            <p>No rooms match your criteria. Try different dates or room type.</p>
          </div>
        }
      </div>
    }
  `
})
export class HomeComponent implements OnInit {
  today = new Date().toISOString().slice(0, 10);
  form: any = { checkIn: '', checkOut: '', roomType: '', adults: 1, children: 0 };
  roomTypes: string[] = [];
  loading = false;
  searched = signal(false);
  quotes = signal<PriceQuoteDto[]>([]);
  error = '';

  constructor(private api: ApiService, private router: Router, public meta: MetaService, public auth: AuthService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      if (m?.enums?.roomTypes) this.roomTypes = m.enums.roomTypes;
    });
  }

  search(): void {
    this.loading = true;
    this.error = '';
    this.api.searchRooms({ ...this.form, roomType: this.form.roomType || null }).subscribe({
      next: res => {
        this.loading = false;
        this.quotes.set(res.quotes || []);
        this.searched.set(true);
      },
      error: err => {
        this.loading = false;
        this.searched.set(true);
        this.quotes.set([]);
        this.error = err.error?.message || 'Search failed. Please try again.';
      }
    });
  }

  book(room: RoomDto): void {
    const req = {
      roomId: room.id,
      checkIn: this.form.checkIn,
      checkOut: this.form.checkOut,
      adults: this.form.adults,
      children: this.form.children,
    };
    // Stash the request so confirm page can re-submit with paymentMethod
    sessionStorage.setItem('bookingRequest', JSON.stringify(req));
    this.router.navigate(['/booking', 0, 'confirm']);
  }
}
