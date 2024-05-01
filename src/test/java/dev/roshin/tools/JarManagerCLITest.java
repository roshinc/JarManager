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
        // download-jars C:\temp\hello.additional --target-folder C:\temp\target --source-target-folder
        // C:\temp\source --changes-log C:\temp\changes.log
        JarManagerCLI.main(new String[]{"download-jars", "C:\\temp\\hello.additional", "--target-folder",
                "C:\\temp\\target", "--source-target-folder", "C:\\temp\\source",
                "--changes-log", "C:\\temp\\changes.log", "--update-different-only"});
    }


    @Test
    void mainTest_generateUserLibs() {
        //JarManagerCLI.main(new String[]{"-h"});
        // generate-userlibs C:\temp\hello.additional --output-xml C:\temp\output.xml --jars-path C:\temp\jars
        // --jars-source-path C:\temp\source
        JarManagerCLI.main(new String[]{"generate-userlibs", "test-lib", "C:\\temp\\hello.additional", "--output-xml",
                "C:\\temp\\output.userlibraries", "--jars-path", "C:\\temp\\target", "--jars-source-path", "C:\\temp\\source",
                "--changes-log", "C:\\temp\\changes.log"});
    }

    @Test
    void mainTest_mergeUserLibs() {
        JarManagerCLI.main(new String[]{
                "combine-userlibs",
                "C:\\temp\\output.userlibraries C:\\temp\\file.userlibraries",
                "--output-xml",
                "C:\\temp\\combined.userlibraries"
        });
    }
}