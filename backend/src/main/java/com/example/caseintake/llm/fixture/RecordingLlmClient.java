package com.example.caseintake.llm.fixture;

import com.example.caseintake.json.JsonFormatException;
import com.example.caseintake.json.JsonIo;
import com.example.caseintake.llm.LlmClient;
import com.example.caseintake.llm.LlmRequest;
import com.example.caseintake.llm.LlmResponse;

import java.util.logging.Logger;

/**
 * Calls the live model and records every exchange. With {@code reuseRecordings}, a document that already has a
 * recording for its exact text is answered from it, so an interrupted run resumes without spending quota.
 */
public final class RecordingLlmClient implements LlmClient {

    private static final Logger LOG = Logger.getLogger(RecordingLlmClient.class.getName());

    private final LlmClient live;
    private final FixtureStore store;
    private final ReplayLlmClient replay;
    private final boolean reuseRecordings;

    public RecordingLlmClient(LlmClient live, FixtureStore store, boolean reuseRecordings) {
        this.live = live;
        this.store = store;
        this.replay = new ReplayLlmClient(store);
        this.reuseRecordings = reuseRecordings;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        if (reuseRecordings && replay.hasRecording(request)) {
            LOG.info(() -> request.documentId() + ": using existing recording (pass --refresh to call the model again)");
            return replay.complete(request);
        }
        LlmResponse response = live.complete(request);
        store.write(Fixture.of(request, response, parsedOrRaw(response.rawResponse())));
        LOG.info(() -> request.documentId() + ": recorded " + store.pathFor(request.documentId()));
        return response;
    }

    private static Object parsedOrRaw(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return JsonIo.parse(raw);
        } catch (JsonFormatException e) {
            return raw;
        }
    }
}
