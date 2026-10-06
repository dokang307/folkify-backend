package com.folkify.quiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Cấu hình quiz, bind từ prefix "folkify.quiz". */
@Component
@ConfigurationProperties(prefix = "folkify.quiz")
public class QuizProperties {

    /** Điểm phần trăm tối thiểu để tính là qua bài (và hoàn thành lesson). */
    private int passPercent = 70;

    public int getPassPercent() { return passPercent; }
    public void setPassPercent(int passPercent) { this.passPercent = passPercent; }
}
