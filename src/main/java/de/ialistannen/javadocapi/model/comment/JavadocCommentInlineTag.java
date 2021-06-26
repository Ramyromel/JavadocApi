package de.ialistannen.javadocapi.model.comment;

import java.util.Optional;

public class JavadocCommentInlineTag implements JavadocCommentFragment {

  private final Type type;
  private final String argument;

  public JavadocCommentInlineTag(Type type, String argument) {
    this.type = type;
    this.argument = argument;
  }

  public Type getType() {
    return type;
  }

  public Optional<String> getArgument() {
    return Optional.ofNullable(argument);
  }

  @Override
  public <T> T accept(CommentVisitor<T> visitor) {
    return visitor.visitInlineTag(this);
  }

  @Override
  public String toString() {
    return "{@" + type + getArgument().map(it -> " " + it).orElse("") + "}";
  }

  public enum Type {
    /**
     * Equivalent to {@literal <code>{@literal text }</code>.} Displays text in a code font, doesn't
     * interpret javadoc.
     */
    CODE,
    /**
     * Represents the relative path to the generated document's (destination) root directory from
     * any generated page. This tag is useful when you want to include a file, such as a copyright
     * page or company logo, that you want to reference from all generated pages. Linking to the
     * copyright page from the bottom of each page is common.
     */
    DOC_ROOT,
    /**
     * Declares that a word or phrase, together with an optional short description, should appear in
     * the index files generated by the standard doclet. The index entry will be linked to the word
     * or phrase that will appear at this point in the generated documentation. The description may
     * be used when the word or phrase to be indexed is not clear by itself, such as for an
     * acronym.
     */
    INDEX,
    /**
     * Inherits (copies) documentation from the nearest inheritable class or implementable interface
     * into the current documentation comment at this tag's location. This enables you to write more
     * general comments higher up the inheritance tree and to write around the copied text.
     */
    INHERIT_DOC,
    /**
     * Inserts an inline link with a visible text label that points to the documentation for the
     * specified package, class, or member name of a referenced class.
     */
    LINK,
    /**
     * A link but not typeset in monospace.
     */
    LINKPLAIN,
    /**
     * Displays text without interpreting the text as HTML markup or nested Javadoc tags.
     */
    LITERAL,
    /**
     * Displays constant values, i.e. the value of a public static constant
     */
    VALUE
  }
}