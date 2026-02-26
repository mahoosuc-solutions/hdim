# Investor Outreach Dashboard — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an internal Node/Express web server at `tools/investor-dashboard/` that displays investor outreach content, tracks pipeline status, and generates copy-paste-ready personalized emails.

**Architecture:** Express server with EJS templates, SQLite via better-sqlite3, Bootstrap 5 UI. Markdown docs are parsed once via a seed script to populate the database. All pages are server-rendered.

**Tech Stack:** Node 18+, Express, EJS, better-sqlite3, Bootstrap 5, Chart.js, marked (markdown parsing)

## Tasks

### Task 1: Project Scaffold & Dependencies
### Task 2: SQLite Schema
### Task 3: Seed Script — Parse Markdown Into SQLite
### Task 4: Layout & Dashboard Page
### Task 5: Contacts List & Detail Pages
### Task 6: Compose Page — Form-Based Email Personalization
### Task 7: Templates & Partnerships Pages
### Task 8: Polish & Final Integration

See full implementation details in the design doc: `docs/plans/2026-02-25-investor-outreach-dashboard.md`
