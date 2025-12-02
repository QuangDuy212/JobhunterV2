package vn.hoidanit.jobhunter.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;
import com.turkraft.springfilter.builder.FilterBuilder;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;

import jakarta.validation.Valid;
import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Resume;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResIdDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResCountResumeByStausDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResCreateResumeDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResResumeDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResUpdateResumeDTO;
import vn.hoidanit.jobhunter.service.JobService;
import vn.hoidanit.jobhunter.service.ResumeService;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.constant.ResumeStateEnum;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
public class ResumeController {

    @Autowired
    private FilterParser filterParser;

    @Autowired
    private FilterSpecificationConverter filterSpecificationConverter;

    @Autowired
    private FilterBuilder filterBuilder;

    private final ResumeService resumeService;
    private final UserService userService;
    private final JobService jobService;

    public ResumeController(ResumeService resumeService, UserService userService, JobService jobService) {
        this.resumeService = resumeService;
        this.userService = userService;
        this.jobService = jobService;
    }

    @PostMapping("/resumes")
    @ApiMessage("Create a resume")
    public ResponseEntity<ResCreateResumeDTO> createNewResume(@Valid @RequestBody Resume reqResume)
            throws IdInvalidException {
        boolean check = this.resumeService.checkResumeExistByUserAndJob(reqResume);
        if (!check) {
            throw new IdInvalidException("User/Job not found");
        }
        // create
        Resume resume = this.resumeService.handleCreateResume(reqResume);

        // convert to rescreate
        ResCreateResumeDTO res = this.resumeService.convertResumeToResCreateResumeDTO(resume);

        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @PutMapping("/resumes")
    @ApiMessage("update a resume")
    public ResponseEntity<ResUpdateResumeDTO> updateResume(@RequestBody Resume reqResume) throws IdInvalidException {
        Resume currentResume = this.resumeService.fetchResumeById(reqResume.getId());
        if (currentResume == null) {
            throw new IdInvalidException("Resume not found");
        }
        Resume resume = this.resumeService.handleUpdateResume(reqResume);
        // convert resume to ResUpdateResumeDTO to display
        ResUpdateResumeDTO res = this.resumeService.convertResumeToResUpdateResumeDTO(resume);
        // return ResponseEntity.status(HttpStatus.OK).body(ericUser);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/resumes/{id}")
    @ApiMessage("fetch resume by id")
    public ResponseEntity<ResResumeDTO> fetchResumeById(@PathVariable("id") long id) throws IdInvalidException {
        Resume checkResume = this.resumeService.fetchResumeById(id);
        if (checkResume == null) {
            throw new IdInvalidException("Resume not found");
        }
        // convert resume to ResUserDTO to display
        ResResumeDTO res = this.resumeService.convertResumeToResResumeDTO(checkResume);
        // return ResponseEntity.status(HttpStatus.OK).body(user);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/resumes")
    @ApiMessage("fetch all resumes")
    public ResponseEntity<ResultPaginationDTO> fetchAllUsers(
            @Filter Specification<Resume> spec, // PHẢI là Specification<Resume>
            Pageable pageable) {

        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = this.userService.handleGetUserByUsername(email);

        // Mặc định, finalSpec là spec (điều kiện lọc từ người dùng)
        Specification<Resume> finalSpec = spec;

        // 1. Chỉ áp dụng logic phân quyền nếu không phải SUPER_ADMIN
        if (currentUser != null && !currentUser.getRole().getName().equals("SUPER_ADMIN")) {

            List<Long> arrJobIds = null;
            Company userCompany = currentUser.getCompany();

            if (userCompany != null) {
                // Lấy Job IDs của công ty người dùng (HR)
                List<Job> companyJobs = userCompany.getJobs();
                if (companyJobs != null && companyJobs.size() > 0) {
                    arrJobIds = companyJobs.stream()
                            .map(Job::getId)
                            .collect(Collectors.toList());
                }
            }

            // 2. Tạo Specification giới hạn Job ID
            if (arrJobIds != null && !arrJobIds.isEmpty()) {
                // Tạo điều kiện: Resume.job.id IN (arrJobIds)
                // Đảm bảo kiểu trả về là Specification<Resume>
                Specification<Resume> jobInSpec = filterSpecificationConverter.convert(filterBuilder.field("job")
                        .in(filterBuilder.input(arrJobIds)).get());

                // 3. Kết hợp điều kiện phân quyền (jobInSpec) với điều kiện lọc người dùng
                // (spec)
                // Lỗi đã được sửa bằng cách đảm bảo jobInSpec và spec cùng kiểu <Resume>
                finalSpec = jobInSpec.and(spec);
            } else {
                // Trường hợp không có Job ID nào để xem (HR không có công ty/job)
                // Lọc theo ID = -1L để trả về kết quả rỗng (No resumes)
                // Điều này hiệu quả hơn việc trả về null hoặc throw exception
                Specification<Resume> noResultSpec = filterSpecificationConverter.convert(filterBuilder.field("id")
                        .equal(filterBuilder.input(-1L)).get());

                finalSpec = noResultSpec.and(spec);
            }
        }
        // Nếu là SUPER_ADMIN, finalSpec vẫn là spec (cho phép xem tất cả resumes)

        return ResponseEntity.ok(this.resumeService.fetchAllResumes(finalSpec, pageable));
    }

    @GetMapping("/resumes/deleted")
    @ApiMessage("fetch deleted resumes")
    public ResponseEntity<ResultPaginationDTO> fetchDeletedResumes(
            @Filter Specification<Resume> spec, // PHẢI là Specification<Resume>
            Pageable pageable) {

        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        User currentUser = this.userService.handleGetUserByUsername(email);

        // Mặc định, finalSpec là spec (điều kiện lọc từ người dùng)
        Specification<Resume> finalSpec = spec;

        // 1. Chỉ áp dụng logic phân quyền nếu không phải SUPER_ADMIN
        if (currentUser != null && !currentUser.getRole().getName().equals("SUPER_ADMIN")) {

            List<Long> arrJobIds = null;
            Company userCompany = currentUser.getCompany();

            if (userCompany != null) {
                // Lấy Job IDs của công ty người dùng (HR)
                List<Job> companyJobs = userCompany.getJobs();
                if (companyJobs != null && companyJobs.size() > 0) {
                    arrJobIds = companyJobs.stream()
                            .map(Job::getId)
                            .collect(Collectors.toList());
                }
            }

            // 2. Tạo Specification giới hạn Job ID
            if (arrJobIds != null && !arrJobIds.isEmpty()) {
                // Tạo điều kiện: Resume.job.id IN (arrJobIds)
                // Đảm bảo kiểu trả về là Specification<Resume>
                Specification<Resume> jobInSpec = filterSpecificationConverter.convert(filterBuilder.field("job")
                        .in(filterBuilder.input(arrJobIds)).get());

                // 3. Kết hợp điều kiện phân quyền (jobInSpec) với điều kiện lọc người dùng
                // (spec)
                // Lỗi đã được sửa bằng cách đảm bảo jobInSpec và spec cùng kiểu <Resume>
                finalSpec = jobInSpec.and(spec);
            } else {
                // Trường hợp không có Job ID nào để xem (HR không có công ty/job)
                // Lọc theo ID = -1L để trả về kết quả rỗng (No resumes)
                // Điều này hiệu quả hơn việc trả về null hoặc throw exception
                Specification<Resume> noResultSpec = filterSpecificationConverter.convert(filterBuilder.field("id")
                        .equal(filterBuilder.input(-1L)).get());

                finalSpec = noResultSpec.and(spec);
            }
        }
        // Nếu là SUPER_ADMIN, finalSpec vẫn là spec (cho phép xem tất cả resumes)

        return ResponseEntity.ok(this.resumeService.fetchDeletedResumes(finalSpec, pageable));
    }

    @DeleteMapping("/resumes/hard/{id}")
    @ApiMessage("delete resume by id")
    public ResponseEntity<ResIdDTO> deleteResume(@PathVariable("id") long id) throws IdInvalidException {
        Resume resume = this.resumeService.fetchResumeById(id);
        if (resume == null) {
            throw new IdInvalidException("Resume not found");
        }
        this.resumeService.handleDeleteResume(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ResIdDTO() {
            {
                setId(id);
            }
        });
    }

    @DeleteMapping("/resumes/{id}")
    @ApiMessage("delete resume by id")
    public ResponseEntity<ResIdDTO> softDeleteResume(@PathVariable("id") long id) throws IdInvalidException {
        Resume resume = this.resumeService.fetchResumeById(id);
        if (resume == null) {
            throw new IdInvalidException("Resume not found");
        }
        this.resumeService.softDeleteResume(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ResIdDTO() {
            {
                setId(id);
            }
        });
    }

    @PutMapping("/resumes/restore/{id}")
    @ApiMessage("restore resume by id")
    public ResponseEntity<ResIdDTO> restore(@PathVariable("id") long id) throws IdInvalidException {
        boolean checkExist = this.resumeService.isExistId(id);
        if (!checkExist) {
            throw new IdInvalidException("Job not found");
        }
        // TODO: process PUT request
        this.resumeService.restoreResume(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ResIdDTO() {
            {
                setId(id);
            }
        });
    }

    @PostMapping("/resumes/by-user")
    @ApiMessage("Get list resumes by user")
    public ResponseEntity<ResultPaginationDTO> fetchResumeByUser(Pageable pageable)
            throws IdInvalidException {

        return ResponseEntity.status(HttpStatus.CREATED).body(this.resumeService.fetchResumeByUser(pageable));
    }

    @GetMapping("/resumes/count-all-resumes")
    @ApiMessage("Fetch count all resumes")
    public ResponseEntity<Long> countAllResumes() {
        long count = this.resumeService.countAllResumes();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/resumes/count-resumes-by-time")
    @ApiMessage("Fetch count resumes by time")
    public ResponseEntity<Long> countResumesByTime(
            @RequestParam int year,
            @RequestParam int month) throws IdInvalidException {
        // Kiểm tra cơ bản
        if (month < 1 || month > 12) {
            return ResponseEntity.badRequest().build();
        }

        Long resumes = resumeService.getResumesByMonth(year, month);

        return ResponseEntity.ok(resumes);
    }

    @GetMapping("/resumes/count/count-by-status")
    @ApiMessage("Fetch count resumes by status")
    public ResponseEntity<List<ResCountResumeByStausDTO>> getMethodName() {
        return ResponseEntity.ok(this.resumeService.countResumesByStatus());
    }
}
