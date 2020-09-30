package org.akhq.configs;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

class BasicAuthTest {

    @Test
    void isValidPasswordSha256Invalid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "xx";
        assertFalse(basicAuth.isValidPassword("Passw0rd"));
    }

    @Test
    void isValidPasswordSha256Valid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "ec04b8cb3cb42d6e28f4472c33efe93e41b138637e5223d7d758c5bceff11df8";
        assertTrue(basicAuth.isValidPassword("TestAKHQ"));
    }

    @Test
    void isValidPasswordCryptSha256Valid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "$5$ec04b8cb3cb42d6e28f4472c33efe93e41b138637e5223d7d758c5bceff11df8";
        assertFalse(basicAuth.isValidPassword("Bad"));
    }

    @Test
    void isValidPasswordCryptSha256Invalid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "$5$ec04b8cb3cb42d6e28f4472c33efe93e41b138637e5223d7d758c5bceff11df8";
        assertTrue(basicAuth.isValidPassword("TestAKHQ"));
    }

    @Test
    void isValidPasswordCryptSha512Invalid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "$6$211e747869b31084a70813d9e1dd8436bbde805db1429a5d322c4a6b1ab0bf304ea66fbb9e5457191f8568db035258845cd5f1b6b7c5439d21d888475fbe1f9c";
        assertFalse(basicAuth.isValidPassword("Bad"));
    }

    @Test
    void isValidPasswordCryptSha512Valid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "$6$211e747869b31084a70813d9e1dd8436bbde805db1429a5d322c4a6b1ab0bf304ea66fbb9e5457191f8568db035258845cd5f1b6b7c5439d21d888475fbe1f9c";
        assertTrue(basicAuth.isValidPassword("TestAKHQ"));
    }

    @Test
    void isValidPasswordBCryptInvalid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "$2a$10$AtmQQDDSWnqsskzRxX1vGO0k6txZcJv.XlWRWA13.QOPq1wGB0DjS";
        assertFalse(basicAuth.isValidPassword("Bad"));
    }

    @Test
    void isValidPasswordBCryptValid() {
        BasicAuth basicAuth = new BasicAuth();
        basicAuth.password = "$2a$10$AtmQQDDSWnqsskzRxX1vGO0k6txZcJv.XlWRWA13.QOPq1wGB0DjS";
        assertTrue(basicAuth.isValidPassword("TestAKHQ"));
    }

}