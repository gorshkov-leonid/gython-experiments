package sample.plugin.util;


import sample.plugin.util.visitor.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

public class DirUtils
{
    private DirUtils()
    {
    }

    /**
     * Copies a directory tree
     */
    public static void copy(Path from, Path to) throws IOException
    {
        validate(from);
        Files.walkFileTree(from, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new CopyDirVisitor(from, to));
    }

    /**
     * Traverses the directory structure and will only copy sub-tree structures where the provided predicate is true
     */
    public static void copyWithPredicate(Path from, Path to, Predicate<Path> predicate) throws IOException
    {
        validate(from);
        Files.walkFileTree(from, new CopyPredicateVisitor(from, to, predicate));
    }

    private static void validate(Path... paths)
    {
        for (Path path : paths)
        {
            Objects.requireNonNull(path);
            if (!Files.isDirectory(path))
            {
                throw new IllegalArgumentException(String.format("%s is not a directory", path.toString()));
            }
        }
    }
}