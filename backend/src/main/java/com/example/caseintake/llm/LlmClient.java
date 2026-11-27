package com.example.caseintake.llm;

/**
 * A language model behind one method. Providers, recording, replay, rate limiting and retries are all
 * implementations or decorators of this interface, chosen by configuration in {@link LlmClientFactory}.
 */
public interface LlmClient {

    LlmResponse complete(LlmRequest request);
}
