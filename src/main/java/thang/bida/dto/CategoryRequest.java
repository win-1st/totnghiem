package thang.bida.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    @Size(max = 500)
    private String imageUrl;
}