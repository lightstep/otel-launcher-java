# Lightstep Agent Example

## Build and Run
```shell script
export LS_ACCESS_TOKEN=your-token
export OTEL_RESOURCE_ATTRIBUTES=service.name=your-service-name
export OTEL_EXPORTER_OTLP_SPAN_ENDPOINT=ingest.staging.lightstep.com

make buid
make run
```

