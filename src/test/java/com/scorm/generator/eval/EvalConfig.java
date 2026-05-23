package com.scorm.generator.eval;

import java.util.List;

/**
 * Parsed view of {@code eval-config.yaml}.
 *
 * <p>Kept intentionally minimal — only fields actually consumed by the
 * evaluation harness are typed. Anything else stays in the raw map.
 */
public record EvalConfig(
        String experimentId,
        ModelCfg model,
        int runsPerDocPerFeature,
        List<DocumentCfg> documents,
        CostCfg cost) {

    public record ModelCfg(
            String name,
            String versionPinned,
            double temperature,
            double topP,
            int maxOutputTokens,
            String judgeName,
            double judgeTemperature) {
    }

    public record DocumentCfg(
            String id,
            String file,
            String domain,
            String title,
            String language,
            int actualPages) {
    }

    public record CostCfg(
            double inputPricePerMillion,
            double outputPricePerMillion,
            double totalBudgetUsd) {

        /** Cost in USD for one call given prompt + completion token counts. */
        public double computeUsd(int promptTokens, int completionTokens) {
            return (promptTokens / 1_000_000.0) * inputPricePerMillion
                    + (completionTokens / 1_000_000.0) * outputPricePerMillion;
        }
    }
}
