package edu.cmu.jacoco.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.UserDefinedFileAttributeView;

/**
 * java code copied from {@linkplain http://stackoverflow.com/questions/17641706/how-to-copy-a-directory-with-its-attributes-permissions-from-one-location-to-ano/18691793#18691793}
 */
public class CopyFileVisitor extends SimpleFileVisitor<Path> {

    private final Path source;
    private final Path target;
    public CopyFileVisitor(final Path source, final Path target) {
	this.source = source;
	this.target = target;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes sourceBasic) throws IOException {
	Path targetDir = Files.createDirectories(this.target.resolve(this.source.relativize(dir)));
	AclFileAttributeView acl = Files.getFileAttributeView(dir, AclFileAttributeView.class);
	if (acl != null)
	    Files.getFileAttributeView(targetDir, AclFileAttributeView.class).setAcl(acl.getAcl());
	DosFileAttributeView dosAttrs = Files.getFileAttributeView(dir, DosFileAttributeView.class);
	if (dosAttrs != null) {
	    DosFileAttributes sourceDosAttrs = dosAttrs.readAttributes();
	    DosFileAttributeView targetDosAttrs = Files.getFileAttributeView(targetDir, DosFileAttributeView.class);
	    targetDosAttrs.setArchive(sourceDosAttrs.isArchive());
	    targetDosAttrs.setHidden(sourceDosAttrs.isHidden());
	    targetDosAttrs.setReadOnly(sourceDosAttrs.isReadOnly());
	    targetDosAttrs.setSystem(sourceDosAttrs.isSystem());
	}
	FileOwnerAttributeView ownerAttrs = Files.getFileAttributeView(dir, FileOwnerAttributeView.class);
	if (ownerAttrs != null) {
	    FileOwnerAttributeView targetOwner = Files.getFileAttributeView(targetDir, FileOwnerAttributeView.class);
	    targetOwner.setOwner(ownerAttrs.getOwner());
	}
	PosixFileAttributeView posixAttrs = Files.getFileAttributeView(dir, PosixFileAttributeView.class);
	if (posixAttrs != null) {
	    PosixFileAttributes sourcePosix = posixAttrs.readAttributes();
	    PosixFileAttributeView targetPosix = Files.getFileAttributeView(targetDir, PosixFileAttributeView.class);
	    targetPosix.setPermissions(sourcePosix.permissions());
	    targetPosix.setGroup(sourcePosix.group());
	}
	UserDefinedFileAttributeView userAttrs = Files.getFileAttributeView(dir, UserDefinedFileAttributeView.class);
	if (userAttrs != null) {
	    UserDefinedFileAttributeView targetUser = Files.getFileAttributeView(targetDir,
		    UserDefinedFileAttributeView.class);
	    for (String key : userAttrs.list()) {
		ByteBuffer buffer = ByteBuffer.allocate(userAttrs.size(key));
		userAttrs.read(key, buffer);
		buffer.flip();
		targetUser.write(key, buffer);
	    }
	}
	// Must be done last, otherwise last-modified time may be
	// wrong
	BasicFileAttributeView targetBasic = Files.getFileAttributeView(targetDir, BasicFileAttributeView.class);
	targetBasic.setTimes(sourceBasic.lastModifiedTime(), sourceBasic.lastAccessTime(), sourceBasic.creationTime());
	return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
	Files.copy(file, this.target.resolve(this.source.relativize(file)), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
	return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException e) throws IOException {
	throw e;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
	if (e != null)
	    throw e;
	return FileVisitResult.CONTINUE;
    }
}
