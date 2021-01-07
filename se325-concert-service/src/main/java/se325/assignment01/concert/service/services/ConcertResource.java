package se325.assignment01.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se325.assignment01.concert.common.dto.*;
import se325.assignment01.concert.common.types.BookingStatus;
import se325.assignment01.concert.service.domain.*;
import se325.assignment01.concert.service.jaxrs.LocalDateTimeParam;
import se325.assignment01.concert.service.mapper.BookingMapper;
import se325.assignment01.concert.service.mapper.ConcertMapper;
import se325.assignment01.concert.service.mapper.PerformerMapper;
import se325.assignment01.concert.service.mapper.SeatMapper;
import se325.assignment01.concert.service.util.TheatreLayout;

import javax.persistence.*;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

@Path("/concert-service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConcertResource {

    private static final Map<ConcertInfoSubscriptionDTO, AsyncResponse> SUBSCRIPTIONS = new HashMap<>();
    private static final ExecutorService SUBSCRIPTIONS_THREAD_POOL = Executors.newFixedThreadPool(5);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);

    // Concerts
    // ----------------------------------------------------------------------------------------------------------

    @GET
    @Path("/concerts/{id}")
    public ConcertDTO getConcert(@PathParam("id") long id) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();
            Concert domainConcert = em.find(Concert.class, id);
            em.getTransaction().commit();

            if (domainConcert == null) {
                throw new NotFoundException(Response.status(Status.NOT_FOUND).build());
            }

            return ConcertMapper.toDTO(domainConcert);

        } finally {
            em.close();
        }
    }

    /**
     * Gets all concerts.
     *
     * @return a 200 OK response, with all concerts in the system in the body.
     */
    @GET
    @Path("/concerts")
    public Response getConcerts() {
        return getConcertsAs(ConcertMapper::toDTO);
    }

    /**
     * Gets summaries of all concerts.
     *
     * @return a 200 OK response, with all concert summaries in the system in the body.
     */
    @GET
    @Path("/concerts/summaries")
    public Response getConcertSummaries() {
        return getConcertsAs(ConcertMapper::toSummaryDTO);
    }

    private <TDTO> Response getConcertsAs(Function<Concert, TDTO> mappingFunction) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {

            em.getTransaction().begin();
            List<Concert> domainConcerts = em.createQuery("select c from Concert c", Concert.class).getResultList();
            em.getTransaction().commit();

            return Response.ok(getListGenericEntity(domainConcerts, mappingFunction)).build();

        } finally {
            em.close();
        }
    }

    // ----------------------------------------------------------------------------------------------------------


    // Performers
    // ----------------------------------------------------------------------------------------------------------
    @GET
    @Path("/performers/{id}")
    public PerformerDTO getPerformer(@PathParam("id") long id) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();
            Performer domainPerformer = em.find(Performer.class, id);
            em.getTransaction().commit();

            if (domainPerformer == null) {
                throw new NotFoundException(Response.status(Status.NOT_FOUND).build());
            }

            return PerformerMapper.toDTO(domainPerformer);

        } finally {
            em.close();
        }
    }

    @GET
    @Path("/performers")
    public Response getPerformers() {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {

            em.getTransaction().begin();
            List<Performer> domainPerformers = em.createQuery("select p from Performer p", Performer.class).getResultList();
            em.getTransaction().commit();

            return Response.ok(getListGenericEntity(domainPerformers, PerformerMapper::toDTO)).build();

        } finally {
            em.close();
        }
    }
    // ----------------------------------------------------------------------------------------------------------


    // Booking & Authentication
    // ----------------------------------------------------------------------------------------------------------

    /**
     * Attempts to authenticate the user.
     * <p>
     * If there's a username / password match, a new auth token will be generated and returned. Otherwise, a
     * 401 unauthorized error will be returned.
     *
     * @param dtoUser the username / password details.
     * @return a 200 response and a cookie if authenticated, a 401 error if not.
     */
    @POST
    @Path("/login")
    public Response login(UserDTO dtoUser) {

        LOGGER.info("login(): Provided details: " + dtoUser);

        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();

            // Try to get user from DB.
            User domainUser = em.createQuery("select u from User u where u.username = :username and u.password = :password", User.class)
                    .setParameter("username", dtoUser.getUsername())
                    .setParameter("password", dtoUser.getPassword())
                    .getSingleResult();

            // If success, update the user's UUID and return a cookie.
            NewCookie authCookie = makeAuthCookie();
            domainUser.setUUId(authCookie.getValue());
            em.merge(domainUser);
            em.getTransaction().commit();

            return Response.ok().cookie(authCookie).build();
        } catch (NoResultException | NonUniqueResultException e) {
            throw new NotAuthorizedException(Response.status(Status.UNAUTHORIZED).build());
        } finally {
            em.close();
        }
    }

    /**
     * Gets all seats with the given booking status on the given date.
     *
     * @param dateParam the date
     * @return a 200 response with all booked seats.
     */
    @GET
    @Path("/seats/{date}")
    public Response getSeats(@PathParam("date") LocalDateTimeParam dateParam,
                             @DefaultValue("Any") @QueryParam("status") BookingStatus status) {

        LocalDateTime date = dateParam.getLocalDateTime();

        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();

            List<Seat> domainSeats;

            if (status == BookingStatus.Any) {
                domainSeats = em
                        .createQuery("select s from Seat s where s.date = :date", Seat.class)
                        .setParameter("date", date)
                        .getResultList();
            } else {
                domainSeats = em
                        .createQuery("select s from Seat s where s.date = :date and s.isBooked = :isBooked", Seat.class)
                        .setParameter("date", date)
                        .setParameter("isBooked", status == BookingStatus.Booked)
                        .getResultList();
            }

            em.getTransaction().commit();

            return Response.ok(getListGenericEntity(domainSeats, SeatMapper::toDTO)).build();

        } finally {
            em.close();
        }
    }

    /**
     * Attempts to book the specified seats at the specified concert on the specified date.
     * Details are contained within the {@link BookingRequestDTO} object.
     * <p>
     * A valid user must be authenticated for bookings to proceed. If not, a 401 unauthorized error will be returned.
     * <p>
     * The concert being booked must match the date. If not, or if the concert doesn't exist, a 400 bad request error will be thrown.
     * <p>
     * A booking is considered successful if and only if all requested seats are still available at the time the
     * booking is made. If the booking is successful, a 201 Created response will be returned, pointing to the new
     * booking information.
     * <p>
     * If the booking is not successful due to some seats already being taken, a 403 Forbidden response is returned. No seats
     * will be booked in this case - partial successes are not possible.
     *
     * @param bookingRequest the request containing the concert id, date, and list of seats to book
     * @param authCookie     the authentication token for a successfully authenticated user
     * @return 201 with a link to the completed booking, 403 if any requested seats are unavailable, 400 for bad concert / date, 401 for auth error
     */
    @POST
    @Path("/bookings")
    public Response makeBooking(BookingRequestDTO bookingRequest, @CookieParam("auth") Cookie authCookie) {

        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();

            // The user that's making the booking
            User domainUser = lookupUserByAuthenticationToken(authCookie, em);

            // The concert being booked
            Concert domainConcert = em.find(Concert.class, bookingRequest.getConcertId());
            if (domainConcert == null || !domainConcert.isScheduledOn(bookingRequest.getDate())) {
                throw new BadRequestException(Response.status(Status.BAD_REQUEST).build());
            }

            // Get all seat labels
            List<String> seatLabels = bookingRequest.getSeatLabels();

            // Get all unbooked seats with matching labels and dates
            List<Seat> domainSeats = em
                    .createQuery("select s from Seat s where s.date = :date and s.isBooked = false and s.label in (:labels)", Seat.class)
                    .setParameter("date", bookingRequest.getDate())
                    .setParameter("labels", seatLabels)
                    .getResultList();

            // The number of requested seats and the number of unbooked seats should be the same. If not, get outta here.
            if (seatLabels.size() != domainSeats.size()) {
                em.getTransaction().rollback();
                return Response.status(Status.FORBIDDEN).build();
            }

            // Update the booked status of all the seats
            domainSeats.forEach(seat -> {
                seat.book();
                em.merge(seat);
            });

            // Make the booking object
            Booking domainBooking = new Booking(bookingRequest.getDate(), domainSeats, domainConcert, domainUser);
            em.persist(domainBooking);

            em.getTransaction().commit();

            // As remaining seats for this concert and date have now changed, process any subscriptions interested in the info.
            SUBSCRIPTIONS_THREAD_POOL.submit(() -> processConcertInfoSubscriptions(bookingRequest.getConcertId(), bookingRequest.getDate()));

            // If the transaction was successful, return the link to the user.
            return Response.created(URI.create("/concert-service/bookings/" + domainBooking.getId())).build();


        } catch (RollbackException e) {
            return Response.status(Status.FORBIDDEN).build();

        } finally {
            em.close();
        }
    }

    @GET
    @Path("/bookings/{id}")
    public BookingDTO getBooking(@PathParam("id") long id, @CookieParam("auth") Cookie authCookie) {

        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {

            em.getTransaction().begin();

            // Get authenticated user
            User user = lookupUserByAuthenticationToken(authCookie, em);

            // Get booking with given id
            Booking booking = em.find(Booking.class, id);

            em.getTransaction().commit();

            // If booking's user doesn't match our user, 403.
            if (booking.getReservedFor() != user) {
                throw new ForbiddenException(Response.status(Status.FORBIDDEN).build());
            }

            return BookingMapper.toDTO(booking);

        } finally {
            em.close();
        }
    }

    @GET
    @Path("/bookings")
    public Response getBookingsForUser(@CookieParam("auth") Cookie authCookie) {

        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {

            em.getTransaction().begin();

            // Get authenticated user - if none, then error.
            User user = lookupUserByAuthenticationToken(authCookie, em);
            LOGGER.info("getBookingsForUser(): Username = " + user.getUsername());

            // Get bookings for the given user
            List<Booking> domainBookings = em
                    .createQuery("select b from Booking b where b.user = :user", Booking.class)
                    .setParameter("user", user)
                    .getResultList();

            em.getTransaction().commit();

            return Response.ok(getListGenericEntity(domainBookings, BookingMapper::toDTO)).build();

        } finally {
            em.close();
        }
    }
    // ----------------------------------------------------------------------------------------------------------


    // Subscriptions
    // ----------------------------------------------------------------------------------------------------------
    @POST
    @Path("/subscribe/concertInfo")
    public void subscribeConcertInfo(ConcertInfoSubscriptionDTO subInfo, @CookieParam("auth") Cookie authCookie, @Suspended AsyncResponse sub) {

        // Make sure the user is authenticated. Unauthenticated users aren't allowed to do this.
        EntityManager em = PersistenceManager.instance().createEntityManager();
        User user;
        em.getTransaction().begin();
        try {
            user = lookupUserByAuthenticationToken(authCookie, em);

            // Make sure the requested concert & date exists. If not, bad request.
            Concert concert = em.find(Concert.class, subInfo.getConcertId());
            if (concert == null || !concert.isScheduledOn(subInfo.getDate())) {
                sub.resume(Response.status(Status.BAD_REQUEST).build());
            }

        } catch (Exception e) {
            // If there's any error (usually because not authenticated), pass it on to the suspended response.
            sub.resume(e);
        } finally {
            em.getTransaction().commit();
            em.close();
        }

        // Save sub info.
        synchronized (SUBSCRIPTIONS) {
            SUBSCRIPTIONS.put(subInfo, sub);
        }
    }

    private void processConcertInfoSubscriptions(long concertId, LocalDateTime date) {

        // Find all matching subscriptions
        List<ConcertInfoSubscriptionDTO> matchingSubs;
        synchronized (SUBSCRIPTIONS) {

            // Optimization - if there are no subs, just get outta here before we do any work.
            if (SUBSCRIPTIONS.size() <= 0) {
                return;
            }

            matchingSubs = SUBSCRIPTIONS.keySet().stream()
                    .filter(sub -> sub.getConcertId() == concertId && sub.getDate().equals(date))
                    .collect(Collectors.toList());
        }

        // Only bother continuing if there are any requests for this concert / date.
        if (matchingSubs.size() <= 0) {
            return;
        }

        LOGGER.info("processConcertInfoSubscriptions(): Found matching subs for " + FORMATTER.format(date) + " (" + matchingSubs.size() + ")");

        // Get all remaining seats for this concert / date
        EntityManager em = PersistenceManager.instance().createEntityManager();
        long remainingSeats;
        try {
            remainingSeats = em
                    .createQuery("select count(s) from Seat s where s.isBooked = false and s.date = :date", Long.class)
                    .setParameter("date", date)
                    .getSingleResult();
        } catch (NonUniqueResultException | NoResultException e) {
            LOGGER.error("processConcertInfoSubscriptions(): Didn't receive a count value!", e);
            return;
        } finally {
            em.close();
        }

        LOGGER.info("processConcertInfoSubscriptions(): " + remainingSeats + " remaining seats for " + FORMATTER.format(date));

        double proportionUnbooked = (double) remainingSeats / (double) TheatreLayout.NUM_SEATS_IN_THEATRE;
        long percentageUnbooked = Math.round(proportionUnbooked * 100);
        long percentageBooked = 100 - percentageUnbooked;
        ConcertInfoNotificationDTO notification = new ConcertInfoNotificationDTO((int) remainingSeats);

        LOGGER.info("processConcertInfoSubscriptions(): Theatre is " + percentageBooked + "% full on " + FORMATTER.format(date));

        // Resume all subs that meet the criteria
        // (Doing my best to keep the synchronized block to a minimum by removing all non-matching subs first)
        matchingSubs = matchingSubs.stream()
                .filter(s -> percentageBooked >= s.getPercentageBooked()).collect(Collectors.toList());

        LOGGER.info("processConcertInfoSubscriptions(): Found matching subs for " +
                FORMATTER.format(date) + ", " + percentageBooked + "% full (" + matchingSubs.size() + ")");

        synchronized (SUBSCRIPTIONS) {
            for (ConcertInfoSubscriptionDTO sub : matchingSubs) {
                SUBSCRIPTIONS.get(sub).resume(notification);
                SUBSCRIPTIONS.remove(sub);
            }
        }

        LOGGER.info("processConcertInfoSubscriptions(): All matches notified.");
    }


    // ----------------------------------------------------------------------------------------------------------


    // Utils
    // ----------------------------------------------------------------------------------------------------------

    /**
     * Creates and returns a new "auth" cookie.
     *
     * @return
     */
    private static NewCookie makeAuthCookie() {
        NewCookie newCookie = null;

        newCookie = new NewCookie("auth", UUID.randomUUID().toString());
        return newCookie;
    }

    /**
     * Gets a User from the DB, given an auth token. If none exists, throw a 401 error.
     */
    private static User lookupUserByAuthenticationToken(Cookie authToken, EntityManager em) {

        try {
            String uuid = authToken.getValue();
            TypedQuery<User> query = em
                    .createQuery("select u from User u where u.uuid = :uuid", User.class)
                    .setParameter("uuid", uuid);
            return query.getSingleResult();

        } catch (NullPointerException | NonUniqueResultException | NoResultException e) {
            LOGGER.error("Error looking up User by authentication token", e);
            throw new NotAuthorizedException(Response.status(Status.UNAUTHORIZED).build());
        }
    }

    /**
     * A utility function which maps the given list of domain objects into a GenericEntity of DTO objects, following the
     * given mapping function.
     *
     * @param domainList
     * @param mappingFunc
     * @param <TDTO>
     * @param <TDomain>
     * @return
     */
    private static <TDTO, TDomain> GenericEntity<List<TDTO>> getListGenericEntity(List<TDomain> domainList, Function<TDomain, TDTO> mappingFunc) {
        List<TDTO> dtoConcerts = domainList.stream().map(mappingFunc)
                .collect(Collectors.toList());

        return new GenericEntity<List<TDTO>>(dtoConcerts) {
        };
    }
    // ----------------------------------------------------------------------------------------------------------
}
