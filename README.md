# StudyDocs Manager Backend

Backend API cho hệ thống quản lý tài liệu StudyDocs, xây dựng với Java Spring Boot.

## 1) Công nghệ chính

- Java 21
- Spring Boot
- Spring Security + JWT
- Spring Data JPA (MySQL)
- Redis
- Elasticsearch
- MinIO (object storage)
- Maven

## 2) Yêu cầu môi trường

Cài sẵn:

- JDK 21
- Maven 3.9+
- MySQL 8+
- Redis
- MinIO
- (Tuỳ chọn) Elasticsearch

## 3) Cấu hình ứng dụng

### 3.1 Nguyên tắc bảo mật config

- **Không commit secret thật** vào git.
- `application.properties` chỉ giữ placeholder (`${ENV_VAR}`).
- Secret đặt trong:
    - environment variables, hoặc
    - `src/main/resources/application-local.properties` (được ignore bởi git).

### 3.2 Tạo file local config

Tạo file:

`src/main/resources/application-local.properties`

Ví dụ:

```properties
DB_URL=jdbc:mysql://localhost:3306/studydocs_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your-db-password

JWT_SECRET=your-very-long-random-secret
JWT_EXPIRATION=86400000

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

REDIS_HOST=localhost
REDIS_PORT=6379

ELASTICSEARCH_URIS=http://localhost:9200

MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=studydocs-documents

STORAGE_PROVIDER=minio
SPRING_PROFILES_ACTIVE=local