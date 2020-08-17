package com.lightstep.opentelemetry.launcher.example;

import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.extensions.auto.annotations.WithSpan;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class App {

  @WithSpan
  public static void nestedMethod() throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder()
        .url("https://www.google.com")
        .build();

    try (Response response = client.newCall(request).execute()) {
      System.out.println("Response code: " + response.code());
    }

    client.dispatcher().executorService().shutdown();
    client.connectionPool().evictAll();
  }

  public static void firstMethod() throws Exception {
    Tracer tracer = OpenTelemetry.getTracer(System.getenv("LS_SERVICE_NAME"));
    Span span = tracer.spanBuilder("firstMethod").startSpan();
    try (Scope scope = tracer.withSpan(span)) {
      nestedMethod();
    }
    span.end();
  }

  public static void main(String[] args) throws Exception {
    firstMethod();

    TimeUnit.SECONDS.sleep(30);
  }
}
