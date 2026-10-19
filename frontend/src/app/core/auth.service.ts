import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, map, Observable, of, tap, throwError } from 'rxjs';
import { LoginRequest, LoginResponse, UserSummary } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'hotel_token';
  private readonly userKey = 'hotel_user';

  readonly user = signal<UserSummary | null>(this.readUser());
  readonly token = signal<string | null>(localStorage.getItem(this.tokenKey));

  constructor(private http: HttpClient, private router: Router) {
    if (this.token() && !this.user()) this.refreshMe().subscribe();
  }

  get isAuthenticated(): boolean { return !!this.token(); }

  hasRole(role: string): boolean {
    return this.user()?.roles?.includes(role as any) ?? false;
  }

  get isCustomer(): boolean { return this.hasRole('CUSTOMER'); }
  get isAdmin(): boolean { return this.hasRole('ADMIN'); }
  get isStaff(): boolean { return this.hasRole('STAFF'); }
  get mustChangePassword(): boolean { return this.user()?.mustChangePassword ?? false; }

  homeFor(user: UserSummary | null = this.user()): string {
    if (!user) return '/login';
    if (user.mustChangePassword) return '/change-password';
    if (user.roles.includes('ADMIN')) return '/admin';
    if (user.roles.includes('STAFF')) return '/staff/complaints';
    return '/';
  }

  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/auth/login', req).pipe(
      tap(res => this.storeSession(res))
    );
  }

  register(form: any): Observable<any> {
    return this.http.post('/api/auth/register', form);
  }

  forgotPassword(form: any): Observable<any> {
    return this.http.post('/api/auth/forgot-password', form);
  }

  changePassword(form: any): Observable<any> {
    return this.http.post('/api/auth/change-password', form);
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.token.set(null);
    this.user.set(null);
    this.router.navigate(['/login']);
  }

  refreshMe(): Observable<UserSummary | null> {
    if (!this.token()) return of(null);
    return this.http.get<UserSummary>('/api/auth/me').pipe(
      tap(u => {
        this.user.set(u);
        localStorage.setItem(this.userKey, JSON.stringify(u));
      }),
      catchError(err => {
        if (err.status === 401 || err.status === 403) this.logout();
        return throwError(() => err);
      })
    );
  }

  private storeSession(res: LoginResponse): void {
    localStorage.setItem(this.tokenKey, res.token);
    localStorage.setItem(this.userKey, JSON.stringify(res.user));
    this.token.set(res.token);
    this.user.set(res.user);
  }

  private readUser(): UserSummary | null {
    try {
      const raw = localStorage.getItem(this.userKey);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  }
}
