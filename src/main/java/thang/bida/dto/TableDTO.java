package thang.bida.dto;

import java.time.LocalDateTime;

import thang.bida.model.BidaTable;

import lombok.Data;

@Data
public class TableDTO {
    private Long id;
    private Integer number;
    private String tableName;
    private Integer capacity;
    private BidaTable.TableStatus status;
    private Long currentOrderId;
    private LocalDateTime startTime;

    // getter / setter
}
