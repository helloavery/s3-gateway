package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.s3gateway.dto.S3GatewayDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

@Service
public class CryptoServiceImpl implements CryptoService{

    private static final Logger LOGGER = LogManager.getLogger(CryptoServiceImpl.class);

    private Cipher cipher;
    private KeyPairGenerator keyPairGenerator;
    private KeyPair keyPair;
    private Signature signature;
    private static final String RSA = "RSA";
    private static final String DSA = "DSA";
    private static final String AES_CBC_PKCS5PADDING = "AES/CBC/PKCS5Padding";
    private static final String SHA256 = "SHA256WithRSA";

    public CryptoServiceImpl(){
        this.setup();
    }

    private void setup(){
        try{
            Security.addProvider(new BouncyCastleProvider());
            this.cipher = Cipher.getInstance(RSA);
            this.keyPairGenerator = KeyPairGenerator.getInstance(RSA);
            this.keyPairGenerator.initialize(2048);
            this.signature = Signature.getInstance(SHA256);
        }
        catch(Exception e){
            LOGGER.error("Error setting up and initializing service");
            throw new RuntimeException("Error setting up and initializing service",e);
        }
    }

    @Override
    public S3GatewayDTO cryptoPrepareSendSecrets(String data, byte[] publicKey){
        try{
            byte[] encryptedData = encryptData(data, publicKey);
            byte[] generatedSignature = generateSignature(data.getBytes(StandardCharsets.UTF_8));
            return new S3GatewayDTO(encryptedData, getPublicKey().getEncoded(), generatedSignature);
        }
        catch(Exception e){
            LOGGER.error("Error encrypting secrets and generating signature");
            throw new RuntimeException("Error encrypting secrets and generating signature",e);
        }
    }

    private byte[] encryptData(String data, byte[] publicKey){
        try{
            LOGGER.info("Encrypting retrieved secrets");
            PublicKey decodedPublicKey = KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(publicKey));
            cipher.init(Cipher.ENCRYPT_MODE, decodedPublicKey);
            byte[] dataInBytes = data.getBytes(StandardCharsets.UTF_8);
            return cipher.doFinal(dataInBytes);
        }
        catch(Exception e){
            LOGGER.error("Error encrypted retrieved secrets");
            throw new RuntimeException("Error encrypted retrieved secrets",e);
        }
    }

    private void decryptData(byte[] data){
        //cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        //cipher.update(data);
        //cipher.doFinal(data);

    }

    private byte[] generateSignature(byte[] data) throws Exception{
        try{
            generateKeyPair();
            LOGGER.info("Generating signature");
            SecureRandom secureRandom = new SecureRandom();
            signature.initSign(getPrivateKey(),secureRandom);
            signature.update(data);
            return signature.sign();
        }
        catch(Exception e){
            LOGGER.error("Error generating signature for data");
            throw e;
        }
    }

    private boolean verifySignature() throws Exception{
        Signature signature = Signature.getInstance(SHA256);
        //signature.initVerify();
        //signature.update();
        //return signature.verify();
        return false;
    }

    private void generateKeyPair(){
        LOGGER.info("Generating new key pair");
        keyPair = keyPairGenerator.generateKeyPair();
    }

    @Override
    public PublicKey getPublicKey(){
        return keyPair.getPublic();
    }

    private PrivateKey getPrivateKey(){
        return keyPair.getPrivate();
    }
}
