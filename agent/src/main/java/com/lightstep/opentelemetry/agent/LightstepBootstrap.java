package com.lightstep.opentelemetry.agent;

import com.lightstep.opentelemetry.common.VariablesConverter;
import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;

public class LightstepBootstrap {

  public static void premain(final String agentArgs, final Instrumentation inst) {
    try {
      VariablesConverter.convertFromEnv();
    } catch (IllegalStateException e) {
      System.err.println("Agent is not installed. " + e.getMessage());
      return;
    }

    OpenTelemetryAgent.premain(agentArgs, inst);

    if (VariablesConverter.getMetricsEnabled()) {
      // TODO: Perform any required set up for metrics in this block.
      // (We used to import the Oshi instrumentation here).
    }
  }

}
