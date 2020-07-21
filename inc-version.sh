#!/bin/bash

# To increment the version, use the Makefile
# make publish
# which will call out to this script

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

# Use maven-help-plugin to get the current project.version
CURRENT_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\[')

# Increment the minor version
NEW_VERSION="${CURRENT_VERSION%.*}.$((${CURRENT_VERSION##*.} + 1))"

# Use maven-help-plugin to update the project.version
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false

# Add and commit the changes
git add lightstep-opentelemetry-agent/pom.xml
git add lightstep-opentelemetry-common/pom.xml
git add lightstep-opentelemetry-exporter/pom.xml
git add pom.xml

git commit -m "VERSION $NEW_VERSION"
git push --set-upstream origin $CURRENT_BRANCH
