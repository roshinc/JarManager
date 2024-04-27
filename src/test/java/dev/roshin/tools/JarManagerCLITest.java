package dev.roshin.tools;

import org.junit.jupiter.api.Test;

class JarManagerCLITest {

    @Test
    void mainTest() {
        //JarManagerCLI.main(new String[]{"-h"});
        // generate-pom --folder-path C:\JARS --output-path C:\temp\hello.txt  --additional-info
        JarManagerCLI.main(new String[]{"generate-pom", "--folder-path", "C:\\JARS", "--output-path", "C:\\temp\\hello.txt", "--additional-info"});
    }
}