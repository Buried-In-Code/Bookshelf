<img src="./app/src/main/resources/static/img/logo.png" align="left" width="128" height="128" alt="Bookshelf Logo"/>

# Bookshelf

![Java Version](https://img.shields.io/badge/Temurin-17-green?style=flat-square&logo=eclipse-adoptium)
![Kotlin Version](https://img.shields.io/badge/Kotlin-2.0.20-green?style=flat-square&logo=kotlin)
![Status](https://img.shields.io/badge/Status-Beta-yellowgreen?style=flat-square)

[![Gradle](https://img.shields.io/badge/Gradle-8.10-informational?style=flat-square&logo=gradle)](https://github.com/gradle/gradle)
[![Ktlint](https://img.shields.io/badge/Ktlint-1.3.1-informational?style=flat-square)](https://github.com/pinterest/ktlint)
[![Javalin](https://img.shields.io/badge/Javalin-6.3.0-informational?style=flat-square)](https://github.com/javalin/javalin)
[![Bulma](https://img.shields.io/badge/Bulma-1.0.1-informational?style=flat-square)](https://github.com/jgthms/bulma)

[![Github - Version](https://img.shields.io/github/v/tag/Buried-In-Code/Bookshelf?logo=Github&label=Version&style=flat-square)](https://github.com/Buried-In-Code/Bookshelf/tags)
[![Github - License](https://img.shields.io/github/license/Buried-In-Code/Bookshelf?logo=Github&label=License&style=flat-square)](https://opensource.org/licenses/MIT)
[![Github - Contributors](https://img.shields.io/github/contributors/Buried-In-Code/Bookshelf?logo=Github&label=Contributors&style=flat-square)](https://github.com/Buried-In-Code/Bookshelf/graphs/contributors)

Tool for tracking books on your bookshelf or books you wish were on it.

## Usage

### via Jar

1. Make sure you have a supported version of [Java](https://adoptium.net/temurin/releases/) installed: `java --version`
2. Clone the repo: `git clone https://github.com/Buried-In-Code/Bookshelf`
3. Build using: `./gradlew build`
4. Run using: `java -jar ./app/build/libs/app-0.4.1-all.jar`

### via Gradle

1. Make sure you have a supported version of [Java](https://adoptium.net/temurin/releases/) installed: `java --version`
2. Clone the repo: `git clone https://github.com/Buried-In-Code/Bookshelf`
3. Run using: `./gradlew build run`

### via Docker-Compose

1. Make sure you have [Docker](https://www.docker.com/) installed: `docker --version`
2. Make sure you have [Docker-Compose](https://github.com/docker/compose) installed: `docker-compose --version`
3. Create a `docker-compose.yaml` file, _an example:_

```yaml
version: '3'

services:
  bookshelf:
    image: 'ghcr.io/buried-in-code/bookshelf:latest'
    container_name: 'Bookshelf'
    environment:
      TZ: 'Pacific/Auckland'
    ports:
      - '25710:25710'
    volumes:
      - './config:/app/config'
      - './data:/app/data'
```

4. Run using: `docker-compose up -d`

## Socials

[![Social - Fosstodon](https://img.shields.io/badge/%40BuriedInCode-teal?label=Fosstodon&logo=mastodon&style=for-the-badge)](https://fosstodon.org/@BuriedInCode)\
[![Social - Matrix](https://img.shields.io/badge/%23The--Dev--Environment-teal?label=Matrix&logo=matrix&style=for-the-badge)](https://matrix.to/#/#The-Dev-Environment:matrix.org)
