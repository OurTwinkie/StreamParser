package ru.zephyrka.Utils;

import com.google.gson.JsonParser;
import lombok.val;
import okhttp3.*;

import java.io.IOException;

public class KickAuth {
    private static final String TOKEN_URL = "https://id.kick.com/oauth/token";

    public static String getAccessToken(String CLIENT_ID, String CLIENT_SECRET) throws IOException {
        val client = new OkHttpClient();

        val formBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build();

        val request = new Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try (val response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            val responseBody = response.body().string();
            val json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.get("access_token").getAsString();
        }
    }
}
