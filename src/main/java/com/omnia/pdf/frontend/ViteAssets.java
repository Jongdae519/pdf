package com.omnia.pdf.frontend;

import java.util.List;

public record ViteAssets(
        String viteJs,
        List<String> viteCssList
) {
    private static final String DIST_PATH = "./dist/";

    public static ViteAssets of(String viteJsFile, List<String> viteCssFileList) {
        String viteJs = DIST_PATH + viteJsFile;
        List<String> viteCssList = viteCssFileList
                .stream()
                .map(cssFile -> DIST_PATH + cssFile)
                .toList();

        return new ViteAssets(viteJs, viteCssList);
    }
}
