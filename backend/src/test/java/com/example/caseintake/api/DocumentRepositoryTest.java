package com.example.caseintake.api;

import com.example.caseintake.TestData;
import com.example.caseintake.config.PathsProperties;
import com.example.caseintake.json.JsonIo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentRepositoryTest {

    @Test
    void titleIsTheSubjectOrReLineOtherwiseTheFirstLine() {
        assertEquals("side effects from paroxetine", DocumentRepository.title("D07", "From: a@b\nSubject: side effects from paroxetine\n"));
        assertEquals("Suspected adverse drug reaction", DocumentRepository.title("D01", "St Alder Infirmary\n\nRe: Suspected adverse drug reaction\n"));
        assertEquals("ROYAL WESTBROOK HOSPITAL", DocumentRepository.title("D02", "\n  ROYAL WESTBROOK HOSPITAL\nDISCHARGE SUMMARY"));
        assertEquals("S03", DocumentRepository.title("S03", null));
    }

    @Test
    void listsDocumentsWithCaseCountsAndServesDetail() throws IOException {
        Path docs = TestData.copyOf(TestData.documents());
        Files.writeString(docs.resolve("S03.pdf"), "%PDF");
        Path out = TestData.tempDir();
        JsonIo.write(out.resolve("D01.json"), Map.of("document_id", "D01", "flags", List.of(), "cases", List.of(Map.of(), Map.of())));
        DocumentRepository repository = new DocumentRepository(new PathsProperties(List.of(docs), TestData.catalogPath(), out, TestData.tempDir()));

        List<DocumentRepository.DocumentSummary> list = repository.list();
        assertEquals(List.of("D01", "S03"), list.stream().map(DocumentRepository.DocumentSummary::id).toList());
        assertEquals(2, list.get(0).caseCount());
        assertTrue(list.get(0).hasOutput());
        assertFalse(list.get(1).hasText());

        DocumentRepository.DocumentDetail detail = repository.get("D01");
        assertTrue(detail.text().startsWith("St Alder Infirmary"));
        assertEquals("D01", detail.extraction().get("document_id"));
        assertNull(repository.get("S03").extraction());

        assertThrows(DocumentNotFoundException.class, () -> repository.get("../catalog"));
        assertThrows(DocumentNotFoundException.class, () -> repository.get("D42"));
        assertEquals("1.0", repository.catalog().get("catalog_version"));
    }
}
