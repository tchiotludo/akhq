package org.akhq.controllers;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import org.akhq.AbstractTest;
import org.akhq.KafkaTestCluster;
import org.akhq.utils.ResultNextList;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopicDataPaginationControllerTest extends AbstractTest {
    private static final String INTERLEAVED_URL = "/api/" + KafkaTestCluster.CLUSTER_ID + "/topic/" + KafkaTestCluster.TOPIC_INTERLEAVED;

    @Test
    void dataOldestFollowsAfterLinkWithoutGapsOrDuplicates() {
        assertEquals(interleavedKeys(false, i -> true), followAfterLinks(INTERLEAVED_URL + "/data?sort=OLDEST&size=10"));
    }

    @Test
    void dataNewestFollowsAfterLinkWithoutGapsOrDuplicates() {
        assertEquals(interleavedKeys(true, i -> true), followAfterLinks(INTERLEAVED_URL + "/data?sort=NEWEST&size=10"));
    }

    @Test
    void dataWithFilterFollowsAfterLinkWithoutGapsOrDuplicates() {
        assertEquals(
            interleavedKeys(false, i -> ("key_" + i).contains("key_1")),
            followAfterLinks(INTERLEAVED_URL + "/data?sort=OLDEST&size=10&searchByKey=key_1_C")
        );
    }

    @Test
    void downloadReturnsEveryMatchOnceInOrder() {
        String body = client.toBlocking().retrieve(
            HttpRequest.GET(INTERLEAVED_URL + "/data/download?searchByKey=key_1_C").basicAuth("admin", "pass")
        );

        JSONArray records = new JSONArray(body);
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < records.length(); i++) {
            keys.add(records.getJSONObject(i).getString("key"));
        }

        assertEquals(interleavedKeys(false, i -> ("key_" + i).contains("key_1")), keys);
    }

    // Pages through /data the way the UI does: request the returned "after" link until it is empty.
    @SuppressWarnings("unchecked")
    private List<String> followAfterLinks(String firstUrl) {
        List<String> keys = new ArrayList<>();
        String url = firstUrl;
        int pages = 0;

        while (url != null && !url.isEmpty()) {
            assertTrue(pages++ <= 40, "Pagination did not terminate within a reasonable number of pages");

            ResultNextList<Map<String, Object>> page = client.toBlocking().retrieve(
                HttpRequest.GET(url).basicAuth("admin", "pass"),
                Argument.of(ResultNextList.class, Map.class)
            );
            // Empty lists are omitted from the JSON response, so the last page has no "results".
            List<Map<String, Object>> results = page.getResults() == null ? List.of() : page.getResults();
            results.forEach(record -> keys.add((String) record.get("key")));
            url = results.isEmpty() ? null : page.getAfter();
        }

        return keys;
    }

    // TOPIC_INTERLEAVED round-robins strictly increasing timestamps over its partitions, so the
    // oldest/newest order is simply ascending/descending key index.
    private static List<String> interleavedKeys(boolean newestFirst, IntPredicate indexFilter) {
        int total = KafkaTestCluster.TOPIC_INTERLEAVED_PER_PARTITION * 3;
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            int index = newestFirst ? total - 1 - i : i;
            if (indexFilter.test(index)) {
                keys.add("key_" + index);
            }
        }
        return keys;
    }
}
