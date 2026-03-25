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
        private int nextChunkIndex;
        private final StringBuilder builder = new StringBuilder();

        String accept(int chunkIndex, int totalChunks, String payload) {
            if (totalChunks <= 0 || chunkIndex < 0 || chunkIndex >= totalChunks) {
                reset();
                return null;
            }

            if (chunkIndex == 0) {
                reset();
                expectedChunks = totalChunks;
                nextChunkIndex = 0;
            } else if (expectedChunks != totalChunks || chunkIndex != nextChunkIndex) {
                reset();
                return null;
            }

            builder.append(payload);
            nextChunkIndex++;
            if (chunkIndex + 1 < expectedChunks) {
                return null;
            }

            String result = builder.toString();
            reset();
            return result;
        }

        private void reset() {
            builder.setLength(0);
            expectedChunks = 0;
            nextChunkIndex = 0;
        }
    }
}
