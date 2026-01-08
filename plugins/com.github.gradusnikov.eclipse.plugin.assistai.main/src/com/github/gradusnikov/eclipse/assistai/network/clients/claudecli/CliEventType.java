package com.github.gradusnikov.eclipse.assistai.network.clients.claudecli;

/**
 * Event types from Claude CLI stream-json output.
 * 
 * <p>Top-level event types that can appear in the CLI output stream:
 * <ul>
 *   <li>{@link #STREAM_EVENT} - Contains streaming content (text or tool use)</li>
 *   <li>{@link #ERROR} - Indicates an error occurred</li>
 * </ul>
 */
public enum CliEventType
{
    /** A streaming event containing content blocks */
    STREAM_EVENT("stream_event"),
    
    /** An error event */
    ERROR("error"),
    
    /** Unknown event type */
    UNKNOWN("");

    private final String value;

    CliEventType(String value)
    {
        this.value = value;
    }

    /**
     * Converts a string value to the corresponding enum constant.
     * 
     * @param value the string value from JSON
     * @return the matching enum constant, or {@link #UNKNOWN} if not found
     */
    public static CliEventType fromString(String value)
    {
        for (CliEventType type : values())
        {
            if (type.value.equals(value))
            {
                return type;
            }
        }
        return UNKNOWN;
    }
}
