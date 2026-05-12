# Content Version Registration

The Open Circuits Android app bundles HTML content from the upstream [open-circuits](https://github.com/trydydd/open-circuits) repository. Content versions are registered explicitly for integrity verification and to ensure all maintainers approve bundled content before release.

## How it works

1. **Content is versioned by upstream release tag** — e.g., `v0.4.0`, `v1.0.0`, `v1.1.0`
2. **SHA256 hashes are pre-registered** — in `scripts/sync_content.sha256`
3. **Sync script verifies integrity** — `sync_content.sh` matches downloaded tarball against registered hash
4. **CI workflow picks latest registered version** — The build workflow automatically syncs and bundles the latest approved version

## Registering a new version

When a new open-circuits release is published, register it by:

### 1. Verify the upstream release exists
   
```bash
# Check https://github.com/trydydd/open-circuits/releases
# Find the tag you want to register (e.g., v1.2.0)
```

### 2. Compute the SHA256 hash

```bash
curl -fL -o /tmp/oc.tar.gz \
  https://github.com/trydydd/open-circuits/archive/refs/tags/v1.2.0.tar.gz
sha256sum /tmp/oc.tar.gz
# Output: f0a18eb5...  /tmp/oc.tar.gz
```

### 3. Add entry to `scripts/sync_content.sha256`

Append the hash and tag to the file. The format matches `sha256sum` output:

```bash
echo "f0a18eb5...  v1.2.0" >> scripts/sync_content.sha256
```

### 4. Verify the sync works

```bash
./scripts/sync_content.sh v1.2.0
# Should succeed and extract content to app/src/main/assets/html/
```

### 5. Commit the changes

```bash
git add scripts/sync_content.sha256 app/src/main/assets/html/
git commit -m "Register content v1.2.0

Upstream release: https://github.com/trydydd/open-circuits/releases/tag/v1.2.0
SHA256: f0a18eb5..."
```

## Workflow automation

The CI build workflow (`.github/workflows/build.yml`) automatically:

1. Checks if content is already bundled (`app/src/main/assets/html/index.html` exists)
2. If not, reads `scripts/sync_content.sha256` to find the latest registered version
3. Runs `sync_content.sh` to download, verify, and extract that version
4. Proceeds with build (lint, test, assemble)

This ensures every build includes approved, verified content without manual intervention.

## Security model

- **Manual registration** — Only explicitly approved versions are bundled
- **Integrity verification** — SHA256 hash prevents tampering or accidental corruption
- **Version discovery** — CI workflow finds the latest approved version automatically
- **Traceability** — Commit history documents when and why each version was approved

## File format: `scripts/sync_content.sha256`

```
# Comments start with #
# Blank lines are ignored
# Format: <sha256>  <tag>
# Example:
f0a18eb564e0ff6d3dfdea0689199b42307dfd626f17815d36d2d2af7159432b  v1.1.0
```

Lines are processed in order. The build workflow uses the **last non-comment, non-blank line** as the latest registered version. To promote a different version, reorder lines or remove older ones.
