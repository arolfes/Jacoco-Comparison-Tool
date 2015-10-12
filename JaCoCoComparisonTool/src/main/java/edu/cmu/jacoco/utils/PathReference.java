package edu.cmu.jacoco.utils;

import java.nio.file.FileSystem;
import java.nio.file.Path;

public class PathReference implements AutoCloseable {

    private final Path path;
    private final FileSystem fileSystem;

    public PathReference(final Path path, final FileSystem fileSystem) {
	this.path = path;
	this.fileSystem = fileSystem;
    }

    @Override
    public void close() throws Exception {
	if (this.fileSystem != null)
	    this.fileSystem.close();
    }

    public Path getPath() {
	return this.path;
    }

    public FileSystem getFileSystem() {
	return this.fileSystem;
    }

}
