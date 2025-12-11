// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;

import io.dymes.client.vsr.ops.VsrOp;

import static java.util.Objects.requireNonNull;

/**
 * Builder for constructing {@link VsrRequest} messages.
 * <p>
 * Validates required fields (client ID, request number, and VSR operation)
 * before building a {@link DymesVsrRequest} instance.
 *
 * @see VsrRequest
 * @see DymesVsrRequest
 * @see VsrOp
 */
public class DymesVsrRequestBuilder implements VsrRequestBuilder {

    public static DymesVsrRequestBuilder of() {
        return new DymesVsrRequestBuilder();
    }

    Long clientId;
    Long requestNumber;
    VsrOp op;


    @Override
    public VsrRequestBuilder withClientId(final long clientId) {
        if (clientId <= 0L) {
            throw new IllegalArgumentException("Client identifier must be positive");
        }
        this.clientId = clientId;
        return this;
    }

    @Override
    public VsrRequestBuilder withRequestNumber(final long requestNumber) {
        if (requestNumber <= 0L) {
            throw new IllegalArgumentException("Request number must be positive");
        }
        this.requestNumber = requestNumber;
        return this;
    }

    @Override
    public VsrRequestBuilder withOp(final VsrOp op) {
        requireNonNull(op);
        this.op = op;
        return this;
    }

    @Override
    public VsrRequest build() {
        requireNonNull(this.clientId, "Client identifier required");
        requireNonNull(this.requestNumber, "Request number required");
        requireNonNull(this.op, "Operation required");

        return DymesVsrRequest.of(this.clientId, this.requestNumber, this.op);
    }

}
