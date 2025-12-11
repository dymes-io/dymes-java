// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.model;

import com.github.f4b6a3.ulid.Ulid;
import jakarta.annotation.Nonnull;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;

public record DymesMessage(Ulid id,
                           Ulid correlationId,
                           BigInteger channel,
                           BigInteger routing,
                           Map<String, String> headers,
                           String payload) implements Message {
    @Override
    public int compareTo(@Nonnull Message that) {
        return id.compareTo(that.id());
    }

    @Override
    public Instant timestamp() {
        return id.getInstant();
    }
}
