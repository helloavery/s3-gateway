package com.averygrimes.credentials.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * @author Avery Grimes-Farrow
 * Created on: 5/14/20
 * https://github.com/helloavery
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class CredentialsResponse {

    private boolean isSuccessful;
    private byte[] data;
    private Map<String, byte[]> secretsDataMap;
    private String secretReference;
    private Map<String, String> secretsReferences;
    private String error;

}
