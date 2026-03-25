package com.modsync;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpFileServerTest {
    @Test
    void resolveApprovedEntryMatchesEncodedFileNameWithSpacesAndSymbols() {
        ManifestEntry approved = new ManifestEntry(
                CategoryType.MOD,
                "mods/My Cool+Mod 100%.jar",
                "My Cool+Mod 100%.jar",
                12L,
                "abc",
                true,
                true,
                ""
        );

        ManifestEntry resolved = HttpFileServer.resolveApprovedEntry(
                "/files/mod/mods/My%20Cool%2BMod%20100%25.jar",
                Map.of(approved.getIdentityKey(), approved)
        );

        assertEquals(approved.getIdentityKey(), resolved.getIdentityKey());
    }

    @Test
    void resolveApprovedEntryReturnsNullWhenEntryWasNotApproved() {
        ManifestEntry resolved = HttpFileServer.resolveApprovedEntry(
                "/files/mod/mods/Missing%20Mod.jar",
                Map.of()
        );

        assertNull(resolved);
    }

    @Test
    void resolveApprovedEntryRejectsEncodedTraversalPayload() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpFileServer.resolveApprovedEntry(
                        "/files/mod/mods/%2Fsecret.jar",
                        Map.of()
                ));
    }

    @Test
    void resolveApprovedEntryRejectsMalformedFilePathWithoutCategorySeparator() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpFileServer.resolveApprovedEntry("/files/mods-only", Map.of()));
    }
}
