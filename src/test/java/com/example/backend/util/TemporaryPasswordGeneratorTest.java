package com.example.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TemporaryPasswordGeneratorTest {

    @Test
    void generatesTssPrefixedAlphanumericPassword() {
        String password = TemporaryPasswordGenerator.generate();
        assertThat(password).startsWith("TSS");
        assertThat(password).hasSize(12);
        assertThat(password.substring(3)).matches("[A-Za-z0-9]+");
        assertThat(password).doesNotContain("I", "O", "l", "1", "0");
    }

    @Test
    void generatesDistinctPasswords() {
        String first = TemporaryPasswordGenerator.generate();
        String second = TemporaryPasswordGenerator.generate();
        assertThat(first).isNotEqualTo(second);
    }
}
