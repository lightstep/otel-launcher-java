# Lightstep Agent Example

## Configuration

Update config.properties with correct values for 
- _ls.access.token_
- _otel.exporter.otlp.span.endpoint_

## Build and Run

```shell script
export OTEL_RESOURCE_ATTRIBUTES=service.name=your-service-name

make buid
make run
```

