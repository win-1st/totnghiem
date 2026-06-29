package thang.bida.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {
    private Long id;
    private Long tableId;
    private Integer tableNumber;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private Integer numberOfGuests;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private LocalTime endTime;
    private String notes;
    private String status;
    private java.math.BigDecimal depositAmount;
    private Boolean isDepositPaid;
    private String createdAt;
    private String updatedAt;
    private String tableType;
}