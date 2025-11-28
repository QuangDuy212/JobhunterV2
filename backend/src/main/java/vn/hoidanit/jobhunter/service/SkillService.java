package vn.hoidanit.jobhunter.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.cfg.BaseSettings;

import jakarta.transaction.Transactional;
import vn.hoidanit.jobhunter.domain.Skill;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResUserDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.SkillRespository;
import vn.hoidanit.jobhunter.util.BaseSpecs;

@Service
public class SkillService {
    private final SkillRespository skillRespository;

    public SkillService(SkillRespository skillRespository) {
        this.skillRespository = skillRespository;
    }

    public Skill handleCreateSkill(Skill skill) {
        return this.skillRespository.save(skill);
    }

    public boolean isExistName(String name) {
        return this.skillRespository.existsByName(name);
    }

    public boolean isExistId(long id) {
        return this.skillRespository.existsById(id);
    }

    public Skill handleUpdateSkill(Skill reqSkill) {
        Optional<Skill> skill = this.skillRespository.findById(reqSkill.getId());
        skill.get().setName(reqSkill.getName());
        return this.skillRespository.save(skill.get());
    }

    public ResultPaginationDTO fetchAllSkills(Specification<Skill> spec, Pageable pageable) {
        Specification<Skill> skillsSpec = BaseSpecs.isActive();
        Page<Skill> pageSkill = this.skillRespository.findAll(skillsSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();

        List<Skill> listSkill = pageSkill.getContent();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageSkill.getTotalPages());
        mt.setTotal(pageSkill.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(listSkill);
        return rs;
    }

    public Optional<Skill> fetchSkillById(long id) {
        return this.skillRespository.findById(id);
    }

    public void handleDeleteSkill(long id) {
        // delete job (inside job_skill table)
        Optional<Skill> skillOptional = this.skillRespository.findById(id);
        Skill currentSkill = skillOptional.get();
        currentSkill.getJobs().forEach(job -> job.getSkills().remove(currentSkill));

        // delete subscriber (inside subscriber_skill table)
        currentSkill.getSubscribers().forEach(job -> job.getSkills().remove(currentSkill));

        // delete skill
        this.skillRespository.deleteById(id);
    }

    public void softDeleteSkill(long id) {
        Skill skill = skillRespository.findById(id).orElse(null);

        // soft delete
        skill.setDeleted(true);
        skill.setDeletedAt(LocalDateTime.now());

        skillRespository.save(skill);
    }

    public void restoreSkill(long id) {
        Skill skill = skillRespository.findById(id).orElse(null);
        skill.setDeleted(false);
        skill.setDeletedAt(null);
        skillRespository.save(skill);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoHardDeleteSkill() {

        LocalDateTime limit = LocalDateTime.now().minusDays(30);

        List<Skill> expiredSkills = this.skillRespository.findAllByDeletedTrueAndDeletedAtBefore(limit);

        for (Skill skill : expiredSkills) {

            // XÓA QUAN HỆ TRONG job_skill
            skill.getJobs().forEach(job -> job.getSkills().remove(skill));
            skill.getJobs().clear();

            // XÓA QUAN HỆ TRONG subscriber_skill
            skill.getSubscribers().forEach(sub -> sub.getSkills().remove(skill));
            skill.getSubscribers().clear();

            // Lưu lại để xóa foreign key
            skillRespository.save(skill);

            // xóa hẳn skill
            this.skillRespository.delete(skill);
        }
        System.out.println("Auto hard delete skills executed, removed: " + expiredSkills.size() + " records");
    }

    public List<Skill> fetchListSkillByListId(List<Long> listIds) {
        return this.skillRespository.findByIdIn(listIds);
    }

    public ResultPaginationDTO fetchDeletedSkills(Specification<Skill> spec, Pageable pageable) {
        Specification<Skill> deletedSkillsSpec = BaseSpecs.isDeleted();
        Page<Skill> pageDeletedSkill = this.skillRespository.findAll(deletedSkillsSpec, pageable);
        ResultPaginationDTO rs = new ResultPaginationDTO();

        List<Skill> listDeletedSkill = pageDeletedSkill.getContent();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageDeletedSkill.getTotalPages());
        mt.setTotal(pageDeletedSkill.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(listDeletedSkill);
        return rs;
    }

    public Long countAllSkills() {
        return this.skillRespository.count();
    }
}
