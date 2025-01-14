# Figstract

[![Maven Central Version](https://img.shields.io/maven-central/v/com.anifichadia.figstract/cli)](https://central.sonatype.com/namespace/com.anifichadia.figstract)
![Release](https://github.com/AniFichadia/Figstract/actions/workflows/release.yml/badge.svg)
![Build](https://github.com/AniFichadia/Figstract/actions/workflows/build.yml/badge.svg)

Figstract bridges the process for maintaining design systems (in Figma) between frontend engineers and designers and helps automate mundane upkeep tasks.

This tool extracts the following design system tokens produced by designers in [Figma](https://www.figma.com/):

- Assets (raster and vector assets like images and icons)
- Design system [variables](https://help.figma.com/hc/en-us/articles/15339657135383-Guide-to-variables-in-Figma) (booleans, numbers, strings, and colors)

And imports them in native formats for frontend engineers to use on:

- Web
- Android (natively, using Android SDKs)
- iOS (natively, using Apple / iOS SDKs)

Figstract comes with an out-of-the-box CLI, which includes options for filtering and extraction, but has been designed to be modular so engineers can compose their own CLIs or other tools based on Figstract's foundation.

> [!NOTE]
> While Figstract is pre-release, some CLI options may change

## Features and internals

Figstract uses the [Figma REST API](https://www.figma.com/developers/api) and has the following features:

- **Concurrent and Parallel**: Uses concurrency and multithreading to process Figma files at speed
- **Multi-file**: Operations can handle multiple Figma files in parallel
- **Flexible filters**: Manage included and excluded tokens

Figstract operations use a processing pipeline:

1. Reading: Accessing Figma files
2. Exporting: Extracting tokens
3. Importing: Converting tokens

## Usage

### Compiling

To create a runnable fat JAR, just run the following Gradle command:

```shell
./gradlew :cli:shadowJar
```

The compiled JAR will be located in `cli/build/libs/cli-0.0.1-alpha01-all.jar` (make sure you use the `.jar` with the name ending in `-all`).

## Running

Run the following command to run Figstract and list the subcommands and options:

```shell
java -jar /path/to/cli.jar --help
```

The CLI can be configured with CLI args, or by supplying a `[subcommandName].properties` file with the same keys as the arguments in the working directory (e.g. `assets.properties`).

## Authentication

Figstract supports the following authentication mechanisms to accessing Figma:

- Personal Access Tokens (PATs).
  Refer to the [Figma documentation](https://help.figma.com/hc/en-us/articles/8085703771159-Manage-personal-access-tokens) and [API reference](https://www.figma.com/developers/api#authentication) for more info.

### Scopes

When generating credentials, ensure that the following [scopes](https://www.figma.com/developers/api#authentication-scopes) are configured.

| Operation | Required scope              |
|-----------|-----------------------------|
| assets    | `File content`              |
| variables | `File content`, `Variables` |

> [!CAUTION]
> Ensure scopes are set to `Read only` and tokens are refreshed regularly

Either one auth credential can be generated with all the scopes above, or specific auth credentials can be created for each subcommand.

## Logging

Figstract uses [kotlin-logging](https://github.com/oshai/kotlin-logging) and [Logback](https://logback.qos.ch/) for logging, and logs errors to the console by default.
When using the CLI, the log level can be configured using the `--logLevel` option (e.g.
`--logLevel DEBUG`), or by configuring logback using environment variables (refer to https://logback.qos.ch/manual/configuration.html#configFileProperty).

## Assets

TODO:

- supported output formats
    - Web
    - Android
        - WEBP images automatically scaled in density buckets
        - Android Vector Drawables which have been magenta-fied for tinting
    - iOS
        - [Asset catalogs](https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/index.html) with scale support
- Asset format within figma files
- Composable pipeline for importing and converting assets
- JsonPath support
    - Supports Stefan Goessner's JsonPath implementation using https://github.com/json-path/JsonPath
    - JsonPath expressions are relative to each Canvas and should locate the required node. Refer to the [Figma API Node reference](https://www.figma.com/developers/api#node-types)
    - Canvas and node filters will be applied, but parent node filters aren't supported for now
- Custom naming

## Variables

Figstract can extract [local variables](https://www.figma.com/developers/api#get-local-variables-endpoint) from Figma files.
All variable types (booleans, numbers, strings and colors) are supported.

All variable types will be outputted by default, but can be configured to be omitted completely.

### Output formats

All output formats use the variable collection's name as the file name.
Variables will be first grouped by mode then by variable type within each variable collection's file.
Colors can be configured to be outputted as either Hex or RGBA values (RGBA values are floats between 0 and 1 inclusive).

Note: Variable collection, mode and variable names will be sanitised to conform with platform / language conventions.

#### Web / Any

For web (or any platform), Figstract generates JSON files with the following format:

*My Variable Collection.json*

```json
{
  "My Mode": {
    "booleans": {
      "bool var 1": true,
      "bool var 2": false
    },
    "numbers": {
      "number var 1": 123.45,
      "number var 2": -543.21
    },
    "strings": {
      "string var 1": "Hello",
      "string var 2": "World"
    },
    "colors": {
      "color rgba var 1": {
        "r": 0.1,
        "g": 0.2,
        "b": 0.3,
        "a": 1.0
      },
      "color rgba var 2": {
        "r": 1.0,
        "g": 1.0,
        "b": 1.0,
        "a": 1.0
      },
      "color hex var 1": "0xFF19334C",
      "color hex var 2": "0xFFFFFFFF"
    }
  }
}
```

Refer to [JsonVariableDataWriter](library-core/src/main/java/com/anifichadia/figstract/importer/variable/model/JsonVariableDataWriter.kt) for the implementation.

#### Android Compose

Figstract generates an R-file-like Kotlin object with constants with the following format:

*MyVariableCollection.kt*

```kotlin
package your.pkg.here

import androidx.compose.ui.graphics.Color
import kotlin.Double

public object MyVariableCollection {
    public object MyMode {
        public object Booleans {
            public val boolVar1: Boolean = true
            public val boolVar2: Boolean = false
        }

        public object Numbers {
            public val numberVar1: Double = 123.45
            public val numberVar2: Double = -543.21
        }

        public object Strings {
            public val stringVar1: String = "Hello"
            public val stringVar2: String = "World"
        }

        public object Colors {
            public val colorRgbaVar1: Color = Color(
                red = 0.1f,
                green = 0.2f,
                blue = 0.3f,
                alpha = 1.0f,
            )
            public val colorRgbaVar2: Color = Color(
                red = 1.0f,
                green = 1.0f,
                blue = 1.0f,
                alpha = 1.0f,
            )
            public val colorHexVar1: Color = Color(0xFF19334C)
            public val colorHexVar2: Color = Color(0xFFFFFFFF)
        }
    }
}
```

The package must be specified when configuring this output.

Refer to [AndroidComposeVariableDataWriter](library-android/src/main/java/com/anifichadia/figstract/android/importer/variable/model/AndroidComposeVariableDataWriter.kt) for the implementation.

#### Android XML

> [!NOTE]
> Coming soon

#### iOS

> [!NOTE]
> Coming soon

## Module structure

TODO

## In development

- Shell wrapper
- Github action support
- Better error handling
