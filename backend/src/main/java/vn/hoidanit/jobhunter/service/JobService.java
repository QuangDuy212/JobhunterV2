package vn.hoidanit.jobhunter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async; // Import cho @Async
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Skill;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResCreateJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResUpdateJob;
import vn.hoidanit.jobhunter.repository.JobRepository;
import vn.hoidanit.jobhunter.repository.SkillRespository;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.constant.JobStatusEnum; // Import JobStatusEnum

@Service
public class JobService {
    private final JobRepository jobRepository;
    private final SkillRespository skillRespository;
    private final SkillService skillService;
    private final CompanyService companyService;
    private final UserService userService;
    private final GeminiService geminiService; // Thêm GeminiService

    public JobService(JobRepository jobRepository, SkillRespository skillRespository, SkillService skillService,
            CompanyService companyService,UserService userService, GeminiService geminiService) { // Cập nhật Constructor
        this.jobRepository = jobRepository;
        this.skillRespository = skillRespository;
        this.skillService = skillService;
        this.companyService = companyService;
        this.userService = userService;
        this.geminiService = geminiService; // Khởi tạo GeminiService
    }

    /**
     * Hàm này được gọi ngay sau khi Job được tạo.
     * Nó chạy bất đồng bộ (@Async) để không làm người dùng chờ đợi.
     * @param job Job Entity đã được lưu vào DB (với Status = REVIEWING)
     */
    @Async 
    public void checkAndApproveJob(Job job) {
        // Lấy mô tả công việc
        String jobDescription = job.getDescription();
        
        System.out.println("Bắt đầu kiểm duyệt Job ID: " + job.getId());

        // 1. GỌI AI ĐỂ KIỂM TRA MỨC ĐỘ AN TOÀN (blocking call trong luồng @Async)
        boolean isSafe = geminiService.checkContentSafety(jobDescription);

        // 2. CẬP NHẬT TRẠNG THÁI DỰA TRÊN KẾT QUẢ
        if (isSafe) {
            job.setStatus(JobStatusEnum.APPROVED);
            System.out.println("Job ID " + job.getId() + " đã được AI APPROVE.");
        } else {
            job.setStatus(JobStatusEnum.REJECTED);
            job.setActive(false);
            System.out.println("Job ID " + job.getId() + " đã bị AI REJECT do nội dung phản cảm.");
        }
        
        // Lưu lại trạng thái mới
        this.jobRepository.save(job);
    }


    public ResCreateJobDTO handleCreateJob(Job j) {
        // check skills
        if (j.getSkills() != null) {
            List<Long> reqSkills = j.getSkills()
                    .stream().map(x -> x.getId())
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

        // 1. Create job (Status mặc định là REVIEWING - đã được thiết lập trong Job Entity @PrePersist)
        Job currentJob = this.jobRepository.save(j);

        // 2. Kích hoạt kiểm duyệt AI BẤT ĐỒNG BỘ
        this.checkAndApproveJob(currentJob);

        // convert response
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
            List<String> skills = currentJob.getSkills().stream().map(s -> s.getName())
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

    public Job fetchJobById(long id) {
        Optional<Job> job = this.jobRepository.findById(id);
        if (job.isPresent())
            return job.get();
        return null;
    }

    public ResultPaginationDTO fetchAllJobs(Specification<Job> spec, Pageable pageable) {

        ResultPaginationDTO rs = new ResultPaginationDTO();
        
        String email = SecurityUtil.getCurrentUserLogin().isPresent() ? SecurityUtil.getCurrentUserLogin().get()
        : "";
        
        User currentUserDB = this.userService.handleGetUserByUsername(email);
        if(currentUserDB.getCompany() != null){
            Specification<Job> companySpec = (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("company").get("id"), currentUserDB.getCompany().getId());
            spec = spec == null ? companySpec : spec.and(companySpec);
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

    public List<Job> fetchJobBySkill(long skillId){
        Optional<Skill> skillOptional = this.skillRespository.findById(skillId);
        if(skillOptional.isPresent()){
            return this.jobRepository.findBySkills(skillOptional.get());
        }
        return new ArrayList<>();
    }

    public List<Job> fetchAllJobs() {
        return this.jobRepository.findAll();
    }

}