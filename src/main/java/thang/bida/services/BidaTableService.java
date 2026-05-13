package thang.bida.services;

import org.springframework.stereotype.Service;

import thang.bida.dto.TableDTO;
import thang.bida.model.BidaTable;
import thang.bida.repository.BidaTableRepository;
import thang.bida.repository.OrderRepository;

import java.util.List;

@Service
public class BidaTableService {

    private final BidaTableRepository tableRepository;
    private final OrderRepository orderRepository;

    public BidaTableService(
            BidaTableRepository tableRepository,
            OrderRepository orderRepository) {
        this.tableRepository = tableRepository;
        this.orderRepository = orderRepository;
    }

    public List<BidaTable> getAllTables() {
        return tableRepository.findAll();
    }

    public List<BidaTable> getAvailableTables() {
        return tableRepository.findByStatus(BidaTable.TableStatus.FREE);
    }

    public BidaTable updateTableStatus(Long tableId, BidaTable.TableStatus status) {
        BidaTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));
        table.setStatus(status);
        return tableRepository.save(table);
    }

    // ✅ Thêm hàm này để tạo bàn mới
    public BidaTable createTable(BidaTable table) {
        return tableRepository.save(table);
    }

    public List<BidaTable> getFreeTables() {
        return getAvailableTables();
    }

    public List<TableDTO> getAllTableDTO() {
        return tableRepository.findAll().stream().map(table -> {
            TableDTO dto = new TableDTO();

            dto.setId(table.getId());
            dto.setNumber(table.getNumber());
            dto.setTableName(table.getTableName());
            dto.setCapacity(table.getCapacity());
            dto.setStatus(table.getStatus());

            if (table.getStatus() == BidaTable.TableStatus.OCCUPIED) {
                orderRepository.findActiveOrderByTable(table.getId())
                        .stream()
                        .findFirst()
                        .ifPresent(order -> {
                            dto.setCurrentOrderId(order.getId());
                            dto.setStartTime(order.getStartTime());
                        });
            }

            return dto;
        }).toList();
    }
}
