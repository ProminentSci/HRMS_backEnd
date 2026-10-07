package com.employee.management.backend.service;

import com.employee.management.backend.Entity.Employee;
import com.employee.management.backend.Entity.JobDetails;
import com.employee.management.backend.dto.BirthdayDTO;
import com.employee.management.backend.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Birthdays: who is celebrating today (for the employee dashboard card, scoped per client) and
 * the daily birthday email, which runs at midnight (server time unless app.birthday.zone is set).
 * The actual SMTP send is async via EmailService, so a slow mail server never stalls the
 * scheduler thread.
 */
@Service
@Transactional
public class BirthdayService {

    // dateOfBirth is a free-form string column. The UI's date input saves ISO (yyyy-MM-dd);
    // the others cover data entered or imported by hand.
    private static final List<DateTimeFormatter> DOB_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
    );

    private static final MonthDay LEAP_DAY = MonthDay.of(2, 29);

    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;
    private final String mailFrom;
    private final ZoneId zone;

    public BirthdayService(EmployeeRepository employeeRepository,
                           EmailService emailService,
                           @Value("${app.mail.from}") String mailFrom,
                           @Value("${app.birthday.zone:}") String zone) {
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
        this.mailFrom = mailFrom;
        // Same zone the cron fires in, so "today" means the day that just started.
        this.zone = zone == null || zone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(zone.trim());
    }

    // Active employees of one client whose birthday is today, sorted by name.
    public List<BirthdayDTO> getTodaysBirthdays(Long clientId) {
        if (clientId == null) {
            return List.of();
        }
        LocalDate today = LocalDate.now(zone);
        return employeeRepository.findActiveWithDateOfBirthForClient(clientId).stream()
                .filter(employee -> isBirthdayToday(employee, today))
                .map(this::toDTO)
                .sorted(Comparator.comparing(BirthdayDTO::name, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    @Scheduled(cron = "${app.birthday.cron:0 0 0 * * *}", zone = "${app.birthday.zone:}")
    public void sendTodaysBirthdayEmails() {
        LocalDate today = LocalDate.now(zone);
        int sent = 0;
        for (Employee employee : employeeRepository.findActiveWithDateOfBirth()) {
            if (!isBirthdayToday(employee, today)) {
                continue;
            }
            if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
                continue;
            }
            sendBirthdayEmail(employee);
            sent++;
        }
        System.out.println("Birthday job for " + today + ": queued " + sent + " email(s)");
    }

    private boolean isBirthdayToday(Employee employee, LocalDate today) {
        MonthDay birthday = parseBirthday(employee.getDateOfBirth());
        if (birthday == null) {
            return false;
        }
        // Feb 29 birthdays are celebrated on Feb 28 in non-leap years.
        if (birthday.equals(LEAP_DAY) && !today.isLeapYear()) {
            return MonthDay.from(today).equals(MonthDay.of(2, 28));
        }
        return birthday.equals(MonthDay.from(today));
    }

    private MonthDay parseBirthday(String dateOfBirth) {
        if (dateOfBirth == null) {
            return null;
        }
        String value = dateOfBirth.trim();
        for (DateTimeFormatter format : DOB_FORMATS) {
            try {
                return MonthDay.from(LocalDate.parse(value, format));
            } catch (DateTimeParseException ignored) {
                // try the next format
            }
        }
        return null;
    }

    private String fullName(Employee employee) {
        return String.format("%s %s",
                employee.getFirstName() == null ? "" : employee.getFirstName(),
                employee.getLastName() == null ? "" : employee.getLastName()).trim();
    }

    private String companyName(Employee employee) {
        return employee.getClient() != null && employee.getClient().getCompanyName() != null
                && !employee.getClient().getCompanyName().trim().isEmpty()
                ? employee.getClient().getCompanyName().trim() : null;
    }

    private BirthdayDTO toDTO(Employee employee) {
        JobDetails job = employee.getJobDetails();
        return new BirthdayDTO(
                employee.getEmpId(),
                fullName(employee),
                job != null ? job.getDesignation() : null,
                job != null ? job.getDepartment() : null,
                employee.getProfilePhoto(),
                companyName(employee)
        );
    }

    private void sendBirthdayEmail(Employee employee) {
        String employeeName = fullName(employee);
        String companyName = companyName(employee);

        emailService.sendHtmlEmail(employee.getEmail(), mailFrom, null,
                "Happy Birthday, " + (employeeName.isEmpty() ? "from all of us" : employeeName) + "! 🎂",
                buildBirthdayBody(employeeName, companyName == null ? "Team" : companyName));
    }

    private String buildBirthdayBody(String employeeName, String companyName) {
        String name = HtmlUtils.htmlEscape(employeeName.isEmpty() ? "there" : employeeName);
        String company = HtmlUtils.htmlEscape(companyName);
        return "<p style=\"font-size:18px;\">🎉🎂 <strong>Happy Birthday, " + name + "!</strong> 🎂🎉</p>"
                + "<p>The entire <strong>" + company + "</strong> wishes you a very happy birthday! 🥳</p>"
                + "<p>We hope you have a fantastic day and an amazing year ahead filled with "
                + "<strong>success, happiness, good health, and new achievements</strong>.</p>"
                + "<p>Keep growing, keep inspiring, and keep being a great part of our team! 🌟</p>"
                + "<p><strong>Have a wonderful birthday! 🎁🎈</strong></p>";
    }
}
