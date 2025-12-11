// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;

import io.dymes.client.vsr.UnmarshalFailure;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Dymes VSR message types.
 */
public enum VsrMessageType {
    vsr_request(0x0),
    vsr_prepare(0x1),
    vsr_prepare_ok(0x2),
    vsr_reply(0x3),
    vsr_commit(0x4),
    vsr_svc(0x5),
    vsr_dvc(0x6),
    vsr_sv(0x7);

    private final int wireCode;

    VsrMessageType(int wireCode) {
        this.wireCode = wireCode;
    }

    /**
     * Queries the message type wire code.
     *
     * @return u32 wire code
     */
    public int wireCode() {
        return this.wireCode;
    }

    /**
     * Marshals the operation to the data output stream.
     *
     * @param dataOutputStream Data output stream
     */
    void marshal(final DataOutputStream dataOutputStream) {
        try {
            dataOutputStream.writeInt(Integer.reverseBytes(this.wireCode));
        } catch (IOException e) {
            throw new UnmarshalFailure("Failed to write VSR message type", e);
        }
    }

    /**
     * Unmarshals the operation from the data input stream.
     *
     * @param dataInputStream Data input stream
     */
    static VsrMessageType unmarshal(final DataInputStream dataInputStream) {
        try {
            // Read 8-byte wire code (little-endian)
            byte[] wireBytes = new byte[Integer.BYTES];
            dataInputStream.readFully(wireBytes);
            int wireCode = ByteBuffer.wrap(wireBytes)
                                      .order(ByteOrder.LITTLE_ENDIAN)
                                      .getInt();
            if (wireCode < VsrMessageType.vsr_request.wireCode || wireCode > VsrMessageType.vsr_sv.wireCode) {
                throw new UnsupportedOperationException("Unsupported message type [" + wireCode + "]");
            }

            return VsrMessageType.values()[wireCode];
        } catch (IOException e) {
            throw new UnmarshalFailure("Failed to read VSR message type", e);
        }
    }

}
