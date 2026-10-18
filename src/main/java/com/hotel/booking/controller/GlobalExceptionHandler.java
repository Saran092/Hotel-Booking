package com.hotel.booking.controller;

import com.hotel.booking.config.HotelProperties;
import com.hotel.booking.service.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Shows rule violations ("booking not found", "room is no longer available", ...) on a friendly page. */
@ControllerAdvice
public class GlobalExceptionHandler {

    private final HotelProperties props;

    public GlobalExceptionHandler(HotelProperties props) {
        this.props = props;
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String business(BusinessException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        // @ModelAttribute advice does not apply to @ExceptionHandler views, but the layout
        // fragments (head/nav/foot) need these - supply them explicitly.
        model.addAttribute("hotel", props);
        model.addAttribute("unreadCount", 0L);
        return "error/business";
    }
}
