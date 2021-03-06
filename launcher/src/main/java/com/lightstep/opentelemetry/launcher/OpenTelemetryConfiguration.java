package com.lightstep.opentelemetry.launcher;

import com.lightstep.opentelemetry.common.VariablesConverter;
import com.lightstep.opentelemetry.common.VariablesConverter.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class OpenTelemetryConfiguration {
  private static final Logger logger = Logger.getLogger(OpenTelemetryConfiguration.class.getName());

  public static class Builder {
    private String accessToken;
    private String serviceName;
    private String serviceVersion;
    private String tracesEndpoint;
    private String resourceAttributes;
    @Deprecated
    private boolean insecureTransport;
    private final List<Propagator> propagators = new ArrayList<>();


    private Builder() {
      readEnvVariablesAndSystemProperties();
    }

    /**
     * Sets the token for Lightstep access
     *
     * @param accessToken Your specific token for Lightstep access.
     * @return this builder's instance
     */
    public Builder setAccessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    public Builder setServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Builder setServiceVersion(String serviceVersion) {
      this.serviceVersion = serviceVersion;
      return this;
    }

    /**
     * Sets the satellite url for traces
     *
     * @param tracesEndpoint satellite url
     * @return this builder's instance
     */
    public Builder setTracesEndpoint(String tracesEndpoint) {
      this.tracesEndpoint = tracesEndpoint;
      return this;
    }

    public Builder setPropagator(Propagator propagator) {
      this.propagators.add(propagator);
      return this;
    }

    /**
     * Deprecated, instead set endpoint starting with http:// or https://
     */
    @Deprecated
    public Builder useInsecureTransport(boolean insecureTransport) {
      this.insecureTransport = insecureTransport;
      return this;
    }

    /**
     * Constructs a new instance of the OpenTelemetry based on the builder's values.
     *
     * @return a new OpenTelemetry instance
     */
    public OpenTelemetry buildOpenTelemetry() {
      if (propagators.isEmpty()) {
        String propagatorFromEnv = VariablesConverter.getPropagator();
        String[] propagatorsArray = propagatorFromEnv.split("\\s*,\\s*");
        for (String propagatorLabel : propagatorsArray) {
          Propagator propagator = Propagator.valueOfLabel(propagatorLabel);
          if (propagator == null) {
            logger.warning("Unsupported propagator " + propagatorLabel);
          } else {
            propagators.add(propagator);
          }
        }
      }

      VariablesConverter
          .setSystemProperties(new Configuration()
              .withTracesEndpoint(tracesEndpoint)
              .withInsecureTransport(insecureTransport)
              .withAccessToken(accessToken)
              .withServiceName(serviceName)
              .withServiceVersion(serviceVersion)
              .withPropagators(
                  propagators.stream().map(Propagator::label).collect(Collectors.joining(",")))
              .withResourceAttributes(resourceAttributes), false);

      return OpenTelemetrySdkAutoConfiguration.initialize();
    }

    /**
     * Installs exporter into tracer SDK default provider with batching span processor.
     */
    public void install() {
      buildOpenTelemetry();
    }

    private void readEnvVariablesAndSystemProperties() {
      this.accessToken = VariablesConverter.getAccessToken();
      this.serviceName = VariablesConverter.getServiceName();
      this.serviceVersion = VariablesConverter.getServiceVersion();
      this.tracesEndpoint = VariablesConverter.getTracesEndpoint();
      this.resourceAttributes = VariablesConverter.getResourceAttributes();
      this.insecureTransport = VariablesConverter.useInsecureTransport();
    }

    List<Propagator> getPropagators() {
      return propagators;
    }

  }

  /**
   * Creates a new builder instance.
   *
   * @return a new instance builder for this exporter
   */
  public static Builder newBuilder() {
    return new Builder();
  }

}
