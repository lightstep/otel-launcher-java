package com.lightstep.opentelemetry.common;


import java.util.logging.Logger;

public class VariablesConverter {
  private static final Logger logger = Logger.getLogger(VariablesConverter.class.getName());

  public static final String DEFAULT_OTEL_EXPORTER_OTLP_SPAN_ENDPOINT = "ingest.lightstep.com";
  public static final long DEFAULT_LS_DEADLINE_MILLIS = 30000;
  public static final boolean DEFAULT_OTEL_EXPORTER_OTLP_SPAN_INSECURE = false;
  public static final String DEFAULT_PROPAGATOR = "b3";
  public static final String DEFAULT_OTEL_LOG_LEVEL = "info";

  static final String LS_ACCESS_TOKEN = "LS_ACCESS_TOKEN";
  static final String OTEL_EXPORTER_OTLP_SPAN_ENDPOINT = "OTEL_EXPORTER_OTLP_SPAN_ENDPOINT";
  static final String OTEL_PROPAGATORS = "OTEL_PROPAGATORS";
  static final String OTEL_EXPORTER_OTLP_SPAN_INSECURE = "OTEL_EXPORTER_OTLP_SPAN_INSECURE";
  static final String OTEL_LOG_LEVEL = "OTEL_LOG_LEVEL";
  static final String OTEL_RESOURCE_ATTRIBUTES = "OTEL_RESOURCE_ATTRIBUTES";

  public static void setSystemProperties(String spanEndpoint,
      boolean insecureTransport,
      String accessToken,
      String propagator,
      String logLevel,
      boolean isAgent) {

    if (!hasServiceName()) {
      String msg = "Invalid configuration: service name missing. Set environment variable "
          + "OTEL_RESOURCE_ATTRIBUTES with value service.name=<your-service>.";
      logger.severe(msg);
      throw new IllegalStateException(msg);
    }

    if (isTokenRequired(spanEndpoint) && (accessToken == null || accessToken.isEmpty())) {
      String msg =
          "Invalid configuration: token missing. Must be set to send data to " + spanEndpoint
              + ". Set environment variable LS_ACCESS_TOKEN";
      if (isAgent) {
        msg += ".";
      } else {
        msg += " or call setAccessToken in the code.";
      }
      logger.severe(msg);
      throw new IllegalStateException(msg);
    }

    if (!isValidToken(accessToken)) {
      String msg = "Invalid configuration: invalid token. Token must be a 32, 84 or 104 character long string.";
      logger.severe(msg);
      throw new IllegalStateException(msg);
    }

    System.setProperty("otel.otlp.endpoint", spanEndpoint);
    System.setProperty("otel.otlp.use.tls", String.valueOf(!insecureTransport));
    System.setProperty("otel.otlp.span.timeout", String.valueOf(DEFAULT_LS_DEADLINE_MILLIS));
    System.setProperty("otel.otlp.metadata", "lightstep-access-token=" + accessToken);
    if (propagator != null) {
      System.setProperty("ota.propagators", propagator);
    }
    if (logLevel != null) {
      System.setProperty("io.opentelemetry.auto.slf4j.simpleLogger.defaultLogLevel", logLevel);
    }
  }

  static boolean isValidToken(String token) {
    return token == null || token.isEmpty() || token.length() == 32 || token.length() == 84
        || token.length() == 104;
  }

  static boolean isTokenRequired(String spanEndpoint) {
    return DEFAULT_OTEL_EXPORTER_OTLP_SPAN_ENDPOINT.equals(spanEndpoint);
  }

  public static void convertFromEnv() {
    setSystemProperties(getSpanEndpoint(), useInsecureTransport(),
        getAccessToken(), getPropagator(), getLogLevel(), true);
  }

  public static String getAccessToken() {
    return getProperty(LS_ACCESS_TOKEN, "");
  }

  public static boolean hasServiceName() {
    final String otelResourceAttributes = System.getenv(OTEL_RESOURCE_ATTRIBUTES);
    if (otelResourceAttributes == null) {
      return false;
    }
    return otelResourceAttributes.matches("service.name\\s*=\\s*\\S+");
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

  private static String getProperty(String name, String defaultValue) {
    String val = System.getProperty(name.toLowerCase().replaceAll("_", "."), System.getenv(name));
    if (val == null || val.isEmpty()) {
      return defaultValue;
    }
    return val;
  }
}
