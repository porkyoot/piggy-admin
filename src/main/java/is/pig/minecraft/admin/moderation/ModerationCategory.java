package is.pig.minecraft.admin.moderation;

public enum ModerationCategory {
    SAFE("Safe"),
    HATE_SPEECH("Hate Speech"),
    HARASSMENT("Harassment"),
    SEXUAL("Sexual Content"),
    DANGEROUS("Dangerous Content"),
    SWEARS("Swears"),
    DOX("Dox"),
    OTHER("Other");

    private final String displayName;

    ModerationCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ModerationCategory fromString(String text) {
        for (ModerationCategory c : ModerationCategory.values()) {
            if (c.name().equalsIgnoreCase(text) || c.displayName.equalsIgnoreCase(text)) {
                return c;
            }
        }
        return OTHER;
    }
}
