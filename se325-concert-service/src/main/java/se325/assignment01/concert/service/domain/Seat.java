package se325.assignment01.concert.service.domain;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name="SEATS")
public class Seat {

	@Id
	@GeneratedValue/*(strategy = GenerationType.IDENTITY)*/
	@Column(name="ID")
	private Long id;

	@Column(name="LABEL", nullable = false)
	private String label;

	@Column(name="IS_BOOKED")
	private boolean isBooked;

	@Column(name="DATE", nullable = false)
	private LocalDateTime date;

	@Column(name="PRICE")
	private BigDecimal price;

	@Version
	@Column(name="VERSION")
	private long version;
	
	public Seat() {}

	public Seat(String label, boolean isBooked, LocalDateTime date, BigDecimal price) {
		this.label = label;
		this.isBooked = isBooked;
		this.date = date;
		this.price = price;
	}

	public Long getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public boolean isBooked() {
		return isBooked;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void book() {
		this.isBooked = true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Seat seat = (Seat) o;
		return Objects.equals(label, seat.label) &&
				Objects.equals(date, seat.date);
	}

	@Override
	public int hashCode() {
		return Objects.hash(label, date);
	}
}
