/*
 *
 *  Copyright 2020 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.genie.common.internal.util;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class that produces a map of properties and their values if they match a given pattern.
 * The map is cached for a fixed amount of time to avoid re-computing for each call.
 *
 * @author mprimi
 * @since 4.0.0
 */
@Slf4j
public class PropertiesMapCache {

    private static final String CACHE_KEY = "properties-map";
    private final Environment environment;
    private final Pattern propertiesNamesPattern;
    private final LoadingCache<String, Map<String, String>> cache;

    /**
     * Constructor.
     *
     * @param refreshInterval        the properties snapshot refresh interval
     * @param environment            the environment
     * @param propertiesNamesPattern the properties name regex filter
     */
    public PropertiesMapCache(
        final Duration refreshInterval,
        final Environment environment,
        final Pattern propertiesNamesPattern
    ) {
        this.environment = environment;
        this.propertiesNamesPattern = propertiesNamesPattern;
        this.cache = Caffeine
            .newBuilder()
            .refreshAfterWrite(refreshInterval)
            .initialCapacity(1)
            .build(this::loadProperties);
    }

    /**
     * Get the current map of properties and their values.
     *
     * @return an immutable map of property name and property value, for any property that matches the pattern that has
     * a non-blank value
     */
    public Map<String, String> get() {
        return this.cache.get(CACHE_KEY);
    }

    private Map<String, String> loadProperties(final String propertiesKey) {
        // There's only 1 item in this cache, so key is redundant
        return PropertySourceUtils.createDynamicPropertiesMap(
            this.environment,
            this.propertiesNamesPattern
        );
    }

    /**
     * Factory class that produces {@link PropertiesMapCache} instances.
     */
    public static class Factory {
        private final Environment environment;

        /**
         * Constructor.
         *
         * @param environment the environment
         */
        public Factory(final Environment environment) {
            this.environment = environment;
        }

        /**
         * Create a {@link PropertiesMapCache} with the given refresh interval and filter pattern.
         *
         * @param refreshInterval        the refresh interval
         * @param propertiesNamesPattern the property name regex filter
         * @return a {@link PropertiesMapCache}
         */
        public PropertiesMapCache get(final Duration refreshInterval, final Pattern propertiesNamesPattern) {
            return new PropertiesMapCache(refreshInterval, environment, propertiesNamesPattern);
        }
    }
}
