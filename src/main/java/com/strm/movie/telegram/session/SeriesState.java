package com.strm.movie.telegram.session;

public enum SeriesState {

    IDLE,

    // Bot đang hỏi:
    // 1 = Series mới
    // 2 = Tiếp tục
    WAITING_MODE,

    // Đang chờ tên phim
    WAITING_TITLE,

    // Đang chờ season
    WAITING_SEASON,

    // Đang chờ tập bắt đầu
    WAITING_START_EPISODE,

    // Đang nhận các link Cloudstream
    RECEIVING_LINKS
}