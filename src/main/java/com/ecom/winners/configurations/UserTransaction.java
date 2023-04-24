package com.ecom.winners.configurations;

import com.ecom.winners.dto.TransactionDTO;
import com.ecom.winners.entity.Transaction;
import com.ecom.winners.repositories.UserRepository;
import com.ecom.winners.transformers.TransactionProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class UserTransaction {

    @Bean(name = "transactionReader")
    public FlatFileItemReader<TransactionDTO> reader(
            @Value("${transaction.reader.resource.path}") String resourcePath,
            @Value("${transaction.reader.resource.headers}") String names
    ) {
        String[] headers = names.replaceAll("\\s", "").split(",");
        return new FlatFileItemReaderBuilder<TransactionDTO>()
                .name("transactionReader")
                .resource(new ClassPathResource(resourcePath))
                .delimited()
                .names(headers)
                .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {{
                    setTargetType(TransactionDTO.class);
                }})
                .build();
    }

    @Bean(name = "transactionProcessor")
    public TransactionProcessor processor(UserRepository userRepository) {
        return new TransactionProcessor(userRepository);
    }

    @Bean(name = "transactionWriter")
    public JpaItemWriter<Transaction> writer(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Transaction>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean(name = "insertUserTransactions")
    public Step insertUserTransactions(
            JobRepository jobRepository,
            TransactionProcessor transactionProcessor,
            PlatformTransactionManager transactionManager,
            JpaItemWriter<Transaction> transactionWriter,
            FlatFileItemReader<TransactionDTO> transactionReader,
            @Value("${transaction.reader.chunk.size}") int chunkSize
    ) {
        return new StepBuilder("insertUserTransactions", jobRepository)
                .<TransactionDTO, Transaction>chunk(chunkSize, transactionManager)
                .reader(transactionReader)
                .processor(transactionProcessor)
                .writer(transactionWriter)
                .build();
    }

}
