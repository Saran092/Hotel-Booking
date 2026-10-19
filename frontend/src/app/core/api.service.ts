import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from './auth.service';
import {
  BillDto, BookingDto, CancellationQuoteDto, ComplaintDto, NotificationDto,
  PaymentDto, PriceQuoteDto, RoomDto, UserSummary
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private http: HttpClient, private auth: AuthService) {}

  // -------- meta
  meta(): Observable<any> { return this.http.get('/api/meta'); }

  // -------- profile
  profile(): Observable<UserSummary> { return this.http.get<UserSummary>('/api/profile'); }
  updateProfile(form: any): Observable<any> { return this.http.put('/api/profile', form); }
  notifications(): Observable<NotificationDto[]> { return this.http.get<NotificationDto[]>('/api/notifications'); }
  unreadCount(): Observable<{ count: number }> { return this.http.get<{ count: number }>('/api/notifications/unread-count'); }

  // -------- search + booking
  searchRooms(form: any): Observable<{ quotes: PriceQuoteDto[] }> {
    return this.http.post<{ quotes: PriceQuoteDto[] }>('/api/rooms/search', form);
  }
  previewBooking(form: any): Observable<PriceQuoteDto> {
    return this.http.post<PriceQuoteDto>('/api/bookings/preview', form);
  }
  createBooking(form: any): Observable<any> {
    return this.http.post('/api/bookings', form);
  }
  paymentForm(bookingId: number): Observable<any> {
    return this.http.get(`/api/bookings/${bookingId}/payment`);
  }
  pay(bookingId: number, form: any): Observable<any> {
    return this.http.post(`/api/bookings/${bookingId}/payment`, form);
  }
  success(bookingId: number): Observable<{ booking: BookingDto; payment: PaymentDto | null }> {
    return this.http.get<{ booking: BookingDto; payment: PaymentDto | null }>(`/api/bookings/${bookingId}/success`);
  }

  // -------- PDFs (returns Blob)
  receiptPdf(bookingId: number) {
    return this.http.get(`/api/bookings/${bookingId}/receipt.pdf`, { responseType: 'blob' as 'json' });
  }
  invoicePdf(bookingId: number) {
    return this.http.get(`/api/bookings/${bookingId}/invoice.pdf`, { responseType: 'blob' as 'json' });
  }

  // -------- history + details
  history(): Observable<{ upcoming: BookingDto[]; past: BookingDto[]; upcomingCount: number }> {
    return this.http.get<any>('/api/bookings');
  }
  bookingDetails(id: number): Observable<{ booking: BookingDto; cancellation: CancellationQuoteDto; modifiable: boolean }> {
    return this.http.get<any>(`/api/bookings/${id}`);
  }

  // -------- cancel
  cancelQuote(id: number): Observable<{ booking: BookingDto; quote: CancellationQuoteDto }> {
    return this.http.get<any>(`/api/bookings/${id}/cancel`);
  }
  cancelBooking(id: number): Observable<{ message: string; booking: BookingDto }> {
    return this.http.post<any>(`/api/bookings/${id}/cancel`, {});
  }

  // -------- modify
  modifyForm(id: number): Observable<any> {
    return this.http.get<any>(`/api/bookings/${id}/modify`);
  }
  modifyQuote(id: number, form: any): Observable<any> {
    return this.http.post<any>(`/api/bookings/${id}/modify/quote`, form);
  }
  modifyConfirm(id: number): Observable<any> {
    return this.http.post<any>(`/api/bookings/${id}/modify/confirm`, {});
  }
  modifyAvailability(id: number, params: any): Observable<any> {
    return this.http.get<any>(`/api/bookings/${id}/modify/availability`, { params: this.toParams(params) });
  }

  // -------- complaints (customer)
  myComplaints(): Observable<ComplaintDto[]> {
    return this.http.get<ComplaintDto[]>('/api/complaints');
  }
  complaintFormOptions(): Observable<any> {
    return this.http.get<any>('/api/complaints/form-options');
  }
  createComplaint(form: any): Observable<any> {
    return this.http.post('/api/complaints', form);
  }
  complaintDetails(id: number): Observable<ComplaintDto> {
    return this.http.get<ComplaintDto>(`/api/complaints/${id}`);
  }
  complaintEditForm(id: number): Observable<any> {
    return this.http.get<any>(`/api/complaints/${id}/edit`);
  }
  complaintEdit(id: number, form: any): Observable<any> {
    return this.http.put(`/api/complaints/${id}/edit`, form);
  }
  complaintConfirm(id: number): Observable<any> {
    return this.http.post(`/api/complaints/${id}/confirm`, {});
  }
  complaintReopen(id: number): Observable<any> {
    return this.http.post(`/api/complaints/${id}/reopen`, {});
  }

  // -------- complaints (staff/admin)
  manageComplaints(base: 'staff' | 'admin', filter: any): Observable<any> {
    return this.http.get<any>(`/api/${base}/complaints`, { params: this.toParams(filter) });
  }
  manageComplaintDetails(base: 'staff' | 'admin', id: number): Observable<any> {
    return this.http.get<any>(`/api/${base}/complaints/${id}`);
  }
  addComplaintAction(base: 'staff' | 'admin', id: number, form: any): Observable<ComplaintDto> {
    return this.http.post<ComplaintDto>(`/api/${base}/complaints/${id}/action`, form);
  }
  assignComplaint(id: number, staffId: number): Observable<any> {
    return this.http.post(`/api/admin/complaints/${id}/assign`, null, { params: { staffId } });
  }
  setComplaintPriority(base: 'staff' | 'admin', id: number, priority: string): Observable<any> {
    return this.http.post(`/api/${base}/complaints/${id}/priority`, null, { params: { priority } });
  }

  // -------- admin dashboard
  adminHome(): Observable<any> { return this.http.get<any>('/api/admin/home'); }
  adminReports(): Observable<any> { return this.http.get<any>('/api/admin/reports'); }

  // -------- admin rooms
  adminRooms(filter: any = {}): Observable<{ rooms: RoomDto[]; availableNow: Record<number, boolean> }> {
    return this.http.get<any>('/api/admin/rooms', { params: this.toParams(filter) });
  }
  nextRoomNumber(): Observable<{ nextNumber: string }> {
    return this.http.get<{ nextNumber: string }>('/api/admin/rooms/next-number');
  }
  createRoom(form: any): Observable<RoomDto> { return this.http.post<RoomDto>('/api/admin/rooms', form); }
  updateRoom(id: number, form: any): Observable<RoomDto> { return this.http.put<RoomDto>(`/api/admin/rooms/${id}`, form); }
  deleteRoom(id: number): Observable<any> { return this.http.delete(`/api/admin/rooms/${id}`); }
  bulkUpload(file: File): Observable<any> {
    const fd = new FormData();
    fd.append('file', file);
    return this.http.post('/api/admin/rooms/bulk', fd);
  }

  // -------- admin users
  adminUsers(filter: any = {}): Observable<{ users: UserSummary[] }> {
    return this.http.get<any>('/api/admin/users', { params: this.toParams(filter) });
  }
  createUser(form: any): Observable<any> { return this.http.post('/api/admin/users', form); }
  userEditForm(id: number): Observable<any> { return this.http.get<any>(`/api/admin/users/${id}/edit`); }
  updateUser(id: number, form: any): Observable<any> { return this.http.put(`/api/admin/users/${id}/edit`, form); }
  resetUserPassword(id: number): Observable<any> { return this.http.post(`/api/admin/users/${id}/reset-password`, {}); }
  setUserStatus(id: number, active: boolean): Observable<any> {
    return this.http.post(`/api/admin/users/${id}/status`, null, { params: { active } });
  }

  // -------- admin reservations
  adminReservations(filter: any = {}): Observable<{ reservations: BookingDto[] }> {
    return this.http.get<any>('/api/admin/reservations', { params: this.toParams(filter) });
  }
  adminReservationFormOptions(): Observable<any> { return this.http.get<any>('/api/admin/reservations/new'); }
  adminReservationEditForm(id: number): Observable<any> { return this.http.get<any>(`/api/admin/reservations/${id}/edit`); }
  createAdminReservation(form: any): Observable<BookingDto> {
    return this.http.post<BookingDto>('/api/admin/reservations', form);
  }
  updateAdminReservation(id: number, form: any): Observable<any> {
    return this.http.put(`/api/admin/reservations/${id}/edit`, form);
  }
  cancelAdminReservation(id: number): Observable<any> {
    return this.http.post(`/api/admin/reservations/${id}/cancel`, {});
  }

  // -------- admin bills
  adminBills(filter: any = {}): Observable<{ bills: BillDto[] }> {
    return this.http.get<any>('/api/admin/bills', { params: this.toParams(filter) });
  }
  billDetails(id: number): Observable<BillDto> { return this.http.get<BillDto>(`/api/admin/bills/${id}`); }
  adminBillFormOptions(): Observable<any> { return this.http.get<any>('/api/admin/bills/new'); }
  billEditForm(id: number): Observable<any> { return this.http.get<any>(`/api/admin/bills/${id}/edit`); }
  createBill(form: any): Observable<BillDto> { return this.http.post<BillDto>('/api/admin/bills', form); }
  updateBill(id: number, form: any): Observable<any> { return this.http.put(`/api/admin/bills/${id}/edit`, form); }

  private toParams(obj: any): HttpParams {
    let params = new HttpParams();
    Object.entries(obj || {}).forEach(([k, v]) => {
      if (v !== null && v !== undefined && v !== '') params = params.set(k, String(v));
    });
    return params;
  }
}
