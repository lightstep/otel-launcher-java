package com.lightstep.opentelemetry.launcher;

public enum Propagator {
  TRACE_CONTEXT("tracecontext"), B3_MULTI("b3multi"), B3("b3"), BAGGAGE("baggage"),
  OT_TRACE("ottrace");

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
