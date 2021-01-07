package se325.assignment01.concert.service.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@Table(name = "CONCERTS")
public class Concert implements Comparable<Concert> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "IMAGE_NAME")
    private String imageName;

    @Column(name = "BLURB", length = 1024)
    private String blurb;

    @ElementCollection
    @CollectionTable(
            name = "CONCERT_DATES",
            joinColumns = @JoinColumn(name = "CONCERT_ID"))
    @Column(name = "DATE")
    private Set<LocalDateTime> dates = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "CONCERT_PERFORMER",
            joinColumns = @JoinColumn(name = "CONCERT_ID"),
            inverseJoinColumns = @JoinColumn(name = "PERFORMER_ID"))
    private Set<Performer> performers = new HashSet<>();

    public Concert() {
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImageName() {
        return imageName;
    }

    public String getBlurb() {
        return blurb;
    }

    public Set<LocalDateTime> getDates() {
        return Collections.unmodifiableSet(dates);
    }

    public Set<Performer> getPerformers() {
        return Collections.unmodifiableSet(performers);
    }

    public boolean isScheduledOn(LocalDateTime date) {
        return dates.contains(date);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Concert))
            return false;
        if (obj == this)
            return true;

        Concert rhs = (Concert) obj;
        return new EqualsBuilder().
                append(title, rhs.getTitle()).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(title).hashCode();
    }

    @Override
    public int compareTo(Concert concert) {
        return title.compareTo(concert.getTitle());
    }
}
