package org.akhq.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class Film {
    private final String name;
    private final String producer;
    private final int releaseYear;
    private final int duration;
    private final List<String> starring;
}
