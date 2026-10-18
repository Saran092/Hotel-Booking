package com.hotel.booking.config;

import com.hotel.booking.model.*;
import com.hotel.booking.repository.RoomRepository;
import com.hotel.booking.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;

/** Creates demo accounts and rooms the first time the application starts on an empty database. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final RoomRepository rooms;
    private final PasswordEncoder encoder;
    private final HotelProperties props;

    public DataSeeder(UserRepository users, RoomRepository rooms, PasswordEncoder encoder, HotelProperties props) {
        this.users = users;
        this.rooms = rooms;
        this.encoder = encoder;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.isSeedDemoData() || users.count() > 0) return;

        user("admin", "Hotel Administrator", "admin@hotel.example", "Admin@123", Role.ADMIN, null);
        user("billing.staff", "Bhavna Billing", "billing@hotel.example", "Staff@123", Role.STAFF, StaffExpertise.BILLING);
        user("maintenance.staff", "Manoj Maintenance", "maintenance@hotel.example", "Staff@123", Role.STAFF, StaffExpertise.MAINTENANCE);
        user("housekeeping.staff", "Hema Housekeeping", "housekeeping@hotel.example", "Staff@123", Role.STAFF, StaffExpertise.HOUSEKEEPING);
        User c = user("customer1", "Demo Customer", "customer1@example.com", "Customer@123", Role.CUSTOMER, null);
        c.setCountryCode("+91");
        c.setMobile("9876543210");
        c.setAddress("14, Anna Salai, Chennai, Tamil Nadu");
        users.save(c);

        room("101", RoomType.STANDARD, "Queen", "3500", 2, "Wi-Fi,TV,Air Conditioning", "Comfortable standard room with a queen bed.");
        room("102", RoomType.STANDARD, "Twin", "3500", 2, "Wi-Fi,TV,Air Conditioning", "Standard room with two single beds.");
        room("103", RoomType.STANDARD, "Queen", "3800", 3, "Wi-Fi,TV,Air Conditioning,Coffee Maker", "Standard room with extra space for a child.");
        room("104", RoomType.SINGLE, "Single", "2200", 1, "Wi-Fi,TV", "Compact room for a solo traveller.");
        room("105", RoomType.DOUBLE, "Double", "3000", 2, "Wi-Fi,TV,Air Conditioning", "Double room with a work desk.");
        room("201", RoomType.DELUXE, "King", "5500", 3, "Wi-Fi,TV,Mini-Bar,Air Conditioning,Coffee Maker", "Spacious deluxe room with city view.");
        room("202", RoomType.DELUXE, "King", "5800", 3, "Wi-Fi,TV,Mini-Bar,Air Conditioning,Balcony", "Deluxe room with a private balcony.");
        room("203", RoomType.DELUXE, "Queen", "5200", 4, "Wi-Fi,TV,Mini-Bar,Air Conditioning", "Family-friendly deluxe room.");
        room("301", RoomType.SUITE, "King", "9500", 4, "Wi-Fi,TV,Mini-Bar,Air Conditioning,Bathtub,Room Service", "Suite with a separate living area.");
        room("302", RoomType.SUITE, "King", "10500", 4, "Wi-Fi,TV,Mini-Bar,Air Conditioning,Bathtub,Sea View,Room Service", "Sea-facing suite.");
        room("401", RoomType.SUPREME, "King", "14500", 5, "Wi-Fi,TV,Mini-Bar,Air Conditioning,Bathtub,Sea View,Balcony,Safe,Room Service", "Our finest room: panoramic sea view.");
        room("402", RoomType.SUPREME, "King", "15500", 5, "Wi-Fi,TV,Mini-Bar,Air Conditioning,Bathtub,Sea View,Balcony,Safe,Room Service", "Penthouse level supreme room.");

        log.info("""

                ==== Demo accounts (created on first start) ====
                admin               / Admin@123      (Admin)
                billing.staff       / Staff@123      (Staff - billing)
                maintenance.staff   / Staff@123      (Staff - maintenance)
                housekeeping.staff  / Staff@123      (Staff - housekeeping)
                customer1           / Customer@123   (Customer)
                =================================================""");
    }

    private User user(String username, String name, String email, String password, Role role, StaffExpertise expertise) {
        User u = new User();
        u.setUsername(username);
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.getRoles().add(role);
        u.setExpertise(expertise);
        return users.save(u);
    }

    private void room(String number, RoomType type, String bed, String price, int occupancy, String amenities, String description) {
        Room r = new Room();
        r.setRoomNumber(number);
        r.setRoomType(type);
        r.setBedType(bed);
        r.setPricePerNight(new BigDecimal(price));
        r.setMaxOccupancy(occupancy);
        r.setAmenities(new LinkedHashSet<>(Arrays.asList(amenities.split(","))));
        r.setDescription(description);
        rooms.save(r);
    }
}
