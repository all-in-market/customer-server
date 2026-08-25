package com.example.allinmarket.common.auth.consts;

import com.example.allinmarket.common.enums.UserRole;

import java.util.Locale;

public final class AuthConsts {

    public static final String REFRESH_KEY_PREFIX = "refresh:";
    public static final String USER_REFRESHES_KEY_PREFIX = "user_refreshes:";
    public static final String BLACKLIST_KEY_PREFIX = "blacklist:";
    public static final long REFRESH_TOKEN_TTL_DAYS = 7L;

    private AuthConsts() {}

    public static String refreshKey(UserRole role, String token) {
        return REFRESH_KEY_PREFIX + role.name().toLowerCase(Locale.ROOT) + ":" + token;
    }

    public static String userRefreshesKey(UserRole role, Long userId) {
        return USER_REFRESHES_KEY_PREFIX + role.name().toLowerCase(Locale.ROOT) + ":" + userId;
    }

    public static String legacyRefreshKey(String token) {
        return REFRESH_KEY_PREFIX + token;
    }

    public static String legacyUserRefreshesKey(Long userId) {
        return USER_REFRESHES_KEY_PREFIX + userId;
    }
}
