package com.ecom.winners.transformers;

import com.ecom.winners.dto.AddressDTO;
import com.ecom.winners.dto.CompanyDTO;
import com.ecom.winners.dto.GeoLocationDTO;
import com.ecom.winners.dto.UserDTO;
import com.ecom.winners.entity.Address;
import com.ecom.winners.entity.Company;
import com.ecom.winners.entity.GeoLocation;
import com.ecom.winners.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * UserProcessor implements ItemProcessor functional interface.
 * It transforms the UserDTO list to a list of User entities with mapDtoTOEntity function.
 */
public class UserProcessor implements ItemProcessor<List<UserDTO>, List<User>> {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
    private final Function<UserDTO, User> mapDtoToEntity = userDto -> {
        User user = new User();
        Address address = new Address();
        GeoLocation geoLocation = new GeoLocation();
        Company company = new Company();

        AddressDTO addressDTO = userDto.getAddress();
        GeoLocationDTO geoLocationDTO = addressDTO.getGeo();
        CompanyDTO companyDTO = userDto.getCompany();

        geoLocation.setLatitude(geoLocationDTO.getLat());
        geoLocation.setLongitude(geoLocationDTO.getLng());

        address.setStreet(addressDTO.getStreet());
        address.setCity(addressDTO.getCity());
        address.setGeo(geoLocation);
        address.setZipcode(addressDTO.getZipcode());
        address.setSuite(addressDTO.getSuite());

        company.setName(companyDTO.getName());
        company.setCatchPhrase(companyDTO.getCatchPhrase());
        company.setBs(companyDTO.getBs());

        user.setUserId(userDto.getId());
        user.setName(userDto.getName());
        user.setUsername(userDto.getUsername());
        user.setPhone(userDto.getPhone());
        user.setWebsite(userDto.getWebsite());
        user.setAddress(address);
        user.setCompany(company);
        return user;
    };

    @Override
    public List<User> process(final List<UserDTO> items) {
        logger.debug(items.toString());
        List<User> users = new ArrayList<>();
        for (UserDTO userDTO : items)
            users.add(mapDtoToEntity.apply(userDTO));
        return users;
    }
}