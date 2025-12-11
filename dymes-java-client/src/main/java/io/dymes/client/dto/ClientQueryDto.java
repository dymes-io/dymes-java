// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClientQueryDto(
        @JsonProperty("query_dto") QueryDto queryDto,
        @JsonProperty("filters") List<FilterDto> filters
) {

    public static ClientQueryDto of(final QueryDto query) {
        return new ClientQueryDto(query, List.of());
    }

    public static ClientQueryDto of(final QueryDto query,
                                    final Stream<FilterDto> filters) {
        requireNonNull(query, "Query required");
        requireNonNull(filters, "Filters required");
        return new ClientQueryDto(query, filters.filter(Objects::nonNull).toList());
    }
}
