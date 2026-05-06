# MATSim Slack Integration

Real-time Slack notifications and interactive control for MATSim simulations.

## Features

- **Real-time monitoring** — simulation progress, iteration timing, memory usage, DRT performance
- **Interactive control** — stop simulations by typing `stop` in the Slack thread
- **Extensible notifier framework** — add custom notifiers with ~30 lines of code
- **Typed configuration** — each notifier has its own parameter set with validation and defaults

## Quick Start

### 1. Create a Slack App

1. Go to https://api.slack.com/apps and create a new app
2. Enable **Socket Mode** (Settings → Socket Mode)
3. Create an App-Level Token with `connections:write` scope → this is your `SLACK_APP_TOKEN` (starts with `xapp-`)
4. Under **OAuth & Permissions**, add Bot Token Scopes:
   - `chat:write`
   - `chat:write.customize` (for custom emoji/username)
5. Under **Event Subscriptions**, subscribe to bot events:
   - `message.channels`
6. Install the app to your workspace → copy the Bot Token (starts with `xoxb-`)
7. Invite the bot to your channel: `/invite @YourBotName`

### 2. Set Environment Variables

```bash
export SLACK_BOT_TOKEN="xoxb-..."
export SLACK_APP_TOKEN="xapp-..."
export SLACK_CHANNEL="my-channel"   # Channel name or ID (e.g. "monitoring" or "C0123456789")
```

### 3. Add Dependency

```xml
<dependency>
    <groupId>com.github.moia-oss</groupId>
    <artifactId>matsim-slack</artifactId>
    <version>main-SNAPSHOT</version>
</dependency>
```

Add the JitPack and MATSim repositories:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
    <repository>
        <id>matsim</id>
        <url>https://repo.matsim.org/repository/matsim</url>
    </repository>
</repositories>
```

### 4. Add to Your Simulation

```java
SlackConfigGroup slackConfigGroup = new SlackConfigGroup();
slackConfigGroup.setCredentialSource(SlackConfigGroup.CredentialSource.fromEnvironment);

// Add notifiers
slackConfigGroup.addNotifierConfig(new StuckAgentNotifierParams());
slackConfigGroup.addNotifierConfig(new MemoryObserverNotifierParams());

IterationProgressNotifierParams progress = new IterationProgressNotifierParams();
progress.setMessageInterval(5);
slackConfigGroup.addNotifierConfig(progress);

// Install module
controler.addOverridingModule(new SlackModule(slackConfigGroup));
controler.run();
```

## Built-in Notifiers

| Type | Class | Description |
|------|-------|-------------|
| `stuckAgent` | `StuckAgentNotifierParams` | Reports stuck agents per iteration |
| `linkTraffic` | `LinkTrafficNotifierParams` | Alerts when vehicles exceed a threshold on a link |
| `iterationTime` | `IterationTimeNotifierParams` | Timing reports and slow-iteration warnings |
| `iterationProgress` | `IterationProgressNotifierParams` | Simple "Iteration N started" messages |
| `drtPerformance` | `DrtPerformanceNotifierParams` | DRT wait times, ride times, rejections |
| `memoryObserver` | `MemoryObserverNotifierParams` | JVM heap usage with threshold warnings |

### Configuration Examples

```java
// Link traffic: alert every 50 vehicles on any link
LinkTrafficNotifierParams link = new LinkTrafficNotifierParams();
link.setThreshold(50);
link.setLinkId("link_123"); // optional: monitor specific link

// Iteration timing: report every 10 iterations, warn if > 1 hour
IterationTimeNotifierParams time = new IterationTimeNotifierParams();
time.setReportInterval(10);
time.setWarnThresholdSeconds(3600);

// Memory: report every 5 iterations, warn at 90% heap usage
MemoryObserverNotifierParams memory = new MemoryObserverNotifierParams();
memory.setReportInterval(5);
memory.setWarnThresholdPercent(90);
```

## Creating Custom Notifiers

Adding a custom notifier requires four things: a params class, a notifier class, type registration, and configuration.

### 1. Define Parameters

Create a typed parameter set by extending `NotifierConfig`. Each `@Parameter` field becomes configurable via Java API or XML:

```java
public class ArrivalCountNotifierParams extends NotifierConfig {
    public static final String SET_NAME = "arrivalCount";

    @Parameter
    @Comment("Notify every N arrivals")
    private int threshold = 1000;

    public ArrivalCountNotifierParams() {
        super(SET_NAME, true, SET_NAME);
    }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }
}
```

### 2. Implement the Notifier

Choose the base class based on event frequency:

- **`EventHandlerNotifier`** — for high-frequency mobsim events (millions per iteration)
- **`ControlerEventNotifier`** — for low-frequency controller events (once per iteration)

**Example: mobsim event notifier**

```java
public class ArrivalCountNotifier extends EventHandlerNotifier
        implements PersonArrivalEventHandler {

    private int count = 0;
    private int threshold = 1000;

    public ArrivalCountNotifier(MatsimSlackClient slackClient) {
        super(slackClient);
    }

    @Override
    public String getType() { return "arrivalCount"; }

    @Override
    public void configure(NotifierConfig config) {
        super.configure(config);
        ArrivalCountNotifierParams params = (ArrivalCountNotifierParams) config;
        this.threshold = params.getThreshold();
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (++count % threshold == 0) {
            queueMessage(count + " agents arrived");
        }
    }

    @Override
    public void reset(int iteration) {
        super.reset(iteration);
        count = 0;
    }
}
```

**Example: controller event notifier**

```java
public class ScoreNotifier extends ControlerEventNotifier
        implements IterationEndsListener {

    public ScoreNotifier(MatsimSlackClient slackClient) {
        super(slackClient);
    }

    @Override
    public String getType() { return "score"; }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        double avgScore = /* compute from population */;
        queueMessage(SlackMessage.builder()
                .text("Avg score: " + avgScore)
                .emoji(":chart_with_upwards_trend:")
                .priority(SlackMessage.Priority.NORMAL)
                .build());
    }
}
```

### 3. Register the Type

Register both the **params factory** (for XML config parsing) and the **notifier factory** (for instantiation):

```java
// Register params type so XML config can create instances
slackConfigGroup.registerNotifierType("arrivalCount", ArrivalCountNotifierParams::new);

// Register notifier factory so the module knows how to instantiate it
SlackModule module = new SlackModule(slackConfigGroup);
module.registerNotifierFactory("arrivalCount", ArrivalCountNotifier::new);
controler.addOverridingModule(module);
```

### 4. Configure

**Java API:**

```java
ArrivalCountNotifierParams params = new ArrivalCountNotifierParams();
params.setThreshold(500);
slackConfigGroup.addNotifierConfig(params);
```

**XML config** (works automatically after type registration):

```xml
<module name="slack">
    <parameterset type="arrivalCount">
        <param name="threshold" value="500"/>
        <param name="emoji" value=":runner:"/>
    </parameterset>
</module>
```

Both approaches are equivalent — XML parameters map directly to `@Parameter` fields in your params class.

## Interactive Commands

Type these in the simulation's Slack thread:

| Command | Effect |
|---------|--------|
| `stop` | Graceful shutdown after current iteration |
| `crash` | Force crash (for testing) |

**Note:** Commands are not authenticated — anyone with access to the Slack channel can control the simulation. Use a private channel or restrict membership if this is a concern.

## Architecture

```
SlackModule (Guice)
  ├── MatsimSlackClient (Socket Mode, message sending, lifecycle)
  ├── NotifierManager (throttling, batching, priority)
  └── Notifiers
       ├── ControlerEventNotifier → iteration lifecycle
       └── EventHandlerNotifier → mobsim events
```

Messages are batched and throttled (10s minimum between posts). Critical-priority messages bypass throttling. At each iteration boundary, all pending messages are flushed.

## XML Configuration

All built-in notifiers can be configured via MATSim XML config. Custom notifiers participate in XML config automatically once registered via `registerNotifierType()` (see [Creating Custom Notifiers](#creating-custom-notifiers)).

```xml
<module name="slack">
    <param name="credentialSource" value="fromEnvironment"/>
    <param name="channelName" value="C0123456789"/>
    <param name="notificationCheckIntervalSeconds" value="3600"/>

    <parameterset type="stuckAgent">
        <param name="emoji" value=":warning:"/>
    </parameterset>

    <parameterset type="linkTraffic">
        <param name="threshold" value="50"/>
        <param name="linkId" value="link_123"/>
    </parameterset>

    <parameterset type="iterationTime">
        <param name="reportInterval" value="5"/>
        <param name="warnThresholdSeconds" value="3600"/>
    </parameterset>

    <parameterset type="memoryObserver">
        <param name="reportInterval" value="5"/>
        <param name="warnThresholdPercent" value="80"/>
    </parameterset>
</module>
```

Each `<parameterset type="...">` maps to a registered `NotifierConfig` subclass. The `type` attribute determines which params class is instantiated, and `<param>` elements set the `@Parameter` fields.

## Building

```bash
mvn clean compile    # compile
mvn test             # run tests
```

## Requirements

- Java 25+
- MATSim 2027.0+
- Slack workspace with Socket Mode enabled

## License

GNU General Public License v2.0
