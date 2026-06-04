package com.wms.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptHashPrinter {

    @Test
    void printHash() {
        String hash = new BCryptPasswordEncoder(12).encode("password123");
        System.out.println("===> BCRYPT HASH: " + hash);
    }
}
