package info.kgeorgiy.ja.leshchev.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
/**
 * A class for creating a class with a basic implementation of the corresponding interface.
 * It is also possible to compile this class and create a jar file containing it.
 *
 * @author vertuxty
 * @version 1.0
 * */
public class Implementor implements Impler, JarImpler {
    /** Default final value of {@link System#lineSeparator()}. */
    private static final String LINE_SEPARATOR = System.lineSeparator();
    /** Default final value of {@link File#pathSeparator}. */
    private static final String FILE_PATH_SEPARATOR = File.pathSeparator;
    /** Default final value of {@link File#separator}. */
    private static final String FILE_SEPARATOR = File.separator;
    /** Default final value of tabulation.*/
    private static final String TAB = "\t";
    /** Default form for annotation {@link Override}.*/
    private static final String OVERRIDE_FORM = String.format("%s%s%s", TAB, "@Override", LINE_SEPARATOR);
    /**
     * Default constructor of a class.
     * */
    public Implementor() {
    }
    /**
     * {@inheritDoc}
     * */

    // :NOTE: inherit doc
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (jarFile == null || jarFile.toString().isEmpty()) {
            throw new ImplerException("Error, not have a jarFile path.");
        }
        Path root;
        if (jarFile.getParent() == null) {
            throw new ImplerException("JarFile path doesn't have a root");
        } else {
            root = jarFile.getParent();
        }
        try {
            implement(token, root);
            compile(token, root);
            createJar(token, jarFile, root);
        } catch (ImplerException e) {
            throw new ImplerException(String.format("Error, while implementing jar file: %s", e.getMessage()));
        }
    }

    /**
     *
     * Compile class by {@code token}.
     * Create class file of {@code token} along the {@code root}. Uses {@link #getClassPath(Class, Path)}
     * for classpath and {@link #getTokenAbsoluteName(Class, Path)} for absolute name of a file.
     *
     * @param token a type token of compiling class.
     * @param root a path where to write class file.
     *
     * @throws ImplerException if java compiler is null or {@code exitCode} not equals 0.
     * @see ToolProvider#getSystemJavaCompiler()
     *
     * */
    private static void compile(Class<?> token, final Path root) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Java Compiler is not found");
        }
        final String[] args = Stream.of(getTokenAbsoluteName(token, root), "-cp", getClassPath(token, root), "-encoding", "utf-8").toArray(String[]::new);
        final int exitCode = compiler.run(null, null, null, args);
        if (exitCode != 0) {
            throw new ImplerException(String.format("Error while compiling, expected <0>, got <%s>", exitCode));
        }
    }


    /**
     * Return classpath for current token.
     * Get classpath for file by his {@code token} and {@code root}.
     * Used in {@link #compile(Class, Path)}.
     *
     * @param token a type token of class.
     * @param root a parent path for classpath.
     *
     * @throws ImplerException if {@link URISyntaxException} occurred.
     *
     * @return classpath of a {@code token} and {@code root}.
     * */
    private static String getClassPath(Class<?> token, final Path root) throws ImplerException {
        try {
            return root.toString() + FILE_PATH_SEPARATOR + Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (final URISyntaxException e) {
            throw new ImplerException("Error get class path: " + e.getMessage());
        }
    }


    /**
     * Create manifest for jar file by {@code token}.
     * Write {@link Attributes.Name#MANIFEST_VERSION} equal {@code 1.0}.
     * Write {@link Attributes.Name#MAIN_CLASS} by {@link #getTokenName(Class)}
     * and write {@link Attributes.Name#CLASS_PATH} by {@link #getClassPath(Class, Path)}.
     *
     * @param token a type token of class.
     *
     * @throws ImplerException if can't get name of {@code token} or {@code classpath}.
     *
     * @return {@link Manifest} for jar file.
     *
     * @see Manifest
     * */
    private static Manifest createManifest(Class<?> token) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, getTokenName(token).substring(0, getTokenName(token).length() - 5));
        manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, getClassPath(token, Path.of("")));
        return manifest;
    }

    /**
     * Create jar file for compiled class. If not, throws {@link ImplerException}.
     *
     * @param token a type token of compiled class.
     * @param jarFile a jar file.
     * @param root a path where create jar file.
     *
     * @throws ImplerException if error occurred while creating jar file.
     * @see #implement(Class, Path) 
     * @see #compile(Class, Path) 
     * */

    private static void createJar(Class<?> token, final Path jarFile, final Path root) throws ImplerException {
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarFile), createManifest(token))) {
            String tokenName = getTokenClassName(token);
            System.err.println(tokenName);
            jarOutputStream.putNextEntry(new ZipEntry(tokenName.replace(FILE_SEPARATOR, "/")));
//            Files.copy(root.resolve(tokenName), jarOutputStream);
            writeClassInEntry(jarOutputStream, root.resolve(tokenName));
            jarOutputStream.closeEntry();
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * Write class in {@link ZipEntry}.
     *
     * @param jarOutputStream a {@link JarOutputStream} for writing in jar.
     * @param tokenPath a path where class is located.
     * @throws IOException if I/O error occurred.
     * */
    private static void writeClassInEntry(JarOutputStream jarOutputStream, Path tokenPath) throws IOException {
        try (InputStream input = Files.newInputStream(tokenPath)) {
            byte[] buffer = new byte[1024];
            int pointer;
            while ((pointer = input.read(buffer)) != -1) {
                jarOutputStream.write(buffer, 0, pointer);
            }
        }
    }
    /**
     * {@inheritDoc}
     * */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException(String.format("Token %s is not a interface.", token));
        }
        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException(String.format("Modifiers of %s is private!", token.getSimpleName()));
        }
        String tokenName = getTokenName(token);
        Path path;
        try {
            path = Path.of(root + FILE_SEPARATOR + tokenName);
        } catch (InvalidPathException e) {
            throw new ImplerException(String.format("Invalid path of %s%s%s", root, FILE_SEPARATOR, tokenName));
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
        } catch (IOException e) {
            throw new ImplerException("Unexpected error, when try create path directories");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            classLoad(token, writer);
        } catch (IOException e) {
            throw new ImplerException(String.format("Error, while load a class: %s", e.getMessage()));
        }
    }
    /**
     * Return an absolute path of a file.
     * By {@code token} and {@code root} return absolute path of a class file.
     *
     * @param token a type token of class.
     * @param root a path of parents directories.
     *
     * @return absolute path of a file.
     * @see #getTokenName(Class)
     * @see #getTokenClassName(Class)
     * */
    private static String getTokenAbsoluteName(Class<?> token, Path root) {
        return root.resolve(getTokenName(token)).toAbsolutePath().toString();
    }

    /**
     * Return full name of a java file by {@code token}.
     * Construct full name with package of {@code token} and suffix {@code Impl.java}.
     *
     * @param token a type token.
     *
     * @return full name of a java file.
     * @see #getTokenAbsoluteName(Class, Path)
     * @see #getTokenClassName(Class)
     * */
    private static String getTokenName(Class<?> token) {
        String fullTokenName = token.getPackageName() + FILE_SEPARATOR + token.getSimpleName() + "Impl";
        return fullTokenName.replace(".", FILE_SEPARATOR) + ".java";
    }

    /**
     * Return full name of a class file by {@code token}.
     * Construct full name with package of {@code token} and suffix {@code Impl.class}.
     *
     * @param token a type token.
     *
     * @return full name of a class file.
     * @see #getTokenAbsoluteName(Class, Path)
     * @see #getTokenName(Class)
     * */
    private static String getTokenClassName(Class<?> token) {
        String tokenName = getTokenName(token);
        return getTokenName(token).substring(0, tokenName.length() - 4) + "class";
    }

    /**
     * Write class with {@link BufferedWriter}.
     * Construct class by his structure: {@code HEADER}, {@code METHODS}, {@code END}.
     *
     * @param token a type token.
     * @param writer a bufferedWriter, where to write file.
     *
     * @throws IOException if I/O error occurred.
     * @see #writeTokenHeader(Class, BufferedWriter)
     * @see #writeMethods(Method[], BufferedWriter)
     * */
    private void classLoad(Class<?> token, BufferedWriter writer) throws IOException {
        writeTokenHeader(token, writer);
        writeMethods(token.getMethods(), writer);
        writeEnd(writer);
    }
    /**
     * Escapes characters to write the file correctly.
     * Writer write info in file.
     * @param writer a writer.
     * @param info a info to write in file.F
     *
     * @throws  IOException If I/O error occurred.
     * */
    private void writeCharset(BufferedWriter writer, String info) throws IOException {
        char[] characters = info.toCharArray();
        for (final char c : characters) {
//            if ((int) c >= 128) {
            writer.write(String.format("\\u%04x", (int) c));
//            } else {
//                writer.write(String.format("%"c);
//            }
        }
    }
    /**
     * Write class (token) header with {@link BufferedWriter}.
     * Construct class header by his structure: {@code MODIFIERS},
     * {@code RETURN_TYPE}, {@code TOKEN_NAME}, {@code IMPLEMENTED INTERFACE}.
     *
     * @param token a type token.
     * @param writer a bufferedWriter, where to write file.
     *
     * @throws IOException if I/O error occurred.
     * @see #classLoad(Class, BufferedWriter)
     * @see #writeMethods(Method[], BufferedWriter)
     * */
    private void writeTokenHeader(Class<?> token, BufferedWriter writer) throws IOException {
        writeCharset(writer, String.format("%s;%n%n", token.getPackage()));
        writeCharset(writer, String.format("public class %sImpl implements %s {%n%n",
                token.getSimpleName(),
                token.getCanonicalName())
        );
    }

    /**
     * Write class (token) methods with {@link BufferedWriter}.
     * Write each of methods of class.
     *
     * @param methods a array of class methods.
     * @param writer a bufferedWriter, where to write methods.
     *
     * @throws IOException if I/O error occurred.
     * @see #classLoad(Class, BufferedWriter)
     * @see #writeMethod(Method, BufferedWriter)
     * */

    private void writeMethods(Method[] methods, BufferedWriter writer) throws IOException {
        for (Method method: methods) {
            writeMethod(method, writer);
        }
    }

    /**
     * Write class (token) methods with {@link BufferedWriter}.
     * Write method by structure: {@code OVERRIDE_FORM}, {@code METHOD_BODY}
     *
     * @param method a class method.
     * @param writer a bufferedWriter, where to write method.
     *
     * @throws IOException if I/O error occurred.
     * @see #classLoad(Class, BufferedWriter)
     * @see #writeMethod(Method, BufferedWriter)
     * */

    private void writeMethod(Method method, BufferedWriter writer) throws IOException {
        if (!Modifier.isStatic(method.getModifiers()) && !method.isDefault()) {
            writeCharset(writer, OVERRIDE_FORM);
            writeCharset(writer, getMethod(method));
            writer.newLine();
        }
    }

    /**
     * Return method body. Create method body by structure:
     * {@code METHOD_PREFIX}, {@code ARGUMENTS}, {@code EXCEPTIONS}, {@code RETURN}.
     *
     * @param method a method.
     *
     * @return a string value of a method body.
     * @see #getMethodPrefix(Method)
     * @see #getMethodArguments(Method)
     * @see #getMethodExceptions(Method)
     * @see #getReturn(Method)
     * */
    private String getMethod(Method method) {
        return String.format("%s%s(%s) %s {%s}%n", TAB,
                getMethodPrefix(method),
                getMethodArguments(method),
                getMethodExceptions(method),
                getReturn(method)
        );
    }

    /**
     * Return a {@code RETURN_FORM} in for a method body.
     *
     * @param method a method.
     *
     * @return a string of a return form.
     * @see #getReturnValue(Method)
     * */
    private String getReturn(Method method) {
        String value = getReturnValue(method);
        return value.isEmpty() ? "" : String.format("%n%s%s return %s;%n%s", TAB, TAB, value, TAB);
    }

    /**
     * Return a {@code RETURN_VALUE} of a method.
     *
     * @param method a method.
     *
     * @return a string of a return value.
     * @see #getReturn(Method)
     * */
    private String getReturnValue(Method method) {
        Class<?> clazz = method.getReturnType();
        if (Boolean.TYPE.equals(clazz)) {
            return "false";
        }
        if (Void.TYPE.equals(clazz)) {
            return "";
        }
        if (clazz.isPrimitive()) {
            return "0";
        }
        return "null";
    }

    /**
     * Return a {@code METHOD_PREFIX} of a method.
     *
     * @param method a method.
     *
     * @return a string of a method prefix.
     * @see #getReturn(Method)
     * @see #getMethodExceptions(Method)
     * @see #getMethodArguments(Method)
     * */
    private String getMethodPrefix(Method method) {
        return String.format("public %s %s",
                method.getReturnType().getCanonicalName(),
                method.getName()
        );
    }

    /**
     * Return a {@code EXCEPTIONS} of a method.
     *
     * @param method a method.
     *
     * @return a string of a method exceptions.
     * @see #getReturn(Method)
     * @see #getMethodArguments(Method)
     * @see #getMethodPrefix(Method)
     * */
    private String getMethodExceptions(Method method) {
        String exceptions = Arrays.stream(method.getExceptionTypes()).map(Class::getCanonicalName).collect(Collectors.joining(", "));
        return !exceptions.isEmpty() ? "throws " + exceptions : "";
    }
    /**
     * A function that accepts a {@link Parameter} and returns its full name.
     * */
    private final Function<Parameter, String> getParameterName = parameter -> parameter.getType().getCanonicalName() + " " + parameter.getName();

    /**
     * Return a {@code ARGUMENTS} of a method.
     *
     * @param method a method.
     *
     * @return a string of a method exceptions.
     * @see #getReturn(Method)
     * @see #getMethodExceptions(Method)
     * @see #getMethodPrefix(Method)
     * */
    private String getMethodArguments(Method method) {
        return Arrays.stream(method.getParameters()).map(getParameterName).collect(Collectors.joining(", "));
        // :NOTE: stream + join
    }

    /**
     * Write end of a file.
     * Write {@code '}'} in the end of a file.
     *
     * @param writer a bufferedWriter.
     * @throws IOException if I/O error occurred.
     * */
    private void writeEnd(BufferedWriter writer) throws IOException {
        writeCharset(writer, "}");
    }

    /**
     * Main function.
     * The main method for the program to work. Accepts command line arguments.
     * If you want only implement, then pass two arguments: class name and root - where to create a class.
     * If you want create jar file with compiled class, then pass three arguments: first is a {@code -jar},
     * second - {@code name} of class, third is a {@code jarFile}.
     * @param args a command line arguments.
     *
     * */
    public static void main(String[] args) {
        if (args == null) {
            System.err.println("Args must not be null");
            return;
        }
        if (args.length != 2 && args.length != 3) {
            System.err.printf("Wrong count of args, must be 2 or 3, got: %s", args.length);
            return;
        }
//        System.out.println(args[0]);
        try {
            Implementor implementor = new Implementor();
            if (args.length == 2) {
                implementor.implement(Class.forName(args[0]), Path.of(args[1]));
            } else {
                if (Objects.equals(args[0], "jar")) {
                    implementor.implementJar(Class.forName(args[1]), Path.of(args[2]));
                } else {
                    System.err.println("Invalid argument in input.");
                }
            }
        } catch (InvalidPathException e) {
            System.err.printf("Invalid path for %s%n", args[1]);
        } catch (ImplerException e) {
            System.err.printf(e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.printf("Can't find class by name: %s%n", args[0]);
        }
    }
}
