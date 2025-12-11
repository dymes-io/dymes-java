// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.exceptions;

public class IllegalResponseException extends RuntimeException {
    public IllegalResponseException(String message) {
        super(message);
    }

    public IllegalResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
