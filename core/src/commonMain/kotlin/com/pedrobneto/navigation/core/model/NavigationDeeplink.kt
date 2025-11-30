package com.pedrobneto.navigation.core.model

import br.com.arch.toolkit.lumber.Lumber
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Represents a navigation deeplink, providing structured access to its components.
 *
 * This class parses a raw deeplink string and breaks it down into its constituent parts:
 * `scheme`, `host`, `path`, and `queryParams`. It also provides functionality to resolve this
 * deeplink to a specific [NavigationRoute].
 *
 * The deeplink format is expected to be `scheme://host/path?query=param`.
 * A minimal format of `/{host}` is also supported.
 *
 * Example of a valid deeplink: `nav://home/details/123?source=notification`
 * - `scheme`: "nav"
 * - `host`: "home"
 * - `path`: "details/123"
 * - `queryParams`: `{"source":"notification"}`
 *
 * Path parameters are also supported, e.g., `/user/{userId}`.
 *
 * @param raw The raw deeplink string.
 * @throws IllegalStateException if the `raw` deeplink string is malformed and a host cannot be extracted.
 */
@JvmInline
@Serializable
value class NavigationDeeplink(val raw: String) {

    private val regex: Regex get() = Regex("(((?<scheme>.+):/)*)/(?<host>[^/?]+)((?<path>/[^?]+)*)((\\?(?<queryParams>.+))*)")

    private val groups: MatchGroupCollection
        get() = regex
            .find(raw)
            ?.groups
            ?: error(
                "Malformed deeplink '$raw'. " +
                        "Scheme and path are optional but there must be a host. " +
                        "Full uri example: {scheme}://{host}/{path}. " +
                        "Minimum required: /{host}"
            )

    /** The scheme of the deeplink (e.g., "https", "nav"). Null if not present. */
    val scheme: String? get() = groups["scheme"]?.value

    /** The host of the deeplink. This part is mandatory. */
    val host: String get() = groups["host"]?.value ?: error("Host not found.")

    /** The path of the deeplink, which may contain path parameters like `{userId}`. Null if not present. */
    val path: String? get() = groups["path"]?.value

    /** A map of the query parameters from the deeplink. */
    val queryParams: Map<String, String>
        get() = groups["queryParams"]
            ?.value
            ?.substringAfter('?')
            ?.split("&")
            ?.associate {
                val (key, value) = it.split("=")
                key to value
            }
            ?.toMap()
            .orEmpty()

    /** The "clean" version of the deeplink, stripped of its scheme and query parameters. */
    val clean: String
        get() = raw.split(":/").last().split("?").first()

    init {
        if (!raw.matches(regex)) throw IllegalArgumentException("Malformed deeplink '$raw'")
    }

    /**
     * Resolves the deeplink to a [NavigationRoute].
     *
     * This function searches through the provided `directions` to find a [NavigationDirection]
     * whose deeplink pattern matches this one. It then extracts path and query parameters
     * to construct and return the corresponding [NavigationRoute] instance.
     *
     * @param json The [Json] instance used for deserializing route arguments.
     * @param directions A list of all available [NavigationDirection]s in the app.
     * @return The resolved [NavigationRoute].
     * @throws IllegalArgumentException if no matching direction is found for the deeplink or
     * if an error occurs during the deserialization of route parameters.
     */
    @OptIn(InternalSerializationApi::class)
    @Throws(IllegalArgumentException::class)
    fun resolve(json: Json, directions: List<NavigationDirection>): NavigationRoute {
        var result: Pair<NavigationDeeplink, KClass<out NavigationRoute>>? = null

        directionsLoop@ for (direction in directions) {
            direction.deeplinks.forEach { directionDeeplink ->
                if (matches(directionDeeplink)) {
                    result = directionDeeplink to direction.routeClass
                    break@directionsLoop
                }
            }
        }

        val (directionDeeplink, routeClass) = result
            ?: throw IllegalArgumentException("No route found for deeplink '$raw'")

        val parametersMap = mutableMapOf("deeplink" to raw)

        val pathValues = path?.split("/")
        directionDeeplink.path?.split("/")?.forEachIndexed { index, label ->
            if (label.startsWith("{") && label.endsWith("}")) {
                val key = label.removePrefix("{").removeSuffix("}")
                pathValues?.getOrNull(index)?.let { parametersMap[key] = it }
            }
        }

        parametersMap.putAll(queryParams)

        return json.runCatching {
            decodeFromString(routeClass.serializer(), encodeToString(parametersMap))
        }.getOrElse {
            Lumber.tag("NavigationController")
                .error(it, "Error decoding route parameters")
            throw it
        }
    }

    private fun matches(other: NavigationDeeplink): Boolean = this == other ||
            clean == other.clean ||
            other.clean.replace("\\{[^}]+\\}".toRegex(), "[^/]+")
                .toRegex()
                .matches(clean)
}
