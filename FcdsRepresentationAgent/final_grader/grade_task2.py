"""Bree final-grader adapter; shares the live check contract."""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from assessment import assess

def grade(code):
    passed, feedback = assess("task2", code)
    return float(passed), feedback
