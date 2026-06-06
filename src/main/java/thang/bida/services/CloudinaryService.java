package thang.bida.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    // Method cũ - upload file thông thường
    public String uploadFile(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.emptyMap());
            return result.get("url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Upload file thất bại", e);
        }
    }

    // Method cũ - upload ảnh với folder
    public Map uploadImage(MultipartFile file, String folderName) {
        try {
            Map params = ObjectUtils.asMap(
                    "folder", folderName,
                    "resource_type", "auto");
            return cloudinary.uploader().upload(file.getBytes(), params);
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh thất bại", e);
        }
    }

    // ===== THÊM CÁC METHOD MỚI CHO PRODUCT SERVICE =====

    // Upload ảnh cho sản phẩm (có productId để tạo folder riêng)
    public String uploadProductImage(MultipartFile file, Long productId) {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", "products/" + productId,
                    "public_id", "product_" + productId + "_" + System.currentTimeMillis(),
                    "overwrite", true,
                    "resource_type", "image");

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return result.get("secure_url").toString(); // Dùng secure_url (HTTPS)
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh sản phẩm thất bại: " + e.getMessage());
        }
    }

    // Xóa ảnh trên Cloudinary theo URL
    public void deleteProductImage(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String publicId = extractPublicIdFromUrl(imageUrl);
                if (publicId != null) {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    System.out.println("✅ Đã xóa ảnh Cloudinary: " + publicId);
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Xóa ảnh thất bại: " + e.getMessage());
        }
    }

    // Upload ảnh không cần productId (dùng tạm)
    public String uploadProductImageTemp(MultipartFile file) {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "folder", "products/temp",
                    "resource_type", "image");

            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), uploadParams);
            return result.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Upload ảnh tạm thất bại: " + e.getMessage());
        }
    }

    // Trích xuất public_id từ URL Cloudinary
    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            if (imageUrl == null || imageUrl.isEmpty()) {
                return null;
            }

            // URL mẫu:
            // https://res.cloudinary.com/.../upload/v123456/products/1/product_1_123456.jpg
            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                // Loại bỏ version (v123456/)
                int versionIndex = path.indexOf("/");
                if (versionIndex != -1) {
                    path = path.substring(versionIndex + 1);
                }
                // Loại bỏ extension
                int dotIndex = path.lastIndexOf(".");
                if (dotIndex != -1) {
                    path = path.substring(0, dotIndex);
                }
                return path;
            }
        } catch (Exception e) {
            System.err.println("Lỗi extract public_id: " + e.getMessage());
        }
        return null;
    }
}