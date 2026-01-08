package com.github.gradusnikov.eclipse.assistai.network.clients.claudecli;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a content block from a CONTENT_BLOCK_START event.
 * 
 * <p>A content block contains metadata about the streaming content:
 * <ul>
 *   <li>For text blocks: just the type</li>
 *   <li>For tool_use blocks: type, tool name, and tool id</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * CliContentBlock block = event.getCliContentBlock().get();
 * if (block.getType().isFunctionCall()) {
 *     String name = block.getToolName();
 *     String id = block.getToolId();
 * }
 * </pre>
 */
public class CliContentBlock
{
    private JsonNode contentBlock;

    /**
     * Creates a new CliContentBlock from a JSON node.
     * 
     * @param contentBlock the content_block JSON node
     */
    public CliContentBlock(JsonNode contentBlock)
    {
        this.contentBlock = contentBlock;
    }

    /**
     * @return the block type (text, tool_use, or unknown)
     */
    public BlockType getType()
    {
        return BlockType.fromString(
                contentBlock.has("type") ? contentBlock.get("type").asText() : "");
    }

    /**
     * Gets the tool name for a tool_use block.
     * 
     * @return the tool name, or empty string if not present
     */
    public String getToolName()
    {
        return contentBlock.has("name") ? contentBlock.get("name").asText() : "";
    }

    /**
     * Gets the tool id for a tool_use block.
     * 
     * @return the tool id, or empty string if not present
     */
    public String getToolId()
    {
        return contentBlock.has("id") ? contentBlock.get("id").asText() : "";
    }
}
