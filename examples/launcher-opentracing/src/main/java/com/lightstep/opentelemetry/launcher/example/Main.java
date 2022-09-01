package com.lightstep.opentelemetry.launcher.example;

import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration;
import com.lightstep.opentelemetry.launcher.Propagator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.log.Fields;
import io.opentracing.tag.Tags;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) throws Exception {
    Properties properties = loadConfig();

    // Configure OpenTelemetry via the Lightstep Launcher.
    OpenTelemetryConfiguration.Builder configBuilder = OpenTelemetryConfiguration.newBuilder()
        .setServiceName(properties.getProperty("otel.service.name"))
        .setTracesEndpoint(properties.getProperty("otel.exporter.otlp.traces.endpoint"))
        .setAccessToken(properties.getProperty("ls.access.token"));

    // Optionally configure desired propagation formats
    // (defaults to B3 only).
    configBuilder
      .setPropagator(Propagator.TRACE_CONTEXT) // W3C TraceContext
      .setPropagator(Propagator.BAGGAGE) // Baggage
      .setPropagator(Propagator.B3_MULTI) // Multiple B3 formats
      .setPropagator(Propagator.OT_TRACE); // OpenTracing format

    // Build the OpenTelemetry SDK and expose it as an OpenTracing Tracer.
    Tracer tracer = OpenTracingShim.createTracerShim(
        configBuilder.buildOpenTelemetry().getOpenTelemetrySdk());

    // Use the OpenTracing API.
    Span parentSpan = tracer
      .buildSpan("parent_span")
      .withTag("snack", "pepperoni")
      .withTag("extra_id", 107)
      .start();
    parentSpan.log("Starting outer span");
    TimeUnit.SECONDS.sleep(1);

    try (Scope scope = tracer.activateSpan(parentSpan)) {
      Span childSpan = tracer.buildSpan("child_span")
        .withTag("hello", "world")
        .start();

      try (Scope innerScope = tracer.activateSpan(childSpan)) {
        doWork();
      } catch (Exception e) {
        Tags.ERROR.set(childSpan, true);
        childSpan.log(Map.of(Fields.EVENT, "error", Fields.ERROR_OBJECT, e, Fields.MESSAGE, e.getMessage()));
      } finally {
        childSpan.finish();
      }
    } finally {
      parentSpan.finish();
    }

    TimeUnit.SECONDS.sleep(15);
    System.out.println("Bye");
  }

  private static void doWork() throws InterruptedException {
    TimeUnit.SECONDS.sleep(1);
    throw new RuntimeException("Failed to perform internal work");
  }

  private static Properties loadConfig()
      throws IOException {
    FileInputStream fs = new FileInputStream("config.properties");
    Properties config = new Properties();
    config.load(fs);
    return config;
  }
}
