package com.nlsc.eventflow.config;

import com.nlsc.eventflow.model.AppUser;
import com.nlsc.eventflow.model.Event;
import com.nlsc.eventflow.repository.AppUserRepository;
import com.nlsc.eventflow.repository.EventRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {
    private final AppUserRepository users;
    private final EventRepository events;
    private final PasswordEncoder encoder;

    public DataSeeder(AppUserRepository users, EventRepository events, PasswordEncoder encoder) {
        this.users = users;
        this.events = events;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            users.saveAll(List.of(
                new AppUser("Aarav Sharma", "student@demo.com", "9000000001",
                    encoder.encode("demo123"), AppUser.Role.STUDENT),
                new AppUser("Diya Nair", "captain@demo.com", "9000000002",
                    encoder.encode("demo123"), AppUser.Role.STUDENT),
                new AppUser("Ishaan Rao", "ishaan@demo.com", "9000000003",
                    encoder.encode("demo123"), AppUser.Role.STUDENT),
                new AppUser("Meera Iyer", "meera@demo.com", "9000000004",
                    encoder.encode("demo123"), AppUser.Role.STUDENT),
                new AppUser("NLSC Admin", "admin@demo.com", "9000000010",
                    encoder.encode("admin123"), AppUser.Role.ADMIN),
                new AppUser("MVIT College", "college@demo.com", "9000000011",
                    encoder.encode("college123"), AppUser.Role.COLLEGE)
            ));
        }

        if (events.count() == 0) {
            LocalDate today = LocalDate.now();
            events.saveAll(List.of(
                new Event(
                    "EV Systems Sprint",
                    "Build and demonstrate a practical electric-mobility solution with a focus on reliability, usability and measurable impact.",
                    today,
                    "Innovation Hall · Block A",
                    Event.Status.OPEN,
                    "ev@nlsc.demo",
                    "9876501001"
                ),
                new Event(
                    "Battery Safety Workshop",
                    "A hands-on technical session covering battery packs, thermal safety, diagnostics and safe operating practices.",
                    today,
                    "E-Mobility Lab · Ground Floor",
                    Event.Status.ONGOING,
                    "battery@nlsc.demo",
                    "9876501002"
                ),
                new Event(
                    "Smart Mobility Design Challenge",
                    "Create a user-centred mobility concept and present a working prototype to the judging panel.",
                    today.plusDays(1),
                    "Design Studio · Block C",
                    Event.Status.UPCOMING,
                    "mobility@nlsc.demo",
                    "9876501003"
                ),
                new Event(
                    "Vehicle Dynamics Lab",
                    "Practical vehicle dynamics challenge involving handling, stability and performance observations.",
                    today.plusDays(2),
                    "Automotive Lab · Bay 2",
                    Event.Status.UPCOMING,
                    "dynamics@nlsc.demo",
                    "9876501004"
                ),
                new Event(
                    "Embedded Diagnostics",
                    "A compact challenge focused on embedded diagnostics, fault identification and clear technical communication.",
                    today.minusDays(1),
                    "Electronics Lab · Block B",
                    Event.Status.COMPLETED,
                    "diagnostics@nlsc.demo",
                    "9876501005"
                )
            ));
        }
    }
}
