package org.kafkahq.repositories;

import org.junit.Test;
import org.kafkahq.BaseTest;

import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class TopicRepositoryTest extends BaseTest {
    @Test
    public void list() throws ExecutionException, InterruptedException {
        assertEquals(2, app.require(TopicRepository.class).list().size());
    }
}