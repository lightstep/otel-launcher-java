[![Download](https://img.shields.io/maven-central/v/com.lightstep.opentelemetry/opentelemetry-launcher.svg)](http://search.maven.org/#search%7Cga%7C1%7Ccom.lightstep.opentelemetry%20opentelemetry-launcher) [![Circle CI](https://circleci.com/gh/lightstep/otel-launcher-java.svg?style=shield)](https://circleci.com/gh/lightstep/otel-launcher-java) [![Apache-2.0 license](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Lightstep Distro for OpenTelemetry Java [Deprecated]

In August 2023, [Lightstep became ServiceNow
Cloud](https://docs.lightstep.com/docs/banner-faq) Observability. To ease the
transition, all code artifacts will continue to use the Lightstep name. You
don't need to do anything to keep using this repository.

This is the Lightstep package for configuring OpenTelemetry

## Agent
The Lightstep OpenTelemetry Agent is a configuration layer over OpenTelemetry Instrumentation Agent.
Download the [latest version](https://github.com/lightstep/otel-launcher-java/releases/latest/download/lightstep-opentelemetry-javaagent.jar)
of `lightstep-opentelemetry-javaagent.jar`.

### Run

The instrumentation agent is enabled using the -javaagent flag to the JVM.
Configuration parameters are passed as Java system properties (-D flags) or 
as environment variables. [Full list of supported parameters](#system-properties-and-environmental-variables).

#### Configuration via Java system properties

```shell script
export LS_ACCESS_TOKEN=your-token

java -javaagent:path/to/lightstep-opentelemetry-javaagent.jar \
     -Dotel.service.name=your-service-name
     -Dotel.exporter.otlp.traces.endpoint=https://ingest.lightstep.com:443 \
     -jar myapp.jar
```

#### Configuration via environment variables

```shell script
export LS_ACCESS_TOKEN=your-token
export OTEL_SERVICE_NAME=your-service-name
export OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=https://ingest.lightstep.com:443

java -javaagent:path/to/lightstep-opentelemetry-javaagent.jar \
     -jar myapp.jar
```

Observe that system properties have higher priority than environment variables.

## Launcher

The Lightstep OpenTelemetry Launcher is a configuration layer over OpenTelemetry OTLP trace exporter.

### Installation

pom.xml

```xml
<dependency>
    <groupId>com.lightstep.opentelemetry</groupId>
    <artifactId>opentelemetry-launcher</artifactId>
    <version>VERSION</version>
</dependency>
```

### Usage

#### Easy initialization

```java
// Installs exporter into tracer SDK default provider with batching span processor.
OpenTelemetryConfiguration.newBuilder()
                      .setServiceName("{service_name}")
                      .setAccessToken("{your_access_token}")
                      .setTracesEndpoint("{lightstep_host}")
                      .install();

// Get tracer
Tracer tracer = GlobalOpenTelemetry.getTracer("instrumentation-library-name", "1.0.0");
```

#### Manual configuration

```java
// Create builder
Builder builder = OpenTelemetryConfiguration.newBuilder()
                      .setServiceName("{service_name}")
                      .setAccessToken("{your_access_token}")
                      .setTracesEndpoint("{lightstep_host}");

// Instantiate openTelemetry
OpenTelemetry openTelemetry = builder.buildOpenTelemetry().getOpenTelemetrySdk();

// Get tracer
Tracer tracer = openTelemetry.get("instrumentation-library-name", "1.0.0");
```

### Logging

It uses _java.util.logging_ therefore logging properties file can be specified via system property 
_java.util.logging.config.file_. E.g. `-Djava.util.logging.config.file=path/to/logging.properties`

## System properties and environmental variables
Supported system properties and environmental variables:

| System property                    | Environment variable               | Purpose                                                                           | Default              | 
|------------------------------------|------------------------------------|-----------------------------------------------------------------------------------|----------------------|       
| otel.service.name                  | OTEL_SERVICE_NAME                  | Service name                                                                      |                      |
| ls.service.version                 | LS_SERVICE_VERSION                 | Service version                                                                   |                      |                        
| ls.access.token                    | LS_ACCESS_TOKEN                    | Token for Lightstep access                                                        |                      |                        
| otel.exporter.otlp.traces.endpoint | OTEL_EXPORTER_OTLP_TRACES_ENDPOINT | Satellite URL, should start with _http://_ or _https://_                          | https://ingest.lightstep.com:443 |
| otel.exporter.otlp.metrics.endpoint| OTEL_EXPORTER_OTLP_METRICS_ENDPOINT| Satellite URL, should start with _http://_ or _https://_                          | https://ingest.lightstep.com:443 |
| otel.propagators                   | OTEL_PROPAGATORS                   | Propagator                                                                        | b3multi              |
| otel.log.level                     | OTEL_LOG_LEVEL                     | Log level for agent, to see more messages set to _debug_, to disable set to _off_ | info                 |
| otel.resource.attributes           | OTEL_RESOURCE_ATTRIBUTES           | Comma separated key-value pairs                                                   |                      |
| otel.exporter.otlp.metrics.temporality.preference | OTEL_EXPORTER_OTLP_METRICS_TEMPORALITY_PREFERENCE | Metrics aggregation temporality                     | cumulative           |
| ls.metrics.enabled                 | LS_METRICS_ENABLED                 | Enable or disable metrics                                                         | false                |

## Deprecated properties and environmental variables

| System property                    | Environment variable             |
|------------------------------------|----------------------------------|
| otel.exporter.otlp.span.insecure   | OTEL_EXPORTER_OTLP_SPAN_INSECURE |
| otel.exporter.otlp.span.endpoint   | OTEL_EXPORTER_OTLP_SPAN_ENDPOINT |
| ls.service.name                    | LS_SERVICE_NAME                  |

## OpenTelemetry Metrics support

Metrics support is currently **experimental** and it is disabled by default. It can be enabled via `LS_METRICS_ENABLED=true`.
Breaking changes may still occur. Use at your own risk.

## License

[Apache 2.0 License](./LICENSE).
