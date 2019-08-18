package ru.deysa.response.options;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("ru.deysa.response.options.ResponseOption")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class ResponseOptionsProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
            if (annotatedElements.isEmpty()) {
                continue;
            }
            String className = ((TypeElement) annotatedElements.iterator().next().getEnclosingElement())
                    .getQualifiedName().toString();

            List<String> fieldList = annotatedElements.stream()
                    .map(setter -> setter.getSimpleName().toString())
                    .collect(Collectors.toList());

            try {
                writeBuilderFile(className, fieldList);
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "Can't create file " + e.getMessage());
            }
        }
        return true;
    }

    private void writeBuilderFile(
            String className, List<String> fieldList)
            throws IOException {

        String packageName = null;
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            packageName = className.substring(0, lastDot);
        }

        String builderClassName = className + "Options";
        String builderSimpleClassName = builderClassName
                .substring(lastDot + 1);

        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(builderClassName);

        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

            if (packageName != null) {
                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();
            }
            out.println("import java.io.Serializable;");
            out.println();

            out.print("public class ");
            out.print(builderSimpleClassName);
            out.println(" implements Serializable {");
            out.println();

            fieldList.forEach(fieldName -> {
                out.print("    private boolean");
                out.print(" ");
                out.print(fieldName);
                out.print(" ");
                out.println("= true;");
            });
            out.println();

            fieldList.forEach(fieldName -> {
                out.print("    public void");
                out.print(" ");
                out.print(fieldName);
                out.println("(boolean value) {");
                out.print("        this.");
                out.print(fieldName);
                out.println(" = value;");
                out.print("    }");
                out.println();
            });

            out.println("}");
        }
    }
}
