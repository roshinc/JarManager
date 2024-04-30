package dev.roshin.tools.download_jars.util;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class MavenMetadataUtility {

	/**
	 * Get the latest version of an artifact from a Maven repository.
	 *
	 * @param baseUrl The base URL of the Maven repository.
	 * @param apiKey  The API key to access the Maven repository.
	 * @return The latest version of the artifact.
	 */
	public static Optional<String> getLatestVersion(String baseUrl, String apiKey) {
		Logger logger = LoggerFactory.getLogger(MavenMetadataUtility.class);
 
		String metadataUrl = String.format("%s/maven-metadata.xml", baseUrl);
		logger.debug("The metadata url is {}", metadataUrl);
 
		HttpClientResponseHandler<Optional<String>> responseHandler = response -> {
			if (response.getCode() == 200) { // Check for HTTP 200 OK
				try (InputStream content = response.getEntity().getContent()) {
					SAXBuilder saxBuilder = new SAXBuilder();
					Document document = saxBuilder.build(content);
					Element metadata = document.getRootElement();
					Element versioning = metadata.getChild("versioning");
					return Optional.ofNullable(versioning.getChildText("release"));
				} catch (JDOMException e) {
					logger.error("Could not parse metadata xml", e);
					throw new RuntimeException("Could not parse metadata xml", e);
				}
			} else {
				throw new RuntimeException("Failed to download artifact: HTTP " + response.getCode());
			}
		};
 
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpGet request = new HttpGet(metadataUrl);
			request.setHeader("X-JFrog-Art-Api", apiKey); // Set API key in the Authorization header
 
			return client.execute(request, responseHandler);
		} catch (IOException e) {
			logger.error("Could not get metadata xml", e);
			return Optional.empty();
		}
	}
}