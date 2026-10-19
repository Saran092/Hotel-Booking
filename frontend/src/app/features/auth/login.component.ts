import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card">
        <div class="brand">🏨 Hotel Grand</div>
        <div class="tagline">Sign in to book rooms and manage your stays</div>

        @if (error) { <div class="alert alert-danger">{{ error }}</div> }

        <form (ngSubmit)="submit()" #f="ngForm">
          <div class="form-group">
            <label>Username <span class="req">*</span></label>
            <input type="text" name="username" [(ngModel)]="username" required autocomplete="username" />
          </div>
          <div class="form-group">
            <label>Password <span class="req">*</span></label>
            <input type="password" name="password" [(ngModel)]="password" required autocomplete="current-password" />
          </div>
          <button class="btn btn-primary btn-lg" style="width:100%" [disabled]="loading || f.invalid">
            @if (loading) { <span class="spinner"></span> Signing in… } @else { Sign In }
          </button>
        </form>

        <div class="auth-footer">
          <a routerLink="/forgot-password">Forgot password?</a>
          <div class="mt-1">New here? <a routerLink="/register">Create an account</a></div>
        </div>
      </div>
    </div>
  `
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;
  error = '';

  constructor(private auth: AuthService, private router: Router) {}

  submit(): void {
    this.loading = true;
    this.error = '';
    this.auth.login({ username: this.username.trim(), password: this.password }).subscribe({
      next: res => {
        this.loading = false;
        this.router.navigateByUrl(this.auth.homeFor(res.user));
      },
      error: err => {
        this.loading = false;
        this.error = err.error?.message || 'Sign in failed. Please try again.';
      }
    });
  }
}
