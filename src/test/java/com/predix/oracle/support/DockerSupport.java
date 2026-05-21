package com.predix.oracle.support;

import org.testcontainers.DockerClientFactory;

public final class DockerSupport {

    private DockerSupport() {}

    public static boolean isAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Exception e) {
            return false;
        }
    }
}
