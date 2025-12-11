// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.model;

import com.github.f4b6a3.ulid.Ulid;

import java.math.BigInteger;
import java.time.Instant;
import java.util.Map;

/**
 * Dymes Message.
 */
public interface Message extends Comparable<Message> {

    /**
     * Millisecond precision commit timestamp.
     *
     * @return {@link Instant Timestamp}
     */
    Instant timestamp();

    /**
     * Message {@link Ulid identifier}.
     *
     * @return Message identifier.
     */
    Ulid id();

    /**
     * Message {@link Ulid correlation identifier}.
     *
     * @return Message correlation identifier.
     */
    Ulid correlationId();

    /**
     * Channel identifier ({@code 128-bit unsigned}).
     *
     * @return Channel identifier
     */
    BigInteger channel();

    /**
     * Routing data ({@code 128-bit unsigned}).
     *
     * @return Routing data
     */
    BigInteger routing();

    /**
     * KV header map.
     * <p>
     * Keys and values are UTF-8 strings, and neither key nor value may be null.
     *
     * @return KV header map
     */
    Map<String, String> headers();

    /**
     * Message payload.
     * <p>
     * No marshalling/unmarshalling or parsing is performed on the payload.
     *
     * @return Message payload
     */
    String payload();
}
