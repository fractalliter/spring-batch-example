package com.ecom.winners.writers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.Builder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import java.util.List;

/**
 * JPA bulk item writer is an ItemWriter for the bulk writes of Spring Data JPA.
 * It's somehow a chunk bulk writer
 * @param <T> an entity
 * @see JpaItemWriter
 */
@Builder
public class JpaBulkItemWriter<T> implements ItemWriter<List<T>>, InitializingBean {
    protected static final Log logger = LogFactory.getLog(JpaBulkItemWriter.class);
    private EntityManagerFactory entityManagerFactory;
    private boolean usePersist = false;

    public JpaBulkItemWriter() {
    }

    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public void setUsePersist(boolean usePersist) {
        this.usePersist = usePersist;
    }

    /**
     * Asserts the state of entity manager property.
     */
    public void afterPropertiesSet() {
        Assert.state(this.entityManagerFactory != null, "An EntityManagerFactory is required");
    }

    /**
     * It's a wrapper around doWrite. It takes the transaction from entity manager and checks the existence of it then
     * raise a DataAccessResourceFailure exception if the connection does not exist.
     * If all goes well, it persists the list of items into the database.
     * @param listOfItems a chunk of items as List of generic T
     */
    public void write(Chunk<? extends List<T>> listOfItems) {
        EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(this.entityManagerFactory);
        if (entityManager == null) {
            throw new DataAccessResourceFailureException("Unable to obtain a transactional EntityManager");
        } else {
            this.doWrite(entityManager, listOfItems);
            entityManager.flush();
        }
    }

    /**
     * Does the persistence of data into the database.
     * It checks the existence of the entity before persistence,
     * If an entity already exists, it will merge it.
     * @param entityManager EntityManager
     * @param listOfItems Chunk of list of items from generic T
     */
    protected void doWrite(EntityManager entityManager, Chunk<? extends List<T>> listOfItems) {
        if (logger.isDebugEnabled()) {
            logger.debug("Writing to JPA with " + listOfItems.size() + " items.");
        }

        if (!listOfItems.isEmpty()) {
            long addedToContextCount = 0L;

            for (List<T> item : listOfItems) {
                for (T i : item)
                    if (entityManager.contains(i))
                        entityManager.merge(i);
                    else
                        entityManager.persist(i);

                ++addedToContextCount;
            }

            if (logger.isDebugEnabled()) {
                logger.debug(addedToContextCount + " entities " + (this.usePersist ? " persisted." : "merged."));
                logger.debug((long) listOfItems.size() - addedToContextCount + " entities found in persistence context.");
            }
        }

    }
}