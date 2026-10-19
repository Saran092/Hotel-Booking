import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';

@Component({
  selector: 'app-admin-user-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <h1>{{ isEdit ? 'Edit User' : 'Create New User' }}</h1>
        @if (isEdit && account(); as a) { <div class="subtitle">{{ a.username }}</div> }
      </div>
      <a routerLink="/admin/users" class="btn btn-secondary">← Back to Users</a>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error) { <div class="alert alert-danger">{{ error }}</div> }
    @if (tempPassword()) {
      <div class="alert alert-warning">
        Temporary password: <code>{{ tempPassword() }}</code> — share this with the user; they must change it on first login.
      </div>
    }
    @if (fieldErrorList().length) {
      <div class="alert alert-danger">
        @for (e of fieldErrorList(); track e[0]) { <div><strong>{{ e[0] }}:</strong> {{ e[1] }}</div> }
      </div>
    }

    <div class="card" style="max-width:600px">
      <form (ngSubmit)="save()" #f="ngForm">
        @if (!isEdit) {
          <div class="form-group">
            <label>Username <span class="req">*</span></label>
            <input type="text" name="username" [(ngModel)]="form.username" required />
            <div class="form-hint">3-20 letters, digits, dot or underscore</div>
          </div>
        }
        <div class="form-group">
          <label>Full Name <span class="req">*</span></label>
          <input type="text" name="fullName" [(ngModel)]="form.fullName" required />
        </div>
        <div class="form-group">
          <label>Email <span class="req">*</span></label>
          <input type="email" name="email" [(ngModel)]="form.email" required />
        </div>
        <div class="form-group">
          <label>Roles <span class="req">*</span></label>
          <div class="chip-list">
            @for (r of roles; track r) {
              <span class="chip clickable" [class.selected]="form.roles.includes(r)" (click)="toggleRole(r)">{{ meta.label('allRoles', r) }}</span>
            }
          </div>
        </div>
        @if (form.roles.includes('STAFF')) {
          <div class="form-group">
            <label>Staff Expertise</label>
            <select name="expertise" [(ngModel)]="form.expertise">
              <option [ngValue]="null">Select…</option>
              @for (e of expertises; track e) { <option [value]="e">{{ meta.label('expertises', e) }}</option> }
            </select>
          </div>
        }
        <div class="form-group">
          <label>Status</label>
          <select name="active" [(ngModel)]="form.active">
            <option [ngValue]="true">Active</option>
            <option [ngValue]="false">Inactive</option>
          </select>
        </div>
        <button class="btn btn-primary btn-lg" [disabled]="loading || f.invalid">
          @if (loading) { <span class="spinner"></span> Saving… } @else { {{ isEdit ? 'Save Changes' : 'Create User' }} }
        </button>
      </form>
    </div>
  `
})
export class AdminUserFormComponent implements OnInit {
  form: any = { username: '', fullName: '', email: '', roles: [], expertise: null, active: true };
  roles: string[] = [];
  expertises: string[] = [];
  account = signal<any>(null);
  loading = false;
  message = signal('');
  error = '';
  tempPassword = signal('');
  fieldErrors: Record<string, string> | null = null;
  isEdit = false;
  private id!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, private router: Router, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      this.roles = m?.enums?.allRoles || [];
      this.expertises = m?.enums?.expertises || [];
    });
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = Number(idParam);
      this.api.userEditForm(this.id).subscribe({
        next: res => {
          this.account.set(res.account);
          if (res.form) {
            this.form = {
              username: res.form.username,
              fullName: res.form.fullName,
              email: res.form.email,
              roles: [...(res.form.roles || [])],
              expertise: res.form.expertise || null,
              active: res.form.active,
            };
          }
        },
        error: err => { this.error = err.error?.message || 'Could not load user.'; }
      });
    }
  }

  fieldErrorList(): [string, string][] { return Object.entries(this.fieldErrors || {}); }

  toggleRole(r: string): void {
    const i = this.form.roles.indexOf(r);
    if (i >= 0) this.form.roles.splice(i, 1);
    else this.form.roles.push(r);
  }

  save(): void {
    this.loading = true;
    this.error = '';
    this.fieldErrors = null;
    this.tempPassword.set('');
    const obs = this.isEdit ? this.api.updateUser(this.id, this.form) : this.api.createUser(this.form);
    obs.subscribe({
      next: (res: any) => {
        this.loading = false;
        if (res.tempPassword) {
          this.tempPassword.set(res.tempPassword);
          this.account.set(res.account);
        } else {
          this.message.set(res.message || 'Saved.');
          if (this.isEdit) this.account.set(res.account);
          else setTimeout(() => this.router.navigate(['/admin/users']), 1500);
        }
      },
      error: err => {
        this.loading = false;
        if (err.error?.errors) this.fieldErrors = err.error.errors;
        else this.error = err.error?.message || 'Save failed.';
      }
    });
  }
}
