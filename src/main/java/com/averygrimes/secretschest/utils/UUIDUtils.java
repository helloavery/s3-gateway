package com.averygrimes.secretschest.utils;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * @author Avery Grimes-Farrow
 * Created on: 10/28/19
 * https://github.com/helloavery
 */

public class UUIDUtils {

    private static final Logger LOGGER = LogManager.getLogger(UUIDUtils.class);

    public static String generateUUID() {
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
