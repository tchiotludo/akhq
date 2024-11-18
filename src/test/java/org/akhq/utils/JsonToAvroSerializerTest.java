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

class JsonToAvroSerializerTest {
    Connection.Serialization.AvroSerializationTopicsMapping avroSerializationTopicsMapping;
    AlbumAvro albumAvro;
    FilmAvro filmAvro;

    @BeforeEach
    void before() throws URISyntaxException {
        createTopicAvroSerializationMapping();
        createAlbumObject();
        createFilmObject();
    }

    private void createTopicAvroSerializationMapping() throws URISyntaxException {
        avroSerializationTopicsMapping = new Connection.Serialization.AvroSerializationTopicsMapping();

        URI uri = ClassLoader.getSystemResource("avro").toURI();
        String avroSchemaFolder = Paths.get(uri).toString();
        avroSerializationTopicsMapping.setSchemasFolder(avroSchemaFolder);

        AvroTopicsMapping albumTopicsMapping = new AvroTopicsMapping();
        albumTopicsMapping.setTopicRegex("album.*");
        albumTopicsMapping.setValueSchemaFile("Album.avsc");

        AvroTopicsMapping filmTopicsMapping = new AvroTopicsMapping();
        filmTopicsMapping.setTopicRegex("film.*");
        filmTopicsMapping.setValueSchemaFile("Film.avsc");

        // Do not specify schema neither for a key, nor for a value
        AvroTopicsMapping incorrectTopicsMapping = new AvroTopicsMapping();
        incorrectTopicsMapping.setTopicRegex("incorrect.*");

        avroSerializationTopicsMapping.setTopicsMapping(
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
    void serializeAlbum() throws IOException, URISyntaxException {
        JsonToAvroSerializer jsonToAvroSerializer = new JsonToAvroSerializer(avroSerializationTopicsMapping);
        final String jsonAlbum = "{" +
            "\"title\":\"Origins\"," +
            "\"artist\":[\"Imagine Dragons\"]," +
            "\"releaseYear\":2018," +
            "\"songTitle\":[\"Birds\",\"Zero\",\"Natural\",\"Machine\"]" +
            "}";
        byte[] encodedAlbum = jsonToAvroSerializer.serialize("album.topic.name", jsonAlbum, false);
        byte[] expectedAlbum = toByteArray("Album.avsc", albumAvro);
        assertEquals(expectedAlbum, encodedAlbum);
    }

    @Test
    void serializeFilm() throws IOException, URISyntaxException {
        JsonToAvroSerializer jsonToAvroSerializer = new JsonToAvroSerializer(avroSerializationTopicsMapping);
        final String jsonFilm = "{" +
            "\"name\":\"Star Wars: The Force Awakens\"," +
            "\"producer\":\"J. J. Abrams\"," +
            "\"releaseYear\":2015," +
            "\"duration\":135," +
            "\"starring\":[\"Harrison Ford\",\"Mark Hamill\",\"Carrie Fisher\",\"Adam Driver\",\"Daisy Ridley\"]" +
            "}";
        byte[] encodedFilm = jsonToAvroSerializer.serialize("film.topic.name", jsonFilm, false);
        byte[] expectedFilm = toByteArray("Film.avsc", filmAvro);
        assertEquals(expectedFilm, encodedFilm);
    }

    @Test
    void serializeForNotMatchingTopic() {
        JsonToAvroSerializer jsonToAvroSerializer = new JsonToAvroSerializer(avroSerializationTopicsMapping);
        final String jsonFilm = "{" +
            "\"name\":\"Star Wars: The Force Awakens\"," +
            "\"producer\":\"J. J. Abrams\"," +
            "\"releaseYear\":2015," +
            "\"duration\":135," +
            "\"starring\":[\"Harrison Ford\",\"Mark Hamill\",\"Carrie Fisher\",\"Adam Driver\",\"Daisy Ridley\"]" +
            "}";
        byte[] encodedFilm = jsonToAvroSerializer.serialize("random.topic.name", jsonFilm, false);
        assertNull(encodedFilm);
    }

    @Test
    void serializeForKeyWhenItsTypeNotSet() {
        JsonToAvroSerializer jsonToAvroSerializer = new JsonToAvroSerializer(avroSerializationTopicsMapping);
        final String jsonFilm = "{" +
            "\"name\":\"Star Wars: The Force Awakens\"," +
            "\"producer\":\"J. J. Abrams\"," +
            "\"releaseYear\":2015," +
            "\"duration\":135," +
            "\"starring\":[\"Harrison Ford\",\"Mark Hamill\",\"Carrie Fisher\",\"Adam Driver\",\"Daisy Ridley\"]" +
            "}";
        byte[] encodedFilm = jsonToAvroSerializer.serialize("random.topic.name", jsonFilm, true);
        assertNull(encodedFilm);
    }

    @Test
    void serializeWhenTypeNotSetForKeyAndValue() {
        JsonToAvroSerializer jsonToAvroSerializer = new JsonToAvroSerializer(avroSerializationTopicsMapping);
        final String jsonFilm = "{" +
            "\"name\":\"Star Wars: The Force Awakens\"," +
            "\"producer\":\"J. J. Abrams\"," +
            "\"releaseYear\":2015," +
            "\"duration\":135," +
            "\"starring\":[\"Harrison Ford\",\"Mark Hamill\",\"Carrie Fisher\",\"Adam Driver\",\"Daisy Ridley\"]" +
            "}";
        Exception exception = assertThrows(RuntimeException.class, () -> {
            jsonToAvroSerializer.serialize("incorrect.topic.name", jsonFilm, true);
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
