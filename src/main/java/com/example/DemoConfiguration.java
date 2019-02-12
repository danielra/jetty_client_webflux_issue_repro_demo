package com.example;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.JettyClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DemoConfiguration {

	@Bean
	public WebClient webClient() throws Exception {
                final HttpClient jettyClient = new HttpClient(new SslContextFactory(true));
                jettyClient.setAddressResolutionTimeout(10000);
                jettyClient.setConnectTimeout(10000);
                jettyClient.setIdleTimeout(100000);
                jettyClient.setTCPNoDelay(true);
                jettyClient.setMaxConnectionsPerDestination(1000);
                jettyClient.setFollowRedirects(false);
                jettyClient.start();

                final ClientHttpConnector clientHttpConnector = new JettyClientHttpConnector(jettyClient);

                return WebClient.builder()
                        .clientConnector(clientHttpConnector)
                        .build();
	}

}

