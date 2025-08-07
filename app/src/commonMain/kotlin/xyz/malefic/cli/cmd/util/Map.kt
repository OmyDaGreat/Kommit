package xyz.malefic.cli.cmd.util

/**
 * Extension function for nullable maps that retrieves the value associated with the given key.
 * If the key is not present or the map is null, it returns the provided default value.
 *
 * @param key The key whose associated value is to be returned.
 * @param default The default value to return if the key is not found or the map is null.
 * @return The value associated with the key, or the default value if the key is not found or the map is null.
 */
fun <T> Map<String, T>?.nullGet(
    key: String,
    default: T,
): T = this?.get(key) ?: default
