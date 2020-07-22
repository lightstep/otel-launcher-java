build:
	mvn clean package
	cd ../.. && mvn clean package -DskipTests

run:
	java -javaagent:../../agent/target/lightstep-opentelemetry-auto-0.1.0-SNAPSHOT.jar \
	-jar target/launcher-agent-example.jar