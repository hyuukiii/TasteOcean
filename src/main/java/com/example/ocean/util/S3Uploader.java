package com.example.ocean.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final AmazonS3 amazonS3; // ✅ 변수명 통일 (amazonS3Client → amazonS3)

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public String upload(MultipartFile file, String dirName) {
        String originalFileName = file.getOriginalFilename();
        String fileName = dirName + "/" + UUID.randomUUID() + "_" + originalFileName;

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

    public byte[] download(String key) throws IOException {
        S3Object object = amazonS3.getObject(bucket, key);
        try (S3ObjectInputStream input = object.getObjectContent()) {
            return IOUtils.toByteArray(input);
        }
    }
}
