# Hepsiburada UI Automation — HB-TC01

End-to-end shopping scenario for Hepsiburada.com built with **Java 17 + Selenium WebDriver 4 +
Gauge + Maven** using the Page Object Model (POM):
**login → search "bilgisayar" → pick the first product of the second row → add to cart →
verify in cart → clean the cart up**.

## Running

```bash
mvn test          # single command: compiles + runs the Gauge scenario
```

- Gauge CLI must be installed and on PATH (with the `java` and `html-report` plugins).
- Run via `mvn test`, **not** `gauge run`: Gauge's own compiler cannot see Maven
  dependencies; the gauge-maven-plugin supplies the classpath from Maven.
- Credentials: `env/secrets/credentials.properties` (gitignored; template at
  `credentials.example.properties`). Alternatively set the `HB_USERNAME` /
  `HB_PASSWORD` environment variables.
- HTML report: `reports/html-report/index.html` · Failure screenshots: `screenshots/`
- Console: every step is logged as `[HH:mm:ss.SSS] [HB-TC01] STEP n: ...`

## Project Layout

```
├── pom.xml                     # Maven: dependencies + gauge-maven-plugin (bound to test phase)
├── manifest.json               # Gauge project descriptor (language: java, plugins)
├── specs/
│   └── hepsiburada_cart.spec   # HB-TC01 scenario — human-readable Gauge steps
├── env/
│   ├── default/default.properties      # base URL, browser, timeouts, typing delays
│   └── secrets/credentials.properties  # credentials (GITIGNORED)
└── src/test/java/com/hepsiburada/
    ├── config/   # configuration access
    ├── pages/    # Page Objects (locators + actions; NO assertions)
    ├── steps/    # Gauge @Step implementations (assertions live HERE)
    └── utils/    # driver, waits, popups, bot detection, logging helpers
```

## Layered Design (why this shape?)

One-directional flow: **Spec (Gauge) → Steps → Pages → Utils**

1. `specs/*.spec` — WHAT the test does, in plain language. Gauge matches every `*` line
   to the `@Step` method with the same text.
2. `steps/` — scenario orchestration + the 5 assertions (AssertJ). State carried between
   steps (the selected product name) lives here. Because page objects contain no
   assertions, the same page class can serve different scenarios with different
   expectations.
3. `pages/` — locators and actions per page. The driver arrives via constructor
   injection (no static coupling). Locator rule: dynamic class names (`sc-xyz`, `sf-*`)
   are banned; stable attributes such as `data-test-id`, `id`, `aria-label` are used.
4. `utils/` — cross-cutting concerns: driver lifecycle, waits, popup/bot handling.

## File Guide

### config/
| File | Purpose |
|---|---|
| `ConfigReader` | Single entry point for all settings. Priority: `-D` system property > environment variable > properties file. Missing required values fail with a message that says exactly which file to fill in. |

### pages/
| File | Purpose |
|---|---|
| `BasePage` | Shared parent: `driver`, `waits`, `dismissPopups()`, `scrollIntoCenter()` (click-interception guard) and refreshing the test banner. |
| `LoginPage` | Home page opening + login flow. Adapts to the site's A/B behaviours (direct login page / account menu / swallowed click → hover-and-retry). CAPTCHA check right after submit. Handles one-step and two-step login forms. Skips login entirely when the persistent profile already holds a session. |
| `HeaderPage` | Header navigation: cart link + cart item count badge. |
| `SearchResultsPage` | Search (typing retry that verifies the value actually landed in the box) and second-row-first-product selection via on-screen **Y coordinates** (no column-count assumption); ad cards without a product link are filtered out; switches to the new tab when the product opens in one. |
| `ProductDetailPage` | Product title, Add to Cart, confirmation panel (UUID id matched with `starts-with`). Falls back to the cart badge when the panel does not show; tolerates the "recommended seller" modal by logging instead of throwing. |
| `CartPage` | Scans cart items by name (no index assumptions, other products untouched). `removeAllMatchingProducts` deletes the added product (and its duplicates) including the confirm dialog, so the next run starts with a clean cart. |

### steps/
| File | Purpose |
|---|---|
| `HepsiburadaCartSteps` | 12 `@Step` methods; the 5 assertions placed exactly where the task document requires. Every step logs via `StepLogger`. |

### utils/
| File | Purpose |
|---|---|
| `DriverFactory` | Hardened Chrome lifecycle (automation flags off, notifications blocked, maximized window, ThreadLocal). Optional persistent profile (`chrome.profile.dir`) so the site remembers the device and stops asking for SMS codes. Implicit waits deliberately absent. |
| `ExecutionHooks` | Gauge lifecycle: driver per scenario, screenshot on failure, screenshots folder cleaned at suite start. |
| `Waits` | The ONE waiting mechanism (`Thread.sleep` is banned). Only genuinely transient exceptions (stale element, execution-context) are retried. |
| `PopupHandler` | Registry-based popup dismissal + closing the `<efilli-layout-dynamic>` cookie banner from inside its **shadow DOM** via JS. |
| `CaptchaOrBlockDetector` | CAPTCHA / bot-wall **detection** (never bypass): DOM markers + text scan; on hit → screenshot + `BotDetectionException`. Also detects SMS/OTP challenge screens → `OtpRequiredException` with a human-readable fix. |
| `BotDetectionException` / `OtpRequiredException` | Readable failures carrying context, trigger marker and screenshot path. |
| `HumanTyper` | Human-like typing with randomized per-key delays (W3C Actions `pause`, not `Thread.sleep`). |
| `TestRunBanner` | "This page is being controlled by an automated test" banner at the top of every page; `pointer-events:none` (never intercepts clicks), self-healing keeper re-adds it if the page removes it. |
| `StepLogger` | One call logs to both the console (timestamped) and the Gauge HTML report. |
| `ScreenshotUtil` | Timestamped PNGs under `screenshots/`. |
| `TextNormalizer` | Shared normalization for product-name comparison (Turkish-locale lowercase, whitespace collapsing). |

## Ground Rules
- No `Thread.sleep`; every wait is conditional (`WebDriverWait`).
- No credentials or URLs in source code; everything lives in the config/env layer.
- CAPTCHA / bot protection is never bypassed: it is detected, screenshotted and
  reported as a readable failure. Same policy for SMS/OTP codes.
- The test works against a real account's cart; the scenario deletes what it added,
  but products left over from other sources must be removed manually.
