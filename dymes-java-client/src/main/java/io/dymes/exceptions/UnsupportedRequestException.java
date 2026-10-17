// SPDX-FileCopyrightText: Copyright © 2026 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.exceptions;

/**
 * The node doesn't support the request: for example a batch append on an older or a clustered node.
 */
public class UnsupportedRequestException extends IllegalRequestException {

    public UnsupportedRequestException(String message) {
        super(message);
    }
}
