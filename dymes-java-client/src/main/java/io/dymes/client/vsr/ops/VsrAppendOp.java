// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.ops;

import io.dymes.model.DymesMessage;

import java.io.DataInputStream;

/**
 * VSR client append operation.
 * <p>
 * Dymes-specific: The `append-op` is represented by a Dymes message frame.
 */
public interface VsrAppendOp extends VsrOp {

    @Override
    default VsrOpType opType() {
        return VsrOpType.append;
    }

    /**
     * Convenience method to render the frame as a Dymes message.
     *
     * @return Dymes message represented by message frame
     */
    DymesMessage message();

    static VsrAppendOp unmarshalOuter(final DataInputStream dataInputStream) {
        return DymesVsrAppendOp.unmarshalOuter(dataInputStream);
    }

    static VsrAppendOp of(final DymesMessage message) {
        return DymesVsrAppendOp.of(message);
    }
}
