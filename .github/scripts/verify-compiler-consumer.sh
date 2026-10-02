#!/usr/bin/env bash
set -euo pipefail

fixture=integration/feature/destinations/shared/fixture/InvalidDestination.kt
log=$(mktemp)
trap 'rm -f "$fixture" "$log"' EXIT

cat > "$fixture" <<'KOTLIN'
package fixture
import androidx.compose.runtime.Composable
import com.pedrobneto.easy.navigation.core.annotation.Route
import com.pedrobneto.easy.navigation.core.model.NavigationRoute
class Unserializable : NavigationRoute
@Composable @Route(Unserializable::class)
fun InvalidScreen() = Unit
KOTLIN

if ./gradlew :integration:feature:compileKotlinJvm --configuration-cache --no-daemon > "$log" 2>&1; then
    cat "$log"
    echo 'Expected FIR validation to reject a route without @Serializable.' >&2
    exit 1
fi
if ! rg -F 'must be @Serializable' "$log"; then
    cat "$log"
    exit 1
fi

rm "$fixture"
./gradlew :integration:consumer:compileKotlinJvm :integration:feature:jvmTest --configuration-cache --no-daemon
if rg -l 'Unserializable' integration/feature/build/generated/easyNavigation/kotlin; then
    echo 'Removed destinations survived incremental generation.' >&2
    exit 1
fi

# Same task graph and input snapshot must be reusable after successful generation.
./gradlew :integration:consumer:compileKotlinJvm :integration:feature:jvmTest --configuration-cache --configuration-cache-problems=fail --no-daemon
