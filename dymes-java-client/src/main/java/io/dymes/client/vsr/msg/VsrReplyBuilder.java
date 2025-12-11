// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;

import io.dymes.client.vsr.ops.VsrOpResponse;

public interface VsrReplyBuilder {

    /**
     * Sets the view number (u64 {@code view-num}).
     *
     * @return u64 {@code view-num}
     */
    VsrReplyBuilder withViewNumber(final long clientId);

    /**
     * Sets the request number (u64 {@code request-num}).
     *
     * @return u64 {@code request-num}
     */
    VsrReplyBuilder withRequestNumber(final long requestNumber);

    /**
     * Sets the client identifier (u64 {@code client-id}).
     *
     * @return u64 {@code client-id}
     */
    VsrReplyBuilder withClientId(final long clientId);

    /**
     * Sets the operation to perform {@link VsrOpResponse op-response}.
     *
     * @return {@link VsrOpResponse {@code op-response}}.
     */
    VsrReplyBuilder withOpResponse(final VsrOpResponse opResponse);

    VsrReply build();
}
