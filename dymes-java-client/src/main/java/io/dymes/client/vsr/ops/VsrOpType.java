// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.ops;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * VSR operation type.
 * <p>
 * Dymes-specific: Since queries are currently performed via the HTTP API,
 * we only have to deal with `append` operations.
 */
public enum VsrOpType {
    append;

    // Serialize to DataOutputStream
    public void marshal(DataOutputStream out) {
        try {
            byte[] nameBytes = name().getBytes(StandardCharsets.UTF_8);
            out.writeInt(nameBytes.length);
            out.write(nameBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Deserialize from DataInputStream
    public static VsrOpType unmarshal(DataInputStream in) {
        try {
            int nameLength = in.readInt();
            byte[] nameBytes = new byte[nameLength];
            in.readFully(nameBytes);
            String opTypeName = new String(nameBytes, StandardCharsets.UTF_8);
            return VsrOpType.valueOf(opTypeName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

