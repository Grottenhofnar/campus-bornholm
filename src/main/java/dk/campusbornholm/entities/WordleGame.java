package dk.campusbornholm.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wordle_games",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "word_id"}))
@Getter
@Setter
@NoArgsConstructor
public class WordleGame {
    public static final int MAX_ATTEMPTS = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "word_id")
    private Word word;

    @Column(nullable = false)
    private boolean solved;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("attemptNumber ASC")
    private List<Guess> guesses = new ArrayList<>();

    public WordleGame(User user, Word word) {
        this.user = user;
        this.word = word;
    }

    @PrePersist
    private void onCreate() {
        this.startedAt = LocalDateTime.now();
    }

    public void addGuess(Guess guess) {
        guesses.add(guess);
        guess.setGame(this);
        attempts = guesses.size();
    }

    public boolean isFinished() {
        return solved || attempts >= MAX_ATTEMPTS;
    }
}
