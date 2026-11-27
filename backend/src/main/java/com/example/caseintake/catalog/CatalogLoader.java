package com.example.caseintake.catalog;

import com.example.caseintake.catalog.Catalog.Section;
import com.example.caseintake.catalog.FieldDefinition.FieldOption;
import com.example.caseintake.catalog.FieldDefinition.FieldValidation;
import com.example.caseintake.json.JsonIo;
import com.example.caseintake.result.Confidence;
import com.example.caseintake.result.FieldStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Reads and checks the catalog file. Anything the code cannot honour (an unknown type, an unknown validation key,
 * a coded field without options, a duplicate id) stops the run with a clear message instead of being skipped.
 */
public final class CatalogLoader {

    private static final String KEY_VERSION = "catalog_version";
    private static final String KEY_SECTIONS = "sections";
    private static final String KEY_FIELDS = "fields";
    private static final String KEY_STATUSES = "statuses";
    private static final String KEY_CONFIDENCE = "confidence_levels";
    private static final String KEY_ID = "id";
    private static final String KEY_LABEL = "label";
    private static final String KEY_SECTION = "section";
    private static final String KEY_TYPE = "type";
    private static final String KEY_OPTIONS = "options";
    private static final String KEY_CODE = "code";
    private static final String KEY_VALIDATION = "validation";
    private static final String KEY_GUIDANCE = "guidance";
    private static final String RULE_PATTERN = "pattern";
    private static final String RULE_MIN = "min";
    private static final String RULE_MAX = "max";
    private static final Set<String> VALIDATION_KEYS = Set.of(RULE_PATTERN, RULE_MIN, RULE_MAX);

    private CatalogLoader() {
    }

    public static Catalog load(Path path) {
        String json;
        try {
            json = Files.readString(path);
        } catch (IOException e) {
            throw new CatalogException("Cannot read catalog " + path + ": " + e.getMessage(), e);
        }
        return parse(json);
    }

    public static Catalog parse(String json) {
        Map<String, Object> root = JsonIo.parseObject(json);
        String version = requireString(root, KEY_VERSION, "catalog");
        checkStatuses(root);
        checkConfidenceLevels(root);

        List<Section> sections = new ArrayList<>();
        for (Map<String, Object> s : objectList(root, KEY_SECTIONS, "catalog")) {
            sections.add(new Section(requireString(s, KEY_ID, "section"), requireString(s, KEY_LABEL, "section")));
        }
        Set<String> sectionIds = sections.stream().map(Section::id).collect(Collectors.toSet());

        List<FieldDefinition> fields = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map<String, Object> f : objectList(root, KEY_FIELDS, "catalog")) {
            FieldDefinition field = parseField(f);
            if (!seen.add(field.id())) {
                throw new CatalogException("Duplicate field id '" + field.id() + "'");
            }
            if (!sectionIds.contains(field.sectionId())) {
                throw new CatalogException("Field '" + field.id() + "' names unknown section '" + field.sectionId() + "'");
            }
            fields.add(field);
        }
        return new Catalog(version, sections, fields);
    }

    private static FieldDefinition parseField(Map<String, Object> f) {
        String id = requireString(f, KEY_ID, "field");
        String where = "field '" + id + "'";
        FieldType type = FieldType.fromJson(requireString(f, KEY_TYPE, where));
        List<FieldOption> options = new ArrayList<>();
        if (f.containsKey(KEY_OPTIONS)) {
            for (Map<String, Object> o : objectList(f, KEY_OPTIONS, where)) {
                String code = requireString(o, KEY_CODE, where + " option");
                options.add(new FieldOption(code, o.get(KEY_LABEL) instanceof String l ? l : code));
            }
        }
        if (type.hasOptions() && options.isEmpty()) {
            throw new CatalogException(where + " has type " + type.jsonName() + " but no options");
        }
        if (!type.hasOptions() && !options.isEmpty()) {
            throw new CatalogException(where + " has options but type " + type.jsonName() + " does not use them");
        }
        Object guidance = f.get(KEY_GUIDANCE);
        return new FieldDefinition(
                id,
                requireString(f, KEY_LABEL, where),
                requireString(f, KEY_SECTION, where),
                type,
                options,
                parseValidation(f.get(KEY_VALIDATION), where),
                guidance instanceof String g ? g : null);
    }

    private static FieldValidation parseValidation(Object raw, String where) {
        if (raw == null) {
            return FieldValidation.NONE;
        }
        if (!(raw instanceof Map<?, ?> map)) {
            throw new CatalogException(where + ": validation must be an object");
        }
        for (Object key : map.keySet()) {
            if (!VALIDATION_KEYS.contains(String.valueOf(key))) {
                throw new CatalogException(where + ": unsupported validation key '" + key + "'");
            }
        }
        Pattern pattern = null;
        if (map.get(RULE_PATTERN) instanceof String p) {
            try {
                pattern = Pattern.compile(p);
            } catch (PatternSyntaxException e) {
                throw new CatalogException(where + ": invalid pattern " + p, e);
            }
        }
        return new FieldValidation(pattern, number(map.get(RULE_MIN), where), number(map.get(RULE_MAX), where));
    }

    private static Double number(Object raw, String where) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.doubleValue();
        }
        throw new CatalogException(where + ": min and max must be numbers");
    }

    private static void checkStatuses(Map<String, Object> root) {
        if (root.get(KEY_STATUSES) instanceof Map<?, ?> statuses) {
            Set<String> declared = statuses.keySet().stream().map(String::valueOf).collect(Collectors.toSet());
            Set<String> known = FieldStatus.jsonNames();
            if (!declared.equals(known)) {
                throw new CatalogException("Catalog statuses " + declared + " differ from the supported " + known);
            }
        }
    }

    private static void checkConfidenceLevels(Map<String, Object> root) {
        if (root.get(KEY_CONFIDENCE) instanceof List<?> levels) {
            Set<String> declared = levels.stream().map(String::valueOf).collect(Collectors.toSet());
            Set<String> known = Confidence.jsonNames();
            if (!declared.equals(known)) {
                throw new CatalogException("Catalog confidence levels " + declared + " differ from the supported " + known);
            }
        }
    }

    private static String requireString(Map<String, Object> map, String key, String where) {
        if (map.get(key) instanceof String s && !s.isBlank()) {
            return s;
        }
        throw new CatalogException(where + ": missing or empty '" + key + "'");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objectList(Map<String, Object> map, String key, String where) {
        if (!(map.get(key) instanceof List<?> list)) {
            throw new CatalogException(where + ": '" + key + "' must be a list");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?>)) {
                throw new CatalogException(where + ": every entry of '" + key + "' must be an object");
            }
            result.add((Map<String, Object>) item);
        }
        return result;
    }
}
