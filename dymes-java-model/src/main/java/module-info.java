// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

module dymes.model {
    exports io.dymes.model;

    opens io.dymes.model;

    requires transitive com.github.f4b6a3.ulid;
    requires transitive jakarta.annotation;
}
