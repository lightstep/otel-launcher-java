package com.lightstep.opentelemetry.launcher;

import com.lightstep.opentelemetry.common.VariablesConverter;
import com.lightstep.opentelemetry.common.VariablesConverter.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.propagation.HttpTraceContext;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.extension.trace.propagation.AwsXRayPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.extension.trace.propagation.JaegerPropagator;
import io.opentelemetry.extension.trace.propagation.OtTracerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.util.HashMap;
import java.util.Map;

public class OpenTelemetryConfiguration {

  public static class Builder {
    private String accessToken;
    private String serviceName;
    private String serviceVersion;
    private String spanEndpoint;
    private String resourceAttributes;
    private boolean insecureTransport;
    private Propagator propagator;

    private static final Map<Propagator, TextMapPropagator> PROPAGATORS =
        new HashMap<Propagator, TextMapPropagator>() {
          {
            put(Propagator.TRACE_CONTEXT, HttpTraceContext.getInstance());
            put(Propagator.B3, B3Propagator.builder().injectMultipleHeaders().build());
            put(Propagator.B3_MULTI, B3Propagator.getInstance());
            put(Propagator.JAEGER, JaegerPropagator.getInstance());
            put(Propagator.OT_TRACER, OtTracerPropagator.getInstance());
            put(Propagator.XRAY, AwsXRayPropagator.getInstance());
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
      this.propagator = propagator;
      return this;
    }

    public Builder useInsecureTransport(boolean insecureTransport) {
      this.insecureTransport = insecureTransport;
      return this;
    }

    /**
     * Constructs a new instance of the exporter based on the builder's values.
     *
     * @return a new exporter's instance
     */
    public OtlpGrpcSpanExporter buildExporter() {
      VariablesConverter
          .setSystemProperties(new Configuration()
              .withSpanEndpoint(spanEndpoint)
              .withInsecureTransport(insecureTransport)
              .withAccessToken(accessToken)
              .withServiceName(serviceName)
              .withServiceVersion(serviceVersion)
              .withResourceAttributes(resourceAttributes), false);

      if (propagator != null) {
        final TextMapPropagator textMapPropagator = PROPAGATORS.get(propagator);
        OpenTelemetry.setGlobalPropagators(
            DefaultContextPropagators.builder().addTextMapPropagator(textMapPropagator).build());
      }

      return OtlpGrpcSpanExporter.builder()
          .readSystemProperties()
          .readEnvironmentVariables()
          .build();
    }

    /**
     * Installs exporter into tracer SDK default provider with batching span processor.
     */
    public void install() {
      BatchSpanProcessor spansProcessor = BatchSpanProcessor.builder(this.buildExporter())
          .build();
      OpenTelemetrySdk.getGlobalTracerManagement().addSpanProcessor(spansProcessor);
    }

    private void readEnvVariablesAndSystemProperties() {
      this.accessToken = VariablesConverter.getAccessToken();
      this.serviceName = VariablesConverter.getServiceName();
      this.serviceVersion = VariablesConverter.getServiceVersion();
      this.insecureTransport = VariablesConverter.useInsecureTransport();
      this.spanEndpoint = VariablesConverter.getSpanEndpoint();
      this.propagator = Propagator.valueOfLabel(VariablesConverter.getPropagator());
      this.resourceAttributes = VariablesConverter.getResourceAttributes();
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
