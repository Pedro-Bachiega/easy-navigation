#!/usr/bin/env bash
set -euo pipefail

./gradlew \
    :compiler-plugin:publishAllPublicationsToConsumerRepository \
    :easy-navigation-gradle-plugin:publishAllPublicationsToConsumerRepository \
    :test:publishKotlinMultiplatformPublicationToConsumerRepository \
    :test:publishJvmPublicationToConsumerRepository \
    :core:publishKotlinMultiplatformPublicationToConsumerRepository \
    :core:publishJvmPublicationToConsumerRepository \
    :integration:feature:publishKotlinMultiplatformPublicationToConsumerRepository \
    :integration:feature:publishJvmPublicationToConsumerRepository \
    -PVERSION_NAME=0.0.1-compiler-ci -PsignAllPublications=false --no-daemon

./gradlew -p integration/published-consumer \
    compileCommonMainKotlinMetadata compileKotlinJvm \
    -PverificationRepository="$PWD/build/consumer-repository" \
    --configuration-cache --configuration-cache-problems=fail --no-daemon
