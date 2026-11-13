package com.ecom.winners.repositories;

import com.ecom.winners.entity.Winner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WinnerRepository extends JpaRepository<Winner, Long> {
    List<Winner> findTop10ByOrderByWonAtDesc();
}
