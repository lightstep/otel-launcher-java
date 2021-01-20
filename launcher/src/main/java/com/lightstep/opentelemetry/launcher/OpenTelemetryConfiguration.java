package com.lightstep.opentelemetry.launcher;

import com.lightstep.opentelemetry.common.VariablesConverter;
import com.lightstep.opentelemetry.common.VariablesConverter.Configuration;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class OpenTelemetryConfiguration {
  private static final Logger logger = Logger.getLogger(OpenTelemetryConfiguration.class.getName());

  public static class Builder {
    private String accessToken;
    private String serviceName;
    private String serviceVersion;
    private String spanEndpoint;
    private String resourceAttributes;
    private boolean insecureTransport;

    private final List<Propagator> propagators = new ArrayList<>();

    private static final Map<Propagator, TextMapPropagator> PROPAGATORS =
        new HashMap<Propagator, TextMapPropagator>() {
          {
            put(Propagator.TRACE_CONTEXT, W3CTraceContextPropagator.getInstance());
            put(Propagator.B3, B3Propagator.getInstance());
            put(Propagator.B3_MULTI, B3Propagator.builder().injectMultipleHeaders().build());
            put(Propagator.BAGGAGE, W3CBaggagePropagator.getInstance());
          }
        };

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
     * Sets the satellite url
     *
     * @param spanEndpoint satellite url
     * @return this builder's instance
     */
    public Builder setSpanEndpoint(String spanEndpoint) {
      this.spanEndpoint = spanEndpoint;
      return this;
    }

    public Builder setPropagator(Propagator propagator) {
      this.propagators.add(propagator);
      return this;
    }

    public Builder useInsecureTransport(boolean insecureTransport) {
      this.insecureTransport = insecureTransport;
      return this;
    }

    /**
     * Constructs a new instance of the OpenTelemetry based on the builder's values.
     *
     * @return a new exporter's instance
     */
    public OpenTelemetry buildOpenTelemetry() {
      VariablesConverter
          .setSystemProperties(new Configuration()
              .withSpanEndpoint(spanEndpoint)
              .withInsecureTransport(insecureTransport)
              .withAccessToken(accessToken)
              .withServiceName(serviceName)
              .withServiceVersion(serviceVersion)
              .withResourceAttributes(resourceAttributes), false);

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

      List<TextMapPropagator> textMapPropagators = new ArrayList<>();
      for (Propagator propagator : propagators) {
        textMapPropagators.add(PROPAGATORS.get(propagator));
      }

      final OtlpGrpcSpanExporter otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder()
          .readSystemProperties()
          .readEnvironmentVariables()
          .build();

      SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
          .addSpanProcessor(
              BatchSpanProcessor.builder(otlpGrpcSpanExporter).build())
          .build();

      return OpenTelemetrySdk.builder()
          .setTracerProvider(sdkTracerProvider)
          .setPropagators(
              ContextPropagators.create(TextMapPropagator.composite(textMapPropagators)))
          .build();
    }

    /**
     * Installs exporter into tracer SDK default provider with batching span processor.
     */
    public void install() {
      GlobalOpenTelemetry.set(buildOpenTelemetry());
    }

    private void readEnvVariablesAndSystemProperties() {
      this.accessToken = VariablesConverter.getAccessToken();
      this.serviceName = VariablesConverter.getServiceName();
      this.serviceVersion = VariablesConverter.getServiceVersion();
      this.insecureTransport = VariablesConverter.useInsecureTransport();
      this.spanEndpoint = VariablesConverter.getSpanEndpoint();
      this.resourceAttributes = VariablesConverter.getResourceAttributes();
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
