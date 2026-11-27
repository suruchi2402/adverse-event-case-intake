package com.example.caseintake.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The field catalog: the single source of truth for field ids, labels, sections, types, options and validation.
 * Field order is the order of the catalog file.
 */
public record Catalog(String version, List<Section> sections, List<FieldDefinition> fields) {

    public Catalog {
        sections = List.copyOf(sections);
        fields = List.copyOf(fields);
    }

    public Optional<FieldDefinition> field(String id) {
        return fields.stream().filter(f -> f.id().equals(id)).findFirst();
    }

    public List<FieldDefinition> fieldsIn(String sectionId) {
        return fields.stream().filter(f -> f.sectionId().equals(sectionId)).toList();
    }

    /** Sections in catalog order with their fields, for prompt building. */
    public Map<Section, List<FieldDefinition>> fieldsBySection() {
        Map<Section, List<FieldDefinition>> grouped = new LinkedHashMap<>();
        for (Section section : sections) {
            grouped.put(section, fieldsIn(section.id()));
        }
        return grouped;
    }

    public record Section(String id, String label) {
    }
}
