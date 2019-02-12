package com.example.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@Log4j2
public class ResultsProviderService {

    private final WebClient webClient;


    @Autowired
    public ResultsProviderService(final WebClient webClient) {

        this.webClient = webClient;
    }


    public Mono<Map<Integer, String>> getResults(List<Integer> ints) {


        return Mono.just((Map<Integer,String>)Collections.EMPTY_MAP)
                .map(cacheResultsMap -> cacheResultsMap)
                .flatMap(cacheResultsMap -> this.getResultsInternal(ints)
                                                .map(freshResultsMap -> Tuples.of(cacheResultsMap, freshResultsMap)))
                .map(TupleUtils.function((cacheResultsMap, freshResultsMap) -> this.constructResult(cacheResultsMap, freshResultsMap, ints)));
    }


    private Map<Integer, String> constructResult(final Map<Integer, String> cacheResultsMap,
            final Map<Integer, String> freshResultsMap,
            final List<Integer> resultStructure) {

        Map<Integer, String> resultMap = new HashMap<>();
        for(final Integer i : resultStructure) {
            if(!freshResultsMap.containsKey(i)) {
                System.out.println("MISSING RESULT " + i);
            } else {
                resultMap.put(i, freshResultsMap.get(i));
            }
        }

        return resultMap;
    }


    private Mono<Map<Integer, String>> getResultsInternal(final List<Integer> ints) {

        List<Mono<Tuple2<Integer, String>>> resultList = new LinkedList<>();
        for(Integer id : ints) {
            resultList.add(getRequestMono(id));
        }

        return Flux.merge(resultList)
            .collect(Collectors.toMap(identifierResultPair -> identifierResultPair.getT1(),
                        identifierResultPair -> identifierResultPair.getT2()));
    }


    private Mono<Tuple2<Integer, String>> getRequestMono(final Integer id) {
        return this.webClient.get()
            .uri("http://localhost:8080/demo/json")
            .retrieve()
            .bodyToMono(String.class)
            .log("Request#: " +id)
            .map(response -> Tuples.of(id, response))
            .doOnError(t -> log.error("Attempt to GET data failed." + t))
            .onErrorReturn(Tuples.of(id, "Oops..."));
    }

}
