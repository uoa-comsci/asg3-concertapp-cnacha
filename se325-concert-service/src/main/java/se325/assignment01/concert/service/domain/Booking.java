package se325.assignment01.concert.service.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@Table(name="BOOKINGS")
public class Booking {
	
	public enum Status {Soft, Confirmed, Cancelled}
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="ID")
	private Long id;
	
	@Column(name="DATE")
	private LocalDateTime date;
	
	@Column(name="TIME_CREATED")
	private LocalDateTime timeCreated;
	
	@OneToMany()
	@JoinColumn(name="BOOKING_ID")
	private Set<Seat> seats;
	
	@ManyToOne
	@JoinColumn(name="CONCERT_ID")
	private Concert concert;
	
	@ManyToOne
	@JoinColumn(name="USER_ID")
	private User user;
	
	@Version
	@Column(name="VERSION")
	private long version;

	public Booking() {}
	
	public Booking(LocalDateTime concertDate, Collection<Seat> seats, Concert concert, User user) {
		// Initialise the Booking.
		date = concertDate;
		
		this.seats = new HashSet<>();
		this.seats.addAll(seats);
		
		this.concert = concert;
		this.user = user;
		
		// Record the current time for creating the Booking.
		this.timeCreated = LocalDateTime.now();
	}
	
	public Long getId() {
		return id;
	}
	
	public LocalDateTime getDate() {
		return date;
	}
	
	public LocalDateTime getTimeCreated() {
		return timeCreated;
	}
	
	public Set<Seat> getSeats() {
		return Collections.unmodifiableSet(seats);
	}
	
	public Concert getConcert() {
		return concert;
	}
	
	public User getReservedFor() {
		return user;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Concert))
            return false;
        if (obj == this)
            return true;

        Booking rhs = (Booking) obj;
        return new EqualsBuilder().
            append(timeCreated, rhs.getTimeCreated()).
            append(date, rhs.getDate()).
            append(seats, rhs.getSeats()).
            isEquals();
	}
	
	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 31). 
	            append(timeCreated).
	            append(date).
	            append(seats).
	            hashCode();
	}
}
