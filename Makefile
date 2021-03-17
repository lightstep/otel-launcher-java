.PHONY: build publish clean test inc-version

build: test
	mvn package

clean:
	mvn clean

test:
	mvn test

inc-version:
	./inc-version.sh

# Needed only in CircleCI to FORCE the help plugin to be installed (resolve-plugins is NOT enough).
resolve_plugins:
	mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version

# If no version is specified, the minor version will be automatically increased.
inc-version:
	./inc-version.sh $(NEW_VERSION)

# See https://bintray.com/lightstep for published artifacts
# You must have the following entry in your settings.xml of your .m2 directory
# This matches the distributionManagement/repository defined in the pom.xml
#
#    <server>
#        <id>lightstep-bintray</id>
#        <username>xxx</username>
#        <password>xxx</password>
#    </server>
#
publish: build resolve_plugins
	@test -n "$$SONATYPE_USERNAME" || (echo "SONATYPE_USERNAME must be defined to publish" && false)
	@test -n "$$SONATYPE_PASSWORD" || (echo "SONATYPE_PASSWORD must be defined to publish" && false)
	@test -n "$$GPG_KEY_NAME" || (echo "GPG_KEY_NAME must be defined to publish" && false)

	@git diff-index --quiet HEAD || (echo "git has uncommitted changes. Refusing to publish." && false)
	./deploy.sh
