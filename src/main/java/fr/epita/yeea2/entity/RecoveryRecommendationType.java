package fr.epita.yeea2.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecoveryRecommendationType {
    STRETCH("Light stretching is recommended."),
    WALK("Taking a walk will help."),
    BREAK("Take a short break."),
    SLEEP("Get enough sleep."),
    DRINK_WATER("Stay hydrated."),
    NORMAL("You are in good condition. Keep it up consistently.");

    private final String message;
}
