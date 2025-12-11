// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.msg;

import com.github.f4b6a3.ulid.Ulid;
import io.dymes.model.DymesMessage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Writes Dymes messages to a data output stream in the Dymes wire format.
 * <p>
 * This class handles marshalling of {@link DymesMessage} objects into a binary format
 * consisting of:
 * <ul>
 *   <li>128-byte frame header containing message metadata and checksums</li>
 *   <li>Key-value header control block (16 bytes per header)</li>
 *   <li>Key-value header data (variable length)</li>
 *   <li>Message payload (variable length)</li>
 *   <li>Padding to align frame to 64-byte boundary</li>
 * </ul>
 * <p>
 * All multibyte numeric values are written in little-endian byte order. The frame size is
 * automatically calculated and padded to maintain 64-byte alignment.
 *
 * @see DymesMessage
 */
public class DymesMessageWriter {

    public static void write(DataOutput out, DymesMessage message) throws IOException {
        Map<String, String> headers = message.headers();
        int headerCount = headers.size();

        // Write header control block and data
        ByteArrayOutputStream headerDataStream = new ByteArrayOutputStream(); // key+value bytes
        ByteArrayOutputStream headerControlStream = new ByteArrayOutputStream(); // offsets + lengths
        DataOutputStream headerData = new DataOutputStream(headerDataStream);
        DataOutputStream headerControlInfo = new DataOutputStream(headerControlStream);

        int headerDataOffset = 128; // Start offset after frame header (which is 128 bytes)
        int currentOffset = headerDataOffset + (headerCount * 16); // Each header is 16 bytes in control block

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            byte[] keyBytes = entry.getKey().getBytes(UTF_8);
            byte[] valueBytes = entry.getValue().getBytes(UTF_8);

            // Write control block
            headerControlInfo.writeInt(Integer.reverseBytes(currentOffset)); // key offset
            headerControlInfo.writeInt(Integer.reverseBytes(keyBytes.length)); // key length
            currentOffset += keyBytes.length;
            headerControlInfo.writeInt(Integer.reverseBytes(currentOffset)); // value offset
            headerControlInfo.writeInt(Integer.reverseBytes(valueBytes.length)); // value length
            currentOffset += valueBytes.length;

            headerData.write(keyBytes);
            headerData.write(valueBytes);
        }

        byte[] kvControlBytes = headerControlStream.toByteArray();
        byte[] kvDataBytes = headerDataStream.toByteArray();
        byte[] payloadBytes = message.payload().getBytes(UTF_8);

        int rawFrameSize = 128 + kvControlBytes.length + kvDataBytes.length + payloadBytes.length;
        int frameSize = ((rawFrameSize + 63) / 64) * 64; // Round up to 64-byte boundary

        // Write frame header (128 bytes)
        writeU128(out, BigInteger.ZERO); // Header checksum (placeholder)
        out.writeLong(Long.reverseBytes(0L)); // Log sequence number
        out.writeLong(Long.reverseBytes(0L)); // Reserved
        writeUlid(out, message.id());
        writeUlid(out, message.correlationId());
        writeU128(out, message.channel());
        writeU128(out, message.routing());
        out.writeInt(Integer.reverseBytes(frameSize)); // Frame size
        out.writeInt(Integer.reverseBytes(headerCount)); // KV header count
        out.writeInt(Integer.reverseBytes(kvDataBytes.length)); // KV header data size
        out.writeInt(Integer.reverseBytes(payloadBytes.length)); // Content data size
        writeU128(out, BigInteger.ZERO); // Payload checksum (placeholder)

        out.write(kvControlBytes);
        out.write(kvDataBytes);
        out.write(payloadBytes);

        int padding = frameSize - rawFrameSize;
        out.write(new byte[padding]);
    }

    private static void writeU128(DataOutput out, BigInteger value) throws IOException {
        byte[] raw = value.toByteArray();  // big-endian
        byte[] target = new byte[16];          // little-endian target buffer

        // Fill LE target array from the end of raw[] (big-endian)
        int len = Math.min(raw.length, 16);

        for (int i = 0; i < len; i++) {
            target[i] = raw[raw.length - 1 - i];
        }
        out.write(target);
    }


    private static void writeUlid(DataOutput out, Ulid ulid) throws IOException {
        byte[] raw = new byte[16];

        long msb = ulid.getMostSignificantBits();
        long lsb = ulid.getLeastSignificantBits();

        raw[0x5] = (byte) (msb >>> 56);
        raw[0x4] = (byte) (msb >>> 48);
        raw[0x3] = (byte) (msb >>> 40);
        raw[0x2] = (byte) (msb >>> 32);
        raw[0x1] = (byte) (msb >>> 24);
        raw[0x0] = (byte) (msb >>> 16);
        raw[0xf] = (byte) (msb >>> 8);
        raw[0xe] = (byte) (msb);

        raw[0xd] = (byte) (lsb >>> 56);
        raw[0xc] = (byte) (lsb >>> 48);
        raw[0xb] = (byte) (lsb >>> 40);
        raw[0xa] = (byte) (lsb >>> 32);
        raw[0x9] = (byte) (lsb >>> 24);
        raw[0x8] = (byte) (lsb >>> 16);
        raw[0x7] = (byte) (lsb >>> 8);
        raw[0x6] = (byte) (lsb);

        out.write(raw);
    }
}
