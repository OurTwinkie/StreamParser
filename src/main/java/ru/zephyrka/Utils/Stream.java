package ru.zephyrka.Utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Stream {
    private String link;
    private String streamName;
    private String gameName;
    private long viewerCount;
    private String streamer;

    public String toString() {
        return String.format("link: %s, streamName: %s, gameName: %s, viewerCount: %d, streamer: %s", link, streamName, gameName, viewerCount, streamer);
    }
}
