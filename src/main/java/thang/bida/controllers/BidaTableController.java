package thang.bida.controllers;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import thang.bida.model.BidaTable;
import thang.bida.services.BidaTableService;
import thang.bida.payload.request.TableRequest;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class BidaTableController {

    private final BidaTableService tableService;

    public BidaTableController(BidaTableService tableService) {
        this.tableService = tableService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getAllTables() {
        List<BidaTable> tables = tableService.getAllTables();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tables);
        response.put("count", tables.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/free")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getFreeTables() {
        List<BidaTable> tables = tableService.getFreeTables();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", tables);

        return ResponseEntity.ok(response);
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
            table.setType("STANDARD"); // Mặc định
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