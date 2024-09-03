package org.browser.automation.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.fluent.Request;
import org.browser.automation.exception.browser.url.InvalidUrlException;
import org.browser.automation.exception.browser.url.UrlFetchException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Utility class for checking if a URL is blacklisted.
 * <p>
 * This class provides methods to fetch a blacklist from a remote source,
 * normalize URLs, and check if a given URL is present in the blacklist.
 * The URL for fetching the blacklist is dynamically loaded from the application's configuration
 * using the Typesafe Config library. This ensures that configuration changes are
 * automatically reflected without requiring code changes.
 * </p>
 * <p>
 * The blacklist is cached in memory to avoid repeated network requests. The cache is
 * updated every time the blacklist is fetched from the remote source.
 * </p>
 */
@Slf4j
@UtilityClass
public class UrlUtil {

    // Load configuration from application.properties
    private final Config config = ConfigFactory.load();

    // URL to fetch the blacklist from, loaded from configuration
    private final String URLHAUS_API = config.getString("urlhaus.api.url");

    // Cache to store the blacklist
    private final Map<String, List<String>> blacklistCache = new ConcurrentHashMap<>();

    /**
     * Checks if the given URL is blacklisted.
     * <p>
     * This method normalizes the URL and compares it against the blacklist.
     * If {@code matchFullUrl} is true, the method checks if the full URL (normalized) is present in the blacklist.
     * If {@code matchFullUrl} is false, the method checks if the base domain (normalized) is present in the blacklist.
     * </p>
     *
     * @param url          the URL to check
     * @param matchFullUrl if true, the URL must match exactly with an entry in the blacklist;
     *                     if false, only the base domain of the URL will be checked
     * @return true if the URL is blacklisted, false otherwise
     */
    @SneakyThrows
    public boolean isUrlBlacklisted(String url, boolean matchFullUrl) {
        validateUrl(url);

        String normalizedUrl = normalizeUrl(url, matchFullUrl);
        List<String> blacklist = getBlacklist();

        return blacklist.parallelStream()
                .map(entry -> normalizeUrl(entry, matchFullUrl))
                .anyMatch(entry -> matchFullUrl ? entry.equals(normalizedUrl) : entry.equals(normalizedUrl.split("/")[0]));
    }

    /**
     * Validates the given URL to ensure it is neither null nor empty.
     * <p>
     * This method throws an {@link InvalidUrlException} if the URL is invalid.
     * </p>
     *
     * @param url the URL to validate
     * @throws InvalidUrlException if the URL is null or empty
     */
    private void validateUrl(String url) throws InvalidUrlException {
        if (url == null || url.trim().isEmpty()) {
            throw new InvalidUrlException("URL must not be null or empty.");
        }
    }

    /**
     * Retrieves the blacklist from the cache or fetches it if not cached.
     * <p>
     * This method checks if the blacklist is already cached. If not, it fetches the blacklist from
     * the remote source and caches it.
     * </p>
     *
     * @return the list of blacklisted URLs
     */
    private List<String> getBlacklist() {
        return blacklistCache.computeIfAbsent(URLHAUS_API, key -> {
            try {
                return fetchBlacklist().orElseThrow(() -> new UrlFetchException(URLHAUS_API, "Blacklist is empty"));
            } catch (UrlFetchException e) {
                log.error("Failed to fetch or parse the blacklist from URL: {}", URLHAUS_API, e);
                throw new RuntimeException("Failed to fetch blacklist", e);
            }
        });
    }

    /**
     * Fetches the blacklist from the remote source.
     * <p>
     * This method sends an HTTP GET request to the URL specified by {@code URLHAUS_API} to retrieve
     * the blacklist data. The data is processed to remove empty lines and comments, and then split
     * into a list of blacklisted URLs. The fetched blacklist is cached to reduce the number of network requests.
     * </p>
     *
     * @return an Optional containing the list of blacklisted URLs, or an empty Optional if an error occurs
     * @throws UrlFetchException if there is an error fetching or parsing the blacklist
     */
    private Optional<List<String>> fetchBlacklist() throws UrlFetchException {
        try {
            String response = Request.get(URLHAUS_API).execute().returnContent().asString();
            return Optional.of(Arrays.stream(response.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("Failed to fetch or parse the blacklist from URL: {}", URLHAUS_API, e);
            throw new UrlFetchException(URLHAUS_API, e.getMessage());
        }
    }

    /**
     * Normalizes a URL by removing the protocol and optional "www." prefix.
     * Optionally truncates the URL to the base domain if {@code matchFullUrl} is false.
     * <p>
     * For example, the URL "<a href="https://www.example.com/path">...</a>" would be normalized to "example.com/path"
     * if {@code matchFullUrl} is true, and to "example.com" if {@code matchFullUrl} is false.
     * </p>
     *
     * @param url          the URL to normalize
     * @param matchFullUrl if true, returns the full normalized URL;
     *                     if false, truncates the URL at the first '/'
     * @return the normalized URL
     */
    private String normalizeUrl(String url, boolean matchFullUrl) {
        String normalizedUrl = url.replaceAll("^(https?://)?(www\\.)?", "");
        return matchFullUrl ? normalizedUrl : normalizedUrl.split("/")[0];
    }
}