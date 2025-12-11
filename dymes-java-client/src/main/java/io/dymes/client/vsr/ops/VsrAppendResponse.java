// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.ops;

import com.github.f4b6a3.ulid.Ulid;

import java.io.DataInputStream;

/**
 * VSR response to a client {@link VsrAppendOp append operation}.
 * <p>
 * Dymes-specific: The `append-rsp` (once applied) represents the response to a client's append request.
 */
public interface VsrAppendResponse extends VsrOpResponse {

    Ulid messageId();

    @Override
    default VsrOpType opType() {
        return VsrOpType.append;
    }

    static VsrAppendResponse unmarshalOuter(final DataInputStream dataInputStream) {
        return DymesVsrAppendResponse.unmarshalOuter(dataInputStream);
    }

    static VsrAppendResponse of(final Ulid messageId) {
        return DymesVsrAppendResponse.of(messageId);
    }
}

