package se325.assignment01.concert.common.dto;

public class ConcertInfoNotificationDTO {

    private int numSeatsRemaining;

    public ConcertInfoNotificationDTO() {
    }

    public ConcertInfoNotificationDTO(int numSeatsRemaining) {
        this.numSeatsRemaining = numSeatsRemaining;
    }

    public int getNumSeatsRemaining() {
        return numSeatsRemaining;
    }

    public void setNumSeatsRemaining(int numSeatsRemaining) {
        this.numSeatsRemaining = numSeatsRemaining;
    }
}
