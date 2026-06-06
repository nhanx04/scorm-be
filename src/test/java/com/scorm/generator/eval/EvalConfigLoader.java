package com.scorm.generator.eval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.yaml.snakeyaml.Yaml;

/**
 * Loads {@code eval-config.yaml} into a typed {@link EvalConfig}.
 *
 * <p>Uses raw SnakeYAML rather than Jackson YAML to avoid an extra dependency.
 */
public final class EvalConfigLoader {

    public static final Path DEFAULT_CONFIG_PATH = Path.of(
            "src/test/resources/ai-eval/eval-config.yaml");

    private EvalConfigLoader() {
    }

    public static EvalConfig load() throws IOException {
        return load(DEFAULT_CONFIG_PATH);
    }

    @SuppressWarnings("unchecked")
    public static EvalConfig load(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        Map<String, Object> root = new Yaml().load(text);

        Map<String, Object> experiment = asMap(root.get("experiment"));
        String experimentId = (String) experiment.get("id");

        Map<String, Object> modelMap = asMap(root.get("model"));
        Map<String, Object> judgeMap = asMap(modelMap.get("judge"));
        EvalConfig.ModelCfg model = new EvalConfig.ModelCfg(
                (String) modelMap.get("name"),
                (String) modelMap.get("version_pinned"),
                asDouble(modelMap.get("temperature")),
                asDouble(modelMap.get("top_p")),
                asInt(modelMap.get("max_output_tokens")),
                (String) judgeMap.get("name"),
                asDouble(judgeMap.get("temperature")));

        Map<String, Object> runsMap = asMap(root.get("runs"));
        int runsPerDocPerFeature = asInt(runsMap.get("per_document_per_feature"));

        List<Map<String, Object>> docsRaw = (List<Map<String, Object>>) root.get("documents");
        List<EvalConfig.DocumentCfg> docs = new ArrayList<>(docsRaw.size());
        for (Map<String, Object> d : docsRaw) {
            docs.add(new EvalConfig.DocumentCfg(
                    (String) d.get("id"),
                    (String) d.get("file"),
                    (String) d.get("domain"),
                    (String) d.get("title"),
                    (String) d.get("language"),
                    asInt(d.get("actual_pages"))));
        }

        Map<String, Object> metrics = asMap(root.get("metrics"));
        Map<String, Object> costMap = asMap(metrics.get("cost"));
        EvalConfig.CostCfg cost = new EvalConfig.CostCfg(
                asDouble(costMap.get("input_price_per_million")),
                asDouble(costMap.get("output_price_per_million")),
                asDouble(costMap.get("total_budget_usd")));

        return new EvalConfig(experimentId, model, runsPerDocPerFeature, docs, cost);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        Objects.requireNonNull(o, "Missing expected map node in eval-config.yaml");
        return (Map<String, Object>) o;
    }

    private static int asInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        return Integer.parseInt(o.toString());
    }

    private static double asDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        return Double.parseDouble(o.toString());
    }
}
