// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;

import io.dymes.client.vsr.ops.VsrOpResponse;

import java.io.DataInputStream;

/**
 * `Reply` message from primary to client after successful preparation and execution of operation.
 * <p>
 * Triggered (in primary) by a quorum of `VsrPrepareOK` messages (from 𝑓+1 replicas including itself).
 */
public interface VsrReply extends VsrMessage {

    /**
     * Current view number (u64 {@code view-num}).
     *
     * @return u64 {@code view-num}
     */
    long viewNumber();

    /**
     * Request number (u64 {@code request-num}).
     *
     * @return u64 {@code request-num}
     */
    long requestNumber();

    /**
     * Client identifier (u64 {@code client-id}).
     *
     * @return u64 {@code client-id}
     */
    long clientId();

    /**
     * Operation result ({@code response}).
     *
     * @return {@link VsrOpResponse {@code response}}.
     */
    VsrOpResponse response();

    @Override
    default VsrMessageType messageType() {
        return VsrMessageType.vsr_reply;
    }

    static VsrReply unmarshalOuter(final DataInputStream dataInputStream) {
        return DymesVsrReply.unmarshalOuter(dataInputStream);
    }

    static VsrReplyBuilder builder() {
        return DymesVsrReplyBuilder.of();
    }

    static VsrReply of(final long viewNumber,
                       final long requestNumber,
                       final long clientId,
                       final VsrOpResponse response) {
        return DymesVsrReply.of(viewNumber, requestNumber, clientId, response);
    }
}
