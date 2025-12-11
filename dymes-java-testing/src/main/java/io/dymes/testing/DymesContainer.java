// SPDX-FileCopyrightText: Copyright © 2025 The Dymes project authors
//
// SPDX-License-Identifier: Apache-2.0

package io.dymes.testing;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Allows Dymes container to be launched and controlled.
 */
public class DymesContainer extends GenericContainer<DymesContainer> {

    private static final Logger logger = LoggerFactory.getLogger(DymesContainer.class);

    private static final int DYMES_LISTEN_PORT = 6510;

    private static final String DOCKER_REGISTRY_ENV_VAR = "DOCKER_REGISTRY";
    private static final String DEFAULT_DYMES_REGISTRY = "";
    private static final String DEFAULT_DYMES_IMAGE_NAME = "dymes";
    private static final String DEFAULT_DYMES_TAG = "latest";
    private static final String DYMES_TAG_ENV_VAR = "DYMES_TAG";

    /**
     * Creates a Dymes container using defaults.
     *
     * @return {@link DymesContainer} Dymes container
     */
    public static DymesContainer of() {
        return of(DEFAULT_DYMES_IMAGE_NAME);
    }

    /**
     * Creates a Dymes container using a container image name.
     *
     * @param baseImageName Container image base name
     * @return {@link DymesContainer} Dymes container
     */
    public static DymesContainer of(String baseImageName) {
        return of(baseImageName, dymesTag(), defaultRegistry());
    }

    /**
     * Creates a Dymes container using a container image and docker registry name.
     *
     * @param baseImageName  Container image base name
     * @param dockerRegistry Docker registry name
     * @return {@link DymesContainer} Dymes container
     */
    public static DymesContainer of(String baseImageName, String dockerRegistry) {
        return of(baseImageName, dymesTag(), dockerRegistry);
    }

    /**
     * Creates a Dymes container using a container image name, tag, and docker registry name.
     *
     * @param baseImageName  Container image base name
     * @param tag            Image tag
     * @param dockerRegistry Docker registry name
     * @return {@link DymesContainer} Dymes container
     */
    public static DymesContainer of(String baseImageName, String tag, String dockerRegistry) {
        return new DymesContainer(dockerImageName(dockerRegistry, baseImageName, tag));
    }

    @SuppressWarnings("resource")
    private DymesContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        withExposedPorts(DYMES_LISTEN_PORT);
        waitingFor(Wait.forListeningPort());
        withStartupTimeout(Duration.ofSeconds(10));
        logger.debug("Dymes test container configured with image: {}", dockerImageName);
    }

    /**
     * Queries Dymes client configuration appropriate for connecting to this Dymes container.
     *
     * @return Dymes Client {@link Config Configuration}
     */
    public Config clientConfig() {
        final var port = Objects.requireNonNull(getMappedPort(DYMES_LISTEN_PORT),
                                                "Dymes listen port has not been bound");

        final var configSources = ConfigSources
                .create(Map.of("http-uri", "http://localhost:%d".formatted(port))).build();
        return Config.builder(configSources).build();
    }

    private static DockerImageName dockerImageName(final String dockerRegistry,
                                                   final String baseImageName,
                                                   final String tag) {
        return DockerImageName.parse("%s%s%s:%s".formatted(dockerRegistry,
                                                           dockerRegistry.isEmpty() ? "" : "/dymes-io/",
                                                           baseImageName,
                                                           tag));
    }

    private static String defaultRegistry() {
        return Objects.requireNonNullElse(System.getenv(DOCKER_REGISTRY_ENV_VAR), DEFAULT_DYMES_REGISTRY);
    }

    private static String dymesTag() {
        return Objects.requireNonNullElse(System.getenv(DYMES_TAG_ENV_VAR), DEFAULT_DYMES_TAG);
    }
}
