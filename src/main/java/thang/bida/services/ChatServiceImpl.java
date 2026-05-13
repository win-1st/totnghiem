package thang.bida.services;

import org.springframework.stereotype.Service;

@Service
public class ChatServiceImpl implements ChatService {

    @Override
    public String reply(String message, String tableCode) {
        if (message == null || message.isBlank()) {
            return "🤖 Bạn muốn hỏi gì nè?";
        }

        String msg = message.toLowerCase();

        if (msg.contains("giá")) {
            return "💰 Giá bàn bida: 60.000đ / giờ";
        }

        if (msg.contains("giờ")) {
            return "⏰ Quán mở cửa từ 9h sáng đến 2h sáng";
        }

        return "🤔 Mình chưa hiểu lắm, bạn hỏi lại nha!";
    }
}