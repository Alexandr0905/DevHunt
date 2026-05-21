package com.devhunt.dto;

import java.util.List;
import java.util.Set;

public record TelegramSettingsDto(
        boolean enabled,
        Set<String> selectedKeywords,
        List<String> allKeywords, // Список всех доступных слов из БД
        Set<String> selectedGrades
) {}