# Compact Daily Memory

When coworker load daily memory, if the memory is larger than 3000 characters, it will be truncated to the last 3000 characters, it should be compressed.
The compressed memory should be written back the memory file, and the original memory should be saved as a backup file with the name `MEMORY.YYYYMMDD.long.md`.

## Compression Rules

- shorten descriptions for all points.
- shorten task descriptions, can be very brief, just keep the keywords.
- combine similar tasks into one entry.
- remove redundant information.
- make sure the compressed memory is less than 3000 characters.
