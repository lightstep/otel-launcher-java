package com.lightstep.opentelemetry.launcher;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Strings;
import com.lightstep.opentelemetry.launcher.OpenTelemetryConfiguration.Builder;
import java.util.List;
import org.junit.Test;

public class OpenTelemetryConfigurationTest {

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