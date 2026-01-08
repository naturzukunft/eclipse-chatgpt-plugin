package com.github.gradusnikov.eclipse.assistai.network.clients.claudecli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Represents a single line of output from the Claude CLI stream.
 * 
 * <p>Each line in the CLI stream-json output is a separate JSON object.
 * This class wraps the parsing and provides typed access to the content.
 * 
 * <p>Usage:
 * <pre>
 * CliOutputLine line = new CliOutputLine(rawLine);
 * if (!line.isBlank()) {
 *     switch (line.getCliEventType()) {
 *         case STREAM_EVENT -> handleEvent(line.getCliOutputEvent());
 *         case ERROR -> throw new RuntimeException(line.getErrorMessage());
 *     }
 * }
 * </pre>
 */
public class CliOutputLine
{
    protected final ObjectMapper objectMapper = new ObjectMapper();
    private JsonNode json;
    private String line;

    /**
     * Creates a new CliOutputLine from a raw string.
     * 
     * @param line the raw line from CLI output
     * @throws JsonMappingException if JSON structure is invalid
     * @throws JsonProcessingException if JSON parsing fails
     */
    public CliOutputLine(String line) throws JsonMappingException, JsonProcessingException
    {
        this.line = line;
        if (!isBlank())
        {
            json = objectMapper.readTree(line);
        }
    }

    /**
     * @return true if the line is blank (empty or whitespace only)
     */
    public boolean isBlank()
    {
        return line.isBlank();
    }

    /**
     * @return the event type of this line
     */
    public CliEventType getCliEventType()
    {
        return CliEventType.fromString(json.has("type") ? json.get("type").asText() : "");
    }

    /**
     * @return true if this line contains an event object
     */
    public boolean hasEvent()
    {
        return json.has("event");
    }

    /**
     * Gets the event object from this line.
     * 
     * @return the event as a {@link CliOutputEvent}
     * @throws IllegalStateException if {@link #hasEvent()} is false
     */
    public CliOutputEvent getCliOutputEvent()
    {
        if (!hasEvent())
        {
            throw new IllegalStateException("No event present in this line");
        }
        return new CliOutputEvent(json.get("event"));
    }

    /**
     * Gets the error message from an ERROR event.
     * 
     * @return the error message, or "Unknown CLI error" if not present
     */
    public String getErrorMessage()
    {
        return json.path("error").asText("Unknown CLI error");
    }
}
