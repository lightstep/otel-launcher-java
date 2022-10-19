#!/bin/bash

# To increment the version, use the Makefile
# make publish
# which will call out to this script
set -e

# File containing the launcher version (internal usage, avoid funky Maven version fetching)
VERSION_FILE=common/src/main/java/com/lightstep/opentelemetry/common/Version.java

if [ "$#" -lt 1 ]; then
        echo "Increasing minor version automatically"

        # Use maven-help-plugin to get the current project.version
        CURRENT_VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\['`
        # Increment the minor version
        NEW_VERSION="${CURRENT_VERSION%.*}.$((${CURRENT_VERSION##*.}+1))"
else
	NEW_VERSION=$1
fi

# Create a branch with the version bump.
NEW_VERSION_BRANCH="v${NEW_VERSION}_bump"
git checkout -b $NEW_VERSION_BRANCH

# Use maven-help-plugin to update the project.version
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false

# Update the version file (internal usage, avoid funky Maven version fetching)
perl -pi -e 's/(\d+)\.(\d+)\.(\d+)/$ENV{NEW_VERSION}/ge' $VERSION_FILE

# Commit the changes
git add $VERSION_FILE
git add agent/pom.xml
git add common/pom.xml
git add launcher/pom.xml
git add pom.xml

git commit -m "VERSION $NEW_VERSION"
git push --set-upstream origin $NEW_VERSION_BRANCH
