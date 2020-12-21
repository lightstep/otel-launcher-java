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
      try {
        Class<?> oshiSystemInfoClass =
            ClassLoader.getSystemClassLoader()
                .loadClass("io.opentelemetry.instrumentation.oshi.SystemMetrics");
        Method getCurrentPlatformEnumMethod = oshiSystemInfoClass.getMethod("registerObservers");
        getCurrentPlatformEnumMethod.invoke(null);
      } catch (Throwable ex) {
        ex.printStackTrace();
      }
    }
  }

}
