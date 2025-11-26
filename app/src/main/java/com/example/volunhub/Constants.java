package com.example.volunhub;

/**
 * A utility class for holding global constant values used throughout the application.
 * This class cannot be instantiated.
 */
public final class Constants {

    // Private constructor prevents instantiation
    private Constants() {}

    /**
     * A list of predefined organization fields/categories.
     * Used to populate dropdown menus for organization sign-up and profile editing.
     */
    public static final String[] ORG_FIELDS = new String[] {
            "Health & Wellness",
            "Education & Literacy",
            "Social Services",
            "Environment & Animals",
            "Community Development",
            "Arts & Culture",
            "Sports & Recreation",
            "Human Rights & Advocacy",
            "Crisis & Disaster Relief",
            "Technology & IT",
            "Professional Services",
            "Youth Development",
            "Elderly Care"
    };
}
