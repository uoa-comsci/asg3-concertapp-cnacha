package se325.assignment01.concert.service.domain;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import se325.assignment01.concert.common.types.Genre;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@Table(name = "PERFORMERS")
public class Performer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "IMAGE_NAME")
    private String imageName;

    @Enumerated(EnumType.STRING)
    @Column(name = "GENRE")
    private Genre genre;

    @Column(name = "BLURB", length = 1024)
    private String blurb;

    @ManyToMany(mappedBy = "performers")
    private Set<Concert> performingIn = new HashSet<>();

    public Performer() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImageName() {
        return imageName;
    }

    public Genre getGenre() {
        return genre;
    }

    public String getBlurb() {
        return blurb;
    }

    public Set<Concert> getConcerts() {
        return Collections.unmodifiableSet(performingIn);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Performer))
            return false;
        if (obj == this)
            return true;

        Performer rhs = (Performer) obj;
        return new EqualsBuilder().
                append(name, rhs.getName()).
                append(genre, rhs.getGenre()).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(name).
                append(genre).
                hashCode();
    }
}
