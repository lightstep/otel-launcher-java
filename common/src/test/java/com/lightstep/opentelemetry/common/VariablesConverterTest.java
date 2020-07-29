package com.lightstep.opentelemetry.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_LOG_LEVEL));
    System.clearProperty(toSystemProperty(VariablesConverter.LS_ACCESS_TOKEN));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_ENDPOINT));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_PROPAGATORS));
    System.clearProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_INSECURE));
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
        .isTokenRequired(VariablesConverter.DEFAULT_OTEL_EXPORTER_OTLP_SPAN_ENDPOINT));

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
  public void getAccessToken_Default() {
    assertEquals("", VariablesConverter.getAccessToken());
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
  public void getSpanEndpoint_Default() {
    assertEquals(VariablesConverter.DEFAULT_OTEL_EXPORTER_OTLP_SPAN_ENDPOINT,
        VariablesConverter.getSpanEndpoint());
  }

  @Test
  public void getSpanEndpoint_fromSystemProperty() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_ENDPOINT),
        "endpoint-prop");
    assertEquals("endpoint-prop", VariablesConverter.getSpanEndpoint());
  }

  @Test
  public void getSpanEndpoint_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_ENDPOINT))
        .thenReturn("endpoint-env");
    assertEquals("endpoint-env", VariablesConverter.getSpanEndpoint());
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
  public void useInsecureTransport_Default() {
    assertEquals(VariablesConverter.DEFAULT_OTEL_EXPORTER_OTLP_SPAN_INSECURE,
        VariablesConverter.useInsecureTransport());
  }

  @Test
  public void useInsecureTransport_fromSystemProperty() {
    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_INSECURE),
        "true");
    assertTrue(VariablesConverter.useInsecureTransport());

    System.setProperty(toSystemProperty(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_INSECURE),
        "false");
    assertFalse(VariablesConverter.useInsecureTransport());
  }

  @Test
  public void useInsecureTransport_fromEnvVariable() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_EXPORTER_OTLP_SPAN_INSECURE))
        .thenReturn("true");
    assertTrue(VariablesConverter.useInsecureTransport());
  }

  @Test
  public void hasServiceName_noEqualSign() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES))
        .thenReturn("service.name");
    assertFalse(VariablesConverter.hasServiceName());
  }

  @Test
  public void hasServiceName_null() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES))
        .thenReturn(null);
    assertFalse(VariablesConverter.hasServiceName());
  }

  @Test
  public void hasServiceName_empty() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES))
        .thenReturn("");
    assertFalse(VariablesConverter.hasServiceName());
  }

  @Test
  public void hasServiceName_noValue() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES))
        .thenReturn("service.name=");
    assertFalse(VariablesConverter.hasServiceName());
  }

  @Test
  public void hasServiceName() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES))
        .thenReturn("service.name=test");
    assertTrue(VariablesConverter.hasServiceName());
  }

  @Test
  public void hasServiceName_spaces() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES))
        .thenReturn("service.name  =test");
    assertTrue(VariablesConverter.hasServiceName());
  }

  @Test
  public void hasServiceName_moreSpaces() {
    mockSystem();
    Mockito.when(System.getenv(VariablesConverter.OTEL_RESOURCE_ATTRIBUTES))
        .thenReturn("service.name  =  test");
    assertTrue(VariablesConverter.hasServiceName());
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