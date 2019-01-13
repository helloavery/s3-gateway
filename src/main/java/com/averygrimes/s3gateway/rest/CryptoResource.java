package com.averygrimes.s3gateway.rest;

import com.averygrimes.s3gateway.dto.S3GatewayDTO;
import com.averygrimes.s3gateway.service.CryptoService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2019-01-04
 * https://github.com/helloavery
 */

@RestController
@RequestMapping(value = "/v1/crypto")
public class CryptoResource {

    private CryptoService cryptoService;

    public CryptoResource(CryptoService cryptoService){
        this.cryptoService = cryptoService;
    }

    @RequestMapping(value = "/generateSymmetricKey", method = RequestMethod.POST)
    public S3GatewayDTO addItemRequest(@RequestBody byte[] publicKey){
        return cryptoService.generateSymmetricKey(publicKey);
    }

    @RequestMapping(value = "/requestPubKey", method = RequestMethod.POST)
    public byte[] addItemRequest(@RequestBody Long key){
        return cryptoService.generateAndReturnCachedKeyPair(key);
    }
}
