package com.lightstep.opentelemetry.common;

public class VariablesConverter {
  public static final String DEFAULT_LS_SATELLITE_URL = "ingest.lightstep.com";
  public static final long DEFAULT_LS_DEADLINE_MILLIS = 30000;
  public static final boolean DEFAULT_LS_USE_TLS = true;

  public static void convert(String satelliteUrl, boolean useTls,
      long deadlineMillis, String accessToken) {
    System.setProperty("otel.otlp.endpoint", satelliteUrl);
    System.setProperty("otel.otlp.use.tls", String.valueOf(useTls));
    System.setProperty("otel.otlp.span.timeout", String.valueOf(deadlineMillis));
    System.setProperty("otel.otlp.metadata", "lightstep-access-token=" + accessToken);
  }

  public static void convertFromEnv() {
    convert(getSatelliteUrl(), useTransportSecurity(), getDeadlineMillis(), getAccessToken());
  }

  public static String getAccessToken() {
    return getProperty("LS_ACCESS_TOKEN", "");
  }

  public static String getSatelliteUrl() {
    return getProperty("LS_SATELLITE_URL", DEFAULT_LS_SATELLITE_URL);
  }

  public static boolean useTransportSecurity() {
    return Boolean.parseBoolean(getProperty("LS_USE_TLS", String.valueOf(DEFAULT_LS_USE_TLS)));
  }

  public static long getDeadlineMillis() {
    return Long.parseLong(
        getProperty("LS_DEADLINE_MILLIS", String.valueOf(DEFAULT_LS_DEADLINE_MILLIS)));
  }

  private static String getProperty(String name, String defaultValue) {
    String val = System.getProperty(name, System.getenv(name));
    if (val == null || val.isEmpty()) {
      return defaultValue;
    }
    return val;
  }
}
