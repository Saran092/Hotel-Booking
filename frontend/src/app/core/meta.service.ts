import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, tap, catchError } from 'rxjs';

export interface Meta {
  hotel: Record<string, any>;
  rx: Record<string, string>;
  enums: Record<string, string[]>;
  lists: Record<string, string[]>;
}

@Injectable({ providedIn: 'root' })
export class MetaService {
  private cache: Meta | null = null;
  constructor(private http: HttpClient) {}

  getCache(): Meta | null { return this.cache; }

  get(): Observable<Meta> {
    if (this.cache) return of(this.cache);
    return this.http.get<Meta>('/api/meta').pipe(
      tap(m => (this.cache = m)),
      catchError(() => of(null as any))
    );
  }

  label(enumName: string, value: string | null | undefined): string {
    if (!value) return '—';
    const arr = this.cache?.enums?.[enumName];
    if (!arr) return value;
    // enum values serialize as bare names; labels are Title Case of the name
    const map: Record<string, string> = {
      SINGLE: 'Single', DOUBLE: 'Double', STANDARD: 'Standard', DELUXE: 'Deluxe', SUITE: 'Suite', SUPREME: 'Supreme',
      AVAILABLE: 'Available', OCCUPIED: 'Occupied', UNDER_MAINTENANCE: 'Under Maintenance',
      PENDING_PAYMENT: 'Pending Payment', CONFIRMED: 'Confirmed', CHECKED_IN: 'Checked-in', CHECKED_OUT: 'Checked-out', CANCELLED: 'Canceled',
      PENDING: 'Pending', PARTIALLY_PAID: 'Partially Paid', PAID: 'Paid', REFUNDED: 'Refunded',
      CREDIT_CARD: 'Credit Card', DEBIT_CARD: 'Debit Card', UPI: 'UPI', WALLET: 'Wallet', NET_BANKING: 'Net Banking',
      ROOM_ISSUE: 'Room Issue', SERVICE_ISSUE: 'Service Issue', BILLING_ISSUE: 'Billing Issue', OTHER: 'Other',
      OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved', CLOSED: 'Closed', ESCALATED: 'Escalated',
      LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High',
      CALL: 'Call', EMAIL: 'Email',
      STEP: 'Step implemented', COMMUNICATION: 'Communication with customer', INTERNAL_NOTE: 'Internal note',
      INVESTIGATION: 'Investigation', RESOLUTION: 'Resolution', STATUS_CHANGE: 'Status change',
      CUSTOMER: 'Customer', ADMIN: 'Admin', STAFF: 'Staff',
      BILLING: 'Billing', MAINTENANCE: 'Maintenance', HOUSEKEEPING: 'Housekeeping', GENERAL: 'General',
      ROOM: 'Room Charges', SERVICE: 'Service Charges', ADDITIONAL: 'Additional Fees',
    };
    return map[value] || value;
  }
}
