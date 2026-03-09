# Rendering ARIA Snapshots

Implement ARIA snapshot rendering in the DOM state via `DOMState#render()`.

The current implementation generates a YAML string from `NanoDOMTreeNode`. We want the output format to match Playwright's
implementation. You can find Playwright's reference implementation in the `ariaSnapshot` file.

The rendered output should be a YAML string in the following format:

```yml
- generic [ref=e2]:
    - region "Skip to main content":
        - link "Skip to main content" [ref=e3] [cursor=pointer]:
            - /url: "#__docusaurus_skipToContent_fallback"
    - navigation "Main" [ref=e4]:
        - generic [ref=e5]:
            - generic [ref=e6]:
                - link "API" [ref=e12] [cursor=pointer]:
                    - /url: /docs/api/class-playwright
                - button "Node.js" [ref=e14] [cursor=pointer]
                - link "Community" [ref=e15] [cursor=pointer]:
                    - /url: /community/welcome
            - generic [ref=e16]:
                - link "GitHub repository" [ref=e17] [cursor=pointer]:
                    - /url: https://github.com/microsoft/playwright
                - link "Discord server" [ref=e18] [cursor=pointer]:
                    - /url: https://aka.ms/playwright/discord
                - button "Switch between dark and light mode (currently system mode)" [ref=e20] [cursor=pointer]:
                    - img [ref=e21]
    - generic [ref=e34]:
        - banner [ref=e35]:
            - generic [ref=e36]:
                - heading "Playwright enables reliable end-to-end testing for modern web apps." [level=1] [ref=e37]
                - generic [ref=e38]:
                    - link "Get started" [ref=e39] [cursor=pointer]:
                        - /url: /docs/intro
```

Please review Playwright's implementation and create the Kotlin version so the output format remains consistent.
You can find the expected output in `page-2026-03-02T02-30-32-761Z.yml`.

## References

[ariaSnapshot](D:\workspace\playwright\packages\injected\src\ariaSnapshot.ts)
[page-2026-03-02T02-30-32-761Z.yml](../../../pulsar-tests/pulsar-tests-common/src/test/kotlin/ai/platon/pulsar/test/browser/page-2026-03-02T02-30-32-761Z.yml)
