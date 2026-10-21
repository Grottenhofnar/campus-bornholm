package dk.campusbornholm.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "guesses")
@Getter
@Setter
@NoArgsConstructor
public class Guess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private WordleGame game;

    @Column(name = "guess_text", nullable = false, length = 5)
    private String guessText;

    @Column(nullable = false, length = 5)
    private String result;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    public Guess(String guessText, String result, int attemptNumber) {
        this.guessText = guessText.toLowerCase();
        this.result = result;
        this.attemptNumber = attemptNumber;
    }
}
