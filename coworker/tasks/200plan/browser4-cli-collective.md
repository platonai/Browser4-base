# 实现 browser4-cli 协同模式

## Collective Mode

```shell
# Collective mode: run multiple browser instances in parallel to perform tasks faster and more efficiently.
browser4-cli co create --profile-mode=temporary --max-open-tabs=8 --max-browser-contexts=2 --display-mode=GUI
browser4-cli co submit https://www.amazon.com/dp/B08PP5MSVB -deadline 2026-2-24T23:59:59Z
browser4-cli co submit --seed-file=seeds.txt
browser4-cli co scrape https://www.amazon.com/dp/B08PP5MSVB --selector=".product-title" --attribute="textContent" --output=title.txt

browser4-cli close
```
