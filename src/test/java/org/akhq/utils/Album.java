package org.akhq.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Album {
    private final String title;
    private final List<String> artists;
    private final int releaseYear;
    private final List<String> songsTitles;
}
