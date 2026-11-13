package com.ecom.winners.notifiers;

import com.ecom.winners.dto.UserDTO;
import com.ecom.winners.entity.Winner;
import com.ecom.winners.repositories.WinnerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

/**
 * Nghe topic {@code lucky_winner} và lưu mỗi người thắng vào bảng
 * {@code winners} để dashboard có thể hiển thị danh sách kết quả.
 * Payload là JSON của UserDTO, parse bằng Jackson 3 (Spring Boot 4).
 */
@Component
public class WinnerRecorder {
    private static final Logger log = LoggerFactory.getLogger(WinnerRecorder.class);
    private final WinnerRepository winners;
    private final ObjectMapper json;

    public WinnerRecorder(WinnerRepository winners, ObjectMapper json) {
        this.winners = winners;
        this.json = json;
    }

    @KafkaListener(topics = "${spring.kafka.template.default-topic}",
            containerFactory = "winnerListenerFactory", groupId = "winner-recorder")
    public void record(String payload) {
        UserDTO winner = json.readValue(payload, UserDTO.class);
        Winner record = new Winner();
        record.setUserId(winner.getId());
        record.setName(winner.getName());
        record.setUsername(winner.getUsername());
        record.setPhone(winner.getPhone());
        record.setWebsite(winner.getWebsite());
        record.setCompanyName(winner.getCompany() != null ? winner.getCompany().getName() : null);
        record.setCity(winner.getAddress() != null ? winner.getAddress().getCity() : null);
        record.setWonAt(LocalDateTime.now());
        winners.save(record);
        log.info("Đã ghi lại người thắng #{}: {}", winner.getId(), winner.getName());
    }
}
