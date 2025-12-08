package com.example.WayGo.Service.Translation;

import com.example.WayGo.Entity.enums.SupportedLanguage;
import com.example.WayGo.Constant.ErrorCode;
import com.example.WayGo.Exception.TranslationException;
import org.springframework.stereotype.Service;

@Service
public class LanguageMappingService {

    public String toTranslateCode(String clientValue) {
        SupportedLanguage lang = SupportedLanguage.fromClientValue(clientValue);
        if (lang == null) {
            throw new TranslationException(
                    ErrorCode.INVALID_REQUEST,
                    "지원하지 않는 언어입니다: " + clientValue
            );
        }
        return lang.getTranslateCode();
    }

    public String toSpeechCode(String clientValue) {
        SupportedLanguage lang = SupportedLanguage.fromClientValue(clientValue);
        if (lang == null) {
            throw new TranslationException(
                    ErrorCode.INVALID_REQUEST,
                    "지원하지 않는 음성 언어입니다: " + clientValue
            );
        }
        return lang.getSpeechCode();
    }
}
