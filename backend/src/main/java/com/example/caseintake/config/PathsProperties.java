package com.example.caseintake.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.List;

/**
 * Where the documents, catalog, outputs and recordings live (prefix "case-intake").
 * Relative to the repository root by default.
 */
@ConfigurationProperties(prefix = "case-intake")
public record PathsProperties(List<Path> documents, Path catalog, Path out, Path fixtures) {}
