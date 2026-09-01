# Changelog

## [Unreleased]

- Replaced numbered category/page clerks with one canonical merchant per Trading Post.
- Added a client-side searchable catalog screen with localized-name and registry-ID filtering.
- Added bounded catalog synchronization and server-authoritative purchase payloads with session, distance, payment, capacity, and stale-version validation.
- Added safe legacy page-clerk migration and reduced new structure templates to one merchant marker.
- Restricted the catalog to audited Survival-obtainable vanilla items with centralized JSON-compatible enable/price/quantity metadata.
- Added extensionless `config/config` loading with automatic defaults, field-level validation, partial overrides, and malformed-file fallback.
- Made TradeEverything villagers stationary with persistent anchors, vanilla `NoAI`, and collision correction.
- Made jigsaw placement footprint-aware, rejecting unsuitable terrain and adding a reproducible vanilla-block foundation.
- Replaced custom entities and structure registries with vanilla villagers and a vanilla jigsaw/template structure.
- Added safe reload, persistence/idempotence checks, and client/common compatibility tests.

## 0.1.0 - 2026-08-28

- Initial production release target for Minecraft 26.2.
