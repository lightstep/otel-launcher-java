package com.lightstep.opentelemetry.launcher.example;

import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.exporters.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Span.Kind;
import io.opentelemetry.trace.Tracer;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) throws Exception {
    Properties properties = loadConfig();

    OtlpGrpcSpanExporter exporter = OpenTelemetryConfiguration.newBuilder()
        .setServiceName(properties.getProperty("ls.service.name"))
        .setAccessToken(properties.getProperty("ls.access.token"))
        .setSpanEndpoint(properties.getProperty("otel.exporter.otlp.span.endpoint"))
        .buildExporter();

    OpenTelemetrySdk.getTracerManagement()
        .addSpanProcessor(SimpleSpanProcessor.newBuilder(exporter).build());

    Tracer tracer =
        OpenTelemetry.getTracerProvider().get("LightstepExample");
    Span span = tracer.spanBuilder("start example").setSpanKind(Kind.CLIENT).startSpan();
    span.setAttribute("Attribute 1", "Value 1");
    span.addEvent("Event 0");
    // execute my use case - here we simulate a wait
    doWork();
    span.addEvent("Event 1");
    span.end();

    // wait some seconds
    TimeUnit.SECONDS.sleep(15);

    OpenTelemetrySdk.getTracerManagement().shutdown();
    System.out.println("Bye");
  }

  private static void doWork() throws InterruptedException {
    TimeUnit.SECONDS.sleep(1);
  }

  private static Properties loadConfig()
      throws IOException {
    FileInputStream fs = new FileInputStream("config.properties");
    Properties config = new Properties();
    config.load(fs);
    return config;
  }
}
