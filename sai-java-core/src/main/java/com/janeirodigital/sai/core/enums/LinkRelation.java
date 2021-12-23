package com.janeirodigital.sai.core.enums;

public enum LinkRelation {
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

    LinkRelation(String value) {
        this.value = value;
    }
}
