package com.tronxyz.bug_report.helper;

import java.util.Date;
import java.util.List;

import org.apache.http.cookie.Cookie;

public abstract class CookieDatabase {

    private static CookieDatabase ourInstance;

    private CookieDatabase() {
        ourInstance = this;
    }

    public static CookieDatabase getInstance() {
        return ourInstance;
    }

    public abstract void removeObsolete(Date date);

    public abstract void removeAll();

    public abstract void saveCookies(List<Cookie> cookies);

    public abstract List<Cookie> loadCookies();
}
