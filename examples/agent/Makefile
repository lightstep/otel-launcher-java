build:
	mvn clean package
	cd ../.. && mvn clean package -DskipTests

run:
	java -javaagent:../../agent/target/lightstep-opentelemetry-javaagent-0.1.0-SNAPSHOT.jar \
	-jar target/opentelemetry-agent-example.jar