package com.github.gradusnikov.eclipse.assistai.network.clients.claudecli;

/**
 * Content block event types within stream events.
 * 
 * <p>These events describe the lifecycle of a content block:
 * <ul>
 *   <li>{@link #CONTENT_BLOCK_START} - Announces a new content block with metadata</li>
 *   <li>{@link #CONTENT_BLOCK_DELTA} - Contains incremental content data</li>
 * </ul>
 */
public enum ContentEventType
{
    /** Start of a new content block, contains type and metadata */
    CONTENT_BLOCK_START("content_block_start"),
    
    /** Incremental content update (text or partial JSON) */
    CONTENT_BLOCK_DELTA("content_block_delta"),
    
    /** Unknown event type */
    UNKNOWN("");

    private final String value;

    ContentEventType(String value)
    {
        this.value = value;
    }

    /**
     * Converts a string value to the corresponding enum constant.
     * 
     * @param value the string value from JSON
     * @return the matching enum constant, or {@link #UNKNOWN} if not found
     */
    public static ContentEventType fromString(String value)
    {
        for (ContentEventType type : values())
        {
            if (type.value.equals(value))
            {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * @return true if this is a {@link #CONTENT_BLOCK_START} event
     */
    public boolean isStart()
    {
        return CONTENT_BLOCK_START.equals(this);
    }

    /**
     * @return true if this is a {@link #CONTENT_BLOCK_DELTA} event
     */
    public boolean isDelta()
    {
        return CONTENT_BLOCK_DELTA.equals(this);
    }
}
