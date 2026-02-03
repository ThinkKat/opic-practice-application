package me.thinkcat.opic.practice.service;

import lombok.RequiredArgsConstructor;
import me.thinkcat.opic.practice.config.FileStorageProperties;
import me.thinkcat.opic.practice.exception.FileStorageException;
import me.thinkcat.opic.practice.exception.ValidationException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    public String storeFile(MultipartFile file, String directory) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = getFileExtension(originalFilename);
        String fileName = UUID.randomUUID().toString() + fileExtension;

        try {
            // 저장 경로 생성
            Path uploadPath = Paths.get(fileStorageProperties.getUploadDir(), directory).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // 파일 저장
            Path targetLocation = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // 상대 경로 반환
            return directory + "/" + fileName;

        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + fileName, e);
        }
    }

    public Resource loadFileAsResource(String filePath) {
        try {
            Path file = Paths.get(fileStorageProperties.getUploadDir(), filePath).toAbsolutePath().normalize();
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("File not found: " + filePath);
            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("File not found: " + filePath, e);
        }
    }

    public void deleteFile(String filePath) {
        try {
            Path file = Paths.get(fileStorageProperties.getUploadDir(), filePath).toAbsolutePath().normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file: " + filePath, e);
        }
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !fileStorageProperties.getAllowedTypes().contains(contentType)) {
            throw new ValidationException("Invalid file type. Allowed types: " + fileStorageProperties.getAllowedTypes());
        }

        long maxSize = parseSize(fileStorageProperties.getMaxFileSize());
        if (file.getSize() > maxSize) {
            throw new ValidationException("File size exceeds maximum allowed size: " + fileStorageProperties.getMaxFileSize());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private long parseSize(String size) {
        if (size == null || size.isEmpty()) {
            return 50 * 1024 * 1024; // Default 50MB
        }
        String upperSize = size.toUpperCase();
        if (upperSize.endsWith("MB")) {
            return Long.parseLong(upperSize.replace("MB", "")) * 1024 * 1024;
        } else if (upperSize.endsWith("KB")) {
            return Long.parseLong(upperSize.replace("KB", "")) * 1024;
        }
        return Long.parseLong(size);
    }
}
