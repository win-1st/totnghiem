package thang.bida.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import thang.bida.model.Category;
import thang.bida.payload.request.CategoryRequest;
import thang.bida.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Tạo category từ DTO
    public Category createCategory(CategoryRequest request) {
        // Kiểm tra tên category đã tồn tại chưa
        List<Category> existing = categoryRepository.findByNameContainingIgnoreCase(request.getName());
        if (!existing.isEmpty()) {
            throw new RuntimeException("Category với tên '" + request.getName() + "' đã tồn tại");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setIsActive(true); // Mặc định active

        return categoryRepository.save(category);
    }

    // Cập nhật category từ DTO
    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy category với ID: " + id));

        // Kiểm tra tên mới có trùng với category khác không (trừ chính nó)
        if (!category.getName().equals(request.getName())) {
            List<Category> existing = categoryRepository.findByNameContainingIgnoreCase(request.getName());
            if (!existing.isEmpty() && !existing.get(0).getId().equals(id)) {
                throw new RuntimeException("Category với tên '" + request.getName() + "' đã tồn tại");
            }
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());

        // Chỉ cập nhật imageUrl nếu có giá trị mới
        if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            category.setImageUrl(request.getImageUrl());
        }

        return categoryRepository.save(category);
    }

    // Xóa vĩnh viễn category
    public boolean permanentDeleteCategory(Long id) {
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Lấy tất cả categories
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // Lấy category theo ID
    public Optional<Category> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    // Lấy categories active
    public List<Category> getActiveCategories() {
        // Cần thêm phương thức này vào repository
        return categoryRepository.findAll().stream()
                .filter(Category::getIsActive)
                .toList();
    }

    // Tìm kiếm category theo tên
    public List<Category> searchCategories(String keyword) {
        return categoryRepository.findByNameContainingIgnoreCase(keyword);
    }

    // Toggle trạng thái active/inactive
    public Category toggleCategoryStatus(Long id, Boolean isActive) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy category với ID: " + id));

        category.setIsActive(isActive);
        return categoryRepository.save(category);
    }
}