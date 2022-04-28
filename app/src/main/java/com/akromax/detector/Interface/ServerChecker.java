package com.akromax.detector.Interface;

import okhttp3.Response;

public interface ServerChecker {
    String apiKey="eEafAGlvcGVnEmViaGUwcA";
    String url="https://levakromusic.com/radioapp/iosradio/api-pick.php?method=getSessionID&apikey="+apiKey;
    String dnsurl="https://www.google.com";
    void checkServer(String url);
}
