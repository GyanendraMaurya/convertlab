package com.convertlab.convertlab_backend.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Log4j2
@Configuration
@Profile("prod")
public class AwsS3Config {

    @Value("${aws.s3.region}")
    private String region;

    /**
     * Uses the standard AWS credential chain in order:
     *  1. Environment variables:      AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
     *  2. Java system properties:     aws.accessKeyId / aws.secretAccessKey
     *  3. ~/.aws/credentials file     (local dev)
     *  4. EC2 instance profile / IAM role (production — preferred, no keys needed)
     */
    @Bean
    public S3Client s3Client() {
        S3Client client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
//                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        log.info("S3Client initialised for region: {}", region);
        return client;
    }
}
