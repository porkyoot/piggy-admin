package is.pig.minecraft.admin.moderation;
import is.pig.minecraft.api.*;

/**
 * Data carrier for a word list source.
 */
public record WordListSource(String name, String language, String url) {
}
