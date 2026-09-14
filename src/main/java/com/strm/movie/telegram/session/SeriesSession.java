package com.strm.movie.telegram.session;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SeriesSession {

    private Long chatId;

    /**
     * Trạng thái hiện tại của session
     */
    private SeriesState state = SeriesState.IDLE;

    /**
     * Tên phim
     */
    private String title;

    /**
     * Season
     */
    private Integer season;

    /**
     * Tập bắt đầu
     *
     * Ví dụ:
     * startEpisode = 6
     *
     * 3 link sẽ tạo:
     * E06
     * E07
     * E08
     */
    private int startEpisode = 1;

    /**
     * Danh sách link Cloudstream
     */
    private List<String> links = new ArrayList<>();

    public SeriesSession(Long chatId) {
        this.chatId = chatId;
    }

    /**
     * Thêm một link
     */
    public void addLink(String link) {
        links.add(link);
    }

    /**
     * Thêm nhiều link
     */
    public void addLinks(List<String> links) {
        this.links.addAll(links);
    }

    /**
     * Xóa toàn bộ link
     */
    public void clearLinks() {
        links.clear();
    }

    /**
     * Số lượng tập hiện tại
     */
    public int getEpisodeCount() {
        return links.size();
    }

    /**
     * Tập cuối cùng sẽ được tạo
     *
     * Ví dụ:
     * startEpisode = 6
     * links = 5
     *
     * => E10
     */
    public int getEndEpisode() {
        if (links.isEmpty()) {
            return startEpisode - 1;
        }

        return startEpisode + links.size() - 1;
    }
}