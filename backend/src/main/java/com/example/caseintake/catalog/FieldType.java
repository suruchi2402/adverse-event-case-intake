package com.example.caseintake.catalog;

/**
 * The value types a catalog field can have. These are the only types the brief allows; a new field must use one.
 * Field ids never appear in code: everything field-specific comes from the catalog, and code branches on type only.
 */
public enum FieldType {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    DATE("date"),
    CODED("coded"),
    CODED_MULTI("coded_multi"),
    STRING_LIST("string_list");

    private final String jsonName;

    FieldType(String jsonName) {
        this.jsonName = jsonName;
    }

    public String jsonName() {
        return jsonName;
    }

    public boolean hasOptions() {
        return this == CODED || this == CODED_MULTI;
    }

    public static FieldType fromJson(String name) {
        for (FieldType type : values()) {
            if (type.jsonName.equals(name)) {
                return type;
            }
        }
        throw new CatalogException("Unknown field type '" + name + "'");
    }
}
