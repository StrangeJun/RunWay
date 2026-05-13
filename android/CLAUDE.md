# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

This is a new, empty Android project. The only configuration present is an IntelliJ IDEA module (`.idea/`) targeting JDK 21 (JBR). No Gradle build system, source files, or dependencies have been added yet.

## Setup Notes

- IntelliJ module type: `JAVA_MODULE` (not yet a Gradle-based Android project)
- Language level: JDK 21

When the project is initialized with Gradle and Android SDK, update this file with:
- Build, lint, and test commands (e.g., `./gradlew assembleDebug`, `./gradlew test`, `./gradlew lint`)
- Min/target SDK versions and key dependencies
- Architecture overview (e.g., MVVM, Clean Architecture layers, module structure)
- Any non-obvious conventions or constraints
