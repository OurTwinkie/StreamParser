package ru.zephyrka.utils;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.helix.domain.Stream;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.val;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Parser {
    private static String YOUTUBE_API_KEY;

    private static int MIN_VIEWERS;
    private static int MAX_VIEWERS;

    private static final String KICK_API_URL = "https://api.kick.com/public/v1/livestreams";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final Map<String, ru.zephyrka.utils.Stream> STRING_STREAM_HASH_MAP = new HashMap<>();

    private static void getKickStreams(String kickToken, int targetCount) throws InterruptedException {
        val containsStreams = new AtomicInteger(0);
        val streamsCount = new AtomicInteger(0);
        val perPage = 100;
        List<ru.zephyrka.utils.Stream> matchingStreams = new ArrayList<>();

        for (int i = 0; i < targetCount; i++) {
            val url = HttpUrl.parse(KICK_API_URL).newBuilder()
                    .addQueryParameter("language", "ru")
                    .addQueryParameter("limit", String.valueOf(perPage))
                    .build();

            val request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", "Bearer " + kickToken)
                    .build();

            try (val response = client.newCall(request).execute()) {
                if (response.code() == 429) {
                    System.out.println("429 has been received, we are waiting for 5 seconds...");
                    TimeUnit.SECONDS.sleep(5);
                    continue;
                }

                if (!response.isSuccessful()) {
                    System.out.println("Error: " + response);
                    break;
                }

                val responseBody = response.body().string();
                val json = gson.fromJson(responseBody, JsonObject.class);
                val streams = json.getAsJsonArray("data");

                if (streams == null || streams.size() == 0) {
                    System.out.println("Dont have more streams.");
                    break;
                }

                List<ru.zephyrka.utils.Stream> resultStreams = new ArrayList<>();
                for (val element : streams) {
                    val stream = element.getAsJsonObject();

                    val viewers = stream.get("viewer_count").getAsInt();
                    if (viewers < MIN_VIEWERS || viewers > MAX_VIEWERS) continue;

                    val streamer = stream.get("slug").getAsString();

                    val category = stream.getAsJsonObject("category");
                    String categoryName = "";
                    if (category != null) {
                        val categoryObject = category.getAsJsonObject();
                        categoryName = categoryObject.get("name").getAsString();
                    }

                    val title = stream.get("stream_title").getAsString();
                    val language = stream.get("language").getAsString();
                    if (language.equals("ru")) {
                        ru.zephyrka.utils.Stream stream1 = new ru.zephyrka.utils.Stream(
                                "https://kick.com/" + streamer,
                                title,
                                categoryName,
                                viewers,
                                streamer
                        );
                        resultStreams.add(stream1);
                    }

                }

                for (val stream1 : resultStreams) {
                    if (matchingStreams.stream().noneMatch(stream2 -> stream2.getLink().equals(stream1.getLink()))) {
                        matchingStreams.add(stream1);
                        STRING_STREAM_HASH_MAP.put(stream1.getLink(), stream1);
                        System.out.println("Collected: " + matchingStreams.size() + " streams");
                    }
                }

                if (matchingStreams.size() == streamsCount.get()) {
                    if (containsStreams.incrementAndGet() > 20) {
                        break;
                    }
                }

                streamsCount.set(matchingStreams.size());

                TimeUnit.MILLISECONDS.sleep(500);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
        System.out.println("Collected: " + matchingStreams.size() + " streams");
    }

    private static void getYouTubeStreams(YouTube youtubeService, int targetCount) throws IOException {
        val streams = new AtomicInteger(1);

        val searchRequest = youtubeService.search()
                .list("id")
                .setEventType("live")
                .setType("video")
                .setMaxResults((long) targetCount)
                .setRelevanceLanguage("ru")
                .setQ("с | стрим")
                .setKey(YOUTUBE_API_KEY);

        val searchResponse = searchRequest.execute();

        val videoIds = searchResponse.getItems().stream()
                .map(item -> item.getId().getVideoId())
                .collect(Collectors.toList());

        if (videoIds.isEmpty()) {
            System.out.println("No videos found");
            return;
        }

        val videosRequest = youtubeService.videos()
                .list("snippet,liveStreamingDetails")
                .setId(String.join(",", videoIds))
                .setKey(YOUTUBE_API_KEY);

        val videosResponse = videosRequest.execute();

        List<Video> matching = new ArrayList<>();

        for (val video : videosResponse.getItems()) {
            val live = video.getLiveStreamingDetails();
            if (live != null && live.getConcurrentViewers() != null) {
                long viewers = live.getConcurrentViewers().longValue();
                if (viewers >= MIN_VIEWERS && viewers <= MAX_VIEWERS) {
                    matching.add(video);
                }
            }
        }

        for (Video video : matching) {
            val stream = getStream(video);
            System.out.println("Collected: " + streams.getAndIncrement() + " streams");

            if (!STRING_STREAM_HASH_MAP.containsKey(stream.getLink())) {
                STRING_STREAM_HASH_MAP.put(stream.getLink(), stream);
            }
        }
    }

    @NotNull
    private static ru.zephyrka.utils.Stream getStream(Video video) {
        val title = video.getSnippet().getTitle();
        val channel = video.getSnippet().getChannelTitle();
        val viewers = String.valueOf(video.getLiveStreamingDetails().getConcurrentViewers());
        val url = "https://www.youtube.com/watch?v=" + video.getId();

        return new ru.zephyrka.utils.Stream(
                url,
                title,
                "",
                Long.parseLong(viewers),
                channel
        );
    }

    private static void getTwitchStreams(TwitchClient twitchClient, int targetCount, String twitchToken) throws InterruptedException {
        List<Stream> resultStreams = new ArrayList<>();
        String cursor = null;
        val i = new AtomicInteger(0);
        val atomicInteger = new AtomicInteger(0);

        do {
            val command = twitchClient.getHelix().getStreams(
                    twitchToken,
                    cursor,
                    null,
                    100,
                    null,
                    Collections.singletonList("ru"),
                    null,
                    null
            );

            val response = command.execute();
            if (response == null || response.getStreams().isEmpty()) {
                break;
            }

            for (val stream : response.getStreams()) {
                int viewers = stream.getViewerCount();
                if (viewers >= MIN_VIEWERS && viewers <= MAX_VIEWERS) {
                    resultStreams.add(stream);
                }
            }

            cursor = response.getPagination().getCursor();

            System.out.println("Collected: " + resultStreams.size() + " streams");
            resultStreams.forEach(stream -> {
                if (!STRING_STREAM_HASH_MAP.containsKey(stream.getId()))
                    STRING_STREAM_HASH_MAP.put(stream.getId(), new ru.zephyrka.utils.Stream("https://twitch.tv/"+stream.getUserName(), stream.getTitle(), stream.getGameName(), stream.getViewerCount(), stream.getUserName()));
            });

            if (cursor == null || resultStreams.size() >= targetCount) {
                break;
            }

            atomicInteger.incrementAndGet();
            if (atomicInteger.get() > 20 && i.get() == resultStreams.size())
                break;
            i.set(resultStreams.size());

            Thread.sleep(500);

        } while (true);

    }

    public static void startParsing(
            String twitchClientId, String twitchAuthToken,
            String kickClientId, String kickClientSecret,
            String youtubeApiKey,
            int minViewers, int maxViewers, int targetCount,
            boolean twitchEnabled, boolean youtubeEnabled, boolean kickEnabled
    ) throws GeneralSecurityException, IOException, InterruptedException {

        STRING_STREAM_HASH_MAP.clear();
        MIN_VIEWERS = minViewers;
        MAX_VIEWERS = maxViewers;

        if (twitchEnabled) {
            val twitchClient = TwitchClientBuilder.builder()
                    .withEnableHelix(true)
                    .withClientId(twitchClientId)
                    .withClientSecret(twitchAuthToken)
                    .build();
            getTwitchStreams(twitchClient, targetCount, twitchAuthToken);
        }
        if (youtubeEnabled) {
            val youtubeService = new YouTube.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    request -> {}
            ).setApplicationName("yt-stream-filter").build();
            YOUTUBE_API_KEY = youtubeApiKey;
            getYouTubeStreams(youtubeService, targetCount);
        }
        if (kickEnabled) {
            val kickToken = KickAuth.getAccessToken(kickClientId, kickClientSecret);
            getKickStreams(kickToken, targetCount);
        }

        System.out.println("Total streams found: " + STRING_STREAM_HASH_MAP.size());
    }

    public static Collection<ru.zephyrka.utils.Stream> getCollectedStreams() {
        return STRING_STREAM_HASH_MAP.values();
    }

}
