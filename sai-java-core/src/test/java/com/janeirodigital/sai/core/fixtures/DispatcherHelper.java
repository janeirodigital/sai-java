package com.janeirodigital.sai.core.fixtures;

import java.util.List;
import java.util.Map;

public class DispatcherHelper {

    public static boolean addFixture(RequestMatchingFixtureDispatcher dispatcher, List<String> fixtures, String method, String path, Map<String, List<String>> headers) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(fixtures, method, path, headers));
    }

    public static boolean mockOnGet(RequestMatchingFixtureDispatcher dispatcher, String path, List<String> fixtures) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(fixtures, "GET", path, null));
    }

    public static boolean mockOnGet(RequestMatchingFixtureDispatcher dispatcher, String path, String fixture) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(List.of(fixture), "GET", path, null));
    }

    public static boolean mockOnPut(RequestMatchingFixtureDispatcher dispatcher, String path, List<String> fixtures) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(fixtures, "PUT", path, null));
    }

    public static boolean mockOnPut(RequestMatchingFixtureDispatcher dispatcher, String path, String fixture) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(List.of(fixture), "PUT", path, null));
    }

    public static boolean mockOnPost(RequestMatchingFixtureDispatcher dispatcher, String path, List<String> fixtures) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(fixtures, "POST", path, null));
    }

    public static boolean mockOnPost(RequestMatchingFixtureDispatcher dispatcher, String path, String fixture) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(List.of(fixture), "POST", path, null));
    }

    public static boolean mockOnPatch(RequestMatchingFixtureDispatcher dispatcher, String path, List<String> fixtures) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(fixtures, "PATCH", path, null));
    }

    public static boolean mockOnPatch(RequestMatchingFixtureDispatcher dispatcher, String path, String fixture) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(List.of(fixture), "PATCH", path, null));
    }

    public static boolean mockOnDelete(RequestMatchingFixtureDispatcher dispatcher, String path, List<String> fixtures) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(fixtures, "DELETE", path, null));
    }

    public static boolean mockOnDelete(RequestMatchingFixtureDispatcher dispatcher, String path, String fixture) {
        return dispatcher.getConfiguredFixtures().add(new DispatcherEntry(List.of(fixture), "DELETE", path, null));
    }

}
