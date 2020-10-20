#!/usr/bin/env bash

# This script generates album.desc and film.desc files (binary protobuf descriptors) from album.proto and film.proto files

protoc --descriptor_set_out="../protobuf_desc/film.desc" --include_imports film.proto