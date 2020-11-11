#!/usr/bin/env bash

# This script generates AlbumProto and AlbumFilm Java classes from album.proto and film.proto files
# Run it every time when change album.proto and film.proto files

SRC_DIR="."
DST_DIR="../../java"
protoc -I=${SRC_DIR} --java_out=${DST_DIR} ${SRC_DIR}/film.proto