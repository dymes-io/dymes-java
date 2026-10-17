// SPDX-FileCopyrightText: Copyright © 2026 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The answer to a batch creation request: the ids, in request order; with {@code error}, those stored before a
 * storage failure.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchCreationResponseDto(@JsonProperty("ids") List<String> ids,
                                       @JsonProperty("error") String error) {
}
