package org.akhq.utils;

import org.akhq.AlbumAvro;
import org.akhq.FilmAvro;
import org.akhq.configs.AvroTopicsMapping;
import org.akhq.configs.Connection;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AvroToJsonDeserializerTest {
    Connection.Deserialization.AvroDeserializationTopicsMapping avroDeserializationTopicsMapping;
    AlbumAvro albumAvro;
    FilmAvro filmAvro;

    @BeforeEach
    void before() throws URISyntaxException {
        createTopicAvroDeserializationMapping();
        createAlbumObject();
        createFilmObject();
    }

    private void createTopicAvroDeserializationMapping() throws URISyntaxException {
        avroDeserializationTopicsMapping = new Connection.Deserialization.AvroDeserializationTopicsMapping();

        URI uri = ClassLoader.getSystemResource("avro").toURI();
        String avroSchemaFolder = Paths.get(uri).toString();
        avroDeserializationTopicsMapping.setSchemasFolder(avroSchemaFolder);

        AvroTopicsMapping albumTopicsMapping = new AvroTopicsMapping();
        albumTopicsMapping.setTopicRegex("album.*");
        albumTopicsMapping.setValueSchemaFile("Album.avsc");

        AvroTopicsMapping filmTopicsMapping = new AvroTopicsMapping();
        filmTopicsMapping.setTopicRegex("film.*");
        filmTopicsMapping.setValueSchemaFile("Film.avsc");

        // Do not specify schema neither for a key, nor for a value
        AvroTopicsMapping incorrectTopicsMapping = new AvroTopicsMapping();
        incorrectTopicsMapping.setTopicRegex("incorrect.*");

        avroDeserializationTopicsMapping.setTopicsMapping(
            Arrays.asList(albumTopicsMapping, filmTopicsMapping, incorrectTopicsMapping));
    }

    private void createAlbumObject() {
        List<String> artists = Collections.singletonList("Imagine Dragons");
        List<String> songTitles = Arrays.asList("Birds", "Zero", "Natural", "Machine");
        Album album = new Album("Origins", artists, 2018, songTitles);
        albumAvro = AlbumAvro.newBuilder()
            .setTitle(album.getTitle())
            .setArtist(album.getArtists())
            .setReleaseYear(album.getReleaseYear())
            .setSongTitle(album.getSongsTitles())
            .build();
    }

    private void createFilmObject() {
        List<String> starring = Arrays.asList("Harrison Ford", "Mark Hamill", "Carrie Fisher", "Adam Driver", "Daisy Ridley");
        Film film = new Film("Star Wars: The Force Awakens", "J. J. Abrams", 2015, 135, starring);
        filmAvro = FilmAvro.newBuilder()
            .setName(film.getName())
            .setProducer(film.getProducer())
            .setReleaseYear(film.getReleaseYear())
            .setDuration(film.getDuration())
            .setStarring(film.getStarring())
            .build();
    }

    @Test
    void deserializeAlbum() throws IOException, URISyntaxException {
        AvroToJsonDeserializer avroToJsonDeserializer = new AvroToJsonDeserializer(avroDeserializationTopicsMapping, new AvroToJsonSerializer(null));
        final byte[] binaryAlbum = toByteArray("Album.avsc", albumAvro);
        String decodedAlbum = avroToJsonDeserializer.deserialize("album.topic.name", binaryAlbum, false);
        String expectedAlbum = "{" +
            "\"title\":\"Origins\"," +
            "\"artist\":[\"Imagine Dragons\"]," +
            "\"releaseYear\":2018," +
            "\"songTitle\":[\"Birds\",\"Zero\",\"Natural\",\"Machine\"]" +
            "}";
        assertEquals(expectedAlbum, decodedAlbum);
    }

    @Test
    void deserializeFilm() throws IOException, URISyntaxException {
        AvroToJsonDeserializer avroToJsonDeserializer = new AvroToJsonDeserializer(avroDeserializationTopicsMapping, new AvroToJsonSerializer(null));
        final byte[] binaryFilm = toByteArray("Film.avsc", filmAvro);
        String decodedFilm = avroToJsonDeserializer.deserialize("film.topic.name", binaryFilm, false);
        String expectedFilm = "{" +
            "\"name\":\"Star Wars: The Force Awakens\"," +
            "\"producer\":\"J. J. Abrams\"," +
            "\"releaseYear\":2015," +
            "\"duration\":135," +
            "\"starring\":[\"Harrison Ford\",\"Mark Hamill\",\"Carrie Fisher\",\"Adam Driver\",\"Daisy Ridley\"]" +
            "}";
        assertEquals(expectedFilm, decodedFilm);
    }

    @Test
    void deserializeForNotMatchingTopic() throws IOException, URISyntaxException {
        AvroToJsonDeserializer avroToJsonDeserializer = new AvroToJsonDeserializer(avroDeserializationTopicsMapping, new AvroToJsonSerializer(null));
        final byte[] binaryFilm = toByteArray("Film.avsc", filmAvro);
        String decodedFilm = avroToJsonDeserializer.deserialize("random.topic.name", binaryFilm, false);
        assertNull(decodedFilm);
    }

    @Test
    void deserializeForKeyWhenItsTypeNotSet() throws IOException, URISyntaxException {
        AvroToJsonDeserializer avroToJsonDeserializer = new AvroToJsonDeserializer(avroDeserializationTopicsMapping, new AvroToJsonSerializer(null));
        final byte[] binaryFilm = toByteArray("Film.avsc", filmAvro);
        String decodedFilm = avroToJsonDeserializer.deserialize("film.topic.name", binaryFilm, true);
        assertNull(decodedFilm);
    }

    @Test
    void deserializeWhenTypeNotSetForKeyAndValue() throws IOException, URISyntaxException {
        AvroToJsonDeserializer avroToJsonDeserializer = new AvroToJsonDeserializer(avroDeserializationTopicsMapping, new AvroToJsonSerializer(null));
        final byte[] binaryFilm = toByteArray("Film.avsc", filmAvro);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            avroToJsonDeserializer.deserialize("incorrect.topic.name", binaryFilm, true);
        });
        String expectedMessage = "schema is not specified neither for a key, nor for a value";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    private <T> byte[] toByteArray(String schemaName, T datum) throws IOException, URISyntaxException {
        Schema schema = resolveSchema(schemaName);

        DatumWriter<T> writer = new GenericDatumWriter<>(schema);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(bos, null);
        writer.write(datum, encoder);
        encoder.flush();
        bos.close();

        return bos.toByteArray();
    }

    private Schema resolveSchema(String schemaName) throws URISyntaxException, IOException {
        URI uri = ClassLoader.getSystemResource("avro").toURI();
        File schemaFile = Paths.get(uri).resolve(schemaName).toFile();

        return new Schema.Parser().parse(schemaFile);
    }
}
