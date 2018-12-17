package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-16
 * https://github.com/helloavery
 */

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

public abstract class KeyProvider {

    protected static KeyPair keyPair;
    protected static Cipher cipher;

    public void cryptoInit() throws Exception{
        providerInit();
        cipherGetInstance();
        generateKey();
    }

    private static void providerInit() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static void cipherGetInstance() throws NoSuchAlgorithmException, NoSuchPaddingException {
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    }

    private static void generateKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
    }
}
