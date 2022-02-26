# Komga metadata fetcher

## Features

- automatically pick up added series and update their metadata and thumbnail
- manually search and identify series (rest endpoints only)
- match entire library or a series (rest endpoints only)

## Build

1. `./gradlew clean shadowjar` (output is in /build/libs)
2. (optional) `docker build . --tag komf`

## Run

### Jar
Requires Java 11 or higher

`java -jar komf-1.0-SNAPSHOT-all.jar <path to config>`

### Docker compose

```yml
version: "2.1"
services:
  komf:
    image: komf
    container_name: komf
    ports:
      - 8075:8075
    environment: # optional env config
      - KOMF_KOMGA_BASE_URI=http://komga:8080
      - KOMF_KOMGA_USER=admin@example.org
      - KOMF_KOMGA_PASSWORD=admin
      - KOMF_LOG_LEVEL=INFO
    volumes:
      - /path/to/config:/config #optional path to dir with application.yml
    restart: unless-stopped
```

## Example application.yml config

```yml
komga:
  baseUri: http://localhost:8080 #or env:KOMF_KOMGA_BASE_URI
  komgaUser: admin@example.org #or env:KOMF_KOMGA_USER
  komgaPassword: admin #or env:KOMF_KOMGA_PASSWORD
  eventListener:
    enabled: true #or env:KOMF_KOMGA_EVENT_LISTENER_ENABLED
    libraries: #Listen events only from specified libraries 
      - 07XF6HK1WHZKF
    #libraries: []  #listen to all events
metadataProviders:
  mal: #requires clientId. See https://myanimelist.net/forum/?topicid=1973077
    clientId: "" #or env:KOMF_MAL_CLIENT_ID
    priority: 20 #or env:KOMF_MAL_PRIORITY
    enabled: false #or env:KOMF_MAL_ENABLED
  mangaUpdates:
    priority: 10 #or env:KOMF_MANGAUPDATES_PRIORITY
    enabled: true #or env:KOMF_MANGAUPDATES_ENABLED
server:
  port: 8075 #or env:KOMF_SERVER_PORT
logLevel: INFO #or env:KOMF_LOG_LEVEL
```

## REST endpoints

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
`POST /match/series/{seriesId}` attempt to automatically match series

`POST /match/library/{libraryId}` attempt to automatically match series of a library
