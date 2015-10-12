package edu.cmu.jacoco.utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class JarCopyFileVisitor implements FileVisitor<Path> {

    private Path currentTarget;

    private final Path sourceJarPath;
    private final Path target;

    public JarCopyFileVisitor(final Path sourceJarPath, final Path target) {
	this.sourceJarPath = sourceJarPath;
	this.target = target;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
	this.currentTarget = this.target.resolve(this.sourceJarPath.relativize(dir).toString());
	Files.createDirectories(this.currentTarget);
	return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
	Files.copy(file, this.target.resolve(this.sourceJarPath.relativize(file).toString()),
		StandardCopyOption.REPLACE_EXISTING);
	return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
	Objects.requireNonNull(file);
	throw exc;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
	Objects.requireNonNull(dir);
	if (exc != null)
	    throw exc;
	return FileVisitResult.CONTINUE;
    }

}
