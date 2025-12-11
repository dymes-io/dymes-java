// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.msg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * A VSR message.
 */
public interface VsrMessage {

    /**
     * Queries the {@link VsrMessageType VSR message type}.
     *
     * @return {@link VsrMessageType VSR message type}
     */
    VsrMessageType messageType();

    /**
     * Marshals the message to the data output stream.
     *
     * @param dataOutputStream Data output stream
     */
    void marshal(final DataOutputStream dataOutputStream);

    /**
     * Unmarshals the operation from the data input stream.
     *
     * @param dataInputStream Data input stream
     */
    static VsrMessage unmarshal(final DataInputStream dataInputStream) throws IOException {
        // Read the message type
        VsrMessageType vsrMessageType = VsrMessageType.unmarshal(dataInputStream);

        // Check enum value and delegate to appropriate outer unmarshal method
        return switch (vsrMessageType) {
            case vsr_request -> VsrRequest.unmarshalOuter(dataInputStream);
            case vsr_reply -> VsrReply.unmarshalOuter(dataInputStream);
            default -> throw new IllegalArgumentException("Unknown VSR message type: " + vsrMessageType);
        };
    }
}
