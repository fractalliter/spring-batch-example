package com.ecom.winners.transformers;

import com.ecom.winners.dto.AddressDTO;
import com.ecom.winners.dto.CompanyDTO;
import com.ecom.winners.dto.GeoLocationDTO;
import com.ecom.winners.dto.UserDTO;
import com.ecom.winners.entity.Address;
import com.ecom.winners.entity.Company;
import com.ecom.winners.entity.GeoLocation;
import com.ecom.winners.entity.User;
import org.springframework.batch.item.ItemProcessor;

public class WinnerProcessor implements ItemProcessor<User, UserDTO> {
    @Override
    public UserDTO process(User user) {
        Company company = user.getCompany();
        Address address = user.getAddress();
        if (company == null || address == null) throw new RuntimeException("Address/Company is null");
        GeoLocation geoLocation = address.getGeo();
        if (geoLocation == null) throw new RuntimeException("GeoLocation is null");

        GeoLocationDTO geoLocationDTO = new GeoLocationDTO(
                geoLocation.getLatitude(),
                geoLocation.getLongitude()
        );
        AddressDTO addressDTO = new AddressDTO(
                address.getStreet(),
                address.getSuite(),
                address.getCity(),
                address.getZipcode(),
                geoLocationDTO
        );
        CompanyDTO companyDTO = new CompanyDTO(
                company.getName(),
                company.getCatchPhrase(),
                company.getBs()
        );
        return new UserDTO(
                user.getUserId(),
                user.getName(),
                user.getUsername(),
                user.getPhone(),
                user.getWebsite(),
                addressDTO,
                companyDTO
        );
    }
}
