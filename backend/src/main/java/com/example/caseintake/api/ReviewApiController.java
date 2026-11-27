package com.example.caseintake.api;

import com.example.caseintake.api.DocumentRepository.DocumentDetail;
import com.example.caseintake.api.DocumentRepository.DocumentSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** The read API the review UI uses. The UI talks to the backend only through these endpoints. */
@RestController
@RequestMapping("/api")
public class ReviewApiController {

    private final DocumentRepository repository;

    public ReviewApiController(DocumentRepository repository) {
        this.repository = repository;
    }

    /** The catalog the UI renders from, exactly as the catalog file holds it. */
    @GetMapping("/catalog")
    public Map<String, Object> catalog() {
        return repository.catalog();
    }

    /** Every document with its title, number of cases and flags. */
    @GetMapping("/documents")
    public List<DocumentSummary> documents() {
        return repository.list();
    }

    /** One document's text and its extraction output. */
    @GetMapping("/documents/{id}")
    public DocumentDetail document(@PathVariable("id") String id) {
        return repository.get(id);
    }
}
