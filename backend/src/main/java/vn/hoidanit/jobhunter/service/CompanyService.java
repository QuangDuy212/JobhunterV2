package vn.hoidanit.jobhunter.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.repository.CompanyRepository;
import vn.hoidanit.jobhunter.repository.UserRepository;

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
        // fetchh
        Page<Company> pageCompanies = this.companyRepository.findAll(spec, pageable);

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
            Company company = comOptional.get();
            company.setStatus(0);
            company.setUpdatedAt(Instant.now());
            this.companyRepository.save(company);

            List<User> users = this.userRepository.findByCompany(company);
            users.forEach(user -> {
                user.setStatus(0);
                user.setUpdatedAt(Instant.now());
            });
            this.userRepository.saveAll(users);
        }
    }

    public void restoreCompany(long id) {
        Optional<Company> comOptional = this.fetchCompanyById(id);
        if (comOptional.isPresent()) {
            Company company = comOptional.get();
            company.setStatus(1);
            company.setUpdatedAt(Instant.now());
            this.companyRepository.save(company);

            List<User> users = this.userRepository.findByCompany(company);
            users.forEach(user -> {
                user.setStatus(1);
                user.setUpdatedAt(Instant.now());
            });
            this.userRepository.saveAll(users);
        }
    }

    public boolean isExistId(long id) {
        return this.companyRepository.existsById(id);
    }

    public Long countAllCompanies() {
        return this.companyRepository.count();
    }
}
