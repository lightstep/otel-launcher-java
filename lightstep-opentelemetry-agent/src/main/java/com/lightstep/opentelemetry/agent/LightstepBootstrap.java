package com.lightstep.opentelemetry.agent;

import com.lightstep.opentelemetry.common.VariablesConverter;
import io.opentelemetry.auto.bootstrap.AgentBootstrap;
import java.lang.instrument.Instrumentation;

public class LightstepBootstrap {

  public static void premain(final String agentArgs, final Instrumentation inst) {
    try {
      VariablesConverter.convertFromEnv();
    } catch (IllegalStateException e) {
      System.err.println("Agent is not installed. " + e.getMessage());
      return;
    }
    AgentBootstrap.premain(agentArgs, inst);
  }

}
