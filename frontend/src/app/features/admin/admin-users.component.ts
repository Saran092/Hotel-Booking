import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';
import { UserSummary } from '../../core/models';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <h1>Manage Users</h1>
        <div class="subtitle">{{ users().length }} account(s)</div>
      </div>
      <a routerLink="/admin/users/new" class="btn btn-primary">+ Add User</a>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error) { <div class="alert alert-danger">{{ error }}</div> }
    @if (tempPassword()) {
      <div class="alert alert-warning">
        Temporary password for <strong>{{ tempUsername() }}</strong>: <code>{{ tempPassword() }}</code>
        — share this with the user; they will be required to change it on first login.
      </div>
    }

    <div class="card mb-2">
      <form (ngSubmit)="load()">
        <div class="filter-bar">
          <div class="form-group"><label>Search</label><input type="text" name="q" [(ngModel)]="filter.q" placeholder="Name or username…" /></div>
          <div class="form-group">
            <label>Role</label>
            <select name="role" [(ngModel)]="filter.role">
              <option value="">All</option>
              @for (r of roles; track r) { <option [value]="r">{{ meta.label('allRoles', r) }}</option> }
            </select>
          </div>
          <div class="form-group">
            <label>Status</label>
            <select name="status" [(ngModel)]="filter.status">
              <option value="">All</option>
              <option value="active">Active</option>
              <option value="inactive">Inactive</option>
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
              <th>Username</th><th>Full Name</th><th>Email</th><th>Roles</th>
              <th>Expertise</th><th>Status</th><th></th>
            </tr>
          </thead>
          <tbody>
            @for (u of users(); track u.id) {
              <tr>
                <td><strong>{{ u.username }}</strong></td>
                <td>{{ u.fullName }}</td>
                <td>{{ u.email }}</td>
                <td>
                  @for (r of u.roles; track r) { <span class="badge badge-blue" style="margin-right:4px">{{ meta.label('allRoles', r) }}</span> }
                </td>
                <td>{{ u.expertise ? meta.label('expertises', u.expertise) : '—' }}</td>
                <td><span class="badge {{ u.active ? 'badge-green' : 'badge-red' }}">{{ u.active ? 'Active' : 'Inactive' }}</span></td>
                <td>
                  <div class="flex gap-1 wrap">
                    <a class="btn btn-sm btn-secondary" [routerLink]="['/admin/users', u.id, 'edit']">Edit</a>
                    <button class="btn btn-sm btn-secondary" (click)="resetPassword(u)">Reset PW</button>
                    <button class="btn btn-sm {{ u.active ? 'btn-danger' : 'btn-success' }}" (click)="toggleStatus(u)">
                      {{ u.active ? 'Deactivate' : 'Reactivate' }}
                    </button>
                  </div>
                </td>
              </tr>
            } @empty {
              <tr><td colspan="7" class="text-center text-muted" style="padding:32px">No users found.</td></tr>
            }
          </tbody>
        </table>
      </div>
    }
  `
})
export class AdminUsersComponent implements OnInit {
  users = signal<UserSummary[]>([]);
  roles: string[] = [];
  filter: any = { q: '', role: '', status: '' };
  loading = true;
  message = signal('');
  error = '';
  tempPassword = signal('');
  tempUsername = signal('');

  constructor(private api: ApiService, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => { this.roles = m?.enums?.allRoles || []; });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.api.adminUsers(this.filter).subscribe({
      next: res => { this.users.set(res.users || []); this.loading = false; },
      error: err => { this.loading = false; this.error = err.error?.message || 'Could not load users.'; }
    });
  }

  resetPassword(u: UserSummary): void {
    if (!confirm(`Reset password for ${u.username}?`)) return;
    this.api.resetUserPassword(u.id).subscribe({
      next: res => {
        this.tempPassword.set(res.tempPassword);
        this.tempUsername.set(u.username);
        this.load();
      },
      error: err => { this.error = err.error?.message || 'Reset failed.'; }
    });
  }

  toggleStatus(u: UserSummary): void {
    const action = u.active ? 'deactivate' : 'reactivate';
    if (!confirm(`${action.charAt(0).toUpperCase() + action.slice(1)} ${u.username}?`)) return;
    this.api.setUserStatus(u.id, !u.active).subscribe({
      next: res => { this.message.set(res.message); this.load(); },
      error: err => { this.error = err.error?.message || 'Action failed.'; }
    });
  }
}
