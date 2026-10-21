package dk.campusbornholm.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "dishes")
@Getter
@Setter
@NoArgsConstructor
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Double price;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "source_url")
    private String sourceUrl;

    public Dish(String name, String description, Double price, LocalDate date, String sourceUrl) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.date = date;
        this.sourceUrl = sourceUrl;
    }
}
