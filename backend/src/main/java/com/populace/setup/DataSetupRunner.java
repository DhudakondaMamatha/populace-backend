package com.populace.setup;

import com.populace.domain.*;
import com.populace.domain.enums.*;
import com.populace.leave.service.LeaveTypeInitializer;
import com.populace.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Component
@Profile("setup")
public class DataSetupRunner implements CommandLineRunner {

    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final SiteRepository siteRepository;
    private final RoleRepository roleRepository;
    private final StaffMemberRepository staffMemberRepository;
    private final StaffRoleRepository staffRoleRepository;
    private final StaffSiteRepository staffSiteRepository;
    private final StaffCompensationRepository compensationRepository;
    private final ShiftRepository shiftRepository;
    private final PasswordEncoder passwordEncoder;
    private final LeaveTypeInitializer leaveTypeInitializer;

    private Business business;
    private final List<Site> sites = new ArrayList<>();
    private final List<Role> roles = new ArrayList<>();
    private final List<StaffMember> employees = new ArrayList<>();
    private final List<Shift> shifts = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private String adminPassword;

    public DataSetupRunner(
            BusinessRepository businessRepository,
            UserRepository userRepository,
            SiteRepository siteRepository,
            RoleRepository roleRepository,
            StaffMemberRepository staffMemberRepository,
            StaffRoleRepository staffRoleRepository,
            StaffSiteRepository staffSiteRepository,
            StaffCompensationRepository compensationRepository,
            ShiftRepository shiftRepository,
            PasswordEncoder passwordEncoder,
            LeaveTypeInitializer leaveTypeInitializer) {
        this.businessRepository = businessRepository;
        this.userRepository = userRepository;
        this.siteRepository = siteRepository;
        this.roleRepository = roleRepository;
        this.staffMemberRepository = staffMemberRepository;
        this.staffRoleRepository = staffRoleRepository;
        this.staffSiteRepository = staffSiteRepository;
        this.compensationRepository = compensationRepository;
        this.shiftRepository = shiftRepository;
        this.passwordEncoder = passwordEncoder;
        this.leaveTypeInitializer = leaveTypeInitializer;
    }

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("STARTING DATA SETUP");
        System.out.println("=".repeat(60));

        try {
            createBusiness();
            createAdminUser();
            leaveTypeInitializer.initializeDefaultLeaveTypes(business);
            createSites();
            createRoles();
            createEmployees();
            createShifts();

            printSummary();

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            warnings.add("Setup failed: " + e.getMessage());
        }
    }

    private void createBusiness() {
        business = new Business();
        business.setName("Populace Restaurant Group");
        business.setBusinessCode("PRG" + System.currentTimeMillis() % 10000);
        business.setEmail("admin@populace-restaurant.com");
        business.setPhone("040-12345678");
        business.setToleranceOverPercentage(new BigDecimal("3"));
        business.setToleranceUnderPercentage(new BigDecimal("10"));
        business = businessRepository.save(business);
        System.out.println("Created business: " + business.getName());
    }

    private void createAdminUser() {
        adminPassword = "Test@123";  // Fixed password for testing

        User admin = new User();
        admin.setBusiness(business);
        admin.setEmail("admin@populace.com");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setFirstName("System");
        admin.setLastName("Admin");
        admin.setPhone("040-00000001");
        admin.setUserType(UserType.admin);
        admin.setActive(true);
        admin.setEmailVerifiedAt(Instant.now());
        userRepository.save(admin);

        System.out.println("Created admin user: " + admin.getEmail());
    }

    private void createSites() {
        String[][] siteData = {
            {"Gachibowli", "GACHI", "Plot 123, Gachibowli IT Park", "Hyderabad", "Telangana", "500032"},
            {"Manikonda", "MANIK", "House 45, Manikonda Main Road", "Hyderabad", "Telangana", "500089"},
            {"Madhapur", "MADHA", "Cyber Towers, Madhapur", "Hyderabad", "Telangana", "500081"},
            {"Kukatpally", "KUKAT", "KPHB Colony, Kukatpally", "Hyderabad", "Telangana", "500072"}
        };

        for (String[] data : siteData) {
            Site site = new Site();
            site.setBusiness(business);
            site.setName(data[0]);
            site.setCode(data[1]);
            site.setAddress(data[2]);
            site.setCity(data[3]);
            site.setState(data[4]);
            site.setPostalCode(data[5]);
            site.setCountry("India");
            site.setContactName("Site Manager");
            site.setContactEmail(data[1].toLowerCase() + "@populace.com");
            site.setContactPhone("040-" + (1000 + sites.size()));
            site.setActive(true);
            sites.add(siteRepository.save(site));
        }
        System.out.println("Created " + sites.size() + " sites");
    }

    private void createRoles() {
        // Role: Name, Code, Color (post-refactor: Role is metadata-only)
        Object[][] roleData = {
            {"Chef", "CHEF", "#FF5733"},
            {"Manager", "MGR", "#3498DB"},
            {"Waiter", "WAIT", "#2ECC71"},
            {"Cleaner", "CLN", "#9B59B6"}
        };

        for (Object[] data : roleData) {
            Role role = new Role();
            role.setBusiness(business);
            role.setName((String) data[0]);
            role.setCode((String) data[1]);
            role.setColor((String) data[2]);
            role.setActive(true);
            // Note: Constraint fields have been moved to Staff (personal) and Business (system)
            roles.add(roleRepository.save(role));
        }
        System.out.println("Created " + roles.size() + " roles");
    }

    private void createEmployees() {
        Role chef = roles.get(0);
        Role manager = roles.get(1);
        Role waiter = roles.get(2);
        Role cleaner = roles.get(3);

        String[] firstNames = {"Rahul", "Priya", "Amit", "Sneha", "Vikram",
                               "Anjali", "Ravi", "Meera", "Suresh", "Kavitha",
                               "Deepak", "Lakshmi", "Arjun", "Divya", "Prakash",
                               "Swathi", "Kiran", "Pooja", "Rajesh", "Nandini"};

        String[] lastNames = {"Kumar", "Sharma", "Patel", "Reddy", "Singh"};

        int empIndex = 0;
        Random random = new Random(42); // Fixed seed for reproducibility

        for (int siteIndex = 0; siteIndex < sites.size(); siteIndex++) {
            Site site = sites.get(siteIndex);

            for (int i = 0; i < 5; i++) {
                String firstName = firstNames[empIndex % firstNames.length];
                String lastName = lastNames[empIndex % lastNames.length];

                StaffMember emp = createEmployee(firstName, lastName, empIndex);

                // Role assignments based on pattern
                if (i == 0) {
                    // Waiter + Cleaner
                    assignRole(emp, waiter, ProficiencyLevel.competent, true);
                    assignRole(emp, cleaner, ProficiencyLevel.trainee, false);
                    createCompensation(emp, waiter, randomWage(random, 200, 300));
                    createCompensation(emp, cleaner, randomWage(random, 150, 200));
                } else if (i == 1) {
                    // Manager + Waiter
                    assignRole(emp, manager, ProficiencyLevel.expert, true);
                    assignRole(emp, waiter, ProficiencyLevel.competent, false);
                    createCompensation(emp, manager, randomWage(random, 400, 600));
                    createCompensation(emp, waiter, randomWage(random, 250, 350));
                } else if (i == 2) {
                    // Chef only
                    assignRole(emp, chef, ProficiencyLevel.expert, true);
                    createCompensation(emp, chef, randomWage(random, 350, 500));
                } else if (i == 3) {
                    // Waiter only
                    assignRole(emp, waiter, ProficiencyLevel.competent, true);
                    createCompensation(emp, waiter, randomWage(random, 200, 300));
                } else {
                    // Cleaner only
                    assignRole(emp, cleaner, ProficiencyLevel.trainee, true);
                    createCompensation(emp, cleaner, randomWage(random, 150, 200));
                }

                // Site assignment - primary site
                assignSite(emp, site, SitePreference.primary);

                // First employee of first site can work at ALL sites
                if (siteIndex == 0 && i == 0) {
                    for (int j = 1; j < sites.size(); j++) {
                        assignSite(emp, sites.get(j), SitePreference.secondary);
                    }
                }

                employees.add(emp);
                empIndex++;
            }
        }
        System.out.println("Created " + employees.size() + " employees");
    }

    private StaffMember createEmployee(String firstName, String lastName, int index) {
        StaffMember emp = new StaffMember();
        emp.setBusiness(business);
        emp.setFirstName(firstName);
        emp.setLastName(lastName);
        emp.setEmail(firstName.toLowerCase() + index + "@populace.com");
        emp.setEmploymentStatus(EmploymentStatus.active);
        return staffMemberRepository.save(emp);
    }

    private void assignRole(StaffMember staff, Role role, ProficiencyLevel level, boolean primary) {
        StaffRole sr = new StaffRole();
        sr.setStaff(staff);
        sr.setRole(role);
        sr.setProficiencyLevel(level);
        sr.setPrimary(primary);
        sr.setActive(true);
        sr.setCertifiedAt(LocalDate.now().minusMonths(6));
        staffRoleRepository.save(sr);
    }

    private void assignSite(StaffMember staff, Site site, SitePreference preference) {
        StaffSite ss = new StaffSite();
        ss.setStaff(staff);
        ss.setSite(site);
        ss.setPreference(preference);
        ss.setActive(true);
        staffSiteRepository.save(ss);
    }

    private void createCompensation(StaffMember staff, Role role, BigDecimal hourlyRate) {
        StaffCompensation comp = new StaffCompensation();
        comp.setStaff(staff);
        comp.setRole(role);
        comp.setHourlyRate(hourlyRate);
        comp.setCompensationType(CompensationType.hourly);
        comp.setEffectiveFrom(LocalDate.now().minusYears(1));
        compensationRepository.save(comp);
    }

    private BigDecimal randomWage(Random random, int min, int max) {
        return new BigDecimal(min + random.nextInt(max - min));
    }

    private void createShifts() {
        LocalDate shiftDate = LocalDate.now().plusDays(1);

        Role chef = roles.get(0);
        Role manager = roles.get(1);
        Role waiter = roles.get(2);
        Role cleaner = roles.get(3);

        for (Site site : sites) {
            // Chef shift: 08:00-16:00
            shifts.add(createShift(site, chef, shiftDate, LocalTime.of(8, 0), LocalTime.of(16, 0), 1));

            // Manager shift: 09:00-17:00 (overlaps with chef)
            shifts.add(createShift(site, manager, shiftDate, LocalTime.of(9, 0), LocalTime.of(17, 0), 1));

            // Waiter shift: 10:00-18:00 (overlaps with manager)
            shifts.add(createShift(site, waiter, shiftDate, LocalTime.of(10, 0), LocalTime.of(18, 0), 2));

            // Cleaner shift: 14:00-22:00 (overlaps with waiter)
            shifts.add(createShift(site, cleaner, shiftDate, LocalTime.of(14, 0), LocalTime.of(22, 0), 1));
        }
        System.out.println("Created " + shifts.size() + " shifts");
    }

    private Shift createShift(Site site, Role role, LocalDate date, LocalTime start, LocalTime end, int required) {
        Shift shift = new Shift();
        shift.setBusiness(business);
        shift.setSite(site);
        shift.setRole(role);
        shift.setShiftDate(date);
        shift.setStartTime(start);
        shift.setEndTime(end);
        shift.setStaffRequired(required);
        shift.setStaffAllocated(0);
        shift.setStatus(ShiftStatus.open);
        shift.setBreakDurationMinutes(30);
        return shiftRepository.save(shift);
    }

    private void printSummary() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ADMIN CREDENTIALS");
        System.out.println("=".repeat(60));
        System.out.println("Email:    admin@populace.com");
        System.out.println("Password: " + adminPassword);
        System.out.println();

        System.out.println("=".repeat(60));
        System.out.println("SUMMARY");
        System.out.println("=".repeat(60));
        System.out.println("Total Sites:     " + sites.size());
        System.out.println("Total Roles:     " + roles.size());
        System.out.println("Total Employees: " + employees.size());
        System.out.println("Total Shifts:    " + shifts.size());

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("DATA CREATION STATUS");
        System.out.println("=".repeat(60));
        if (warnings.isEmpty()) {
            System.out.println("Status: SUCCESS - No errors or warnings");
        } else {
            System.out.println("Warnings:");
            for (String warning : warnings) {
                System.out.println("  - " + warning);
            }
        }
        System.out.println("=".repeat(60) + "\n");
    }

    private String generatePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
