package com.strm.movie.telegram;

import com.strm.movie.service.StrmFileService;
import com.strm.movie.telegram.session.SeriesSession;
import com.strm.movie.telegram.session.SeriesSessionManager;
import com.strm.movie.telegram.session.SeriesState;
import com.strm.movie.service.TmdbService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class TelegramBot implements SpringLongPollingBot {

    private final SeriesSessionManager sessionManager;

    private final String botToken ;

    private final TelegramClient telegramClient;

    private final TmdbService tmdbService;

    private final StrmFileService strmFileService;

    public TelegramBot(
            SeriesSessionManager sessionManager,
            TmdbService tmdbService,
            StrmFileService strmFileService,
            @Value("${telegram.bot.token}") String botToken) {
        this.sessionManager = sessionManager;
        this.tmdbService = tmdbService;
        this.strmFileService = strmFileService;
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updates -> {
            for (Update update : updates) {
                handleUpdate(update);
            }
        };
    }

    private void handleUpdate(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Long chatId = update.getMessage().getChatId();

        String message = update.getMessage()
                        .getText()
                        .trim();

        SeriesSession session = sessionManager.getOrCreate(chatId);

        // ==========================================
        // COMMAND
        // ==========================================

        if (message.equalsIgnoreCase("/start")) {
            handleStart(chatId);
            return;
        }

        if (message.equalsIgnoreCase("/series")) {
            handleSeries(chatId);
            return;
        }

        if (message.equalsIgnoreCase("/cancel")) {
            sessionManager.remove(chatId);
            sendMessage(chatId, "❌ Đã hủy phiên tạo series.");
            return;
        }

        // ==========================================
        // STATE
        // ==========================================

        switch (session.getState()) {
            // --------------------------------------
            // CHỌN SERIES MỚI / TIẾP TỤC
            // --------------------------------------

            case WAITING_MODE -> {
                handleMode(chatId, session, message);
            }

            // --------------------------------------
            // NHẬP TÊN
            // --------------------------------------

            case WAITING_TITLE -> {
                handleTitle(chatId, session, message);
            }

            // --------------------------------------
            // NHẬP SEASON
            // --------------------------------------

            case WAITING_SEASON -> {
                handleSeason(chatId, session, message);
            }

            // --------------------------------------
            // NHẬP TẬP BẮT ĐẦU
            // --------------------------------------

            case WAITING_START_EPISODE -> {
                handleStartEpisode(chatId, session, message);
            }

            // --------------------------------------
            // NHẬN LINK
            // --------------------------------------

            case RECEIVING_LINKS -> {

                if (message.equalsIgnoreCase("/done")) {
                    handleDone(chatId, session);
                    return;
                }

                handleLinks(chatId, session, message);
            }

            // --------------------------------------
            // MẶC ĐỊNH
            // --------------------------------------

            default -> {
                sendMessage(
                        chatId,
                        """
                        Hãy dùng /series để bắt đầu.

                        /series
                        /cancel
                        """
                );
            }
        }
    }

    // =================================================
    // /start
    // =================================================

    private void handleStart(Long chatId) {

        sendMessage(
                chatId,
                """
                🤖 STRM Bot

                /series - Tạo TV Series
                """
        );
    }

    // =================================================
    // /series
    // =================================================

    private void handleSeries(Long chatId) {

        /*
         * Tạo session hoàn toàn mới.
         *
         * Nếu user đang có session cũ,
         * session đó sẽ bị thay thế.
         */
        SeriesSession session = sessionManager.createNew(chatId);

        session.setState(SeriesState.WAITING_MODE);

        session.setTitle(null);
        session.setSeason(null);
        session.setStartEpisode(1);
        session.clearLinks();

        sendMessage(
                chatId,
                """
                🎬 TẠO TV SERIES

                Chọn:

                1️⃣ Series mới
                2️⃣ Tiếp tục series

                /cancel - Hủy
                """
        );
    }

    // =================================================
    // CHỌN MODE
    // =================================================

    private void handleMode(Long chatId, SeriesSession session, String message) {

        if (!message.equals("1") && !message.equals("2")) {

            sendMessage(
                    chatId,
                    """
                    ❌ Lựa chọn không hợp lệ.

                    1️⃣ Series mới
                    2️⃣ Tiếp tục series

                    Hãy trả lời 1 hoặc 2.
                    """
            );

            return;
        }

        if (message.equals("1")) {
            // Series mới
            session.setStartEpisode(1);

        } else {
            // Tiếp tục
            // Vẫn hỏi startEpisode ở bước sau
            session.setStartEpisode(1);
        }

        session.setState(SeriesState.WAITING_TITLE);

        sendMessage(
                chatId,
                """
                🎬 Nhập tên TV Series:

                """
        );
    }

    // =================================================
    // NHẬP TÊN
    // =================================================

    private void handleTitle(Long chatId, SeriesSession session, String message) {

        if (message.isBlank()) {
            sendMessage(
                    chatId,
                    "❌ Tên phim không được để trống."
            );

            return;
        }

        session.setTitle(message);

        session.setState(SeriesState.WAITING_SEASON);

        sendMessage(
                chatId,
                """
                📺 Nhập số mùa:
                """
        );
    }

    // =================================================
    // NHẬP SEASON
    // =================================================

    private void handleSeason(Long chatId, SeriesSession session, String message) {
        try {
            int season = Integer.parseInt(message);

            if (season <= 0) {
                throw new NumberFormatException();
            }

            session.setSeason(season);

            session.setState(SeriesState.WAITING_START_EPISODE);

            sendMessage(
                    chatId,
                    """
                    🔢 Tập bắt đầu từ bao nhiêu?
                    """
            );

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Số mùa không hợp lệ. Hãy nhập số nguyên > 0."
            );
        }
    }

    // =================================================
    // NHẬP TẬP BẮT ĐẦU
    // =================================================

    private void handleStartEpisode(Long chatId, SeriesSession session, String message) {
        try {
            int startEpisode = Integer.parseInt(message);

            if (startEpisode <= 0) {
                throw new NumberFormatException();
            }

            session.setStartEpisode(startEpisode);

            session.setState(SeriesState.RECEIVING_LINKS);

            sendMessage(
                    chatId,
                    String.format(
                            """
                            ✅ Đã thiết lập:

                            🎬 %s
                            📺 Season %02d
                            🔢 Bắt đầu từ E%02d

                            🔗 Bây giờ hãy gửi link Cloudstream.

                            Có thể:
                            • Gửi từng link
                            • Hoặc paste nhiều link cùng lúc

                            Khi gửi xong:
                            /done

                            Hủy:
                            /cancel
                            """,
                            session.getTitle(),
                            session.getSeason(),
                            session.getStartEpisode()
                    )
            );

        } catch (NumberFormatException e) {
            sendMessage(chatId, "❌ Số tập không hợp lệ. Hãy nhập số nguyên > 0.");
        }
    }

    // =================================================
    // NHẬN LINKS
    // =================================================

    private void handleLinks(Long chatId, SeriesSession session, String message) {

        /*
         * Cho phép paste nhiều link:
         *
         * https://link-1
         * https://link-2
         * https://link-3
         *
         * Mỗi dòng sẽ là một episode.
         */

        String[] lines = message.split("\\R");

        int added = 0;

        for (String line : lines) {

            String link = line.trim();

            if (link.isBlank()) {
                continue;
            }

            if (!isValidUrl(link)) {
                sendMessage(chatId, "⚠️ Bỏ qua link không hợp lệ:\n" + link);
                continue;
            }

            session.addLink(link);

            added++;
        }

        if (added == 0) {
            sendMessage(chatId, "❌ Không tìm thấy link hợp lệ.");
            return;
        }

        int firstEpisode = session.getStartEpisode() + session.getLinks().size() - added;

        int lastEpisode = session.getStartEpisode() + session.getLinks().size() - 1;

        sendMessage(
                chatId,
                String.format(
                        """
                        ✅ Đã nhận %d link.

                        🎬 %s
                        📺 Season %02d

                        📌 Tập:
                        E%02d → E%02d

                        📦 Tổng hiện tại: %d tập

                        Gửi tiếp link hoặc:
/done
                        """,
                        added,
                        session.getTitle(),
                        session.getSeason(),
                        firstEpisode,
                        lastEpisode,
                        session.getLinks().size()
                )
        );
    }

    // =================================================
    // KIỂM TRA URL
    // =================================================

    private boolean isValidUrl(String url) {

        return url.startsWith("http://") || url.startsWith("https://");
    }

    // =================================================
    // /done
    // =================================================

    private void handleDone(Long chatId, SeriesSession session) {
        if (session.getLinks().isEmpty()) {
            sendMessage(
                    chatId,
                    "❌ Chưa có link tập nào."
            );

            return;
        }

        int startEpisode = session.getStartEpisode();

        int endEpisode = session.getEndEpisode();

        sendMessage(
                chatId,
                String.format(
                        """
                        📦 Đang tạo ZIP...

                        🎬 %s
                        📺 Season %02d
                        🎞️ E%02d → E%02d
                        📊 %d tập

                        ⏳ Vui lòng chờ...
                        """,
                        session.getTitle(),
                        session.getSeason(),
                        startEpisode,
                        endEpisode,
                        session.getLinks().size()
                )
        );

        Path zipFile = null;

        try {

            // ==========================================
            // TẠO ZIP
            // ==========================================

            zipFile = strmFileService.createSeriesZip(
                            session.getTitle(),
                            session.getSeason(),
                            session.getStartEpisode(),
                            session.getLinks()
                    );

            // ==========================================
            // GỬI ZIP
            // ==========================================

            sendFile(chatId, zipFile);

            sendMessage(
                    chatId,
                    String.format(
                            """
                            ✅ Hoàn tất!

                            🎬 %s
                            📺 Season %02d
                            🎞️ E%02d → E%02d
                            📦 %d tập

                            ZIP đã được gửi phía trên.
                            """,
                            session.getTitle(),
                            session.getSeason(),
                            startEpisode,
                            endEpisode,
                            session.getLinks().size()
                    )
            );

            // Xóa session
            sessionManager.remove(chatId);

        } catch (Exception e) {

            e.printStackTrace();

            sendMessage(
                    chatId,
                    """
                    ❌ Không thể tạo file ZIP.

                    Kiểm tra log của ứng dụng để biết lỗi.
                    """
            );

        } finally {

            // ==========================================
            // XÓA ZIP TẠM
            // ==========================================

            if (zipFile != null) {

                try {

                    Files.deleteIfExists(
                            zipFile
                    );

                } catch (Exception e) {

                    e.printStackTrace();
                }
            }
        }
    }

    // =================================================
    // SEND MESSAGE
    // =================================================

    private void sendMessage(Long chatId, String text) {
        SendMessage sendMessage =
                new SendMessage(
                        chatId.toString(),
                        text
                );

        try {

            telegramClient.execute(
                    sendMessage
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =================================================
    // SEND FILE
    // =================================================

    private void sendFile(Long chatId, Path file) {
        try {
            SendDocument sendDocument = new SendDocument(chatId.toString(),
                    new InputFile(file.toFile()));

            telegramClient.execute(sendDocument);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =================================================
    // BOT TOKEN
    // =================================================

    @Override
    public String getBotToken() {
        return botToken;
    }
}