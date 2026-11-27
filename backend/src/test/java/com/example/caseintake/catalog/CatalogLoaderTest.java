package com.example.caseintake.catalog;

import com.example.caseintake.TestData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogLoaderTest {

    @Test
    void loadsTheCommittedCatalog() {
        Catalog catalog = TestData.catalog();
        assertEquals("1.0", catalog.version());
        assertEquals(16, catalog.fields().size());
        assertEquals(4, catalog.sections().size());
        assertTrue(catalog.fields().stream().allMatch(f -> !f.type().hasOptions() || !f.options().isEmpty()));
    }

    @Test
    void acceptsANewFieldOfAnExistingType() {
        String json = TestData.catalogJson().replace("\"fields\": [",
                "\"fields\": [{\"id\": \"patient_weight_kg\", \"label\": \"Weight (kg)\", \"section\": \"patient\", \"type\": \"number\", \"validation\": {\"min\": 0, \"max\": 500}},");
        Catalog catalog = CatalogLoader.parse(json);
        assertEquals(17, catalog.fields().size());
        assertFalse(catalog.field("patient_weight_kg").isEmpty());
    }

    @Test
    void rejectsWhatTheCodeCannotHonour() {
        String base = TestData.catalogJson();
        assertThrows(CatalogException.class, () -> CatalogLoader.parse(base.replace("\"type\": \"integer\"", "\"type\": \"percentage\"")));
        assertThrows(CatalogException.class, () -> CatalogLoader.parse(base.replace("\"validation\": {\"min\": 0, \"max\": 120}", "\"validation\": {\"minLength\": 1}")));
        assertThrows(CatalogException.class, () -> CatalogLoader.parse(base.replace("\"section\": \"report\"", "\"section\": \"nowhere\"")));
    }
}
