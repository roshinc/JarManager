package dev.roshin.tools.config.util;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class MavenSettingsParser {

    public static Server getServerCredentials(String serverId) {
        try {
            SAXBuilder builder = new SAXBuilder();
            File xmlFile = new File(System.getProperty("user.home") + "/.m2/settings.xml");
            Document document = builder.build(xmlFile);
            Element rootNode = document.getRootElement();

            // Find credentials in <servers>
            String username = null;
            String password = null;
            Namespace ns = rootNode.getNamespace();
            List<Element> servers = rootNode.getChild("servers", ns).getChildren("server", ns);
            for (Element server : servers) {
                if (server.getChildText("id",ns).equals(serverId)) {
                    username = server.getChildText("username",ns);
                    password = server.getChildText("password",ns);
                    break;
                }
            }

            // Find URL in <profiles>
            String url = null;
            List<Element> profiles = rootNode.getChild("profiles", ns).getChildren("profile",ns);
            for (Element profile : profiles) {
                List<Element> repositories = profile.getChild("repositories",ns).getChildren("repository",ns);
                for (Element repository : repositories) {
                    if (repository.getChildText("id",ns).equals(serverId)) {
                        url = repository.getChildText("url",ns);
                        break;
                    }
                }
                if (url != null) {
                    break;
                }
            }

            if (username != null && password != null && url != null) {
                return new Server(serverId, username, password, url);
            }
        } catch (Exception e) {
           LoggerFactory.getLogger(MavenSettingsParser.class).error("Error parsing Maven settings", e);
        }
        return null;
    }

    public record Server(String id, String username, String password, String url) {
    }
}
