package com.lightstep.opentelemetry.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class PropagatorTest {

  @Test
  public void valueOfLabel() {
    assertEquals(Propagator.B3, Propagator.valueOfLabel("b3"));

    assertNull(Propagator.valueOfLabel("null"));
  }

}
