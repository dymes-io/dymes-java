// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;

import io.dymes.client.vsr.ops.VsrOpResponse;

import static java.util.Objects.requireNonNull;

/**
 * Builder for constructing {@link VsrReply} messages.
 * <p>
 * Validates required fields (view number, request number, client ID, and VSR operation response)
 * before building a {@link DymesVsrReply} instance.
 *
 * @see VsrReply
 * @see DymesVsrReply
 */
public class DymesVsrReplyBuilder implements VsrReplyBuilder {

    public static DymesVsrReplyBuilder of() {
        return new DymesVsrReplyBuilder();
    }

    Long viewNumber;
    Long requestNumber;
    Long clientId;
    VsrOpResponse opResponse;


    @Override
    public DymesVsrReplyBuilder withViewNumber(final long viewNumber) {
        if (viewNumber < 0L) {
            throw new IllegalArgumentException("View number must be positive");
        }
        this.viewNumber = viewNumber;
        return this;
    }

    @Override
    public DymesVsrReplyBuilder withRequestNumber(final long requestNumber) {
        this.requestNumber = requestNumber;
        return this;
    }

    @Override
    public DymesVsrReplyBuilder withClientId(final long clientId) {
        this.clientId = clientId;
        return this;
    }

    @Override
    public DymesVsrReplyBuilder withOpResponse(final VsrOpResponse opResponse) {
        this.opResponse = requireNonNull(opResponse);
        return this;
    }

    @Override
    public DymesVsrReply build() {
        requireNonNull(this.viewNumber, "View number required");
        requireNonNull(this.requestNumber, "Request number required");
        requireNonNull(this.clientId, "Client identifier required");
        requireNonNull(this.opResponse, "Operation response required");

        return DymesVsrReply.of(this.viewNumber, this.requestNumber, this.clientId, this.opResponse);
    }
}
