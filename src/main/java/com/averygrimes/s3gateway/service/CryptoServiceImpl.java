package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.s3gateway.config.EhCacheManager;
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
public class CryptoServiceImpl extends EhCacheManager implements CryptoService{

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
    public byte[] generateAndReturnCachedKeyPair(Long generatedLong){
        try{
            cacheKeyPair(generatedLong);
            return preConfigured.get(generatedLong).getPublic().getEncoded();
        }
        catch(Exception e){
            LOGGER.error("Error generating and caching key pair");
            throw new RuntimeException("Error generating and caching key pair", e);
        }
    }

    @Override
    public String cryptoPrepareSecretsS3Upload(byte[] cipherText, byte[] encodedPubKey, byte[] digitalSignature, Long key){
        try{
            byte[] decryptedData = decryptData(cipherText, true, key);
            if(!verifySignature(encodedPubKey,decryptedData,digitalSignature)){
                LOGGER.error("Signature verification came back as false");
                throw new RuntimeException("Signature verification came back as false");
            }
            return new String(decryptedData,StandardCharsets.UTF_8);
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets to be uploaded");
            throw new RuntimeException("Error decrypting secrets to be uploaded",e);
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

    private void cacheKeyPair(Long key){
        try{
            super.cacheConfig();
            generateKeyPair();
            preConfigured.put(key,keyPair);
        }
        catch(Exception e){
            LOGGER.error("Error caching KeyPair");
            throw new RuntimeException("Error caching keypair",e);
        }
    }

    private void getCachedKeyPair(Long key){
        try{
            keyPair = preConfigured.get(key);
            if(keyPair == null){
                LOGGER.error("KeyPair not found! Either incorrect cache key passed or keypair cache expired");
                throw new RuntimeException("KeyPair not found! Either incorrect cache key passed or keypair cache expired");
            }
        }
        catch(Exception e){
            LOGGER.error("Error retrieving cached keypair");
            throw new RuntimeException("Error retrieving cached keypair", e);
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
            LOGGER.error("Error encrypting retrieved secrets");
            throw new RuntimeException("Error encrypted retrieved secrets",e);
        }
    }

    private byte[] decryptData(byte[] data, boolean forCachedKeyPair, Long key){
        try{
            LOGGER.info("decrypting secrets");
            if(forCachedKeyPair){
                getCachedKeyPair(key);
            }
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            cipher.update(data);
            return cipher.doFinal(data);
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets");
            throw new RuntimeException("Error decrypting retrieved secrets",e);
        }
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

    private boolean verifySignature(byte[] encodedPubKey, byte[] data, byte[] digitalSignature) throws Exception{
        try{
            LOGGER.info("Verifying signature retrieved from requesting app");
            PublicKey publicKey = KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(encodedPubKey));
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(digitalSignature);
        }
        catch(Exception e){
            LOGGER.error("Error verifying signature");
            throw e;
        }
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
