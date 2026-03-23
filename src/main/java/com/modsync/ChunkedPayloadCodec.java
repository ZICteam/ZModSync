package com.modsync;

import java.util.ArrayList;
import java.util.List;

final class ChunkedPayloadCodec {
    private ChunkedPayloadCodec() {
    }

    static List<String> split(String payload, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (payload == null || payload.isEmpty()) {
            chunks.add("");
            return chunks;
        }

        for (int index = 0; index < payload.length(); index += chunkSize) {
            chunks.add(payload.substring(index, Math.min(payload.length(), index + chunkSize)));
        }
        return chunks;
    }

    static final class ChunkAccumulator {
        private int expectedChunks;
        private final StringBuilder builder = new StringBuilder();

        String accept(int chunkIndex, int totalChunks, String payload) {
            if (chunkIndex == 0) {
                builder.setLength(0);
                expectedChunks = totalChunks;
            }

            builder.append(payload);
            if (chunkIndex + 1 < expectedChunks) {
                return null;
            }

            String result = builder.toString();
            builder.setLength(0);
            expectedChunks = 0;
            return result;
        }
    }
}
