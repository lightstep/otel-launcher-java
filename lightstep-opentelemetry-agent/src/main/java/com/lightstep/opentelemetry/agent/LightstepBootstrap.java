package com.lightstep.opentelemetry.agent;

import com.lightstep.opentelemetry.common.VariablesConverter;
import io.opentelemetry.auto.bootstrap.AgentBootstrap;
import java.lang.instrument.Instrumentation;

public class LightstepBootstrap {

  public static void premain(final String agentArgs, final Instrumentation inst) {
    VariablesConverter.convertFromEnv();
    AgentBootstrap.premain(agentArgs, inst);
  }

}
