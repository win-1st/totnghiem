// ProductTypeDTO.java
package thang.bida.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductTypeDTO {
    private Long id;
    private String code;
    private String name;
    private String icon;
    private String description;
    private Integer sortOrder;
    private Boolean isActive;
}