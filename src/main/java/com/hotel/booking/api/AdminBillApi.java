package com.hotel.booking.api;

import com.hotel.booking.api.dto.BillDto;
import com.hotel.booking.api.dto.UserSummary;
import com.hotel.booking.form.BillFilter;
import com.hotel.booking.form.BillForm;
import com.hotel.booking.model.Bill;
import com.hotel.booking.security.RestExceptionHandler;
import com.hotel.booking.service.BillService;
import com.hotel.booking.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/** US020 (list / search bills) and US021 (add / edit bills). */
@RestController
@RequestMapping("/api/admin/bills")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBillApi {

    private final BillService billService;
    private final UserService userService;
    private final RestExceptionHandler forms;

    public AdminBillApi(BillService billService, UserService userService, RestExceptionHandler forms) {
        this.billService = billService;
        this.userService = userService;
        this.forms = forms;
    }

    @GetMapping
    public Map<String, Object> list(@ModelAttribute BillFilter filter) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(filter, "filter");
        if (filter.getBillId() != null && !filter.getBillId().matches(com.hotel.booking.util.Patterns.SEARCH_TEXT)) {
            errors.rejectValue("billId", "invalid", "Special characters are not allowed here (letters, digits, spaces, hyphens and apostrophes only).");
        }
        if (filter.getCustomer() != null && !filter.getCustomer().matches(com.hotel.booking.util.Patterns.SEARCH_TEXT)) {
            errors.rejectValue("customer", "invalid", "Special characters are not allowed here (letters, digits, spaces, hyphens and apostrophes only).");
        }
        if (filter.getDateFrom() != null && filter.getDateTo() != null && filter.getDateTo().isBefore(filter.getDateFrom())) {
            errors.rejectValue("dateTo", "order", "The end date must not be before the start date.");
        }
        if (errors.hasErrors()) {
            throw new com.hotel.booking.security.ApiValidationException(RestExceptionHandler.fieldErrors(errors));
        }
        return Map.of("bills", billService.filter(filter).stream().map(BillDto::of).toList());
    }

    @GetMapping("/{id}")
    public BillDto view(@PathVariable Long id) {
        return BillDto.of(billService.get(id));
    }

    private Map<String, Object> formModel() {
        return Map.of("customers", userService.customers().stream().map(UserSummary::brief).toList());
    }

    @GetMapping("/new")
    public Map<String, Object> newForm() {
        Map<String, Object> m = new LinkedHashMap<>(formModel());
        BillForm f = new BillForm();
        f.getItems().add(new com.hotel.booking.form.BillItemForm());
        m.put("form", f);
        return m;
    }

    @PostMapping
    @Transactional
    public BillDto create(@RequestBody BillForm form) {
        var result = forms.validate(form);
        billService.validate(form, result);
        RestExceptionHandler.throwIfInvalid(result);
        return BillDto.of(billService.save(null, form));
    }

    @GetMapping("/{id}/edit")
    public Map<String, Object> editForm(@PathVariable Long id) {
        Bill b = billService.get(id);
        Map<String, Object> m = new LinkedHashMap<>(formModel());
        m.put("bill", BillDto.of(b));
        m.put("form", billService.toForm(b));
        return m;
    }

    @PutMapping("/{id}/edit")
    @Transactional
    public Map<String, Object> edit(@PathVariable Long id, @RequestBody BillForm form) {
        var result = forms.validate(form);
        billService.validate(form, result);
        RestExceptionHandler.throwIfInvalid(result);
        Bill b = billService.save(id, form);
        return Map.of("message", "Bill " + b.getBillRef() + " was updated. Total amount due: "
                + com.hotel.booking.service.BookingService.money(b.getTotalAmount()) + ".", "bill", BillDto.of(b));
    }
}
