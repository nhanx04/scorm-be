package com.scorm.generator.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
public class AwsS3Service {

    // Chúng ta xóa bỏ S3Client và bucketName vì không dùng đến nữa

    /**
     * Thay vì upload lên S3, hàm này sẽ lưu file vào thư mục 'uploads' ngay tại máy
     * của bạn.
     *
     * @param fileData    Nội dung file dưới dạng byte array.
     * @param fileName    Tên file muốn lưu (ví dụ: scorm_package_123.zip).
     * @param contentType Loại file (giữ lại để đúng chuẩn hàm cũ, dù không dùng).
     * @return Đường dẫn tuyệt đối tới file đã lưu trên máy tính.
     */
    public String uploadFile(byte[] fileData, String fileName, String contentType) {
        try {
            // 1. Xác định thư mục lưu trữ là thư mục 'uploads' trong thư mục gốc dự án
            String currentDir = System.getProperty("user.dir");
            File uploadDir = new File(currentDir, "uploads");

            // Tạo thư mục nếu chưa tồn tại
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (!created) {
                    System.err.println("Không thể tạo thư mục uploads!");
                }
            }

            // 2. Làm sạch tên file (chỉ lấy tên file, bỏ phần đường dẫn folder ảo nếu có)
            String cleanFileName = new File(fileName).getName();
            File destFile = new File(uploadDir, cleanFileName);

            // 3. Ghi dữ liệu ra file
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                fos.write(fileData);
            }

            System.out.println("✅ [LOCAL STORAGE] Đã lưu file thành công tại: " + destFile.getAbsolutePath());

            // Trả về đường dẫn tuyệt đối để bạn dễ dàng tìm thấy file
            return destFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi khi lưu file cục bộ: " + e.getMessage());
        }
    }

    /**
     * Xóa file khỏi ổ cứng dựa trên đường dẫn file.
     *
     * @param fileUrl Đường dẫn tuyệt đối của file cần xóa.
     */
    public void deleteFileFromUrl(String fileUrl) {
        try {
            File file = new File(fileUrl);
            if (file.exists()) {
                if (file.delete()) {
                    System.out.println("✅ [LOCAL STORAGE] Đã xóa file: " + fileUrl);
                } else {
                    System.err.println("❌ [LOCAL STORAGE] Không thể xóa file: " + fileUrl);
                }
            } else {
                System.out.println("⚠️ [LOCAL STORAGE] File không tồn tại để xóa: " + fileUrl);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa file cục bộ: " + e.getMessage());
        }
    }
}