package com.lightstep.opentelemetry.launcher;

import com.lightstep.opentelemetry.common.VariablesConverter;
import com.lightstep.opentelemetry.common.VariablesConverter.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.api.trace.propagation.HttpTraceContext;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.DefaultContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.extension.trace.propagation.TraceMultiPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import java.lang.reflect.Method;
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
            put(Propagator.TRACE_CONTEXT, HttpTraceContext.getInstance());
            put(Propagator.B3, B3Propagator.builder().injectMultipleHeaders().build());
            put(Propagator.B3_MULTI, B3Propagator.getInstance());
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

      final DefaultContextPropagators.Builder builder = DefaultContextPropagators.builder();
      if (propagators.remove(Propagator.BAGGAGE)) {
        builder.addTextMapPropagator(W3CBaggagePropagator.getInstance());
      }
      if (propagators.size() == 1) {
        builder.addTextMapPropagator(PROPAGATORS.get(propagators.get(0)));
      } else {
        TraceMultiPropagator.Builder multiPropagatorBuilder = TraceMultiPropagator.builder();
        for (Propagator propagator : propagators) {
          multiPropagatorBuilder.addPropagator(PROPAGATORS.get(propagator));
        }
        builder.addTextMapPropagator(multiPropagatorBuilder.build());
      }

      setGlobalPropagators(builder.build());

      return OtlpGrpcSpanExporter.builder()
          .readSystemProperties()
          .readEnvironmentVariables()
          .build();
    }

    // Workaround https://github.com/open-telemetry/opentelemetry-java/pull/2096
    public static void setGlobalPropagators(ContextPropagators propagators) {
      OpenTelemetry.set(
          OpenTelemetrySdk.builder()
              .setResource(OpenTelemetrySdk.get().getResource())
              .setClock(OpenTelemetrySdk.get().getClock())
              .setMeterProvider(OpenTelemetry.getGlobalMeterProvider())
              .setTracerProvider(unobfuscate(OpenTelemetry.getGlobalTracerProvider()))
              .setPropagators(propagators)
              .build());
    }

    private static TracerProvider unobfuscate(TracerProvider tracerProvider) {
      if (tracerProvider.getClass().getName().endsWith("TracerSdkProvider")) {
        return tracerProvider;
      }
      try {
        Method unobfuscate = tracerProvider.getClass().getDeclaredMethod("unobfuscate");
        unobfuscate.setAccessible(true);
        return (TracerProvider) unobfuscate.invoke(tracerProvider);
      } catch (Throwable t) {
        return tracerProvider;
      }
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
