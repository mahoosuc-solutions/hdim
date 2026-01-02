package com.healthdata.agent.tool;

import com.healthdata.agent.core.AgentContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for managing and discovering agent tools.
 * Supports dynamic tool registration and context-based filtering.
 */
@Slf4j
@Service
public class ToolRegistry {

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    public ToolRegistry(List<Tool> toolList) {
        // Register all available tools
        toolList.forEach(tool -> {
            tools.put(tool.getName(), tool);
            log.info("Registered tool: {} [{}]",
                tool.getName(),
                tool.getDefinition().getCategory());
        });

        log.info("Initialized tool registry with {} tools: {}",
            tools.size(), tools.keySet());
    }

    /**
     * Get a tool by name.
     */
    public Optional<Tool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * Get a tool by name or throw exception.
     */
    public Tool getToolOrThrow(String name) {
        return getTool(name)
            .orElseThrow(() -> new ToolNotFoundException("Tool not found: " + name));
    }

    /**
     * List all registered tools.
     */
    public List<Tool> listTools() {
        return List.copyOf(tools.values());
    }

    /**
     * List tools available in the given context.
     */
    public List<Tool> listAvailableTools(AgentContext context) {
        return tools.values().stream()
            .filter(tool -> tool.isAvailable(context))
            .collect(Collectors.toList());
    }

    /**
     * List tools by category.
     */
    public List<Tool> listToolsByCategory(ToolDefinition.ToolCategory category) {
        return tools.values().stream()
            .filter(tool -> tool.getDefinition().getCategory() == category)
            .collect(Collectors.toList());
    }

    /**
     * Get tool definitions for LLM providers.
     */
    public List<ToolDefinition> getToolDefinitions() {
        return tools.values().stream()
            .map(Tool::getDefinition)
            .collect(Collectors.toList());
    }

    /**
     * Get tool definitions for available tools in context.
     */
    public List<ToolDefinition> getToolDefinitions(AgentContext context) {
        return listAvailableTools(context).stream()
            .map(Tool::getDefinition)
            .collect(Collectors.toList());
    }

    /**
     * Get tool definitions filtered by names.
     */
    public List<ToolDefinition> getToolDefinitions(List<String> toolNames) {
        return toolNames.stream()
            .map(this::getTool)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Tool::getDefinition)
            .collect(Collectors.toList());
    }

    /**
     * Check if a tool exists.
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }

    /**
     * Register a new tool dynamically.
     */
    public void registerTool(Tool tool) {
        String name = tool.getName();
        if (tools.containsKey(name)) {
            log.warn("Overwriting existing tool: {}", name);
        }
        tools.put(name, tool);
        log.info("Dynamically registered tool: {}", name);
    }

    /**
     * Unregister a tool.
     */
    public void unregisterTool(String name) {
        Tool removed = tools.remove(name);
        if (removed != null) {
            log.info("Unregistered tool: {}", name);
        }
    }

    /**
     * Get tool count.
     */
    public int getToolCount() {
        return tools.size();
    }

    /**
     * Exception thrown when a tool is not found.
     */
    public static class ToolNotFoundException extends RuntimeException {
        public ToolNotFoundException(String message) {
            super(message);
        }
    }
}
