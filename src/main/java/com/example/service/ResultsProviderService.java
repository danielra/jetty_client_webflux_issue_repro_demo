package com.example.service;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.reactivestreams.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.extern.log4j.Log4j2;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.*;
import reactor.function.TupleUtils;
import reactor.util.function.*;

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

    static final AtomicBoolean once = new AtomicBoolean();

    private Mono<Tuple2<Integer, String>> getRequestMono(final Integer id) {

        if (once.compareAndSet(false, true)) {
            Hooks.onEachOperator(p -> {
                if (p.getClass().getSimpleName().contains("FluxMap")) {
                    return new ProtocolValidator<>(p);
                }
                return p;
            });
        }
        
        return this.webClient.get()
            .uri("http://localhost:8080/demo/json")
            .retrieve()
            .bodyToMono(String.class)
            .log("Request#: " +id)
            .map(response -> Tuples.of(id, response))
            .doOnError(t -> System.err.println("Attempt to GET data failed." + t))
            .onErrorReturn(Tuples.of(id, "Oops..."));
    }
    
    static final class ProtocolValidator<T> extends Flux<T> {

        final Publisher<T> source;
        
        ProtocolValidator(Publisher<T> source) {
            this.source = source;
        }
        
        @Override
        public void subscribe(CoreSubscriber<? super T> actual) {
            source.subscribe(new ProtocolValidatorSubscriber<>(actual));
        }

        static final class ProtocolValidatorSubscriber<T> implements CoreSubscriber<T>, Subscription {

            final CoreSubscriber<? super T> downstream;

            Subscription upstream;

            Throwable done;
            Throwable error;

            ProtocolValidatorSubscriber(CoreSubscriber<? super T> downstream) {
                this.downstream = downstream;
            }
            
            @Override
            public void onNext(T t) {
                if (done != null) {
                    System.err.println("onNext after onComplete");
                    done.printStackTrace();
                }
                if (error != null) {
                    System.err.println("onNext after onError: " + error);
                    error.printStackTrace();
                }
                downstream.onNext(t);
            }

            @Override
            public void onError(Throwable t) {
                error = new Exception("onError", t);
                downstream.onError(t);
            }

            @Override
            public void onComplete() {
                done = new Exception("onComplete");
                downstream.onComplete();
            }

            @Override
            public void onSubscribe(Subscription s) {
                upstream = s;
                downstream.onSubscribe(this);
            }

            @Override
            public void request(long n) {
                upstream.request(n);
            }

            @Override
            public void cancel() {
                upstream.cancel();
            }
            
        }
    }

}
