// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.vsr;

import io.dymes.exceptions.InfrastructureException;

public class UnmarshalFailure extends InfrastructureException {
    public UnmarshalFailure(final String message) {
        super(message);
    }

    public UnmarshalFailure(final String message, final Throwable cause) {
        super(message, cause);
    }
}
