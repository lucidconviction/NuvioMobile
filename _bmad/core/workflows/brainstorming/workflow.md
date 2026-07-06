# BMAD Brainstorming Workflow

> **BMAD** — **B**ound, **M**ap, **A**nalyze, **D**ecide
> A structured brainstorming framework for product features.

---

## Phase 1: Bound — Define the Scope

### 1.1 Problem Statement
What specific problem are we solving? (1-2 sentences)

### 1.2 Goals & Constraints
- **Primary goal:** What must this achieve?
- **Secondary goals:** What would be nice to have?
- **Non-goals:** What is explicitly out of scope?
- **Constraints:** Platform, performance, timeline, dependencies

### 1.3 Stakeholders & Users
Who will use this? What are their needs?

---

## Phase 2: Map — Explore the Landscape

### 2.1 Reference Audit
Analyze existing references (screenshots, competitors, prior art):
- What patterns stand out?
- What would work well in our design system?
- What should we avoid?

### 2.2 Existing Code Audit
Search the codebase for:
- Existing related models, clients, or services
- Reusable UI components and patterns
- Platform capabilities (navigation, data fetching, caching)

### 2.3 User Flow Sketch
Map the primary user journey(s) end-to-end:
- Entry point → Navigation → Content → Interaction

---

## Phase 3: Analyze — Evaluate Ideas

### 3.1 Idea Generation
List all possible approaches, features, and components.

### 3.2 Trade-off Analysis
For each major approach, evaluate:
- **Effort:** Low / Medium / High
- **Impact:** Low / Medium / High
- **Risk:** Low / Medium / High
- **Dependencies:** What else needs to change?

### 3.3 Priority Matrix
Plot ideas on a Effort-vs-Impact grid to identify quick wins vs. long-term investments.

---

## Phase 4: Decide — Commit to a Plan

### 4.1 Selected Approach
Describe the chosen approach and why.

### 4.2 Component Tree
Outline the UI component hierarchy.

### 4.3 Data Flow
How does data move from source to screen?
- API / Repository → ViewModel / State → Composable

### 4.4 Implementation Phases
Break into ordered, incremental steps:
- **Phase A:** Core skeleton (tab, empty state, navigation)
- **Phase B:** Key feature (content loading, primary UI)
- **Phase C:** Enhancement (secondary features, polish)
- **Phase D:** Refinement (edge cases, performance, testing)

### 4.5 Acceptance Criteria
What defines "done" for each phase?

---

## Workflow Outputs

At the end of this workflow you should have:
1. A clear feature specification
2. A prioritized implementation plan
3. A component tree and data flow diagram
4. Acceptance criteria for each phase
