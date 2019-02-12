package com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.service.ResultsProviderService;
import java.util.Arrays;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(value = "/demo")
@Log4j2
public class DemoController {

    private final ResultsProviderService service;


    @Autowired
    public DemoController(final ResultsProviderService service) {
        this.service = service;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/json", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public Mono<String> getJson() {
        return Mono.just("Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content. Some response content.");
    }

    @RequestMapping(method = RequestMethod.GET, value = "/repro", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public Mono<Map<Integer, String>> getResults() {

        return service.getResults(Arrays.asList(1))
            .map(result -> result);
    }

}
