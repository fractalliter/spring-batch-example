package com.ecom.winners.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AddressDTO {
    private String street;
    private String suite;
    private String city;
    private String zipcode;
    private GeoLocationDTO geo;
}
