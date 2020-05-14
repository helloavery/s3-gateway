package com.averygrimes.credentials.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
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
public class RetrieveDataResponse {

    private boolean isSuccessful;
    private byte[] data;
    private List<String> uploadResults;
    private Map<String, byte[]> referenceDataMap;
    private String error;
}
