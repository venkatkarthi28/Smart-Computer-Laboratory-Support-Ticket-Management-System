package com.example.labsupport.config;

import com.example.labsupport.entity.Computer;
import com.example.labsupport.entity.ComputerStatus;
import com.example.labsupport.entity.Laboratory;
import com.example.labsupport.entity.Role;
import com.example.labsupport.entity.TicketCategory;
import com.example.labsupport.entity.TicketPriority;
import com.example.labsupport.entity.User;
import com.example.labsupport.repository.ComputerRepository;
import com.example.labsupport.repository.LaboratoryRepository;
import com.example.labsupport.repository.TicketCategoryRepository;
import com.example.labsupport.repository.TicketPriorityRepository;
import com.example.labsupport.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs at every startup and creates demo data ONLY if it does not exist yet (safe to run many times).
 * Demo users are created only when SEED_PASSWORD is set in the .env file.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final ComputerRepository computerRepository;
    private final TicketCategoryRepository categoryRepository;
    private final TicketPriorityRepository priorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DataSeeder(UserRepository userRepository, LaboratoryRepository laboratoryRepository,
                      ComputerRepository computerRepository, TicketCategoryRepository categoryRepository,
                      TicketPriorityRepository priorityRepository, PasswordEncoder passwordEncoder,
                      @Value("${app.seed.password:}") String seedPassword) {
        this.userRepository = userRepository;
        this.laboratoryRepository = laboratoryRepository;
        this.computerRepository = computerRepository;
        this.categoryRepository = categoryRepository;
        this.priorityRepository = priorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedCategories();
        seedPriorities();

        User technician = null;
        if (seedPassword == null || seedPassword.isBlank()) {
            log.info("SEED_PASSWORD is not set in .env: demo users were NOT created");
        } else {
            seedUser("System Administrator", "admin@college.com", Role.ADMIN);
            technician = seedUser("Lab Technician", "technician@college.com", Role.TECHNICIAN);
            seedUser("Demo Student", "student@college.com", Role.STUDENT);
        }
        seedLaboratory(technician);
    }

    private void seedCategories() {
        String[] names = {"NETWORK", "HARDWARE", "SOFTWARE", "OPERATING_SYSTEM", "KEYBOARD",
                "MOUSE", "MONITOR", "PRINTER", "LOGIN_ACCOUNT", "OTHER"};
        for (String name : names) {
            if (!categoryRepository.existsByNameIgnoreCase(name)) {
                TicketCategory c = new TicketCategory();
                c.setName(name);
                c.setDescription(name.replace('_', ' ').toLowerCase() + " problems");
                c.setActive(true);
                categoryRepository.save(c);
            }
        }
    }

    private void seedPriorities() {
        Object[][] rows = {{"LOW", 72, 1}, {"MEDIUM", 24, 2}, {"HIGH", 4, 3}, {"CRITICAL", 1, 4}};
        for (Object[] row : rows) {
            String name = (String) row[0];
            if (!priorityRepository.existsByNameIgnoreCase(name)) {
                TicketPriority p = new TicketPriority();
                p.setName(name);
                p.setSlaHours((Integer) row[1]);
                p.setSeverityLevel((Integer) row[2]);
                priorityRepository.save(p);
            }
        }
    }

    private User seedUser(String fullName, String email, Role role) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User u = new User();
            u.setFullName(fullName);
            u.setEmail(email);
            u.setPasswordHash(passwordEncoder.encode(seedPassword));
            u.setRole(role);
            u.setActive(true);
            return userRepository.save(u);
        });
        log.info("Demo account: {} (id={}, role={})", email, user.getId(), role);
        return user;
    }

    private void seedLaboratory(User technician) {
        Laboratory lab = laboratoryRepository.findByNameIgnoreCase("Laboratory 1").orElseGet(() -> {
            Laboratory l = new Laboratory();
            l.setName("Laboratory 1");
            l.setLocation("Main Block, Ground Floor");
            l.setDescription("General purpose computer laboratory");
            return laboratoryRepository.save(l);
        });
        if (technician != null) {
            Laboratory withTechs = laboratoryRepository.findWithTechniciansById(lab.getId()).orElse(lab);
            boolean already = withTechs.getTechnicians().stream()
                    .anyMatch(t -> t.getId().equals(technician.getId()));
            if (!already) {
                withTechs.addTechnician(technician);
                laboratoryRepository.save(withTechs);
            }
            lab = withTechs;
        }
        for (int i = 1; i <= 10; i++) {
            String code = String.format("LAB1-PC-%03d", i);
            if (!computerRepository.existsByComputerCodeIgnoreCase(code)) {
                Computer c = new Computer();
                c.setComputerCode(code);
                c.setComputerName(String.format("PC %03d", i));
                c.setLaboratory(lab);
                c.setBrand("Dell");
                c.setModel("OptiPlex 3080");
                c.setProcessor("Intel Core i5-10500");
                c.setRam("8 GB");
                c.setStorageCapacity("256 GB SSD");
                c.setOperatingSystem("Windows 11 Pro");
                c.setIpAddress("192.168.1." + (10 + i));
                c.setStatus(ComputerStatus.WORKING);
                computerRepository.save(c);
            }
        }
    }
}
