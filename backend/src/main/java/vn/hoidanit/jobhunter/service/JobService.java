package vn.hoidanit.jobhunter.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.rsocket.RSocketProperties.Server.Spec;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async; // Import cho @Async
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Skill;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResCreateJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResUpdateJob;
import vn.hoidanit.jobhunter.domain.response.job.SafetyCheckResult;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.SkillRespository;
import vn.hoidanit.jobhunter.util.BaseSpecs;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.constant.JobStatusEnum; // Import JobStatusEnum
import vn.hoidanit.jobhunter.util.error.JobContentException;

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final SkillRespository skillRespository;
    private final SkillService skillService;
    private final CompanyService companyService;
    private final UserService userService;
    private final GeminiService geminiService; // Thêm GeminiService

    public JobService(JobRepository jobRepository, SkillRespository skillRespository, SkillService skillService,
            CompanyService companyService, UserService userService, GeminiService geminiService) { // Cập nhật
                                                                                                   // Constructor
        this.jobRepository = jobRepository;
        this.skillRespository = skillRespository;
        this.skillService = skillService;
        this.companyService = companyService;
        this.userService = userService;
        this.geminiService = geminiService; // Khởi tạo GeminiService
    }

    public ResCreateJobDTO handleCreateJob(Job j) throws JobContentException {
        // 1. Kiểm tra mô tả công việc bằng AI trước khi lưu (Chạy đồng bộ)
        // Giả sử geminiService.checkContentSafety trả về một Object chứa: isSafe và
        // violatedWords
        SafetyCheckResult safetyResult = geminiService.checkContentSafety(j.getDescription());

        if (!safetyResult.isSafe()) {
            throw new JobContentException(
                    "Mô tả công việc chứa nội dung không phù hợp.",
                    safetyResult.getViolatedWords() // Đây là List<String>
            );
        }

        // 2. Nếu an toàn, tiến hành xử lý dữ liệu như cũ
        if (j.getSkills() != null) {
            List<Long> reqSkills = j.getSkills().stream()
                    .map(x -> x.getId())
                    .collect(Collectors.toList());
            List<Skill> dbSkills = this.skillRespository.findByIdIn(reqSkills);
            j.setSkills(dbSkills);
        }

        if (j.getCompany() != null) {
            Optional<Company> cOptional = this.companyService.fetchCompanyById(j.getCompany().getId());
            if (cOptional.isPresent()) {
                j.setCompany(cOptional.get());
            }
        }

        // 3. Lưu vào DB với trạng thái APPROVED luôn vì đã check xong
        j.setStatus(JobStatusEnum.APPROVED);
        Job currentJob = this.jobRepository.save(j);

        // 4. Convert sang DTO trả về
        ResCreateJobDTO dto = new ResCreateJobDTO();
        dto.setId(currentJob.getId());
        dto.setName(currentJob.getName());
        dto.setSalary(currentJob.getSalary());
        dto.setQuantity(currentJob.getQuantity());
        dto.setLocation(currentJob.getLocation());
        dto.setLevel(currentJob.getLevel());
        dto.setStartDate(currentJob.getStartDate());
        dto.setEndDate(currentJob.getEndDate());
        dto.setActive(currentJob.isActive());
        dto.setCreatedAt(currentJob.getCreatedAt());
        dto.setCreatedBy(currentJob.getCreatedBy());

        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills().stream()
                    .map(s -> s.getName())
                    .collect(Collectors.toList());
            dto.setSkills(skills);
        }

        return dto;
    }

    public boolean isExistId(long id) {
        return this.jobRepository.existsById(id);
    }

    public ResUpdateJob handleUpdateJob(Job reqJob) {
        Optional<Job> jobOptional = this.jobRepository.findById(reqJob.getId());
        Job newJob = jobOptional.get();
        if (jobOptional.isPresent()) {
            newJob.setActive(reqJob.isActive());
            newJob.setLevel(reqJob.getLevel());
            if (reqJob.getName() != null)
                newJob.setName(reqJob.getName());
            if (reqJob.getLocation() != null)
                newJob.setLocation(reqJob.getLocation());
            if (reqJob.getSalary() > 0)
                newJob.setSalary(reqJob.getSalary());
            if (reqJob.getLevel() != null)
                newJob.setLevel(reqJob.getLevel());
            if (reqJob.getDescription() != null)
                newJob.setDescription(reqJob.getDescription());
            if (reqJob.getStartDate() != null)
                newJob.setStartDate(reqJob.getStartDate());
            if (reqJob.getEndDate() != null)
                newJob.setEndDate(reqJob.getEndDate());
            if (reqJob.getSkills() != null) {
                List<Long> listIds = reqJob.getSkills()
                        .stream().map(item -> item.getId()).collect(Collectors.toList());
                newJob.setSkills(this.skillService.fetchListSkillByListId(listIds));
            }
            if (reqJob.getCompany() != null) {
                Optional<Company> cOptional = this.companyService.fetchCompanyById(reqJob.getCompany().getId());
                if (cOptional.isPresent()) {
                    newJob.setCompany(cOptional.get());
                }
            }
        }
        // check skills
        // Optional<Job> job = this.jobRepository.findById(j.getId());
        // if (j.getSkills() != null) {
        // List<Long> reqSkills = j.getSkills()
        // .stream().map(x -> x.getId())
        // .collect(Collectors.toList());
        // List<Skill> dbSkills = this.skillRespository.findByIdIn(reqSkills);
        // job.get().setSkills(dbSkills);
        // }

        // // create job
        Job currentJob = this.jobRepository.save(newJob);

        // convert response
        ResUpdateJob dto = new ResUpdateJob();
        dto.setId(currentJob.getId());
        dto.setName(currentJob.getName());
        dto.setSalary(currentJob.getSalary());
        dto.setQuantity(currentJob.getQuantity());
        dto.setLocation(currentJob.getLocation());
        dto.setLevel(currentJob.getLevel());
        dto.setStartDate(currentJob.getStartDate());
        dto.setEndDate(currentJob.getEndDate());
        dto.setActive(currentJob.isActive());
        dto.setCreatedAt(currentJob.getCreatedAt());
        dto.setCreatedBy(currentJob.getCreatedBy());

        if (currentJob.getSkills() != null) {
            List<String> skills = currentJob.getSkills().stream().map(s -> s.getName())
                    .collect(Collectors.toList());
            dto.setSkills(skills);
        }

        return dto;
    }

    public void handleDeleteJob(long id) {
        this.jobRepository.deleteById(id);
    }

    public void softDeleteJob(long id) {
        Job job = this.jobRepository.findById(id).orElse(null);
        job.setDeleted(true);
        job.setDeletedAt(LocalDateTime.now());
        this.jobRepository.save(job);
    }

    public void restoreJob(long id) {
        Job job = this.jobRepository.findById(id).orElse(null);
        job.setDeleted(false);
        job.setDeletedAt(LocalDateTime.now());
        this.jobRepository.save(job);
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void autoHardDeleteJob() {
        LocalDateTime limit = LocalDateTime.now().minusDays(30);
        List<Job> expired = jobRepository.findAllByDeletedTrueAndDeletedAtBefore(limit);

        if (!expired.isEmpty()) {
            jobRepository.deleteAll(expired);
            System.out.println("Auto hard delete jobs executed, removed: " + expired.size() + " records");
        }
    }

    public Job fetchJobById(long id) {
        Optional<Job> job = this.jobRepository.findById(id);
        if (job.isPresent())
            return job.get();
        return null;
    }

    public ResultPaginationDTO fetchAllJobsForAdmin(Specification<Job> spec, Pageable pageable) {

        Specification<Job> jobsSpec = BaseSpecs.isActive();
        ResultPaginationDTO rs = new ResultPaginationDTO();

        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        User currentUserDB = this.userService.handleGetUserByUsername(email);
        if (currentUserDB.getCompany() != null) {
            Specification<Job> companySpec = (root, query, criteriaBuilder) -> criteriaBuilder
                    .equal(root.get("company").get("id"), currentUserDB.getCompany().getId());
            spec = spec == null ? companySpec : spec.and(companySpec);
        }
        Page<Job> pageJob = this.jobRepository.findAll(jobsSpec, pageable);

        List<Job> listJob = pageJob.getContent();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageJob.getTotalPages());
        mt.setTotal(pageJob.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(listJob);
        return rs;
    }

    public ResultPaginationDTO fetchDeletedJobs(Specification<Job> spec, Pageable pageable) {
        Specification<Job> delJobsSpec = BaseSpecs.isDeleted();
        ResultPaginationDTO rs = new ResultPaginationDTO();

        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        User currentUserDB = this.userService.handleGetUserByUsername(email);
        if (currentUserDB.getCompany() != null) {
            Specification<Job> companySpec = (root, query, criteriaBuilder) -> criteriaBuilder
                    .equal(root.get("company").get("id"), currentUserDB.getCompany().getId());
            spec = spec == null ? companySpec : spec.and(companySpec);
        }
        Page<Job> pageDelJob = this.jobRepository.findAll(delJobsSpec, pageable);

        List<Job> listDelJob = pageDelJob.getContent();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageDelJob.getTotalPages());
        mt.setTotal(pageDelJob.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(listDelJob);
        return rs;
    }

    public ResultPaginationDTO fetchAllJobsForUser(Specification<Job> spec, Pageable pageable) {

        ResultPaginationDTO rs = new ResultPaginationDTO();

        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get()
                : "";

        User currentUserDB = this.userService.handleGetUserByUsername(email);
        // if(currentUserDB.getCompany() != null){
        // Specification<Job> companySpec = (root, query, criteriaBuilder) ->
        // criteriaBuilder.equal(root.get("company").get("id"),
        // currentUserDB.getCompany().getId());
        // spec = spec == null ? companySpec : spec.and(companySpec);
        // }

        // 1. Tạo Specification cho điều kiện 'active' = true
        Specification<Job> activeSpec = (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("active"));
        // Hoặc: criteriaBuilder.equal(root.get("active"), true);
        // Hoặc: criteriaBuilder.equal(root.get("active"), 1); (Nếu 'active' là kiểu số
        // nguyên/bit)

        // 2. Kết hợp activeSpec với Specification đã có (spec)
        if (spec == null) {
            spec = activeSpec;
        } else {
            spec = spec.and(activeSpec);
        }
        Page<Job> pageJob = this.jobRepository.findAll(spec, pageable);

        List<Job> listJob = pageJob.getContent();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageJob.getTotalPages());
        mt.setTotal(pageJob.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(listJob);
        return rs;
    }

    public Long countJob() {
        return this.jobRepository.count();
    }

    public List<Job> fetchJobBySkill(long skillId) {
        Optional<Skill> skillOptional = this.skillRespository.findById(skillId);
        if (skillOptional.isPresent()) {
            return this.jobRepository.findBySkills(skillOptional.get());
        }
        return new ArrayList<>();
    }

    public List<Job> fetchAllJobs() {
        return this.jobRepository.findAll();
    }

}