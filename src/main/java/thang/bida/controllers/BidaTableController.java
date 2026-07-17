package thang.bida.controllers;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import thang.bida.dto.TableDTO;
import thang.bida.dto.TableRequest;
import thang.bida.model.BidaTable;
import thang.bida.services.BidaTableService;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class BidaTableController {

    private final BidaTableService tableService;

    public BidaTableController(BidaTableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    public ResponseEntity<?> getAllTables() {
        List<TableDTO> tables = tableService.getAllTableDTO();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tables);
        response.put("count", tables.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{state}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> getTablesByStatus(@PathVariable String state) {
        List<BidaTable> tables;

        switch (state.toUpperCase()) {
            case "FREE":
                tables = tableService.getFreeTables();
                break;
            case "OCCUPIED":
                tables = tableService.getOccupiedTables();
                break;
            case "RESERVED":
                tables = tableService.getReservedTables();
                break;
            case "MAINTENANCE":
                tables = tableService.getMaintenanceTables();
                break;
            default:
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Trạng thái không hợp lệ. Chấp nhận: FREE, OCCUPIED, RESERVED, MAINTENANCE");
                return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tables);
        response.put("count", tables.size());
        response.put("status", state.toUpperCase());
        response.put("message", "Lấy danh sách bàn " + getStatusText(state) + " thành công");

        return ResponseEntity.ok(response);
    }

    private String getStatusText(String status) {
        switch (status.toUpperCase()) {
            case "FREE":
                return "trống";
            case "OCCUPIED":
                return "đang sử dụng";
            case "RESERVED":
                return "đã đặt";
            case "MAINTENANCE":
                return "đang bảo trì";
            default:
                return status;
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CUSTOMER')")
    public ResponseEntity<?> getTableById(@PathVariable Long id) {
        try {
            TableDTO table = tableService.getTableDTOById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", table);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> createTable(@RequestBody TableRequest request) {
        try {
            BidaTable table = new BidaTable();
            table.setTableName(request.getName() != null ? request.getName() : "Bàn " + request.getNumber());
            table.setNumber(request.getNumber());
            table.setCapacity(request.getCapacity());
            table.setStatus(BidaTable.TableStatus.FREE);
            table.setType(request.getType() != null ? request.getType() : "STANDARD");
            table.setVersion(0);

            BidaTable savedTable = tableService.createTable(table);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thêm bàn thành công");
            response.put("data", savedTable);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // =====================================================
    // ✅ THÊM ENDPOINT CẬP NHẬT BÀN
    // =====================================================
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> updateTable(
            @PathVariable Long id,
            @RequestBody TableRequest request) {
        try {
            BidaTable updatedTable = tableService.updateTable(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật bàn thành công");
            response.put("data", updatedTable);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam BidaTable.TableStatus status) {
        try {
            BidaTable updatedTable = tableService.updateTableStatus(id, status);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái thành công");
            response.put("data", updatedTable);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> deleteTable(@PathVariable Long id) {
        try {
            tableService.deleteTable(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa bàn thành công");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}