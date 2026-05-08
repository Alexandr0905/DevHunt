package com.devhunt.dto;

public record UserProfileDto(String email, Boolean emailNotifications, Boolean dailyDigest) {}