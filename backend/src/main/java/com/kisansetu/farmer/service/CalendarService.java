package com.kisansetu.farmer.service;

import com.kisansetu.common.exception.ApiException;
import com.kisansetu.farmer.dto.CalendarEventRequest;
import com.kisansetu.farmer.dto.CalendarEventResponse;
import com.kisansetu.farmer.entity.FarmerCalendarEvent;
import com.kisansetu.farmer.repository.FarmerCalendarEventRepository;
import com.kisansetu.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Smart farming calendar: CRUD, completion toggling and reminders.
 */
@Service
@RequiredArgsConstructor
public class CalendarService {

    private final FarmerCalendarEventRepository eventRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getEvents(UUID farmerId, LocalDate from, LocalDate to) {
        List<FarmerCalendarEvent> events;
        if (from != null && to != null) {
            events = eventRepository.findByFarmerIdAndEventDateBetweenOrderByEventDateAsc(farmerId, from, to);
        } else {
            events = eventRepository.findByFarmerIdOrderByEventDateAsc(farmerId);
        }
        return events.stream().map(CalendarEventResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponse> getUpcoming(UUID farmerId) {
        return eventRepository
                .findByFarmerIdAndCompletedFalseAndEventDateGreaterThanEqualOrderByEventDateAsc(
                        farmerId, LocalDate.now())
                .stream().limit(5).map(CalendarEventResponse::from).toList();
    }

    @Transactional
    public CalendarEventResponse createEvent(UUID farmerId, CalendarEventRequest request) {
        FarmerCalendarEvent event = new FarmerCalendarEvent();
        applyRequest(event, request);
        event.setFarmerId(farmerId);
        eventRepository.save(event);
        return CalendarEventResponse.from(event);
    }

    @Transactional
    public CalendarEventResponse updateEvent(UUID farmerId, UUID eventId, CalendarEventRequest request) {
        FarmerCalendarEvent event = getOwnedEvent(farmerId, eventId);
        applyRequest(event, request);
        eventRepository.save(event);
        return CalendarEventResponse.from(event);
    }

    @Transactional
    public void deleteEvent(UUID farmerId, UUID eventId) {
        eventRepository.delete(getOwnedEvent(farmerId, eventId));
    }

    @Transactional
    public CalendarEventResponse toggleCompleted(UUID farmerId, UUID eventId) {
        FarmerCalendarEvent event = getOwnedEvent(farmerId, eventId);
        event.setCompleted(!event.isCompleted());
        eventRepository.save(event);
        if (event.isCompleted() && event.isReminderEnabled()) {
            notificationService.notify(farmerId, "calendar",
                    "Task completed",
                    event.getTitle() + " marked as completed.");
        }
        return CalendarEventResponse.from(event);
    }

    /**
     * Add a weather/AI suggested activity to the calendar.
     */
    @Transactional
    public CalendarEventResponse createSuggestionEvent(UUID farmerId, String title, String description,
                                                       LocalDate date, String cropType) {
        CalendarEventRequest request = new CalendarEventRequest(
                title, description, "other", date, true, cropType, true, true);
        return createEvent(farmerId, request);
    }

    private FarmerCalendarEvent getOwnedEvent(UUID farmerId, UUID eventId) {
        FarmerCalendarEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Calendar event not found"));
        if (!event.getFarmerId().equals(farmerId)) {
            throw ApiException.forbidden("This event does not belong to you");
        }
        return event;
    }

    private void applyRequest(FarmerCalendarEvent event, CalendarEventRequest request) {
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setEventType(request.eventType());
        event.setEventDate(request.eventDate());
        event.setReminderEnabled(request.reminderEnabled() == null || request.reminderEnabled());
        if (request.cropType() != null) {
            event.setCropType(request.cropType());
        }
        if (request.weatherDependent() != null) {
            event.setWeatherDependent(request.weatherDependent());
        }
        if (request.suggestedByAi() != null) {
            event.setSuggestedByAi(request.suggestedByAi());
        }
    }
}