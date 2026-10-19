import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-page">
      <div class="auth-card" style="max-width:520px">
        <div class="brand">🏨 Hotel Grand</div>
        <div class="tagline">Create your account</div>

        @if (error) { <div class="alert alert-danger">{{ error }}</div> }
        @if (fieldErrors) {
          <div class="alert alert-danger">
            @for (e of fieldErrorList(); track e[0]) { <div><strong>{{ e[0] }}:</strong> {{ e[1] }}</div> }
          </div>
        }

        <form (ngSubmit)="submit()" #f="ngForm">
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
            <label>Address <span class="req">*</span></label>
            <textarea name="address" [(ngModel)]="form.address" required rows="2"></textarea>
          </div>
          <div class="grid grid-2">
            <div class="form-group">
              <label>Username <span class="req">*</span></label>
              <input type="text" name="username" [(ngModel)]="form.username" required autocomplete="username" />
              <div class="form-hint">3-20 letters, digits, dot or underscore</div>
            </div>
            <div class="form-group">
              <label>Password <span class="req">*</span></label>
              <input type="password" name="password" [(ngModel)]="form.password" required autocomplete="new-password" />
              <div class="form-hint">Min 8 chars, upper + lower + digit + special</div>
            </div>
          </div>
          <div class="form-group">
            <label>Confirm Password <span class="req">*</span></label>
            <input type="password" name="confirmPassword" [(ngModel)]="form.confirmPassword" required />
          </div>
          <button class="btn btn-primary btn-lg" style="width:100%" [disabled]="loading || f.invalid">
            @if (loading) { <span class="spinner"></span> Creating… } @else { Create Account }
          </button>
        </form>

        <div class="auth-footer">
          Already have an account? <a routerLink="/login">Sign in</a>
        </div>
      </div>
    </div>
  `
})
export class RegisterComponent implements OnInit {
  form: any = {
    fullName: '', email: '', countryCode: '+91', mobile: '',
    address: '', username: '', password: '', confirmPassword: ''
  };
  countryCodes: string[] = ['+91', '+1', '+44', '+61', '+49', '+33', '+81', '+65', '+971', '+880'];
  loading = false;
  error = '';
  fieldErrors: Record<string, string> | null = null;

  constructor(private auth: AuthService, private api: ApiService, private router: Router) {}

  ngOnInit(): void {
    this.api.meta().subscribe(m => {
      if (m?.lists?.countryCodes?.length) this.countryCodes = m.lists.countryCodes;
    });
  }

  fieldErrorList(): [string, string][] {
    return Object.entries(this.fieldErrors || {});
  }

  submit(): void {
    this.loading = true;
    this.error = '';
    this.fieldErrors = null;
    this.auth.register(this.form).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/login'], { queryParams: { registered: 1 } });
      },
      error: err => {
        this.loading = false;
        if (err.error?.errors) this.fieldErrors = err.error.errors;
        else this.error = err.error?.message || 'Registration failed. Please try again.';
      }
    });
  }
}
