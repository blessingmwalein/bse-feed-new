# BSE Trading Feed

Botswana Stock Exchange Market Data Gateway.  
Connects to BSE Millennium Exchange FAST/FIX feed over UDP multicast, decodes FAST 1.1 messages, maintains an order book, and provides a web dashboard.

## Project Structure

| Module | Description |
|---|---|
| `bse-trading-feed-core` | Domain models, order book logic, shared utilities |
| `bse-trading-feed-gateway` | FAST/FIX protocol decoding, UDP multicast listener |
| `bse-trading-feed-web` | Web dashboard (Thymeleaf + REST endpoints) |
| `bse-trading-feed-app` | Spring Boot application — assembles all modules |

## Prerequisites

- **Java 17**
- **Maven 3.8+**

## Quick Start (Compile & Run)

### 1. Clone the project

```bash
git clone <repo-url> bse-trading-feed
cd bse-trading-feed
```

### 2. Verify Java and Maven

```bash
java -version
mvn -version
```

### 3. Dev Mode (compile & run without generating a JAR)

Run the application directly from source — no JAR file is created. Best for development:

```bash
mvn -pl bse-trading-feed-app -am spring-boot:run
```

> The `-am` (also-make) flag tells Maven to build all dependency modules (core, gateway, web) automatically.

The web dashboard will be available at **http://localhost:8080**.  
Press `Ctrl+C` to stop.

### 4. Production Build (generate JAR & run)

Compile all modules and produce a runnable JAR:

```bash
mvn clean package -DskipTests
```

This creates: `bse-trading-feed-app/target/bse-trading-feed-app-1.0.0-SNAPSHOT.jar`

Run it:

```bash
java -jar bse-trading-feed-app/target/bse-trading-feed-app-1.0.0-SNAPSHOT.jar
```

The web dashboard will be available at **http://localhost:8080**.

### Run in the background (server)

To keep the application running after you close the terminal:

```bash
nohup java -jar bse-trading-feed-app/target/bse-trading-feed-app-1.0.0-SNAPSHOT.jar > app.log 2>&1 &
```

Check logs:

```bash
tail -f app.log
```

Stop the application:

```bash
kill $(pgrep -f bse-trading-feed-app)
```

### Full command summary

```bash
cd bse-trading-feed
mvn clean package -DskipTests
java -jar bse-trading-feed-app/target/bse-trading-feed-app-1.0.0-SNAPSHOT.jar
```

## Configuration

Key settings are in [`bse-trading-feed-app/src/main/resources/application.yml`](bse-trading-feed-app/src/main/resources/application.yml):

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port for the web dashboard |
| `bse.feed.mode` | `offline` | `live` for UDP multicast, `offline` for file replay |
| `bse.feed.templates-path` | `classpath:templates/FastGWMsgConfig.xml` | FAST template file |
| `bse.feed.heartbeat-interval` | `2` | Heartbeat interval in seconds |

## Feed Modes

- **`offline`** — Replays captured market data from file (useful for development/testing).
- **`live`** — Connects to BSE UDP multicast feed for real-time data.

Set the mode in `application.yml`:

```yaml
bse:
  feed:
    mode: offline   # or "live"
```
