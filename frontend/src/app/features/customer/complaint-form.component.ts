import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { MetaService } from '../../core/meta.service';

@Component({
  selector: 'app-complaint-form',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="page-header">
      <div>
        <h1>{{ isEdit ? 'Edit Complaint' : 'File a Complaint' }}</h1>
        <div class="subtitle">{{ isEdit ? 'Update your complaint while it is still open' : 'Tell us about an issue and we will get back to you' }}</div>
      </div>
      <a routerLink="/complaints" class="btn btn-secondary">← Back to Complaints</a>
    </div>

    @if (message()) { <div class="alert alert-success">{{ message() }}</div> }
    @if (error) { <div class="alert alert-danger">{{ error }}</div> }
    @if (fieldErrorList().length) {
      <div class="alert alert-danger">
        @for (e of fieldErrorList(); track e[0]) { <div><strong>{{ e[0] }}:</strong> {{ e[1] }}</div> }
      </div>
    }

    <div class="card" style="max-width:700px">
      <form (ngSubmit)="submit()" #f="ngForm">
        <div class="grid grid-2">
          <div class="form-group">
            <label>Category <span class="req">*</span></label>
            <select name="category" [(ngModel)]="form.category" required>
              <option value="">Select…</option>
              @for (c of categories; track c) { <option [value]="c">{{ meta.label('complaintCategories', c) }}</option> }
            </select>
          </div>
          <div class="form-group">
            <label>Related Booking (optional)</label>
            <select name="bookingId" [(ngModel)]="form.bookingId">
              <option [ngValue]="null">— None —</option>
              @for (b of myBookings(); track b.id) { <option [ngValue]="b.id">{{ b.label || b.bookingRef }}</option> }
            </select>
          </div>
        </div>

        <div class="form-group">
          <label>Title <span class="req">*</span></label>
          <input type="text" name="title" [(ngModel)]="form.title" maxlength="100" required />
          <div class="form-hint">At least 10 characters</div>
        </div>

        <div class="form-group">
          <label>Description <span class="req">*</span></label>
          <textarea name="description" [(ngModel)]="form.description" rows="5" maxlength="500" required></textarea>
          <div class="form-hint">At least 20 characters. Please provide as much detail as possible.</div>
        </div>

        <div class="form-group">
          <label>Preferred Contact Method <span class="req">*</span></label>
          <select name="contactPreference" [(ngModel)]="form.contactPreference" required>
            <option value="">Select…</option>
            @for (p of contactPrefs; track p) { <option [value]="p">{{ meta.label('contactPreferences', p) }}</option> }
          </select>
        </div>

        <button class="btn btn-primary btn-lg" [disabled]="loading || f.invalid">
          @if (loading) { <span class="spinner"></span> Submitting… } @else { {{ isEdit ? 'Update Complaint' : 'Submit Complaint' }} }
        </button>
      </form>
    </div>
  `
})
export class ComplaintFormComponent implements OnInit {
  form: any = { category: '', bookingId: null, title: '', description: '', contactPreference: '' };
  categories: string[] = [];
  contactPrefs: string[] = [];
  myBookings = signal<any[]>([]);
  loading = false;
  message = signal('');
  error = '';
  fieldErrors: Record<string, string> | null = null;
  isEdit = false;
  private id!: number;

  constructor(private api: ApiService, private route: ActivatedRoute, private router: Router, public meta: MetaService) {}

  ngOnInit(): void {
    this.meta.get().subscribe(m => {
      this.categories = m?.enums?.complaintCategories || [];
      this.contactPrefs = m?.enums?.contactPreferences || [];
    });
    this.api.complaintFormOptions().subscribe({
      next: res => this.myBookings.set(res.myBookings || [])
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEdit = true;
      this.id = Number(idParam);
      this.api.complaintEditForm(this.id).subscribe({
        next: res => {
          if (res.form) this.form = { ...this.form, ...res.form };
          if (res.myBookings) this.myBookings.set(res.myBookings);
        },
        error: err => { this.error = err.error?.message || 'Could not load complaint for editing.'; }
      });
    }
  }

  fieldErrorList(): [string, string][] { return Object.entries(this.fieldErrors || {}); }

  submit(): void {
    this.loading = true;
    this.error = '';
    this.fieldErrors = null;
    const req = this.api[this.isEdit ? 'complaintEdit' : 'createComplaint'];
    (this.isEdit ? this.api.complaintEdit(this.id, this.form) : this.api.createComplaint(this.form)).subscribe({
      next: (res: any) => {
        this.loading = false;
        this.message.set(res.message || (this.isEdit ? 'Complaint updated.' : 'Complaint submitted.'));
        setTimeout(() => this.router.navigate(['/complaints']), 1500);
      },
      error: err => {
        this.loading = false;
        if (err.error?.errors) this.fieldErrors = err.error.errors;
        else this.error = err.error?.message || 'Submission failed. Please try again.';
      }
    });
  }
}
