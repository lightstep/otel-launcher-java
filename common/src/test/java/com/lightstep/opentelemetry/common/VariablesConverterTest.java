package com.lightstep.opentelemetry.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VariablesConverter.class})
public class VariablesConverterTest {

  @Before
  public void before() {
    System.clearProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN));
    System.clearProperty(toSystemProperty(VariablesConverter.LS_SERVICE_NAME));
    System.clearProperty(toSystemProperty(VariablesConverter.LS_SERVICE_VERSION));
    System.clearProperty(toSystemProperty(VariablesConverter.LS_METRICS_ENABLED));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_LOG_LEVEL));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_ENDPOINT));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_TRACES_ENDPOINT));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_METRICS_ENDPOINT));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_INSECURE));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_METRIC_INSECURE));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_PROPAGATORS));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES));
  }

  @Test
  public void convertFromEnv() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME), "service-1");
    System.setProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN),
        StringUtils.repeat("s", 32));
    System.setProperty(toSystemProperty(VariablesConverter.LS_SERVICE_VERSION), "1.0");

    VariablesConverter.convertFromEnv();

    String hostname = VariablesConverter.getHostName();

    String resourceAttributes = System.getProperty("otel.resource.attributes");
    assertTrue(resourceAttributes.contains("service.name=service-1"));
    assertTrue(resourceAttributes.contains("service.version=1.0"));
    assertTrue(resourceAttributes.contains("host.name=" + hostname));
  }

  @Test
  public void convertFromEnv_withAttributes() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME), "service-1");
    System.setProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN),
        StringUtils.repeat("s", 32));
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES),
        "host.name=my-host");

    VariablesConverter.convertFromEnv();

    String hostname = VariablesConverter.getHostName();

    String resourceAttributes = System.getProperty("otel.resource.attributes");
    assertTrue(resourceAttributes.contains("service.name=service-1"));
    assertTrue(resourceAttributes.contains("host.name=my-host"));
    assertFalse(resourceAttributes.contains("host.name=" + hostname));

    assertEquals(VariablesConverter.DEFAULT_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT, System
        .getProperty("otel.exporter.otlp.traces.endpoint"));
  }

  @Test
  public void convertFromEnv_withtMetricsDisabled() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME), "service-1");
    System.setProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN),
        StringUtils.repeat("s", 32));
    System.setProperty(toSystemProperty(VariablesConverter.LS_METRICS_ENABLED), "false");

    VariablesConverter.convertFromEnv();

    assertNull(System.getProperty("otel.exporter.otlp.metrics.endpoint"));
  }

  @Test
  public void convertFromEnv_withMetricsEnabled() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME), "service-1");
    System.setProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN),
        StringUtils.repeat("s", 32));
    System.setProperty(toSystemProperty(VariablesConverter.LS_METRICS_ENABLED), "true");

    VariablesConverter.convertFromEnv();

    assertEquals(VariablesConverter.DEFAULT_OTEL_EXPORTER_OTLP_METRICS_ENDPOINT, System
        .getProperty("otel.exporter.otlp.metrics.endpoint"));
  }

  @Test
  public void convertFromEnv_InsecureFalse() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME), "service-1");
    System.setProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN),
        StringUtils.repeat("s", 32));
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_TRACES_ENDPOINT),
        "endpoint");
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_INSECURE),
        "false");

    VariablesConverter.convertFromEnv();

    assertEquals("https://endpoint", System.getProperty("otel.exporter.otlp.traces.endpoint"));
  }

  @Test
  public void convertFromEnv_InsecureTrue() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME), "service-1");
    System.setProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN),
        StringUtils.repeat("s", 32));
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_TRACES_ENDPOINT),
        "endpoint");
    System
        .setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_INSECURE), "true");

    VariablesConverter.convertFromEnv();

    assertEquals("http://endpoint", System.getProperty("otel.exporter.otlp.traces.endpoint"));
  }

  @Test
  public void convertFromEnv_InsecureMetricFalse() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME), "service-1");
    System.setProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN),
        StringUtils.repeat("s", 32));
    System.setProperty(toSystemProperty(VariablesConverter.LS_METRICS_ENABLED), "true");
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_METRICS_ENDPOINT),
        "endpoint");
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_METRIC_INSECURE),
        "false");

    VariablesConverter.convertFromEnv();

    assertEquals("https://endpoint", System.getProperty("otel.exporter.otlp.metrics.endpoint"));
  }

  @Test
  public void convertFromEnv_InsecureMetricTrue() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME), "service-1");
    System.setProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN),
        StringUtils.repeat("s", 32));
    System.setProperty(toSystemProperty(VariablesConverter.LS_METRICS_ENABLED), "true");
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_METRICS_ENDPOINT),
        "endpoint");
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_METRIC_INSECURE),
        "true");

    VariablesConverter.convertFromEnv();

    assertEquals("http://endpoint", System.getProperty("otel.exporter.otlp.metrics.endpoint"));
  }

  @Test
  public void isValidToken() {
    assertTrue(VariablesConverter.isValidToken(StringUtils.repeat("s", 32)));
    assertTrue(VariablesConverter.isValidToken(StringUtils.repeat("s", 84)));
    assertTrue(VariablesConverter.isValidToken(StringUtils.repeat("s", 104)));

    assertFalse(VariablesConverter.isValidToken(StringUtils.repeat("s", 30)));
    assertFalse(VariablesConverter.isValidToken(StringUtils.repeat("s", 83)));
    assertFalse(VariablesConverter.isValidToken(StringUtils.repeat("s", 105)));
  }

  @Test
  public void isTokenRequired() {
    assertTrue(VariablesConverter
        .isTokenRequired(VariablesConverter.DEFAULT_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT));

    assertFalse(VariablesConverter.isTokenRequired("localhost"));
  }

  @Test
  public void getLogLevel_Default() {
    assertEquals(VariablesConverter.DEFAULT_OTEL_LOG_LEVEL, VariablesConverter.getLogLevel());
  }

  @Test
  public void getLogLevel_fromSystemProperty() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_LOG_LEVEL), "debug");
    assertEquals("debug", VariablesConverter.getLogLevel());
  }

  @Test
  public void getLogLevel_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_LOG_LEVEL)).thenReturn("error");
    assertEquals("error", VariablesConverter.getLogLevel());
  }

  @Test
  public void getServiceName_Default() {
    assertNull(VariablesConverter.getServiceName());
  }

  @Test
  public void getServiceName_fromSystemProperty() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_SERVICE_NAME), "service-prop");
    assertEquals("service-prop", VariablesConverter.getServiceName());
  }

  @Test
  public void getServiceName_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_SERVICE_NAME)).thenReturn("service-env");
    assertEquals("service-env", VariablesConverter.getServiceName());
  }

  @Test
  public void getServiceName_legacy() {
    System.setProperty(toSystemProperty(VariablesConverter.LS_SERVICE_NAME), "service-prop");
    assertEquals("service-prop", VariablesConverter.getServiceName());
  }

  @Test
  public void getServiceVersion_Default() {
    assertNull(VariablesConverter.getServiceVersion());
  }

  @Test
  public void getServiceVersion_fromSystemProperty() {
    System.setProperty(toSystemProperty(VariablesConverter.LS_SERVICE_VERSION), "version-prop");
    assertEquals("version-prop", VariablesConverter.getServiceVersion());
  }

  @Test
  public void getServiceVersion_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.LS_SERVICE_VERSION)).thenReturn("version-env");
    assertEquals("version-env", VariablesConverter.getServiceVersion());
  }

  @Test
  public void getAccessToken_Default() {
    assertNull(VariablesConverter.getAccessToken());
  }

  @Test
  public void getAccessToken_fromSystemProperty() {
    System.setProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN), "token-prop");
    assertEquals("token-prop", VariablesConverter.getAccessToken());
  }

  @Test
  public void getAccessToken_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.LS_ACCESS_TOKEN)).thenReturn("token-env");
    assertEquals("token-env", VariablesConverter.getAccessToken());
  }

  @Test
  public void getTracesEndpoint_Default() {
    assertEquals(VariablesConverter.DEFAULT_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT,
        VariablesConverter.getTracesEndpoint());
  }

  @Test
  public void getTracesEndpoint_fromSystemProperty() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_TRACES_ENDPOINT),
        "endpoint-prop");
    assertEquals("endpoint-prop", VariablesConverter.getTracesEndpoint());
  }

  @Test
  public void getTracesEndpoint_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_EXPORTER_OTLP_TRACES_ENDPOINT))
        .thenReturn("endpoint-env");
    assertEquals("endpoint-env", VariablesConverter.getTracesEndpoint());
  }

  @Test
  public void getMetricsEndpoint_Default() {
    assertEquals(VariablesConverter.DEFAULT_OTEL_EXPORTER_OTLP_METRICS_ENDPOINT,
        VariablesConverter.getMetricsEndpoint());
  }

  @Test
  public void getMetricsEndpoint_fromSystemProperty() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_METRICS_ENDPOINT),
        "endpoint-prop");
    assertEquals("endpoint-prop", VariablesConverter.getMetricsEndpoint());
  }

  @Test
  public void getMetricsEndpoint_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_EXPORTER_OTLP_METRICS_ENDPOINT))
        .thenReturn("endpoint-env");
    assertEquals("endpoint-env", VariablesConverter.getMetricsEndpoint());
  }

  @Test
  public void getPropagator_Default() {
    assertEquals(VariablesConverter.DEFAULT_PROPAGATOR,
        VariablesConverter.getPropagator());
  }

  @Test
  public void getPropagator_fromSystemProperty() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_PROPAGATORS),
        "propagator-prop");
    assertEquals("propagator-prop", VariablesConverter.getPropagator());
  }

  @Test
  public void getPropagator_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_PROPAGATORS))
        .thenReturn("propagator-env");
    assertEquals("propagator-env", VariablesConverter.getPropagator());
  }

  @Test
  public void getResourceAttributes_Default() {
    assertNull(VariablesConverter.getResourceAttributes());
  }

  @Test
  public void getResourceAttributes_fromSystemProperty() {
    System
        .setProperty(toSystemProperty(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES), "key1=value1");
    assertEquals("key1=value1", VariablesConverter.getResourceAttributes());
  }

  @Test
  public void getResourceAttributes_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES))
        .thenReturn("key1=value1");
    assertEquals("key1=value1", VariablesConverter.getResourceAttributes());
  }

  @Test
  public void getHostName() {
    final String hostName = VariablesConverter.getHostName();
    assertNotNull(hostName);
    assertFalse(hostName.isEmpty());
  }

  // Make sure metrics is enabled till we *officially* support it.
  @Test
  public void getMetricsEnabled() {
    assertFalse(VariablesConverter.getMetricsEnabled());
  }

  private void mockSystem() {
    PowerMockito.mockStatic(System.class);
    Mockito.when(System.getProperty(anyString(), anyString())).thenAnswer(
        new Answer<String>() {
          @Override
          public String answer(InvocationOnMock invocationOnMock) {
            return invocationOnMock.getArgument(1);
          }
        });
  }

  private static String toSystemProperty(String variable) {
    return variable.toLowerCase().replaceAll("_", ".");
  }
}
