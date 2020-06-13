package com.averygrimes.secretschest.service;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import com.averygrimes.secretschest.config.ProgramArguments;
import com.averygrimes.secretschest.exceptions.SecretsChestCryptoException;
import com.averygrimes.secretschest.pojo.SecretsChestConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DecryptRequest;
import software.amazon.awssdk.services.kms.model.DecryptResponse;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CryptoServiceImpl implements CryptoService{

    private static final Logger LOGGER = LogManager.getLogger(CryptoServiceImpl.class);

    private ProgramArguments programArguments;
    private KmsClient kmsClient;
    private Cipher cipher;
    private static final String AES = "AES";
    private static final String AES_256 = "AES_256";

    @Inject
    public void setProgramArguments(ProgramArguments programArguments) {
        this.programArguments = programArguments;
    }

    @PostConstruct
    private void init(){
        try{
            Security.addProvider(new BouncyCastleProvider());
            this.kmsClient = KmsClient.builder().region(Region.US_EAST_2).credentialsProvider(awsCredentialsProviderSetup()).build();
        }
        catch(Exception e){
            LOGGER.error("Error setting up and initializing service", e);
            throw new SecretsChestCryptoException("Error setting up and initializing service", e);
        }
    }

    @Override
    public Map<String, byte[]> generateDataKeyAndEncryptData(byte[] dataToUpload){
        try{
            GenerateDataKeyResponse dataKeyResult = generateDataKey();
            SdkBytes plaintextKey = dataKeyResult.plaintext();
            SdkBytes encryptedKey = dataKeyResult.ciphertextBlob();

            SecretKey plaintextSecretKey = getExistingSecretKey(plaintextKey.asByteArray());
            byte[] encryptedData = encryptData(dataToUpload, plaintextSecretKey);
            Map<String, byte[]> encryptedDataAndKey = new ConcurrentHashMap<>();
            encryptedDataAndKey.put(SecretsChestConstants.ENCRYPTED_DATA_MAP_KEY, encryptedData);
            encryptedDataAndKey.put(SecretsChestConstants.ENCRYPTED_KEY_MAP_KEY, encryptedKey.asByteArray());
            return encryptedDataAndKey;
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets to be uploaded", e);
            throw new SecretsChestCryptoException("Error decrypting secrets to be uploaded", e);
        }
    }

    @Override
    public byte[] encryptDataWithoutGeneratingDataKey(byte[] dataToUpload, SdkBytes encryptedKey){
        try{
            SdkBytes decryptedKey = decryptDataKeyAndReturnPlainTextKey(encryptedKey);
            SecretKey plaintextSecretKey = getExistingSecretKey(decryptedKey.asByteArray());
            return encryptData(dataToUpload, plaintextSecretKey);
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets to be uploaded", e);
            throw new SecretsChestCryptoException("Error decrypting secrets to be uploaded", e);
        }
    }

    @Override
    public byte[] decryptData(byte[] encryptedData, ByteBuffer encryptedKey){
        try{
            SdkBytes plaintextKey = decryptDataKeyAndReturnPlainTextKey(SdkBytes.fromByteBuffer(encryptedKey));
            SecretKey plaintextSecretKey = getExistingSecretKey(plaintextKey.asByteArray());
            return decryptData(encryptedData, plaintextSecretKey);
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets to be uploaded", e);
            throw new SecretsChestCryptoException("Error decrypting secrets to be uploaded: " + e.getMessage());
        }
    }

    private AwsCredentialsProvider awsCredentialsProviderSetup(){
        LOGGER.info("Retrieving IAM credentials");
        AwsCredentials credentials = ProfileCredentialsProvider.create("kmsUser").resolveCredentials();
        return StaticCredentialsProvider.create(credentials);
    }

    private GenerateDataKeyResponse generateDataKey(){
        String keyId = programArguments.getKmsKeyARN();
        GenerateDataKeyRequest dataKeyRequest = GenerateDataKeyRequest.builder().keyId(keyId).keySpec(AES_256).build();
        return kmsClient.generateDataKey(dataKeyRequest);
    }

    private SdkBytes decryptDataKeyAndReturnPlainTextKey(SdkBytes ciphertextBlob){
        DecryptRequest req = DecryptRequest.builder().ciphertextBlob(ciphertextBlob).build();
        DecryptResponse res = kmsClient.decrypt(req);
        return res.plaintext();
    }

    private SecretKey getExistingSecretKey(byte[] encodedSecretKey){
        return new SecretKeySpec(encodedSecretKey, 0, encodedSecretKey.length, AES_256);
    }

    private byte[] encryptData(byte[] dataToEncrypt, SecretKey secretKey) {
        try {
            LOGGER.info("Encrypting retrieved secrets");
            cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(dataToEncrypt);
        } catch (Exception e) {
            LOGGER.error("Error encrypting retrieved secrets", e);
            throw new SecretsChestCryptoException("Error encrypted retrieved secrets", e);
        }
    }

    private byte[] decryptData(byte[] encryptedData, SecretKey secretKey){
        try{
            LOGGER.info("decrypting secrets");
            cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedData);
        }
        catch(Exception e){
            LOGGER.error("Error decrypting secrets", e);
            throw new SecretsChestCryptoException("Error decrypting retrieved secrets: " + e.getMessage());
        }
    }
}
