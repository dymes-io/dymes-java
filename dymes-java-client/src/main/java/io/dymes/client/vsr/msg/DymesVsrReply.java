// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;


import io.dymes.client.vsr.MarshalFailure;
import io.dymes.client.vsr.UnmarshalFailure;
import io.dymes.client.vsr.ops.VsrOpResponse;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of VsrReply.
 *
 * @param viewNumber View number (u64)
 * @param requestNumber Request number (u64)
 * @param clientId Client identifier (u64)
 * @param response Response to the VSR operation
 */
public record DymesVsrReply(
    long viewNumber,
    long requestNumber,
    long clientId,
    VsrOpResponse response
) implements VsrReply {
    public static DymesVsrReply of(final long viewNumber, final long requestNumber, final long clientId, final VsrOpResponse response) {
        requireNonNull(response, "VSR operation response required");
        return new DymesVsrReply(viewNumber, requestNumber, clientId, response);
    }

    @Override
    public void marshal(final DataOutputStream dataOutputStream) {
        messageType().marshal(dataOutputStream);
        try {
            dataOutputStream.writeLong(Long.reverseBytes(viewNumber()));
            dataOutputStream.writeLong(Long.reverseBytes(requestNumber()));
            dataOutputStream.writeLong(Long.reverseBytes(clientId()));
        } catch (IOException e) {
            throw new MarshalFailure("Failed to marshal VSR Reply. Couldn't write buffer to stream", e);
        }

        response().marshal(dataOutputStream);
    }

    public static VsrReply unmarshalOuter(final DataInputStream dataInputStream) {

        try {
            final long viewNo = Long.reverseBytes(dataInputStream.readLong());
            final long requestNo = Long.reverseBytes(dataInputStream.readLong());
            final long clientId = Long.reverseBytes(dataInputStream.readLong());
            final VsrOpResponse response = VsrOpResponse.unmarshal(dataInputStream);
            return VsrReply.of(viewNo, requestNo, clientId, response);
        } catch (IOException e) {
            throw new UnmarshalFailure("Failed to unmarshal VSR Reply. Couldn't read buffer from stream", e);
        }
    }

}
