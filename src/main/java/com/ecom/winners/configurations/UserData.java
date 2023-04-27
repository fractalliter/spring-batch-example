package com.ecom.winners.configurations;

import com.ecom.winners.dto.UserDTO;
import com.ecom.winners.entity.User;
import com.ecom.winners.readers.RestApiItemReaderBuilder;
import com.ecom.winners.transformers.UserProcessor;
import com.ecom.winners.writers.JpaBulkItemWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;

/**
 * UserData configuration class comprises fetching user data from an API step.
 * This class consists of all three phases related to reading user's data from API, transforming user data
 * to User Entity and persisting it into the database with the help of JPA bulk item writer.
 *
 * @see Configuration
 * @see User
 */
@Configuration
public class UserData {
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
    public ItemWriter<List<User>> writer(EntityManagerFactory entityManagerFactory) {
        JpaBulkItemWriter<User> jpaBulkItemWriter = new JpaBulkItemWriter<>();
        jpaBulkItemWriter.setUsePersist(true);
        jpaBulkItemWriter.setEntityManagerFactory(entityManagerFactory);
        return jpaBulkItemWriter;
    }

    @Bean(name = "fetchUserData")
    public Step fetchUserData(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemWriter<List<User>> userWriter,
            ItemReader<List<UserDTO>> userReader,
            UserProcessor userProcessor,
            @Value("${userdata.reader.chunk.size}") int chunkSize
    ) {
        return new StepBuilder("fetchUserData", jobRepository)
                .<List<UserDTO>, List<User>>chunk(chunkSize, transactionManager)
                .reader(userReader)
                .processor(userProcessor)
                .writer(userWriter)
                .build();
    }
}
