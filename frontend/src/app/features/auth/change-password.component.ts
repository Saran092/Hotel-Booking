import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="brand">🏨 Hotel Grand</div>
        <div class="tagline">
          @if (auth.mustChangePassword) {
            Your account password must be changed before you can continue.
          } @else {
            Change your password
          }
        </div>

        @if (message) { <div class="alert alert-success">{{ message }}</div> }
        @if (error) { <div class="alert alert-danger">{{ error }}</div> }

        <form (ngSubmit)="submit()" #f="ngForm">
          <div class="form-group">
            <label>New Password <span class="req">*</span></label>
            <input type="password" name="newPassword" [(ngModel)]="form.newPassword" required autocomplete="new-password" />
            <div class="form-hint">Min 8 chars, upper + lower + digit + special</div>
          </div>
          <div class="form-group">
            <label>Confirm Password <span class="req">*</span></label>
            <input type="password" name="confirmPassword" [(ngModel)]="form.confirmPassword" required />
          </div>
          <button class="btn btn-primary btn-lg" style="width:100%" [disabled]="loading || f.invalid">
            @if (loading) { <span class="spinner"></span> Changing… } @else { Change Password }
          </button>
        </form>
      </div>
    </div>
  `
})
export class ChangePasswordComponent {
  form = { newPassword: '', confirmPassword: '' };
  loading = false;
  message = '';
  error = '';

  constructor(public auth: AuthService, private router: Router) {}

  submit(): void {
    this.loading = true;
    this.error = '';
    this.message = '';
    this.auth.changePassword(this.form).subscribe({
      next: async res => {
        this.message = res.message || 'Password changed.';
        // refresh user to clear mustChangePassword flag
        await this.auth.refreshMe().toPromise();
        this.loading = false;
        setTimeout(() => this.router.navigateByUrl(this.auth.homeFor()), 1200);
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Password change failed. Please try again.';
      }
    });
  }
}
