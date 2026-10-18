package com.hotel.booking.controller;

import com.hotel.booking.form.BillFilter;
import com.hotel.booking.form.BillForm;
import com.hotel.booking.form.BillItemForm;
import com.hotel.booking.model.Bill;
import com.hotel.booking.service.BillService;
import com.hotel.booking.util.ListQuery;
import com.hotel.booking.util.PageResult;
import com.hotel.booking.util.Paginator;
import com.hotel.booking.util.Patterns;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** US020 (list / search bills) and US021 (add / edit bills). */
@Controller
@RequestMapping("/admin/bills")
public class AdminBillController extends BaseController {

    private static final int PAGE_SIZE = 10;
    private static final String BAD_TEXT = "Special characters are not allowed here (letters, digits, spaces, hyphens and apostrophes only).";

    private final BillService billService;

    public AdminBillController(BillService billService) {
        this.billService = billService;
    }

    private static final Map<String, Comparator<Bill>> SORTABLE = new HashMap<>();

    static {
        SORTABLE.put("id", Comparator.comparing(Bill::getId));
        SORTABLE.put("customer", Comparator.comparing((Bill b) -> b.getCustomer().getFullName().toLowerCase()));
        SORTABLE.put("date", Comparator.comparing(Bill::getIssueDate));
        SORTABLE.put("amount", Comparator.comparing(Bill::getTotalAmount));
        SORTABLE.put("status", Comparator.comparing((Bill b) -> b.getPaymentStatus().ordinal()));
    }

    @GetMapping
    public String list(@ModelAttribute("filter") BillFilter filter, BindingResult errors,
                       @RequestParam(defaultValue = "1") int page, @RequestParam(required = false) String sort,
                       @RequestParam(required = false) String dir, HttpServletRequest request, Model model) {
        if (filter.getBillId() != null && !filter.getBillId().matches(Patterns.SEARCH_TEXT)) errors.rejectValue("billId", "invalid", BAD_TEXT);
        if (filter.getCustomer() != null && !filter.getCustomer().matches(Patterns.SEARCH_TEXT)) errors.rejectValue("customer", "invalid", BAD_TEXT);
        if (filter.getDateFrom() != null && filter.getDateTo() != null && filter.getDateTo().isBefore(filter.getDateFrom())) {
            errors.rejectValue("dateTo", "order", "The end date must not be before the start date.");
        }
        List<Bill> found = errors.hasErrors() ? List.of() : billService.filter(filter);
        PageResult<Bill> result = Paginator.paginate(found, SORTABLE, sort == null ? "date" : sort, sort == null ? "desc" : dir, page, PAGE_SIZE);
        model.addAttribute("page", result);
        model.addAttribute("lq", new ListQuery(request));
        return "admin/bills";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("bill", billService.get(id));
        return "admin/bill-view";
    }

    private void formModel(Model model) {
        model.addAttribute("customers", userService.customers());
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        BillForm f = new BillForm();
        f.getItems().add(new BillItemForm());
        model.addAttribute("form", f);
        formModel(model);
        return "admin/bill-form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("form") BillForm form, BindingResult errors, Model model, RedirectAttributes ra) {
        billService.validate(form, errors);
        if (errors.hasErrors()) {
            formModel(model);
            return "admin/bill-form";
        }
        Bill b = billService.save(null, form);
        success(ra, "Bill " + b.getBillRef() + " was created. Total amount due: " + com.hotel.booking.service.BookingService.money(b.getTotalAmount()) + ".");
        return "redirect:/admin/bills/" + b.getId();
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Bill b = billService.get(id);
        model.addAttribute("bill", b);
        model.addAttribute("form", billService.toForm(b));
        formModel(model);
        return "admin/bill-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute("form") BillForm form, BindingResult errors, Model model,
                       RedirectAttributes ra) {
        billService.validate(form, errors);
        if (errors.hasErrors()) {
            model.addAttribute("bill", billService.get(id));
            formModel(model);
            return "admin/bill-form";
        }
        Bill b = billService.save(id, form);
        success(ra, "Bill " + b.getBillRef() + " was updated. Total amount due: " + com.hotel.booking.service.BookingService.money(b.getTotalAmount()) + ".");
        return "redirect:/admin/bills/" + b.getId();
    }
}
