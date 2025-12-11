// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.msg;

import com.github.f4b6a3.ulid.Ulid;
import io.dymes.client.vsr.UnmarshalFailure;
import io.dymes.model.DymesMessage;
import io.dymes.model.Message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Reader for Dymes wire-format message frames.
 * Deserializes binary data into {@link DymesMessage} instances.
 *
 * <h2>Frame Header Format (128 bytes)</h2>
 * <pre>
 * Offset   | Size | Field
 * ---------|------|------------------
 * 0-15     | 16   | Header Checksum
 * 16-23    | 8    | Log Sequence Number
 * 24-31    | 8    | Reserved
 * 32-47    | 16   | Message ULID
 * 48-63    | 16   | Correlation ULID
 * 64-79    | 16   | Channel ID
 * 80-95    | 16   | Routing Data
 * 96-99    | 4    | Frame Size
 * 100-103  | 4    | KV Header Count
 * 104-107  | 4    | KV Header Data Size
 * 108-111  | 4    | Content Data Size
 * 112-127  | 16   | Payload Checksum
 * </pre>
 *
 * <h2>KV Header Entry (16 bytes per entry)</h2>
 * <pre>
 * Offset | Size | Field
 * -------|------|-------------
 * 0-3    | 4    | Key Offset
 * 4-7    | 4    | Key Length
 * 8-11   | 4    | Value Offset
 * 12-15  | 4    | Value Length
 * </pre>
 */
public class DymesMessageReader {

    /**
     * Reads a single Dymes message from the input stream.
     *
     * @param in the input stream to read from
     * @return the deserialized DymesMessage
     * @throws UnmarshalFailure if reading fails or stream ends unexpectedly
     */
    public static DymesMessage readOne(InputStream in) {
        return readOneInternal(in);
    }

    /**
     * Reads all available Dymes messages from the input stream until EOF.
     *
     * @param in the input stream to read from
     * @return a list of deserialized Messages
     * @throws UnmarshalFailure if reading fails
     */
    public static List<Message> readAll(InputStream in) {
        try {
            byte[] allBytes = in.readAllBytes();
            InputStream bufferedIn = new ByteArrayInputStream(allBytes);

            List<Message> messages = new ArrayList<>();
            while (bufferedIn.available() > 0) {
                messages.add(readOneInternal(bufferedIn));
            }
            return messages;
        } catch (IOException e) {
            throw new UnmarshalFailure("Failed to read messages from input stream", e);
        }
    }

    private static DymesMessage readOneInternal(InputStream in){
        try {
            // -------------------- FRAME HEADER (128 bytes) --------------------

            BigInteger headerChecksum = readU128(in);

            long logSeq        = reverseLong(readLong(in));
            long reserved      = reverseLong(readLong(in));

            Ulid messageId     = readUlid(in);
            Ulid correlationId = readUlid(in);

            BigInteger channel = readU128(in);
            BigInteger routing = readU128(in);

            int frameSize   = reverseInt(readInt(in));
            int headerCount = reverseInt(readInt(in));
            int kvDataSize  = reverseInt(readInt(in));
            int payloadSize = reverseInt(readInt(in));

            BigInteger payloadChecksum = readU128(in);

            // -------------------- KV CONTROL BLOCK --------------------

            int[] keyOffsets   = new int[headerCount];
            int[] keyLengths   = new int[headerCount];
            int[] valueOffsets = new int[headerCount];
            int[] valueLengths = new int[headerCount];

            for (int i = 0; i < headerCount; i++) {
                keyOffsets[i]   = reverseInt(readInt(in));
                keyLengths[i]   = reverseInt(readInt(in));
                valueOffsets[i] = reverseInt(readInt(in));
                valueLengths[i] = reverseInt(readInt(in));
            }

            // -------------------- KV DATA --------------------

            byte[] kvDataBytes = readBytes(in, kvDataSize);

            // -------------------- PAYLOAD --------------------

            byte[] payloadBytes = readBytes(in, payloadSize);

            // -------------------- PADDING --------------------

            int consumed = 128 + (headerCount * 16) + kvDataSize + payloadSize;
            int padding  = frameSize - consumed;

            if (padding > 0) {
                skipBytes(in, padding);
            }

            // -------------------- RECONSTRUCT HEADERS --------------------

            Map<String, String> headers = new HashMap<>();
            int dataStart = 128 + (headerCount * 16);

            for (int i = 0; i < headerCount; i++) {
                String key = new String(kvDataBytes, keyOffsets[i] - dataStart, keyLengths[i], UTF_8);
                String value = new String(kvDataBytes, valueOffsets[i] - dataStart, valueLengths[i], UTF_8);
                headers.put(key, value);
            }

            // -------------------- BUILD MESSAGE --------------------

            return new DymesMessage(
                    messageId,
                    correlationId,
                    channel,
                    routing,
                    headers,
                    new String(payloadBytes, UTF_8)
            );
        } catch (IOException e) {
            throw new UnmarshalFailure("Failed to read Dymes message from input stream", e);
        }

    }

    // ---------------------------------------------------------------------
    // ------------------------- Helper Methods ----------------------------
    // ---------------------------------------------------------------------

    private static byte[] readBytes(InputStream in, int len) throws IOException {
        byte[] buf = new byte[len];
        int read = 0;
        while (read < len) {
            int r = in.read(buf, read, len - read);
            if (r < 0) throw new UnmarshalFailure("Unexpected end of stream");
            read += r;
        }
        return buf;
    }

    private static void skipBytes(InputStream in, int len) throws IOException {
        long skipped = 0;
        while (skipped < len) {
            long s = in.skip(len - skipped);
            if (s <= 0) throw new UnmarshalFailure("Unexpected end of stream during padding skip");
            skipped += s;
        }
    }

    private static int readInt(InputStream in) throws IOException {
        byte[] b = readBytes(in, 4);
        return ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    private static long readLong(InputStream in) throws IOException {
        byte[] b = readBytes(in, 8);
        return ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    private static int reverseInt(int i) {
        return Integer.reverseBytes(i);
    }

    private static long reverseLong(long l) {
        return Long.reverseBytes(l);
    }

    private static BigInteger readU128(InputStream in) throws IOException {
        byte[] le = readBytes(in, 16);  // little-endian from wire
        byte[] be = new byte[16];       // convert to big-endian

        // Reverse LE → BE
        for (int i = 0; i < 16; i++) {
            be[i] = le[15 - i];
        }
        return new BigInteger(1, be);
    }

    private static Ulid readUlid(InputStream in) throws IOException {
        byte[] raw = readBytes(in, 16);

        long msb = ((raw[0x5] & 0xffL) << 56) | ((raw[0x4] & 0xffL) << 48) |
                ((raw[0x3] & 0xffL) << 40) | ((raw[0x2] & 0xffL) << 32) |
                ((raw[0x1] & 0xffL) << 24) | ((raw[0x0] & 0xffL) << 16) |
                ((raw[0xf] & 0xffL) << 8)  | (raw[0xe] & 0xffL);

        long lsb = ((raw[0xd] & 0xffL) << 56) | ((raw[0xc] & 0xffL) << 48) |
                ((raw[0xb] & 0xffL) << 40) | ((raw[0xa] & 0xffL) << 32) |
                ((raw[0x9] & 0xffL) << 24) | ((raw[0x8] & 0xffL) << 16) |
                ((raw[0x7] & 0xffL) << 8)  | (raw[0x6] & 0xffL);

        return new Ulid(msb, lsb);
    }
}
