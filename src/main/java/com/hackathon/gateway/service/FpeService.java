package com.hackathon.gateway.service;

import org.springframework.stereotype.Service;

/**
 * Simplified Format-Preserving Encryption for the hackathon demo.
 *
 * Real FPE (e.g. FF3-1, as used in production systems) is a full
 * Feistel-network cipher. For this demo we use a keyed digit-substitution
 * cipher: the Vault-issued permutation is a rearrangement of the digits
 * 0-9, and every digit in the field is mapped through it. This keeps the
 * same property that matters for the pitch - a 12-digit Aadhaar number
 * encrypts to another valid-looking 12-digit number, and is fully
 * reversible with the same key - without pulling in an external crypto
 * library for a time-boxed hackathon build.
 */
@Service
public class FpeService {

    private final VaultKeyService vaultKeyService;

    public FpeService(VaultKeyService vaultKeyService) {
        this.vaultKeyService = vaultKeyService;
    }

    public String encryptDigits(String digits) {
        String permutation = vaultKeyService.getPermutationKey();
        StringBuilder out = new StringBuilder();
        for (char c : digits.toCharArray()) {
            if (Character.isDigit(c)) {
                int index = c - '0';
                out.append(permutation.charAt(index));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public String decryptDigits(String encrypted) {
        String permutation = vaultKeyService.getPermutationKey();
        StringBuilder out = new StringBuilder();
        for (char c : encrypted.toCharArray()) {
            if (Character.isDigit(c)) {
                int index = permutation.indexOf(c);
                out.append(index >= 0 ? (char) ('0' + index) : c);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
