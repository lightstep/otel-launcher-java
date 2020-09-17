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
  static final String LS_SERVICE_NAME = "LS_SERVICE_NAME";
  static final String LS_SERVICE_VERSION = "LS_SERVICE_VERSION";
  static final String OTEL_EXPORTER_OTLP_SPAN_ENDPOINT = "OTEL_EXPORTER_OTLP_SPAN_ENDPOINT";
  static final String OTEL_PROPAGATORS = "OTEL_PROPAGATORS";
  static final String OTEL_EXPORTER_OTLP_SPAN_INSECURE = "OTEL_EXPORTER_OTLP_SPAN_INSECURE";
  static final String OTEL_LOG_LEVEL = "OTEL_LOG_LEVEL";
  static final String OTEL_RESOURCE_ATTRIBUTES = "OTEL_RESOURCE_ATTRIBUTES";

  public static void setSystemProperties(String spanEndpoint,
      boolean insecureTransport,
      String accessToken,
      String propagators,
      String logLevel,
      String serviceName,
      String serviceVersion,
      String resourceAttributes,
      boolean isAgent) {

    if (spanEndpoint == null || spanEndpoint.isEmpty()) {
      String msg = "Invalid configuration: span endpoint missing. Set environment variable "
          + OTEL_EXPORTER_OTLP_SPAN_ENDPOINT;
      if (isAgent) {
        msg += ".";
      } else {
        msg += " or call setSpanEndpoint in the code.";
      }
      logger.severe(msg);
      throw new IllegalStateException(msg);
    }

    if (serviceName == null || serviceName.isEmpty()) {
      String msg = "Invalid configuration: service name missing. Set environment variable "
          + LS_SERVICE_NAME;
      if (isAgent) {
        msg += ".";
      } else {
        msg += " or call setServiceName in the code.";
      }
      logger.severe(msg);
      throw new IllegalStateException(msg);
    }

    if (isTokenRequired(spanEndpoint) && (accessToken == null || accessToken.isEmpty())) {
      String msg =
          "Invalid configuration: token missing. Must be set to send data to " + spanEndpoint
              + ". Set environment variable " + LS_ACCESS_TOKEN;
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
    if (propagators != null) {
      System.setProperty("otel.propagators", propagators);
    }

    String otelResourceAttributes = "service.name=" + serviceName;
    if (serviceVersion != null) {
      otelResourceAttributes += ",service.version=" + serviceVersion;
    }
    if (resourceAttributes != null && !resourceAttributes.isEmpty()) {
      otelResourceAttributes += "," + resourceAttributes;
    }
    System.setProperty("otel.resource.attributes", otelResourceAttributes);

    if (logLevel != null) {
      System.setProperty("io.opentelemetry.javaagent.slf4j.simpleLogger.defaultLogLevel", logLevel);
      if (logLevel.equals("debug")) {
        String msg = "spanEndpoint: " + spanEndpoint;
        if (propagators != null) {
          msg += ", propagators: " + propagators;
        }
        msg += ", accessToken: " + accessToken + ", serviceName: " + serviceName;
        logger.info(msg);
      }
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
        getAccessToken(), getPropagator(), getLogLevel(), getServiceName(), getServiceVersion(),
        getResourceAttributes(), true);
  }

  public static String getAccessToken() {
    return getProperty(LS_ACCESS_TOKEN, "");
  }

  public static String getServiceName() {
    return getProperty(LS_SERVICE_NAME, null);
  }

  public static String getServiceVersion() {
    return getProperty(LS_SERVICE_VERSION, null);
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

  // Internal usage, do not need to expose publicly.
  public static String getResourceAttributes() {
    return getProperty(OTEL_RESOURCE_ATTRIBUTES, null);
  }

  private static String getProperty(String name, String defaultValue) {
    String val = System.getProperty(name.toLowerCase().replaceAll("_", "."), System.getenv(name));
    if (val == null || val.isEmpty()) {
      return defaultValue;
    }
    return val;
  }
}
