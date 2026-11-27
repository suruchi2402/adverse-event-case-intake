package com.example.caseintake.extraction;

import com.example.caseintake.catalog.Catalog;
import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.result.Confidence;
import com.example.caseintake.result.FieldStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.example.caseintake.extraction.ModelOutputKeys.CASES;
import static com.example.caseintake.extraction.ModelOutputKeys.CONFIDENCE;
import static com.example.caseintake.extraction.ModelOutputKeys.CRITERIA;
import static com.example.caseintake.extraction.ModelOutputKeys.FIELDS;
import static com.example.caseintake.extraction.ModelOutputKeys.FLAGS;
import static com.example.caseintake.extraction.ModelOutputKeys.LABEL;
import static com.example.caseintake.extraction.ModelOutputKeys.NOTE;
import static com.example.caseintake.extraction.ModelOutputKeys.PRESENT;
import static com.example.caseintake.extraction.ModelOutputKeys.QUOTE;
import static com.example.caseintake.extraction.ModelOutputKeys.QUOTES;
import static com.example.caseintake.extraction.ModelOutputKeys.STATUS;
import static com.example.caseintake.extraction.ModelOutputKeys.VALUE;

/**
 * The structured-output schema for the model (the OpenAPI subset Gemini's responseSchema accepts), built from the
 * catalog: one property per field, value typed by the field type, codes as enums. Every field is required, so the
 * model cannot silently skip one. The schema narrows what the model can say; the pipeline still validates everything.
 */
public final class ResponseSchemaBuilder {

    private static final String TYPE = "type";
    private static final String OBJECT = "OBJECT";
    private static final String ARRAY = "ARRAY";
    private static final String STRING = "STRING";
    private static final String BOOLEAN = "BOOLEAN";
    private static final String INTEGER = "INTEGER";
    private static final String NUMBER = "NUMBER";

    private final Catalog catalog;

    public ResponseSchemaBuilder(Catalog catalog) {
        this.catalog = catalog;
    }

    public Map<String, Object> build() {
        Map<String, Object> caseProps = new LinkedHashMap<>();
        caseProps.put(LABEL, Map.of(TYPE, STRING));
        caseProps.put(CRITERIA, criteriaSchema());
        caseProps.put(FIELDS, fieldsSchema());

        Map<String, Object> root = new LinkedHashMap<>();
        root.put(FLAGS, array(Map.of(TYPE, STRING)));
        root.put(CASES, array(object(caseProps, List.of(LABEL, CRITERIA, FIELDS))));
        return object(root, List.of(FLAGS, CASES));
    }

    private Map<String, Object> criteriaSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        for (MinimumCriterion criterion : MinimumCriterion.values()) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put(PRESENT, Map.of(TYPE, BOOLEAN));
            c.put(QUOTE, nullable(Map.of(TYPE, STRING)));
            props.put(criterion.jsonName(), object(c, List.of(PRESENT)));
        }
        return object(props, Arrays.stream(MinimumCriterion.values()).map(MinimumCriterion::jsonName).toList());
    }

    private Map<String, Object> fieldsSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        for (FieldDefinition field : catalog.fields()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put(STATUS, enumOf(Arrays.stream(FieldStatus.values()).map(FieldStatus::jsonName).toList()));
            f.put(QUOTES, array(Map.of(TYPE, STRING)));
            f.put(VALUE, nullable(valueSchema(field)));
            f.put(CONFIDENCE, nullable(enumOf(Arrays.stream(Confidence.values()).map(Confidence::jsonName).toList().reversed())));
            f.put(NOTE, nullable(Map.of(TYPE, STRING)));
            Map<String, Object> schema = object(f, List.of(STATUS));
            schema.put("description", field.label());
            props.put(field.id(), schema);
        }
        return object(props, catalog.fields().stream().map(FieldDefinition::id).toList());
    }

    static Map<String, Object> valueSchema(FieldDefinition field) {
        return switch (field.type()) {
            case STRING -> Map.of(TYPE, STRING);
            case DATE -> Map.of(TYPE, STRING, "description", "YYYY-MM-DD");
            case INTEGER -> Map.of(TYPE, INTEGER);
            case NUMBER -> Map.of(TYPE, NUMBER);
            case CODED -> enumOf(field.optionCodes());
            case CODED_MULTI -> array(enumOf(field.optionCodes()));
            case STRING_LIST -> array(Map.of(TYPE, STRING));
        };
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(TYPE, OBJECT);
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("propertyOrdering", new ArrayList<>(properties.keySet()));
        return schema;
    }

    private static Map<String, Object> array(Map<String, Object> items) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(TYPE, ARRAY);
        schema.put("items", items);
        return schema;
    }

    private static Map<String, Object> enumOf(List<String> values) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put(TYPE, STRING);
        schema.put("enum", values);
        return schema;
    }

    private static Map<String, Object> nullable(Map<String, Object> schema) {
        Map<String, Object> copy = new LinkedHashMap<>(schema);
        copy.put("nullable", true);
        return copy;
    }
}
