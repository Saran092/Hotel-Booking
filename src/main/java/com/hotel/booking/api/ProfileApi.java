package com.hotel.booking.api;

import com.hotel.booking.api.dto.NotificationDto;
import com.hotel.booking.api.dto.UserSummary;
import com.hotel.booking.config.AppUserDetails;
import com.hotel.booking.form.ProfileForm;
import com.hotel.booking.model.User;
import com.hotel.booking.service.NotificationService;
import com.hotel.booking.service.UserService;
import com.hotel.booking.util.ValidationGroups;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Profile editing and the notification inbox for every role. */
@RestController
@RequestMapping("/api")
public class ProfileApi {

    private final UserService userService;
    private final NotificationService notifications;
    private final com.hotel.booking.security.RestExceptionHandler forms;

    public ProfileApi(UserService userService, NotificationService notifications,
                      com.hotel.booking.security.RestExceptionHandler forms) {
        this.userService = userService;
        this.notifications = notifications;
        this.forms = forms;
    }

    @GetMapping("/profile")
    public UserSummary profile(@AuthenticationPrincipal AppUserDetails principal) {
        return UserSummary.of(userService.get(principal.getId()));
    }

    @PutMapping("/profile")
    @Transactional
    public Map<String, Object> update(@RequestBody ProfileForm form,
                                      @AuthenticationPrincipal AppUserDetails principal) {
        User user = userService.get(principal.getId());
        var result = forms.validate(form, ValidationGroups.Ordered.class);
        userService.validateProfile(user, form, result);
        com.hotel.booking.security.RestExceptionHandler.throwIfInvalid(result);
        userService.updateProfile(user, form);
        return Map.of("message", "Your profile has been updated successfully.",
                "user", UserSummary.of(user));
    }

    @GetMapping("/notifications")
    public List<NotificationDto> inbox(@AuthenticationPrincipal AppUserDetails principal) {
        User user = userService.get(principal.getId());
        List<NotificationDto> list = notifications.latest(user).stream().map(NotificationDto::of).toList();
        notifications.markAllRead(user);
        return list;
    }

    @GetMapping("/notifications/unread-count")
    public Map<String, Long> unread(@AuthenticationPrincipal AppUserDetails principal) {
        return Map.of("count", notifications.unreadCount(userService.get(principal.getId())));
    }
}
