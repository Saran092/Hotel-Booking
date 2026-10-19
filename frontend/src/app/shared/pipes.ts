import { DecimalPipe, DatePipe } from '@angular/common';
import { Pipe, PipeTransform } from '@angular/core';

/** Formats a number as Indian Rupees: 1234.5 -> "₹1,234.50" */
@Pipe({ name: 'inr', standalone: true })
export class InrPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return '₹' + new Intl.NumberFormat('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
  }
}

/** Formats "2026-10-19T12:00:00" -> "19 Oct 2026, 12:00" */
@Pipe({ name: 'fmtDate', standalone: true })
export class FmtDatePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '—';
    const d = new Date(value);
    if (isNaN(d.getTime())) return value;
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })
      + ', ' + d.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }
}

/** Formats just the date part "2026-10-19" -> "19 Oct 2026" */
@Pipe({ name: 'fmtDay', standalone: true })
export class FmtDayPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) return '—';
    const d = new Date(value + (value.length === 10 ? 'T00:00:00' : ''));
    if (isNaN(d.getTime())) return value;
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
