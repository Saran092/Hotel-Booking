export type RoomType = 'SINGLE' | 'DOUBLE' | 'STANDARD' | 'DELUXE' | 'SUITE' | 'SUPREME';
export type RoomStatus = 'AVAILABLE' | 'OCCUPIED' | 'UNDER_MAINTENANCE';
export type BookingStatus = 'PENDING_PAYMENT' | 'CONFIRMED' | 'CHECKED_IN' | 'CHECKED_OUT' | 'CANCELLED';
export type PaymentStatus = 'PENDING' | 'PARTIALLY_PAID' | 'PAID' | 'REFUNDED';
export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'UPI' | 'WALLET' | 'NET_BANKING';
export type ComplaintCategory = 'ROOM_ISSUE' | 'SERVICE_ISSUE' | 'BILLING_ISSUE' | 'OTHER';
export type ComplaintStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED' | 'ESCALATED';
export type ComplaintPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type ContactPreference = 'CALL' | 'EMAIL';
export type ActionType = 'STEP' | 'COMMUNICATION' | 'INTERNAL_NOTE' | 'INVESTIGATION' | 'RESOLUTION' | 'STATUS_CHANGE';
export type Role = 'CUSTOMER' | 'ADMIN' | 'STAFF';
export type StaffExpertise = 'BILLING' | 'MAINTENANCE' | 'HOUSEKEEPING' | 'GENERAL';
export type BillItemCategory = 'ROOM' | 'SERVICE' | 'ADDITIONAL';

export interface UserSummary {
  id: number;
  username: string;
  fullName: string;
  email: string;
  countryCode: string;
  mobile: string;
  address: string | null;
  primaryRole: Role;
  roles: Role[];
  expertise: StaffExpertise | null;
  active: boolean;
  mustChangePassword: boolean;
  lockedUntil: string | null;
}

export interface LoginRequest { username: string; password: string; }
export interface LoginResponse { token: string; user: UserSummary; }

export interface RoomDto {
  id: number;
  roomNumber: string;
  roomType: RoomType;
  bedType: string;
  pricePerNight: number;
  maxOccupancy: number;
  status: RoomStatus;
  amenities: string[];
  description: string;
}

export interface PriceQuoteDto {
  room: RoomDto;
  checkIn: string;
  checkOut: string;
  nights: number;
  nightlyRate: number;
  nightlyRateWithTax: number;
  base: number;
  gst: number;
  service: number;
  total: number;
}

export interface BookingDto {
  id: number;
  bookingRef: string;
  room: RoomDto;
  customer: UserSummary | null;
  checkInDate: string;
  checkOutDate: string;
  nights: number;
  adults: number;
  children: number;
  specialRequests: string | null;
  adminNotes: string | null;
  paymentMethod: PaymentMethod | null;
  nightlyRate: number;
  baseAmount: number;
  gstAmount: number;
  serviceChargeAmount: number;
  totalAmount: number;
  amountPaid: number;
  refundAmount: number;
  status: BookingStatus;
  effectiveStatus: BookingStatus;
  paymentStatus: PaymentStatus;
  upcoming: boolean;
  createdAt: string;
  canceledAt: string | null;
  holdExpiresAt: string | null;
}

export interface CancellationQuoteDto {
  allowed: boolean;
  blockedReason: string | null;
  refundPercent: number;
  refundAmount: number;
  message: string;
}

export interface PaymentDto {
  id: number;
  transactionId: string;
  amount: number;
  method: PaymentMethod;
  purpose: string;
  success: boolean;
  failureReason: string | null;
  cardLast4: string | null;
  payerName: string | null;
  createdAt: string;
}

export interface ComplaintActionDto {
  id: number;
  actor: string | null;
  type: ActionType;
  details: string;
  createdAt: string;
  visibleToCustomer: boolean;
}

export interface ComplaintDto {
  id: number;
  reference: string;
  customer: UserSummary | null;
  bookingId: number | null;
  bookingRef: string | null;
  category: ComplaintCategory;
  title: string;
  description: string;
  contactPreference: ContactPreference | null;
  status: ComplaintStatus;
  priority: ComplaintPriority;
  submittedAt: string;
  expectedResolutionDate: string | null;
  assignedTo: UserSummary | null;
  supportResponse: string | null;
  resolutionNotes: string | null;
  actions: ComplaintActionDto[];
}

export interface BillItemDto {
  id: number;
  category: BillItemCategory;
  description: string;
  serviceDateTime: string | null;
  quantity: number;
  unitPrice: number;
  taxPercent: number;
  discount: number;
  lineTotal: number;
}

export interface BillDto {
  id: number;
  billRef: string;
  customer: UserSummary | null;
  bookingId: number | null;
  bookingRef: string | null;
  issueDate: string;
  paymentStatus: PaymentStatus;
  totalAmount: number;
  notes: string | null;
  items: BillItemDto[];
}

export interface NotificationDto {
  id: number;
  message: string;
  read: boolean;
  createdAt: string;
}

export interface ApiError {
  message: string;
  code?: string;
  errors?: Record<string, string>;
}
