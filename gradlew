#!/bin/sh

if [ -x /usr/lib/jvm/temurin-8-jdk-amd64/bin/java ]; then
  export JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64
  export PATH="$JAVA_HOME/bin:$PATH"
fi

exec java -cp "gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
