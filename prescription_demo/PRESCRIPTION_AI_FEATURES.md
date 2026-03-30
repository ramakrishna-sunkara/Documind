# Prescription AI Demo

## Overview

**Prescription AI** is an intelligent medication assistant that helps patients understand their prescriptions using on-device AI with cloud fallback. It provides instant answers about medications, dosages, interactions, and safety - all while maintaining privacy.

---

## Key Features

### 1. Prescription List View
- **Visual medication cards** displaying:
  - Medicine name and dosage
  - Frequency (e.g., "Twice daily")
  - Duration of treatment
  - Prescribing doctor and date
  - Special instructions
  - Warning labels (highlighted in amber)

### 2. AI Chat Assistant
- **Natural language queries** - Ask questions in plain English
- **Quick action chips** for common queries:
  - Summary
  - Today's Medications
  - Drug Interactions
  - Dosage Schedule
  - Side Effects

### 3. Intelligent Response System

| Mode | Description |
|------|-------------|
| **Offline AI** | On-device LLM processes queries privately - no data leaves your phone |
| **Online Fallback** | When offline AI is uncertain, automatically fetches verified medical information |

### 4. Voice Integration
- **Speech-to-Text**: Tap mic button to speak your question
- **Text-to-Speech**: AI reads responses aloud (speaker icon on messages)
- Hands-free operation for accessibility

---

## Sample Interactions

| User Question | AI Response Type |
|---------------|------------------|
| "Summarize my prescriptions" | Lists all medications with key details |
| "Can I drink alcohol?" | Shows alcohol warnings for relevant medications |
| "What's my schedule for today?" | Morning/Evening medication timetable |
| "What if I miss a dose?" | Missed dose guidance |
| "Can I take aspirin with these?" | OTC interaction warnings |
| "I'm pregnant, is this safe?" | Pregnancy safety information |

---

## Privacy & Security

- **100% Offline Capable** - Core AI runs entirely on device
- **No Data Upload** - Prescription data never leaves your phone
- **On-Device Processing** - Powered by Gemma 1B LLM
- **Cloud Fallback** - Only used when offline AI can't answer; no personal data sent

---

## Technical Highlights

- Built with **Jetpack Compose** (Modern Android UI)
- **MediaPipe LLM Inference** for on-device AI
- **Android Speech APIs** for voice features
- **Material 3 Design** with accessibility support
- **Offline-first architecture** with smart fallback

---

## Use Cases

1. **Medication Understanding** - "What does Metformin do?"
2. **Safety Checks** - "Can I drive after taking this?"
3. **Schedule Management** - "When should I take my pills?"
4. **Interaction Warnings** - "Are there any drug conflicts?"
5. **Lifestyle Questions** - "Can I drink alcohol with my tablets?"

---

## Demo Questions

### Questions answered by Offline LLM:
- "Summarize all my prescriptions"
- "What medications am I taking?"
- "Tell me about my dosages"

### Questions that fallback to Online:
- "Can I take ibuprofen with my tablets?"
- "I'm pregnant, is this safe?"
- "Can I drive after taking my pills?"
- "How should I store these medications?"
- "What if I took too much?"
- "Can I stop taking this early?"

---

*Prescription AI - Your Personal Medication Assistant*
