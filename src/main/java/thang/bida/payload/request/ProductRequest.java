package thang.bida.payload.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;

    private BigDecimal price;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    private Integer stockQuantity = 0;

    private String imageUrl;

    private Boolean active = true;

    private MultipartFile image;

    // Thêm 2 field cho TIME_BASED
    private String productType = "FOOD";

    private BigDecimal pricePerMinute;
}