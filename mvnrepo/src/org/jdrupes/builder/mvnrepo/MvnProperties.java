package org.jdrupes.builder.mvnrepo;

import org.jdrupes.builder.api.PropertyKey;

/// Additional properties used with maven repositories.
///
public enum MvnProperties implements PropertyKey {

    /// The group that the project's artifacts belong to.
    @SuppressWarnings("PMD.FieldNamingConventions")
    Group("");
    
    private final Object defaultValue;

    <T> MvnProperties(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T defaultValue() {
        return (T)defaultValue;
    }
}