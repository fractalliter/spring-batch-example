package com.ecom.winners.repositories;

import com.ecom.winners.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}
