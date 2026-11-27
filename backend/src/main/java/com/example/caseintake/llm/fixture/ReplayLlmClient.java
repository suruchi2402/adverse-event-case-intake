package com.example.caseintake.llm.fixture;

import com.example.caseintake.llm.LlmClient;
import com.example.caseintake.llm.LlmRequest;
import com.example.caseintake.llm.LlmResponse;

import java.util.Objects;

/**
 * Answers from recordings only, with no network. A recording is matched by document id and the hash of the
 * document text, not by prompt: with a changed catalog the prompt changes, and the brief asks replay to re-check
 * the recorded values against the new catalog rather than fail.
 */
public final class ReplayLlmClient implements LlmClient {

    private final FixtureStore store;

    public ReplayLlmClient(FixtureStore store) {
        this.store = store;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        Fixture fixture = store.read(request.documentId()).orElseThrow(() -> new FixtureException(
                "No recording for " + request.documentId() + " at " + store.pathFor(request.documentId())
                        + ". Run the pipeline once with a key to record it."));
        if (!Objects.equals(fixture.documentSha256(), request.documentSha256())) {
            throw new FixtureException("The recording for " + request.documentId()
                    + " was made for a different version of the document text. Record it again.");
        }
        return fixture.toResponse();
    }

    /** Whether a usable recording exists for this exact document text. */
    public boolean hasRecording(LlmRequest request) {
        return store.read(request.documentId())
                .map(f -> Objects.equals(f.documentSha256(), request.documentSha256()))
                .orElse(false);
    }
}
