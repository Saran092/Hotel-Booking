import { Routes } from '@angular/router';
import { authGuard, guestGuard, roleGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'register', canActivate: [guestGuard], loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent) },
  { path: 'forgot-password', canActivate: [guestGuard], loadComponent: () => import('./features/auth/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'change-password', canActivate: [authGuard], loadComponent: () => import('./features/auth/change-password.component').then(m => m.ChangePasswordComponent) },

  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/layout.component').then(m => m.LayoutComponent),
    children: [
      { path: '', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/home.component').then(m => m.HomeComponent) },
      { path: 'booking/:id/confirm', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/booking-confirm.component').then(m => m.BookingConfirmComponent) },
      { path: 'booking/:id/payment', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/payment.component').then(m => m.PaymentComponent) },
      { path: 'booking/:id/success', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/booking-success.component').then(m => m.BookingSuccessComponent) },
      { path: 'bookings', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/booking-history.component').then(m => m.BookingHistoryComponent) },
      { path: 'booking/:id', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/booking-details.component').then(m => m.BookingDetailsComponent) },
      { path: 'booking/:id/modify', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/booking-modify.component').then(m => m.BookingModifyComponent) },
      { path: 'booking/:id/cancel', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/booking-cancel.component').then(m => m.BookingCancelComponent) },
      { path: 'complaints', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/complaint-list.component').then(m => m.ComplaintListComponent) },
      { path: 'complaints/new', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/complaint-form.component').then(m => m.ComplaintFormComponent) },
      { path: 'complaints/:id', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/complaint-details.component').then(m => m.ComplaintDetailsComponent) },
      { path: 'complaints/:id/edit', canActivate: [roleGuard('CUSTOMER')], loadComponent: () => import('./features/customer/complaint-form.component').then(m => m.ComplaintFormComponent) },

      { path: 'profile', loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent) },
      { path: 'notifications', loadComponent: () => import('./features/profile/notifications.component').then(m => m.NotificationsComponent) },

      { path: 'staff/complaints', canActivate: [roleGuard('STAFF', 'ADMIN')], loadComponent: () => import('./features/staff/staff-complaints.component').then(m => m.StaffComplaintsComponent) },
      { path: 'staff/complaints/:id', canActivate: [roleGuard('STAFF', 'ADMIN')], loadComponent: () => import('./features/staff/staff-complaint-details.component').then(m => m.StaffComplaintDetailsComponent) },

      {
        path: 'admin', canActivate: [roleGuard('ADMIN')],
        children: [
          { path: '', pathMatch: 'full', loadComponent: () => import('./features/admin/admin-dashboard.component').then(m => m.AdminDashboardComponent) },
          { path: 'reports', loadComponent: () => import('./features/admin/admin-reports.component').then(m => m.AdminReportsComponent) },
          { path: 'rooms', loadComponent: () => import('./features/admin/admin-rooms.component').then(m => m.AdminRoomsComponent) },
          { path: 'rooms/new', loadComponent: () => import('./features/admin/admin-room-form.component').then(m => m.AdminRoomFormComponent) },
          { path: 'rooms/:id/edit', loadComponent: () => import('./features/admin/admin-room-form.component').then(m => m.AdminRoomFormComponent) },
          { path: 'users', loadComponent: () => import('./features/admin/admin-users.component').then(m => m.AdminUsersComponent) },
          { path: 'users/new', loadComponent: () => import('./features/admin/admin-user-form.component').then(m => m.AdminUserFormComponent) },
          { path: 'users/:id/edit', loadComponent: () => import('./features/admin/admin-user-form.component').then(m => m.AdminUserFormComponent) },
          { path: 'reservations', loadComponent: () => import('./features/admin/admin-reservations.component').then(m => m.AdminReservationsComponent) },
          { path: 'reservations/new', loadComponent: () => import('./features/admin/admin-reservation-form.component').then(m => m.AdminReservationFormComponent) },
          { path: 'reservations/:id/edit', loadComponent: () => import('./features/admin/admin-reservation-form.component').then(m => m.AdminReservationFormComponent) },
          { path: 'bills', loadComponent: () => import('./features/admin/admin-bills.component').then(m => m.AdminBillsComponent) },
          { path: 'bills/new', loadComponent: () => import('./features/admin/admin-bill-form.component').then(m => m.AdminBillFormComponent) },
          { path: 'bills/:id', loadComponent: () => import('./features/admin/admin-bill-details.component').then(m => m.AdminBillDetailsComponent) },
          { path: 'bills/:id/edit', loadComponent: () => import('./features/admin/admin-bill-form.component').then(m => m.AdminBillFormComponent) },
          { path: 'complaints', canActivate: [roleGuard('ADMIN')], loadComponent: () => import('./features/staff/staff-complaints.component').then(m => m.StaffComplaintsComponent) },
          { path: 'complaints/:id', canActivate: [roleGuard('ADMIN')], loadComponent: () => import('./features/staff/staff-complaint-details.component').then(m => m.StaffComplaintDetailsComponent) },
        ]
      },
    ]
  },

  { path: '**', redirectTo: '' },
];
