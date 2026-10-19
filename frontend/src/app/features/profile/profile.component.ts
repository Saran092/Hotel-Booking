import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { MetaService } from '../../core/meta.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="page-header">
      <div>
        <h1>My Profile</h1>
        <div class="subtitle">Manage your account details</div>
      </div>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error) { <div class="alert alert-danger">{{ error }}</div> }

    <div class="grid" style="grid-template-columns:1fr 320px; align-items:start">
      <div class="card">
        <h2 class="mb-1">Personal Details</h2>
        <form (ngSubmit)="save()" #f="ngForm">
          <div class="grid grid-2">
            <div class="form-group">
              <label>Full Name <span class="req">*</span></label>
              <input type="text" name="fullName" [(ngModel)]="form.fullName" required />
            </div>
            <div class="form-group">
              <label>Email <span class="req">*</span></label>
              <input type="email" name="email" [(ngModel)]="form.email" required />
            </div>
            <div class="form-group">
              <label>Country Code <span class="req">*</span></label>
              <select name="countryCode" [(ngModel)]="form.countryCode" required>
                @for (c of countryCodes; track c) { <option [value]="c">{{ c }}</option> }
              </select>
            </div>
            <div class="form-group">
              <label>Mobile <span class="req">*</span></label>
              <input type="tel" name="mobile" [(ngModel)]="form.mobile" required />
            </div>
          </div>
          <div class="form-group">
            <label>Address</label>
            <textarea name="address" [(ngModel)]="form.address" rows="2" maxlength="100"></textarea>
          </div>
          <button class="btn btn-primary" [disabled]="loading || f.invalid">
            @if (loading) { <span class="spinner"></span> Saving… } @else { Save Changes }
          </button>
        </form>
      </div>

      <div>
        <div class="card">
          <h2 class="mb-1">Account</h2>
          @if (auth.user(); as u) {
            <div class="summary-row"><span>Username</span><span>{{ u.username }}</span></div>
            <div class="summary-row"><span>Roles</span><span>{{ u.roles?.join(', ') }}</span></div>
            @if (u.expertise) { <div class="summary-row"><span>Expertise</span><span>{{ meta.label('expertises', u.expertise) }}</span></div> }
            <div class="summary-row"><span>Status</span><span class="badge {{ u.active ? 'badge-green' : 'badge-red' }}">{{ u.active ? 'Active' : 'Inactive' }}</span></div>
          }
        </div>
        <div class="card mt-2">
          <a class="btn btn-secondary" style="width:100%" routerLink="/change-password">Change Password</a>
        </div>
      </div>
    </div>
  `
})
export class ProfileComponent implements OnInit {
  form: any = { fullName: '', email: '', countryCode: '+91', mobile: '', address: '' };
  countryCodes: string[] = ['+91', '+1', '+44', '+61', '+49'];
  loading = false;
  message = signal('');
  error = '';

  constructor(private api: ApiService, public auth: AuthService, public meta: MetaService, private router: Router) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      if (m?.lists?.countryCodes?.length) this.countryCodes = m.lists.countryCodes;
    });
    const u = this.auth.user();
    if (u) {
      this.form = {
        fullName: u.fullName, email: u.email,
        countryCode: u.countryCode, mobile: u.mobile, address: u.address || ''
      };
    }
  }

  save(): void {
    this.loading = true;
    this.error = '';
    this.message.set('');
    this.api.updateProfile(this.form).subscribe({
      next: res => {
        this.loading = false;
        this.message.set(res.message || 'Profile updated.');
        if (res.user) {
          localStorage.setItem('hotel_user', JSON.stringify(res.user));
          this.auth.user.set(res.user);
        }
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Could not save profile.';
      }
    });
  }
}
