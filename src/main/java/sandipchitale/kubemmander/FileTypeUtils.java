package sandipchitale.kubemmander;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.PlainTextFileType;

import java.util.Arrays;
import java.util.Optional;

class FileTypeUtils {
    static FileType getFileType(String fileTypeName) {
        return getFileType(fileTypeName, "PLAIN_TEXT");
    }

    static FileType getFileType(String fileTypeName, String defaultFileTypeName) {
        Optional<FileType> fileTypeOptional = Arrays.stream(FileTypeManager.getInstance().getRegisteredFileTypes())
                .filter((FileType fileType) -> fileTypeName.equals(fileType.getName()))
                .findFirst();

        return fileTypeOptional.orElseGet(() -> {
            Optional<FileType> deafultFileTypeOPtional = Arrays.stream(FileTypeManager.getInstance().getRegisteredFileTypes())
                    .filter((FileType fileType) -> defaultFileTypeName.equals(fileType.getName()))
                    .findFirst();
            return deafultFileTypeOPtional.orElse(PlainTextFileType.INSTANCE);
        });
    }
}
