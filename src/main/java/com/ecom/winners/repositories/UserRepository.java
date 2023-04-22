package com.ecom.winners.repositories;

import com.ecom.winners.entity.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
