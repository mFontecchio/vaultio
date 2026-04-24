# Plan: On-Device AI Grading Assistant

## Overview

Implement an on-device AI system to provide "pre-grading" estimates for trading cards. This feature
helps users decide which cards are worth sending to professional grading services (PSA/BGS) by
analyzing centering, corners, edges, and surface quality.

## Core Objectives (2026 Standards)

- **Privacy First:** 100% on-device processing. No images uploaded to the cloud.
- **Modern AI Stack:** Use Google AI Edge (Gemini Nano) for visual analysis and MediaPipe for
  precise geometric measurements (centering).
- **Proactive UX:** Provide real-time AR feedback during scanning to ensure optimal capture quality.

## Technical Architecture

### 1. Data Layer

- **Entity Updates:** Update `UserCard` or create `CardGradeEstimate` entity in Room to store
  sub-grades (Centering, Corners, Edges, Surface) and the timestamp of the analysis.
- **Repository Integration:** Add `GradingService` to handle AI model lifecycle and analysis logic.

### 2. Domain Layer (AI Logic)

- **Centering Analyzer:** MediaPipe-based edge detection to calculate T/B and L/R ratios.
- **Condition Classifier:** Gemini Nano (Vision) to detect "Whitening," "Dings," "Scratches," and "
  Silvering."
- **Grading Algorithm:** A weighted logic that combines sub-grades into a final 1.0–10.0 "Vaultio
  Score."

### 3. UI Layer (MVI)

- **Scanner Enhancement:** New `GradingMode` in `ScannerScreen`.
- **Guided Capture:** UI overlays instructing the user to "Tilt for Surface Scan" or "Hold Steady
  for Centering."
- **Grade Detail Screen:** A premium view using `ThreeDCard.kt` to show the card with a "Digital
  Grade" slab and interactive sub-grade breakdowns.

## Progress Tracking

- [x] **Phase 1: Foundation (Architecture & Data)**
    - [x] Define `CardGradeEntity` and update Room schema (v13).
    - [x] Create `CardGradeDao` and integrate into `VaultioDatabase`.
    - [x] Add MediaPipe & AI dependencies to Version Catalog and Gradle.
    - [x] Implement MVI Contract (`GradingContract.kt`).
    - [x] Create `GradingRepository` (Placeholder logic).
    - [x] Create `GradingViewModel` & `GradingScreen` (Base UI).
    - [x] Setup Dependency Injection in `VaultioApplication`.
    - [x] Wire Navigation in `MainActivity`.
- [x] **Phase 2: Actual AI Implementation (Centering)**
    - [x] Run Gradle Sync to resolve new AI dependencies.
    - [x] Implement `ObjectDetector` in `GradingRepository` using MediaPipe.
    - [x] Create `CameraAnalyzer` overlay for "Safe Zone" in `ScannerScreen`.
    - [x] Implement image capture passing (Scanner -> Grading).
- [x] **Phase 3: Condition Analysis (Corners/Edges/Surface)**
    - [x] Integrate Gemini Nano (via AICore) for visual reasoning.
    - [ ] Implement Multi-frame capture for surface reflection detection.
    - [x] Refine "Reasoning" text generation with real AI insights.
- [x] **Phase 4: Polish & UX**
    - [x] Add 3D "Holographic Slab" effect to results.
    - [x] Add vibration feedback and Material 3 animations.
    - [ ] Final accessibility pass (TalkBack, Content Descriptions).

## Current Status

**Phase 1 Completed.** The infrastructure is ready. We have the database, the MVI flow, and the
navigation wired up.
**Next Up:** Synchronizing Gradle and implementing the actual MediaPipe edge detection logic.

## UI/UX Best Practices

- **Material 3:** Use adaptive color schemes and expressive motion.
- **Micro-interactions:** Vibration feedback on successful analysis steps.
- **Transparency:** Clearly state that this is an *estimate* and not a professional guarantee.

## Success Metrics

- Analysis completes in < 2 seconds on NPU-enabled devices.
- Accuracy within ±0.5 grade of professional samples (tested against a set of known PSA graded
  cards).
