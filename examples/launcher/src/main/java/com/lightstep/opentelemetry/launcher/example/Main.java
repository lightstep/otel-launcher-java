package com.lightstep.opentelemetry.launcher.example;

import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) throws Exception {
    Properties properties = loadConfig();

    OpenTelemetry openTelemetry = OpenTelemetryConfiguration.newBuilder()
        .setServiceName(properties.getProperty("otel.service.name"))
        .setAccessToken(properties.getProperty("ls.access.token"))
        .setTracesEndpoint(properties.getProperty("otel.exporter.otlp.traces.endpoint"))
        .buildOpenTelemetry();

    Tracer tracer = openTelemetry.getTracer("LightstepExample");
    Span span = tracer.spanBuilder("start example").setSpanKind(SpanKind.CLIENT).startSpan();
    span.setAttribute("Attribute 1", "Value 1");
    span.addEvent("Event 0");
    // execute my use case - here we simulate a wait
    doWork();
    span.addEvent("Event 1");
    span.end();

    // wait some seconds
    TimeUnit.SECONDS.sleep(30);

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
