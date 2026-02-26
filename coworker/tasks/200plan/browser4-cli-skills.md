# Visit-and-extract Skill

## Visit-and-Extract Skill

```shell

browser4-cli agent run --skill=visit-and-extract '
Go to https://www.amazon.com/dp/B08PP5MSVB

After browser launch:
  - clear browser cookies
  - go to https://www.amazon.com/
  - wait for 5 seconds
  - click the first product link
After page load: scroll to the middle.

Summarize the product.
Extract: product name, price, ratings.
Find all links containing /dp/.
'
```
