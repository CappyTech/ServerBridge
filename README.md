# ServerBridge

Two-way Matrix chat bridge for Minecraft servers.

ServerBridge enables Spigot/Paper servers to relay chat messages between Minecraft and Matrix rooms in real time. It’s lightweight, server-focused, and ideal for community coordination, moderation, and cross-platform communication.

---

## Features

- Relay Minecraft player chat to a Matrix room
- Broadcast Matrix messages into Minecraft chat
- Configurable Matrix homeserver, room, and token
- Lightweight and minimal dependencies (uses Gson for JSON)
- Ideal for community coordination and moderation

---

## Usage

1. Add `ServerBridge-1.0.7.jar` to your server’s `/plugins` folder.
2. Configure `config.yml` after first launch:

```yaml
matrix:
  homeserver: "https://your.domain"
  access_token: "YOUR_ACCESS_TOKEN_HERE"
  user_id: "@serverbridge-bot:your.domain"
  room_id: "!yourroomid:your.domain"
```

3. Chat messages in Minecraft will appear in your Matrix room, and vice versa.

---

## Example

**Minecraft → Matrix**
```
[Server] Alice: Hello Matrix!
```

**Matrix → Minecraft**
```
[Matrix] @bob:matrix.org: hey everyone!
```

---

## Plugin Info

- Plugin Name: `serverbridge`
- Main Class: `com.sovereigncraft.serverbridge.ServerBridge`
- Java: 17+
- Minecraft: 1.20+ (Spigot/Paper)

---

## License

MIT

```
Copyright 2025 CappyTech

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```