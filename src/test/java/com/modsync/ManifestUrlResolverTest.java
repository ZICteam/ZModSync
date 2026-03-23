package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManifestUrlResolverTest {
    @Test
    void extractHostRemovesPortFromIpv4StyleAddress() {
        assertEquals("mc.example.com", ManifestUrlResolver.extractHost("mc.example.com:25565"));
    }

    @Test
    void extractHostKeepsBareIpv6Address() {
        assertEquals("2001:db8::42", ManifestUrlResolver.extractHost("2001:db8::42"));
    }

    @Test
    void extractHostReadsBracketedIpv6WithPort() {
        assertEquals("2001:db8::42", ManifestUrlResolver.extractHost("[2001:db8::42]:25565"));
    }

    @Test
    void normalizeHostWrapsIpv6ButNotIpv4() {
        assertEquals("[2001:db8::42]", ManifestUrlResolver.normalizeHost("2001:db8::42"));
        assertEquals("mc.example.com", ManifestUrlResolver.normalizeHost("mc.example.com"));
    }

    @Test
    void resolvePublicBaseUrlPrefersConfiguredValueAndTrimsTrailingSlash() {
        assertEquals(
                "https://cdn.example.com/modsync",
                ManifestUrlResolver.resolvePublicBaseUrl("https://cdn.example.com/modsync///", "ignored.example.com", 8080)
        );
    }

    @Test
    void resolvePublicBaseUrlFallsBackToDirectHttpAddress() {
        assertEquals(
                "http://[2001:db8::42]:8080",
                ManifestUrlResolver.resolvePublicBaseUrl("", "2001:db8::42", 8080)
        );
    }

    @Test
    void buildManifestCandidateUrlsKeepsDeterministicFallbackOrder() {
        assertEquals(
                List.of(
                        "http://mc.example.com:9000/manifest",
                        "http://mc.example.com:8080/manifest",
                        "https://mc.example.com/manifest",
                        "http://mc.example.com/manifest"
                ),
                ManifestUrlResolver.buildManifestCandidateUrls("mc.example.com", 9000, 8080)
        );
    }

    @Test
    void buildManifestCandidateUrlsAvoidsDuplicatesWhenPortsMatch() {
        assertEquals(
                List.of(
                        "http://mc.example.com:8080/manifest",
                        "https://mc.example.com/manifest",
                        "http://mc.example.com/manifest"
                ),
                ManifestUrlResolver.buildManifestCandidateUrls("mc.example.com", 8080, 8080)
        );
    }
}
