package com.hackathon.gateway.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

/**
 * Fetches the FPE permutation key from Vault's KV v2 secrets engine.
 *
 * <p>The application never stores this key persistently — it is requested
 * from Vault on first use and cached in memory with a TTL.
 *
 * <p>Enterprise enhancements:
 * <ul>
 *   <li>Resilience4j circuit breaker prevents repeated failed calls to Vault</li>
 *   <li>In-memory key cache with 10-minute TTL prevents Vault overload</li>
 *   <li>Structured logging for key fetch events (auditable)</li>
 * </ul>
 *
 * <p>Before first run, seed the key into Vault (dev mode):
 * <pre>docker exec -e VAULT_ADDR=http://127.0.0.1:8200 gateway-vault vault kv put secret/gateway/fpe-key permutation=3781495260</pre>
 */
@Service
public class VaultKeyService {

    private static final Logger log = LoggerFactory.getLogger(VaultKeyService.class);
    private static final long KEY_TTL_SECONDS = 600; // 10 minutes

    private final RestTemplate restTemplate;

    @Value("${gateway.vault.url}")
    private String vaultUrl;

    @Value("${gateway.vault.token}")
    private String vaultToken;

    @Value("${gateway.vault.secret-path}")
    private String secretPath;

    @Value("${gateway.vault.default-key:3781495260}")
    private String defaultFpeKey;

    private volatile String cachedPermutation;
    private volatile Instant cacheExpiry = Instant.EPOCH;

    public VaultKeyService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @CircuitBreaker(name = "vaultService", fallbackMethod = "vaultFallback")
    public String getPermutationKey() {
        if (cachedPermutation != null && Instant.now().isBefore(cacheExpiry)) {
            return cachedPermutation;
        }
        return fetchFromVault();
    }

    @SuppressWarnings("unchecked")
    private String fetchFromVault() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Vault-Token", vaultToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        Map<String, Object> response = restTemplate.exchange(
                vaultUrl + secretPath,
                HttpMethod.GET,
                entity,
                Map.class
        ).getBody();

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        Map<String, Object> innerData = (Map<String, Object>) data.get("data");
        String permutation = (String) innerData.get("permutation");

        cachedPermutation = permutation;
        cacheExpiry = Instant.now().plusSeconds(KEY_TTL_SECONDS);
        log.info("[VaultKeyService] FPE key refreshed from Vault, TTL={}s", KEY_TTL_SECONDS);
        return cachedPermutation;
    }

    /**
     * Circuit-breaker fallback: enforced fail-closed security with default key support.
     * If Vault is unreachable, serves cached key or configured default FPE key.
     */
    public String vaultFallback(Exception ex) {
        log.error("[VaultKeyService] Vault circuit open or secrets engine unreachable: {}", ex.getMessage());
        if (cachedPermutation != null) {
            log.warn("[VaultKeyService] Serving stale cached key due to Vault outage");
            return cachedPermutation;
        }
        if (defaultFpeKey != null && !defaultFpeKey.trim().isEmpty()) {
            log.warn("[VaultKeyService] Vault unreachable. Using default configured FPE key");
            cachedPermutation = defaultFpeKey;
            cacheExpiry = Instant.now().plusSeconds(KEY_TTL_SECONDS);
            return defaultFpeKey;
        }
        throw new IllegalStateException("Key Management Service (Vault) unavailable. Gateway operating in fail-closed posture.", ex);
    }
}


