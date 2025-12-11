// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr.ops;

import io.dymes.client.msg.DymesMessageReader;
import io.dymes.client.msg.DymesMessageWriter;
import io.dymes.client.vsr.MarshalFailure;
import io.dymes.model.DymesMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

public record DymesVsrAppendOp(DymesMessage message) implements VsrAppendOp {

    public static DymesVsrAppendOp of(final DymesMessage message) {
        requireNonNull(message, "Dymes message required for append operation");
        return new DymesVsrAppendOp(message);
    }

    @Override
    public void marshal(final DataOutputStream out) {
        try {
            // Write op type name as string
            opType().marshal(out);

            // Serialize message to bytes
            DymesMessageWriter.write(out, message());
        } catch (IOException e) {
            throw new MarshalFailure("Failed to marshal VSR append operation", e);
        }
    }

    static VsrAppendOp unmarshalOuter(final DataInputStream dataInputStream) {
        DymesMessage dymesMessage = DymesMessageReader.readOne(dataInputStream);
        return new DymesVsrAppendOp(dymesMessage);
    }
}
