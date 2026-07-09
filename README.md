# Hepsiburada UI Automation — HB-TC01

[![CI](https://github.com/seceremrecan/hepsiburada-automation/actions/workflows/ci.yml/badge.svg)](https://github.com/seceremrecan/hepsiburada-automation/actions/workflows/ci.yml)

End-to-end shopping scenario for Hepsiburada.com built with **Java 17 + Selenium WebDriver 4 +
Gauge + Maven**, using a **keyword-driven** architecture (no Page Object Model):
**login → search "bilgisayar" → pick the first product of the second row → open the product →
add to cart → verify it is in the cart.**

## Running

```bash
mvn test                          # compiles + runs the Gauge UI scenario (opens a browser)
mvn test-compile surefire:test    # only the fast unit tests (no browser)
```

## Continuous Integration

`.github/workflows/ci.yml` runs on every push and pull request:

| Job | Trigger | What it does |
|---|---|---|
| **build-and-verify** | every push / PR | Compiles, runs the JUnit unit tests (no browser), validates every `element-infos/*.json`, fails on duplicate locator keys, and asserts that the credentials file is **never** committed. Fast and deterministic — stays green. |
| **ui-test** | manual (`workflow_dispatch`) | Installs the Gauge CLI and runs the real browser scenario headless, taking credentials from GitHub Secrets (`HB_USERNAME` / `HB_PASSWORD`). Uploads the HTML report and failure screenshots as artifacts. |

The live UI scenario is deliberately **not** run on every commit: it depends on a live
site, a real account and the site's bot protection, which would make CI flaky. This is
standard practice — fast, deterministic checks gate every commit; end-to-end runs are
triggered on demand.

- Gauge CLI must be installed and on PATH (with the `java` and `html-report` plugins).
- Run via `mvn test`, **not** `gauge run`: Gauge's own compiler cannot see Maven
  dependencies; the gauge-maven-plugin supplies the classpath from Maven.
- Credentials: `env/secrets/credentials.properties` (gitignored; template at
  `credentials.example.properties`). Alternatively set the `HB_USERNAME` /
  `HB_PASSWORD` environment variables — no file needed.
- HTML report: `reports/html-report/index.html` · Failure screenshots: `screenshots/`
- Console: every step is logged as `[HH:mm:ss.SSS] [HB-TC01] ...`

## Expected Results (Assertions)

1. User logs in successfully (username visible in the header).
2. Search results match the user input (product grid shown + URL contains the term).
3. Redirected to the second-row first product's page (detail title matches).
4. Product is added to the cart (confirmation shown).
5. The added product is visible on the cart screen.

## Project Layout

```
├── pom.xml                       # Maven: deps + gauge-maven-plugin (bound to test phase) + JUnit 5
├── manifest.json                 # Gauge project descriptor (language: java, plugins)
├── specs/
│   ├── Scenarios/hepsiburada_sepet.spec    # WHAT: business-language steps (Turkish)
│   └── Concepts/hepsiburada_tanimlar.cpt    # each business step → generic keyword steps
├── env/
│   ├── default/default.properties          # base URL, browser, timeouts, typing delays
│   └── secrets/credentials.properties       # credentials (GITIGNORED)
└── src/test/
    ├── java/com/hepsiburada/
    │   ├── config/   # ConfigReader — single settings entry point
    │   ├── steps/    # StepImplementation — the ONE generic keyword-driven step library
    │   └── utils/    # driver, waits, popups, bot detection, logging helpers
    └── resources/
        ├── element-infos/*.json             # locators addressed by KEY (no locators in code)
        ├── value-infos/<env>/values.json    # "Data_*" test data (passwords via config: layer)
        ├── qa-web.yaml / browser-options.yaml
        └── env config
```

## Layered Design (keyword-driven)

Three layers, each in its own language, so new scenarios can be built **without writing Java** —
only a spec, a concept, and JSON element/value entries:

1. **`specs/*.spec` — business language (Turkish).** WHAT the test does, sentence by sentence:
   `* Basariyla Email "Data_Email_User" ve Sifre "Data_Password" Giris Yapilir`.
2. **`specs/Concepts/*.cpt` — concept layer.** Each business sentence expands into small, reusable
   **generic keyword steps**: `Find element "txt_username" and sendkey text <email>`,
   `Verify element "lbl_account_logged_in" is displayed`, …
3. **`steps/StepImplementation` — generic implementations.** No step is tied to a specific page or
   button; every step takes an element **key** (resolved from `element-infos/*.json`) and/or a text
   value. Actions and assertions (AssertJ) both live here.

Supporting data lives outside the code:
- **Locators** in `element-infos/*.json`, addressed by key (`txt_username`, `btn_add_to_cart`, …).
  Dynamic class names are avoided; stable attributes (`data-test-id`, `id`, `aria-label`) are used.
- **Test data** in `value-infos/<env>/values.json` via `Data_*` keys; passwords are never stored
  here — they resolve through `config:hb.password` to the gitignored credentials layer.

## File Guide

### steps/
| File | Purpose |
|---|---|
| `StepImplementation` | The single generic keyword-driven step library. Steps: navigate, dismiss popups, click / click-if-present / click-unless-visible (handles one-step **and** two-step login forms), type, type-and-search (resilient to Hepsiburada's search-box swap), store attribute, switch tab, verify displayed / url contains / text contains stored / one-of contains stored. |

### config/
| File | Purpose |
|---|---|
| `ConfigReader` | Single entry point for all settings. Priority: `-D` system property > environment variable > properties/yaml file. Missing required values fail with a message naming exactly which file to fill in. |

### utils/
| File | Purpose |
|---|---|
| `DriverFactory` | Hardened Chrome lifecycle (automation flags, notifications blocked, ThreadLocal). Optional persistent profile (`chrome.profile.dir`) so the site remembers the device and stops asking for SMS codes. Implicit waits deliberately absent. |
| `ExecutionHooks` | Gauge lifecycle: driver per scenario, screenshot on failure, screenshots folder cleaned at suite start, `holdBrowserSeconds` pause before closing (for demo/video). |
| `Waits` | The ONE waiting mechanism (`Thread.sleep` is banned). Only genuinely transient exceptions (stale element, execution-context) are retried. |
| `ElementRepository` | Resolves an element **key** to a Selenium `By` from `element-infos/*.json`. |
| `ValueResolver` | Resolves `Data_*` keys from `value-infos`; `config:x.y` delegates to `ConfigReader` so secrets stay out of the data files. |
| `PopupHandler` | Registry-based popup dismissal + closing the cookie banner from inside its **shadow DOM** via JS; targeted removal of a blocking checkout overlay. |
| `BotDetection` | CAPTCHA / bot-wall **detection** (never bypass): DOM markers + text scan → screenshot + `BlockedException`. Also detects SMS/OTP screens → `OtpRequiredException` with a human-readable fix. Wired into navigation and the display verifications. |
| `HumanTyper` | Human-like typing with randomized per-key delays (W3C Actions `pause`, not `Thread.sleep`). |
| `StepLogger` | One call logs to both the console (timestamped) and the Gauge HTML report. |
| `ScreenshotUtil` | Timestamped PNGs under `screenshots/`. |
| `TextNormalizer` | Shared normalization for product-name comparison (Turkish-locale lowercase, whitespace collapsing). Covered by `TextNormalizerTest` (JUnit 5). |

## Ground Rules
- No `Thread.sleep`; every wait is conditional (`WebDriverWait`).
- No credentials or URLs in source code; everything lives in the config/env layer.
- CAPTCHA / bot protection is never bypassed: it is detected, screenshotted and
  reported as a readable failure. Same policy for SMS/OTP codes.
- The scenario leaves the product in the cart (natural end state); each run does not
  depend on a clean cart because the "in cart" check matches the product by name.
