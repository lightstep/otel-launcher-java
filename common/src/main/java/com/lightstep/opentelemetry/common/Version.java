
package com.lightstep.opentelemetry.common;

class Version {
    private static final String LAUNCHER_VERSION = "1.17.0";

    static String get() {
      return LAUNCHER_VERSION;
    }
}
