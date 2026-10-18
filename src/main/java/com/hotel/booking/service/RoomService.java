package com.hotel.booking.service;

import com.hotel.booking.form.RoomFilter;
import com.hotel.booking.form.RoomForm;
import com.hotel.booking.form.RoomSearchForm;
import com.hotel.booking.model.*;
import com.hotel.booking.repository.BookingRepository;
import com.hotel.booking.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoomService {

    public static final List<String> AMENITIES = List.of("Wi-Fi", "TV", "Mini-Bar", "Air Conditioning", "Room Service",
            "Balcony", "Sea View", "Bathtub", "Coffee Maker", "Safe");
    public static final List<String> BED_TYPES = List.of("Single", "Double", "Twin", "Queen", "King");
    public static final String CSV_HEADER = "roomNumber,roomType,bedType,pricePerNight,maxOccupancy,status,amenities,description";
    private static final int MAX_CSV_ROWS = 500;

    private final RoomRepository rooms;
    private final BookingRepository bookings;

    public RoomService(RoomRepository rooms, BookingRepository bookings) {
        this.rooms = rooms;
        this.bookings = bookings;
    }

    public Room get(Long id) {
        return rooms.findById(id).orElseThrow(() -> new BusinessException("Room not found."));
    }

    // ------------------------------------------------------------------ availability

    /** True if no active reservation overlaps the stay. A booking given as {@code excludeBookingId} is ignored. */
    public boolean isAvailable(Room room, LocalDate checkIn, LocalDate checkOut, Long excludeBookingId) {
        if (room.getStatus() == RoomStatus.UNDER_MAINTENANCE) return false;
        return bookings.countOverlapping(room.getId(), checkIn, checkOut, excludeBookingId == null ? -1L : excludeBookingId,
                LocalDateTime.now()) == 0;
    }

    /** "Available / Not Available as on current date" for the admin room list. */
    public boolean isAvailableNow(Room room) {
        return room.getStatus() == RoomStatus.AVAILABLE
                && bookings.countOverlapping(room.getId(), LocalDate.now(), LocalDate.now().plusDays(1), -1L, LocalDateTime.now()) == 0;
    }

    public boolean isAvailableOn(Room room, LocalDate date) {
        return isAvailable(room, date, date.plusDays(1), null);
    }

    /** Rooms of the requested type that are free for the whole stay and big enough for the party. */
    public List<Room> findAvailable(RoomSearchForm f) {
        int guests = f.getAdults() + (f.getChildren() == null ? 0 : f.getChildren());
        Set<Long> booked = new HashSet<>(bookings.findBookedRoomIds(f.getCheckIn(), f.getCheckOut(), LocalDateTime.now()));
        return rooms.findByRoomTypeAndStatusNot(f.getRoomType(), RoomStatus.UNDER_MAINTENANCE).stream()
                .filter(r -> !booked.contains(r.getId()))
                .filter(r -> r.getMaxOccupancy() >= guests)
                .sorted(Comparator.comparing(Room::getPricePerNight).thenComparing(Room::getRoomNumber))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ admin list / search

    public List<Room> filter(RoomFilter f) {
        String q = f.getQ() == null ? "" : f.getQ().trim().toLowerCase(Locale.ROOT);
        return rooms.findAll().stream()
                .filter(r -> q.isEmpty() || r.getRoomNumber().toLowerCase(Locale.ROOT).contains(q)
                        || r.getRoomType().getLabel().toLowerCase(Locale.ROOT).contains(q))
                .filter(r -> f.getRoomType() == null || r.getRoomType() == f.getRoomType())
                .filter(r -> f.getMinPrice() == null || r.getPricePerNight().compareTo(f.getMinPrice()) >= 0)
                .filter(r -> f.getMaxPrice() == null || r.getPricePerNight().compareTo(f.getMaxPrice()) <= 0)
                .filter(r -> !StringUtils.hasText(f.getAmenity()) || r.getAmenities().contains(f.getAmenity()))
                .filter(r -> f.getMaxOccupancy() == null || r.getMaxOccupancy() >= f.getMaxOccupancy())
                .filter(r -> f.getDate() == null || isAvailableOn(r, f.getDate()))
                .filter(r -> !StringUtils.hasText(f.getAvailability())
                        || ("AVAILABLE".equals(f.getAvailability()) == isAvailableNow(r)))
                .sorted(Comparator.comparing(Room::getRoomNumber))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ create / update / delete

    /** Next free room number (numeric, at most 6 characters). */
    public synchronized String nextRoomNumber() {
        long max = 100;
        for (Room r : rooms.findAll()) {
            if (r.getRoomNumber().matches("\\d{1,6}")) max = Math.max(max, Long.parseLong(r.getRoomNumber()));
        }
        return String.valueOf(max + 1);
    }

    private List<String> cleanAmenities(Collection<String> in) {
        if (in == null) return List.of();
        return in.stream().filter(AMENITIES::contains).distinct().collect(Collectors.toList());
    }

    @Transactional
    public Room create(RoomForm f) {
        Room r = new Room();
        r.setRoomNumber(nextRoomNumber());
        apply(r, f);
        return rooms.save(r);
    }

    private void apply(Room r, RoomForm f) {
        r.setRoomType(f.getRoomType());
        r.setBedType(StringUtils.hasText(f.getBedType()) ? f.getBedType().trim() : null);
        r.setPricePerNight(f.getPricePerNight().setScale(2, java.math.RoundingMode.HALF_UP));
        r.setAmenities(new LinkedHashSet<>(cleanAmenities(f.getAmenities())));
        r.setMaxOccupancy(f.getMaxOccupancy());
        r.setStatus(f.getStatus());
        r.setDescription(StringUtils.hasText(f.getDescription()) ? f.getDescription().trim() : null);
    }

    public RoomForm toForm(Room r) {
        RoomForm f = new RoomForm();
        f.setRoomNumber(r.getRoomNumber());
        f.setRoomType(r.getRoomType());
        f.setBedType(r.getBedType());
        f.setPricePerNight(r.getPricePerNight());
        f.setAmenities(new ArrayList<>(r.getAmenities()));
        f.setMaxOccupancy(r.getMaxOccupancy());
        f.setStatus(r.getStatus());
        f.setDescription(r.getDescription());
        return f;
    }

    private static final List<BookingStatus> LIVE = List.of(BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN);

    public boolean hasActiveOrUpcomingReservations(Room r) {
        return bookings.countByRoomIdAndStatusInAndCheckOutDateGreaterThanEqual(r.getId(), LIVE, LocalDate.now()) > 0;
    }

    /** Rooms with active reservations or marked Occupied cannot be edited (the room number never can). */
    @Transactional
    public Room update(Long id, RoomForm f) {
        Room r = get(id);
        if (r.getStatus() == RoomStatus.OCCUPIED) {
            throw new BusinessException("Rooms marked as 'Occupied' cannot be updated.");
        }
        if (hasActiveOrUpcomingReservations(r)) {
            throw new BusinessException("Room " + r.getRoomNumber() + " has active or upcoming reservations, so it cannot be updated.");
        }
        apply(r, f);
        return rooms.save(r);
    }

    @Transactional
    public void delete(Long id) {
        Room r = get(id);
        if (hasActiveOrUpcomingReservations(r)) {
            throw new BusinessException("Room " + r.getRoomNumber() + " has active or upcoming reservations and cannot be removed.");
        }
        if (bookings.existsByRoomId(r.getId())) {
            throw new BusinessException("Room " + r.getRoomNumber()
                    + " has booking history and cannot be removed. Set it to 'Under Maintenance' to stop new bookings.");
        }
        rooms.delete(r);
    }

    // ------------------------------------------------------------------ bulk CSV import

    public String csvTemplate() {
        return CSV_HEADER + "\n"
                + ",Standard,Queen,3500,2,Available,Wi-Fi|TV|Air Conditioning,Comfortable standard room\n"
                + ",Deluxe,King,5500,3,Available,Wi-Fi|TV|Mini-Bar|Air Conditioning,Spacious room with city view\n"
                + "250,Suite,King,9500,4,Available,Wi-Fi|TV|Mini-Bar|Air Conditioning|Bathtub,Leave roomNumber blank to auto-generate it\n";
    }

    /** All-or-nothing import: if any row is invalid nothing is saved and every problem is reported. */
    @Transactional
    public BulkImportResult importCsv(MultipartFile file) {
        BulkImportResult result = new BulkImportResult();
        if (file == null || file.isEmpty()) {
            result.getErrors().add("Please choose a CSV file to upload.");
            return result;
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv")) {
            result.getErrors().add("Only .csv files are supported.");
            return result;
        }
        List<List<String>> lines;
        try {
            lines = parseCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            result.getErrors().add("The file could not be read.");
            return result;
        }
        if (lines.isEmpty() || !lines.get(0).stream().map(s -> s.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toList())
                .equals(Arrays.asList(CSV_HEADER.toLowerCase(Locale.ROOT).split(",")))) {
            result.getErrors().add("Invalid header. Download the template and keep its first row: " + CSV_HEADER);
            return result;
        }
        if (lines.size() == 1) {
            result.getErrors().add("The file has no room rows.");
            return result;
        }
        if (lines.size() - 1 > MAX_CSV_ROWS) {
            result.getErrors().add("A file can contain at most " + MAX_CSV_ROWS + " rooms.");
            return result;
        }

        Set<String> usedNumbers = rooms.findAll().stream().map(Room::getRoomNumber).collect(Collectors.toCollection(HashSet::new));
        List<Room> toSave = new ArrayList<>();
        long nextNumber = Long.parseLong(nextRoomNumber());

        for (int i = 1; i < lines.size(); i++) {
            List<String> c = lines.get(i);
            int row = i + 1;
            while (c.size() < 8) c.add("");
            List<String> rowErrors = new ArrayList<>();
            Room r = new Room();

            String number = c.get(0).trim();
            if (number.isEmpty()) {
                while (usedNumbers.contains(String.valueOf(nextNumber))) nextNumber++;
                number = String.valueOf(nextNumber++);
            } else if (!number.matches("[A-Za-z0-9]{1,6}")) {
                rowErrors.add("Room Number must be 1-6 letters or digits.");
            } else if (usedNumbers.contains(number)) {
                rowErrors.add("Room Number '" + number + "' already exists. Room Number is required and must be unique.");
            }
            r.setRoomNumber(number);

            RoomType type = parseEnum(RoomType.values(), c.get(1));
            if (type == null) rowErrors.add("Room Type must be one of " + labels(RoomType.values()) + ".");
            r.setRoomType(type);

            String bed = c.get(2).trim();
            if (bed.length() > 30) rowErrors.add("Bed Type cannot exceed 30 characters.");
            r.setBedType(bed.isEmpty() ? null : bed);

            try {
                BigDecimal price = new BigDecimal(c.get(3).trim());
                if (price.signum() <= 0 || price.scale() > 2 || price.precision() - price.scale() > 8) throw new NumberFormatException();
                r.setPricePerNight(price.setScale(2));
            } catch (NumberFormatException e) {
                rowErrors.add("Price must be a positive number (max 2 decimals).");
            }

            try {
                int occ = Integer.parseInt(c.get(4).trim());
                if (occ < 1 || occ > 10) throw new NumberFormatException();
                r.setMaxOccupancy(occ);
            } catch (NumberFormatException e) {
                rowErrors.add("Max Occupancy must be a whole number from 1 to 10.");
            }

            String statusText = c.get(5).trim();
            RoomStatus status = statusText.isEmpty() ? RoomStatus.AVAILABLE : parseEnum(RoomStatus.values(), statusText);
            if (status == null || status == RoomStatus.OCCUPIED) rowErrors.add("Status must be Available or Under Maintenance.");
            r.setStatus(status == null ? RoomStatus.AVAILABLE : status);

            Set<String> amenities = new LinkedHashSet<>();
            for (String a : c.get(6).split("\\|")) {
                String t = a.trim();
                if (t.isEmpty()) continue;
                String match = AMENITIES.stream().filter(x -> x.equalsIgnoreCase(t)).findFirst().orElse(null);
                if (match == null) rowErrors.add("Unknown amenity '" + t + "'. Allowed: " + String.join(", ", AMENITIES) + ".");
                else amenities.add(match);
            }
            r.setAmenities(amenities);

            String desc = c.get(7).trim();
            if (desc.length() > 500) rowErrors.add("Description cannot exceed 500 characters.");
            r.setDescription(desc.isEmpty() ? null : desc);

            if (rowErrors.isEmpty()) {
                usedNumbers.add(number);
                toSave.add(r);
            } else {
                for (String e : rowErrors) result.getErrors().add("Row " + row + ": " + e);
            }
        }
        if (result.isSuccess()) {
            rooms.saveAll(toSave);
            result.setImported(toSave.size());
        }
        return result;
    }

    private static <E extends Enum<E>> E parseEnum(E[] values, String text) {
        String t = text == null ? "" : text.trim();
        for (E v : values) {
            String label = v instanceof RoomType rt ? rt.getLabel() : v instanceof RoomStatus rs ? rs.getLabel() : v.name();
            if (v.name().equalsIgnoreCase(t) || label.equalsIgnoreCase(t)) return v;
        }
        return null;
    }

    private static String labels(RoomType[] types) {
        return Arrays.stream(types).map(RoomType::getLabel).collect(Collectors.joining(", "));
    }

    /** Minimal RFC-4180 parser: quoted fields, escaped quotes and commas inside quotes. */
    static List<List<String>> parseCsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        if (text.startsWith("﻿")) text = text.substring(1);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                    else quoted = false;
                } else cell.append(ch);
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                row.add(cell.toString()); cell.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cell.toString()); cell.setLength(0);
                if (!(row.size() == 1 && row.get(0).isBlank())) rows.add(row);
                row = new ArrayList<>();
            } else cell.append(ch);
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            if (!(row.size() == 1 && row.get(0).isBlank())) rows.add(row);
        }
        return rows;
    }
}
