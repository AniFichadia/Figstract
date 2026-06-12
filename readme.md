# Figstract

[![Maven Central Version](https://img.shields.io/maven-central/v/com.anifichadia.figstract/cli)](https://central.sonatype.com/namespace/com.anifichadia.figstract)
[![Release](https://github.com/AniFichadia/Figstract/actions/workflows/release-deploy.yml/badge.svg)](https://github.com/AniFichadia/Figstract/actions/workflows/release.yml)
[![Build](https://github.com/AniFichadia/Figstract/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/AniFichadia/Figstract/actions/workflows/build.yml?query=branch%3Amain)

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
- **Renaming**: Remap canvas and node names before output using a JSON file
- **Theme-aware**: Supports light/dark theme variant mapping for variables, generating separate light and dark values in a single pass

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

The compiled JAR will be located in `cli/build/libs/cli-<version>-all.jar` (make sure you use the `.jar` with the name ending in `-all`).

## Running

Run the following command to run Figstract and list the subcommands and options:

```shell
java -jar /path/to/cli.jar --help
```

The CLI can be configured with CLI args, or by supplying a `[subcommandName].properties` file with the same keys as the arguments in the working directory (e.g. `assets.properties`).

### Custom CA Certificates (Corporate networks)

Some corporates may use of custom CA certificates for internal monitoring, causing the JVM to fail certificate verification and prevent access to the Figma API or where assets are stored.
You can create a custom truststore that includes your organisation's CA certificates:

1. Copy default JVM cacerts:
    ```shell
    cp $JAVA_HOME/lib/security/cacerts ~/.figstract-cacerts
    ```
2. Export your organisation's CA certs as a `.pem` or `.cer` file.
   This really depends on how CA certs are managed within your organisation.
   On MacOS, these may be located in **Keychain access** under the **System** or **System Roots** keychains.
   CA certs may also be provided for use for these purposes.
3. Import each CA cert into the truststore using (include the appropriate `<ca-cert>` and
   `/path/to/ca/cert.pem` values per cert):
    ```shell
    keytool -importcert \
      -alias <cert-alias> \
      -file /path/to/ca/cert.pem \
      -keystore ~/.figstract-cacerts \
      -storepass changeit \
      -noprompt
    ```
4. Configure truststore when running Figstract by providing the java truststore flags (`javax.net.ssl.trustStore` and
   `javax.net.ssl.trustStorePassword`):
    ```shell
    java -Djavax.net.ssl.trustStore=~/.figstract-cacerts \
         -Djavax.net.ssl.trustStorePassword=changeit \
         -jar /path/to/cli.jar [options]
   ```

### Proxy

If your network requires a proxy to reach the Figma API or asset download URLs, configure it using:

- `--proxyType`: `HTTP` or `SOCKS` (default: none)
- `--proxyHost`: proxy hostname
- `--proxyPort`: proxy port

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
When using the CLI, the log level can be configured using the `--logLevel` option (e.g. `--logLevel DEBUG`), or by configuring logback using environment variables (refer to https://logback.qos.ch/manual/configuration.html#configFileProperty).

## Assets

Figstract extracts two types of assets from Figma files: **artwork** (raster images) and **icons** (vector graphics).
Both are configured independently and can target multiple platforms in a single run.

### Asset format in Figma files

By default, Figstract locates assets by traversing the Figma node tree:

- **Artwork** are parent nodes containing a child with an image fill.
  Cropped and uncropped variants are produced by exporting either the parent node or the image fill child.
- **Icons** are `Component` nodes containing `Vector` children, placed as direct children of a canvas (page)

If your Figma file uses a non-standard layout, use [JsonPath](#jsonpath) to locate nodes instead.

### Figma file targeting

Each asset handler targets a single Figma file using its file key.
The file can be targeted at a specific version or branch.
Figma file branches have their own distinct file key, but can alternatively be accessed using the parent file's key combined with `--[artwork | icon]FigmaFileBranchName`.

The following args provide file targeting options:
- `--artworkFigmaFile` / `--iconsFigmaFile`: the Figma file key
- `--artworkFigmaFileBranchName` / `--iconsFigmaFileBranchName`: file branch name
- `--artworkFigmaFileVersion` / `--iconsFigmaFileVersion`: file version

### Composable pipeline

Asset importing uses a composable pipeline, allowing you to chain transformers and handlers together.
This makes it possible to apply format conversion, renaming, and other processing steps in a flexible way.

### Filtering

Assets can be filtered by canvas (page), node, and parent node name using regex patterns.
Include and exclude filters are mutually exclusive.
Filters can be repeated to supply multiple patterns.

- Canvas filters: `--artworkFilterIncludedCanvas` / `--artworkFilterExcludedCanvas` (and `icons` equivalents)
- Node filters: `--artworkFilterIncludedNode` / `--artworkFilterExcludedNode`
- Parent node filters: `--artworkFilterIncludedParentNode` / `--artworkFilterExcludedParentNode`

### Renaming

Canvas and node names can be remapped before output using a JSON renaming map file using `--artworkRenamingMap <path>` and `--iconsRenamingMap <path>`.
This is useful for normalizing names that don't follow engineering naming conventions without modifying the Figma file itself.

The file contains uses the following format where `canvases` and `nodes` are dictionaries of old name (case-sensitive) to new name:

```json
{
  "canvases": {
    "Old Canvas Name": "New Canvas Name"
  },
  "nodes": {
    "old/node/name": "new/node/name"
  }
}
```

Non-matching entries will produce a warning in the log.

### JsonPath

[JsonPath](https://github.com/json-path/JsonPath) expressions (Stefan Goessner's implementation) can be used to locate nodes.
Expressions are relative to each canvas and should locate the required node (refer to the [Figma API Node reference](https://www.figma.com/developers/api#node-types) for node types).
Canvas and node filters will be applied, but parent node filters aren't supported when using JsonPath.

Use `--artworkJsonPath` or `--iconsJsonPath` to supply the expression.

### Custom naming

Output file names can be customised using a format string.
The following tokens are supported, wrapped in `{}`:

- `canvas.id` — the canvas (page) ID
- `canvas.name` — the canvas (page) name
- `node.id` — the node ID
- `node.name` — the node name
- `node.name.split "<sep>" first` — splits the node name on `<sep>` (a regex) and takes the first segment
- `node.name.split "<sep>" last` — splits the node name on `<sep>` (a regex) and takes the last segment
- `node.name.split "<sep>" <N>` — splits the node name on `<sep>` (a regex) and takes the segment at index `N`

If the separator is not found in the node name, the full name is used.

Names are automatically cased to match platform conventions (snake_case for Android and Web, UpperCamelCase for iOS).
Override the format using `--artworkAndroidFormat`, `--artworkIosFormat`, `--artworkWebFormat` (and `icons` equivalents).

### Processing records

Processing records prevent re-processing Figma files that haven't changed since the last run, based on the file's last-modified timestamp.
The record is stored as `processing_record.json` in the output directory.

- `--processingRecordEnabled` (default `true`): enable or disable processing records
- `--processingRecordName`: a unique name suffix for the record file, useful when running multiple configurations against the same Figma file (e.g. `processing_record_icons.json`)

### Output formats

At least one platform must be enabled (`--platformAndroid`, `--platformIos`, `--platformWeb`).
Output is written to `android/`, `ios/`, and `web/` subdirectories within the output directory.

Artwork supports two crop modes, configured per run:

- `--artworkCreateUncropped` (default `true`): exports the full frame using the parent
- `--artworkCreateCropped` (default `false`): exports the image fill only, appending a `_cropped` suffix

#### Web

- **Artwork**: PNG
- **Icons**: SVG

#### Android

- **Artwork**: PNG scaled into density buckets (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) from a `xxxhdpi` source
- **Icons**: SVG converted to Android Vector Drawable (AVD), with colors replaced by a magenta placeholder for tinting

#### iOS

- **Artwork**: PNG stored in an [Asset Catalog](https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/index.html) with `@1x` - `@3x` scales from a `@3x` source.
  Optionally, HEIC format can be used instead of PNG via `--artworkIosOutputHeic` (see [HEIC output](#heic-output-ios) for setup requirements).
- **Icons**: SVG stored in an [Asset Catalog](https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/index.html) at `@1x` scale

##### Grouping within asset catalogs

Assets can be grouped into named folders within the Asset Catalog with the same [Custom naming tokens](#custom-naming) using `--artworkIosGroupByTokenFormat` and `--iconsIosGroupByTokenFormat`.
Not supplying a value or using a blank string will disable the option.

Recommended formats are:
 - `{canvas.name}`

When enabled, each group a namespace folder in the asset catalog:

```
Assets.xcassets/
  MyGroup/
    Images/
      hero_image.imageset/
  AnotherGroup/
    Images/
      banner.imageset/
```

> [!TIP]
> When enabled, consider setting `--artworkIosFormat` or `--iconsIosFormat` to avoid redundant info between the namespace and asset. E.g. `--artworkIosFormat` to `{node.name}` and `--iconsIosFormat` to `{node.name.split "/" first}`.

### HEIC output (iOS)

HEIC can be enabled for iOS artwork via `--artworkIosOutputHeic`. This requires [`ImageMagick`](https://imagemagick.org/).

**macOS**
```shell
brew install imagemagick
```

**Linux**
```shell
apt-get install -y imagemagick
```

**Windows:** Not supported as ImageMagick does not support writing HEIC on Windows due to licensing restrictions.
Consider running this on macOS or Linux (including WSL2).

## Variables

Figstract can extract [local variables](https://www.figma.com/developers/api#get-local-variables-endpoint) from Figma files.
All variable types (booleans, numbers, strings and colors) are supported.

All variable types will be outputted by default, but can be configured to be omitted completely.

### Filtering

Variables can be filtered by collection name, mode name, and type.
Include and exclude filters are mutually exclusive for a given dimension and can be repeated to supply multiple patterns.

- Collection filters: `--filterIncludedVariableCollection` / `--filterExcludedVariableCollection`
- Mode filters: `--filterIncludedMode` / `--filterExcludedMode`
- Type filters: `--includeTypeBoolean`, `--includeTypeNumber`, `--includeTypeString`, `--includeTypeColor` (all default `true`)

### Theme variant mapping

Figstract supports resolving variables to themes.

#### Light / Dark

When a Figma variable collection uses modes to represent light and dark themes, Figstract can map these to a `LightAndDark` resolved mapping with separate light and dark values.
This is particularly useful for Android Compose, where `light` and `dark` companion object properties are generated on the output type, ready to be used with `isSystemInDarkTheme()`.

When no light/dark mapping is detected, all modes are output individually as separate nested objects.

#### Material theming

> [!NOTE]
> Coming soon

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

When light/dark theme variant mapping is active (see [Theme variant mapping](#theme-variant-mapping)), the output uses a `data class` with `light` and `dark` companion properties instead of nested objects:

```kotlin
public object MyVariableCollection {
   public data class Colors(
      val primaryColor: Color,
      val backgroundColor: Color,
   ) {
      public companion object {
         public val light: Colors = Colors(
            primaryColor = Color(0xFF0057FF),
            backgroundColor = Color(0xFFFFFFFF),
         )
         public val dark: Colors = Colors(
            primaryColor = Color(0xFF82AAFF),
            backgroundColor = Color(0xFF121212),
         )
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

Figstract is structured as a multi-module Gradle project.
The modules are layered so that platform-specific modules depend on core, and the CLI depends on all of them.

| Module            | Artifact                                    | Description                                                                                              |
|-------------------|---------------------------------------------|----------------------------------------------------------------------------------------------------------|
| `library-core`    | `com.anifichadia.figstract:library-core`    | Core pipeline abstractions, Figma REST API client, JSON variable writer, JsonPath-based asset extraction |
| `library-android` | `com.anifichadia.figstract:library-android` | Android-specific importers: WEBP, AVD conversion, density scaling, etc.                                  |
| `library-ios`     | `com.anifichadia.figstract:library-ios`     | iOS-specific importers: iOS asset scaling, asset catalog management                                      |
| `cli-core`        | `com.anifichadia.figstract:cli-core`        | Reusable CLI building blocks (option groups, base commands) for composing custom CLIs on top of Figstract |
| `cli`             | `com.anifichadia.figstract:cli`             | Out-of-the-box CLI built on Clikt, bundles all modules into a fat JAR via Shadow                         |

If you want to build your own tooling on top of Figstract rather than using the CLI out of the box, depend on only the library modules you need and use the `cli-core` and `cli` modules as a template.

### Adding as a dependency

```kotlin
// Core only (custom tooling, web/JSON output)
implementation("com.anifichadia.figstract:library-core:<version>")

// Android-specific output
implementation("com.anifichadia.figstract:library-android:<version>")

// iOS-specific output
implementation("com.anifichadia.figstract:library-ios:<version>")

// CLI core
implementation("com.anifichadia.figstract:cli-core:<version>")
```

Snapshots are available from Maven Central's snapshot repository:

```kotlin
// settings.gradle.kts
repositories {
    maven {
        name = "Central Portal Snapshots"
        url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        mavenContent { snapshotsOnly() }
    }
    mavenCentral()
}
```

## In development

- Shell wrapper
- GitHub action support
- Better error handling
- Android XML variable output
- iOS variable output
