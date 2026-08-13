package com.hackathon.gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the FPE cipher engine.
 *
 * <p>Verifies format preservation, reversibility, key-dependent transformation,
 * and pass-through behavior for non-digit characters.
 */
@ExtendWith(MockitoExtension.class)
class FpeServiceTest {

    @Mock
    private VaultKeyService vaultKeyService;

    @InjectMocks
    private FpeService fpeService;

    private static final String TEST_KEY = "3781495260"; // index 0->3, 1->7, 2->8, ...

    @BeforeEach
    void setUp() {
        when(vaultKeyService.getPermutationKey()).thenReturn(TEST_KEY);
    }

    @Test
    @DisplayName("Aadhaar: encrypted result is same length as input")
    void encrypt_preservesLength() {
        String aadhaar = "123456789012";
        String encrypted = fpeService.encryptDigits(aadhaar);
        assertThat(encrypted).hasSameSizeAs(aadhaar);
    }

    @Test
    @DisplayName("Aadhaar: encrypted result contains only digits")
    void encrypt_producesOnlyDigits() {
        String encrypted = fpeService.encryptDigits("123456789012");
        assertThat(encrypted).matches("\\d+");
    }

    @Test
    @DisplayName("FPE is fully reversible: decrypt(encrypt(x)) == x")
    void encryptDecrypt_roundTrip() {
        String original = "987654321012";
        String encrypted = fpeService.encryptDigits(original);
        String decrypted = fpeService.decryptDigits(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    @DisplayName("Non-digit characters pass through unchanged")
    void encrypt_nonDigitsPassThrough() {
        String withSpaces = "1234 5678 9012";
        String encrypted = fpeService.encryptDigits(withSpaces);
        // spaces must survive unchanged
        assertThat(encrypted.charAt(4)).isEqualTo(' ');
        assertThat(encrypted.charAt(9)).isEqualTo(' ');
    }

    @Test
    @DisplayName("Same input always produces same output (deterministic)")
    void encrypt_isDeterministic() {
        String input = "123456789012";
        assertThat(fpeService.encryptDigits(input))
                .isEqualTo(fpeService.encryptDigits(input));
    }

    @Test
    @DisplayName("Encrypted output is different from plaintext")
    void encrypt_producesDifferentValue() {
        // Unless the key is identity permutation, output should differ
        String input = "123456789012";
        String encrypted = fpeService.encryptDigits(input);
        // With key "3781495260", digit '0' maps to '3' so first char should change
        assertThat(encrypted).isNotEqualTo(input);
    }

    @Test
    @DisplayName("Empty string returns empty string")
    void encrypt_emptyString() {
        assertThat(fpeService.encryptDigits("")).isEmpty();
        assertThat(fpeService.decryptDigits("")).isEmpty();
    }
}
