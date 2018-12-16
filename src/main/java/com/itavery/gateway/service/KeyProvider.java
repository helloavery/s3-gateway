package com.itavery.gateway.service;
 
 /*=============================================================================
 |                Forecaster V1.0
 |
 |       File created by: Avery Grimes-Farrow
 |
 |       Created On:  2018-12-16            
 |            
 *===========================================================================*/

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;

public abstract class KeyProvider {

    protected static KeyPair keyPair;
    protected static Cipher cipher;

    public void cryptoInit() throws Exception{
        providerInit();
        cipherGetInstance();
    }

    private static void providerInit() {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static void cipherGetInstance()) throws Exception {
        cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    }

    private static void generateKey() {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        keyPair = keyPairGenerator.generateKeyPair();
    }
}
