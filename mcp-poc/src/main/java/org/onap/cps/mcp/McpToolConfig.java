package org.onap.cps.mcp;

import java.util.List;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpToolConfig {

    @Bean
    public ToolCallbackProvider cmHandleSearchToolProvider(CmHandleSearchTool cmHandleSearchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(cmHandleSearchTool)
                .build();
    }
}
