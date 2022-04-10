# Komga metadata fetcher

## Features

- automatically pick up added series and update their metadata and thumbnail
- manually search and identify series (rest endpoints only)
- match entire library or a series (rest endpoints only)

## Build

1. `./gradlew clean shadowjar` (output is in /build/libs)

## Run

### Jar

Requires Java 11 or higher

`java -jar komf-1.0-SNAPSHOT-all.jar <path to config>`

### Docker compose

```yml
version: "2.1"
services:
  komf:
    image: sndxr/komf:latest
    container_name: komf
    ports:
      - 8085:8085
    environment: # optional env config
      - KOMF_KOMGA_BASE_URI=http://komga:8080
      - KOMF_KOMGA_USER=admin@example.org
      - KOMF_KOMGA_PASSWORD=admin
      - KOMF_LOG_LEVEL=INFO
    volumes:
      - /path/to/config:/config #path to directory with application.yml and database file
    restart: unless-stopped
```

## Example application.yml config

```yml
komga:
  baseUri: http://localhost:8080 #or env:KOMF_KOMGA_BASE_URI
  komgaUser: admin@example.org #or env:KOMF_KOMGA_USER
  komgaPassword: admin #or env:KOMF_KOMGA_PASSWORD
  eventListener:
    enabled: true
    libraries: [ ]  #listen to all events if empty
  metadataUpdate:
    bookThumbnails: false #update book thumbnails
    seriesThumbnails: true #update series thumbnails
    seriesTitle: false #update series title
database:
  file: ./database.sqlite #database file location.
metadataProviders:
  mangaUpdates:
    priority: 10
    enabled: true
  mal: #requires clientId. See https://myanimelist.net/forum/?topicid=1973077
    clientId: ""
    priority: 20
    enabled: false
  nautiljon:
    priority: 30
    enabled: false
    fetchBookMetadata: false #fetch volume information and thumbnails if available. Can take a while to load
    useOriginalPublisher: false # use original publisher and release dates for series and volumes. If false will use french publisher
    originalPublisherTag:  #if present will add additional tag with specified name ({tagname}: publisherName)
    frenchPublisherTag:  #if present will add additional tag with specified name ({tagname}: publisherName)
server:
  port: 8085 #or env:KOMF_SERVER_PORT
logLevel: INFO #or env:KOMF_LOG_LEVEL
```

## Http endpoints

`GET /providers` - list of enabled metadata providers

`GET /search?name=...` - search results from enabled metadata providers

`POST /identify` - set series metadata from specified provider

```json
{
  "seriesId": "07XF6HKAWHHV4",
  "provider": "MANGA_UPDATES",
  "providerSeriesId": "1"
}
```

`POST /match/series/{seriesId}`try to match series. Optional `provider` param can be passed to use only specified
provider

`POST /match/library/{libraryId}` try to match series of a library. Optional `provider` param can be passed
