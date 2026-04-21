package org.onap.cps.mcp;

import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class CmHandleSearchTool {

    private final WebClient webClient;

    public CmHandleSearchTool(@Value("${cps.base-url:http://localhost:8883}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Tool(description = "Search for CM handle IDs in the CPS NCMP inventory. "
            + "Supports conditions: 'hasAllProperties' (match public properties), "
            + "'cmHandleWithCpsPath' (match by CPS path expression), "
            + "'cmHandleWithDmiPlugin' (match by DMI plugin name). "
            + "Returns a list of CM handle ID strings.")
    public List<String> searchCmHandleIds(
            @ToolParam(description = "Condition name: 'hasAllProperties', 'cmHandleWithCpsPath', or 'cmHandleWithDmiPlugin'")
            String conditionName,
            @ToolParam(description = "Condition parameters as key-value pairs, e.g. {\"color\":\"red\"} for hasAllProperties, "
                    + "{\"cpsPath\":\"//state[@cm-handle-state='READY']\"} for cmHandleWithCpsPath, "
                    + "{\"dmiPluginName\":\"http://my-dmi:8080\"} for cmHandleWithDmiPlugin")
            Map<String, String> conditionParameters,
            @ToolParam(description = "If true, return alternate IDs instead of CM handle IDs", required = false)
            Boolean outputAlternateId) {

        Map<String, Object> body = Map.of(
                "cmHandleQueryParameters", List.of(
                        Map.of("conditionName", conditionName,
                                "conditionParameters", List.of(conditionParameters))));

        String uri = "/ncmpInventory/v1/ch/searches"
                + (Boolean.TRUE.equals(outputAlternateId) ? "?outputAlternateId=true" : "");

        return webClient.post()
                .uri(uri)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .collectList()
                .block();
    }
}
