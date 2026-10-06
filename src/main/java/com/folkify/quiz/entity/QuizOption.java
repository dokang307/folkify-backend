package com.folkify.quiz.entity;

import com.folkify.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "quiz_options")
public class QuizOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    public QuizOption() {}

    public QuizOption(String text, boolean correct, int orderIndex) {
        this.text = text;
        this.correct = correct;
        this.orderIndex = orderIndex;
    }

    public QuizQuestion getQuestion() { return question; }
    public String getText() { return text; }
    public boolean isCorrect() { return correct; }
    public int getOrderIndex() { return orderIndex; }

    void setQuestion(QuizQuestion question) { this.question = question; }
}
