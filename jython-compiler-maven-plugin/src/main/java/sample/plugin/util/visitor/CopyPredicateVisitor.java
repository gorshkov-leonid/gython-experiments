package sample.plugin.util.visitor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

public class CopyPredicateVisitor extends SimpleFileVisitor<Path>
{

    private Path fromPath;
    private Path toPath;
    private Predicate<Path> copyPredicate;

    public CopyPredicateVisitor(Path fromPath, Path toPath, Predicate<Path> copyPredicate)
    {
        this.fromPath = fromPath;
        this.toPath = toPath;
        this.copyPredicate = copyPredicate;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
    {
        if (copyPredicate.test(dir))
        {
            Path targetPath = toPath.resolve(fromPath.relativize(dir));
            if (!Files.exists(targetPath))
            {
                Files.createDirectory(targetPath);
            }
            return FileVisitResult.CONTINUE;
        }
        return FileVisitResult.SKIP_SUBTREE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
    {
        Files.copy(file, toPath.resolve(fromPath.relativize(file)));
        return FileVisitResult.CONTINUE;
    }
}