package sandipchitale.kubemmander;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.contents.DiffContent;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightVirtualFile;

class DiffUtils {
    // Helper method to create DiffContent with syntax coloring
    static DiffContent createDiffContent(DiffContentFactory diffContentFactory, Project project, String fileName, String content, FileType fileType) {
        LightVirtualFile virtualFile = new LightVirtualFile(fileName, fileType, content);
        return diffContentFactory.create(project, virtualFile);
    }
}
