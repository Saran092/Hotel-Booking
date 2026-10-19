import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../core/auth.service';
import { ApiService } from '../core/api.service';
import { MetaService } from '../core/meta.service';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  template: `
    <header class="topbar">
      <a routerLink="/" class="brand">
        <span>🏨</span>
        <span>{{ metaService.getCache()?.hotel?.name || 'Hotel Grand' }}</span>
      </a>

      <nav class="nav-links">
        @if (auth.isCustomer) {
          <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Book a Room</a>
          <a routerLink="/bookings" routerLinkActive="active">My Bookings</a>
          <a routerLink="/complaints" routerLinkActive="active">Complaints</a>
        }
        @if (auth.isStaff && !auth.isAdmin) {
          <a routerLink="/staff/complaints" routerLinkActive="active">Complaints</a>
        }
        @if (auth.isAdmin) {
          <a routerLink="/admin" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">Dashboard</a>
          <a routerLink="/admin/rooms" routerLinkActive="active">Rooms</a>
          <a routerLink="/admin/reservations" routerLinkActive="active">Reservations</a>
          <a routerLink="/admin/users" routerLinkActive="active">Users</a>
          <a routerLink="/admin/bills" routerLinkActive="active">Bills</a>
          <a routerLink="/admin/complaints" routerLinkActive="active">Complaints</a>
        }
        <a routerLink="/notifications" routerLinkActive="active">Inbox@if(unreadCount() > 0){ ({{ unreadCount() }})}</a>
      </nav>

      <div class="user-menu">
        <button class="user-menu-btn" (click)="menuOpen.set(!menuOpen())">
          <span>👤</span>
          <span>{{ auth.user()?.fullName || auth.user()?.username }}</span>
          <span style="font-size:10px">▾</span>
        </button>
        @if (menuOpen()) {
          <div class="user-dropdown">
            <div class="dropdown-header">{{ auth.user()?.roles?.join(', ') }}</div>
            <a routerLink="/profile" (click)="menuOpen.set(false)">My Profile</a>
            <a routerLink="/notifications" (click)="menuOpen.set(false)">Notification Inbox</a>
            @if (auth.mustChangePassword) {
              <a routerLink="/change-password" (click)="menuOpen.set(false)">Change Password</a>
            }
            <div class="divider"></div>
            <button (click)="logout()">Sign Out</button>
          </div>
        }
      </div>
    </header>

    <main class="main-content">
      <router-outlet />
    </main>
  `,
  styles: [`
    @media (max-width: 768px) {
      .topbar { flex-wrap: wrap; height: auto; padding: 8px 12px; gap: 8px; }
      .nav-links { width: 100%; overflow-x: auto; }
    }
  `]
})
export class LayoutComponent implements OnInit {
  menuOpen = signal(false);
  unreadCount = signal(0);

  constructor(public auth: AuthService, public metaService: MetaService, private api: ApiService) {}

  ngOnInit(): void {
    this.metaService.get().subscribe();
    this.api.unreadCount().subscribe({
      next: r => this.unreadCount.set(r.count || 0),
      error: () => {}
    });
    // click-outside to close menu
    if (typeof document !== 'undefined') {
      document.addEventListener('click', (e) => {
        const t = e.target as HTMLElement;
        if (!t.closest('.user-menu')) this.menuOpen.set(false);
      });
    }
  }

  logout(): void { this.auth.logout(); }
}
