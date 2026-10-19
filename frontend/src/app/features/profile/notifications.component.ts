import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { NotificationDto } from '../../core/models';
import { FmtDatePipe } from '../../shared/pipes';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, RouterLink, FmtDatePipe],
  template: `
    <div class="page-header">
      <div>
        <h1>Notification Inbox</h1>
        <div class="subtitle">Updates about your bookings, complaints and account</div>
      </div>
    </div>

    @if (loading) { <div class="loading-center"><span class="spinner spinner-lg"></span></div> }

    @if (!loading) {
      @if (items().length) {
        <div class="grid" style="gap:8px">
          @for (n of items(); track n.id) {
            <div class="card" style="padding:14px 18px">
              <div class="flex justify-between items-center">
                <div style="white-space:pre-wrap;flex:1">{{ n.message }}</div>
                <span class="text-muted" style="font-size:12px;white-space:nowrap;margin-left:12px">{{ n.createdAt | fmtDate }}</span>
              </div>
            </div>
          }
        </div>
      } @else {
        <div class="empty-state card">
          <div class="icon">📭</div>
          <p>No notifications yet.</p>
        </div>
      }
    }
  `
})
export class NotificationsComponent implements OnInit {
  items = signal<NotificationDto[]>([]);
  loading = true;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.notifications().subscribe({
      next: list => { this.items.set(list); this.loading = false; },
      error: () => (this.loading = false)
    });
  }
}
