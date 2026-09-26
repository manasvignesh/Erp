package com.nlsc.eventflow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlsc.eventflow.model.AppUser;
import com.nlsc.eventflow.model.Attendance;
import com.nlsc.eventflow.model.Event;
import com.nlsc.eventflow.model.Team;
import com.nlsc.eventflow.repository.AppUserRepository;
import com.nlsc.eventflow.repository.AttendanceRepository;
import com.nlsc.eventflow.repository.EventRepository;
import com.nlsc.eventflow.repository.TeamRepository;
import com.nlsc.eventflow.service.QrCodeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final AppUserRepository users;
    private final EventRepository events;
    private final TeamRepository teams;
    private final AttendanceRepository attendance;
    private final PasswordEncoder encoder;
    private final ObjectMapper mapper;
    private final QrCodeService qrCodeService;

    public ApiController(
            AppUserRepository users,
            EventRepository events,
            TeamRepository teams,
            AttendanceRepository attendance,
            PasswordEncoder encoder,
            ObjectMapper mapper,
            QrCodeService qrCodeService) {
        this.users = users;
        this.events = events;
        this.teams = teams;
        this.attendance = attendance;
        this.encoder = encoder;
        this.mapper = mapper;
        this.qrCodeService = qrCodeService;
    }

    // Simple demo login for the competition MVP.
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        AppUser user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!encoder.matches(request.password(), user.passwordHash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return publicUser(user);
    }

    @GetMapping("/events")
    public List<Event> listEvents(@RequestParam(defaultValue = "") String search) {
        String q = search.trim().toLowerCase();

        return events.findAllByOrderByEventDateAsc().stream()
                .filter(event ->
                        q.isBlank()
                                || event.eventName.toLowerCase().contains(q)
                                || event.eventDate.toString().contains(q))
                .toList();
    }

    @GetMapping("/events/{id}")
    public Event event(@PathVariable Long id) {
        return events.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @GetMapping("/users")
    public List<Map<String, Object>> searchUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) Long exclude) {

        String q = search.trim().toLowerCase();

        return users.findAll().stream()
                .filter(user -> exclude == null || !user.id.equals(exclude))
                .filter(user -> user.role == AppUser.Role.STUDENT)
                .filter(user ->
                        q.isBlank()
                                || user.name.toLowerCase().contains(q)
                                || user.email.toLowerCase().contains(q))
                .map(this::publicUser)
                .toList();
    }

    @PostMapping("/teams")
    public Team createTeam(@RequestBody TeamRequest request) {
        if (request.teamName() == null || request.teamName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team name is required");
        }

        events.findById(request.eventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        users.findById(request.captainId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Captain not found"));

        if (teams.findByEventIdAndCaptainId(request.eventId(), request.captainId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already created a team for this event");
        }

        List<Long> memberIds = request.memberIds() == null
                ? new ArrayList<>()
                : new ArrayList<>(new LinkedHashSet<>(request.memberIds()));

        memberIds.remove(request.captainId());

        // Sheet says maximum 3 members per team.
        // Change this single number if the organiser clarifies that captain is excluded.
        if (memberIds.size() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maximum team size is 3 including captain");
        }

        for (Long memberId : memberIds) {
            users.findById(memberId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid team member"));
        }

        Team team = new Team();
        team.eventId = request.eventId();
        team.teamName = request.teamName().trim();
        team.captainId = request.captainId();

        try {
            team.memberIdsJson = mapper.writeValueAsString(memberIds);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not save team members");
        }

        return teams.save(team);
    }

    @GetMapping("/events/{eventId}/team")
    public Map<String, Object> myTeam(@PathVariable Long eventId, @RequestParam Long captainId) {
        Team team = teams.findByEventIdAndCaptainId(eventId, captainId).orElse(null);

        if (team == null) {
            return Map.of("registered", false);
        }

        return Map.of(
                "registered", true,
                "team", team,
                "members", teamMembers(team)
        );
    }

    @GetMapping("/events/{eventId}/teams")
    public List<Map<String, Object>> eventTeams(@PathVariable Long eventId) {
        return teams.findByEventIdOrderByCreatedAtAsc(eventId).stream()
                .map(team -> Map.<String, Object>of(
                        "team", team,
                        "captain", users.findById(team.captainId).map(this::publicUser).orElse(Map.of()),
                        "members", teamMembers(team)
                ))
                .toList();
    }

    // Bonus module: QR is generated only on the event day.
    @GetMapping("/events/{eventId}/qr")
    public Map<String, String> qr(@PathVariable Long eventId) {
        Event event = events.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (!LocalDate.now().equals(event.eventDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR is available only on the event day");
        }

        if (event.attendanceCode == null || !LocalDate.now().equals(event.attendanceCodeDate)) {
            event.attendanceCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            event.attendanceCodeDate = LocalDate.now();
            events.save(event);
        }

        return Map.of(
                "code", event.attendanceCode,
                "qr", qrCodeService.asDataUrl(event.attendanceCode)
        );
    }

    @PostMapping("/attendance")
    public Attendance markAttendance(@RequestBody AttendanceRequest request) {
        Event event = events.findById(request.eventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (!LocalDate.now().equals(event.eventDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attendance can be marked only on the event day");
        }

        if (event.attendanceCode == null || !event.attendanceCode.equalsIgnoreCase(request.code())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid QR code");
        }

        users.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return attendance.findByEventIdAndUserId(request.eventId(), request.userId())
                .orElseGet(() -> attendance.save(new Attendance(request.eventId(), request.userId())));
    }

    private List<Map<String, Object>> teamMembers(Team team) {
        try {
            Long[] ids = mapper.readValue(team.memberIdsJson, Long[].class);
            List<Map<String, Object>> result = new ArrayList<>();
            result.add(users.findById(team.captainId).map(this::publicUser).orElse(Map.of()));
            for (Long id : ids) {
                users.findById(id).ifPresent(user -> result.add(publicUser(user)));
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> publicUser(AppUser user) {
        return Map.of(
                "id", user.id,
                "name", user.name,
                "email", user.email,
                "mobile", user.mobile,
                "role", user.role.name()
        );
    }

    public record LoginRequest(String email, String password) {}
    public record TeamRequest(String teamName, Long eventId, Long captainId, List<Long> memberIds) {}
    public record AttendanceRequest(Long eventId, Long userId, String code) {}
}
