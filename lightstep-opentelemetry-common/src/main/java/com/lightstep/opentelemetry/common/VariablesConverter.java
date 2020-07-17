package com.lightstep.opentelemetry.common;

public class VariablesConverter {
  public static final String DEFAULT_OTEL_EXPORTER_OTLP_SPAN_ENDPOINT = "ingest.lightstep.com";
  public static final long DEFAULT_LS_DEADLINE_MILLIS = 30000;
  public static final boolean DEFAULT_OTEL_EXPORTER_OTLP_SPAN_INSECURE = false;
  public static final String DEFAULT_PROPAGATOR = "b3";
  public static final String DEFAULT_OTEL_LOG_LEVEL = "info";

  private static final String LS_ACCESS_TOKEN = "LS_ACCESS_TOKEN";
  private static final String OTEL_EXPORTER_OTLP_SPAN_ENDPOINT = "OTEL_EXPORTER_OTLP_SPAN_ENDPOINT";
  private static final String OTEL_PROPAGATORS = "OTEL_PROPAGATORS";
  private static final String OTEL_EXPORTER_OTLP_SPAN_INSECURE = "OTEL_EXPORTER_OTLP_SPAN_INSECURE";
  private static final String LS_DEADLINE_MILLIS = "LS_DEADLINE_MILLIS";
  private static final String OTEL_LOG_LEVEL = "OTEL_LOG_LEVEL";

  public static void setSystemProperties(String spanEndpoint,
      boolean insecureTransport,
      long deadlineMillis,
      String accessToken,
      String propagator,
      String logLevel) {
    System.setProperty("otel.otlp.endpoint", spanEndpoint);
    System.setProperty("otel.otlp.use.tls", String.valueOf(!insecureTransport));
    System.setProperty("otel.otlp.span.timeout", String.valueOf(deadlineMillis));
    System.setProperty("otel.otlp.metadata", "lightstep-access-token=" + accessToken);
    if (propagator != null) {
      System.setProperty("ota.propagators", propagator);
    }
    if (logLevel != null) {
      System.setProperty("io.opentelemetry.auto.slf4j.simpleLogger.defaultLogLevel", logLevel);
    }
  }

  public static void convertFromEnv() {
    setSystemProperties(getSpanEndpoint(), useInsecureTransport(), getDeadlineMillis(),
        getAccessToken(), getPropagator(), getLogLevel());
  }

  public static String getAccessToken() {
    return getProperty(LS_ACCESS_TOKEN, "");
  }

  public static String getLogLevel() {
    return getProperty(OTEL_LOG_LEVEL, DEFAULT_OTEL_LOG_LEVEL);
  }

  public static String getSpanEndpoint() {
    return getProperty(OTEL_EXPORTER_OTLP_SPAN_ENDPOINT, DEFAULT_OTEL_EXPORTER_OTLP_SPAN_ENDPOINT);
  }

  public static String getPropagator() {
    return getProperty(OTEL_PROPAGATORS, DEFAULT_PROPAGATOR);
  }

  public static boolean useInsecureTransport() {
    return Boolean.parseBoolean(getProperty(OTEL_EXPORTER_OTLP_SPAN_INSECURE, String.valueOf(
        DEFAULT_OTEL_EXPORTER_OTLP_SPAN_INSECURE)));
  }

  public static long getDeadlineMillis() {
    return Long.parseLong(
        getProperty(LS_DEADLINE_MILLIS, String.valueOf(DEFAULT_LS_DEADLINE_MILLIS)));
  }

  private static String getProperty(String name, String defaultValue) {
    String val = System.getProperty(name.toLowerCase().replaceAll("_", "."), System.getenv(name));
    if (val == null || val.isEmpty()) {
      return defaultValue;
    }
    return val;
  }
}
