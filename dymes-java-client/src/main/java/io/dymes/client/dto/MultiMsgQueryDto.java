// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.github.f4b6a3.ulid.Ulid;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@JsonSerialize
public record MultiMsgQueryDto(
        @JsonProperty("msg_ids") List<String> msgIds
) {

    public static MultiMsgQueryDto of(final Stream<Ulid> msgIds) {
        final var ids = requireNonNull(msgIds, "Query message ids  required")
                .filter(Objects::nonNull)
                .map(Ulid::toString)
                .toList();
        return new MultiMsgQueryDto(ids);
    }

}
