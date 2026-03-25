package com.modsync;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class HttpPathCodec {
    private HttpPathCodec() {
    }

    public static String encodeRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }

        String[] segments = relativePath.split("/");
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(encodeSegment(segments[i]));
        }
        return encoded.toString();
    }

    public static String decodeRelativePath(String encodedPath) {
        if (encodedPath == null || encodedPath.isBlank()) {
            return "";
        }

        String[] segments = encodedPath.split("/");
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                decoded.append('/');
            }
            decoded.append(decodeSegment(segments[i]));
        }
        return decoded.toString();
    }

    private static String encodeSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String decodeSegment(String segment) {
        String decoded = URLDecoder.decode(segment, StandardCharsets.UTF_8);
        if (decoded.contains("/") || decoded.contains("\\") || decoded.equals(".") || decoded.equals("..")) {
            throw new IllegalArgumentException("Blocked encoded path traversal segment: " + segment);
        }
        return decoded;
    }
}
