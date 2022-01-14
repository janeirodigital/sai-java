package com.janeirodigital.sai.core.fixtures;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter @AllArgsConstructor
public class DispatcherEntry {
    private List<String> fixtureNames;
    private String expectedMethod;
    private String expectedPath;
    private Map<String, List<String>> expectedHeaders;

    @Override
    public String toString() {
        return "DispatcherEntry{" +
                fixtureNames +
                ":" + expectedMethod + '\'' +
                " " + expectedPath + '\'' +
                " " + expectedHeaders +
                "}";
    }
}
