package com.cover.time2gather.config.i18n;

import com.cover.time2gather.domain.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import java.util.List;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;

class GlobalLocaleTest {
    private static final List<String> LANGUAGES = List.of("en", "zh-CN", "hi", "es", "ar", "fr", "bn", "pt", "ru", "id", "ur", "de", "ja", "pcm", "ar-EG", "mr", "vi", "te", "ha", "tr", "ko", "th");
    @Test void negotiatesEverySupportedLocaleAndRegionalBrowserPreferences() {
        var resolver = new LocaleConfig().localeResolver();
        for (String tag : LANGUAGES) {
            var request = new MockHttpServletRequest();
            request.addHeader("Accept-Language", tag);
            assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.forLanguageTag(tag));
        }
        var request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", "ko;q=0.1,en;q=0.9");
        assertThat(resolver.resolveLocale(request).getLanguage()).isEqualTo("en");
    }
    @Test void everyErrorCodeHasALocalizedResourceAndInterpolatesParameters() {
        var source = new MessageConfig().messageSource();
        for (String tag : LANGUAGES) {
            var locale = Locale.forLanguageTag(tag);
            for (ErrorCode code : ErrorCode.values()) {
                String result = source.getMessage(code.getMessageKey(), new Object[]{1, 2, 3, 4}, locale);
                assertThat(result).as("%s/%s", tag, code).isNotBlank().isNotEqualTo(code.getMessageKey()).doesNotContain("{0}", "{1}", "{2}", "{3}");
            }
        }
    }
}
