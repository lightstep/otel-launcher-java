package com.lightstep.opentelemetry.agent.manual.example;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.extensions.auto.annotations.WithSpan;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import java.util.concurrent.TimeUnit;

public class App {

  @WithSpan
  public static void nestedMethod() throws Exception {
    System.out.println("Second method");
    TimeUnit.SECONDS.sleep(1);
  }

  public static void firstMethod() throws Exception {
    Tracer tracer = OpenTelemetry.getTracer(System.getenv("LS_SERVICE_NAME"));
    Span span = tracer.spanBuilder("firstMethod").startSpan();
    nestedMethod();
    span.end();
  }

  public static void main(String[] args) throws Exception {
    firstMethod();

    TimeUnit.SECONDS.sleep(30);
  }
}
