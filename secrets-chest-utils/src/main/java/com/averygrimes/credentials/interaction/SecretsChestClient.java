package com.averygrimes.credentials.interaction;


import com.averygrimes.credentials.pojo.CredentialConstants;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import javax.ws.rs.core.MediaType;
import java.util.concurrent.TimeUnit;

/**
 * @author Avery Grimes-Farrow
 * Created on: 7/4/20
 * https://github.com/helloavery
 */

public class SecretsChestClient {

    private WebClient client;

    public SecretsChestClient(){
        String baseUrl;
        String instance = System.getProperty("secrets.chest.profile", "dev");
        if(instance.equalsIgnoreCase("prod")){
            baseUrl = CredentialConstants.PROD_SS3GATEWAY_ENDPOINT;
        }else{
            baseUrl = CredentialConstants.TEST_S3GATEWAY_ENDPOINT;
        }

        TcpClient tcpClient = TcpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .doOnConnected(connection -> {
                    connection.addHandlerLast(new ReadTimeoutHandler(10000, TimeUnit.MILLISECONDS));
                    connection.addHandlerLast(new WriteTimeoutHandler(10000, TimeUnit.MILLISECONDS));
                });

        if(client == null){
            client = WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(HttpClient.from(tcpClient)))
                    .baseUrl(baseUrl)
                    .build();
        }
    }

    public ClientResponse uploadSecrets(byte[] dataToUpload){
        return client.post()
                .uri("/uploadSecrets")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(Mono.just(dataToUpload), byte[].class))
                .exchange()
                .block();
    }

    public ClientResponse updateSecrets(String secretsReference, byte[] data){
        return client.put()
                .uri("/updateSecrets/" + secretsReference)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromPublisher(Mono.just(data), byte[].class))
                .exchange()
                .block();
    }

    public ClientResponse retrieveSecrets(String secretReference){
        return client.post()
                .uri("/retrieveSecrets/" + secretReference)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                .exchange()
                .block();
    }
}
