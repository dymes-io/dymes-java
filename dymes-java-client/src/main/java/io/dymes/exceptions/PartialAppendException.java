// SPDX-FileCopyrightText: Copyright © 2026 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.exceptions;

import com.github.f4b6a3.ulid.Ulid;

import java.util.List;

/**
 * A batch append failed after some of its messages were stored. They stay (the store is append-only):
 * {@link #appended()} lists them, in order, so the caller can repair.
 */
public class PartialAppendException extends InfrastructureException {

    private final List<Ulid> appended;

    public PartialAppendException(String message, List<Ulid> appended, Throwable cause) {
        super(message, cause);
        this.appended = List.copyOf(appended);
    }

    public PartialAppendException(String message, List<Ulid> appended) {
        super(message);
        this.appended = List.copyOf(appended);
    }

    /**
     * @return the messages stored before the failure, in the order added
     */
    public List<Ulid> appended() {
        return appended;
    }
}
