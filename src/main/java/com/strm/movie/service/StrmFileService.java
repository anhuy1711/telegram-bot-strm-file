package com.strm.movie.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class StrmFileService {

    public Path createSeriesZip(
            String title,
            int season,
            int startEpisode,
            List<String> links
    ) throws IOException {

        String safeTitle = sanitizeFileName(title);

        Path tempDirectory = Files.createTempDirectory("strm_series_");

        // Thư mục:
        // Silo/
        // └── Season 01/
        Path seriesDirectory = tempDirectory.resolve(safeTitle);

        Path seasonDirectory = seriesDirectory.resolve(
                        String.format(
                                "Season %02d",
                                season
                        )
                );

        Files.createDirectories(seasonDirectory);

        try {

            // =========================
            // Tạo các file STRM
            // =========================

            for (int i = 0; i < links.size(); i++) {

                int episode = startEpisode + i;

                String fileName = String.format(
                                "%s S%02dE%02d.strm",
                                safeTitle,
                                season,
                                episode
                        );

                Path strmFile = seasonDirectory.resolve(fileName);

                Files.writeString(strmFile, links.get(i), StandardCharsets.UTF_8);
            }

            // =========================
            // Tạo ZIP
            // =========================

            String zipFileName = String.format("%s S%02d.zip", safeTitle, season);

            Path zipDirectory = Files.createTempDirectory("strm_zip_");

            Path zipFile = zipDirectory.resolve(zipFileName);

            try (
                    OutputStream outputStream =
                            Files.newOutputStream(zipFile);

                    ZipOutputStream zipOutputStream =
                            new ZipOutputStream(outputStream)
            ) {

                Files.walk(seriesDirectory)
                        .filter(Files::isRegularFile)
                        .forEach(file -> {
                            try {
                                String entryName = tempDirectory
                                                .relativize(file)
                                                .toString()
                                                .replace(
                                                        "\\",
                                                        "/"
                                                );

                                ZipEntry entry = new ZipEntry(entryName);

                                zipOutputStream.putNextEntry(entry);

                                Files.copy(file, zipOutputStream);

                                zipOutputStream.closeEntry();

                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }

            return zipFile;

        } finally {

            // Chỉ xóa thư mục STRM tạm.
            // ZIP vẫn được giữ để Telegram gửi.
            deleteDirectory(tempDirectory);
        }
    }

    private String sanitizeFileName(String name) {

        if (name == null || name.isBlank()) {
            return "Unknown";
        }

        return name.replaceAll(
                        "[\\\\/:*?\"<>|]",
                        ""
                )
                .trim();
    }

    private void deleteDirectory(Path directory)
            throws IOException {

        if (!Files.exists(directory)) {
            return;
        }

        try (var stream = Files.walk(directory)) {

            stream
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {

                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}