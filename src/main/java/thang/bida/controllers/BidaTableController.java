package thang.bida.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<List<BidaTable>> getAllTables() {
        return ResponseEntity.ok(tableService.getAllTables());
    }

    @GetMapping("/free")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<List<BidaTable>> getFreeTables() {
        return ResponseEntity.ok(tableService.getFreeTables());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<BidaTable> createTable(@RequestBody BidaTable table) {
        return ResponseEntity.ok(tableService.createTable(table));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestParam BidaTable.TableStatus status) {
        tableService.updateTableStatus(id, status);
        return ResponseEntity.ok().build();
    }
}