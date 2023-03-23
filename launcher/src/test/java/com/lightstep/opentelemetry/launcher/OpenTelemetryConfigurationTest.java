package com.lightstep.opentelemetry.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Strings;
import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration.Builder;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.GlobalLoggerProvider;
import io.opentelemetry.api.events.GlobalEventEmitterProvider;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class OpenTelemetryConfigurationTest {

  @Before
  public void before() {
    GlobalOpenTelemetry.resetForTest();
    // Need to reset GlobalLoggerProvider/GlobalEventEmitterProvider as
    // they are not part of the main API yet
    // BUT it is used/set by default using the autoconfiguration artifact.
    GlobalLoggerProvider.resetForTest();
    GlobalEventEmitterProvider.resetForTest();
    System.clearProperty("otel.propagators");
  }

  @Test
  public void testInstall() {
    OpenTelemetryConfiguration.newBuilder()
        .setServiceName("service-name")
        .setAccessToken(Strings.repeat("x", 32))
        .install();

    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();
    TextMapPropagator propagator = openTelemetry.getPropagators()
        .getTextMapPropagator();
    assertThat(propagator).isInstanceOf(B3Propagator.class);
  }

  @Test
  public void testPropagators_Default() {
    Builder builder = OpenTelemetryConfiguration.newBuilder()
        .setServiceName("service-name")
        .setAccessToken(Strings.repeat("x", 32));
    builder.buildOpenTelemetry();
    List<Propagator> propagators = builder.getPropagators();
    assertThat(propagators).hasSize(1);
    assertThat(propagators.get(0)).isEqualTo(Propagator.B3_MULTI);
  }

  @Test
  public void testPropagators_NotDefault() {
    Builder builder = OpenTelemetryConfiguration.newBuilder()
        .setServiceName("service-name")
        .setAccessToken(Strings.repeat("x", 32))
        .setPropagator(Propagator.TRACE_CONTEXT);
    builder.buildOpenTelemetry();
    List<Propagator> propagators = builder.getPropagators();
    assertThat(propagators).hasSize(1);
    assertThat(propagators.get(0)).isEqualTo(Propagator.TRACE_CONTEXT);
  }

  @Test
  public void testPropagators_Multi() {
    Builder builder = OpenTelemetryConfiguration.newBuilder()
        .setServiceName("service-name")
        .setAccessToken(Strings.repeat("x", 32))
        .setPropagator(Propagator.TRACE_CONTEXT)
        .setPropagator(Propagator.B3_MULTI);

    builder.buildOpenTelemetry();
    List<Propagator> propagators = builder.getPropagators();
    assertThat(propagators).hasSize(2);
    assertThat(propagators).containsOnly(Propagator.TRACE_CONTEXT, Propagator.B3_MULTI);
  }

}
