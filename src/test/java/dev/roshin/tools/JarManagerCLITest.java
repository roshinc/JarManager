package dev.roshin.tools;

import org.junit.jupiter.api.Test;

class JarManagerCLITest {

    @Test
    void mainTest() {
        //JarManagerCLI.main(new String[]{"-h"});
        // generate-pom --folder-path C:\JARS --output-path C:\temp\hello.txt  --additional-info
        JarManagerCLI.main(new String[]{"generate-pom", "--folder-path", "C:\\temp\\JARS", "--output-path", "C:\\temp\\hello.txt", "--additional-info"});
    }

    @Test
    void mainTest_downloadJars() {
        //JarManagerCLI.main(new String[]{"-h"});
        // download-jars C:\temp\hello.additional --target-folder C:\temp\target --source-target-folder C:\temp\source --changes-log C:\temp\changes.log
        JarManagerCLI.main(new String[]{"download-jars", "C:\\temp\\hello.additional", "--target-folder", "C:\\temp\\target", "--source-target-folder", "C:\\temp\\source", "--changes-log", "C:\\temp\\changes.log", "--update-different-only"});
    }
}