package com.wildcard.storage;

import com.wildcard.common.BusinessException;
import com.wildcard.config.WildcardProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class StorageService {

    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "webp");

    private final Path root;

    public StorageService(WildcardProperties props) {
        String dir = props.getStorage().getLocalDir();
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory: " + root, e);
        }
    }

    public StoredFile storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("No file was uploaded");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("Image is larger than 2 MB");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (!ALLOWED.contains(ext)) {
            throw new BusinessException("Only jpg, png and webp images are allowed");
        }

        String name = UUID.randomUUID().toString().replace("-", "")
                + "_" + Instant.now().toEpochMilli() + "." + ext;

        try {
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("Invalid file name");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Could not store the image");
        }

        return new StoredFile(name, "/files/" + name, file.getSize());
    }

    private String extensionOf(String original) {
        if (original == null) {
            return "";
        }
        int dot = original.lastIndexOf('.');
        if (dot < 0 || dot == original.length() - 1) {
            return "";
        }
        return original.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
