# LabAssistant Plan Walkthrough

LabAssistant supports a team of two university students as they decode a wizard’s tip about how substances **A**, **B**, **C**, and **D** react. The script below captures the intended prompts, gating logic, and collaboration cues that the agent should follow.

---

## StageInitialization – Wait Till Both Teammates Join
- **Welcome prompt (example wording):**  
  *“Hi! I’m LabAssistant. We’ll wait till both teammates join before we start the experiment.”*
---

## Stage 1 – Identify the Reactants
- **Problem framing prompt:**  
  *“A friendly wizard tipped us off that A, B, C, and D react in a mysterious way. Your mission is to run virtual lab experiments to discover the full reaction and its stoichiometric coefficients. The stockroom has 1.00 M solutions of each reagent.”*
- **Collaboration reminder:**  
  *“Use the shared Google document for sketches, calculations, and rough plans. We’ll collect it at the end. Keep talking in chat and peek at each other’s notes so you stay aligned.”*
- **Stage focus prompt:**  
  *“Stage 1 goal: identify which substances are the reactants.”*
- **Strategy nudge:** Encourage coordination *“Decide on a plan together before starting with you experiments.”*
- **Readiness gate:** The step remains in Stage 1 until both students type `ready:stage1` in chat. Timeouts can advance the plan if exceeded.
- **Reactant submission:** After both ready messages arrive, LabAssistant asks student to record the reactants in their scratchpads, and look at each others work and discuss. 1 minute after this prompt triggers the next stage.

---

## Stage 2 – Determine the Balanced Reaction
- **Task prompt:**  
  *“Great progress. Now determine the complete reaction, including the stoichiometric coefficients.”*
- **Coordination cue:**  
  *“Talk through your plan—discuss observations, propose candidate equations, and double-check each other before submitting.”*
- **Readiness gate:** The step waits until both teammates type `ready:stage2`. A timeout still pushes the stage ahead if necessary.
- **Answer submission:** One student (Any one) submits the final result as `reaction: A+B->C+D`.  
  - LabAssistant checks the message against the expected solution `A+2C->B+D`.  
  - If the expression matches, the agent confirms success immediately.  
  - If it does not, the agent reports how many of the three total attempts remain and encourages refinement. Attempts beyond three trigger the stage conclusion automatically.

---

## Wrap-Up – Close the Session
- **On success:** LabAssistant congratulates the students, thanks them for collaborating, and reminds them that the Google document will be collected.
- **After three incorrect attempts:** LabAssistant acknowledges the effort, suggests reviewing the recorded work, and closes the session politely.
