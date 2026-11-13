package com.ecom.winners.configurations;

import com.ecom.winners.dto.UserDTO;
import com.ecom.winners.entity.User;
import com.ecom.winners.readers.RestApiItemReaderBuilder;
import com.ecom.winners.transformers.UserProcessor;
import com.ecom.winners.notifiers.ChunkLogListener;
import com.ecom.winners.repositories.UserRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;

/**
 * UserData configuration class comprises fetching user data from an API step.
 * This class consists of all three phases related to reading user's data from API, transforming user data
 * to User Entity and upserting it into the database.
 *
 * @see Configuration
 * @see User
 */
@Configuration
public class UserData {

    private static final org.slf4j.Logger logger =
            org.slf4j.LoggerFactory.getLogger(UserData.class);

    /**
     * User reader bean is responsible for fetching data from an API and path with pagination.
     * It reads data until the predicate meets the criteria.
     * For reading the data from API, it uses RestApiItemReader builder to create an Item reader object.
     *
     * @param objectMapper ObjectMapper for mapping data to UserDTO
     * @param host         the host where data resides, injected from application properties
     * @param path         the path where data resides, injected from application properties
     * @param pageSize     the fetch size for each page, injected from application properties
     * @param startPage    the start page to read, injected from application properties
     * @param endPage      the end page, injected from application properties
     * @return RestApiItemReader that is an implementation of ItemReader functional interface
     * @see ItemReader
     * @see UserDTO
     * @see ObjectMapper
     * @see Value
     * @see Bean
     */
    @Bean(name = "userReader")
    @StepScope
    public ItemReader<List<UserDTO>> reader(
            ObjectMapper objectMapper,
            @Value("${userdata.reader.resource.host}") String host,
            @Value("${userdata.reader.resource.path}") String path,
            @Value("${userdata.reader.page.size}") int pageSize,
            @Value("${userdata.reader.start.page}") int startPage,
            @Value("${userdata.reader.end.page}") int endPage
    ) {
        return RestApiItemReaderBuilder.<UserDTO>builder()
                .name("userReader")
                .webClient(WebClient.create(host))
                .path(path)
                .page(startPage)
                .size(pageSize)
                .endRead(currentPage -> currentPage >= endPage)
                .headers(new HashMap<>())
                .entity(UserDTO.class)
                .mapper(objectMapper)
                .build();
    }

    /**
     * User processor bean creates an instance of UserProcessor that processes user data that comes from reader bean.
     *
     * @return UserProcessor maps a list of UserDTO to a list of User entities.
     * @see UserDTO
     * @see User
     */
    @Bean(name = "userProcessor")
    public UserProcessor processor() {
        return new UserProcessor();
    }

    @Bean(name = "userWriter")
    public ItemWriter<List<User>> writer(UserRepository repository) {
        return chunk -> {
            for (List<User> users : chunk) {
                for (User incoming : users) {
                    User user = repository.findByUserId(incoming.getUserId()).orElseGet(User::new);
                    boolean existed = user.getId() != null;
                    user.setUserId(incoming.getUserId());
                    user.setName(incoming.getName());
                    user.setUsername(incoming.getUsername());
                    user.setPhone(incoming.getPhone());
                    user.setWebsite(incoming.getWebsite());
                    if (user.getAddress() == null) user.setAddress(incoming.getAddress());
                    else {
                        user.getAddress().setStreet(incoming.getAddress().getStreet());
                        user.getAddress().setSuite(incoming.getAddress().getSuite());
                        user.getAddress().setCity(incoming.getAddress().getCity());
                        user.getAddress().setZipcode(incoming.getAddress().getZipcode());
                        if (user.getAddress().getGeo() == null) user.getAddress().setGeo(incoming.getAddress().getGeo());
                        else {
                            user.getAddress().getGeo().setLatitude(incoming.getAddress().getGeo().getLatitude());
                            user.getAddress().getGeo().setLongitude(incoming.getAddress().getGeo().getLongitude());
                        }
                    }
                    if (user.getCompany() == null) user.setCompany(incoming.getCompany());
                    else {
                        user.getCompany().setName(incoming.getCompany().getName());
                        user.getCompany().setCatchPhrase(incoming.getCompany().getCatchPhrase());
                        user.getCompany().setBs(incoming.getCompany().getBs());
                    }
                    repository.save(user);
                    logger.info("Writer — {} user #{}: {}", existed ? "cập nhật" : "chèn mới",
                            incoming.getUserId(), incoming.getName());
                }
            }
        };
    }

    @Bean(name = "fetchUserData")
    public Step fetchUserData(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemWriter<List<User>> userWriter,
            ItemReader<List<UserDTO>> userReader,
            UserProcessor userProcessor,
            ChunkLogListener chunkLogListener,
            @Value("${userdata.reader.chunk.size}") int chunkSize
    ) {
        return new StepBuilder("fetchUserData", jobRepository)
                .<List<UserDTO>, List<User>>chunk(chunkSize).transactionManager(transactionManager)
                .reader(userReader)
                .processor(userProcessor)
                .writer(userWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .listener(chunkLogListener)
                .build();
    }
}
