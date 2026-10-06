package com.folkify.quiz.entity;

import com.folkify.infrastructure.persistence.BaseEntity;
import com.folkify.instrument.entity.Lesson;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quiz_questions")
public class QuizQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private QuestionType type = QuestionType.SINGLE;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuizOption> options = new ArrayList<>();

    public QuizQuestion() {}

    public void addOption(QuizOption option) {
        option.setQuestion(this);
        options.add(option);
    }

    public Lesson getLesson() { return lesson; }
    public String getQuestion() { return question; }
    public QuestionType getType() { return type; }
    public String getExplanation() { return explanation; }
    public int getOrderIndex() { return orderIndex; }
    public List<QuizOption> getOptions() { return options; }

    public void setLesson(Lesson lesson) { this.lesson = lesson; }
    public void setQuestion(String question) { this.question = question; }
    public void setType(QuestionType type) { this.type = type; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
