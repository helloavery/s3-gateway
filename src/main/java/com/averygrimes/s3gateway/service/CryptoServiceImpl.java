package com.averygrimes.s3gateway.service;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.s3gateway.config.EhCacheManager;
import com.averygrimes.s3gateway.pojo.KeyType;
import com.averygrimes.s3gateway.pojo.S3GatewayDTO;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ehcache.Cache;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.UUID;

@Service
public class CryptoServiceImpl<T> implements CryptoService{

    private static final Logger LOGGER = LogManager.getLogger(CryptoServiceImpl.class);

    private EhCacheManager ehCacheManager;
    private Cache<String, SecretKey> secretKeyCache;
    private Cache<String, KeyPair> keyPairCache;
    private Cipher cipher;
    private KeyGenerator keyGenerator;
    private KeyPairGenerator keyPairGenerator;
    private KeyPair keyPair;
    private SecretKey secretKey;
    private Signature signature;
    private static final String RSA = "RSA";
    private static final String AES = "AES";
    private static final String AES_CBC_PKCS5PADDING = "AES/CBC/PKCS5Padding";
    private static final String SHA256 = "SHA256WithRSA";

    @Inject
    public CryptoServiceImpl(EhCacheManager ehCacheManager){
        this.ehCacheManager = ehCacheManager;
        this.setup();
    }

    private void setup(){
        try{
            Security.addProvider(new BouncyCastleProvider());
            this.keyGenerator = KeyGenerator.getInstance(AES);
            this.keyPairGenerator = KeyPairGenerator.getInstance(RSA);
            this.keyPairGenerator.initialize(2048);
            this.signature = Signature.getInstance(SHA256);
            this.secretKeyCache = ehCacheManager.getSecretKeyCache();
            this.keyPairCache = ehCacheManager.getKeyPairCache();
        }
        catch(Exception e){
            LOGGER.error("Error setting up and initializing service");
            throw new RuntimeException("Error setting up and initializing service",e);
        }
    }

    @Override
    public S3GatewayDTO generateSymmetricKey(byte[] publicKey) {
        try{
            String secretKeyUUID = generateUUID();
            String keyPairUUID = generateUUID();
            generateSecretKey();
            generateKeyPair();
            cacheSecretKey(secretKeyUUID, secretKey);
            cacheKeyPair(keyPairUUID, keyPair);
            PublicKey decodedPublicKey = KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(publicKey));
            byte[] encryptedSymmetricKey = encryptSymmetricKey(secretKey, decodedPublicKey);
            S3GatewayDTO response = new S3GatewayDTO();
            response.setSymmetricKeyUUID(secretKeyUUID);
            response.setSymmetricKey(encryptedSymmetricKey);
            response.setKeyPairUUID(keyPairUUID);
            return response;
        }
        catch(Exception e){
            LOGGER.error("Error generating and caching key pair");
            throw new RuntimeException("Error generating and caching key pair", e);
        }
    }

    @Override
    public S3GatewayDTO encryptAndSendSecrets(String data, String secretKeyUUID){
        try{
            getCachedKey(secretKeyUUID, KeyType.SECRET_KEY);
            byte[] encryptedData = encryptData(data, secretKey);
            byte[] generatedSignature = generateSignature(data.getBytes(StandardCharsets.UTF_8));
            S3GatewayDTO response = new S3GatewayDTO();
            response.setCipherText(encryptedData);
            response.setSignature(generatedSignature);
            response.setPublicKey(getPublicKey().getEncoded());
            return response;
        }
        catch(Exception e){
            LOGGER.error("Error encrypting secrets and generating signature");
            throw new RuntimeException("Error encrypting secrets and generating signature",e);
        }
    }

    @Override
    public String decryptAndUploadSecrets(byte[] cipherText, byte[] encodedPubKey, byte[] digitalSignature, String UUID){
        try{
            getCachedKey(UUID, KeyType.SECRET_KEY);
            byte[] decryptedData = decryptData(cipherText, secretKey);
            PublicKey decodedPublicKey = KeyFactory.getInstance(RSA).generatePublic(new X509EncodedKeySpec(encodedPubKey));
            if(!verifySignature(decodedPublicKey,decryptedData,digitalSignature)){
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
    public byte[] generateAndReturnCachedKeyPair(Long generatedLong){
        try{
            cacheKeyPair(null, null);
            return null;
            //return cachePreConfigured.get(generatedLong).getPublic().getEncoded();
        }
        catch(Exception e){
            LOGGER.error("Error generating and caching key pair");
            throw new RuntimeException("Error generating and caching key pair", e);
        }
    }

    private void cacheKeyPair(String key, KeyPair keyPair){
        try{
            keyPairCache.put(key, keyPair);
        }
        catch(Exception e){
            LOGGER.error("Error caching KeyPair");
            throw new RuntimeException("Error caching keypair",e);
        }
    }

    private void cacheSecretKey(String key, SecretKey secretKey){
        try{
            secretKeyCache.put(key,secretKey);
        }
        catch(Exception e){
            LOGGER.error("Error caching KeyPair");
            throw new RuntimeException("Error caching keypair",e);
        }
    }

    private void getCachedKey(String UUID, KeyType keyType){
        try{
            switch(keyType){
                case KEYPAIR:
                    keyPair = keyPairCache.get(UUID);
                    if(keyPair == null){
                        LOGGER.error("KeyPair not found! Either incorrect cache key passed or keypair cache expired");
                        throw new RuntimeException("KeyPair not found! Either incorrect cache key passed or keypair cache expired");
                    }
                    break;
                case SECRET_KEY:
                    secretKey = secretKeyCache.get(UUID);
                    if(secretKey == null){
                        LOGGER.error("SecretKey not found! Either incorrect cache key passed or secret key cache expired");
                        throw new RuntimeException("SecretKey not found! Either incorrect cache key passed or secret key cache expired");
                    }
                    break;
            }
        }
        catch(Exception e){
            LOGGER.error("Error retrieving cached key");
            throw new RuntimeException("Error retrieving cached key", e);
        }
    }

    private byte[] encryptData(String data, SecretKey secretKey){
        try{
            LOGGER.info("Encrypting retrieved secrets");
            cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] dataInBytes = data.getBytes(StandardCharsets.UTF_8);
            return cipher.doFinal(dataInBytes);
        }
        catch(Exception e){
            LOGGER.error("Error encrypting retrieved secrets");
            throw new RuntimeException("Error encrypted retrieved secrets",e);
        }
    }

    private byte[] encryptSymmetricKey(SecretKey secretKey, PublicKey publicKey){
        try{
            LOGGER.info("Encrypting symmetric key");
            cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.WRAP_MODE, publicKey);
            return cipher.wrap(secretKey);
        }
        catch(Exception e){
            LOGGER.error("Error encrypting retrieved secrets");
            throw new RuntimeException("Error encrypted retrieved secrets",e);
        }
    }

    private SecretKey decryptSymmetricKey(byte[] encryptedSecretKey, PublicKey publicKey){
        try{
            LOGGER.info("Decrypting symmetric key");
            cipher = Cipher.getInstance(RSA);
            cipher.init(Cipher.UNWRAP_MODE, publicKey);
            return (SecretKey) cipher.unwrap(encryptedSecretKey,AES, Cipher.SECRET_KEY);
        }
        catch(Exception e){
            LOGGER.error("Error encrypting retrieved secrets");
            throw new RuntimeException("Error encrypted retrieved secrets",e);
        }
    }

    private byte[] decryptData(byte[] data, SecretKey secretKey){
        try{
            LOGGER.info("decrypting secrets");
            cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
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

    private boolean verifySignature(PublicKey publicKey, byte[] data, byte[] digitalSignature) throws Exception{
        try{
            LOGGER.info("Verifying signature retrieved from requesting app");
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(digitalSignature);
        }
        catch(Exception e){
            LOGGER.error("Error verifying signature");
            throw e;
        }
    }

    private void generateSecretKey(){
        try{
            secretKey = keyGenerator.generateKey();
        }
        catch(Exception e){
            LOGGER.error("Error generating symmetric secret key");
            throw e;
        }
    }

    private void generateKeyPair(){
        LOGGER.info("Generating new key pair");
        keyPair = keyPairGenerator.generateKeyPair();
    }

    private PublicKey getPublicKey(){
        return keyPair.getPublic();
    }

    private PrivateKey getPrivateKey(){
        return keyPair.getPrivate();
    }

    private String generateUUID() {
        try {
            MessageDigest salt = MessageDigest.getInstance("SHA-256");
            salt.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(salt.digest());
        } catch (Exception e) {
            LOGGER.error("Error generating new UUID");
            throw new RuntimeException("Error generating new UUID", e);
        }
    }
}
