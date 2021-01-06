#!/bin/bash

# To publish a build, use the Makefile
# make publish
# which will call out to this script
set -e

# Use maven-help-plugin to get the current project.version
VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\['`

echo "Publishing $VERSION"

echo "Build and deploy to Bintray (do not include the agent jar)"
mvn -s .circleci.settings.xml -Dmaven.test.skip=true deploy -pl .,common,launcher

echo "Sign the jar and other files in Bintray"
curl -H "X-GPG-PASSPHRASE:$BINTRAY_GPG_PASSPHRASE" -u $BINTRAY_USER:$BINTRAY_API_KEY -X POST "https://api.bintray.com/gpg/lightstep/maven/otel-launcher-java/versions/$VERSION"

echo "Sync the repository with Maven Central"
curl -H "Content-Type: application/json" -u $BINTRAY_USER:$BINTRAY_API_KEY -X POST -d '{"username":"'$MAVEN_CENTRAL_USER_TOKEN'","password":"'$MAVEN_CENTRAL_TOKEN_PASSWORD'","close":"1"}' "https://api.bintray.com/maven_central_sync/lightstep/maven/otel-launcher-java/versions/$VERSION"
