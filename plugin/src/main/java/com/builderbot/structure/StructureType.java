package com.builderbot.structure;

/**
 * Enum representing different structure types the bot can build
 * Each type has multiple aliases for flexible chat parsing
 */
public enum StructureType {

    TREEHOUSE("treehouse", "tree", "tree house"),
    BRIDGE("bridge"),
    MASJID("masjid", "mosque");

    private final String[] aliases;

    StructureType(String... aliases) {
        this.aliases = aliases;
    }

    /**
     * Get all aliases for this structure type
     */
    public String[] getAliases() {
        return aliases;
    }

    /**
     * Get the primary name (first alias)
     */
    public String getPrimaryName() {
        return aliases[0];
    }

    /**
     * Find a StructureType from a string input
     * Case-insensitive matching against all aliases
     *
     * @param input The user's input string
     * @return The matching StructureType, or null if no match
     */
    public static StructureType fromString(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        String normalized = input.toLowerCase().trim();

        for (StructureType type : values()) {
            for (String alias : type.aliases) {
                if (alias.equalsIgnoreCase(normalized)) {
                    return type;
                }
            }
        }

        return null;
    }
}
