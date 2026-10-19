package com.hotel.booking.api;

import com.hotel.booking.api.dto.RoomDto;
import com.hotel.booking.api.dto.UserSummary;
import com.hotel.booking.form.RoomFilter;
import com.hotel.booking.form.RoomForm;
import com.hotel.booking.model.Room;
import com.hotel.booking.service.RoomService;
import com.hotel.booking.security.RestExceptionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/rooms")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRoomApi {

    private final RoomService roomService;
    private final RestExceptionHandler forms;

    public AdminRoomApi(RoomService roomService, RestExceptionHandler forms) {
        this.roomService = roomService;
        this.forms = forms;
    }

    @GetMapping
    public Map<String, Object> list(@ModelAttribute RoomFilter filter) {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(filter, "filter");
        List<Room> rooms = roomService.filter(filter);
        Map<Long, Boolean> availableNow = new HashMap<>();
        rooms.forEach(r -> availableNow.put(r.getId(), roomService.isAvailableNow(r)));
        return Map.of("rooms", rooms.stream().map(RoomDto::of).toList(), "availableNow", availableNow);
    }

    @GetMapping("/next-number")
    public Map<String, String> nextNumber() {
        return Map.of("nextNumber", roomService.nextRoomNumber());
    }

    @PostMapping("/bulk")
    @Transactional
    public Map<String, Object> bulkUpload(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        var result = roomService.importCsv(file);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", result.isSuccess());
        m.put("imported", result.getImported());
        m.put("errors", result.getErrors());
        m.put("message", result.isSuccess()
                ? "Bulk upload is successful. " + result.getImported() + " rooms were added."
                : "Bulk upload failed. " + result.getErrors().size() + " error(s) found.");
        return m;
    }

    @GetMapping("/{id}")
    public RoomDto get(@PathVariable Long id) {
        return RoomDto.of(roomService.get(id));
    }

    @PostMapping
    @Transactional
    public RoomDto create(@RequestBody RoomForm form) {
        var result = forms.validate(form);
        RestExceptionHandler.throwIfInvalid(result);
        return RoomDto.of(roomService.create(form));
    }

    @PutMapping("/{id}")
    @Transactional
    public RoomDto update(@PathVariable Long id, @RequestBody RoomForm form) {
        var result = forms.validate(form);
        RestExceptionHandler.throwIfInvalid(result);
        return RoomDto.of(roomService.update(id, form));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Map<String, Object> delete(@PathVariable Long id) {
        Room r = roomService.get(id);
        roomService.delete(id);
        return Map.of("message", "Room " + r.getRoomNumber() + " has been removed.");
    }
}
