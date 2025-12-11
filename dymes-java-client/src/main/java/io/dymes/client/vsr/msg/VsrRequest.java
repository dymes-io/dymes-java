// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;

import io.dymes.client.vsr.ops.VsrOp;

import java.io.DataInputStream;

/**
 * VSR client request message.
 *
 * @implNote This is sent to the primary replica, which triggers a `VsrPrepare`
 */
public interface VsrRequest extends VsrMessage {

    /**
     * Client identifier (u64 {@code client-id}).
     *
     * @return u64 {@code client-id}
     */
    long clientId();

    /**
     * Request number (u64 {@code request-num}).
     *
     * @return u64 {@code request-num}
     */
    long requestNumber();

    /**
     * Operation to perform {@link VsrOp op}.
     *
     * @return {@link VsrOp {@code op}}.
     */
    VsrOp op();

    @Override
    default VsrMessageType messageType() {
        return VsrMessageType.vsr_request;
    }

    static VsrRequest unmarshalOuter(final DataInputStream dataInputStream) {
        return DymesVsrRequest.unmarshalOuter(dataInputStream);
    }

    static VsrRequestBuilder builder() {
        return DymesVsrRequestBuilder.of();
    }
}
