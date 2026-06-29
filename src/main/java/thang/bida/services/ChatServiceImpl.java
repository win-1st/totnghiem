package thang.bida.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import thang.bida.model.BidaTable;
import thang.bida.repository.BidaTableRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private BidaTableRepository bidaTableRepository;

    @Override
    public String reply(String message, String tableCode) {
        if (message == null || message.isBlank()) {
            return "🤖 Bạn muốn hỏi gì nè? Tôi sẵn sàng giúp bạn!";
        }

        String msg = message.toLowerCase().trim();

        // ========== GIÁ BÀN (ƯU TIÊN CAO NHẤT) ==========
        if (msg.contains("giá") || msg.contains("bao nhiêu") ||
                msg.contains("tiền") || msg.contains("chi phí") ||
                msg.contains("giá bàn") || msg.contains("giá cả") ||
                msg.contains("bàn bao nhiêu")) {

            // Lấy danh sách bàn từ database để lấy giá
            List<BidaTable> tables = bidaTableRepository.findAll();

            // Nếu chưa có dữ liệu trong database, dùng giá mặc định
            if (tables.isEmpty()) {
                return "💰 Bảng giá bàn bida:\n" +
                        "┌─────────────────────────┐\n" +
                        "│ 🟢 Bàn Standard: 50.000đ/giờ │\n" +
                        "│ 🔵 Bàn VIP: 100.000đ/giờ    │\n" +
                        "│ 🟣 Bàn Pro: 150.000đ/giờ    │\n" +
                        "└─────────────────────────┘\n" +
                        "💡 Giảm 20% cho lần đặt đầu tiên!";
            }

            // Lấy giá từ database (giả sử giá được lưu trong bàn hoặc bảng riêng)
            // Ở đây tôi lấy giá mặc định theo loại bàn
            StringBuilder response = new StringBuilder();
            response.append("💰 Bảng giá bàn bida:\n");
            response.append("┌─────────────────────────┐\n");

            // Nhóm bàn theo loại và lấy giá
            tables.stream()
                    .map(BidaTable::getType)
                    .distinct()
                    .forEach(type -> {
                        String price = getPriceByType(type);
                        String emoji = getEmojiByType(type);
                        response.append(String.format("│ %s %s: %s    │\n",
                                emoji, type != null ? type : "Standard", price));
                    });

            response.append("└─────────────────────────┘\n");
            response.append("💡 Giảm 20% cho lần đặt đầu tiên!");
            return response.toString();
        }

        // ========== CHÀO HỎI ==========
        if (msg.contains("xin chào") || msg.contains("chào") ||
                msg.contains("hello") || msg.contains("hi") || msg.contains("hey")) {
            // Lấy số bàn trống hiện tại
            long freeTables = bidaTableRepository.countByStatus(BidaTable.TableStatus.FREE);

            return "👋 Xin chào! Tôi là trợ lý ảo của Win BIDA.\n" +
                    "Tôi có thể giúp bạn:\n" +
                    "💰 Giá bàn bida\n" +
                    "🕐 Giờ mở cửa\n" +
                    "📍 Cách đặt bàn\n" +
                    "🎁 Khuyến mãi hiện tại\n" +
                    "🪑 Bàn trống (" + freeTables + " bàn đang trống)\n" +
                    "Bạn muốn hỏi gì nhỉ?";
        }

        // ========== DANH SÁCH BÀN TRỐNG (LẤY TỪ DATABASE) ==========
        if (msg.contains("bàn trống") || msg.contains("trống") ||
                msg.contains("bàn nào") || msg.contains("còn bàn") ||
                msg.contains("bàn free")) {

            // Lấy danh sách bàn trống từ database
            List<BidaTable> freeTables = bidaTableRepository.findByStatus(BidaTable.TableStatus.FREE);

            // Kiểm tra nếu không có bàn trống
            if (freeTables.isEmpty()) {
                return "😅 Hiện tại không có bàn trống. Bạn có thể:\n" +
                        "1️⃣ Đặt bàn trước để giữ chỗ\n" +
                        "2️⃣ Quay lại sau ít phút\n" +
                        "3️⃣ Liên hệ hotline để được hỗ trợ\n\n" +
                        "📞 Hotline: 1900.xxxx";
            }

            // Đếm số bàn theo loại
            long standardCount = freeTables.stream()
                    .filter(t -> "STANDARD".equalsIgnoreCase(t.getType())).count();
            long premiumCount = freeTables.stream()
                    .filter(t -> "PREMIUM".equalsIgnoreCase(t.getType())).count();
            long vipCount = freeTables.stream()
                    .filter(t -> "VIP".equalsIgnoreCase(t.getType())).count();

            // Tạo danh sách chi tiết
            String tableList = freeTables.stream()
                    .map(t -> String.format("   ✅ Bàn %d (%s)",
                            t.getNumber(),
                            t.getType() != null ? t.getType() : "Standard"))
                    .collect(Collectors.joining("\n"));

            return "🪑 Hiện tại có các bàn trống:\n" +
                    "─────────────────────────\n" +
                    tableList + "\n" +
                    "─────────────────────────\n" +
                    "📊 Tổng: " + freeTables.size() + " bàn trống\n" +
                    "   🟢 STANDARD: " + standardCount + " bàn\n" +
                    "   🟡 PREMIUM: " + premiumCount + " bàn\n" +
                    "   🔵 VIP: " + vipCount + " bàn\n\n" +
                    "👉 Hãy đặt ngay để giữ chỗ!\n" +
                    "📱 Đặt bàn tại: [Đặt bàn]";
        }

        // ========== GIỜ MỞ CỬA ==========
        if (msg.contains("giờ mở") || msg.contains("mở cửa") ||
                msg.contains("đóng cửa") || msg.contains("giờ làm") ||
                msg.contains("thời gian") || msg.contains("mấy giờ")) {
            return "⏰ Win BIDA mở cửa:\n" +
                    "🟢 09:00 - 23:00 (Thứ 2 - Thứ 6)\n" +
                    "🟢 08:00 - 02:00 (Thứ 7 - Chủ Nhật)\n" +
                    "📅 Quán mở cửa tất cả các ngày trong tuần!";
        }

        // ========== CÁCH ĐẶT BÀN ==========
        if (msg.contains("đặt bàn") || msg.contains("đặt") ||
                msg.contains("book") || msg.contains("reserve") ||
                msg.contains("đặt chỗ") || msg.contains("cách đặt")) {
            return "📱 Cách đặt bàn tại Win BIDA:\n" +
                    "1️⃣ Đăng nhập tài khoản\n" +
                    "2️⃣ Chọn bàn yêu thích\n" +
                    "3️⃣ Chọn ngày và khung giờ\n" +
                    "4️⃣ Xác nhận đặt bàn\n\n" +
                    "✅ Đặt bàn nhanh tại đây: [Đặt bàn]";
        }

        // ========== KHUYẾN MÃI ==========
        if (msg.contains("khuyến mãi") || msg.contains("giảm giá") ||
                msg.contains("ưu đãi") || msg.contains("sale") ||
                msg.contains("deal") || msg.contains("hot")) {
            return "🎁 Các chương trình khuyến mãi:\n" +
                    "🔥 Giảm 20% cho lần đặt đầu tiên\n" +
                    "🎂 Giảm 30% vào ngày sinh nhật\n" +
                    "👥 Giảm 15% cho nhóm từ 4 người\n" +
                    "💳 Tích điểm đổi quà hấp dẫn";
        }

        // ========== THỰC ĐƠN ==========
        if (msg.contains("thực đơn") || msg.contains("đồ uống") ||
                msg.contains("đồ ăn") || msg.contains("món") ||
                msg.contains("nước") || msg.contains("bia")) {
            return "🍺 Thực đơn Win BIDA:\n" +
                    "🥤 Nước ngọt: 20.000đ\n" +
                    "☕ Cà phê: 25.000đ\n" +
                    "🍺 Bia: 30.000đ - 50.000đ\n" +
                    "🍟 Snack: 35.000đ - 60.000đ\n" +
                    "🍕 Pizza: 80.000đ - 150.000đ\n\n" +
                    "📋 Xem thực đơn đầy đủ tại đây: [Thực đơn]";
        }

        // ========== THANH TOÁN ==========
        if (msg.contains("thanh toán") || msg.contains("trả tiền") ||
                msg.contains("pay") || msg.contains("chuyển khoản") ||
                msg.contains("momo")) {
            return "💳 Win BIDA hỗ trợ thanh toán:\n" +
                    "💰 Tiền mặt\n" +
                    "🏦 Chuyển khoản ngân hàng\n" +
                    "📱 Momo, ZaloPay\n" +
                    "🔒 Bảo mật và an toàn 100%!";
        }

        // ========== CHÍNH SÁCH ==========
        if (msg.contains("chính sách") || msg.contains("hủy") ||
                msg.contains("hủy bàn") || msg.contains("hoàn tiền")) {
            return "📋 Chính sách Win BIDA:\n" +
                    "✅ Hủy bàn trước 1 giờ: Miễn phí\n" +
                    "⏰ Hủy bàn trước 30 phút: Phí 50%\n" +
                    "❌ Hủy dưới 30 phút: Phí 100%\n" +
                    "💡 Vui lòng hủy đúng hạn để được miễn phí!";
        }

        // ========== LIÊN HỆ ==========
        if (msg.contains("liên hệ") || msg.contains("sđt") ||
                msg.contains("điện thoại") || msg.contains("hotline") ||
                msg.contains("zalo") || msg.contains("fb") ||
                msg.contains("facebook")) {
            return "📞 Liên hệ Win BIDA:\n" +
                    "📱 Hotline: 1900.xxxx\n" +
                    "📧 Email: winbida@gmail.com\n" +
                    "📘 Facebook: fb.com/winbida\n" +
                    "💬 Zalo: 0988.xxx.xxx\n\n" +
                    "🕐 Tư vấn 24/7!";
        }

        // ========== CẢM ƠN ==========
        if (msg.contains("cảm ơn") || msg.contains("thanks") ||
                msg.contains("thank") || msg.contains("tks") ||
                msg.contains("cám ơn")) {
            return "❤️ Không có gì! Rất vui được giúp bạn.\n" +
                    "Nếu cần thêm gì, cứ hỏi nhé! Chúc bạn có trải nghiệm tuyệt vời tại Win BIDA 🎱✨";
        }

        // ========== DEFAULT ==========
        // Lấy số bàn trống để hiển thị
        long freeTables = bidaTableRepository.countByStatus(BidaTable.TableStatus.FREE);

        return "🤔 Mình chưa hiểu lắm câu hỏi của bạn.\n\n" +
                "Bạn có thể hỏi tôi về:\n" +
                "💰 Giá bàn bida\n" +
                "⏰ Giờ mở cửa\n" +
                "📱 Cách đặt bàn\n" +
                "🎁 Khuyến mãi\n" +
                "🍺 Thực đơn\n" +
                "🪑 Bàn trống (" + freeTables + " bàn)\n" +
                "💳 Thanh toán\n\n" +
                "💡 Hoặc nhập: 'help' để xem hướng dẫn!";
    }

    // ========== HELPER METHODS ==========

    private String getPriceByType(String type) {
        if (type == null)
            return "50.000đ/giờ";
        switch (type.toUpperCase()) {
            case "STANDARD":
                return "50.000đ/giờ";
            case "PREMIUM":
                return "100.000đ/giờ";
            case "VIP":
                return "150.000đ/giờ";
            default:
                return "50.000đ/giờ";
        }
    }

    private String getEmojiByType(String type) {
        if (type == null)
            return "🟢";
        switch (type.toUpperCase()) {
            case "STANDARD":
                return "🟢";
            case "PREMIUM":
                return "🟡";
            case "VIP":
                return "🔵";
            default:
                return "🟢";
        }
    }
}