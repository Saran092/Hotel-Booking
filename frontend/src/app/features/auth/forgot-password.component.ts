import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="brand">🏨 Hotel Grand</div>
        <div class="tagline">Reset your password</div>

        @if (message) { <div class="alert alert-success">{{ message }}</div> }
        @if (error) { <div class="alert alert-danger">{{ error }}</div> }

        <form (ngSubmit)="submit()" #f="ngForm">
          <div class="form-group">
            <label>Username <span class="req">*</span></label>
            <input type="text" name="username" [(ngModel)]="form.username" required />
          </div>
          <div class="form-group">
            <label>Email <span class="req">*</span></label>
            <input type="email" name="email" [(ngModel)]="form.email" required />
          </div>
          <div class="form-group">
            <label>New Password <span class="req">*</span></label>
            <input type="password" name="newPassword" [(ngModel)]="form.newPassword" required />
            <div class="form-hint">Min 8 chars, upper + lower + digit + special</div>
          </div>
          <div class="form-group">
            <label>Confirm Password <span class="req">*</span></label>
            <input type="password" name="confirmPassword" [(ngModel)]="form.confirmPassword" required />
          </div>
          <button class="btn btn-primary btn-lg" style="width:100%" [disabled]="loading || f.invalid">
            @if (loading) { <span class="spinner"></span> Resetting… } @else { Reset Password }
          </button>
        </form>

        <div class="auth-footer">
          <a routerLink="/login">Back to sign in</a>
        </div>
      </div>
    </div>
  `
})
export class ForgotPasswordComponent {
  form = { username: '', email: '', newPassword: '', confirmPassword: '' };
  loading = false;
  message = '';
  error = '';

  constructor(private auth: AuthService, private router: Router) {}

  submit(): void {
    this.loading = true;
    this.error = '';
    this.message = '';
    this.auth.forgotPassword(this.form).subscribe({
      next: res => {
        this.loading = false;
        this.message = res.message || 'Password reset. Please sign in with your new password.';
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Password reset failed. Please try again.';
      }
    });
  }
}
