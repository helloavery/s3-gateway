package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

public interface CryptoService {

    PublicKey getPublicKey();

    void encryptData(String data) throws UnsupportedEncodingException;

    void decryptData(byte[] data);
}
