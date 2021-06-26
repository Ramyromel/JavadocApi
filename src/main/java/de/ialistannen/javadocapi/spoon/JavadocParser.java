package de.ialistannen.javadocapi.spoon;

import de.ialistannen.javadocapi.model.QualifiedName;
import de.ialistannen.javadocapi.model.comment.JavadocComment;
import de.ialistannen.javadocapi.model.comment.JavadocCommentFragment;
import de.ialistannen.javadocapi.model.comment.JavadocCommentInlineTag;
import de.ialistannen.javadocapi.model.comment.JavadocCommentInlineTag.Type;
import de.ialistannen.javadocapi.model.comment.JavadocCommentLink;
import de.ialistannen.javadocapi.model.comment.JavadocCommentTag;
import de.ialistannen.javadocapi.model.comment.JavadocCommentText;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import spoon.javadoc.internal.Javadoc;
import spoon.javadoc.internal.JavadocDescriptionElement;
import spoon.javadoc.internal.JavadocInlineTag;
import spoon.javadoc.internal.JavadocSnippet;
import spoon.reflect.code.CtJavaDoc;
import spoon.reflect.code.CtJavaDocTag;
import spoon.reflect.declaration.CtCompilationUnit;
import spoon.reflect.declaration.CtType;
import spoon.reflect.reference.CtTypeReference;

public class JavadocParser {

  public JavadocComment fromCtJavadoc(CtJavaDoc javadoc) {
    List<JavadocCommentFragment> shortDescription = fromJavadoc(
        javadoc, Javadoc.parse(javadoc.getShortDescription())
    );
    List<JavadocCommentFragment> longDescription = fromJavadoc(
        javadoc, Javadoc.parse(javadoc.getLongDescription())
    );

    List<JavadocCommentTag> tags = new ArrayList<>();
    for (CtJavaDocTag tag : javadoc.getTags()) {
      tags.add(new JavadocCommentTag(
          tag.getRealName(),
          tag.getParam(),
          fromJavadoc(javadoc, Javadoc.parse(tag.getContent()))
      ));
    }

    return new JavadocComment(tags, shortDescription, longDescription);
  }

  private List<JavadocCommentFragment> fromJavadoc(CtJavaDoc reference, Javadoc doc) {
    List<JavadocCommentFragment> fragments = new ArrayList<>();

    for (JavadocDescriptionElement element : doc.getDescription().getElements()) {
      if (element instanceof JavadocInlineTag) {
        fragments.add(parseInlineTag(reference, (JavadocInlineTag) element));
      } else if (element instanceof JavadocSnippet) {
        fragments.add(new JavadocCommentText(element.toText()));
      } else {
        throw new AssertionError("Unknown javadoc type: " + element.getClass());
      }
    }

    return fragments;
  }

  private JavadocCommentInlineTag parseInlineTag(CtJavaDoc reference, JavadocInlineTag inline) {
    Type type = Type.valueOf(inline.getType().name());

    if (type == Type.LINK || type == Type.LINKPLAIN) {
      String text = inline.getContent();
      Pattern pattern = Pattern.compile("^([\\w$.]*)(#([\\w$]*)(\\((.*)\\))?)?( .+)?$");
      Matcher matcher = pattern.matcher(text);
      if (!matcher.find()) {
        throw new IllegalArgumentException(":(");
      }
      String className = matcher.group(1);
      String methodName = matcher.group(3);
      String parameters = matcher.group(5);
      String label = matcher.group(6);

      String qualifiedString = qualifyTypeName(reference, className);
      if (methodName != null) {
        qualifiedString += "#" + methodName + "(";
        if (parameters != null) {
          qualifiedString += Arrays.stream(parameters.split(","))
              .map(it -> it.split(" ")[0])
              .map(it -> qualifyTypeName(reference, it))
              .collect(Collectors.joining(","));
        }
        qualifiedString += ")";
      }

      return new JavadocCommentLink(
          new QualifiedName(qualifiedString),
          label,
          type == Type.LINKPLAIN
      );
    }

    return new JavadocCommentInlineTag(
        type,
        inline.getContent()
    );
  }

  private String qualifyTypeName(CtJavaDoc element, String name) {
    CtType<?> parentType = element.getParent(CtType.class);
    if (parentType != null && !name.isBlank()) {
      Optional<CtTypeReference<?>> type = parentType.getReferencedTypes()
          .stream()
          .filter(it -> it.getQualifiedName().endsWith(name))
          .findAny();
      if (type.isPresent()) {
        return type.get().getQualifiedName();
      }
    }
    if (parentType != null && name.isBlank()) {
      return parentType.getQualifiedName();
    }

    CtCompilationUnit parentUnit = element.getPosition().getCompilationUnit();
    return parentUnit.getImports()
        .stream()
        .filter(it -> it.getReference().getSimpleName().equals(name))
        .findAny()
        .map(it -> {
          if (it.getReferencedTypes().size() != 1) {
            throw new AssertionError("Multiple types for import?");
          }
          return it.getReferencedTypes().iterator().next().getQualifiedName();
        })
        .orElse("" + name);
  }
}