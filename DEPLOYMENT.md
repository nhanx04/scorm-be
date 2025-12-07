# Hướng dẫn Triển khai (Deployment)

## 1. Triển khai trên máy chủ Linux

### Bước 1: Chuẩn bị máy chủ

```bash
# Cập nhật hệ thống
sudo apt-get update
sudo apt-get upgrade -y

# Cài đặt Java
sudo apt-get install openjdk-11-jdk -y

# Cài đặt Maven (nếu cần build trên server)
sudo apt-get install maven -y

# Cài đặt MySQL (nếu sử dụng MySQL)
sudo apt-get install mysql-server -y
```

### Bước 2: Tạo database

```bash
mysql -u root -p
```

```sql
CREATE DATABASE scormdb;
CREATE USER 'scorm_user'@'localhost' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON scormdb.* TO 'scorm_user'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

### Bước 3: Build ứng dụng

```bash
# Clone repository
git clone <your-repo-url>
cd scorm-be

# Build JAR
mvn clean package -DskipTests
```

### Bước 4: Tạo thư mục cho ứng dụng

```bash
sudo mkdir -p /opt/scorm-api
sudo chown $USER:$USER /opt/scorm-api
cp target/scorm-package-generator-1.0.0.jar /opt/scorm-api/
```

### Bước 5: Tạo file cấu hình production

```bash
cp src/main/resources/application-prod.yml.example /opt/scorm-api/application-prod.yml
```

Chỉnh sửa `/opt/scorm-api/application-prod.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/scormdb
    username: scorm_user
    password: strong_password
  jpa:
    database-platform: org.hibernate.dialect.MySQL8Dialect
    hibernate:
      ddl-auto: update

jwt:
  secret: your-very-long-random-secret-key-here-change-this
  expiration: 86400000
```

### Bước 6: Tạo systemd service

```bash
sudo nano /etc/systemd/system/scorm-api.service
```

Thêm nội dung:

```ini
[Unit]
Description=SCORM Package Generator API
After=network.target

[Service]
Type=simple
User=scorm
WorkingDirectory=/opt/scorm-api
ExecStart=java -jar /opt/scorm-api/scorm-package-generator-1.0.0.jar --spring.config.location=file:/opt/scorm-api/application-prod.yml
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

### Bước 7: Khởi động service

```bash
# Reload systemd
sudo systemctl daemon-reload

# Khởi động service
sudo systemctl start scorm-api

# Bật auto-start
sudo systemctl enable scorm-api

# Kiểm tra status
sudo systemctl status scorm-api
```

### Bước 8: Cấu hình Nginx (reverse proxy)

```bash
sudo nano /etc/nginx/sites-available/scorm-api
```

Thêm nội dung:

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location /api {
        proxy_pass http://localhost:8080/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Kích hoạt site:

```bash
sudo ln -s /etc/nginx/sites-available/scorm-api /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### Bước 9: Cấu hình SSL với Let's Encrypt

```bash
sudo apt-get install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
```

---

## 2. Triển khai trên Docker

### Bước 1: Tạo Dockerfile

```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/scorm-package-generator-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Bước 2: Tạo docker-compose.yml

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: scormdb
      MYSQL_USER: scorm_user
      MYSQL_PASSWORD: scorm_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

  scorm-api:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/scormdb
      SPRING_DATASOURCE_USERNAME: scorm_user
      SPRING_DATASOURCE_PASSWORD: scorm_password
      JWT_SECRET: your-secret-key-here
    depends_on:
      - mysql
    volumes:
      - scorm_packages:/root/scorm-packages

volumes:
  mysql_data:
  scorm_packages:
```

### Bước 3: Build và chạy

```bash
# Build image
docker-compose build

# Chạy containers
docker-compose up -d

# Kiểm tra logs
docker-compose logs -f scorm-api
```

---

## 3. Triển khai trên Heroku

### Bước 1: Cài đặt Heroku CLI

```bash
curl https://cli-assets.heroku.com/install.sh | sh
heroku login
```

### Bước 2: Tạo Procfile

```
web: java -Dserver.port=$PORT $JAVA_OPTS -jar target/scorm-package-generator-1.0.0.jar
```

### Bước 3: Tạo app trên Heroku

```bash
heroku create your-app-name
```

### Bước 4: Thêm add-ons

```bash
# Thêm MySQL
heroku addons:create cleardb:ignite

# Kiểm tra DATABASE_URL
heroku config
```

### Bước 5: Deploy

```bash
git push heroku main
```

---

## 4. Monitoring và Maintenance

### Kiểm tra logs

```bash
# Linux
sudo journalctl -u scorm-api -f

# Docker
docker-compose logs -f scorm-api
```

### Backup database

```bash
# MySQL
mysqldump -u scorm_user -p scormdb > backup.sql

# Restore
mysql -u scorm_user -p scormdb < backup.sql
```

### Update ứng dụng

```bash
# Build version mới
mvn clean package -DskipTests

# Copy JAR mới
cp target/scorm-package-generator-1.0.0.jar /opt/scorm-api/

# Restart service
sudo systemctl restart scorm-api
```

---

## 5. Bảo mật

### Thay đổi JWT Secret

Tạo secret mạnh:

```bash
openssl rand -base64 32
```

Cập nhật trong `application-prod.yml`:

```yaml
jwt:
  secret: <your-generated-secret>
```

### Firewall

```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

### HTTPS

Luôn sử dụng HTTPS trong production. Cấu hình Let's Encrypt như hướng dẫn ở trên.

---

## 6. Performance Tuning

### JVM Options

```bash
java -Xmx512m -Xms256m -jar scorm-package-generator-1.0.0.jar
```

### Database Connection Pool

Cập nhật `application-prod.yml`:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000
```

---

## Troubleshooting

| Vấn đề | Giải pháp |
|--------|----------|
| Port đã được sử dụng | Thay đổi port hoặc kill process |
| Database connection failed | Kiểm tra credentials và firewall |
| Out of memory | Tăng JVM heap size |
| Slow response | Tối ưu database queries |

---

**Chúc bạn triển khai thành công!** 🚀

