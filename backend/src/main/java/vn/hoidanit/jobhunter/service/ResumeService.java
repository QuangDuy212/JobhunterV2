package vn.hoidanit.jobhunter.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.turkraft.springfilter.converter.FilterSpecification;
import com.turkraft.springfilter.converter.FilterSpecificationConverter;
import com.turkraft.springfilter.parser.FilterParser;
import com.turkraft.springfilter.parser.node.FilterNode;

import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Resume;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResCountResumeByStausDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResCreateResumeDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResResumeDTO;
import vn.hoidanit.jobhunter.domain.response.resume.ResUpdateResumeDTO;
import vn.hoidanit.jobhunter.repository.ResumeRepository;
import vn.hoidanit.jobhunter.util.BaseSpecs;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.constant.ResumeStateEnum;

@Service
public class ResumeService {
    private final ResumeRepository resumeRepository;
    private final UserService userService;
    private final JobService jobService;

    @Autowired
    private FilterParser filterParser;

    @Autowired
    private FilterSpecificationConverter filterSpecificationConverter;

    public ResumeService(ResumeRepository resumeRepository, UserService userService, JobService jobService) {
        this.resumeRepository = resumeRepository;
        this.userService = userService;
        this.jobService = jobService;
    }

    public boolean isExistId(long id) {
        return this.resumeRepository.existsById(id);
    }

    public boolean checkResumeExistByUserAndJob(Resume resume) {
        // check user by id
        if (resume.getUser() == null)
            return false;
        User user = this.userService.fetchUserById(resume.getUser().getId());
        if (user == null)
            return false;

        // check job by id
        if (resume.getJob() == null)
            return false;
        Job job = this.jobService.fetchJobById(resume.getJob().getId());
        if (job == null)
            return false;
        return true;
    }

    public Resume handleCreateResume(Resume reqResume) {
        if (reqResume.getUser() != null) {
            long idUser = reqResume.getUser().getId();
            User user = this.userService.fetchUserById(idUser);
            reqResume.setUser(user);
        }
        if (reqResume.getJob() != null) {
            long idJob = reqResume.getJob().getId();
            Job job = this.jobService.fetchJobById(idJob);
            reqResume.setJob(job);
        }
        return this.resumeRepository.save(reqResume);
    }

    public ResCreateResumeDTO convertResumeToResCreateResumeDTO(Resume resume) {
        ResCreateResumeDTO res = new ResCreateResumeDTO();
        res.setId(resume.getId());
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());
        return res;
    }

    public Resume handleUpdateResume(Resume reqResume) {
        Resume resume = this.fetchResumeById(reqResume.getId());
        if (reqResume.getStatus() != null) {
            resume.setStatus(reqResume.getStatus());
        }
        return this.resumeRepository.save(resume);
    }

    public Resume fetchResumeById(long id) {
        Optional<Resume> resume = this.resumeRepository.findById(id);
        if (resume.isPresent())
            return resume.get();
        return null;
    }

    public ResUpdateResumeDTO convertResumeToResUpdateResumeDTO(Resume resume) {
        ResUpdateResumeDTO res = new ResUpdateResumeDTO();
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        return res;
    }

    public ResResumeDTO convertResumeToResResumeDTO(Resume resume) {
        ResResumeDTO res = new ResResumeDTO();
        res.setId(resume.getId());
        res.setEmail(resume.getEmail());
        res.setStatus(resume.getStatus());
        res.setUrl(resume.getUrl());
        res.setCreatedAt(resume.getCreatedAt());
        res.setCreatedBy(resume.getCreatedBy());
        res.setUpdatedAt(resume.getUpdatedAt());
        res.setUpdatedBy(resume.getUpdatedBy());
        if (resume.getUser() != null) {
            ResResumeDTO.UserResume user = new ResResumeDTO.UserResume();
            user.setId(resume.getUser().getId());
            user.setName(resume.getUser().getName());
            res.setUser(user);
        }
        if (resume.getJob() != null) {
            ResResumeDTO.JobResume job = new ResResumeDTO.JobResume();
            job.setId(resume.getJob().getId());
            job.setName(resume.getJob().getName());
            res.setJob(job);
            res.setCompanyName(resume.getJob().getCompany().getName());
        }
        return res;
    }

    public ResultPaginationDTO fetchAllResumes(Specification<Resume> spec, Pageable pageable) {
        Specification<Resume> resumesSpec = BaseSpecs.isActive();
        Page<Resume> pageResume = this.resumeRepository.findAll(resumesSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();

        List<ResResumeDTO> listResume = pageResume.getContent()
                .stream().map(item -> this.convertResumeToResResumeDTO(item)).collect(Collectors.toList());
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageResume.getTotalPages());
        mt.setTotal(pageResume.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(listResume);
        return rs;
    }

    public ResultPaginationDTO fetchDeletedResumes(Specification<Resume> spec, Pageable pageable) {
        Specification<Resume> delResumesSpec = BaseSpecs.isDeleted();
        Page<Resume> pageDelResume = this.resumeRepository.findAll(delResumesSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();

        List<ResResumeDTO> listDelResume = pageDelResume.getContent()
                .stream().map(item -> this.convertResumeToResResumeDTO(item)).collect(Collectors.toList());
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageDelResume.getTotalPages());
        mt.setTotal(pageDelResume.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(listDelResume);
        return rs;
    }

    public void handleDeleteResume(long id) {
        this.resumeRepository.deleteById(id);
    }

    public void softDeleteResume(long id) {
        Resume resume = this.resumeRepository.findById(id).orElse(null);
        resume.setDeleted(true);
        resume.setDeletedAt(LocalDateTime.now());
        this.resumeRepository.save(resume);
    }

    public void restoreResume(long id) {
        Resume resume = this.resumeRepository.findById(id).orElse(null);
        resume.setDeleted(false);
        resume.setDeletedAt(null);
        this.resumeRepository.save(resume);
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void autoHardDelete() {
        LocalDateTime limit = LocalDateTime.now().minusDays(30);
        List<Resume> expired = resumeRepository.findAllByDeletedTrueAndDeletedAtBefore(limit);

        if (!expired.isEmpty()) {
            resumeRepository.deleteAll(expired);
            System.out.println("Auto hard delete users executed, removed: " + expired.size() + " records");
        }
    }

    public ResultPaginationDTO fetchResumeByUser(Pageable pageable) {
        // query builder
        String email = SecurityUtil.getCurrentUserLogin().isPresent() == true
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        FilterNode node = filterParser.parse("email='" + email + "'");
        FilterSpecification<Resume> spec = filterSpecificationConverter.convert(node);
        Page<Resume> pageResume = this.resumeRepository.findAll(spec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();

        List<ResResumeDTO> listResume = pageResume.getContent()
                .stream().map(item -> this.convertResumeToResResumeDTO(item)).collect(Collectors.toList());
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageResume.getTotalPages());
        mt.setTotal(pageResume.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(listResume);
        return rs;
    }

    public Long countAllResumes() {
        return this.resumeRepository.count();
    }

    public Long getResumesByMonth(int year, int month) {
        // 1. Tạo YearMonth từ tham số đầu vào
        YearMonth yearMonth = YearMonth.of(year, month);

        // 2. Lấy thời điểm bắt đầu của tháng (tại 00:00:00)
        Instant startDate = yearMonth.atDay(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC); // Sử dụng UTC để khớp với cách JPA/Instant lưu trữ

        // 3. Lấy thời điểm kết thúc của tháng (tại 23:59:59.999999999)
        Instant endDate = yearMonth.atEndOfMonth()
                .atTime(23, 59, 59, 999999999)
                .toInstant(ZoneOffset.UTC);

        // 4. Gọi Repository để truy
        List<Resume> resumes = this.resumeRepository.findAllByCreatedAtBetween(startDate, endDate);
        return resumes.size() + 0L;
    }

    public List<ResCountResumeByStausDTO> countResumesByStatus() {
        List<ResumeStateEnum> statuses = List.of(ResumeStateEnum.values());
        List<ResCountResumeByStausDTO> result = statuses.stream().map(status -> {
            long count = this.resumeRepository.countByStatus(status);
            ResCountResumeByStausDTO dto = new ResCountResumeByStausDTO();
            dto.setStatus(status.name());
            dto.setCount(count);
            return dto;
        }).collect(Collectors.toList());
        return result;
    }
}
