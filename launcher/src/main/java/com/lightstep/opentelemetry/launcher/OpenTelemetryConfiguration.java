package com.lightstep.opentelemetry.launcher;

import com.lightstep.opentelemetry.common.VariablesConverter;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.exporters.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.extensions.trace.propagation.AwsXRayPropagator;
import io.opentelemetry.extensions.trace.propagation.B3Propagator;
import io.opentelemetry.extensions.trace.propagation.JaegerPropagator;
import io.opentelemetry.extensions.trace.propagation.OtTracerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.trace.propagation.HttpTraceContext;
import java.util.HashMap;
import java.util.Map;

public class OpenTelemetryConfiguration {

  public static class Builder {
    private String accessToken;
    private String serviceName;
    private String serviceVersion;
    private String spanEndpoint;
    private String resourceLabels;
    private boolean insecureTransport;
    private Propagator propagator;

    private static final Map<Propagator, HttpTextFormat> PROPAGATORS =
        new HashMap<Propagator, HttpTextFormat>() {
          {
            put(Propagator.TRACE_CONTEXT, new HttpTraceContext());
            put(Propagator.B3, B3Propagator.getMultipleHeaderPropagator());
            put(Propagator.B3_SINGLE, B3Propagator.getSingleHeaderPropagator());
            put(Propagator.JAEGER, new JaegerPropagator());
            put(Propagator.OT_TRACER, OtTracerPropagator.getInstance());
            put(Propagator.XRAY, new AwsXRayPropagator());
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
          .setSystemProperties(spanEndpoint, insecureTransport, accessToken, null, null,
              serviceName, serviceVersion, resourceLabels, false);

      if (propagator != null) {
        final HttpTextFormat httpTextFormat = PROPAGATORS.get(propagator);
        OpenTelemetry.setPropagators(
            DefaultContextPropagators.builder().addHttpTextFormat(httpTextFormat).build());
      }

      return OtlpGrpcSpanExporter.newBuilder()
          .readSystemProperties()
          .readEnvironmentVariables()
          .build();
    }

    /**
     * Installs exporter into tracer SDK default provider with batching span processor.
     */
    public void install() {
      BatchSpanProcessor spansProcessor = BatchSpanProcessor.newBuilder(this.buildExporter())
          .build();
      OpenTelemetrySdk.getTracerProvider().addSpanProcessor(spansProcessor);
    }

    private void readEnvVariablesAndSystemProperties() {
      this.accessToken = VariablesConverter.getAccessToken();
      this.serviceName = VariablesConverter.getServiceName();
      this.serviceVersion = VariablesConverter.getServiceVersion();
      this.insecureTransport = VariablesConverter.useInsecureTransport();
      this.spanEndpoint = VariablesConverter.getSpanEndpoint();
      this.propagator = Propagator.valueOfLabel(VariablesConverter.getPropagator());
      this.resourceLabels = VariablesConverter.getResourceLabels();
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
