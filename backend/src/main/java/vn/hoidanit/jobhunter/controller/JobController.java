package vn.hoidanit.jobhunter.controller;

import java.util.List;

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

import jakarta.validation.Valid;
import vn.hoidanit.jobhunter.domain.Job;
import vn.hoidanit.jobhunter.domain.Skill;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResIdDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResCreateJobDTO;
import vn.hoidanit.jobhunter.domain.response.job.ResUpdateJob;
import vn.hoidanit.jobhunter.service.JobService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;
import vn.hoidanit.jobhunter.util.error.JobContentException;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
public class JobController {
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/jobs")
    @ApiMessage("Create a job")
    public ResponseEntity<ResCreateJobDTO> createNewJob(@Valid @RequestBody Job reqJob) throws JobContentException {
        // Logic check AI đã nằm trọn trong handleCreateJob
        // Nếu có lỗi, Exception sẽ được ném ra và GlobalException sẽ bắt lại
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(this.jobService.handleCreateJob(reqJob));
    }

    @PutMapping("/jobs")
    @ApiMessage("Update a job")
    public ResponseEntity<ResUpdateJob> putMethodName(@RequestBody Job reqJob) throws IdInvalidException {
        boolean checkExist = this.jobService.isExistId(reqJob.getId());
        if (!checkExist) {
            throw new IdInvalidException("Job not found");
        }
        return ResponseEntity.ok().body(this.jobService.handleUpdateJob(reqJob));
    }

    @DeleteMapping("/jobs/hard/{id}")
    @ApiMessage("hard delete job by id")
    public ResponseEntity<ResIdDTO> deleteJob(@PathVariable("id") long id) throws IdInvalidException {
        boolean checkExist = this.jobService.isExistId(id);
        if (!checkExist) {
            throw new IdInvalidException("Job not found");
        }
        this.jobService.handleDeleteJob(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ResIdDTO() {
            {
                setId(id);
            }
        });
    }

    @DeleteMapping("/jobs/{id}")
    @ApiMessage("delete job by id")
    public ResponseEntity<ResIdDTO> doftDeleteJob(@PathVariable("id") long id) throws IdInvalidException {
        boolean checkExist = this.jobService.isExistId(id);
        if (!checkExist) {
            throw new IdInvalidException("Job not found");
        }
        this.jobService.softDeleteJob(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ResIdDTO() {
            {
                setId(id);
            }
        });
    }

    @PutMapping("/jobs/restore/{id}")
    @ApiMessage("restore job by id")
    public ResponseEntity<ResIdDTO> restoreJob(@PathVariable("id") long id) throws IdInvalidException {
        boolean checkExist = this.jobService.isExistId(id);
        if (!checkExist) {
            throw new IdInvalidException("Job not found");
        }
        // TODO: process PUT request
        this.jobService.restoreJob(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ResIdDTO() {
            {
                setId(id);
            }
        });
    }

    @GetMapping("/admin-jobs")
    @ApiMessage("Fetch all jobs")
    public ResponseEntity<ResultPaginationDTO> fetchAllJobsForAdmin(@Filter Specification<Job> spec,
            Pageable pageable) throws IdInvalidException {
        return ResponseEntity.ok().body(this.jobService.fetchAllJobsForAdmin(spec, pageable));
    }

    @GetMapping("/jobs")
    @ApiMessage("Fetch all jobs")
    public ResponseEntity<ResultPaginationDTO> fetchAllJobsForUser(@Filter Specification<Job> spec,
            Pageable pageable) throws IdInvalidException {
        return ResponseEntity.ok().body(this.jobService.fetchAllJobsForUser(spec, pageable));
    }

    @GetMapping("/jobs/deleted")
    @ApiMessage("Fetch deleted jobs")
    public ResponseEntity<ResultPaginationDTO> fetchDeletedJobs(@Filter Specification<Job> spec,
            Pageable pageable) throws IdInvalidException {
        return ResponseEntity.ok().body(this.jobService.fetchDeletedJobs(spec, pageable));
    }

    @GetMapping("/jobs/fetch-job-detail/{id}")
    @ApiMessage("Fetch job by id")
    public ResponseEntity<Job> fetchJobById(@PathVariable("id") long id) throws IdInvalidException {
        boolean checkExist = this.jobService.isExistId(id);
        if (!checkExist) {
            throw new IdInvalidException("Job not found");
        }
        Job job = this.jobService.fetchJobById(id);
        return ResponseEntity.ok().body(job);
    }

    @GetMapping("/jobs/count-all-jobs")
    @ApiMessage("Fetch count all jobs")
    public ResponseEntity<Long> fetchCountAllJobs() {
        long count = this.jobService.countJob();
        return ResponseEntity.ok().body(count);
    }

    @GetMapping("/jobs/fetch-by-skill/{id}")
    @ApiMessage("Fetch job by skill")
    public ResponseEntity<List<Job>> fetchJobBySkill(@PathVariable("id") long id) {
        List<Job> jobs = this.jobService.fetchJobBySkill(id);
        return ResponseEntity.ok().body(jobs);
    }

}
