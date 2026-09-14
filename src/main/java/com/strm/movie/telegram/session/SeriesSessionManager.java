package com.strm.movie.telegram.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SeriesSessionManager {

    private final Map<Long, SeriesSession> sessions =
            new ConcurrentHashMap<>();

    /**
     * Lấy session hiện tại.
     * Nếu chưa có thì tạo mới.
     */
    public SeriesSession getOrCreate(Long chatId) {

        return sessions.computeIfAbsent(
                chatId,
                SeriesSession::new
        );
    }

    /**
     * Lấy session.
     * Trả về null nếu chưa tồn tại.
     */
    public SeriesSession get(Long chatId) {

        return sessions.get(chatId);
    }

    /**
     * Kiểm tra user có session hay chưa.
     */
    public boolean exists(Long chatId) {

        return sessions.containsKey(chatId);
    }

    /**
     * Tạo một session mới hoàn toàn.
     *
     * Dùng khi user chọn:
     * "🆕 Series mới"
     *
     * Nếu user đang có session cũ thì session cũ
     * sẽ bị thay thế.
     */
    public SeriesSession createNew(Long chatId) {

        SeriesSession session =
                new SeriesSession(chatId);

        sessions.put(
                chatId,
                session
        );

        return session;
    }

    /**
     * Xóa session.
     *
     * Dùng sau khi /done thành công
     * hoặc user muốn hủy.
     */
    public void remove(Long chatId) {

        sessions.remove(chatId);
    }

    /**
     * Xóa toàn bộ session.
     *
     * Hiện tại chưa cần dùng nhưng có thể
     * hữu ích sau này.
     */
    public void clear() {

        sessions.clear();
    }
}