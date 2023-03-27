package com.lightstep.opentelemetry.common;


import java.io.IOException;
import java.net.InetAddress;
import java.util.logging.Logger;

public class VariablesConverter {
  private static final Logger logger = Logger.getLogger(VariablesConverter.class.getName());

  public static final String DEFAULT_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT = "https://ingest.lightstep.com:443";
  public static final String DEFAULT_OTEL_EXPORTER_OTLP_METRICS_ENDPOINT = "https://ingest.lightstep.com:443";
  public static final long DEFAULT_LS_DEADLINE_MILLIS = 30000;
  public static final boolean DEFAULT_METRICS_ENABLED = false;
  @Deprecated
  public static final boolean DEFAULT_OTEL_EXPORTER_OTLP_INSECURE = false;
  public static final String DEFAULT_PROPAGATOR = "b3multi";
  public static final String DEFAULT_OTEL_LOG_LEVEL = "info";

  static final String OTEL_SERVICE_NAME = "OTEL_SERVICE_NAME";
  @Deprecated
  static final String LS_ACCESS_TOKEN = "LS_ACCESS_TOKEN";
  static final String LS_SERVICE_NAME = "LS_SERVICE_NAME";
  static final String LS_SERVICE_VERSION = "LS_SERVICE_VERSION";
  static final String LS_METRICS_ENABLED = "LS_METRICS_ENABLED";
  @Deprecated
  static final String OTEL_EXPORTER_OTLP_SPAN_ENDPOINT = "OTEL_EXPORTER_OTLP_SPAN_ENDPOINT";
  static final String OTEL_EXPORTER_OTLP_TRACES_ENDPOINT = "OTEL_EXPORTER_OTLP_TRACES_ENDPOINT";
  static final String OTEL_EXPORTER_OTLP_METRICS_ENDPOINT = "OTEL_EXPORTER_OTLP_METRICS_ENDPOINT";
  static final String OTEL_PROPAGATORS = "OTEL_PROPAGATORS";
  @Deprecated
  static final String OTEL_EXPORTER_OTLP_SPAN_INSECURE = "OTEL_EXPORTER_OTLP_SPAN_INSECURE"; // Backwards support.
  @Deprecated
  static final String OTEL_EXPORTER_OTLP_METRIC_INSECURE = "OTEL_EXPORTER_OTLP_METRIC_INSECURE"; // Backwards support.
  static final String OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE = "OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE";
  static final String OTEL_LOG_LEVEL = "OTEL_LOG_LEVEL";
  static final String OTEL_RESOURCE_ATTRIBUTES = "OTEL_RESOURCE_ATTRIBUTES";
  static final String OTEL_METRIC_EXPORT_INTERVAL = "OTEL_METRIC_EXPORT_INTERVAL"; // Yes, _METRIC_, singular.

  // TODO: Once Metrics are enabled by default, check that the metrics endpoint is always set.
  public static void setSystemProperties(Configuration configuration, boolean isAgent) {
    if (configuration.tracesEndpoint == null || configuration.tracesEndpoint.isEmpty()) {
      String msg = "Invalid configuration: traces endpoint missing. Set environment variable "
          + OTEL_EXPORTER_OTLP_TRACES_ENDPOINT;
      if (isAgent) {
        msg += ".";
      } else {
        msg += " or call setTracesEndpoint in the code.";
      }
      logger.severe(msg);
      throw new IllegalStateException(msg);
    }

    configuration.tracesEndpoint = normalizeEndpoint(configuration.tracesEndpoint,
        configuration.tracesInsecureTransport);
    configuration.metricsEndpoint = normalizeEndpoint(configuration.metricsEndpoint,
        configuration.metricsInsecureTransport);

    if (configuration.serviceName == null || configuration.serviceName.isEmpty()) {
      String msg = "Invalid configuration: service name missing. Set environment variable "
          + OTEL_SERVICE_NAME;
      if (isAgent) {
        msg += ".";
      } else {
        msg += " or call setServiceName in the code.";
      }
      logger.severe(msg);
      throw new IllegalStateException(msg);
    }

    if (isTokenRequired(configuration.tracesEndpoint) && (
        configuration.accessToken == null || configuration.accessToken
            .isEmpty())) {
      String msg =
          "Invalid configuration: token missing. Must be set to send data to "
              + configuration.tracesEndpoint
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

    // "otel.exporter.otlp.endpoint" should be replaced by "otel.exporter.otlp.traces.endpoint" in next releases
    System.setProperty("otel.exporter.otlp.endpoint", configuration.tracesEndpoint);
    System.setProperty("otel.exporter.otlp.traces.endpoint", configuration.tracesEndpoint);
    System
        .setProperty("otel.exporter.otlp.timeout", String.valueOf(DEFAULT_LS_DEADLINE_MILLIS));

    if (configuration.accessToken != null && !configuration.accessToken.isEmpty()) {
      System.setProperty("otel.exporter.otlp.headers",
          "lightstep-access-token=" + configuration.accessToken);
    }

    if (configuration.propagators != null) {
      System.setProperty("otel.propagators", configuration.propagators);
    }

    String otelResourceAttributes =
      "telemetry.distro.name=lightstep," +
      "telemetry.distro.version=" + Version.get() + "," +
      "service.name=" + configuration.serviceName;

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
        String msg = "tracesEndpoint: " + configuration.tracesEndpoint;
        if (configuration.propagators != null) {
          msg += ", propagators: " + configuration.propagators;
        }
        msg += ", accessToken: " + configuration.accessToken + ", serviceName: "
            + configuration.serviceName;
        logger.info(msg);
      }
    }

    System.setProperty("otel.traces.exporter", "otlp");
    if (configuration.metricsEnabled) {
      if (configuration.metricsEndpoint != null) {
        System.setProperty("otel.exporter.otlp.metrics.endpoint", configuration.metricsEndpoint);
      }
      if (configuration.metricsExportInterval != null) {
        System
            .setProperty("otel.metric.export.interval", String.valueOf(configuration.metricsExportInterval));
      }

      if (configuration.metricsTemporality != null) {
        System.setProperty("otel.exporter.otlp.metrics.temporality.preference", configuration.metricsTemporality);
      }
      // Enable by default expo histogram aggregation.
      System.setProperty("otel.exporter.otlp.metrics.default.histogram.aggregation", "base2_exponential_bucket_histogram");

      System.setProperty("otel.metrics.exporter", "otlp");
    } else {
      // Disable metrics
      System.setProperty("otel.metrics.exporter", "none");
    }
  }

  static String normalizeEndpoint(String endpoint, boolean insecureTransport) {
    if (endpoint.toLowerCase().startsWith("http://")
        || endpoint.toLowerCase().startsWith("https://")) {
      // Explicit/full endpoint, no need to adjust it.
      return endpoint;
    }

    String prefix = insecureTransport ? "http://" : "https://";
    return prefix + endpoint;
  }

  static boolean isValidToken(String token) {
    return token == null || token.isEmpty() || token.length() == 32 || token.length() == 84
        || token.length() == 104;
  }

  static boolean isTokenRequired(String tracesEndpoint) {
    return DEFAULT_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT.equals(tracesEndpoint);
  }

  public static void convertFromEnv() {
    setSystemProperties(new Configuration()
        .withTracesEndpoint(getTracesEndpoint())
        .withMetricsEndpoint(getMetricsEndpoint())
        .withTracesInsecureTransport(useTracesInsecureTransport())
        .withMetricsInsecureTransport(useMetricsInsecureTransport())
        .withAccessToken(getAccessToken())
        .withPropagators(getPropagator())
        .withLogLevel(getLogLevel())
        .withServiceName(getServiceName())
        .withServiceVersion(getServiceVersion())
        .withResourceAttributes(getResourceAttributes())
        .withMetricsExportInterval(getMetricsExportInterval())
        .withMetricsTemporalityPreference(getMetricsTemporalityPreference())
        .withMetricsEnabled(getMetricsEnabled()), true);
  }

  public static boolean getMetricsEnabled() {
    return Boolean.parseBoolean(getProperty(LS_METRICS_ENABLED, String.valueOf(
        DEFAULT_METRICS_ENABLED)));
  }

  public static Long getMetricsExportInterval() {
    String interval = getProperty(OTEL_METRIC_EXPORT_INTERVAL, null);
    if (interval != null) {
      return Long.parseLong(interval);
    }
    return null;
  }

  public static String getMetricsTemporalityPreference() {
    return getProperty(OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE, null);
  }

  public static String getAccessToken() {
    return getProperty(LS_ACCESS_TOKEN, null);
  }

  public static String getServiceName() {
    return getProperty(OTEL_SERVICE_NAME,
        getProperty(LS_SERVICE_NAME, null));
  }

  public static String getServiceVersion() {
    return getProperty(LS_SERVICE_VERSION, null);
  }

  public static String getLogLevel() {
    return getProperty(OTEL_LOG_LEVEL, DEFAULT_OTEL_LOG_LEVEL);
  }

  public static String getTracesEndpoint() {
    return getProperty(OTEL_EXPORTER_OTLP_TRACES_ENDPOINT,
        getProperty(OTEL_EXPORTER_OTLP_SPAN_ENDPOINT, DEFAULT_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT));
  }

  public static String getMetricsEndpoint() {
    return getProperty(OTEL_EXPORTER_OTLP_METRICS_ENDPOINT,
        DEFAULT_OTEL_EXPORTER_OTLP_METRICS_ENDPOINT);
  }

  public static String getPropagator() {
    return getProperty(OTEL_PROPAGATORS, DEFAULT_PROPAGATOR);
  }

  @Deprecated
  public static boolean useTracesInsecureTransport() {
    return Boolean.parseBoolean(getProperty(OTEL_EXPORTER_OTLP_SPAN_INSECURE, String.valueOf(
        DEFAULT_OTEL_EXPORTER_OTLP_INSECURE)));
  }

  @Deprecated
  public static boolean useMetricsInsecureTransport() {
    return Boolean.parseBoolean(getProperty(OTEL_EXPORTER_OTLP_METRIC_INSECURE, String.valueOf(
        DEFAULT_OTEL_EXPORTER_OTLP_INSECURE)));
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
    private String tracesEndpoint;
    private String metricsEndpoint;
    @Deprecated
    private boolean tracesInsecureTransport;
    @Deprecated
    private boolean metricsInsecureTransport;
    private String accessToken;
    private String propagators;
    private String logLevel;
    private String serviceName;
    private String serviceVersion;
    private String resourceAttributes;

    private boolean metricsEnabled;
    private Long metricsExportInterval;
    private String metricsTemporality;

    public Configuration withTracesEndpoint(String tracesEndpoint) {
      this.tracesEndpoint = tracesEndpoint;
      return this;
    }

    public Configuration withMetricsEndpoint(String metricsEndpoint) {
      this.metricsEndpoint = metricsEndpoint;
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

    public Configuration withMetricsExportInterval(Long metricsExportInterval) {
      this.metricsExportInterval = metricsExportInterval;
      return this;
    }

    public Configuration withMetricsTemporalityPreference(String metricsTemporality) {
      this.metricsTemporality = metricsTemporality;
      return this;
    }

    @Deprecated
    public Configuration withTracesInsecureTransport(boolean insecureTransport) {
      this.tracesInsecureTransport = insecureTransport;
      return this;
    }

    @Deprecated
    public Configuration withMetricsInsecureTransport(boolean insecureTransport) {
      this.metricsInsecureTransport = insecureTransport;
      return this;
    }

    public Configuration withMetricsEnabled(boolean metricsEnabled) {
      this.metricsEnabled = metricsEnabled;
      return this;
    }
  }
}
