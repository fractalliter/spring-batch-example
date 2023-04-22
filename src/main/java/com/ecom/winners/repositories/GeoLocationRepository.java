package com.ecom.winners.repositories;

import com.ecom.winners.entity.GeoLocation;
import org.springframework.data.repository.CrudRepository;

public interface GeoLocationRepository extends CrudRepository<GeoLocation, Long> {
}
