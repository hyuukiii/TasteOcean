package com.example.ocean.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
public class S3Uploader {

    @Autowired(required = false)
    private AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    
    @Value("${file.upload-dir}")
    private String localUploadDir;
    
    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    public String upload(MultipartFile file, String dirName) {
        String originalFileName = file.getOriginalFilename();
        String fileName = dirName + "/" + UUID.randomUUID() + "_" + originalFileName;
        
        // 로컬 개발 환경이거나 S3 클라이언트가 없으면 로컬 파일 시스템 사용
        if ("local".equals(activeProfile) || amazonS3 == null) {
            return uploadToLocal(file, fileName);
        }
        
        // 프로덕션 환경에서는 S3 사용
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try {
            amazonS3.putObject(new PutObjectRequest(bucket, fileName, file.getInputStream(), metadata));
            return amazonS3.getUrl(bucket, fileName).toString();
        } catch (IOException e) {
            throw new RuntimeException("S3 파일 업로드 실패: " + originalFileName, e);
        }
    }
    
    private String uploadToLocal(MultipartFile file, String fileName) {
        try {
            // 로컬 디렉토리 생성
            Path uploadPath = Paths.get(localUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // 파일 저장
            Path filePath = uploadPath.resolve(fileName);
            Path parentDir = filePath.getParent();
            if (!Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(file.getBytes());
            }
            
            log.info("파일 저장 완료: {}", filePath);
            // 파일 이름만 반환 (전체 경로가 아닌)
            return fileName;
            
        } catch (IOException e) {
            log.error("로컬 파일 업로드 실패: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("로컬 파일 업로드 실패: " + file.getOriginalFilename(), e);
        }
    }

    public byte[] download(String key) throws IOException {
        // 로컬 개발 환경이거나 S3 클라이언트가 없으면 로컬 파일 시스템에서 읽기
        if ("local".equals(activeProfile) || amazonS3 == null) {
            log.info("로컬 파일 다운로드 시도 - key: {}", key);
            
            Path filePath = Paths.get(key);
            
            // 절대 경로가 아니면 로컬 업로드 디렉토리와 결합
            if (!filePath.isAbsolute()) {
                filePath = Paths.get(localUploadDir, key);
            }
            
            log.info("최종 파일 경로: {}", filePath);
            
            if (!Files.exists(filePath)) {
                log.error("파일이 존재하지 않습니다: {}", filePath);
                throw new IOException("파일을 찾을 수 없습니다: " + filePath);
            }
            
            return Files.readAllBytes(filePath);
        }
        
        // 프로덕션 환경에서는 S3에서 읽기
        S3Object object = amazonS3.getObject(bucket, key);
        try (S3ObjectInputStream input = object.getObjectContent()) {
            return IOUtils.toByteArray(input);
        }
    }
}