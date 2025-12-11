// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.ops;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A VSR operation.
 */
public interface VsrOp {

    /**
     * Queries the {@link VsrOpType VSR operation type} of this operation.
     *
     * @return {@link VsrOpType VSR operation type} of this operation
     */
    VsrOpType opType();

    /**
     * Marshals the operation to the data output stream.
     *
     * @param dataOutputStream Data output stream
     */
    void marshal(final DataOutputStream dataOutputStream);

    /**
     * Unmarshals the operation from the data input stream.
     *
     * @param dataInputStream Data input stream
     * @return Unmarshalled VSR operation
     * @throws IOException if reading from stream fails
     */
    static VsrOp unmarshal(final DataInputStream dataInputStream) throws IOException {
        // Read the op type
        VsrOpType vsrOpType = VsrOpType.unmarshal(dataInputStream);

        // Check enum value and delegate to appropriate outer unmarshal method
        return switch (vsrOpType) {
            case append -> VsrAppendOp.unmarshalOuter(dataInputStream);
        };
    }

}
