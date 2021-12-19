package com.janeirodigital.sai.core.enums;

public enum LinkRelations {
    DESCRIBED_BY("describedby"),
    FOCUS_NODE("http://www.w3.org/ns/shapetrees#FocusNode"),
    MANAGED_BY("http://www.w3.org/ns/shapetrees#managedBy"),
    MANAGES("http://www.w3.org/ns/shapetrees#manages"),
    TARGET_SHAPETREE("http://www.w3.org/ns/shapetrees#TargetShapeTree"),
    TYPE("type"),
    ACL("acl");

    private final String value;

    public String getValue() {
        return this.value;
    }

    LinkRelations(String value) {
        this.value = value;
    }
}
