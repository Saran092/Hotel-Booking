package com.hotel.booking.service;

import com.hotel.booking.model.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BulkImportResult {

    private int imported;

    private List<String> errors = new ArrayList<>();

    public int getImported() { return imported; }
    public void setImported(int imported) { this.imported = imported; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public boolean isSuccess() { return errors.isEmpty(); }

}
