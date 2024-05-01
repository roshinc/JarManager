package dev.roshin.tools.userlibs_combiner;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class UserLibrariesMerger {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java UserLibrariesMerger <output-file> <input-file1> [<input-file2> ...]");
            System.exit(1);
        }

        Path outputPath = Path.of(args[0]);
        List<Path> inputPaths = List.of(args).subList(1, args.length).stream().map(Path::of).toList();

        try {
            mergeUserLibraries(outputPath, inputPaths);
            System.out.println("User libraries merged successfully.");
        } catch (IOException | JDOMException e) {
            System.err.println("Error merging user libraries: " + e.getMessage());
        }
    }

    public static void mergeUserLibraries(Path outputPath, List<Path> inputPaths) throws IOException, JDOMException {
        Element rootElement = new Element("eclipse-userlibraries");
        rootElement.setAttribute("version", "2");

        SAXBuilder builder = new SAXBuilder();

        for (Path inputPath : inputPaths) {
            Document document = builder.build(Files.newInputStream(inputPath));
            Element inputRoot = document.getRootElement();

            if (!inputRoot.getName().equals("eclipse-userlibraries")) {
                throw new JDOMException("Invalid input file format. Expected <eclipse-userlibraries> root element.");
            }

            List<Element> libraryElements = inputRoot.getChildren("library");
            for (Element libraryElement : libraryElements) {
                rootElement.addContent(libraryElement.clone());
            }
        }

        Document outputDocument = new Document(rootElement);
        XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
        outputter.output(outputDocument, Files.newOutputStream(outputPath));
    }
}