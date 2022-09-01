# Lightstep Launcher with OpenTracing Shim Example

This example shows to to configure the Lightstep OTel Launcher along the
OpenTracing Shim layer, in order to easily report OTel traces/spans from
codebases already instrumented using the OpenTracing API.

## Configuration

Update [config.properties](./config.properties) file with correct values for 
- _otel.service.name_
- _ls.access.token_
- _otel.exporter.otlp.traces.endpoint_

## Build and Run

```shell script
make buid
make run
```

## Dependencies

A dependency to the [OpenTracing Shim](https://github.com/open-telemetry/opentelemetry-java/tree/main/opentracing-shim)
needs to added:

```xml
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-opentracing-shim</artifactId>
  <version>OTEL-JAVA-VERSION</version>
</dependency>
```

