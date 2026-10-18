package com.hotel.booking.controller;

import com.hotel.booking.form.RoomFilter;
import com.hotel.booking.form.RoomForm;
import com.hotel.booking.model.Room;
import com.hotel.booking.model.RoomStatus;
import com.hotel.booking.service.BulkImportResult;
import com.hotel.booking.service.BusinessException;
import com.hotel.booking.service.RoomService;
import com.hotel.booking.util.ListQuery;
import com.hotel.booking.util.PageResult;
import com.hotel.booking.util.Paginator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** US015 (list / edit rooms) and US016 (add room, bulk add). */
@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController extends BaseController {

    private static final int PAGE_SIZE = 10;

    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    private static final Map<String, Comparator<Room>> SORTABLE = new HashMap<>();

    static {
        SORTABLE.put("roomNumber", Comparator.comparing(Room::getRoomNumber));
        SORTABLE.put("roomType", Comparator.comparing((Room r) -> r.getRoomType().ordinal()));
        SORTABLE.put("price", Comparator.comparing(Room::getPricePerNight));
        SORTABLE.put("occupancy", Comparator.comparingInt(Room::getMaxOccupancy));
        SORTABLE.put("status", Comparator.comparing((Room r) -> r.getStatus().ordinal()));
    }

    @GetMapping
    public String list(@ModelAttribute("filter") RoomFilter filter, BindingResult errors,
                       @RequestParam(defaultValue = "1") int page, @RequestParam(required = false) String sort,
                       @RequestParam(required = false) String dir, HttpServletRequest request, Model model) {
        List<Room> rooms = errors.hasErrors() ? List.of() : roomService.filter(filter);
        PageResult<Room> result = Paginator.paginate(rooms, SORTABLE, sort, dir, page, PAGE_SIZE);
        Map<Long, Boolean> availableNow = new HashMap<>();
        result.getItems().forEach(r -> availableNow.put(r.getId(), roomService.isAvailableNow(r)));
        model.addAttribute("page", result);
        model.addAttribute("availableNow", availableNow);
        model.addAttribute("lq", new ListQuery(request));
        return "admin/rooms";
    }

    // ------------------------------------------------------------------ add

    @GetMapping("/new")
    public String addForm(Model model) {
        model.addAttribute("form", new RoomForm());
        model.addAttribute("nextNumber", roomService.nextRoomNumber());
        return "admin/room-form";
    }

    @PostMapping("/new")
    public String add(@Valid @ModelAttribute("form") RoomForm form, BindingResult errors, Model model, RedirectAttributes ra) {
        validateAddStatus(form, errors);
        if (errors.hasErrors()) {
            model.addAttribute("nextNumber", roomService.nextRoomNumber());
            return "admin/room-form";
        }
        Room r = roomService.create(form);
        success(ra, "Room " + r.getRoomNumber() + " was added: " + r.getRoomType().getLabel() + ", ₹" + r.getPricePerNight()
                + " per night, up to " + r.getMaxOccupancy() + " guests.");
        ra.addFlashAttribute("added", true);
        return "redirect:/admin/rooms/new";
    }

    /** A new room is either available or not available (under maintenance); it cannot start as occupied. */
    private void validateAddStatus(RoomForm form, BindingResult errors) {
        if (form.getStatus() == RoomStatus.OCCUPIED) {
            errors.rejectValue("status", "invalid", "A new room cannot be marked as Occupied.");
        }
    }

    // ------------------------------------------------------------------ edit

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Room room = roomService.get(id);
        model.addAttribute("room", room);
        model.addAttribute("form", roomService.toForm(room));
        model.addAttribute("blocked", blockedReason(room));
        return "admin/room-edit";
    }

    private String blockedReason(Room room) {
        if (room.getStatus() == RoomStatus.OCCUPIED) return "Rooms marked as 'Occupied' cannot be updated.";
        if (roomService.hasActiveOrUpcomingReservations(room)) {
            return "Room " + room.getRoomNumber() + " has active or upcoming reservations, so it cannot be updated.";
        }
        return null;
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @Valid @ModelAttribute("form") RoomForm form, BindingResult errors,
                       Model model, RedirectAttributes ra) {
        Room room = roomService.get(id);
        if (!errors.hasErrors()) {
            try {
                roomService.update(id, form);
                success(ra, "Room " + room.getRoomNumber() + " details are updated successfully.");
                return "redirect:/admin/rooms";
            } catch (BusinessException e) {
                errors.reject("blocked", e.getMessage());
            }
        }
        model.addAttribute("room", room);
        model.addAttribute("blocked", blockedReason(room));
        return "admin/room-edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            Room r = roomService.get(id);
            roomService.delete(id);
            success(ra, "Room " + r.getRoomNumber() + " has been removed.");
        } catch (BusinessException e) {
            error(ra, e.getMessage());
        }
        return "redirect:/admin/rooms";
    }

    // ------------------------------------------------------------------ bulk add

    @GetMapping("/bulk")
    public String bulkForm() {
        return "admin/room-bulk";
    }

    @PostMapping("/bulk")
    public String bulkUpload(@RequestParam("file") MultipartFile file, Model model, RedirectAttributes ra) {
        BulkImportResult result = roomService.importCsv(file);
        if (result.isSuccess()) {
            success(ra, "Bulk upload is successful. " + result.getImported() + " rooms were added.");
            return "redirect:/admin/rooms";
        }
        model.addAttribute("result", result);
        return "admin/room-bulk";
    }

    @GetMapping("/bulk/template")
    public ResponseEntity<byte[]> template() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("room-import-template.csv").build());
        return ResponseEntity.ok().headers(headers).body(roomService.csvTemplate().getBytes(StandardCharsets.UTF_8));
    }
}
