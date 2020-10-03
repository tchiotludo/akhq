package org.akhq.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BasicAuthTest {

    @Test
    void isValidPasswordSha256Invalid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "ec04b8cb3cb42d6e28f4472c33efe93e41b138637e5223d7d758c5bceff11df8";
        assertFalse(basicAuth.isValidPassword("Bad"));
    }


    @Test
    void isValidPasswordSha256Valid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "ec04b8cb3cb42d6e28f4472c33efe93e41b138637e5223d7d758c5bceff11df8";
        assertTrue(basicAuth.isValidPassword("TestAKHQ"));
    }

    @Test
    void isValidPasswordBCryptInvalid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.passwordHash = BasicAuth.PasswordHash.BCRYPT;
        basicAuth.password = "$2a$10$AtmQQDDSWnqsskzRxX1vGO0k6txZcJv.XlWRWA13.QOPq1wGB0DjS";
        assertFalse(basicAuth.isValidPassword("Bad"));
    }

    @Test
    void isValidPasswordBCryptValid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.passwordHash = BasicAuth.PasswordHash.BCRYPT;
        basicAuth.password = "$2a$10$AtmQQDDSWnqsskzRxX1vGO0k6txZcJv.XlWRWA13.QOPq1wGB0DjS";
        assertTrue(basicAuth.isValidPassword("TestAKHQ"));
    }

}