package org.akhq.utils;

import com.google.protobuf.Any;
import com.google.protobuf.DoubleValue;
import com.google.protobuf.StringValue;
import org.akhq.configs.Connection.ProtobufDeserializationTopicsMapping;
import org.akhq.configs.TopicsMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProtobufToJsonDeserializerTest {
    ProtobufDeserializationTopicsMapping protobufDeserializationTopicsMapping;
    AlbumProto.Album albumProto;
    FilmProto.Film filmProto;
    ComplexProto.Complex complexProtobufObject;


    @BeforeEach
    public void before() throws URISyntaxException, IOException {
        createTopicProtobufDeserializationMapping();
        createAlbumObject();
        createFilmObject();
        createComplexObject();
    }

    private void createTopicProtobufDeserializationMapping() throws URISyntaxException, IOException {
        protobufDeserializationTopicsMapping = new ProtobufDeserializationTopicsMapping();

        URI uri = ClassLoader.getSystemResource("protobuf_desc").toURI();
        String protobufDescriptorsFolder = Paths.get(uri).toString();
        protobufDeserializationTopicsMapping.setDescriptorsFolder(protobufDescriptorsFolder);

        TopicsMapping albumTopicsMapping = new TopicsMapping();
        albumTopicsMapping.setTopicRegex("album.*");
        albumTopicsMapping.setDescriptorFile("album.desc");
        albumTopicsMapping.setValueMessageType("Album");

        TopicsMapping filmTopicsMapping = new TopicsMapping();
        filmTopicsMapping.setTopicRegex("film.*");
        String base64FilmDescriptor = encodeDescriptorFileToBase64("film.desc");
        filmTopicsMapping.setDescriptorFileBase64(base64FilmDescriptor);
        filmTopicsMapping.setValueMessageType("Film");

        // Do not specify message type neither for a key, nor for a value
        TopicsMapping incorrectTopicsMapping = new TopicsMapping();
        incorrectTopicsMapping.setTopicRegex("incorrect.*");
        String base64IncorrectDescriptor = encodeDescriptorFileToBase64("film.desc");
        incorrectTopicsMapping.setDescriptorFileBase64(base64IncorrectDescriptor);

        TopicsMapping complexObjectTopicsMapping = new TopicsMapping();
        complexObjectTopicsMapping.setTopicRegex("complex.*");
        complexObjectTopicsMapping.setDescriptorFile("complex.desc");
        complexObjectTopicsMapping.setValueMessageType("Complex");

        protobufDeserializationTopicsMapping.setTopicsMapping(
                Arrays.asList(albumTopicsMapping, filmTopicsMapping, complexObjectTopicsMapping, incorrectTopicsMapping));
    }

    private String encodeDescriptorFileToBase64(String descriptorFileName) throws URISyntaxException, IOException {
        URI uri = ClassLoader.getSystemResource("protobuf_desc").toURI();
        String protobufDescriptorsFolder = Paths.get(uri).toString();

        String fullName = protobufDescriptorsFolder + File.separator + descriptorFileName;
        byte[] descriptorFileBytes = Files.readAllBytes(Path.of(fullName));
        return Base64.getEncoder().encodeToString(descriptorFileBytes);
    }

    private void createAlbumObject() {
        List<String> artists = Collections.singletonList("Imagine Dragons");
        List<String> songTitles = Arrays.asList("Birds", "Zero", "Natural", "Machine");
        Album album = new Album("Origins", artists, 2018, songTitles);
        albumProto = AlbumProto.Album.newBuilder()
                .setTitle(album.getTitle())
                .addAllArtist(album.getArtists())
                .setReleaseYear(album.getReleaseYear())
                .addAllSongTitle(album.getSongsTitles())
                .build();
    }

    private void createFilmObject() {
        List<String> starring = Arrays.asList("Harrison Ford", "Mark Hamill", "Carrie Fisher", "Adam Driver", "Daisy Ridley");
        Film film = new Film("Star Wars: The Force Awakens", "J. J. Abrams", 2015, 135, starring);
        filmProto = FilmProto.Film.newBuilder()
                .setName(film.getName())
                .setProducer(film.getProducer())
                .setReleaseYear(film.getReleaseYear())
                .setDuration(film.getDuration())
                .addAllStarring(film.getStarring())
                .build();
    }

    private void createComplexObject() {
        BookProto.Book bookProto = BookProto.Book.newBuilder()
                .setTitle("Les Miserables")
                .setAuthor("Victor Hugo")
                .setPrice(DoubleValue.newBuilder().setValue(123d))
                .build();
        complexProtobufObject = ComplexProto.Complex.newBuilder()
                .setAlbum(albumProto)
                .setFilm(filmProto)
                .setAnything(Any.pack(bookProto))
                .setStringWrapper(StringValue.newBuilder().setValue("stringvalue").build()).build();
    }

    @Test
    public void deserializeAlbum() {
        ProtobufToJsonDeserializer protobufToJsonDeserializer = new ProtobufToJsonDeserializer(protobufDeserializationTopicsMapping);
        final byte[] binaryAlbum = albumProto.toByteArray();
        String decodedAlbum = protobufToJsonDeserializer.deserialize("album.topic.name", binaryAlbum, false);
        String expectedAlbum = "{\n" +
                "  \"title\": \"Origins\",\n" +
                "  \"artist\": [\"Imagine Dragons\"],\n" +
                "  \"releaseYear\": 2018,\n" +
                "  \"songTitle\": [\"Birds\", \"Zero\", \"Natural\", \"Machine\"]\n" +
                "}";
        assertEquals(expectedAlbum, decodedAlbum);
    }

    @Test
    public void deserializeFilm() {
        ProtobufToJsonDeserializer protobufToJsonDeserializer = new ProtobufToJsonDeserializer(protobufDeserializationTopicsMapping);
        final byte[] binaryFilm = filmProto.toByteArray();
        String decodedFilm = protobufToJsonDeserializer.deserialize("film.topic.name", binaryFilm, false);
        String expectedFilm = "{\n" +
                "  \"name\": \"Star Wars: The Force Awakens\",\n" +
                "  \"producer\": \"J. J. Abrams\",\n" +
                "  \"releaseYear\": 2015,\n" +
                "  \"duration\": 135,\n" +
                "  \"starring\": [\"Harrison Ford\", \"Mark Hamill\", \"Carrie Fisher\", \"Adam Driver\", \"Daisy Ridley\"]\n" +
                "}";
        assertEquals(expectedFilm, decodedFilm);
    }

    @Test
    public void deserializeForNotMatchingTopic() {
        ProtobufToJsonDeserializer protobufToJsonDeserializer = new ProtobufToJsonDeserializer(protobufDeserializationTopicsMapping);
        final byte[] binaryFilm = filmProto.toByteArray();
        String decodedFilm = protobufToJsonDeserializer.deserialize("random.topic.name", binaryFilm, false);
        assertNull(decodedFilm);
    }

    @Test
    public void deserializeForKeyWhenItsTypeNotSet() {
        ProtobufToJsonDeserializer protobufToJsonDeserializer = new ProtobufToJsonDeserializer(protobufDeserializationTopicsMapping);
        final byte[] binaryFilm = filmProto.toByteArray();
        String decodedFilm = protobufToJsonDeserializer.deserialize("film.topic.name", binaryFilm, true);
        assertNull(decodedFilm);
    }

    @Test
    public void deserializeWhenTypeNotSetForKeyAndValue() {
        ProtobufToJsonDeserializer protobufToJsonDeserializer = new ProtobufToJsonDeserializer(protobufDeserializationTopicsMapping);
        final byte[] binaryFilm = filmProto.toByteArray();
        Exception exception = assertThrows(RuntimeException.class, () -> {
            protobufToJsonDeserializer.deserialize("incorrect.topic.name", binaryFilm, true);
        });
        String expectedMessage = "message type is not specified neither for a key, nor for a value";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void deserializeComplexObject() {
        ProtobufToJsonDeserializer protobufToJsonDeserializer = new ProtobufToJsonDeserializer(protobufDeserializationTopicsMapping);
        final byte[] binaryComplexObject = complexProtobufObject.toByteArray();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            protobufToJsonDeserializer.deserialize("complex.topic.name", binaryComplexObject, false);
        });
        String expectedMessage = "Cannot deserialize message with Protobuf deserializer";
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}
