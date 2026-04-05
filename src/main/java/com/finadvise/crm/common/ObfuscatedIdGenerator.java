package com.finadvise.crm.common;

import jakarta.annotation.PostConstruct;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ObfuscatedIdGenerator {
    private final String salt;
    private final int minLength;
    private final String alphabet;
    private static final int DB_COLUMN_MAX = 20;
    private static final long HASHIDS_LIMIT = 9007199254740991L;
    private Hashids hashids;

    public ObfuscatedIdGenerator(
            @Value("${HASHID_SALT}") String salt,
            @Value("${HASHID_ALPHABET}") String alphabet,
            @Value("${HASHID_LENGTH}") int minLength) {
        this.salt = salt;
        this.alphabet = alphabet;
        this.minLength = minLength;
    }

    @PostConstruct
    public void init() {
        this.hashids = new Hashids(salt, minLength, alphabet);

        // Sanity check: Test a very large ID to ensure it fits the column
        String testHash = hashids.encode(HASHIDS_LIMIT);

        if (testHash.length() > DB_COLUMN_MAX) {
            throw new IllegalStateException(
                    String.format("Hashid configuration exceeds DB column limit! Hash: %s, Max allowed: %s",
                            testHash.length(), DB_COLUMN_MAX)
            );
        }
    }

    public String encode(Long id) {
        return hashids.encode(id);
    }
}
