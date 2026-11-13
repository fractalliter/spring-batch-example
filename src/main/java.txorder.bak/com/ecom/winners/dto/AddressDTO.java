package com.ecom.winners.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {
    private String street;
    private String suite;
    private String city;
    private String zipcode;
    private GeoLocationDTO geo;
}
