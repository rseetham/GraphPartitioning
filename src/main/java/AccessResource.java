import java.io.File;

public class AccessResource {
    public File readFileFromResources(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        return file;
    }
}

