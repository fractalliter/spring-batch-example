package com.ecom.winners.writers;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.util.Assert;

import java.util.List;

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

    public void afterPropertiesSet() {
        Assert.state(this.entityManagerFactory != null, "An EntityManagerFactory is required");
    }

    public void write(Chunk<? extends List<T>> listOfItems) {
        EntityManager entityManager = EntityManagerFactoryUtils.getTransactionalEntityManager(this.entityManagerFactory);
        if (entityManager == null) {
            throw new DataAccessResourceFailureException("Unable to obtain a transactional EntityManager");
        } else {
            this.doWrite(entityManager, listOfItems);
            entityManager.flush();
        }
    }

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