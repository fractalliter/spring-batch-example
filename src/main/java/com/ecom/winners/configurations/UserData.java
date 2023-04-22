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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;

@Configuration
public class UserData {

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${userdata.reader.chunk.size}")
    private Integer chunkSize;

    @Value("${userdata.reader.resource.url}")
    private String url;

    @Bean(name = "userReader")
    public ItemReader<List<UserDTO>> reader() {
        return RestApiItemReaderBuilder.<UserDTO>builder()
                .name("fetchUserData")
                .url(url).webClient(WebClient.create())
                .page(0)
                .size(10)
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

    @Bean
    public Step fetchUserData(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemWriter<List<User>> writer
    ) {
        return new StepBuilder("fetchUserData", jobRepository)
                .<List<UserDTO>, List<User>>chunk(chunkSize, transactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer)
                .build();
    }
}
