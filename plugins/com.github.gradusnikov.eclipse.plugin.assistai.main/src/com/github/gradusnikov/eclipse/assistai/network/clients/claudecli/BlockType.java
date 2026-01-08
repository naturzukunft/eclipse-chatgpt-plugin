package com.github.gradusnikov.eclipse.assistai.network.clients.claudecli;

/**
 * Content block types in Claude CLI responses.
 * 
 * <p>A content block can be either:
 * <ul>
 *   <li>{@link #TEXT} - Regular text response</li>
 *   <li>{@link #TOOL_USE} - A function/tool call with name, id, and arguments</li>
 * </ul>
 */
public enum BlockType
{
    /** Regular text content */
    TEXT("text"),
    
    /** Tool/function call */
    TOOL_USE("tool_use"),
    
    /** Unknown block type */
    UNKNOWN("");

    private final String value;

    BlockType(String value)
    {
        this.value = value;
    }

    /**
     * Converts a string value to the corresponding enum constant.
     * 
     * @param value the string value from JSON
     * @return the matching enum constant, or {@link #UNKNOWN} if not found
     */
    public static BlockType fromString(String value)
    {
        for (BlockType type : values())
        {
            if (type.value.equals(value))
            {
                return type;
            }
        }
        return UNKNOWN;
    }

    /**
     * @return true if this block represents a function/tool call
     */
    public boolean isFunctionCall()
    {
        return TOOL_USE.equals(this);
    }
}
