package com.ecom.winners.configurations;

import com.ecom.winners.dto.TransactionDTO;
import com.ecom.winners.entity.Transaction;
import com.ecom.winners.repositories.UserRepository;
import com.ecom.winners.transformers.TransactionProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
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

/**
 * User configuration component.
 * Configuration for the User transactions job.
 * In this step, user's transactions are streamed from a CSV file from resources as TransactionDTO,
 * transformed into a Transaction entity, and persisted into the database.
 *
 * @see Configuration
 */
@Configuration
public class UserTransaction {

    /**
     * Transaction reader bean.
     * Reader starts with streaming the rows from the CSV file from the resource path, selects headers,
     * and converts rows into TransactionDTO
     *
     * @param resourcePath path to CSV file
     * @param names        headers of the CSV file
     * @return FlatFileItemReader of type TransactionDTO
     */
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

    /**
     * Transaction process bean.
     * The Transaction processor checks the user_id from TransactionDTO with the help of UserRepository
     * and finds the equivalent user in the database.
     * It creates the Transaction entity based on the user entity and amount.
     * Transaction entity generates a creation date for each Transaction.
     *
     * @param userRepository repository for managing interactions for User resource in the database
     * @return TransactionProcessor that is an implementation of ItemProcessor
     * @see ItemProcessor
     * @see UserRepository
     */
    @Bean(name = "transactionProcessor")
    public TransactionProcessor processor(UserRepository userRepository) {
        return new TransactionProcessor(userRepository);
    }

    /**
     * Transaction writer bean
     * The Transaction writer takes a Transaction entity from the processor,
     * persists into the database where it also manages the required relation between user and it's transactions.
     *
     * @param entityManagerFactory EntityManagerFactory
     * @return JpaItemWriter of type Transaction entity
     * @see ItemWriter
     * @see EntityManagerFactory
     */
    @Bean(name = "transactionWriter")
    public JpaItemWriter<Transaction> writer(EntityManagerFactory entityManagerFactory) {
        return new JpaItemWriterBuilder<Transaction>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    /**
     * The User's transaction Step bean
     * A step that manages the insertion of the user's transactions from a CSV file into the database
     *
     * @param jobRepository        JobRepository
     * @param transactionProcessor TransactionProcessor
     * @param transactionManager   PlatformTransactionManager
     * @param transactionWriter    JpaItemWriter for Transactions
     * @param transactionReader    FlatFileItemReader for TransactionDTO
     * @param chunkSize            size of the batch
     * @return Step
     * @see JobRepository
     * @see TransactionProcessor
     * @see PlatformTransactionManager
     * @see JpaItemWriter
     * @see FlatFileItemReader
     * @see Value
     */
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
