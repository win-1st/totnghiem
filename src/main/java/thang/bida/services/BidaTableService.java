package thang.bida.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import thang.bida.dto.TableDTO;
import thang.bida.dto.TableRequest;
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

    public List<BidaTable> getOccupiedTables() {
        return tableRepository.findByStatus(BidaTable.TableStatus.OCCUPIED);
    }

    public List<BidaTable> getReservedTables() {
        return tableRepository.findByStatus(BidaTable.TableStatus.RESERVED);
    }

    public List<BidaTable> getMaintenanceTables() {
        return tableRepository.findByStatus(BidaTable.TableStatus.MAINTENANCE);
    }

    public List<BidaTable> getTablesByStatus(BidaTable.TableStatus status) {
        return tableRepository.findByStatus(status);
    }

    @Transactional
    public BidaTable updateTableStatus(Long tableId, BidaTable.TableStatus status) {
        BidaTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + tableId));
        table.setStatus(status);
        return tableRepository.save(table);
    }

    public BidaTable createTable(BidaTable table) {
        BidaTable existingTable = tableRepository.findByNumber(table.getNumber());
        if (existingTable != null) {
            throw new RuntimeException("Số bàn " + table.getNumber() + " đã tồn tại!");
        }
        return tableRepository.save(table);
    }

    // =====================================================
    // ✅ THÊM METHOD CẬP NHẬT BÀN
    // =====================================================
    @Transactional
    public BidaTable updateTable(Long id, TableRequest request) {
        BidaTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + id));

        // Kiểm tra số bàn trùng (nếu thay đổi số bàn)
        if (!table.getNumber().equals(request.getNumber())) {
            BidaTable existingTable = tableRepository.findByNumber(request.getNumber());
            if (existingTable != null) {
                throw new RuntimeException("Số bàn " + request.getNumber() + " đã tồn tại!");
            }
        }

        // Cập nhật thông tin
        table.setTableName(request.getName() != null ? request.getName() : "Bàn " + request.getNumber());
        table.setNumber(request.getNumber());
        table.setCapacity(request.getCapacity());
        if (request.getType() != null) {
            table.setType(request.getType());
        }

        return tableRepository.save(table);
    }

    @Transactional
    public void deleteTable(Long tableId) {
        BidaTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + tableId));

        if (table.getStatus() == BidaTable.TableStatus.OCCUPIED) {
            throw new RuntimeException("Không thể xóa bàn đang được sử dụng!");
        }

        tableRepository.delete(table);
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
            dto.setType(table.getType());

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

    public TableDTO getTableDTOById(Long id) {
        BidaTable table = tableRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn với ID: " + id));

        TableDTO dto = new TableDTO();
        dto.setId(table.getId());
        dto.setNumber(table.getNumber());
        dto.setTableName(table.getTableName());
        dto.setCapacity(table.getCapacity());
        dto.setStatus(table.getStatus());
        dto.setType(table.getType());

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
    }
}