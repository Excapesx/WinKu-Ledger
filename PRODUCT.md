# WinKu 小账本

## Platform
Android 8.0+ phone app, lightweight native notification collector and local WebView ledger UI.

## Users and purpose
User wants automatic personal bookkeeping from WeChat/Alipay notifications, without opening transaction pages or entering each payment. First installation requires notification access and phone-specific background settings.

## Confirmed scope
Small installation, event-driven background collection, recovery after reboot, local storage, minimal performance impact. Ledger features follow the user-provided 小账本-手机版.html. UI uses the existing WinKu brand colors. No promises of uninterrupted execution or complete payment history when notifications are missing.

## Implementation decisions
Native Java, Android platform APIs only, no bundled browser, analytics, ads, network permission, accessibility service, or polling. SQLite in private app storage. Collector in a separate process so the WebView is not loaded to collect notifications. Only clear completed transaction notifications auto-post; ambiguous candidates remain in inbox. Original HTML remains unchanged at its source path.
