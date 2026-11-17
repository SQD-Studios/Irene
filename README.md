# Irene

Irene is a chat plugin for Paper servers, with formatting, moderation and more!

## Features

- **Chat Formatting**: Full [MiniMessage](https://docs.papermc.io/adventure/minimessage/format/) support with
  group-based formats and formats
- **Moderation**:
    - Customizable word filter and censoring
    - Spam prevention and message similarity
    - Configurable message length limits
    - Chat clearing and social spy for administrators
- **Player Enhancements**:
    - **Pings & Mentions**
    - **Private Messaging**
    - **Ignore System**
    - **Chat Heads**
    - **Emojis & Items**
- **Geyser Fixes**: Makes bedrock clients able to chat without disabling secure profiles
- **Integrations**: Integrations with LuckPerms, DiscordSRV, and EssentialsDiscord.
- **Multi-Server Chatting**
- **Automated Broadcasts**
- **Join/Leave Customization**

## Building

Clone the repository:

```shell
git clone https://github.com/SQD-Studios/Irene
```

Build it with:

```shell
./gradlew build
```

The finished output is in `./build/libs/Irene-*.jar` (Except if it doesn't build xD)

## Installing

1. Download the jar from the source of your choice.
2. Add it to your server's `plugins` folder.
3. Ensure you have LuckPerms
4. Restart your server.