package com.averygrimes.s3gateway.service;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

import javax.crypto.Cipher;
import java.io.UnsupportedEncodingException;
import java.security.PublicKey;
import java.security.Signature;

public class CryptoServiceImpl extends KeyProvider implements CryptoService{

    private static final String UTF_8 = "UTF-8";

    @Override
    public void cryptoInit() throws Exception{
        super.cryptoInit();
    }

    @Override
    public PublicKey getPublicKey(){
        return keyPair.getPublic();
    }

    @Override
    public void encryptData(String data) throws UnsupportedEncodingException {
        cipher.init(Cipher.ENCRYPT_MODE, );
        byte[] dataInBytes = data.getBytes(UTF_8);

    }

    @Override
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
