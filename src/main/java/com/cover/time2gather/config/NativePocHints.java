package com.cover.time2gather.config;

import com.cover.time2gather.domain.comment.MeetingComment;
import com.cover.time2gather.domain.meeting.Meeting;
import com.cover.time2gather.domain.meeting.MeetingLocation;
import com.cover.time2gather.domain.meeting.MeetingLocationSelection;
import com.cover.time2gather.domain.meeting.MeetingUserSelection;
import com.cover.time2gather.domain.user.User;
import com.cover.time2gather.api.mcp.McpMeetingTools;
import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

/** PoC-only metadata for the application's explicit reflective Firestore/JWT paths. */
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(NativePocHints.Hints.class)
class NativePocHints {
    static class Hints implements RuntimeHintsRegistrar {
        private static void registerDto(RuntimeHints hints, Class<?> type) {
            new BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(), type);
            for (Class<?> nested : type.getDeclaredClasses()) {
                registerDto(hints, nested);
            }
        }

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader loader) {
            // Explicit DTO binding also covers nested values inside generic Map responses.
            for (Class<?> dto : new Class<?>[]{
                    com.cover.time2gather.api.admin.dto.AdminMeetingResponse.class,
                    com.cover.time2gather.api.admin.dto.AdminPageResponse.class,
                    com.cover.time2gather.api.admin.dto.AdminSummaryResponse.class,
                    com.cover.time2gather.domain.admin.AdminUsageStats.class,
                    com.cover.time2gather.api.admin.dto.AdminUserResponse.class,
                    com.cover.time2gather.api.auth.dto.response.UserInfoResponse.class,
                    com.cover.time2gather.api.meeting.dto.ConfirmMeetingRequest.class,
                    com.cover.time2gather.api.meeting.dto.ExportCalendarRequest.class,
                    com.cover.time2gather.api.meeting.dto.request.AddLocationRequest.class,
                    com.cover.time2gather.api.meeting.dto.request.AnonymousLoginRequest.class,
                    com.cover.time2gather.api.meeting.dto.request.ConfirmLocationRequest.class,
                    com.cover.time2gather.api.meeting.dto.request.CreateMeetingRequest.class,
                    com.cover.time2gather.api.meeting.dto.request.UpdateAvailableDatesRequest.class,
                    com.cover.time2gather.api.meeting.dto.request.UpdateCandidatesRequest.class,
                    com.cover.time2gather.api.meeting.dto.request.UpdateConfirmedTimeRequest.class,
                    com.cover.time2gather.api.meeting.dto.request.UpsertUserSelectionRequest.class,
                    com.cover.time2gather.api.meeting.dto.request.VoteLocationsRequest.class,
                    com.cover.time2gather.api.meeting.dto.response.AnonymousLoginResponse.class,
                    com.cover.time2gather.api.meeting.dto.response.CreateMeetingResponse.class,
                    com.cover.time2gather.api.meeting.dto.response.LocationResponse.class,
                    com.cover.time2gather.api.meeting.dto.response.MeetingDetailResponse.class,
                    com.cover.time2gather.api.meeting.dto.response.UserLocationSelectionsResponse.class,
                    com.cover.time2gather.api.meeting.dto.response.UserSelectionResponse.class}) {
                registerDto(hints, dto);
            }
            Class<?>[] entities = {User.class, Meeting.class, MeetingUserSelection.class,
                    MeetingLocation.class, MeetingLocationSelection.class, MeetingComment.class,
                    MeetingComment.Mention.class};
            for (Class<?> dto : com.cover.time2gather.domain.comment.CommentService.class.getDeclaredClasses()) {
                registerDto(hints, dto);
            }
            new BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(), entities);
            hints.reflection().registerType(McpMeetingTools.class, MemberCategory.INVOKE_DECLARED_METHODS);
            new BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(), McpMeetingTools.class.getDeclaredClasses());
            for (Class<?> entity : entities) {
                for (Class<?> type = entity; type != Object.class && type != null; type = type.getSuperclass()) {
                    hints.reflection().registerType(type, MemberCategory.DECLARED_FIELDS,
                            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_DECLARED_METHODS);
                }
            }
            for (String name : new String[]{"io.jsonwebtoken.impl.DefaultJwtBuilder",
                    "io.jsonwebtoken.impl.DefaultJwtParserBuilder", "io.jsonwebtoken.impl.DefaultClaims",
                    "io.jsonwebtoken.impl.DefaultJwsHeader", "io.jsonwebtoken.jackson.io.JacksonSerializer",
                    "io.jsonwebtoken.jackson.io.JacksonDeserializer", "io.jsonwebtoken.impl.DefaultClaimsBuilder",
                    "io.jsonwebtoken.impl.DefaultJwtHeaderBuilder", "io.jsonwebtoken.impl.io.StandardCompressionAlgorithms",
                    "io.jsonwebtoken.impl.security.KeysBridge", "io.jsonwebtoken.impl.security.JwksBridge",
                    "io.jsonwebtoken.impl.security.DefaultDynamicJwkBuilder", "io.jsonwebtoken.impl.security.DefaultJwkParserBuilder",
                    "io.jsonwebtoken.impl.security.DefaultJwkSetBuilder", "io.jsonwebtoken.impl.security.DefaultJwkSetParserBuilder",
                    "io.jsonwebtoken.impl.security.DefaultKeyOperationBuilder", "io.jsonwebtoken.impl.security.DefaultKeyOperationPolicyBuilder",
                    "io.jsonwebtoken.impl.security.StandardCurves", "io.jsonwebtoken.impl.security.StandardEncryptionAlgorithms",
                    "io.jsonwebtoken.impl.security.StandardHashAlgorithms", "io.jsonwebtoken.impl.security.StandardKeyAlgorithms",
                    "io.jsonwebtoken.impl.security.StandardKeyOperations", "io.jsonwebtoken.impl.security.StandardSecureDigestAlgorithms"}) {
                hints.reflection().registerType(TypeReference.of(name), MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_PUBLIC_METHODS);
            }
            for (String resource : new String[]{"messages*.properties", "ValidationMessages*.properties",
                    "zoneinfo/**", "zoneinfo-outlook/**", "ical4j.properties", "META-INF/services/io.jsonwebtoken.*",
                    "com/cover/time2gather/api/mcp/**/*.class"}) {
                hints.resources().registerPattern(resource);
            }
            hints.resources().registerResourceBundle("messages");
            hints.resources().registerResourceBundle("ValidationMessages");
        }
    }
}
