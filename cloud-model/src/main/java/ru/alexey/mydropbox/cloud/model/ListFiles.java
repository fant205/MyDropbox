package ru.alexey.mydropbox.cloud.model;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class ListFiles implements CloudMessage {

    //    private final List<FileSystemObject> fileSystemObjects = new ArrayList<>();
    private final Map<String, Integer> files;

    public ListFiles(Path path) throws IOException {
        List<String> list = Files.list(path)
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());

        List<Path> collect = Files.list(path).collect(Collectors.toList());
        Iterator<Path> iterator = collect.iterator();
        files = new HashMap<>();
        while (iterator.hasNext()) {
            Path next = iterator.next();
            if (Files.isDirectory(next)) {
                files.put(next.getFileName().toString(), 1);
            } else {
                files.put(next.getFileName().toString(), 0);
            }
        }

//        Iterator<String> iterator = files.iterator();
//        while (iterator.hasNext()){
//            String next = iterator.next();
//            Path p = Paths.get(next);
//            if(Files.isDirectory(p)){
//                fileSystemObjects.add(new Folder(p.toString()));
//            } else {
//                fileSystemObjects.add(new File(p.toString()));
//            }
//        }
    }
}