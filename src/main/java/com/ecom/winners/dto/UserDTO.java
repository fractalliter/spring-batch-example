package com.ecom.winners.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UserDTO {
    private Long id;
    private String name;
    private String username;
    private String phone;
    private String website;
    private AddressDTO address;
    private CompanyDTO company;
}
