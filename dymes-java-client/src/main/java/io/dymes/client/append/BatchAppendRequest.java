// SPDX-FileCopyrightText: Copyright © 2026 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.append;

import com.github.f4b6a3.ulid.Ulid;

import java.util.List;

/**
 * A batch append: many messages stored in order and synchronized once by the node
 * ({@code POST /api/v1/messages/batch}). Batches above the node's limits are sent in several requests.
 */
public interface BatchAppendRequest {

    /**
     * Adds a message. A {@link BatchMessage#correlationIndex()} must point at a message added earlier.
     *
     * @param message the message
     * @return this request
     */
    BatchAppendRequest add(BatchMessage message);

    /**
     * Adds messages, in order.
     *
     * @param messages the messages
     * @return this request
     */
    BatchAppendRequest addAll(List<BatchMessage> messages);

    /**
     * @return the messages added so far
     */
    int size();

    /**
     * Appends the messages.
     *
     * @return their ids, in the order added
     * @throws io.dymes.exceptions.UnsupportedRequestException the node has no batch append (an older or clustered
     *                                                         node): append the messages one at a time instead
     * @throws io.dymes.exceptions.PartialAppendException      the batch failed after some messages were stored;
     *                                                         they stay, and the exception lists them
     * @throws io.dymes.exceptions.IllegalRequestException     the node refused the batch; nothing was stored
     */
    List<Ulid> commit();
}
