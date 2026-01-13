# HytaleMiniFormat

A standalone Hytale-native rich text formatting library. Extracted from `TaleLib`, it allows you to parse MiniMessage-like tags (Modern formatting) into Hytale's native `Message` component without any external dependencies like Adventure.

## Features

- **RGB Gradients**: `<gradient:#start:#end>Text</gradient>`
- **Hex Colors**: `<#RRGGBB>Text`
- **Standard Colors**: `<red>`, `<gold>`, `<light_purple>`, etc.
- **Styles**: `<bold>`, `<italic>`, `<underlined>`, `<monospace>`
- **Nesting**: Fully supports nested tags.
- **Native**: Returns `com.hypixel.hytale.server.core.Message` objects directly.

## Installation

This library is available via [JitPack](https://jitpack.io).

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.ssquadteam:HytaleMiniFormat:main-SNAPSHOT")
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.ssquadteam</groupId>
    <artifactId>HytaleMiniFormat</artifactId>
    <version>main-SNAPSHOT</version>
</dependency>
```

## Usage

```kotlin
import com.github.ssquadteam.hytaleminiformat.MiniFormat
import com.hypixel.hytale.server.core.universe.PlayerRef

fun sendWelcome(player: PlayerRef) {
    // Simple parsing
    val message = MiniFormat.parse("<green>Hello <bold>Hytale!</bold></green>")
    player.sendMessage(message)

    // Gradients
    val gradientMsg = MiniFormat.parse("<gradient:#ff0000:#0000ff>Rainbow Text</gradient>")
    player.sendMessage(gradientMsg)
}
```
