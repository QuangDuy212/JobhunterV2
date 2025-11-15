package vn.hoidanit.jobhunter.controller;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.Role;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.RoleRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;

import java.time.Instant;
import java.util.List;

@Component
public class ScheduleController {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public ScheduleController(CompanyRepository companyRepository, UserRepository userRepository, RoleRepository roleRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupSoftDeletedCompaniesAndUsers() {
        Instant time = Instant.now().minusSeconds(60 * 60);

        List<User> usersToDelete = userRepository.findAll().stream()
                .filter(user -> user.getStatus() == 0 && user.getUpdatedAt().isBefore(time) )
                .toList();
        userRepository.deleteAll(usersToDelete);

        List<Company> companiesToDelete = companyRepository.findAll().stream()
                .filter(company -> company.getStatus() == 0 && company.getUpdatedAt().isBefore(time) )
                .toList();
        companyRepository.deleteAll(companiesToDelete);
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupSoftDeletedRoles() {
        Instant time = Instant.now().minusSeconds(60 * 60);
        List<Role> rolesToDelete = roleRepository.findAll().stream()
                .filter(role -> role.getStatus() == 0 && role.getUpdatedAt().isBefore(time))
                .toList();
        roleRepository.deleteAll(rolesToDelete);
    }
}