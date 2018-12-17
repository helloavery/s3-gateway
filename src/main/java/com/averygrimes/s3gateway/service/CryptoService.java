package com.averygrimes.s3gateway.service;
 
 /*=============================================================================
 |                Forecaster V1.0
 |
 |       File created by: Avery Grimes-Farrow
 |
 |       Created On:  2018-12-14            
 |            
 *===========================================================================*/

import java.io.UnsupportedEncodingException;
import java.security.PublicKey;

public interface CryptoService {

    PublicKey getPublicKey();

    void encryptData(String data) throws UnsupportedEncodingException;

    void decryptData(byte[] data);
}
