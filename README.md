# Lightstep OpenTelemetry Configuration Layer for Java

_NOTE: the code in this repo is currently in alpha and will likely change_

This is the Lightstep package for configuring OpenTelemetry

## Configuration

### Service Name

It's required to define service name.  
Currently, it's possible only via environment variable `OTEL_RESOURCE_ATTRIBUTES`

```shell script
export OTEL_RESOURCE_ATTRIBUTES=service.name=test
```

###  System properties and environmental variables
Supported system properties and environmental variables:

| System property             | Environment variable         | Purpose                                                                                               | Default              | 
|-----------------------------|------------------------------|-------------------------------------------------------------------------------------------------------|----------------------|       
| ls.satellite.url            | LS_SATELLITE_URL             | Satellite URL                                                                                         | ingest.lightstep.com |
| ls.access.token             | LS_ACCESS_TOKEN              | Token for Lightstep access                                                                            |                      |                        
| ls.deadline.millis          | LS_DEADLINE_MILLIS           | Maximum amount of time the tracer should wait for a response from the collector when sending a report | 30000                |
| ls.use.tls                  | LS_USE_TLS                   | use TLS or not                                                                                        | true                 |
| otel.propagators            | OTEL_PROPAGATORS             | Propagator                                                                                            | b3                   |

## Agent
The Lightstep OpenTelemetry Agent is a configuration layer over OpenTelemetry Instrumentation Agent.


### Usage

#### Run

java -javaagent:path/to/target/lightstep-opentelemetry-auto-<version>.jar \
     -jar myapp.jar


## Exporter

The Lightstep OpenTelemetry Exporter is a configuration layer over OpenTelemetry OTLP trace exporter.

### Installation

pom.xml

```xml
<dependency>
    <groupId>com.lightstep.opentelemetry</groupId>
    <artifactId>lightstep-opentelemetry-exporter</artifactId>
    <version>VERSION</version>
</dependency>
```

### Usage

#### Manual configuration

```java
// Create builder
Builder builder = LightstepExporter.newBuilder()
                      .setAccessToken("{your_access_token}")
                      .setSatelliteUrl("{lightstep_host}");

// Instantiate the otlp exporter
OtlpGrpcSpanExporter exporter = builder.build();

// Add Span Processor with Lightstep exporter
OpenTelemetrySdk.getTracerProvider()
                      .addSpanProcessor(SimpleSpanProcessor.newBuilder(exporter).build());

// Get tracer
Tracer tracer = OpenTelemetry.getTracer("instrumentation-library-name","1.0.0");
```

#### Configuration from system properties and environmental variables

Lightstep exporter can be configured by system properties and environmental variables:

```java
Builder builder = LightstepExporter.Builder.fromEnv();
```



#### Easy initialization

```java
// Installs exporter into tracer SDK default provider with batching span processor.
builder.install();
```


## License

[Apache 2.0 License](./LICENSE).