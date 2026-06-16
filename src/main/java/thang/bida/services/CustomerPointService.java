package thang.bida.services;

import thang.bida.dto.CustomerPointDTO;
import thang.bida.model.CustomerPoint;
import thang.bida.repository.CustomerPointRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerPointService {

    @Autowired
    private CustomerPointRepository customerPointRepository;

    private CustomerPointDTO convertToDTO(CustomerPoint entity) {
        CustomerPointDTO dto = new CustomerPointDTO();
        dto.setId(entity.getId());
        dto.setPhone(entity.getPhone());
        dto.setCustomerName(entity.getCustomerName());
        dto.setTotalPoints(entity.getTotalPoints());
        dto.setTotalHoursPlayed(entity.getTotalHoursPlayed());
        // Sửa lỗi: dùng getCreatedAt() và getUpdatedAt() (có sẵn từ @Getter)
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        return dto;
    }

    public List<CustomerPointDTO> getAllCustomers() {
        return customerPointRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CustomerPointDTO getCustomerById(Long id) {
        CustomerPoint customer = customerPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));
        return convertToDTO(customer);
    }

    public CustomerPointDTO getCustomerByPhone(String phone) {
        CustomerPoint customer = customerPointRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với SĐT: " + phone));
        return convertToDTO(customer);
    }

    @Transactional
    public CustomerPointDTO createCustomer(CustomerPointDTO dto) {
        if (customerPointRepository.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("Số điện thoại " + dto.getPhone() + " đã tồn tại!");
        }

        // SỬA: Dùng constructor có 3 tham số (phone, customerName, totalPoints)
        CustomerPoint customer = new CustomerPoint(
                dto.getPhone(),
                dto.getCustomerName(),
                dto.getTotalPoints() != null ? dto.getTotalPoints() : 0);

        CustomerPoint saved = customerPointRepository.save(customer);
        return convertToDTO(saved);
    }

    @Transactional
    public CustomerPointDTO updateCustomer(Long id, CustomerPointDTO dto) {
        CustomerPoint existing = customerPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        if (!existing.getPhone().equals(dto.getPhone()) &&
                customerPointRepository.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("Số điện thoại " + dto.getPhone() + " đã thuộc về khách hàng khác!");
        }

        existing.setPhone(dto.getPhone());
        existing.setCustomerName(dto.getCustomerName());
        existing.setTotalPoints(dto.getTotalPoints());
        // updatedAt sẽ tự động cập nhật nhờ @PreUpdate

        CustomerPoint updated = customerPointRepository.save(existing);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        CustomerPoint existing = customerPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));
        customerPointRepository.delete(existing);
    }

    @Transactional
    public CustomerPointDTO addPoints(String phone, int points) {
        int updated = customerPointRepository.addPoints(phone, points);
        if (updated == 0) {
            throw new RuntimeException("Không tìm thấy khách hàng với SĐT: " + phone);
        }
        return getCustomerByPhone(phone);
    }

    @Transactional
    public CustomerPointDTO redeemPoints(String phone, int points) {
        int updated = customerPointRepository.redeemPoints(phone, points);
        if (updated == 0) {
            throw new RuntimeException("Không tìm thấy khách hàng hoặc không đủ điểm!");
        }
        return getCustomerByPhone(phone);
    }

    @Transactional
    public CustomerPointDTO addHoursPlayed(String phone, int hours) {
        int updated = customerPointRepository.addHoursPlayed(phone, hours);
        if (updated == 0) {
            throw new RuntimeException("Không tìm thấy khách hàng với SĐT: " + phone);
        }
        return getCustomerByPhone(phone);
    }
}