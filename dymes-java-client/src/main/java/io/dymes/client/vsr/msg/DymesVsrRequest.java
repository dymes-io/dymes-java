// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;

import io.dymes.client.vsr.MarshalFailure;
import io.dymes.client.vsr.UnmarshalFailure;
import io.dymes.client.vsr.ops.VsrOp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Dymes variation of VsrRequest.
 *
 * @param clientId      Client identifier (u64)
 * @param requestNumber Request number (u64)
 * @param op            Operation to perform
 */
public record DymesVsrRequest(
        long clientId,
        long requestNumber,
        VsrOp op
) implements VsrRequest {
    public static DymesVsrRequest of(final long clientId, final long requestNumber, final VsrOp op) {
        requireNonNull(op, "VSR operation required");
        return new DymesVsrRequest(clientId, requestNumber, op);
    }

    @Override
    public void marshal(final DataOutputStream dataOutputStream) {
        messageType().marshal(dataOutputStream);

        try {
            dataOutputStream.writeLong(Long.reverseBytes(clientId()));
            dataOutputStream.writeLong(Long.reverseBytes(requestNumber()));
        } catch (IOException e) {
            throw new MarshalFailure("Failed to marshal VSR request. Couldn't write buffer to stream", e);
        }

        op().marshal(dataOutputStream);
    }

    public static VsrRequest unmarshalOuter(final DataInputStream dataInputStream) {
        try {
            final long clientId = Long.reverseBytes(dataInputStream.readLong());
            final long requestNo = Long.reverseBytes(dataInputStream.readLong());
            VsrOp op = VsrOp.unmarshal(dataInputStream);
            return DymesVsrRequest.of(clientId, requestNo, op);
        } catch (IOException e) {
            throw new UnmarshalFailure("Failed to unmarshal VSR request. Couldn't read buffer from stream", e);
        }
    }

}
