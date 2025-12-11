// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.ops;

import com.github.f4b6a3.ulid.Ulid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public record DymesVsrAppendResponse(Ulid messageId) implements VsrAppendResponse {
    public static DymesVsrAppendResponse of(final Ulid messageId) {
        requireNonNull(messageId, "Message identifier required");
        return new DymesVsrAppendResponse(messageId);
    }

    @Override
    public void marshal(final DataOutputStream out) {
        try {
            // Write op type name as string
            byte[] opTypeBytes = opType().name().getBytes(StandardCharsets.UTF_8);
            out.writeInt(opTypeBytes.length);
            out.write(opTypeBytes);

            // Write ULID as 16 bytes, little-endian
            byte[] ulidBytes = messageId().toBytes();
            ByteBuffer buf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
            ByteBuffer source = ByteBuffer.wrap(ulidBytes);
            buf.putLong(source.getLong());  // MSB
            buf.putLong(source.getLong());  // LSB
            out.write(buf.array());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static VsrAppendResponse unmarshalOuter(final DataInputStream in) {
        try {
            byte[] ulidBytes = new byte[16];
            in.readFully(ulidBytes);
            ByteBuffer buf = ByteBuffer.wrap(ulidBytes).order(ByteOrder.LITTLE_ENDIAN);
            long msb = buf.getLong();  // Read first 8 bytes as MSB
            long lsb = buf.getLong();  // Read next 8 bytes as LSB
            UUID uuid = new UUID(msb, lsb);
            Ulid ulid = Ulid.from(uuid);
            return new DymesVsrAppendResponse(ulid);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

