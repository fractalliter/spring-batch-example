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

@Configuration
public class UserData {
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
