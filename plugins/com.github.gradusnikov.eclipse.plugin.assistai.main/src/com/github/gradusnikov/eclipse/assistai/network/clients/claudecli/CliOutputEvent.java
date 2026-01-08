package com.github.gradusnikov.eclipse.assistai.network.clients.claudecli;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents an event object within a CLI stream line.
 * 
 * <p>Events contain the actual streaming content, either:
 * <ul>
 *   <li>Content block start - announces a new block with metadata</li>
 *   <li>Content block delta - incremental text or JSON data</li>
 * </ul>
 * 
 * <p>Usage:
 * <pre>
 * CliOutputEvent event = outputLine.getCliOutputEvent();
 * if (event.getContentEventType().isStart()) {
 *     event.getCliContentBlock().ifPresent(block -> ...);
 * } else if (event.getContentEventType().isDelta()) {
 *     event.getDeltaText().ifPresent(text -> ...);
 * }
 * </pre>
 */
public class CliOutputEvent
{
    private JsonNode event;

    /**
     * Creates a new CliOutputEvent from a JSON node.
     * 
     * @param event the event JSON node
     */
    public CliOutputEvent(JsonNode event)
    {
        this.event = event;
    }

    /**
     * @return the content event type (start, delta, or unknown)
     */
    public ContentEventType getContentEventType()
    {
        return ContentEventType.fromString(
                event.has("type") ? event.get("type").asText() : "");
    }

    /**
     * @return true if this event has a delta object
     */
    public boolean hasDelta()
    {
        return event.has("delta");
    }

    /**
     * Gets the content block from a CONTENT_BLOCK_START event.
     * 
     * @return the content block, or empty if not present
     */
    public Optional<CliContentBlock> getCliContentBlock()
    {
        if (event.has("content_block"))
        {
            return Optional.of(new CliContentBlock(event.get("content_block")));
        }
        return Optional.empty();
    }

    /**
     * Gets the text content from a delta.
     * 
     * @return the text, or empty if not a text delta
     */
    public Optional<String> getDeltaText()
    {
        return Optional.ofNullable(event.get("delta"))
                .filter(d -> d.has("text"))
                .map(d -> d.get("text").asText());
    }

    /**
     * @return true if the delta contains text
     */
    public boolean isTextDelta()
    {
        return Optional.ofNullable(event.get("delta"))
                .map(delta -> delta.has("text"))
                .orElse(false);
    }

    /**
     * @return true if the delta contains partial JSON (for tool arguments)
     */
    public boolean isPartialJsonDelta()
    {
        return Optional.ofNullable(event.get("delta"))
                .map(delta -> delta.has("partial_json"))
                .orElse(false);
    }

    /**
     * Gets the partial JSON from a delta (used for streaming tool arguments).
     * 
     * @return the partial JSON string, or empty if not a partial JSON delta
     */
    public Optional<String> getDeltaPartialJson()
    {
        return Optional.ofNullable(event.get("delta"))
                .filter(d -> d.has("partial_json"))
                .map(d -> d.get("partial_json").asText());
    }
}
