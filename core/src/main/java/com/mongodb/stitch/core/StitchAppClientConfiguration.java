/*
 * Copyright 2018-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.stitch.core;

/**
 * Properties representing the configuration of a client that communicate with a particular MongoDB
 * Stitch application.
 */
public final class StitchAppClientConfiguration extends StitchClientConfiguration {
  private final String clientAppId;
  private final String localAppName;
  private final String localAppVersion;

  private StitchAppClientConfiguration(
      final StitchClientConfiguration config,
      final String clientAppId,
      final String localAppName,
      final String localAppVersion) {
    super(config);
    this.clientAppId = clientAppId;
    this.localAppVersion = localAppVersion;
    this.localAppName = localAppName;
  }

  /**
   * Gets the client app id of the Stitch application that this client is going to communicate with.
   */
  public String getClientAppId() {
    return clientAppId;
  }

  /**
   * Gets the name of the local application.
   */
  public String getLocalAppName() {
    return localAppName;
  }

  /**
   * Gets the current version of the local application.
   */
  public String getLocalAppVersion() {
    return localAppVersion;
  }

  /**
   * Gets the builder form of this configuration.
   */
  public Builder builder() {
    return new Builder(this);
  }

  /**
   * A builder that can build a {@link StitchAppClientConfiguration }object.
   */
  public static class Builder extends StitchClientConfiguration.Builder {
    private String clientAppId;
    private String localAppName;
    private String localAppVersion;

    public Builder() {}

    private Builder(final StitchAppClientConfiguration config) {
      super(config);
      clientAppId = config.clientAppId;
      localAppVersion = config.localAppVersion;
      localAppName = config.localAppName;
    }

    /**
     * Returns a builder for a given client app id.
     */
    public static Builder forApp(final String clientAppId) {
      return new Builder().withClientAppId(clientAppId);
    }

    /**
     * Returns a builder for a given client app id and a Stitch app server base URL.
     */
    public static Builder forApp(final String clientAppId, final String baseUrl) {
      final Builder builder = new Builder();
      builder.withBaseUrl(baseUrl);
      return builder.withClientAppId(clientAppId);
    }

    /**
     * Sets the client app id of the Stitch application that this client is going to communicate
     * with.
     */
    public Builder withClientAppId(final String clientAppId) {
      this.clientAppId = clientAppId;
      return this;
    }

    /** Sets the name of the local application. */
    public Builder withLocalAppName(final String localAppName) {
      this.localAppName = localAppName;
      return this;
    }

    /** Sets the current version of the local application. */
    public Builder withLocalAppVersion(final String localAppVersion) {
      this.localAppVersion = localAppVersion;
      return this;
    }

    /**
     * Gets the client app id of the Stitch application that this client is going to communicate
     * with.
     */
    public String getClientAppId() {
      return clientAppId;
    }

    /**
     * Gets the name of the local application.
     */
    public String getLocalAppName() {
      return localAppName;
    }

    /**
     * Gets the current version of the local application.
     */
    public String getLocalAppVersion() {
      return localAppVersion;
    }

    /**
     * Builds the {@link StitchAppClientConfiguration}.
     */
    public StitchAppClientConfiguration build() {
      if (clientAppId == null || clientAppId.isEmpty()) {
        throw new IllegalArgumentException("clientAppId must be set to a non-empty string");
      }

      final StitchClientConfiguration config = super.build();
      return new StitchAppClientConfiguration(config, clientAppId, localAppName, localAppVersion);
    }
  }
}
