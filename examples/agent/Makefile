build:
	mvn clean package
	cd ../.. && mvn clean package -DskipTests

run:
	java -javaagent:../../agent/target/lightstep-opentelemetry-javaagent-0.14.0.jar \
	-jar target/opentelemetry-agent-example.jar