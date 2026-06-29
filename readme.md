# Figstract

[![Maven Central Version](https://img.shields.io/maven-central/v/com.anifichadia.figstract/cli)](https://central.sonatype.com/namespace/com.anifichadia.figstract)
[![Release](https://github.com/AniFichadia/Figstract/actions/workflows/release-deploy.yml/badge.svg)](https://github.com/AniFichadia/Figstract/actions/workflows/release-deploy.yml)
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
- **Renaming**: Remap names for assets and variables before output
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

The CLI has three subcommands:

- `assets`: extract artwork and icons using per-type CLI flags
- `asset-batch`: extract one or more asset batches defined in a JSON config file
- `variables`: extract design system variables

The CLI can be configured with CLI args, or by supplying a `[subcommandName].properties` or a `[subcommandName].json` file with the same keys as the arguments in the working directory (e.g. `assets.properties` or `assets.json`)

### Custom CA Certificates (Corporate networks)

Some corporates may use of custom CA certificates for internal monitoring, causing the JVM to fail certificate verification and prevent access to the Figma API or where assets are stored.
You can create a custom truststore that includes your organization's CA certificates:

1. Copy default JVM cacerts:
    ```shell
    cp $JAVA_HOME/lib/security/cacerts ~/.figstract-cacerts
    ```
2. Export your organization's CA certs as a `.pem` or `.cer` file.
   This really depends on how CA certs are managed within your organization.
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

### Output directory

All subcommands write to `./out/<subcommand>/` by default.
Override using `--out <path>` / `-o <path>`.

### Advanced API options

The following global options tune Figma API behavior:

- `--figmaApiConcurrencyLimit`: maximum number of concurrent Figma API requests (default: `5`)
- `--figmaApiRetryLimit`: maximum number of retries on transient failures (default: `15`)

## Authentication

Figstract supports the following authentication mechanisms to accessing Figma:

- Personal Access Tokens (PATs).
  Refer to the [Figma documentation](https://help.figma.com/hc/en-us/articles/8085703771159-Manage-personal-access-tokens) and [API reference](https://www.figma.com/developers/api#authentication) for more info.

The following global options configure authentication:

- `--auth`: the credential value (required)
- `--authType`: `AccessToken` (default)

### Scopes

When generating credentials, ensure that the following [scopes](https://www.figma.com/developers/api#authentication-scopes) are configured.

| Operation              | Required scope              |
|------------------------|-----------------------------|
| assets / asset batches | `File content`              |
| variables              | `File content`, `Variables` |

> [!CAUTION]
> Ensure scopes are set to `Read only` and tokens are refreshed regularly

Either one auth credential can be generated with all the scopes above, or specific auth credentials can be created for each subcommand.

### Logging

Figstract uses [kotlin-logging](https://github.com/oshai/kotlin-logging) and [Logback](https://logback.qos.ch/) for logging, and logs errors to the console by default.
When using the CLI, the log level can be configured using the `--logLevel` option (e.g. `--logLevel DEBUG`), or by configuring logback using environment variables (refer to https://logback.qos.ch/manual/configuration.html#configFileProperty).


## Assets (`assets` subcommand)

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

Output file names can be customized using a format string.
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
Override the format using `--artworkAndroidNamingFormat`, `--artworkIosNamingFormat`, `--artworkWebNamingFormat` (and `icons` equivalents).

### Processing records

Processing records prevent re-processing Figma files that haven't changed since the last run, based on the file's last-modified timestamp.
The record is stored as `processing_record.json` in the output directory.

- `--processingRecordEnabled` (default `true`): enable or disable processing records
- `--processingRecordName`: a unique name suffix for the record file, useful when running multiple configurations against the same Figma file (e.g. `processing_record_icons.json`)

### Import reports

After each run, Figstract writes a JSON import report to the output directory with the name `import_report_<figmaFileKey>_<timestamp>.json`.
The report captures which assets or variables were processed, skipped, or failed, and is useful for debugging and auditing runs.

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
  Optionally, HEIC format can be used instead of PNG (see [HEIC output](#heic-output-ios)).
- **Icons**: SVG stored in an [Asset Catalog](https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/index.html) at `@1x` scale

##### Grouping within asset catalogs

Assets can be grouped into named folders within the Asset Catalog with the same [Custom naming tokens](#custom-naming) using `--artworkIosGroupByTokenNamingFormat` and `--iconsIosGroupByTokenNamingFormat`.
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
> When enabled, consider setting `--artworkIosNamingFormat` or `--iconsIosNamingFormat` to avoid redundant info between the namespace and asset. E.g. `--artworkIosNamingFormat` to `{node.name}` and `--iconsIosNamingFormat` to `{node.name.split "/" first}`.

### HEIC output (iOS)

HEIC can be enabled for iOS artwork via setting `--artworkIosOutputFormat` to `Heic`. This requires [`ImageMagick`](https://imagemagick.org/).

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

### Pipeline DSL

You can supply additional pre-processing steps to inject before Figstract's built-in platform steps using a text-based pipeline DSL. 
This lets you apply transformations (scaling, format conversion, renaming, path manipulation) without writing Kotlin and creating custom implementations.

#### DSL format

Each non-blank, non-comment line is a pipeline expression. 
Lines starting with `#` (or with an inline `#` outside a quoted value) are treated as comments. 
Multiple top-level lines are sequenced with `->`.

Each step is written as a function call:

```
stepName()
stepName(param=value)
stepName(param1=value1, param2=value2)
```

##### Sequential composition (`->` / `then`)

Outputs from the left step are fed into the right step in order.

```
# Scale then convert then rename
scale(scale=2.0) -> convertToWebPLossy(qualityPercent=80) -> renameSuffix(suffix=_web)

# Or spread across lines (equivalent to joining with ->)
scale(scale=2.0)
convertToWebPLossy(qualityPercent=80)
renameSuffix(suffix=_web)
```

##### Parallel fan-out (`and`)

Runs all comma-separated branches against the same input and collects all outputs.
Each branch is a full pipeline chain and can itself use [sequential steps](#sequential-composition----then).

```
# Produce both WebP and PNG from the same input
and(convertToWebPLossy(qualityPercent=75), convertToPngLossless())

# Branches can be chains
and(
  convertToWebPLossy(qualityPercent=75) -> renameSuffix(suffix=_web),
  convertToPngLossless() -> renameSuffix(suffix=_fallback)
)
```

##### First-non-empty fallback (`or`)

Tries each branch in order and returns the output of the first branch that produces a non-empty result.

```
or(convertToWebPLossy(qualityPercent=75), convertToPngLossless())
```

##### Nesting

Combinators can be arbitrarily nested and combined with `->`:

```
# Scale first, then fan out into two formats
scale(scale=2.0) -> and(
  convertToWebPLossy(qualityPercent=75) -> renameSuffix(suffix=_web),
  or(convertToWebPLossy(qualityPercent=50), convertToPngLossless()) -> renameSuffix(suffix=_small)
)
```

#### Built-in steps

| Step                                 | Parameters                                                       | Description                                      |
|--------------------------------------|------------------------------------------------------------------|--------------------------------------------------|
| `passThrough()`                      | -                                                                | No-op; useful as a placeholder                   |
| `scale(scale)`                       | `scale`: Float                                                   | Scales the image by a factor                     |
| `scaleToSize(width, height)`         | `width`: Int, `height`: Int                                      | Scales to exact pixel dimensions                 |
| `scaleToWidth(width)`                | `width`: Int                                                     | Scales to target width, preserving aspect ratio  |
| `scaleToHeight(height)`              | `height`: Int                                                    | Scales to target height, preserving aspect ratio |
| `rename(name)`                       | `name`: String                                                   | Replaces the output file name                    |
| `renameSuffix(suffix)`               | `suffix`: String                                                 | Appends a suffix to the output file name         |
| `renamePrefix(prefix)`               | `prefix`: String                                                 | Prepends a prefix to the output file name        |
| `pathElementsAppend(pathElements)`   | `pathElements`: comma-separated String, or `pathElement`: String | Appends path segments to the output path         |
| `convertToPngLossless()`             | -                                                                | Converts to lossless PNG                         |
| `convertToPngLossy(qualityPercent)`  | `qualityPercent`: Int (default `75`)                             | Converts to lossy PNG                            |
| `convertToWebPLossless()`            | -                                                                | Converts to lossless WebP                        |
| `convertToWebPLossy(qualityPercent)` | `qualityPercent`: Int (default `75`)                             | Converts to lossy WebP                           |

**Destination steps** (require `destinationStepRegistry(baseDirectory)` to be composed in):

| Step                         | Parameters     | Description                                                                     |
|------------------------------|----------------|---------------------------------------------------------------------------------|
| `destinationNone()`          | -              | Black hole; discards output                                                     |
| `destinationDirectory(path)` | `path`: String | Writes output to a directory resolved relative to the configured base directory |

**Android-specific steps** (available when using `library-android`):

| Step                                | Parameters | Description                                                            |
|-------------------------------------|------------|------------------------------------------------------------------------|
| `androidSvgToAvd()`                 | -          | Converts SVG to Android Vector Drawable XML                            |
| `androidVectorColorToPlaceholder()` | -          | Replaces vector drawable colors with the Figstract magenta placeholder |

**iOS-specific steps** (available when using `library-ios`):

| Step                            | Parameters                           | Description                                                |
|---------------------------------|--------------------------------------|------------------------------------------------------------|
| `convertToHeic(qualityPercent)` | `qualityPercent`: Int (default `85`) | Converts PNG to HEIC (see [HEIC output](#heic-output-ios)) |

**iOS asset catalog steps** (require `iosAssetCatalogStepRegistry(baseDirectory)` to be composed in):

| Step                             | Parameters                                                                                                                                                                | Description                                                                      |
|----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| `iosStoreInAssetCatalog`         | `path`, `scale`, `assetType`? (`imageset`/`iconset`, default `imageset`), `catalogName`? (default `Assets`), `idiom`? (default `universal`)                               | Stores a single image into an asset catalog at the given scale                   |
| `iosScaleAndStoreInAssetCatalog` | `path`, `sourceScale`, `assetType`?, `catalogName`?, `scales`? (comma-separated, default all), `idiom`?, `outputFormat`? (`Default`/`Heic`/`PngLossy`, default `Default`) | Scales an image to multiple scales and stores all variants into an asset catalog |

`scale` / `sourceScale` / `scales` values: See [Scale](library-ios/src/main/java/com/anifichadia/figstract/ios/assetcatalog/Scale.kt)

#### Library usage

Each module ships its own platform specific registries.
These can be composed with the `+` / `plus` operator, with the right-hand side winning on any name collision.

```kotlin
// Core steps only
val step = ImportPipelineDsl(
    registry = CoreImportPipelineStepRegistry,
).parse(
    """
    scale(scale=0.5)
    convertToWebPLossy(qualityPercent=80)
    """.trimIndent()
)

// With destination steps - base directory is required for path resolution
val registry = CoreImportPipelineStepRegistry + destinationStepRegistry(baseDirectory = outputDir)
val step = ImportPipelineDsl(
    registry).parse(
    """
    and(
      convertToWebPLossy(qualityPercent=75) -> destinationDirectory(path=web),
      convertToPngLossless()                -> destinationDirectory(path=fallback)
    )
    """.trimIndent(),
)

// The CLI pre-builds a combined registry for all platforms and destinations:
val registry = combinedStepRegistries(outDir)

// With custom steps — use the builder DSL and compose in
val custom = ImportPipelineStepRegistry.buildImportPipelineStepRegistry {
    "myStep" withFactory { params ->
        val quality = params.valueOrDefault<Int>("quality") { 80 }
        convertToWebPLossy(quality)
    }
}
val registry = CoreImportPipelineStepRegistry + custom
// etc
```


## Asset Batch processing (`asset-batch` subcommand)

The `asset-batch` subcommand processes one or more asset batches defined in a single JSON config file, removing the need to invoke the CLI separately for each Figma file or asset type.
Refer to [Assets](#assets-assets-subcommand) for additional details.
The config file supports `//` and `/* */` comments.
Its top-level structure is:

```json
{
  "batches": [
    {
      "type": "Artwork",
      // etc 
    },
    {
      "type": "Icon",
      // etc
    },
    {
      "type": "Custom",
      // etc
    }
  ]
}
```

Each batch entry is one of three types selected by the `type` discriminator field.
Batches with `"enabled": false` are skipped.

### Common fields

All batch types share these fields:

| Field              | Type    | Default                 | Description                                                                               |
|--------------------|---------|-------------------------|-------------------------------------------------------------------------------------------|
| `fileDefinition`   | object  | required                | Figma file to extract from: `{ "fileKey": "...", "branchName": "...", "version": "..." }` |
| `enabled`          | boolean | required                | Set `false` to skip this batch                                                            |
| `outDirectory`     | string  | global `--outDirectory` | Per-batch output directory override                                                       |
| `jsonPath`         | string  | required for Custom     | JsonPath expression to locate nodes. For Artwork and Icons, this is optional              |
| `instructionLimit` | int     | -                       | Max assets to process                                                                     |
| `assetFilter`      | object  | no filter               | Include/exclude by canvas, node, or parent name using regex patterns                      |
| `renamingMap`      | object  | empty                   | Rename canvases or nodes before name generation. Refer to [Renaming](#renaming)           |

`assetFilter`:
```json
{
  "canvasNameFilter": {
    "include": [
      "regex"
    ],
    "exclude": [
      "regex"
    ]
  },
  "nodeNameFilter": {
    "include": [
      "regex"
    ],
    "exclude": [
      "regex"
    ]
  },
  "parentNameFilter": {
    "include": [
      "regex"
    ],
    "exclude": [
      "regex"
    ]
  }
}
```

### Type: `"Artwork"`

Shares the same output behavior as the `assets` subcommand's artwork handler.

| Field                         | Type    | Default           | Description                                               |
|-------------------------------|---------|-------------------|-----------------------------------------------------------|
| `namingFormats`               | object  | see below         | Token format strings for generated file names             |
| `platformOptions`             | object  | all enabled       | Enable/disable Android, iOS, web output                   |
| `iosGroupByTokenNamingFormat` | string  | -                 | Token format for iOS asset catalog grouping               |
| `createUncropped`             | boolean | `true`            | Export full uncropped image                               |
| `createCropped`               | boolean | `false`           | Export cropped image fill only                            |
| `androidOutputDensityBuckets` | array   | all except `LDPI` | Subset of `LDPI` `MDPI` `HDPI` `XHDPI` `XXHDPI` `XXXHDPI` |
| `iosOutputScales`             | array   | all               | Subset of `1x` `2x` `3x`                                  |
| `iosOutputFormat`             | string  | `Default`         | One of `Default` `Heic` `PngLossy`                        |

`namingFormats` defaults:
```json
{
  "androidFormat": "{canvas.name}_{node.name}",
  "iosFormat": "{canvas.name}{node.name}",
  "webFormat": "{canvas.name}_{node.name}"
}
```

`platformOptions`:
```json
{
  "androidEnabled": true,
  "iosEnabled": true,
  "webEnabled": true
}
```

Example:
```json
{
  "type": "Artwork",
  "fileDefinition": {
    "fileKey": "abc123"
  },
  "enabled": true,
  "createCropped": true,
  "iosOutputFormat": "Heic",
  "platformOptions": {
    "webEnabled": false
  }
}
```

### Type: `"Icon"`

Shares the same output behavior as the `assets` subcommand's icon handler.

| Field                         | Type   | Default     | Description                                   |
|-------------------------------|--------|-------------|-----------------------------------------------|
| `namingFormats`               | object | see below   | Token format strings for generated file names |
| `platformOptions`             | object | all enabled | Enable/disable Android, iOS, web output       |
| `iosGroupByTokenNamingFormat` | string | -           | Token format for iOS asset catalog grouping   |

`namingFormats` defaults:
```json
{
  "androidFormat": "ic_{node.name}",
  "iosFormat": "{node.name}",
  "webFormat": "{node.name}",
  "webCasing": "SnakeCase" // optional, values are None, UpperCamelCase, LowerCamelCase, SnakeCase, ScreamingSnakeCase
}
```

Example
```json
{
  "type": "Icon",
  "fileDefinition": {
    "fileKey": "abc123"
  },
  "enabled": true,
  "platformOptions": {
    "iosEnabled": false
  }
}
```

### Type: `"Custom"`

Fully custom pipeline.
Gives direct control over how assets are exported from Figma and processed.
The `pipelineDefinition` field accepts the full [Pipeline DSL](#pipeline-dsl) syntax, and has access to all registered steps including destination and iOS asset catalog steps.

| Field                | Type   | Default  | Description                                                                                                              |
|----------------------|--------|----------|--------------------------------------------------------------------------------------------------------------------------|
| `pipelineDefinition` | string | required | Pipeline DSL steps                                                                                                       |
| `exportConfig`       | object | required | How to request the asset from Figma                                                                                      |
| `namingFormat`       | string | required | Token format string for generated file names                                                                             |
| `namingCasing`       | string | required | One of `LOWER_CAMEL_CASE` `UPPER_CAMEL_CASE` `LOWER_SNAKE_CASE` `UPPER_SNAKE_CASE` `LOWER_KEBAB_CASE` `UPPER_KEBAB_CASE` |

`exportConfig`
See [ExportSettings.Format](library-core/src/main/java/com/anifichadia/figstract/figma/model/ExportSetting.kt)
```json
{
  "format": "PNG", // Options: JPG, PNG, SVG, PDF
  "scale": 1.0, // Must be between 0.01 and 4
  "contentsOnly": false, // optional
  "useAbsoluteBounds": false // optional
}
```

Example
```json
{
  "type": "Custom",
  "fileDefinition": {
    "fileKey": "abc123"
  },
  "enabled": true,
  "jsonPath": "$.children[?(@.type == 'COMPONENT' && @.children[?(@.type == 'VECTOR')])]",
  "exportConfig": {
    "format": "PNG",
    "scale": 3.0
  },
  "namingFormat": "{node.name}",
  "namingCasing": "LOWER_SNAKE_CASE",
  "pipelineDefinition": "convertToWebPLossy(qualityPercent=80) -> destinationDirectory(path=web/assets)"
}
```

### Full example

```json
{
  "batches": [
    {
      "type": "Artwork",
      "fileDefinition": {
        "fileKey": "abc123"
      },
      "enabled": true,
      "createCropped": true,
      "iosOutputFormat": "Heic",
      "platformOptions": {
        "webEnabled": false
      }
    },
    {
      "type": "Icon",
      "fileDefinition": {
        "fileKey": "abc123"
      },
      "enabled": true,
      "platformOptions": {
        "iosEnabled": false
      }
    },
    {
      "type": "Custom",
      "fileDefinition": {
        "fileKey": "abc123"
      },
      "enabled": true,
      "jsonPath": "$.children[?(@.type == 'COMPONENT' && @.children[?(@.type == 'VECTOR')])]",
      "exportConfig": {
        "format": "PNG",
        "scale": 3.0
      },
      "namingFormat": "{node.name}",
      "namingCasing": "LOWER_SNAKE_CASE",
      "pipelineDefinition": "convertToWebPLossy(qualityPercent=80) -> destinationDirectory(path=web/assets)"
    }
  ]
}
```


## Variables (`variables` subcommand)

Figstract can extract [local variables](https://www.figma.com/developers/api#get-local-variables-endpoint) from Figma files.
All variable types (booleans, numbers, strings and colors) are supported.

All variable types will be outputted by default, but can be configured to be omitted completely.

### Figma file targeting

The `variables` subcommand accepts one or more Figma file keys via `--figmaFile` (repeatable).
All files are processed in parallel.
The following options apply globally across all targeted files:

- `--figmaFile`: Figma file key (repeatable)
- `--figmaFileBranchName`: file branch name
- `--figmaFileVersion`: file version

### Filtering

Variables can be filtered using the following options:
- Collection: `--filterIncludedVariableCollection` / `--filterExcludedVariableCollection`
- Mode: `--filterIncludedMode` / `--filterExcludedMode`
- Variable name: `--filterIncludedVariableName` / `--filterExcludedVariableName`
- Type: `--includeTypeBoolean`, `--includeTypeNumber`, `--includeTypeString`, `--includeTypeColor` (all default `true`)

Include and exclude filters are mutually exclusive for a given dimension and can be repeated to supply multiple regex patterns.

### Renaming

Variable collection and variable path names can be remapped before output using a JSON renaming map file supplied via `--variableRenamingMapFile <path>`.
This is useful for normalizing names that don't follow engineering naming conventions without modifying the Figma file itself.

Collection renames are applied first.
Variable path renames are then looked up using the **resolved** (post-rename) collection name, so if you rename a collection you should use its new name as the key in `variables`.

The file contains uses the following format where `collections` and `variables` are case-sensitive dictionaries of old name (case-sensitive) to new name:

```json
{
  "collections": {
    "Old Collection Name": "New Collection Name"
  },
  "variables": {
    "New Collection Name": {
      "old/variable/path": "new/variable/path",
      "colors/primary 500": "colors/primary-500"
    }
  }
}
```

Both `collections` and `variables` are optional. Entries not present in a map are left unchanged.
Non-matching entries will produce a warning in the log.

### Variable organization strategies

Variable paths from Figma are rewritten before generating output using a configurable strategy.
This controls how deeply-nested variable paths (e.g. `colour/Primary/Primary`) map to names and structure in generated files.

- `--variableOrganizationStrategy`: one of `Default`, `FullPath`, `LeafOnly`, `StripRoot`, `CustomRegex` (default: `Default`, which is equivalent to `FullPath` with nested as false)
- `--variableOrganizationStrategyNested`: split the rewritten path on `/` to produce nested group hierarchy (default: `false`).
  Applies to `FullPath`, `StripRoot`, and `CustomRegex`.
- `--variableOrganizationStrategyPattern`: Kotlin regex applied to the raw Figma variable path.
  Required when strategy is `CustomRegex`.
- `--variableOrganizationStrategyReplacement`: replacement string for `--variableOrganizationStrategyPattern`.
  Supports backreferences (`$1`, `${name}`).
  Required when strategy is `CustomRegex`.

For the input `colour/Primary/Primary`:

| Strategy      | Nested | Example input                             | Example output                |
|---------------|--------|-------------------------------------------|-------------------------------|
| `Default`     | N/A    | N / A                                     | `colourPrimaryPrimary` (flat) |
| `FullPath`    | false  | N / A                                     | `colourPrimaryPrimary` (flat) |
| `FullPath`    | true   | N / A                                     | `colour > Primary > Primary`  |
| `LeafOnly`    | N/A    | N / A                                     | `Primary`                     |
| `StripRoot`   | false  | N / A                                     | `PrimaryPrimary` (flat)       |
| `StripRoot`   | true   | N / A                                     | `Primary > Primary`           |
| `CustomRegex` | false  | pattern `^colour/(.+)$`, replacement `$1` | `PrimaryPrimary`              |

> [!NOTE]
> Strategies other than `Default` and `FullPath` are experimental and subject to change.

### Theme variant mapping

Figstract supports resolving variables to themes using `--themeVariantMappingsFile <path>`, which takes a JSON file.

#### Light / Dark

When a Figma variable collection uses modes to represent light and dark themes, Figstract can map these to a `LightAndDark` resolved mapping with separate light and dark values.
This is particularly useful for Android Compose, where `light` and `dark` companion object properties are generated on the output type, ready to be used with `isSystemInDarkTheme()`.

When no light/dark mapping is detected, all modes are output individually as separate nested objects.

The file maps variable collection names to theme variant mappings:

```json
{
  "My Variable Collection": {
    "type": "LightAndDark",
    "lightThemeModeName": "Light",
    "darkThemeModeName": "Dark"
  }
}
```

Collections not present in the file are treated as having no theme variant mapping and all modes are output individually.

#### Material theming

> [!NOTE]
> Coming soon

### Output formats

At least one output format must be enabled.
Multiple formats can be enabled simultaneously.
All output formats use the variable collection's name as the file name.
Variables are grouped by mode then by variable type within each collection's file.
Colors can be output as either Hex or RGBA values (RGBA values are floats between 0 and 1 inclusive), controlled by `--outputColorAsHex` (default: `true`).

Note: Variable collection, mode and variable names will be sanitized to conform with platform / language conventions.

#### Web / Any

For web (or any platform), Figstract generates JSON files with the following format.
Enable with `--outputJson true`.

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
      "color hex var 1": "0xFF19334C"
    }
  }
}
```

Output is written to `json/` within the output directory.
Refer to [JsonVariableDataWriter](library-core/src/main/java/com/anifichadia/figstract/importer/variable/model/writer/JsonVariableDataWriter.kt) for the implementation.

#### Android Compose

Figstract generates an R-file-like Kotlin object with constants.
Enable with `--outputAndroidCompose true --outputAndroidComposePackageName <package>`.

*MyVariableCollection.kt*

```kotlin
package your.pkg.here

import androidx.compose.ui.graphics.Color
import kotlin.Double

public object MyVariableCollection {
    public object MyMode {
        public object Booleans {
            public val boolVar1: Boolean = true
        }

        public object Numbers {
            public val numberVar1: Double = 123.45
        }

        public object Strings {
            public val stringVar1: String = "Hello"
        }

        public object Colors {
            public val colorRgbaVar1: Color = Color(
                red = 0.1f,
                green = 0.2f,
                blue = 0.3f,
                alpha = 1.0f,
            )
            public val colorHexVar1: Color = Color(0xFF19334C)
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

Output is written to `android/compose/` within the output directory.
Refer to [AndroidComposeVariableDataWriter](library-android/src/main/java/com/anifichadia/figstract/android/importer/variable/model/writer/AndroidComposeVariableDataWriter.kt) for the implementation.

#### Android XML

> [!NOTE]
> This output is experimental and subject to change.

Figstract generates Android XML resource files.
Enable with `--outputAndroidXml true`.

Light and dark theme values are written to `res/values/` and `res/values-night/` respectively.
Single-mode variables are written to `res/values/` only.

The following additional options are available:

- `--outputAndroidXmlSplitByType` (default `true`): write each resource type (`bools`, `integers`, `strings`, `colors`, etc.) to its own file, following Android conventions.
- `--outputAndroidXmlNamespaceUsingCollectionName` (default `true`): prefix XML resource names with the sanitized collection name (e.g. `my_collection_group_leaf`).
  When `false`, the collection name is omitted and the first path segment is used (e.g. `group_leaf`).
- `--outputAndroidXmlNumberOutput`: controls how number variables are written.
  One of `NONE` (skipped), `INTEGER` (truncated to `<integer>`), `DIMEN` (`<dimen>` with `dp` unit), `FLOAT` (unitless `<item type="dimen" format="float">`).
  Default: `INTEGER`.

Output is written to `android/xml/` within the output directory.

#### iOS SwiftUI

> [!NOTE]
> This output is experimental and subject to change.

Figstract generates Swift source files using SwiftPoet.
Enable with `--outputIosSwiftUi true --outputIosSwiftUiModule <module>`.

Variable groups are represented as Swift enums, with each type bucket as a nested enum.
When light/dark theme variant mapping is active, color buckets are represented as structs with `light` and `dark` static properties.

Output is written to `ios/swiftui/<module>/` within the output directory.

#### iOS Asset Catalog

> [!NOTE]
> This output is experimental and subject to change.
> Only color variables are supported; booleans and numbers are not written.

Figstract generates Xcode asset catalog color sets for color variables.
Enable with `--outputIosAssetCatalog true`.

Output is written to `ios/asset catalog/` within the output directory.

## Module structure

Figstract is structured as a multi-module Gradle project.
The modules are layered so that platform-specific modules depend on core, and the CLI depends on all of them.

| Module            | Artifact                                    | Description                                                                                               |
|-------------------|---------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| `library-core`    | `com.anifichadia.figstract:library-core`    | Core pipeline abstractions, Figma REST API client, JSON variable writer, JsonPath-based asset extraction  |
| `library-android` | `com.anifichadia.figstract:library-android` | Android-specific importers: WEBP, AVD conversion, density scaling, etc.                                   |
| `library-ios`     | `com.anifichadia.figstract:library-ios`     | iOS-specific importers: iOS asset scaling, asset catalog management                                       |
| `cli-core`        | `com.anifichadia.figstract:cli-core`        | Reusable CLI building blocks (option groups, base commands) for composing custom CLIs on top of Figstract |
| `cli`             | `com.anifichadia.figstract:cli`             | Out-of-the-box CLI built on Clikt, bundles all modules into a fat JAR via Shadow                          |

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
- Material theming support for variables
- Android XML variable output (experimental, available, subject to change)
- iOS variable output (experimental, available, subject to change)
