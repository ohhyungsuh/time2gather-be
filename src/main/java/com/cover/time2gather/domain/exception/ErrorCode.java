package com.cover.time2gather.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 * 각 에러 코드는 메시지 키와 HTTP 상태 코드를 가집니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== Common Errors =====
    INTERNAL_SERVER_ERROR("error.internal", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("error.validation.failed", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("error.request.invalid", HttpStatus.BAD_REQUEST),
    INVALID_STATE("error.state.invalid", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("error.resource.not.found", HttpStatus.NOT_FOUND),
    NULL_POINTER("error.null.pointer", HttpStatus.BAD_REQUEST),
    NUMBER_FORMAT("error.number.format", HttpStatus.BAD_REQUEST),

    // ===== Auth Errors =====
    AUTH_REQUIRED("error.auth.required", HttpStatus.UNAUTHORIZED),
    AUTH_REQUIRED_LOGIN("error.auth.required.login", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("error.auth.access.denied", HttpStatus.FORBIDDEN),
    USER_NOT_AUTHENTICATED("error.auth.not.authenticated", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("error.user.not.found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND_EMAIL("error.user.not.found.email", HttpStatus.NOT_FOUND),
    USER_ID_REQUIRED("error.user.id.required", HttpStatus.BAD_REQUEST),

    // ===== OAuth Errors =====
    OAUTH_INVALID_CODE("error.oauth.invalid.code", HttpStatus.BAD_REQUEST),
    OAUTH_API_FAILED("error.oauth.api.failed", HttpStatus.BAD_GATEWAY),
    OAUTH_NO_ID_TOKEN("error.oauth.no.id.token", HttpStatus.BAD_GATEWAY),
    OAUTH_NO_USER_INFO("error.oauth.no.user.info", HttpStatus.BAD_GATEWAY),

    // ===== Meeting Errors =====
    MEETING_NOT_FOUND("error.meeting.not.found", HttpStatus.NOT_FOUND),
    MEETING_ID_REQUIRED("error.meeting.id.required", HttpStatus.BAD_REQUEST),
    MEETING_ALREADY_CONFIRMED("error.meeting.already.confirmed", HttpStatus.BAD_REQUEST),
    MEETING_NOT_CONFIRMED("error.meeting.not.confirmed", HttpStatus.BAD_REQUEST),
    MEETING_INVALID_DATE("error.meeting.invalid.date", HttpStatus.BAD_REQUEST),
    MEETING_DATE_NOT_AVAILABLE("error.meeting.date.not.available", HttpStatus.BAD_REQUEST),
    MEETING_NO_INTERVAL_INFO("error.meeting.no.interval.info", HttpStatus.BAD_REQUEST),
    MEETING_HOST_ONLY("error.meeting.host.only", HttpStatus.FORBIDDEN),
    MEETING_NO_PARTICIPANTS("error.meeting.no.participants", HttpStatus.BAD_REQUEST),
    MEETING_DATE_FORMAT_INVALID("error.meeting.date.format.invalid", HttpStatus.BAD_REQUEST),

    // ===== Meeting Validation Errors =====
    MEETING_DATE_REQUIRED("error.meeting.validation.date.required", HttpStatus.BAD_REQUEST),
    MEETING_DATE_SLOT_REQUIRED("error.meeting.validation.date.slot.required", HttpStatus.BAD_REQUEST),
    MEETING_TIME_REQUIRED("error.meeting.validation.time.required", HttpStatus.BAD_REQUEST),
    MEETING_ALL_DAY_EMPTY_SLOTS("error.meeting.validation.all.day.empty.slots", HttpStatus.BAD_REQUEST),
    MEETING_SLOT_INDEX_REQUIRED("error.meeting.validation.slot.index.required", HttpStatus.BAD_REQUEST),
    MEETING_SLOT_INDEX_INVALID("error.meeting.validation.slot.index.invalid", HttpStatus.BAD_REQUEST),
    MEETING_DATE_SLOT_TOGETHER("error.meeting.validation.date.slot.together", HttpStatus.BAD_REQUEST),

    // ===== Location Errors =====
    LOCATION_NOT_FOUND("error.location.not.found", HttpStatus.NOT_FOUND),
    LOCATION_NOT_BELONG_TO_MEETING("error.location.not.belong.to.meeting", HttpStatus.BAD_REQUEST),
    LOCATION_VOTE_NOT_ENABLED("error.location.vote.not.enabled", HttpStatus.BAD_REQUEST),
    LOCATION_ALREADY_CONFIRMED("error.location.already.confirmed", HttpStatus.BAD_REQUEST),
    LOCATION_NOT_CONFIRMED("error.location.not.confirmed", HttpStatus.BAD_REQUEST),
    LOCATION_ID_REQUIRED("error.location.id.required", HttpStatus.BAD_REQUEST),
    LOCATION_NAME_REQUIRED("error.location.name.required", HttpStatus.BAD_REQUEST),
    LOCATION_NAME_TOO_LONG("error.location.name.too.long", HttpStatus.BAD_REQUEST),
    LOCATION_MAX_EXCEEDED("error.location.max.exceeded", HttpStatus.BAD_REQUEST),
    LOCATION_MIN_REQUIRED("error.location.min.required", HttpStatus.BAD_REQUEST),
    LOCATION_MIN_FOR_VOTE("error.location.min.for.vote", HttpStatus.BAD_REQUEST),
    LOCATION_CANNOT_DISABLE("error.location.cannot.disable", HttpStatus.BAD_REQUEST),
    LOCATION_NOT_EXIST("error.location.not.exist", HttpStatus.NOT_FOUND),

    // ===== Selection Validation Errors =====
    SELECTION_EMPTY("error.selection.empty", HttpStatus.BAD_REQUEST),
    SELECTION_DATE_REQUIRED("error.selection.date.required", HttpStatus.BAD_REQUEST),
    SELECTION_TYPE_REQUIRED("error.selection.type.required", HttpStatus.BAD_REQUEST),
    SELECTION_TYPE_UNKNOWN("error.selection.type.unknown", HttpStatus.BAD_REQUEST),
    SELECTION_TIME_REQUIRED("error.selection.time.required", HttpStatus.BAD_REQUEST),
    SELECTION_TIME_EMPTY("error.selection.time.empty", HttpStatus.BAD_REQUEST),
    SELECTION_TIME_FORMAT_INVALID("error.selection.time.format.invalid", HttpStatus.BAD_REQUEST),
    SELECTION_DATE_NOT_AVAILABLE("error.selection.date.not.available", HttpStatus.BAD_REQUEST),
    SELECTION_SLOT_NULL("error.selection.slot.null", HttpStatus.BAD_REQUEST),
    SELECTION_SLOT_NOT_AVAILABLE("error.selection.slot.not.available", HttpStatus.BAD_REQUEST),
    SELECTION_SLOT_EMPTY_FOR_TIME("error.selection.slot.empty.for.time", HttpStatus.BAD_REQUEST),

    // ===== TimeSlot Errors =====
    TIMESLOT_INVALID_FORMAT("error.timeslot.invalid.format", HttpStatus.BAD_REQUEST),
    TIMESLOT_INVALID_HOUR("error.timeslot.invalid.hour", HttpStatus.BAD_REQUEST),
    TIMESLOT_INVALID_MINUTE("error.timeslot.invalid.minute", HttpStatus.BAD_REQUEST),
    TIMESLOT_NOT_ALIGNED("error.timeslot.not.aligned", HttpStatus.BAD_REQUEST),
    TIMESLOT_INDEX_OUT_OF_RANGE("error.timeslot.index.out.of.range", HttpStatus.BAD_REQUEST),
    TIMESLOT_INTERVAL_POSITIVE("error.timeslot.interval.positive", HttpStatus.BAD_REQUEST),
    TIMESLOT_INTERVAL_DIVISOR("error.timeslot.interval.divisor", HttpStatus.BAD_REQUEST),

    // ===== Calendar Export Errors =====
    CALENDAR_EXPORT_FAILED("error.calendar.export.failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // ===== AI Errors =====
    AI_API_FAILED("error.ai.api.failed", HttpStatus.BAD_GATEWAY),
    AI_PROVIDER_UNKNOWN("error.ai.provider.unknown", HttpStatus.BAD_REQUEST),

    // ===== Validation (DTO) Errors =====
    VALIDATION_TITLE_REQUIRED("validation.meeting.title.required", HttpStatus.BAD_REQUEST),
    VALIDATION_DATES_REQUIRED("validation.meeting.dates.required", HttpStatus.BAD_REQUEST),
    VALIDATION_SELECTIONS_REQUIRED("validation.selection.required", HttpStatus.BAD_REQUEST),
    VALIDATION_AUTH_CODE_REQUIRED("validation.auth.code.required", HttpStatus.BAD_REQUEST),
    VALIDATION_USERNAME_REQUIRED("validation.username.required", HttpStatus.BAD_REQUEST),
    VALIDATION_PASSWORD_REQUIRED("validation.password.required", HttpStatus.BAD_REQUEST),
    VALIDATION_DATE_REQUIRED("validation.date.required", HttpStatus.BAD_REQUEST),
    VALIDATION_DATE_FORMAT("validation.date.format", HttpStatus.BAD_REQUEST),
    VALIDATION_TIME_REQUIRED("validation.time.required", HttpStatus.BAD_REQUEST),
    VALIDATION_TIME_FORMAT("validation.time.format", HttpStatus.BAD_REQUEST),
    VALIDATION_LOCATION_ID_REQUIRED("validation.location.id.required", HttpStatus.BAD_REQUEST),
    VALIDATION_LOCATION_IDS_REQUIRED("validation.location.ids.required", HttpStatus.BAD_REQUEST),
    VALIDATION_LOCATION_NAME_REQUIRED("validation.location.name.required", HttpStatus.BAD_REQUEST),
    VALIDATION_LOCATION_MAX("validation.location.max", HttpStatus.BAD_REQUEST);

    private final String messageKey;
    private final HttpStatus httpStatus;
}
