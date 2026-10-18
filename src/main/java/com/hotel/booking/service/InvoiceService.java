package com.hotel.booking.service;

import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.model.Booking;
import com.hotel.booking.model.Payment;
import com.hotel.booking.repository.PaymentRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Builds the invoice and the payment receipt as PDF documents. */
@Service
public class InvoiceService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private static final Color BRAND = new Color(0x1F, 0x4E, 0x79);
    private static final Color LIGHT = new Color(0xEE, 0xF3, 0xF8);

    private final HotelProperties props;
    private final PaymentRepository payments;

    public InvoiceService(HotelProperties props, PaymentRepository payments) {
        this.props = props;
        this.payments = payments;
    }

    private static String rs(BigDecimal v) {
        return "Rs. " + String.format(Locale.US, "%,.2f", v);
    }

    public String invoiceNumber(Booking b) {
        return String.format("INV-%s-%06d", b.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyyMMdd")), b.getId());
    }

    private Payment lastPayment(Booking b) {
        return payments.findFirstByBookingAndSuccessTrueOrderByCreatedAtDesc(b).orElse(null);
    }

    // ------------------------------------------------------------------ invoice (US007)

    public byte[] invoice(Booking b) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();
            header(doc, "INVOICE", invoiceNumber(b));

            section(doc, "Customer Details");
            PdfPTable customer = table();
            row(customer, "Name", b.getCustomer().getFullName());
            row(customer, "Email", b.getCustomer().getEmail());
            row(customer, "Mobile", b.getCustomer().getFullMobile());
            doc.add(customer);

            section(doc, "Booking Details");
            PdfPTable booking = table();
            row(booking, "Booking ID", b.getBookingRef());
            row(booking, "Room", b.getRoom().getRoomType().getLabel() + " - Room " + b.getRoom().getRoomNumber());
            row(booking, "Occupancy limit", b.getRoom().getMaxOccupancy() + " guests");
            row(booking, "Price per night", rs(b.getNightlyRate()));
            row(booking, "Check-in", b.getCheckInDate().format(DATE));
            row(booking, "Check-out", b.getCheckOutDate().format(DATE));
            row(booking, "Number of nights", String.valueOf(b.getNights()));
            row(booking, "Number of guests", b.getAdults() + " adult(s), " + b.getChildren() + " child(ren)");
            doc.add(booking);

            section(doc, "Charges");
            PdfPTable charges = table();
            row(charges, "Base price (before tax)", rs(b.getBaseAmount()));
            row(charges, "GST (" + props.getGstPercent().stripTrailingZeros().toPlainString() + "%)", rs(b.getGstAmount()));
            row(charges, "Service charges (" + props.getServiceChargePercent().stripTrailingZeros().toPlainString() + "%)", rs(b.getServiceChargeAmount()));
            row(charges, "Additional fees", rs(BigDecimal.ZERO));
            totalRow(charges, "Total amount paid", rs(b.getTotalAmount()));
            doc.add(charges);

            Payment p = lastPayment(b);
            section(doc, "Payment");
            PdfPTable pay = table();
            row(pay, "Payment method", b.getPaymentMethod() == null ? "-" : b.getPaymentMethod().getLabel());
            row(pay, "Transaction ID", p == null ? "-" : p.getTransactionId());
            row(pay, "Invoice date & time", LocalDateTime.now().format(DATE_TIME));
            doc.add(pay);

            footer(doc);
            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new BusinessException("Invoice generation failed. Please try again later.");
        }
    }

    // ------------------------------------------------------------------ receipt (US006)

    public byte[] receipt(Booking b) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();
            Payment p = lastPayment(b);
            header(doc, "PAYMENT RECEIPT", "Booking " + b.getBookingRef());

            PdfPTable t = table();
            row(t, "Booking ID", b.getBookingRef());
            row(t, "Payment transaction ID", p == null ? "-" : p.getTransactionId());
            row(t, "Customer name", b.getCustomer().getFullName());
            row(t, "Room details", b.getRoom().getRoomType().getLabel() + " - Room " + b.getRoom().getRoomNumber());
            row(t, "Stay duration", b.getCheckInDate().format(DATE) + " to " + b.getCheckOutDate().format(DATE)
                    + " (" + b.getNights() + " night(s))");
            row(t, "Payment method", b.getPaymentMethod() == null ? "-" : b.getPaymentMethod().getLabel());
            row(t, "Paid on", p == null ? "-" : p.getCreatedAt().format(DATE_TIME));
            totalRow(t, "Payment amount", rs(b.getAmountPaid()));
            doc.add(t);

            footer(doc);
            doc.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new BusinessException("Receipt generation failed. Please try again later.");
        }
    }

    // ------------------------------------------------------------------ layout helpers

    private void header(Document doc, String title, String subtitle) throws DocumentException {
        PdfPTable h = new PdfPTable(new float[]{3f, 2f});
        h.setWidthPercentage(100);
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(BRAND);
        left.setPadding(12);
        left.setBorder(0);
        left.addElement(new Paragraph(props.getName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE)));
        left.addElement(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA, 11, Color.WHITE)));
        PdfPCell right = new PdfPCell(new Phrase(subtitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE)));
        right.setBackgroundColor(BRAND);
        right.setBorder(0);
        right.setPadding(12);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        h.addCell(left);
        h.addCell(right);
        doc.add(h);
        doc.add(new Paragraph(" "));
    }

    private void section(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND));
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private PdfPTable table() throws DocumentException {
        PdfPTable t = new PdfPTable(new float[]{2f, 3f});
        t.setWidthPercentage(100);
        return t;
    }

    private void row(PdfPTable t, String label, String value) {
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        PdfPCell a = new PdfPCell(new Phrase(label, bold));
        a.setBackgroundColor(LIGHT);
        a.setPadding(6);
        a.setBorderColor(Color.LIGHT_GRAY);
        PdfPCell b = new PdfPCell(new Phrase(value == null ? "-" : value, normal));
        b.setPadding(6);
        b.setBorderColor(Color.LIGHT_GRAY);
        t.addCell(a);
        t.addCell(b);
    }

    private void totalRow(PdfPTable t, String label, String value) {
        Font f = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        PdfPCell a = new PdfPCell(new Phrase(label, f));
        a.setBackgroundColor(BRAND);
        a.setPadding(7);
        PdfPCell b = new PdfPCell(new Phrase(value, f));
        b.setBackgroundColor(BRAND);
        b.setPadding(7);
        t.addCell(a);
        t.addCell(b);
    }

    private void footer(Document doc) throws DocumentException {
        Paragraph p = new Paragraph("\n" + props.getName() + "\n" + props.getAddress() + "\nEmail: " + props.getEmail()
                + "   |   Customer support: " + props.getPhone() + "\nThank you for staying with us.",
                FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY));
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
    }
}
