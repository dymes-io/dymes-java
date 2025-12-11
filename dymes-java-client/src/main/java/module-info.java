// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

/**
 * DYMES Java Client
 */
module dymes.client {
    exports io.dymes.client;
    exports io.dymes.client.internal;
    exports io.dymes.client.append;
    exports io.dymes.client.dto;
    exports io.dymes.client.msg;
    exports io.dymes.client.query;

    requires transitive dymes.model;

    requires io.helidon.http.media.jackson;
    requires io.helidon.webclient.api;
    requires io.helidon.webclient.tracing;
    requires io.helidon.logging.common;

    requires org.slf4j;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.module.paramnames;
    requires org.javamoney.moneta;
    requires org.threeten.extra;
    requires jackson.datatype.money;
}
