# Komga metadata fetcher

## Features

- automatically pick up added series and update their metadata and thumbnail
- manually search and identify series (rest endpoints only)
- match entire library or a series (rest endpoints only)

In addition, you can also install [userscript](https://github.com/Snd-R/komf-userscript) that adds komf integration
directly to komga ui allowing you to launch manual or automatic metadata identification

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
    readingDirectionValue: #override reading direction for all series. should be one of these: LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL, WEBTOON
  aggregateMetadata: false #if enabled will search and aggregate metadata from all configured providers
discord:
  webhooks: #list of discord webhook urls. Will call these webhooks after series or books were added
  templatesDirectory: "./" #path to a directory with discordWebhook.vm template
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
    useOriginalPublisher: false # use original publisher and release dates for series and volumes. If false will use french publisher
    originalPublisherTag:  #if present will add additional tag with specified name ({tagname}: publisherName)
    frenchPublisherTag:  #if present will add additional tag with specified name ({tagname}: publisherName)
  aniList:
    priority: 40
    enabled: false
  yenPress:
    priority: 50
    enabled: false
server:
  port: 8085 #or env:KOMF_SERVER_PORT
logLevel: INFO #or env:KOMF_LOG_LEVEL
```

## Metadata aggregation

By default, all metadata will be fetched from the first positive match in configured providers by order of priority. If
you want to enable metadata aggregation from multiple sources you need to set `aggregateMetadata` to true in the config.

If enabled, initial metadata will be taken from the first positive match in configured providers. Additional search
request will be made to all the other configured providers and metadata will be aggregated from the results. Metadata
fields will only be set from another provider if previous provider did not have any data for that particular field. For
example provider1 did not return thumbnail in that case thumbnail will be taken from provider2

You can configure which fields each provider will have in the config both for series and books. By default, all
available fields will be fetched. Example of default fields configuration

```yml
metadataProviders:
  mangaUpdates:
    priority: 10
    enabled: true
    seriesMetadata:
      status: true
      title: true
      titleSort: true
      summary: true
      publisher: true
      readingDirection: true
      ageRating: true
      language: true
      genres: true
      tags: true
      totalBookCount: true
      authors: true
      thumbnail: true
      books: true
    bookMetadata:
      title: true
      summary: true
      number: true
      numberSort: true
      releaseDate: true
      authors: true
      tags: true
      isbn: true
      links: true
      thumbnail: true
```
If you want to disable particular field you just need to set the field value to false
```yml
metadataProviders:
  mangaUpdates:
    priority: 10
    enabled: true
    seriesMetadata:
      thumbnail: false
```

## Discord notifications

if any webhook urls are specified then after new book is added a call to webhooks will be triggered. You can change
message format by providing your own template file called `discordWebhook.vm` and specifying directory path to this
template in `templatesDirectory` under discord configuration. For docker deployments `discordWebhook.vm` should be
placed in mounted `/config` directory without specifying `templatesDirectory`

Templates are written using Apache Velocity ([link to docs](https://velocity.apache.org/engine/2.3/user-guide.html)).
Example of a template:

```velocity
**$series.name**

#if ($series.summary != "")
    $series.summary

#end
***new books were added to library $library.name***:
#foreach ($book in $books)
**$book.name**
#end
```

Variables available in template: `library.(name)`, `series.(id, name, summary)`, `books.(id, name)`(list of book
objects)

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

`POST /match/series/{seriesId}`try to match series

`POST /match/library/{libraryId}` try to match series of a library
