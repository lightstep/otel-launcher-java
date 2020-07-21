package com.lightstep.opentelemetry.exporter;

public enum Propagator {
  TRACE_CONTEXT("tracecontext"), B3("b3"), B3_SINGLE("b3single"), JAEGER("jaeger");

  private final String label;

  Propagator(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }

  public static Propagator valueOfLabel(String label) {
    for (Propagator propagator : values()) {
      if (propagator.label.equals(label)) {
        return propagator;
      }
    }
    return null;
  }

}
