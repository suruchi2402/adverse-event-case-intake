package com.example.caseintake.extraction;

import com.example.caseintake.catalog.Catalog;
import com.example.caseintake.catalog.FieldDefinition;
import com.example.caseintake.extraction.ModelOutput.ModelCase;
import com.example.caseintake.extraction.ModelOutput.ModelCriterion;
import com.example.caseintake.result.CaseResult;
import com.example.caseintake.result.DocumentFlag;
import com.example.caseintake.result.FieldResult;
import com.example.caseintake.text.PageMarkers;
import com.example.caseintake.text.SourceText;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the cases and flags of one document from the model output. A case is kept only when the model reports
 * all four minimum elements; otherwise it is dropped and a flag says which element was missing. Flags the pipeline
 * can determine itself (multiple patients) are set from the result, not taken from the model.
 */
public final class CaseAssembler {

    private final Catalog catalog;
    private final FieldResolver resolver;

    public CaseAssembler(Catalog catalog, FieldResolver resolver) {
        this.catalog = catalog;
        this.resolver = resolver;
    }

    public record Assembled(List<String> flags, List<CaseResult> cases) {
    }

    public Assembled assemble(ModelOutput output, SourceText source) {
        PageMarkers pages = new PageMarkers(source);
        Set<String> flags = new LinkedHashSet<>();
        output.flags().stream()
                .filter(f -> DocumentFlag.modelFlagNames().contains(f))
                .forEach(flags::add);

        List<CaseResult> cases = new ArrayList<>();
        for (ModelCase modelCase : output.cases()) {
            List<String> missing = missingCriteria(modelCase);
            if (!missing.isEmpty()) {
                flags.add("case dropped (" + labelOf(modelCase, cases.size()) + "): missing " + String.join(", ", missing));
                continue;
            }
            Map<String, FieldResult> fields = new LinkedHashMap<>();
            for (FieldDefinition field : catalog.fields()) {
                fields.put(field.id(), resolver.resolve(field, modelCase.fields().get(field.id()), source, pages));
            }
            cases.add(new CaseResult(cases.size(), labelOf(modelCase, cases.size()), fields));
        }

        if (cases.size() > 1) {
            flags.add(DocumentFlag.MULTIPLE_PATIENTS.jsonName());
        }
        if (!cases.isEmpty()) {
            flags.remove(DocumentFlag.NO_ADVERSE_EVENT.jsonName());
        }
        return new Assembled(new ArrayList<>(flags), cases);
    }

    private static List<String> missingCriteria(ModelCase modelCase) {
        List<String> missing = new ArrayList<>();
        for (MinimumCriterion criterion : MinimumCriterion.values()) {
            ModelCriterion reported = modelCase.criteria().get(criterion);
            if (reported == null || !reported.present()) {
                missing.add(criterion.description());
            }
        }
        return missing;
    }

    private static String labelOf(ModelCase modelCase, int index) {
        return modelCase.label() != null ? modelCase.label().strip() : "Patient " + (index + 1);
    }
}
