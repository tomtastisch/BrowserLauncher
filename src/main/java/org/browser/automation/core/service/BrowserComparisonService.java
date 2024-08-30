package org.browser.automation.core.service;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.browser.automation.core.functional.BrowserComparison;
import org.browser.automation.exception.WebDriverComparisonException;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Utility class for managing and combining multiple {@link BrowserComparison} instances.
 *
 * <p>
 * The purpose of this class is to facilitate the comparison of browser behaviors
 * by allowing multiple {@link BrowserComparison} objects to be composed together.
 * This is particularly useful in scenarios where multiple criteria or checks need
 * to be applied sequentially across a set of {@link WebDriver} instances.
 * </p>
 *
 * <p>
 * The {@code and} method allows you to chain multiple {@link BrowserComparison}
 * objects, ensuring that all specified comparisons are applied to a list of {@link WebDriver}
 * instances. The {@link CompositeBrowserComparison} class is a container that
 * aggregates multiple {@link BrowserComparison} instances and applies them in sequence.
 * </p>
 *
 * <p>
 * The comparisons are executed in a thread-safe manner, leveraging the {@link CopyOnWriteArrayList}
 * to ensure safe operations in concurrent environments. The {@link SneakyThrows} annotation
 * is used to propagate checked exceptions thrown by the comparisons without explicit
 * try-catch blocks, simplifying the code structure.
 * </p>
 */
@Slf4j
@UtilityClass
public class BrowserComparisonService {

    /**
     * Combines multiple {@link BrowserComparison} instances into a single comparison.
     *
     * <p>
     * This method allows you to pass any number of {@link BrowserComparison} instances,
     * which will be applied sequentially to the provided list of {@link WebDriver} instances.
     * If any of the comparisons throws a {@link WebDriverComparisonException}, it will be logged.
     * </p>
     *
     * @param comparisons The {@link BrowserComparison} instances to be combined.
     * @return A combined {@link BrowserComparison} that applies all provided comparisons.
     */
    public static BrowserComparison and(BrowserComparison... comparisons) {
        return drivers -> Arrays.stream(comparisons).forEach(comparison -> {
            try {
                comparison.apply(drivers);
            } catch (WebDriverComparisonException e) {
                log.error(e.getMessage(), StringUtils.join(drivers, ", "));
            }
        });
    }

    /**
     * A composite {@link BrowserComparison} that aggregates multiple {@link BrowserComparison} instances.
     *
     * <p>
     * This class allows for the aggregation of multiple comparisons into a single comparison operation.
     * The comparisons are stored in a thread-safe {@link CopyOnWriteArrayList}, which ensures that
     * modifications to the list are safe in concurrent environments.
     * </p>
     */
    @Getter
    @ToString
    public static class CompositeBrowserComparison implements BrowserComparison {
        private final List<BrowserComparison> comparisons = new CopyOnWriteArrayList<>();

        /**
         * Constructs a {@link CompositeBrowserComparison} from the given {@link BrowserComparison} instances.
         *
         * <p>
         * If any of the provided comparisons is itself a {@link CompositeBrowserComparison},
         * its constituent comparisons are flattened into the current instance, ensuring a
         * single-level structure of comparisons.
         * </p>
         *
         * @param comparisons The {@link BrowserComparison} instances to be aggregated.
         */
        public CompositeBrowserComparison(BrowserComparison... comparisons) {
            Arrays.stream(comparisons).forEach(comparison -> {
                if (comparison instanceof CompositeBrowserComparison) {
                    this.comparisons.addAll(((CompositeBrowserComparison) comparison).getComparisons());
                } else {
                    this.comparisons.add(comparison);
                }
            });
        }

        /**
         * Applies all aggregated {@link BrowserComparison} instances to the provided list of {@link WebDriver} instances.
         *
         * <p>
         * This method sequentially applies each {@link BrowserComparison} in the list to the
         * provided {@link WebDriver} instances. If any comparison throws a checked exception,
         * it will be propagated without the need for explicit handling, thanks to the {@link SneakyThrows} annotation.
         * </p>
         *
         * @param drivers The list of {@link WebDriver} instances to which the comparisons will be applied.
         */
        @Override
        @SneakyThrows
        public void apply(List<WebDriver> drivers) {
            for (BrowserComparison comparison : comparisons) {
                comparison.apply(drivers);
            }
        }
    }
}