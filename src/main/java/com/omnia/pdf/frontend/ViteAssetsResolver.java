package com.omnia.pdf.frontend;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class ViteAssetsResolver {

    private final Resource manifestResource;
    private final ObjectMapper objectMapper;
    private ViteAssets viteAssets;

    public ViteAssetsResolver(
            @Value("classpath:static/dist/.vite/manifest.json") Resource manifestResource,
            ObjectMapper objectMapper
    ) {
        this.manifestResource = manifestResource;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (!manifestResource.exists()) {
            throw new IllegalStateException("Vite manifest.json not found.");
        }

        try (InputStream is = manifestResource.getInputStream()) {
            JsonNode rootNode = objectMapper.readTree(is);
            String viteJsFile = rootNode.path("index.js").path("file").asText();
            List<String> viteCssFileList = rootNode.path("index.js").path("css")
                    .valueStream()
                    .map(jsonNode -> jsonNode.asText())
                    .toList();

            this.viteAssets = ViteAssets.of(viteJsFile, viteCssFileList);

            System.out.println("ViteAssets: " + this.viteAssets);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getViteJs() {
        return this.viteAssets.viteJs();
    }

    public List<String> getViteCss() {
        return this.viteAssets.viteCssList();
    }

}
