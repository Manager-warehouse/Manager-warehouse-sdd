# WMS Constitution Index

Canonical constitution source: `.specify/memory/constitution.md`

This file intentionally stays short to avoid two divergent constitutions.
Speckit skills, agents, and human reviewers MUST treat
`.specify/memory/constitution.md` as the source of truth.

Use these detailed constraint files for quick domain lookup:

- `.sdd/constraints/global.md` - stack, naming, API, structure, and tooling.
- `.sdd/constraints/business.md` - WMS domain invariants and approval rules.
- `.sdd/constraints/safety.md` - security, data integrity, and operational
  safety rules.

If a rule in this folder conflicts with `.specify/memory/constitution.md`, amend
the canonical constitution first, then update the derived constraint documents.
