# Komga and Kavita Metadata Fetcher

## Overview

Komga and Kavita Metadata Fetcher is a tool that fetches metadata and thumbnails for your digital comic book library. It
can automatically pick up added series and update their metadata and thumbnail. You can also manually search and
identify series, or match the entire library or a series. Additionally, you can install
the [Komf userscript](https://github.com/Snd-R/komf-userscript) to add Komf integration directly to Komga and Kavita UI,
allowing you to launch manual or automatic metadata identification.

## Features

- automatically pick up added series and update their metadata and thumbnail
- manually search and identify series (http endpoints or cli commands)
- match entire library or a series (http endpoints or cli commands)

## Building

To build the application, follow these steps:

1. Run `./gradlew :komf-app:clean :komf-app:shadowjar`.
2. The output will be in `komf-app/build/libs`.

## Running

To run the application, you can either use the JAR file or Docker Compose.

### Running with JAR

To run the application using the JAR file, follow these steps:

1. Ensure you have Java 17 or higher installed on your system.
2. Run `java -jar komf-1.0-SNAPSHOT-all.jar <path to config>`.

### Running with Docker Compose

To run the application using Docker Compose, use the following YAML configuration:

```yml
version: "3.7"
services:
  komf:
    image: sndxr/komf:latest
    container_name: komf
    ports:
      - "8085:8085"
    user: "1000:1000"
    environment:
      - KOMF_KOMGA_BASE_URI=http://komga:25600
      - KOMF_KOMGA_USER=admin@example.org
      - KOMF_KOMGA_PASSWORD=admin
      - KOMF_KAVITA_BASE_URI=http://kavita:5000
      - KOMF_KAVITA_API_KEY=16707507-d05d-4696-b126-c3976ae14ffb
      - KOMF_LOG_LEVEL=INFO
      # optional jvm options. Example config for low memory usage. Runs guaranteed cleanup up every 3600000ms(1hour)
      - JAVA_TOOL_OPTIONS=-XX:+UnlockExperimentalVMOptions -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=compact -XX:ShenandoahGuaranteedGCInterval=3600000 -XX:TrimNativeHeapInterval=3600000
    volumes:
      - /path/to/config:/config #path to directory with application.yml and database file
    restart: unless-stopped
```

### Running with Docker Create

```
docker create \
  --name komf \
  -p 8085:8085 \
  -u 1000:1000 \
  -e KOMF_KOMGA_BASE_URI=http://komga:25600 \
  -e KOMF_KOMGA_USER=admin@example.org \
  -e KOMF_KOMGA_PASSWORD=admin \
  -e KOMF_KAVITA_BASE_URI=http://kavita:5000 \
  -e KOMF_KAVITA_API_KEY=16707507-d05d-4696-b126-c3976ae14ffb \
  -e KOMF_LOG_LEVEL=INFO \
  -v /path/to/config:/config \
  --restart unless-stopped \
  sndxr/komf:latest
```

- if you don't already have a komga or kavita network you'll need to network create a new one
    - `docker network create my_network`
- attach komf and media server to new network:
    - `docker network connect my_network komga_or_kavita`
    - `docker network connect my_network komf`
- start the container `docker start komf`

## Example `application.yml` Config

### Important

- Update modes is the way komf will update metadata.
- If you're using anything other than API then your existing files might be modified with embedded metadata
- Can use multiple options at once. available options are API, COMIC_INFO
- Experimental OPF mode is available for epub books. This mode is using calibre system install to update metadate

```yml
komga:
  baseUri: http://localhost:25600 #or env:KOMF_KOMGA_BASE_URI
  komgaUser: admin@example.org #or env:KOMF_KOMGA_USER
  komgaPassword: admin #or env:KOMF_KOMGA_PASSWORD
  eventListener:
    enabled: false # if disabled will not connect to komga and won't pick up newly added entries
    metadataLibraryFilter: [ ]  # listen to all events if empty
    metadataSeriesExcludeFilter: [ ]
    notificationsLibraryFilter: [ ] # Will send notifications if any notification source is enabled. If empty will send notifications for all libraries
  metadataUpdate:
    default:
      libraryType: "MANGA" # Can be "MANGA", "NOVEL" or "COMIC". Hint to help better match book numbers
      updateModes: [ API ] # can use multiple options at once. available options are API, COMIC_INFO
      aggregate: false # if enabled will search and aggregate metadata from all configured providers
      mergeTags: false # if true and aggregate is enabled will merge tags from all providers
      mergeGenres: false # if true and aggregate is enabled will merge genres from all providers
      bookCovers: false # update book thumbnails
      seriesCovers: false # update series thumbnails
      overrideExistingCovers: true # if false will upload but not select new cover if another cover already exists
      overrideComicInfo: false # Replace existing ComicInfo file. If false, only append additional data
      postProcessing:
        seriesTitle: false # update series title
        seriesTitleLanguage: "en" # series title update language. If empty chose first matching title
        fallbackToAltTitle: false # fallback to first alternative tile if series title is not found
        alternativeSeriesTitles: false # use other title types as alternative title option
        alternativeSeriesTitleLanguages: # alternative title languages
          - "en"
          - "ja"
          - "ja-ro"
        orderBooks: false # will order books using parsed volume or chapter number
        scoreTagName: "score" # adds score tag of specified format e.g. "score: 8" only uses integer part of rating. Can be used in search using query: tag:"score: 8" in komga
        readingDirectionValue: # override reading direction for all series. should be one of these: LEFT_TO_RIGHT, RIGHT_TO_LEFT, VERTICAL, WEBTOON
        languageValue: # set default language for series. Must use BCP 47 format e.g. "en"
        #TagName: if specified and if provider has data about publisher in that language then additional tag will be added using format ({TagName}: publisherName)
        #e.g. originalPublisherTagName: "Original Publisher" will add tag "Original Publisher: Shueisha"
        originalPublisherTagName:
        #publisherTagNames:
        #  - tagName: "English Publisher"
        #    language: "en"

kavita:
  baseUri: "http://localhost:5000" #or env:KOMF_KAVITA_BASE_URI
  apiKey: "16707507-d05d-4696-b126-c3976ae14ffb" #or env:KOMF_KAVITA_API_KEY
  eventListener:
    enabled: false # if disabled will not connect to kavita and won't pick up newly added entries
    metadataLibraryFilter: [ ]  # listen to all events if empty
    metadataSeriesExcludeFilter: [ ]
    notificationsLibraryFilter: [ ] # Will send notifications if any notification source is enabled. If empty will send notifications for all libraries
  metadataUpdate:
    default:
      libraryType: "MANGA" # Can be "MANGA", "NOVEL" or "COMIC". Hint to help better match book numbers
      updateModes: [ API ] # can use multiple options at once. available options are API, COMIC_INFO
      aggregate: false # if enabled will search and aggregate metadata from all configured providers
      mergeTags: false # if true and aggregate is enabled will merge tags from all providers
      mergeGenres: false # if true and aggregate is enabled will merge genres from all providers
      bookCovers: false #update book thumbnails
      seriesCovers: false #update series thumbnails
      overrideExistingCovers: true # if false will upload but not select new cover if another cover already exists
      lockCovers: true # lock cover images so that kavita does not change them
      postProcessing:
        seriesTitle: false #update series title
        seriesTitleLanguage: "en" # series title update language. If empty chose first matching title
        alternativeSeriesTitles: false # use other title types as alternative title option
        alternativeSeriesTitleLanguages: # alternative title language. Only first language is used. Use single value for consistency
          - "ja-ro"
        orderBooks: false # will order books using parsed volume or chapter number. works only with COMIC_INFO
        languageValue: # set default language for series. Must use BCP 47 format e.g. "en"

notifications:
  templatesDirectory: "./" # path to a directory with templates
  discord:
    # List of discord webhook urls. Will call these webhooks after series or books were added. 
    webhooks: # config example: webhooks: ["https://discord.com/api/webhooks/9..."] (env:KOMF_DISCORD_WEBHOOKS - comma separated list of webhooks)
    seriesCover: false # include series cover in message
    embedColor: "1F8B4C"
  apprise:
    # List of apprise urls. Will call these after series or books were added. 
    urls:
    seriesCover: false # include series cover as attachment

database:
  file: ./database.sqlite # database file location.

metadataProviders:
  malClientId: "" # required for mal provider. See https://myanimelist.net/forum/?topicid=1973077 env:KOMF_METADATA_PROVIDERS_MAL_CLIENT_ID
  comicVineApiKey: # required for comicVine provider https://comicvine.gamespot.com/api/ env:KOMF_METADATA_PROVIDERS_COMIC_VINE_API_KEY
  bangumiToken: # bangumi provider require a token to show nsfw items https://next.bgm.tv/demo/access-token  env:KOMF_METADATA_PROVIDERS_BANGUMI_TOKEN
  defaultProviders:
    mangaUpdates:
      priority: 10
      enabled: true
      mediaType: "MANGA" # filter used in matching. Can be NOVEL or MANGA. MANGA type includes everything except novels
      authorRoles: [ "WRITER" ] # roles that will be mapped to author role
      artistRoles: [ "PENCILLER","INKER","COLORIST","LETTERER","COVER" ] # roles that will be mapped to artist role
    mal:
      priority: 20
      enabled: false
      mediaType: "MANGA" # filter used in matching. Can be NOVEL or MANGA. MANGA type includes everything except novels
    nautiljon:
      priority: 30
      enabled: false
    aniList:
      priority: 40
      enabled: false
      mediaType: "MANGA" # filter used in matching. Can be NOVEL or MANGA. MANGA type includes everything except novels
      tagsScoreThreshold: 60 # tags with this score or higher will be included
      tagsSizeLimit: 15 # amount of tags that will be included
    yenPress:
      priority: 50
      enabled: false
      mediaType: "MANGA" # filter used in matching. Can be NOVEL or MANGA.
    kodansha:
      priority: 60
      enabled: false
    viz:
      priority: 70
      enabled: false
    bookWalker:
      priority: 80
      enabled: false
      mediaType: "MANGA" # filter used in matching. Can be NOVEL or MANGA.
    mangaDex:
      priority: 90
      enabled: false
      coverLanguages:
        - "en"
        - "ja"
    bangumi: # Chinese metadata provider. https://bgm.tv/
      priority: 100
      enabled: false
    comicVine: # https://comicvine.gamespot.com/ requires API key. Experimental provider, can mismatch issue numbers
      priority: 110
      enabled: false
    hentag:
      priority: 120
      enabled: false

server:
  port: 8085 # or env:KOMF_SERVER_PORT

logLevel: INFO # or env:KOMF_LOG_LEVEL
```

## Metadata update config for a library

You can configure a set of metadata update options that will only be used with specified library. If no options are
specified for a library
then default options will be used. kavita or komga library ids are used as library identifiers

```yaml
komga_or_kavita:
  metadataUpdate:
    default:
      aggregate: false
    library:
      09PERX1TW8GEK:
        updateModes: [ API ]
        aggregate: false
        bookCovers: false
        seriesCovers: false
        postProcessing:
          seriesTitle: false
          titleType: LOCALIZED
          alternativeSeriesTitles: false
          languageValue:
      123:
        aggregate: true
        seriesCovers: true
```

## Providers config for a library

You can configure a set of metadata providers that will only be used with specified library. If no providers are
specified for a library
then default providers will be used. kavita or komga library ids are used as library identifiers

```yaml
metadataProviders:
  defaultProviders:
    mangaUpdates:
      priority: 10
      enabled: true
  libraryProviders:
    09PERX1TW8GEK:
      mangaUpdates:
        priority: 10
        enabled: true
      bookWalker:
        priority: 20
        enabled: true
    123:
      aniList:
        priority: 10
        enabled: true
      mal:
        priority: 20
        enabled: true
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
  default:
    mangaUpdates:
      priority: 10
      enabled: true
      authorRoles: [ "WRITER" ]
      artistRoles: [ "PENCILLER","INKER","COLORIST","LETTERER","COVER" ]
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
        releaseDate: true
        links: true
        score: true
        books: true
        useOriginalPublisher: true # prefer original publisher and volume information if source has data about multiple providers. If false will use english or other available publisher
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
  default:
    mangaUpdates:
      priority: 10
      enabled: true
      seriesMetadata:
        thumbnail: false
```

## Notifications

if any webhook urls are specified then after new book is added a call to webhooks will be triggered. You can change
message format by providing your own template files and specifying directory path in `templatesDirectory/discord` or
`templatesDirectory/apprise`.
For docker deployments templates should be
placed in mounted `/config/<discord or apprise>` directory without specifying `templatesDirectory`

### Discord template file names:

- title.vm
- title_url.vm
- description.vm
- footer.vm
- field_<index>_name<_inline>.vm
- field_<index>_value.vm

### Apprise template file names:

- apprise_title.vm
- apprise_body.vm

Templates are written using Apache Velocity ([link to docs](https://velocity.apache.org/engine/2.3/user-guide.html)).

```velocity
## Example of the default description template
**$series.name**

#if ($series.metadata.summary != "")
    $series.metadata.summary

#end
#if($books.size() == 1)
***new book was added to library $library.name:***
#else
***new books were added to library $library.name:***
#end
#foreach ($book in $books)
**$book.name**
#end
```

```typescript
// Variables available in templates:
interface Webhook {
    library: {
        id: string,
        name: string
    },
    series: {
        id: string,
        name: string,
        bookCount: number,
        metadata: {
            status: string,
            title: string,
            titleSort: string,
            alternativeTitles: { label: string, title: string }[],
            summary: string,
            readingDirection?: string,
            publisher?: string,
            alternativePublishers: string[],
            ageRating?: number,
            language?: string,
            genres: string[],
            tags: string[],
            totalBookCount?: number,
            authors: { name: string, role: string }[],
            releaseYear: number,
            liks: { label: string, url: string }[],
        }
    },
    books: {
        id: string,
        name: string,
        number: int,
        metadata: {
            title: string,
            summary: string,
            number: string,
            releaseDate: string,
            authors: { name: string, role: string }
            tags: string[],
            isbn?: string,
            links: { label: string, url: string }[]
        }
    }[],
    mediaServer: string //can be `KOMGA` or `KAVITA`
}
```

## HTTP Endpoints (deprecated)

Use Komga or Kavita in place of `{media-server}`.

### Providers

Use the following HTTP endpoints to get information about enabled metadata providers:

- `GET /{media-server}/providers`: list of enabled metadata providers. Optional `libraryId` parameter can be used for
  library providers.

### Search

Use the following HTTP endpoint to search for metadata:

- `GET /{media-server}/search?name=...`: search results from enabled metadata providers. Optional `libraryId` parameter
  can be used for library providers.

### Identify

Use the following HTTP endpoint to set series metadata from specified provider:

- `POST /{media-server}/identify`:

```json
{
  "libraryId": "09TDSWK3Q0XRA",
  "seriesId": "07XF6HKAWHHV4",
  "provider": "MANGA_UPDATES",
  "providerSeriesId": "1"
}

```

- `POST /{media-server}/match/library/{libraryId}/series/{seriesId}`: Attempts to match the specified series in the
  specified library.
- `POST /{media-server}/match/library/{libraryId}`: Attempts to match all series in the specified library.
- `POST /{media-server}/reset/library/{libraryId}/series/{seriesId}`: Resets all metadata for the specified series in
  the specified library.
- `POST /{media-server}/reset/library/{libraryId}`: Resets all metadata for all series in the specified library.
