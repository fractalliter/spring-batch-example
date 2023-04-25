package com.ecom.winners.readers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import org.springframework.batch.item.ItemReader;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * REST API item reader is a builder for creating an object of item reader functional interface that fetches a page of
 * data from and API with the help of WebClients library and returns a List of T object for further processing.
 * It Fetches data from a source, caches the data locally, and maps it to a generic object of type T.
 * It uses a predicate to end the fetching of data from the data source.
 * @param <T> generic for a mapping object.
 * @see ItemReader
 * @see List
 */
@Builder
public class RestApiItemReaderBuilder<T> implements ItemReader<List<T>> {
    private String path;
    private Map<String, String> headers;
    private WebClient webClient;
    private Class<T> entity;
    private String name;
    private int page;
    private int size;
    private ObjectMapper mapper;
    private Predicate<Integer> endRead;

    @Override
    public List<T> read() {
        if (endRead.test(page)) {
            return null;
        }
        Object[] response = webClient.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path(path)
                                .queryParam("page", page)
                                .queryParam("size", size)
                                .build()
                ).headers(httpHeaders -> headers.forEach(httpHeaders::set))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Object[].class).cache().block();
        page += 1;
        List<T> users = new ArrayList<>();
        assert response != null;
        for (Object user : response)
            users.add(mapper.convertValue(user, entity));
        return users;
    }
}
