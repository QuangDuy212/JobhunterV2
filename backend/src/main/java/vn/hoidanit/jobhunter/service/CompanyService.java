package vn.hoidanit.jobhunter.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.Skill;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;
import vn.hoidanit.jobhunter.util.BaseSpecs;

@Service
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    public CompanyService(CompanyRepository companyRepository, UserRepository userRepository) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
    }

    public Company handleCreateCompany(Company company) {
        return this.companyRepository.save(company);
    }

    public ResultPaginationDTO fetchAllCompanies(Specification<Company> spec, Pageable pageable) {
        Specification<Company> companiesSpec = BaseSpecs.isActive();
        // fetchh
        Page<Company> pageCompanies = this.companyRepository.findAll(companiesSpec, pageable);

        // handle result
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageCompanies.getTotalPages());
        mt.setTotal(pageCompanies.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageCompanies.getContent());
        return rs;
    }
    public ResultPaginationDTO fetchDeletedCompanies(Specification<Company> spec, Pageable pageable) {
        Specification<Company> delConpaniesSpec = BaseSpecs.isDeleted();
        // fetchh
        Page<Company> pageDelCompanies = this.companyRepository.findAll(delConpaniesSpec, pageable);

        // handle result
        ResultPaginationDTO rs = new ResultPaginationDTO();
        ResultPaginationDTO.Meta mt = new ResultPaginationDTO.Meta();

        mt.setPage(pageable.getPageNumber() + 1);
        mt.setPageSize(pageable.getPageSize());

        mt.setPages(pageDelCompanies.getTotalPages());
        mt.setTotal(pageDelCompanies.getTotalElements());

        rs.setMeta(mt);
        rs.setResult(pageDelCompanies.getContent());
        return rs;
    }

    public Optional<Company> fetchCompanyById(long id) {
        Optional<Company> company = this.companyRepository.findById(id);
        return company;
    }

    public Company handleUpdateCompany(Company company) {
        Optional<Company> optionalCompany = this.fetchCompanyById(company.getId());
        if (optionalCompany.isPresent()) {
            Company currentCompany = optionalCompany.get();
            currentCompany.setName(company.getName());
            currentCompany.setAddress(company.getAddress());
            currentCompany.setDescription(company.getDescription());
            currentCompany.setLogo(company.getLogo());
            // update
            currentCompany = this.companyRepository.save(currentCompany);
            return currentCompany;
        }
        return null;
    }

    public void handleDeleteCompany(long id) {
        Optional<Company> comOptional = this.fetchCompanyById(id);
        if (comOptional.isPresent()) {
            Company com = comOptional.get();
            // fetch all users belong to this company
            List<User> users = this.userRepository.findByCompany(com);
            this.userRepository.deleteAll(users);
        }
        this.companyRepository.deleteById(id);
    }

    public void softDeleteCompany(long id) {
        Company company = this.companyRepository.findById(id).orElse(null);
        company.setDeleted(true);
        company.setDeletedAt(LocalDateTime.now());
        this.companyRepository.save(company);
    }

    public void restoreCompany(long id) {
        Company company = this.companyRepository.findById(id).orElse(null);
        company.setDeleted(false);
        company.setDeletedAt(null);
        this.companyRepository.save(company);
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoHardDeleteCompany() {

        LocalDateTime limit = LocalDateTime.now().minusDays(30);

        List<Company> expiredCompanies = this.companyRepository.findAllByDeletedTrueAndDeletedAtBefore(limit);

        for (Company company : expiredCompanies) {

            List<User> users = this.userRepository.findByCompany(company);
            this.userRepository.deleteAll(users);
            this.companyRepository.delete(company);
        }
        System.out.println("Auto hard delete companies executed, removed: " + expiredCompanies.size() + " records");
    }

    public boolean isExistId(long id) {
        return this.companyRepository.existsById(id);
    }

    public Long countAllCompanies() {
        return this.companyRepository.count();
    }
}
