package com.github.tpgamesnl.skriptaddonpatcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class SkriptAddonPatcher {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java -jar SkriptAddonPatcher.jar <addon jar>");
            System.exit(-1);
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            System.err.println("The file " + args[0] + " does not exist");
            System.exit(-1);
        }

        // Directory check
        if (file.isDirectory()) {
            OutputStream outputStream = new OutputStream() {
                @Override
                public void write(int b) { }
            };

            for (File loopFile : file.listFiles()) {
                System.out.println("File: " + loopFile);
                JarFile jarFile = new JarFile(loopFile);
                convertJar(jarFile, outputStream);
            }
            return;
        }

        if (!file.getName().endsWith(".jar")) {
            System.err.println("That file isn't a jar file");
            System.exit(-1);
        }
        JarFile jarFile = new JarFile(file);

        // Only check, no conversion
        if (args.length == 2 && (args[1].equals("--check") || args[1].equals("-c"))) {
            OutputStream outputStream = new OutputStream() {
                @Override
                public void write(int b) { }
            };
            convertJar(jarFile, outputStream);
            return;
        }

        String outputName = args[0].substring(0, args[0].length() - 4) + "-CONVERTED.jar";
        OutputStream outputStream = new FileOutputStream(outputName);
        convertJar(jarFile, outputStream);
    }

    /**
     * Given {@link OutputStream} will be closed.
     * Not really an API method, as this could kill runtime
     * and it prints to stdout and stderr.
     */
    public static void convertJar(JarFile jarFile, OutputStream outputStream) throws IOException {
        long start = System.currentTimeMillis();

        JarOutputStream jarOutputStream = new JarOutputStream(outputStream);

        Enumeration<JarEntry> enumeration = jarFile.entries();
        int modifiedAmount = 0;
        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement();
            try {
                boolean changed = handleJarEntry(jarFile, jarOutputStream, jarEntry);

                if (changed) {
                    System.out.println(jarEntry.getName() + " has been modified");
                    modifiedAmount++;
                }
            } catch (Throwable t) {
                Util.exitError(t, "An error occurred while converting the class file " + jarEntry.getName() + ":");
            }
        }
        jarOutputStream.close();

        System.out.println();
        System.out.println("" + modifiedAmount + " class files were modified");
        System.out.println("Took " + (System.currentTimeMillis() - start) + "ms");
    }

    public static boolean handleJarEntry(JarFile jarFile, JarOutputStream jarOutputStream, JarEntry jarEntry) throws IOException {
        JarEntry newJarEntry = Util.newJarEntry(jarEntry);

        InputStream inputStream = jarFile.getInputStream(jarEntry);
        if (!newJarEntry.getName().endsWith(".class")) {
            jarOutputStream.putNextEntry(newJarEntry);
            Util.transferStreams(inputStream, jarOutputStream);
            return false;
        }

        byte[] oldClassBytes = Util.readAll(inputStream);

        AtomicBoolean used = new AtomicBoolean();
        byte[] newBytes = replaceClass(oldClassBytes, used);

        if (!used.get()) { // Class didn't have anything replaced
            jarOutputStream.putNextEntry(newJarEntry);
            jarOutputStream.write(oldClassBytes);
            return false;
        } else { // The entry needs replacing
            newJarEntry.setComment("Converted with SkriptAddonPatcher");
            newJarEntry.setLastModifiedTime(FileTime.from(Instant.now()));
            newJarEntry.setSize(newBytes.length);

            jarOutputStream.putNextEntry(newJarEntry);
            jarOutputStream.write(newBytes);
            return true;
        }
    }

    public static byte[] replaceClass(byte[] classBytes, AtomicBoolean used) {
        ClassReader classReader = new ClassReader(classBytes);
        ClassWriter classWriter = new ClassWriter(0);

        ClassVisitor currentScriptReplacer = new MethodWrappingVisitor(classWriter, mv -> new FieldEncapsulatingMethodVisitor(
                mv,
                "ch/njol/skript/ScriptLoader",
                "currentScript",
                "Lch/njol/skript/config/Config;",
                "getCurrentScript",
                "setCurrentScript",
                used
        ));

        ClassVisitor currentSectionsReplacer = new MethodWrappingVisitor(currentScriptReplacer, mv -> new FieldEncapsulatingMethodVisitor(
                mv,
                "ch/njol/skript/ScriptLoader",
                "currentSections",
                "Ljava/util/List;",
                "getCurrentSections",
                "setCurrentSections",
                used
        ));

        ClassVisitor currentLoopsReplacer = new MethodWrappingVisitor(currentSectionsReplacer, mv -> new FieldEncapsulatingMethodVisitor(
                mv,
                "ch/njol/skript/ScriptLoader",
                "currentLoops",
                "Ljava/util/List;",
                "getCurrentLoops",
                "setCurrentLoops",
                used
        ));

        ClassVisitor hasDelayBeforeReplacer = new MethodWrappingVisitor(currentLoopsReplacer, mv -> new FieldEncapsulatingMethodVisitor(
                mv,
                "ch/njol/skript/ScriptLoader",
                "hasDelayBefore",
                "Lch/njol/util/Kleenean;",
                "getHasDelayBefore",
                "setHasDelayBefore",
                used
        ));

        classReader.accept(hasDelayBeforeReplacer, 0);

        return classWriter.toByteArray();
    }

}
