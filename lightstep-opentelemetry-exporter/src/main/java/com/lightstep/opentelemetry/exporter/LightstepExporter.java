package com.lightstep.opentelemetry.exporter;

import com.google.common.collect.ImmutableMap;
import com.lightstep.opentelemetry.common.VariablesConverter;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.exporters.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.extensions.trace.propagation.B3Propagator;
import io.opentelemetry.extensions.trace.propagation.JaegerPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.trace.propagation.HttpTraceContext;
import java.util.Map;

public class LightstepExporter {

  public static class Builder {
    private String accessToken;
    private String spanEndpoint;
    private long deadlineMillis;
    private boolean insecureTransport;
    private Propagator propagator;

    private static final Map<Propagator, HttpTextFormat> PROPAGATORS =
        ImmutableMap.of(
            Propagator.TRACE_CONTEXT,
            new HttpTraceContext(),
            Propagator.B3,
            B3Propagator.getMultipleHeaderPropagator(),
            Propagator.B3_SINGLE,
            B3Propagator.getSingleHeaderPropagator(),
            Propagator.JAEGER,
            new JaegerPropagator());

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

    /**
     * Overrides the default deadlineMillis with the provided value.
     *
     * @param deadlineMillis The maximum amount of time the tracer should wait for a response from
     * the collector when sending a report.
     * @return this builder's instance
     */
    public Builder setDeadlineMillis(long deadlineMillis) {
      this.deadlineMillis = deadlineMillis;
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
    public OtlpGrpcSpanExporter build() {
      VariablesConverter
          .setSystemProperties(spanEndpoint, insecureTransport, deadlineMillis, accessToken,
              null, null, false);

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
      BatchSpanProcessor spansProcessor = BatchSpanProcessor.newBuilder(this.build()).build();
      OpenTelemetrySdk.getTracerProvider().addSpanProcessor(spansProcessor);
    }

    private void readEnvVariablesAndSystemProperties() {
      this.accessToken = VariablesConverter.getAccessToken();
      this.insecureTransport = VariablesConverter.useInsecureTransport();
      this.deadlineMillis = VariablesConverter.getDeadlineMillis();
      this.spanEndpoint = VariablesConverter.getSpanEndpoint();
      this.propagator = Propagator.valueOf(VariablesConverter.getPropagator());
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
