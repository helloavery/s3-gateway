package com.itavery.gateway.service;
 
 /*=============================================================================
 |                Forecaster V1.0
 |
 |       File created by: Avery Grimes-Farrow
 |
 |       Created On:  2018-12-14            
 |            
 *===========================================================================*/

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;

public class CryptoServiceImpl extends KeyProvider{

    private static final String UTF_8 = "UTF-8";

    @Override
    public void cryptoInit() throws Exception{
        super.cryptoInit();
    }

    public PublicKey getPublicKey(){
        return keyPair.getPublic();
    }

    public void encryptData(String data){
        cipher.init(Cipher.ENCRYPT_MODE, );
        byte[] dataInBytes = data.getBytes(UTF_8);

    }

    public void decryptData(byte[] data){
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        //cipher.update(data);
        cipher.doFinal(data);

    }

    private boolean verifySignature() throws Exception{
        Signature signature = Signature.getInstance("SHA256WithDSA");
        signature.initVerify();
        signature.update();
        return signature.verify();
    }
}
