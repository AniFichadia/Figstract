# Figstract

Figstract bridges the process for maintaining design systems between frontend engineers and designers and helps automate mundane upkeep tasks.

This tool extracts the following design system tokens produced by designers in [Figma](https://www.figma.com/):

- Assets (raster and vector assets like images and icons)
- Design system [variables](https://help.figma.com/hc/en-us/articles/15339657135383-Guide-to-variables-in-Figma) (booleans, numbers, strings, and colors)

And imports them in native formats for frontend engineers to use on:

- Web
- Android (natively, using Android SDKs)
- iOS (natively, using Apple / iOS SDKs)

Figstract comes with an out-of-the-box CLI, which includes options for filtering and extraction, but has been designed to be modular so engineers can compose their own CLIs or other tools based on Figstract's foundation.

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
> Ensure scopes are set to `Read only` and are refreshed regularly

Either one auth credential can be generated with all the scopes above, or specific auth credentials can be created for each subcommand.

## Assets

TODO:

- supported output formats
  - Web
  - Android
    - WEBP images automatically scaled in density buckets
    - Android Vector Drawables which have been magenta-fied for tinting
  - iOS
    - [Asset catalogs](https://developer.apple.com/library/archive/documentation/Xcode/Reference/xcode_ref-Asset_Catalog_Format/index.html) with scale support
- Composable pipeline for importing and converting assets

## Variables

TODO:

- Output formats
  - Web
    - JSON
  - Android
    - Compose
      - R-file-like Kotlin object with Constants
    - XML UI
      - Coming soon
  - iOS
    - Coming soon

## Module structure

TODO

## In development

- Shell wrapper
- Github action support
- Better error handling
