package com.averygrimes.secretschest.service;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.DecryptRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyRequest;
import com.amazonaws.services.kms.model.GenerateDataKeyResult;
import com.averygrimes.secretschest.pojo.SecretsChestConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;

@Service
public class CryptoServiceImpl implements CryptoService{

    private static final Logger LOGGER = LogManager.getLogger(CryptoServiceImpl.class);

    private AWSKMS kmsClient;
    private Cipher cipher;
    private static final String AES_256 = "AES_256";

    @PostConstruct
    private void init(){
        try{
            Security.addProvider(new BouncyCastleProvider());
            this.kmsClient = AWSKMSClientBuilder.standard().withRegion(Region.US_EAST_2.toString()).build();
        }
        catch(Exception e){
            LOGGER.error("Error setting up and initializing service");
            throw new RuntimeException("Error setting up and initializing service",e);
        }
    }

    @Override
    public Map<String, Object> generateDataKeyAndEncryptData(byte[] dataToUpload){
        try{
            GenerateDataKeyResult dataKeyResult = generateDataKey();
            ByteBuffer plaintextKey = dataKeyResult.getPlaintext();
            ByteBuffer encryptedKey = dataKeyResult.getCiphertextBlob();

            SecretKey plaintextSecretKey = getExistingSecretKey(convertByteBufferToByteArray(plaintextKey));
            byte[] encryptedData = encryptData(dataToUpload, plaintextSecretKey);
            Map<String, Object> encryptedDataAndKey = new HashMap<>();
            encryptedDataAndKey.put(SecretsChestConstants.ENCRYPTED_DATA_MAP_KEY, encryptedData);
            encryptedDataAndKey.put(SecretsChestConstants.ENCRYPTED_KEY_MAP_KEY, encryptedKey);
            return encryptedDataAndKey;
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets to be uploaded");
            throw new RuntimeException("Error decrypting secrets to be uploaded",e);
        }
    }

    @Override
    public byte[] encryptDataWithoutGeneratingDataKey(byte[] dataToUpload, ByteBuffer encryptedKey){
        try{
            ByteBuffer decryptedKey = decryptDataKeyAndReturnPlainTextKey(encryptedKey);
            SecretKey plaintextSecretKey = getExistingSecretKey(convertByteBufferToByteArray(decryptedKey));
            return encryptData(dataToUpload, plaintextSecretKey);
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets to be uploaded");
            throw new RuntimeException("Error decrypting secrets to be uploaded",e);
        }
    }

    @Override
    public byte[] decryptData(byte[] encryptedData, ByteBuffer encryptedKey){
        try{
            ByteBuffer plaintextKey = decryptDataKeyAndReturnPlainTextKey(encryptedKey);
            SecretKey plaintextSecretKey = getExistingSecretKey(convertByteBufferToByteArray(plaintextKey));
            return decryptData(encryptedData, plaintextSecretKey);
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets to be uploaded");
            throw new RuntimeException("Error decrypting secrets to be uploaded",e);
        }
    }

    private GenerateDataKeyResult generateDataKey(){
        String keyId = "arn:aws:kms:us-west-2:111122223333:key/1234abcd-12ab-34cd-56ef-1234567890ab";

        GenerateDataKeyRequest dataKeyRequest = new GenerateDataKeyRequest();
        dataKeyRequest.setKeyId(keyId);
        dataKeyRequest.setKeySpec("AES_256");

        return kmsClient.generateDataKey(dataKeyRequest);
    }

    private ByteBuffer decryptDataKeyAndReturnPlainTextKey(ByteBuffer ciphertextBlob){
        DecryptRequest req = new DecryptRequest().withCiphertextBlob(ciphertextBlob);
        return kmsClient.decrypt(req).getPlaintext();
    }

    private SecretKey getExistingSecretKey(byte[] encodedSecretKey){
        return new SecretKeySpec(encodedSecretKey, 0, encodedSecretKey.length, AES_256);
    }

    private byte[] convertByteBufferToByteArray(ByteBuffer byteBuffer){
        byte[] bytesArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytesArray, 0, bytesArray.length);
        return bytesArray;
    }

    private byte[] encryptData(byte[] dataToEncrypt, SecretKey secretKey) {
        try {
            LOGGER.info("Encrypting retrieved secrets");
            cipher = Cipher.getInstance(AES_256);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(dataToEncrypt);
        } catch (Exception e) {
            LOGGER.error("Error encrypting retrieved secrets");
            throw new RuntimeException("Error encrypted retrieved secrets", e);
        }
    }

    private byte[] decryptData(byte[] encryptedData, SecretKey secretKey){
        try{
            LOGGER.info("decrypting secrets");
            cipher = Cipher.getInstance(AES_256);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedData);
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets");
            throw new RuntimeException("Error decrypting retrieved secrets",e);
        }
    }
}
