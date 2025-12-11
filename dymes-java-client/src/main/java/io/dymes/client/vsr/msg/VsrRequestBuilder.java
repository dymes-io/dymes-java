// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;

import io.dymes.client.vsr.ops.VsrOp;

public interface VsrRequestBuilder {

    /**
     * Sets the client identifier (u64 {@code client-id}).
     *
     * @return u64 {@code client-id}
     */
    VsrRequestBuilder withClientId(final long clientId);

    /**
     * Sets the request number (u64 {@code request-num}).
     *
     * @return u64 {@code request-num}
     */
    VsrRequestBuilder withRequestNumber(final long requestNumber);

    /**
     * Sets the operation to perform {@link VsrOp op}.
     *
     * @return {@link VsrOp {@code op}}.
     */
    VsrRequestBuilder withOp(final VsrOp op);

    VsrRequest build();
}
