package vn.hoidanit.jobhunter.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.Permission;
import vn.hoidanit.jobhunter.domain.Role;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.repository.PermissionRepository;
import vn.hoidanit.jobhunter.repository.RoleRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.constant.GenderEnum;

@Service
public class DatabaseInitializer implements CommandLineRunner {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(PermissionRepository permissionRepository, RoleRepository roleRepository,
            UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> START INIT DATABASE");
        long countPermissions = this.permissionRepository.count();
        long countRoles = this.roleRepository.count();
        long countUsers = this.userRepository.count();

        if (countPermissions == 0) {
            ArrayList<Permission> arr = new ArrayList<>();
            arr.add(new Permission("Create a company", "/api/v1/companies", "POST", "COMPANIES"));
            arr.add(new Permission("Update a company", "/api/v1/companies", "PUT", "COMPANIES"));
            arr.add(new Permission("Delete a company", "/api/v1/companies/{id}", "DELETE", "COMPANIES"));
            arr.add(new Permission("Get a company by id", "/api/v1/companies/{id}", "GET", "COMPANIES"));
            arr.add(new Permission("Get companies with pagination", "/api/v1/companies", "GET", "COMPANIES"));
            arr.add(new Permission("Get count all companies", "/api/v1/companies/count-all-companies", "GET",
                    "COMPANIES"));
            arr.add(new Permission("Hard delete a company", "/api/v1/companies/hard/{id}", "DELETE", "COMPANIES"));
            arr.add(new Permission("Restore a company", "/api/v1/companies/restore/{id}", "PUT", "COMPANIES"));
            arr.add(new Permission("Fetch all deleted companies", "/api/v1/companies/deleted", "GET", "COMPANIES"));

            arr.add(new Permission("Create a job", "/api/v1/jobs", "POST", "JOBS"));
            arr.add(new Permission("Update a job", "/api/v1/jobs", "PUT", "JOBS"));
            arr.add(new Permission("Delete a job", "/api/v1/jobs/{id}", "DELETE", "JOBS"));
            arr.add(new Permission("Get a job by id", "/api/v1/jobs/fetch-job-detail/{id}", "GET", "JOBS"));
            arr.add(new Permission("Get jobs with pagination", "/api/v1/jobs", "GET", "JOBS"));
            arr.add(new Permission("Get jobs for admin", "/api/v1/admin-jobs", "GET", "JOBS"));
            arr.add(new Permission("Get count all jobs", "/api/v1/jobs/count-all-jobs", "GET", "JOBS"));
            arr.add(new Permission("Get jobs by skill id", "/api/v1/jobs/fetch-by-skill/{id}", "GET", "JOBS"));
            arr.add(new Permission("Hard delete a job", "/api/v1/jobs/hard/{id}", "DELETE", "JOBS"));
            arr.add(new Permission("Restore a job", "/api/v1/jobs/restore/{id}", "PUT", "JOBS"));
            arr.add(new Permission("Fetch all deleted jobs", "/api/v1/jobs/deleted", "GET", "JOBS"));

            arr.add(new Permission("Create a permission", "/api/v1/permissions", "POST", "PERMISSIONS"));
            arr.add(new Permission("Update a permission", "/api/v1/permissions", "PUT", "PERMISSIONS"));
            arr.add(new Permission("Delete a permission", "/api/v1/permissions/{id}", "DELETE", "PERMISSIONS"));
            arr.add(new Permission("Get a permission by id", "/api/v1/permissions/{id}", "GET", "PERMISSIONS"));
            arr.add(new Permission("Get permissions with pagination", "/api/v1/permissions", "GET", "PERMISSIONS"));
            arr.add(new Permission("Hard delete a permission", "/api/v1/permissions/hard/{id}", "DELETE",
                    "PERMISSIONS"));
            arr.add(new Permission("Restore a permission", "/api/v1/permissions/restore/{id}", "PUT", "PERMISSIONS"));
            arr.add(new Permission("Fetch all deleted permissions", "/api/v1/permissions/deleted", "GET",
                    "PERMISSIONS"));

            arr.add(new Permission("Create a resume", "/api/v1/resumes", "POST", "RESUMES"));
            arr.add(new Permission("Update a resume", "/api/v1/resumes", "PUT", "RESUMES"));
            arr.add(new Permission("Delete a resume", "/api/v1/resumes/{id}", "DELETE", "RESUMES"));
            arr.add(new Permission("Get a resume by id", "/api/v1/resumes/{id}", "GET", "RESUMES"));
            arr.add(new Permission("Get resumes with pagination", "/api/v1/resumes", "GET", "RESUMES"));
            arr.add(new Permission("Get count all resumes", "/api/v1/resumes/count-all-resumes", "GET", "RESUMES"));
            arr.add(new Permission("Get count resumes by time", "/api/v1/resumes/count-resumes-by-time", "GET",
                    "RESUMES"));
            arr.add(new Permission("Get count resumes by status", "/api/v1/resumes/count/count-by-status", "GET",
                    "RESUMES"));
            arr.add(new Permission("Hard delete a resume", "/api/v1/resumes/hard/{id}", "DELETE", "RESUMES"));
            arr.add(new Permission("Fetch all deleted resumes", "/api/v1/resumes/deleted", "GET", "RESUMES"));
            arr.add(new Permission("Restore a resume", "/api/v1/resumes/restore/{id}", "PUT", "RESUMES"));

            arr.add(new Permission("Create a role", "/api/v1/roles", "POST", "ROLES"));
            arr.add(new Permission("Update a role", "/api/v1/roles", "PUT", "ROLES"));
            arr.add(new Permission("Delete a role", "/api/v1/roles/{id}", "DELETE", "ROLES"));
            arr.add(new Permission("Get a role by id", "/api/v1/roles/{id}", "GET", "ROLES"));
            arr.add(new Permission("Get roles with pagination", "/api/v1/roles", "GET", "ROLES"));
            arr.add(new Permission("Hard delete a role", "/api/v1/roles/hard/{id}", "DELETE", "ROLES"));
            arr.add(new Permission("Restore a role", "/api/v1/roles/restore/{id}", "PUT", "ROLES"));
            arr.add(new Permission("Fetch all deleted roles", "/api/v1/roles/deleted", "GET", "ROLES"));

            arr.add(new Permission("Create a user", "/api/v1/users", "POST", "USERS"));
            arr.add(new Permission("Change password", "/api/v1/auth/change-password", "POST", "USERS"));
            arr.add(new Permission("Update a user", "/api/v1/users", "PUT", "USERS"));
            arr.add(new Permission("Delete a user", "/api/v1/users/{id}", "DELETE", "USERS"));
            arr.add(new Permission("Get a user by id", "/api/v1/users/{id}", "GET", "USERS"));
            arr.add(new Permission("Get users with pagination", "/api/v1/users", "GET", "USERS"));
            arr.add(new Permission("Get count all users", "/api/v1/users/count-all-users", "GET", "USERS"));
            arr.add(new Permission("Hard delete a user", "/api/v1/users/hard/{id}", "DELETE", "USERS"));
            arr.add(new Permission("Restore a user", "/api/v1/users/restore/{id}", "PUT", "USERS"));
            arr.add(new Permission("Fetch all deleted users", "/api/v1/users/deleted", "GET", "USERS"));

            arr.add(new Permission("Create a subscriber", "/api/v1/subscribers", "POST", "SUBSCRIBERS"));
            arr.add(new Permission("Update a subscriber", "/api/v1/subscribers", "PUT", "SUBSCRIBERS"));
            arr.add(new Permission("Delete a subscriber", "/api/v1/subscribers/{id}", "DELETE", "SUBSCRIBERS"));
            arr.add(new Permission("Get a subscriber by id", "/api/v1/subscribers/{id}", "GET", "SUBSCRIBERS"));
            arr.add(new Permission("Get subscribers with pagination", "/api/v1/subscribers", "GET", "SUBSCRIBERS"));

            arr.add(new Permission("Download a file", "/api/v1/files", "POST", "FILES"));
            arr.add(new Permission("Upload a file", "/api/v1/files", "GET", "FILES"));

            arr.add(new Permission("Chat with AI", "/socket.io/", "GET", "CHATBOT"));
            arr.add(new Permission("Fetch 10 audit-logs", "/api/v1/admin/audit-logs", "GET", "HISTORY"));

            this.permissionRepository.saveAll(arr);
        }

        if (countRoles == 0) {
            List<Permission> allPermissions = this.permissionRepository.findAll();

            Role adminRole = new Role();
            adminRole.setName("SUPER_ADMIN");
            adminRole.setDescription("Admin thì full permissions");
            adminRole.setActive(true);
            adminRole.setPermissions(allPermissions);

            this.roleRepository.save(adminRole);

            Role userRole = new Role();
            userRole.setName("USER");
            userRole.setDescription("USER has basic permissions");
            userRole.setActive(true);
            List<Permission> pUsArr = new ArrayList<>();
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/companies/{id}", "GET"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/companies", "GET"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/jobs/{id}", "GET"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/jobs", "GET"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/resumes", "POST"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/resumes/{id}", "GET"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/users/{id}", "GET"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/auth/change-password", "POST"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/subscribers", "GET"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/subscribers", "POST"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/files", "POST"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/api/v1/files", "GET"));
            pUsArr.add(this.permissionRepository.findByApiPathAndMethod("/socket.io/", "GET"));

            userRole.setPermissions(pUsArr);

            this.roleRepository.save(userRole);
        }

        if (countUsers == 0) {
            User adminUser = new User();
            adminUser.setEmail("admin@gmail.com");
            adminUser.setAddress("hn");
            adminUser.setAge(25);
            adminUser.setGender(GenderEnum.MALE);
            adminUser.setName("I'm super admin");
            adminUser.setPassword(this.passwordEncoder.encode("123456"));

            Role adminRole = this.roleRepository.findByName("SUPER_ADMIN");
            if (adminRole != null) {
                adminUser.setRole(adminRole);
            }

            this.userRepository.save(adminUser);

            User user = new User();
            user.setEmail("user@gmail.com");
            user.setAddress("hn");
            user.setAge(25);
            user.setGender(GenderEnum.MALE);
            user.setName("I'm a user");
            user.setPassword(this.passwordEncoder.encode("123456"));

            Role userRole = this.roleRepository.findByName("USER");
            if (userRole != null) {
                user.setRole(userRole);
            }

            this.userRepository.save(user);
        }

        if (countPermissions > 0 && countRoles > 0 && countUsers > 0) {
            System.out.println(">>> SKIP INIT DATABASE ~ ALREADY HAVE DATA...");
        } else
            System.out.println(">>> END INIT DATABASE");
    }

}
