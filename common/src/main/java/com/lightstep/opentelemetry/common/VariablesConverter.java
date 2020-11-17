package com.lightstep.opentelemetry.common;


import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

public class VariablesConverter {
  private static final Logger logger = Logger.getLogger(VariablesConverter.class.getName());

  public static final String DEFAULT_OTEL_EXPORTER_OTLP_SPAN_ENDPOINT = "ingest.lightstep.com";
  public static final String DEFAULT_OTEL_EXPORTER_OTLP_METRIC_ENDPOINT = "ingest.lightstep.com";
  public static final long DEFAULT_LS_DEADLINE_MILLIS = 30000;
  public static final boolean DEFAULT_OTEL_EXPORTER_OTLP_SPAN_INSECURE = false;
  public static final boolean DEFAULT_METRICS_ENABLED = false;
  public static final String DEFAULT_PROPAGATOR = "b3";
  public static final String DEFAULT_OTEL_LOG_LEVEL = "info";

  static final String LS_ACCESS_TOKEN = "LS_ACCESS_TOKEN";
  static final String LS_SERVICE_NAME = "LS_SERVICE_NAME";
  static final String LS_SERVICE_VERSION = "LS_SERVICE_VERSION";
  static final String LS_METRICS_ENABLED = "LS_METRICS_ENABLED";
  static final String OTEL_EXPORTER_OTLP_SPAN_ENDPOINT = "OTEL_EXPORTER_OTLP_SPAN_ENDPOINT";
  static final String OTEL_EXPORTER_OTLP_METRIC_ENDPOINT = "OTEL_EXPORTER_OTLP_METRIC_ENDPOINT";
  static final String OTEL_PROPAGATORS = "OTEL_PROPAGATORS";
  static final String OTEL_EXPORTER_OTLP_SPAN_INSECURE = "OTEL_EXPORTER_OTLP_SPAN_INSECURE";
  static final String OTEL_LOG_LEVEL = "OTEL_LOG_LEVEL";
  static final String OTEL_RESOURCE_ATTRIBUTES = "OTEL_RESOURCE_ATTRIBUTES";
  static final String OTEL_IMR_EXPORT_INTERVAL = "OTEL_IMR_EXPORT_INTERVAL";

  public static void setSystemProperties(Configuration configuration, boolean isAgent) {
    if (configuration.spanEndpoint == null || configuration.spanEndpoint.isEmpty()) {
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

    if (configuration.serviceName == null || configuration.serviceName.isEmpty()) {
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

    if (isTokenRequired(configuration.spanEndpoint) && (
        configuration.accessToken == null || configuration.accessToken
            .isEmpty())) {
      String msg =
          "Invalid configuration: token missing. Must be set to send data to "
              + configuration.spanEndpoint
              + ". Set environment variable " + LS_ACCESS_TOKEN;
      if (isAgent) {
        msg += ".";
      } else {
        msg += " or call setAccessToken in the code.";
      }
      logger.severe(msg);
      throw new IllegalStateException(msg);
    }

    if (!isValidToken(configuration.accessToken)) {
      String msg = "Invalid configuration: invalid token. Token must be a 32, 84 or 104 character long string.";
      logger.severe(msg);
      throw new IllegalStateException(msg);
    }

    System.setProperty("otel.exporter.otlp.span.endpoint", configuration.spanEndpoint);
    System
        .setProperty("otel.exporter.otlp.span.insecure",
            String.valueOf(configuration.insecureTransport));
    System
        .setProperty("otel.exporter.otlp.span.timeout", String.valueOf(DEFAULT_LS_DEADLINE_MILLIS));
    System.setProperty("otel.exporter.otlp.span.headers",
        "lightstep-access-token=" + configuration.accessToken);
    if (configuration.propagators != null) {
      System.setProperty("otel.propagators", configuration.propagators);
    }

    String otelResourceAttributes = "service.name=" + configuration.serviceName;
    if (configuration.serviceVersion != null) {
      otelResourceAttributes += ",service.version=" + configuration.serviceVersion;
    }

    if (configuration.resourceAttributes == null || configuration.resourceAttributes.isEmpty()
        || !configuration.resourceAttributes.contains("host.name=")) {
      String hostname = getHostName();
      if (hostname != null && !hostname.isEmpty()) {
        otelResourceAttributes += ",host.name=" + hostname;
      }
    }

    if (configuration.resourceAttributes != null && !configuration.resourceAttributes.isEmpty()) {
      otelResourceAttributes += "," + configuration.resourceAttributes;
    }

    System.setProperty("otel.resource.attributes", otelResourceAttributes);

    if (configuration.logLevel != null) {
      System.setProperty("io.opentelemetry.javaagent.slf4j.simpleLogger.defaultLogLevel",
          configuration.logLevel);
      if (configuration.logLevel.equals("debug")) {
        String msg = "spanEndpoint: " + configuration.spanEndpoint;
        if (configuration.propagators != null) {
          msg += ", propagators: " + configuration.propagators;
        }
        msg += ", accessToken: " + configuration.accessToken + ", serviceName: "
            + configuration.serviceName;
        logger.info(msg);
      }
    }

    if (configuration.metricsEnabled) {
      if (configuration.metricEndpoint == null || configuration.metricEndpoint.isEmpty()) {
        String msg = "Invalid configuration: metric endpoint missing. Set environment variable "
            + OTEL_EXPORTER_OTLP_METRIC_ENDPOINT;
        if (isAgent) {
          msg += ".";
        } else {
          msg += " or call setMetricEndpoint in the code.";
        }
        logger.severe(msg);
        throw new IllegalStateException(msg);
      }

      if (configuration.exportInterval != null) {
        System
            .setProperty("otel.imr.export.interval", String.valueOf(configuration.exportInterval));
      }
      System.setProperty("otel.exporter.otlp.metric.endpoint", configuration.metricEndpoint);
      System.setProperty("otel.exporter.otlp.metric.insecure",
          String.valueOf(configuration.insecureTransport));
      System.setProperty("otel.exporter.otlp.metric.headers",
          "lightstep-access-token=" + configuration.accessToken);
      System.setProperty("otel.exporter.otlp.timeout", String.valueOf(DEFAULT_LS_DEADLINE_MILLIS));
    } else {
      // Disable metrics
      System.setProperty("otel.exporter", "otlp_span");
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
    setSystemProperties(new Configuration()
        .withSpanEndpoint(getSpanEndpoint())
        .withInsecureTransport(useInsecureTransport())
        .withAccessToken(getAccessToken())
        .withPropagators(getPropagator())
        .withLogLevel(getLogLevel())
        .withServiceName(getServiceName())
        .withServiceVersion(getServiceVersion())
        .withResourceAttributes(getResourceAttributes())
        .withMetricEndpoint(getMetricEndpoint())
        .withExportInterval(getExportInterval())
        .withMetricsEnabled(getMetricsEnabled()), true);
  }

  private static boolean getMetricsEnabled() {
    return Boolean.parseBoolean(getProperty(LS_METRICS_ENABLED, String.valueOf(
        DEFAULT_METRICS_ENABLED)));
  }

  private static Long getExportInterval() {
    String interval = getProperty(OTEL_IMR_EXPORT_INTERVAL, null);
    if (interval != null) {
      return Long.parseLong(interval);
    }
    return null;
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

  public static String getMetricEndpoint() {
    return getProperty(OTEL_EXPORTER_OTLP_METRIC_ENDPOINT,
        DEFAULT_OTEL_EXPORTER_OTLP_METRIC_ENDPOINT);
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

  static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (final IOException e) {
      return "";
    }
  }

  public static class Configuration {
    private String spanEndpoint;
    private boolean insecureTransport;
    private String accessToken;
    private String propagators;
    private String logLevel;
    private String serviceName;
    private String serviceVersion;
    private String resourceAttributes;

    private boolean metricsEnabled;
    private String metricEndpoint;
    private Long exportInterval;

    public Configuration withSpanEndpoint(String spanEndpoint) {
      this.spanEndpoint = spanEndpoint;
      return this;
    }

    public Configuration withAccessToken(String accessToken) {
      this.accessToken = accessToken;
      return this;
    }

    public Configuration withPropagators(String propagators) {
      this.propagators = propagators;
      return this;
    }

    public Configuration withLogLevel(String logLevel) {
      this.logLevel = logLevel;
      return this;
    }

    public Configuration withServiceName(String serviceName) {
      this.serviceName = serviceName;
      return this;
    }

    public Configuration withServiceVersion(String serviceVersion) {
      this.serviceVersion = serviceVersion;
      return this;
    }

    public Configuration withResourceAttributes(String resourceAttributes) {
      this.resourceAttributes = resourceAttributes;
      return this;
    }

    public Configuration withInsecureTransport(boolean insecureTransport) {
      this.insecureTransport = insecureTransport;
      return this;
    }

    public Configuration withMetricEndpoint(String metricEndpoint) {
      this.metricEndpoint = metricEndpoint;
      return this;
    }

    public Configuration withExportInterval(Long exportInterval) {
      this.exportInterval = exportInterval;
      return this;
    }

    public Configuration withMetricsEnabled(boolean metricsEnabled) {
      this.metricsEnabled = metricsEnabled;
      return this;
    }
  }
}
