package vn.hoidanit.jobhunter.controller;

import org.springframework.web.bind.annotation.RestController;

import com.turkraft.springfilter.boot.Filter;

import jakarta.validation.Valid;
import vn.hoidanit.jobhunter.domain.Company;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.domain.response.ResIdDTO;
import vn.hoidanit.jobhunter.domain.response.ResultPaginationDTO;
import vn.hoidanit.jobhunter.service.CompanyService;
import vn.hoidanit.jobhunter.util.annotation.ApiMessage;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
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
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1")
public class CompanyController {
    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/companies")
    @ApiMessage("create a company")
    public ResponseEntity<Company> createNewCompany(@Valid @RequestBody Company reqCompany) {
        Company company = this.companyService.handleCreateCompany(reqCompany);
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }

    @GetMapping("/companies")
    @ApiMessage("fetch all company")
    public ResponseEntity<ResultPaginationDTO> fetchAllCompanies(
            @Filter Specification<Company> spec,
            Pageable pageable) {

        // fetch all companies
        return ResponseEntity.ok(this.companyService.fetchAllCompanies(spec, pageable));
    }

    @GetMapping("/companies/deleted")
    @ApiMessage("fetch deleted company")
    public ResponseEntity<ResultPaginationDTO> fetchDeletedCompanies(
            @Filter Specification<Company> spec,
            Pageable pageable) {

        // fetch all companies
        return ResponseEntity.ok(this.companyService.fetchDeletedCompanies(spec, pageable));
    }

    @GetMapping("/companies/{id}")
    @ApiMessage("fetch company by id")
    public ResponseEntity<Company> fetchCompanyById(@PathVariable("id") long id) {
        Optional<Company> company = this.companyService.fetchCompanyById(id);
        // return ResponseEntity.status(HttpStatus.OK).body(user);
        return ResponseEntity.ok(company.get());
    }

    @PutMapping("/companies")
    @ApiMessage("update a company")
    public ResponseEntity<Company> updateAUser(@RequestBody Company rqCompany) {
        Company company = this.companyService.handleUpdateCompany(rqCompany);
        // return ResponseEntity.status(HttpStatus.OK).body(ericUser);
        return ResponseEntity.ok(company);
    }

    @DeleteMapping("/companies/hard/{id}")
    @ApiMessage("hard delete company by id")
    public ResponseEntity<ResIdDTO> deleteCompany(@PathVariable("id") long id) throws IdInvalidException {
        Optional<Company> company = this.companyService.fetchCompanyById(id);
        if (!company.isPresent()) {
            throw new IdInvalidException("Không tồn tại công ty với ID được truyền vào");
        }
        this.companyService.handleDeleteCompany(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ResIdDTO() {
            {
                setId(id);
            }
        });
    }

    @DeleteMapping("/companies/{id}")
    @ApiMessage("delete company by id")
    public ResponseEntity<ResIdDTO> softDeleteCompany(@PathVariable("id") long id) throws IdInvalidException {
        Optional<Company> company = this.companyService.fetchCompanyById(id);
        if (!company.isPresent()) {
            throw new IdInvalidException("Không tồn tại công ty với ID được truyền vào");
        }
        this.companyService.softDeleteCompany(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ResIdDTO() {
            {
                setId(id);
            }
        });
    }

    @PutMapping("/companies/restore/{id}")
    @ApiMessage("restore company by id")
    public ResponseEntity<ResIdDTO> restoreCompany(@PathVariable("id") long id) throws IdInvalidException {
        Optional<Company> company = this.companyService.fetchCompanyById(id);
        if (company == null) {
            throw new IdInvalidException("User với id = " + id + " không tồn tại");
        }
        // TODO: process PUT request
        this.companyService.restoreCompany(id);
        return ResponseEntity.status(HttpStatus.OK).body(new ResIdDTO() {
            {
                setId(id);
            }
        });
    }

    @GetMapping("/companies/count-all-companies")
    @ApiMessage("Fetch count all companies")
    public ResponseEntity<Long> countAllCompanies() {
        long count = this.companyService.countAllCompanies();
        return ResponseEntity.ok(count);
    }

}
