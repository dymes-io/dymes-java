// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.ops;

import java.io.DataInputStream;
import java.io.DataOutputStream;

/**
 * Response to a VSR operation.
 */
public interface VsrOpResponse {

    /**
     * Queries the {@link VsrOpType VSR operation type} of the operation being responded to.
     *
     * @return {@link VsrOpType VSR operation type} of the operation being responded to.
     */
    VsrOpType opType();

    /**
     * Marshals the response to the data output stream.
     *
     * @param dataOutputStream Data output stream
     */
    void marshal(final DataOutputStream dataOutputStream);

    /**
     * Unmarshalls the response from the data input stream.
     *
     * @param dataInputStream Data input stream
     */
    static VsrOpResponse unmarshal(final DataInputStream dataInputStream) {
        // Read the op type
        VsrOpType vsrOpType = VsrOpType.unmarshal(dataInputStream);

        // Check enum value and delegate to appropriate outer unmarshal method
        return switch (vsrOpType) {
            case append -> VsrAppendResponse.unmarshalOuter(dataInputStream);
            default -> throw new IllegalArgumentException("Unknown VSR op response type: " + vsrOpType);
        };
    }
}
