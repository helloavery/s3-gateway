package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.s3gateway.dto.S3GatewayDTO;

public interface CryptoService {

    S3GatewayDTO generateSymmetricKey(byte[] publicKey);

    S3GatewayDTO encryptAndSendSecrets(String data, String secretKeyUUID);

    String decryptAndUploadSecrets(byte[] cipherText, byte[] encodedPubKey, byte[] digitalSignature, String UUID);

    byte[] generateAndReturnCachedKeyPair(Long key);
}
