package com.ecom.winners.repositories;

import com.ecom.winners.entity.Company;
import org.springframework.data.repository.CrudRepository;

public interface CompanyRepository extends CrudRepository<Company, Long> {
}
